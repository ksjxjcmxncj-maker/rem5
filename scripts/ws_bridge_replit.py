#!/usr/bin/env python3
"""
NRO WebSocket Bridge — Chạy trên REPLIT (thay thế Codespace)
Replit expose HTTPS/WebSocket qua domain replit.dev — không cần bore.pub

Kiến trúc:
  Android (NRO Bridge APK)
      ↓ WebSocket wss://YOUR-REPL.replit.dev/ws
  Replit (script này — chạy port $PORT)
      ↓ TCP 127.0.0.1:14445
  Game Server Java (cần chạy cùng Replit hoặc Codespace)

CÁCH CHẠY:
  python3 scripts/ws_bridge_replit.py

LƯU Ý: 
  - Replit expose port qua HTTPS/WSS tự động
  - URL: wss://$REPLIT_DEV_DOMAIN/ws
  - Game server phải chạy trên port 14445 (cùng máy)
"""
import asyncio
import os
import logging
import sys
import signal

logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s %(message)s',
    datefmt='%H:%M:%S'
)
log = logging.getLogger("NROBridge")

GAME_HOST = "127.0.0.1"
GAME_PORT = 14445
LISTEN_PORT = int(os.environ.get("PORT", 8080))
WS_PATH = "/ws"

try:
    import websockets
    from websockets.server import serve
except ImportError:
    import subprocess
    subprocess.run([sys.executable, "-m", "pip", "install", "websockets", "-q"])
    import websockets
    from websockets.server import serve

try:
    import websockets.legacy.server as legacy
    USE_LEGACY = True
except ImportError:
    USE_LEGACY = False


# ── Thống kê kết nối
stats = {"total": 0, "active": 0}


async def handle_client(websocket, path=None):
    client_addr = websocket.remote_address
    
    # Chỉ nhận kết nối ở path /ws
    req_path = getattr(websocket, 'path', path or '/ws')
    if req_path not in ('/ws', '/ws/'):
        log.info(f"Từ chối {client_addr} — path: {req_path}")
        return

    stats["total"] += 1
    stats["active"] += 1
    conn_id = stats["total"]
    log.info(f"[#{conn_id}] Game client kết nối: {client_addr} (active: {stats['active']})")

    try:
        reader, writer = await asyncio.open_connection(GAME_HOST, GAME_PORT)
        log.info(f"[#{conn_id}] Đã kết nối game server {GAME_HOST}:{GAME_PORT}")
    except ConnectionRefusedError:
        log.error(f"[#{conn_id}] ❌ Game server {GAME_HOST}:{GAME_PORT} chưa chạy!")
        stats["active"] -= 1
        return
    except Exception as e:
        log.error(f"[#{conn_id}] Lỗi kết nối game server: {e}")
        stats["active"] -= 1
        return

    bytes_up = [0]
    bytes_down = [0]

    async def ws_to_tcp():
        """WebSocket → TCP (dữ liệu từ client đến game server)"""
        try:
            async for msg in websocket:
                if isinstance(msg, bytes):
                    writer.write(msg)
                    await writer.drain()
                    bytes_up[0] += len(msg)
        except Exception as e:
            log.debug(f"[#{conn_id}] ws→tcp end: {e}")
        finally:
            writer.close()
            try:
                await writer.wait_closed()
            except Exception:
                pass

    async def tcp_to_ws():
        """TCP → WebSocket (dữ liệu từ game server đến client)"""
        try:
            while True:
                data = await reader.read(65536)
                if not data:
                    break
                await websocket.send(data)
                bytes_down[0] += len(data)
        except Exception as e:
            log.debug(f"[#{conn_id}] tcp→ws end: {e}")
        finally:
            try:
                await websocket.close()
            except Exception:
                pass

    await asyncio.gather(ws_to_tcp(), tcp_to_ws())
    stats["active"] -= 1
    log.info(
        f"[#{conn_id}] Ngắt kết nối {client_addr} | "
        f"↑{bytes_up[0]//1024}KB ↓{bytes_down[0]//1024}KB | "
        f"active: {stats['active']}"
    )


async def health_check_server():
    """HTTP health check endpoint cho Replit (giữ repl alive)"""
    from aiohttp import web

    async def handle(request):
        return web.Response(
            text=f"NRO Bridge OK | connections: {stats['active']}/{stats['total']}",
            content_type='text/plain'
        )

    app = web.Application()
    app.router.add_get('/', handle)
    app.router.add_get('/health', handle)
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, '0.0.0.0', LISTEN_PORT)
    await site.start()
    log.info(f"Health check: http://0.0.0.0:{LISTEN_PORT}/")
    return runner


async def main():
    # WebSocket server chạy trên port LISTEN_PORT+1 (hoặc cùng port nếu dùng reverse proxy)
    ws_port = LISTEN_PORT + 1 if LISTEN_PORT < 65534 else LISTEN_PORT

    replit_domain = os.environ.get('REPLIT_DEV_DOMAIN', '')

    log.info("╔══════════════════════════════════════════╗")
    log.info("║   NRO WebSocket Bridge — Replit          ║")
    log.info("╚══════════════════════════════════════════╝")
    log.info(f"→ WebSocket: ws://0.0.0.0:{ws_port}{WS_PATH}")
    log.info(f"→ Game Server: {GAME_HOST}:{GAME_PORT}")

    if replit_domain:
        log.info(f"→ Public URL: wss://{replit_domain}/ws")
        log.info(f"→ Dùng URL trên cho NRO Bridge APK")

    # Thử khởi động health check (aiohttp)
    health_runner = None
    try:
        health_runner = await health_check_server()
    except Exception as e:
        log.warning(f"Health check không khởi động được: {e}")

    # WebSocket server
    ws_server = await serve(
        handle_client,
        "0.0.0.0",
        ws_port,
        ping_interval=20,
        ping_timeout=10,
        max_size=10 * 1024 * 1024,
        compression=None,
    )

    log.info(f"✅ Đang lắng nghe ws://0.0.0.0:{ws_port}/ws")
    log.info("Ctrl+C để dừng")

    # Graceful shutdown
    loop = asyncio.get_running_loop()
    stop = loop.create_future()

    def _sig(sig, frame):
        log.info("Nhận tín hiệu dừng...")
        loop.call_soon_threadsafe(stop.set_result, None)

    signal.signal(signal.SIGTERM, _sig)
    signal.signal(signal.SIGINT, _sig)

    await stop

    ws_server.close()
    await ws_server.wait_closed()
    if health_runner:
        await health_runner.cleanup()
    log.info("Bridge dừng.")


if __name__ == "__main__":
    asyncio.run(main())

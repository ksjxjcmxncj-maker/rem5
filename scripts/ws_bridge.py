#!/usr/bin/env python3
"""
ws_bridge.py v5 — NRO WebSocket Bridge (Server-side)
Codespace port 8080 → localhost:14445 (TCP game server)
Không cần frpc/bore/playit — dùng mạng cloud Codespace trực tiếp
"""
import asyncio, websockets, logging, signal, sys, os

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [WS] %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
log = logging.getLogger(__name__)

TARGET_HOST = os.environ.get('NRO_HOST', '127.0.0.1')
TARGET_PORT = int(os.environ.get('NRO_PORT', '14445'))
LISTEN_PORT = int(os.environ.get('WS_PORT', '8080'))
MAX_CONN    = int(os.environ.get('WS_MAX_CONN', '200'))

active_connections = 0

async def handle(ws):
    global active_connections
    if active_connections >= MAX_CONN:
        log.warning(f"Max connections reached ({MAX_CONN}), rejecting")
        await ws.close(1013, "Too many connections")
        return

    peer = ws.remote_address
    active_connections += 1
    log.info(f"[+] {peer} connected (total={active_connections})")

    reader = writer = None
    try:
        reader, writer = await asyncio.wait_for(
            asyncio.open_connection(TARGET_HOST, TARGET_PORT), timeout=5.0
        )

        async def ws_to_tcp():
            async for msg in ws:
                data = msg if isinstance(msg, bytes) else msg.encode('latin1')
                writer.write(data)
                await writer.drain()

        async def tcp_to_ws():
            while True:
                data = await reader.read(8192)
                if not data:
                    break
                await ws.send(data)

        await asyncio.gather(ws_to_tcp(), tcp_to_ws())

    except asyncio.TimeoutError:
        log.warning(f"[!] {peer} — game server timeout (14445 không phản hồi)")
    except websockets.exceptions.ConnectionClosed as e:
        log.info(f"[-] {peer} disconnected: {e.code}")
    except Exception as e:
        log.error(f"[!] {peer} error: {e}")
    finally:
        active_connections -= 1
        if writer:
            try: writer.close(); await writer.wait_closed()
            except: pass
        log.info(f"[-] {peer} cleaned up (total={active_connections})")

async def health_check(path, headers):
    """HTTP health endpoint tại /health"""
    if path == '/health':
        return 200, [('Content-Type', 'text/plain')], b'OK'

async def main():
    log.info(f"ws_bridge v5 khởi động — ws://0.0.0.0:{LISTEN_PORT} → {TARGET_HOST}:{TARGET_PORT}")

    stop = asyncio.Future()
    loop = asyncio.get_event_loop()
    loop.add_signal_handler(signal.SIGTERM, stop.set_result, None)
    loop.add_signal_handler(signal.SIGINT,  stop.set_result, None)

    async with websockets.serve(
        handle, '0.0.0.0', LISTEN_PORT,
        process_request=health_check,
        ping_interval=20,
        ping_timeout=10,
        max_size=2**20,  # 1MB max message
    ):
        log.info(f"✅ ws_bridge sẵn sàng tại port {LISTEN_PORT}")
        await stop

    log.info("ws_bridge dừng.")

if __name__ == '__main__':
    asyncio.run(main())

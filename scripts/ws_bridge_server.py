#!/usr/bin/env python3
"""
WebSocket Bridge SERVER — chạy trên Codespace port 8080
Client gửi TCP game data qua WebSocket → bridge forward tới game server 14445

PHIÊN BẢN TỐI ƯU:
  - ping_interval=None  → KHÔNG gửi PING (BridgeService không handle PONG → kết nối drop sau 30s)
  - TCP_NODELAY         → giảm latency, không buffer nhỏ
  - SO_RCVBUF/SNDBUF    → buffer 512KB mỗi chiều
  - uvloop              → async loop nhanh hơn 2-3x (nếu có)
  - write coalescing    → không drain() sau mỗi packet
"""
import asyncio
import sys
import socket
import logging

logging.basicConfig(level=logging.INFO, format='[%(asctime)s] %(message)s', datefmt='%H:%M:%S')
log = logging.getLogger(__name__)

GAME_HOST = "127.0.0.1"
GAME_PORT = 14445
LISTEN_PORT = 8080
SOCKET_BUF = 524288  # 512KB buffer

try:
    import websockets
except ImportError:
    import subprocess
    subprocess.run([sys.executable, "-m", "pip", "install", "websockets", "-q"])
    import websockets

# Dùng uvloop nếu có — nhanh hơn asyncio mặc định 2-3x
try:
    import uvloop
    asyncio.set_event_loop_policy(uvloop.EventLoopPolicy())
    log.info("✅ uvloop activated")
except ImportError:
    log.info("uvloop không có, dùng asyncio mặc định")

# Thống kê kết nối
stats = {"total": 0, "active": 0, "bytes_up": 0, "bytes_down": 0}


async def handle_client(websocket, path=None):
    client_addr = websocket.remote_address
    conn_id = stats["total"] + 1
    stats["total"] += 1
    stats["active"] += 1
    log.info(f"[#{conn_id}] Client kết nối: {client_addr} (active={stats['active']})")

    try:
        # Kết nối tới game server
        reader, writer = await asyncio.open_connection(GAME_HOST, GAME_PORT)

        # Tối ưu socket game: TCP_NODELAY + buffer lớn
        sock = writer.get_extra_info('socket')
        if sock:
            sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, SOCKET_BUF)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, SOCKET_BUF)

        log.info(f"[#{conn_id}] Đã kết nối game server {GAME_HOST}:{GAME_PORT} (TCP_NODELAY on)")

        bytes_up = 0
        bytes_dn = 0

        async def ws_to_tcp():
            """WebSocket → TCP (client gửi lên game server) — không drain() hot path"""
            nonlocal bytes_up
            try:
                async for data in websocket:
                    if isinstance(data, bytes):
                        writer.write(data)
                        bytes_up += len(data)
                        # Drain chỉ khi buffer đầy (>64KB) để tránh overhead
                        if writer.transport.get_write_buffer_size() > 65536:
                            await writer.drain()
            except Exception as e:
                log.debug(f"[#{conn_id}] ws→tcp end: {e}")
            finally:
                try:
                    await writer.drain()
                    writer.close()
                except Exception:
                    pass

        async def tcp_to_ws():
            """TCP → WebSocket (game server trả về client) — đọc tối đa 65536 mỗi chunk"""
            nonlocal bytes_dn
            try:
                while True:
                    data = await reader.read(65536)
                    if not data:
                        break
                    await websocket.send(data)
                    bytes_dn += len(data)
            except Exception as e:
                log.debug(f"[#{conn_id}] tcp→ws end: {e}")
            finally:
                try:
                    await websocket.close()
                except Exception:
                    pass

        await asyncio.gather(ws_to_tcp(), tcp_to_ws())

    except ConnectionRefusedError:
        log.error(f"[#{conn_id}] ❌ Game server {GAME_HOST}:{GAME_PORT} chưa chạy!")
    except Exception as e:
        log.error(f"[#{conn_id}] Lỗi: {e}")
    finally:
        stats["active"] -= 1
        stats["bytes_up"] += bytes_up if 'bytes_up' in dir() else 0
        stats["bytes_down"] += bytes_dn if 'bytes_dn' in dir() else 0
        log.info(
            f"[#{conn_id}] Ngắt kết nối: {client_addr} | "
            f"active={stats['active']} | "
            f"↑{stats['bytes_up']//1024}KB ↓{stats['bytes_down']//1024}KB"
        )


async def main():
    log.info("╔══════════════════════════════════════════════╗")
    log.info("║   NRO WebSocket Bridge Server v3.0 (OPT)    ║")
    log.info("╚══════════════════════════════════════════════╝")
    log.info(f"→ Game server : {GAME_HOST}:{GAME_PORT}")
    log.info(f"→ Listen port : ws://0.0.0.0:{LISTEN_PORT}")
    log.info(f"→ ping_interval: None (disabled — BridgeService không handle PONG)")
    log.info(f"→ TCP_NODELAY  : ON")
    log.info(f"→ Socket buf   : {SOCKET_BUF//1024}KB RX/TX")

    async with websockets.serve(
        handle_client,
        "0.0.0.0",
        LISTEN_PORT,
        # QUAN TRỌNG: Tắt ping! BridgeService không gửi PONG → websockets library
        # đóng kết nối sau ping_interval+ping_timeout giây → drop mỗi ~30s.
        ping_interval=None,
        ping_timeout=None,
        max_size=10 * 1024 * 1024,  # 10MB frame tối đa
        compression=None,            # không compress — giảm CPU lag
        # Tối ưu WebSocket server socket
        create_protocol=None,
    ):
        log.info(f"✅ Đang lắng nghe ws://0.0.0.0:{LISTEN_PORT}")
        await asyncio.Future()  # chạy mãi mãi


if __name__ == "__main__":
    asyncio.run(main())

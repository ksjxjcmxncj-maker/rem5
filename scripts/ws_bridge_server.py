#!/usr/bin/env python3
"""
WebSocket Bridge SERVER — chạy trên Codespace port 8080
Client gửi TCP game data qua WebSocket → bridge forward tới game server 14445

PHIÊN BẢN TỐI ƯU v4:
  - ping_interval=None  → KHÔNG gửi PING (BridgeService không handle PONG → drop sau 30s)
  - TCP_NODELAY         → giảm latency, không buffer nhỏ
  - SO_RCVBUF/SNDBUF    → buffer 512KB mỗi chiều
  - SO_KEEPALIVE        → phát hiện kết nối chết nhanh hơn
  - uvloop              → async loop nhanh hơn 2-3x (nếu có)
  - Immediate drain     → drain ngay sau mỗi packet nhỏ (< 4KB) → game packet đến server nhanh hơn
  - Larger read chunk   → 128KB thay vì 64KB để tận dụng burst
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
SOCKET_BUF = 524288   # 512KB buffer
READ_CHUNK  = 131072  # 128KB read chunk (tăng từ 64KB → tận dụng burst packets)

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

    bytes_up = 0
    bytes_dn = 0

    try:
        # Kết nối tới game server
        reader, writer = await asyncio.open_connection(GAME_HOST, GAME_PORT)

        # Tối ưu socket game: TCP_NODELAY + SO_KEEPALIVE + buffer lớn
        sock = writer.get_extra_info('socket')
        if sock:
            sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, SOCKET_BUF)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, SOCKET_BUF)
            # SO_KEEPALIVE: phát hiện kết nối chết sau ~75s thay vì timeout mặc định
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            try:
                # TCP keepalive chi tiết (Linux): 10s idle → probe 5s × 3 lần = 25s detect
                sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPIDLE, 10)
                sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPINTVL, 5)
                sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPCNT, 3)
            except (AttributeError, OSError):
                pass

        log.info(f"[#{conn_id}] Đã kết nối game server (TCP_NODELAY + SO_KEEPALIVE ON)")

        async def ws_to_tcp():
            """WebSocket → TCP (client gửi lên game server)
            
            Drain ngay sau packet nhỏ (< 4KB) để game server nhận ngay lập tức.
            Chỉ skip drain khi buffer lớn (burst) để tránh overhead.
            """
            nonlocal bytes_up
            try:
                async for data in websocket:
                    if isinstance(data, bytes):
                        writer.write(data)
                        bytes_up += len(data)
                        buf = writer.transport.get_write_buffer_size()
                        if buf < 4096:
                            # Packet nhỏ (lệnh game thông thường): drain ngay → server nhận ngay
                            await writer.drain()
                        elif buf > 65536:
                            # Buffer đầy (burst lớn): drain để tránh overflow
                            await writer.drain()
                        # Nếu 4KB < buf < 64KB: asyncio tự flush ở vòng lặp tiếp theo
            except Exception as e:
                log.debug(f"[#{conn_id}] ws→tcp end: {e}")
            finally:
                try:
                    await writer.drain()
                    writer.close()
                except Exception:
                    pass

        async def tcp_to_ws():
            """TCP → WebSocket (game server trả về client)
            
            Đọc 128KB chunk để tận dụng burst packets (vd chuyển map gửi nhiều packets liên tiếp).
            """
            nonlocal bytes_dn
            try:
                while True:
                    data = await reader.read(READ_CHUNK)
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
        stats["bytes_up"] += bytes_up
        stats["bytes_down"] += bytes_dn
        log.info(
            f"[#{conn_id}] Ngắt kết nối: {client_addr} | "
            f"active={stats['active']} | "
            f"↑{stats['bytes_up']//1024}KB ↓{stats['bytes_down']//1024}KB"
        )


async def main():
    log.info("╔══════════════════════════════════════════════╗")
    log.info("║   NRO WebSocket Bridge Server v4.0 (OPT)    ║")
    log.info("╚══════════════════════════════════════════════╝")
    log.info(f"→ Game server  : {GAME_HOST}:{GAME_PORT}")
    log.info(f"→ Listen port  : ws://0.0.0.0:{LISTEN_PORT}")
    log.info(f"→ ping_interval: None (disabled — BridgeService không handle PONG)")
    log.info(f"→ TCP_NODELAY  : ON")
    log.info(f"→ SO_KEEPALIVE : ON (10s idle, 5s×3 probe)")
    log.info(f"→ Socket buf   : {SOCKET_BUF//1024}KB RX/TX")
    log.info(f"→ Read chunk   : {READ_CHUNK//1024}KB")
    log.info(f"→ Drain mode   : immediate for <4KB packets")

    async with websockets.serve(
        handle_client,
        "0.0.0.0",
        LISTEN_PORT,
        # QUAN TRỌNG: Tắt ping! BridgeService không gửi PONG → websockets library
        # đóng kết nối sau ping_interval+ping_timeout giây → drop mỗi ~30s.
        ping_interval=None,
        ping_timeout=None,
        max_size=10 * 1024 * 1024,   # 10MB frame tối đa
        compression=None,             # không compress — giảm CPU lag
        write_limit=2 * 1024 * 1024, # 2MB WS write buffer trước khi backpressure
        read_limit=2 * 1024 * 1024,  # 2MB WS read buffer
    ):
        log.info(f"✅ Đang lắng nghe ws://0.0.0.0:{LISTEN_PORT}")
        await asyncio.Future()  # chạy mãi mãi


if __name__ == "__main__":
    asyncio.run(main())

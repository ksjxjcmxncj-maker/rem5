#!/usr/bin/env python3
"""
ws_client_proxy.py v5 — NRO Client-side Proxy
Nghe TCP localhost:14445 → forward qua WebSocket đến Codespace
Player chạy file này trên máy, rồi đăng nhập game bình thường với IP 127.0.0.1:14445
"""
import asyncio, websockets, logging, sys, os, socket

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [PROXY] %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
log = logging.getLogger(__name__)

# ══════════════════════════════════════════════════
# CẤU HÌNH — chỉnh WS_URL theo Codespace của bạn
# ══════════════════════════════════════════════════
WS_URL       = os.environ.get('NRO_WS_URL',
    'wss://improved-fishstick-966vx76qqgx7cqjp-8080.app.github.dev')
LISTEN_HOST  = os.environ.get('PROXY_HOST', '127.0.0.1')
LISTEN_PORT  = int(os.environ.get('PROXY_PORT', '14445'))
# ══════════════════════════════════════════════════

async def handle_client(reader, writer):
    peer = writer.get_extra_info('peername')
    log.info(f"[+] Game client {peer} → kết nối WS {WS_URL}")

    try:
        async with websockets.connect(
            WS_URL,
            ping_interval=20,
            ping_timeout=10,
            max_size=2**20,
            open_timeout=10,
        ) as ws:
            log.info(f"[+] WebSocket tới Codespace OK")

            async def tcp_to_ws():
                while True:
                    data = await reader.read(8192)
                    if not data:
                        break
                    await ws.send(data)

            async def ws_to_tcp():
                async for msg in ws:
                    data = msg if isinstance(msg, bytes) else msg.encode('latin1')
                    writer.write(data)
                    await writer.drain()

            done, pending = await asyncio.wait(
                [asyncio.create_task(tcp_to_ws()),
                 asyncio.create_task(ws_to_tcp())],
                return_when=asyncio.FIRST_COMPLETED
            )
            for task in pending:
                task.cancel()

    except websockets.exceptions.ConnectionClosed as e:
        log.info(f"[-] WS closed: {e.code}")
    except Exception as e:
        log.error(f"[!] Lỗi: {e}")
    finally:
        writer.close()
        try: await writer.wait_closed()
        except: pass
        log.info(f"[-] {peer} ngắt kết nối")

async def main():
    log.info("=" * 55)
    log.info("  NRO Client Proxy v5")
    log.info(f"  Nghe TCP : {LISTEN_HOST}:{LISTEN_PORT}")
    log.info(f"  Codespace: {WS_URL}")
    log.info("  Đăng nhập game: IP=127.0.0.1  Port=14445")
    log.info("=" * 55)

    server = await asyncio.start_server(handle_client, LISTEN_HOST, LISTEN_PORT)
    log.info(f"✅ Proxy sẵn sàng — chạy game và đăng nhập 127.0.0.1:{LISTEN_PORT}")

    async with server:
        await server.serve_forever()

if __name__ == '__main__':
    # Kiểm tra port chưa bị chiếm
    try:
        s = socket.socket(); s.bind((LISTEN_HOST, LISTEN_PORT)); s.close()
    except OSError:
        log.error(f"Port {LISTEN_PORT} đang bị chiếm. Tắt phần mềm khác đang dùng port này rồi chạy lại.")
        sys.exit(1)

    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        log.info("Proxy đã dừng.")

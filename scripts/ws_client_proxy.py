#!/usr/bin/env python3
"""
ws_client_proxy.py v6 — NRO Client-side Proxy
URL tu dong lay tu Replit API (khong hardcode) — dung ca khi Codespace doi.
Chay: python3 ws_client_proxy.py
"""
import asyncio, websockets, logging, sys, os, socket
import urllib.request, json as _json

logging.basicConfig(level=logging.INFO, format='%(asctime)s [PROXY] %(message)s',
                    handlers=[logging.StreamHandler(sys.stdout)])
log = logging.getLogger(__name__)

REPLIT_API  = os.environ.get('NRO_REPLIT_API',
    'https://ea51c8e9-773e-49aa-b27a-2e1cafc9b3b7-00-3i4ep1yycnhrl.pike.replit.dev')
LISTEN_HOST = os.environ.get('PROXY_HOST', '127.0.0.1')
LISTEN_PORT = int(os.environ.get('PROXY_PORT', '14445'))
FALLBACK_WS = os.environ.get('NRO_WS_FALLBACK',
    'wss://improved-fishstick-966vx76qqgx7cqjp-8080.app.github.dev')

def fetch_ws_url():
    """Lay WSS URL tu Replit API. Fallback neu loi."""
    try:
        url = f"{REPLIT_API.rstrip('/')}/api/ws-url"
        with urllib.request.urlopen(url, timeout=5) as resp:
            data = _json.loads(resp.read())
            if data.get('url'):
                log.info(f"[API] WSS: {data['url']} (updated: {data.get('updatedAt','')})")
                return data['url']
    except Exception as e:
        log.warning(f"[API] Khong lay duoc URL tu Replit API ({e}), dung fallback")
    log.warning(f"[API] Fallback: {FALLBACK_WS}")
    return FALLBACK_WS

async def handle_client(reader, writer, ws_url):
    peer = writer.get_extra_info('peername')
    log.info(f"[+] {peer} -> {ws_url}")
    try:
        async with websockets.connect(ws_url, ping_interval=20, ping_timeout=10,
                                      max_size=2**20, open_timeout=10) as ws:
            async def tcp_to_ws():
                while True:
                    data = await reader.read(8192)
                    if not data: break
                    await ws.send(data)
            async def ws_to_tcp():
                async for msg in ws:
                    data = msg if isinstance(msg, bytes) else msg.encode('latin1')
                    writer.write(data); await writer.drain()
            done, pending = await asyncio.wait(
                [asyncio.create_task(tcp_to_ws()), asyncio.create_task(ws_to_tcp())],
                return_when=asyncio.FIRST_COMPLETED)
            for t in pending: t.cancel()
    except websockets.exceptions.ConnectionClosed as e:
        log.info(f"[-] WS closed: {e.code}")
    except Exception as e:
        log.error(f"[!] {e}")
    finally:
        writer.close()
        try: await writer.wait_closed()
        except: pass
        log.info(f"[-] {peer} ngat")

async def main():
    ws_url = fetch_ws_url()
    log.info("=" * 60)
    log.info("  NRO Client Proxy v6")
    log.info(f"  TCP  : {LISTEN_HOST}:{LISTEN_PORT}")
    log.info(f"  WSS  : {ws_url}")
    log.info(f"  API  : {REPLIT_API}")
    log.info("  Dang nhap game: IP=127.0.0.1  Port=14445")
    log.info("=" * 60)
    server = await asyncio.start_server(
        lambda r, w: handle_client(r, w, ws_url), LISTEN_HOST, LISTEN_PORT)
    log.info(f"Proxy san sang — dang nhap 127.0.0.1:{LISTEN_PORT}")
    async with server:
        await server.serve_forever()

if __name__ == '__main__':
    try:
        s = socket.socket(); s.bind((LISTEN_HOST, LISTEN_PORT)); s.close()
    except OSError:
        log.error(f"Port {LISTEN_PORT} bi chiem. Tat phan mem khac roi chay lai.")
        sys.exit(1)
    try: asyncio.run(main())
    except KeyboardInterrupt: log.info("Proxy dung.")

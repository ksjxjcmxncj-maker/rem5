# NRO Server Info (WebSocket Cloud / Codespace)

## Ket noi cho Player
**Khong can frpc/bore/playit.** Dung WebSocket bridge qua cloud Codespace.

### Cach ket noi:
1. Cai Python 3 + websockets: `pip install websockets`
2. Tai script: `scripts/ws_client_proxy.py`
3. Chay: `python3 ws_client_proxy.py`
   - Script **tu dong lay WSS URL** tu Replit API — khong can cap nhat tay
4. Dang nhap game: IP = `127.0.0.1`, Port = `14445`

### Lay URL thu cong (debug):
```
curl https://ea51c8e9-773e-49aa-b27a-2e1cafc9b3b7-00-3i4ep1yycnhrl.pike.replit.dev/api/ws-url
```

---

## Ha tang Codespace
| Thanh phan | Gia tri |
|-----------|---------|
| Main Codespace | `improved-fishstick-966vx76qqgx7cqjp` |
| Backup Codespace | `vigilant-system-r79x75p7j5p2q55` |
| JAR game | `~/nro/SRC/SrcTeam.jar` (port 14445) |
| JAR login | `~/nro/SRC/Login.jar` (port 8888) |
| Database | MariaDB `nro1`, user `root`, pass trong |
| WS bridge | `~/bin/ws_bridge.py` v5, port 8080 |
| Tunnel | cloudflare trycloudflare.com (URL dong) |

## Tu dong hoa (2 lop bao ve)
| Co che | Mo ta | Phan ung |
|--------|-------|----------|
| Watchdog loop (lop 1) | Vong lap 2 phut trong start.sh | < 2 phut |
| GitHub Actions (lop 2) | `*/5 * * * *`, SSH keepalive_check.sh | < 5 phut |
| quick_check.sh | Check ca 5 service (v2) | Triggered boi watchdog |
| syncReplitUrl | Doc `.replit-url` khi Codespace wake | Tu dong |

## Khoi dong thu cong tren Codespace
```bash
bash /workspaces/rem5/start.sh
```

## Rebuild Codespace (luu y)
1. `postCreateCommand` chay `.devcontainer/setup.sh` (v2, Debian-compatible)
2. setup.sh: cai deps bang `apt-get` (KHONG dung `apk`), copy `ws_bridge.py` v5
3. `postStartCommand` chay `start.sh` tu dong

## DB Accounts
- Player: `a` / pass nao cung duoc -> nhan vat `admin`
- Admin: `admin` / `12345678` -> nhan vat `memeiue`

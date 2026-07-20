# NRO Server Info (No-TCP / WebSocket Cloud)

## Kết nối cho Player
**Không cần frpc/bore/playit nữa.** Dùng WebSocket bridge qua cloud Codespace.

### Cách kết nối:
1. Cài Python 3 + websockets: `pip install websockets`
2. Tải script: `scripts/ws_client_proxy.py`
3. Chạy: `python3 ws_client_proxy.py`
4. Đăng nhập game: IP = `127.0.0.1`, Port = `14445`

### WebSocket URL (server side):
```
wss://improved-fishstick-966vx76qqgx7cqjp-8080.app.github.dev
```
*(URL cố định — Codespace improved-fishstick)*

---

## Hạ tầng Codespace
- **Main Codespace**: `improved-fishstick-966vx76qqgx7cqjp` (4-core, 16GB, Java 25)
- **JAR**: `/home/codespace/nro/SRC/SrcTeam.jar` (alias: `NgocRongOnline.jar`)
- **DB**: MariaDB local, database `nro1`, user `root`, pass rỗng
- **Port game**: `14445` (localhost only)
- **Port WS**: `8080` (public via Codespace cloud)
- **Keep-alive**: `scripts/keepalive_codespace.sh` (Replit → SSH → Codespace)
- **Logs**: `~/logs/server.log`, `~/logs/ws_bridge.log`

## Khởi động thủ công trên Codespace
```bash
# ws_bridge
pip install websockets -q
curl -sf "https://raw.githubusercontent.com/ksjxjcmxncj-maker/rem5/main/scripts/ws_bridge.py" -o ~/bin/ws_bridge.py
nohup python3 ~/bin/ws_bridge.py > ~/logs/ws_bridge.log 2>&1 &

# Set port public
gh codespace ports visibility 8080:public -c improved-fishstick-966vx76qqgx7cqjp

# Game server
cd ~/nro/SRC
nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
  -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
  -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
  -jar SrcTeam.jar >> ~/logs/server.log 2>&1 &
```

## DB Accounts
- Player: `a` → nhân vật `admin`
- Admin: `admin` / `12345678` → nhân vật `memeiue`

## Fix đã compile vào JAR
Xem: `.agents/memory/nro-server-codespaces.md`

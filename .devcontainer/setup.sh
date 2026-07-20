#!/bin/bash
# NRO Server Setup for GitHub Codespaces
# Architecture: APK → WSS (Codespace port 8080) → ws_bridge → TCP:14445 (Java)
set -e
LOG=~/logs
mkdir -p $LOG ~/nro/SRC

echo "========================================="
echo "  NRO Server Setup (Codespace Network)"
echo "========================================="

echo "[1] Khởi động MariaDB..."
sudo service mariadb start 2>/dev/null || sudo mysqld_safe --datadir=/var/lib/mysql &
sleep 4
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY ''; FLUSH PRIVILEGES;" 2>/dev/null || true

echo "[2] Sync server files..."
REPO_DIR="${CODESPACE_REPO:-/workspaces/rem5}"
[ ! -d "$REPO_DIR" ] && REPO_DIR="$HOME/workspace/rem5"
cd "$REPO_DIR" 2>/dev/null && git pull --quiet 2>/dev/null || true
cp -f  "$REPO_DIR/server/SrcTeam.jar"       ~/nro/SRC/SrcTeam.jar        2>/dev/null || true
cp -f  "$REPO_DIR/server/_Login/Login.jar"  ~/nro/SRC/Login.jar          2>/dev/null || true
cp -rf "$REPO_DIR/server/resources"         ~/nro/SRC/resources          2>/dev/null || true
cp -f  "$REPO_DIR/server/Config.properties" ~/nro/SRC/Config.properties  2>/dev/null || true
echo "  SrcTeam.jar: $(ls -lh ~/nro/SRC/SrcTeam.jar 2>/dev/null | awk '{print $5}')"

echo "[3] Import database nro1..."
sudo mysql -e "CREATE DATABASE IF NOT EXISTS nro1 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
TABLES=$(sudo mysql nro1 -se "SHOW TABLES;" 2>/dev/null | wc -l)
if [ "$TABLES" -lt 5 ]; then
  sudo mysql nro1 < "$REPO_DIR/database/srcteam_nro.sql" 2>/dev/null \
    && echo "  ✅ DB imported" || echo "  ⚠️ DB import lỗi"
else
  echo "  ✅ DB sẵn sàng ($TABLES tables)"
fi

echo "[4] Cấu hình Config.properties..."
CFG=~/nro/SRC/Config.properties
CS_NAME="${CODESPACE_NAME:-localhost}"
WS_HOST="${CS_NAME}-8080.app.github.dev"
sed -i "s|server.sv1=.*|server.sv1=NRO:${WS_HOST}:443:0,0,0|" "$CFG"
sed -i "s|server.local=.*|server.local=false|"     "$CFG"
sed -i "s|database.host=.*|database.host=localhost|" "$CFG"
sed -i "s|database.name=.*|database.name=nro1|"    "$CFG"
sed -i "s|database.user=.*|database.user=root|"    "$CFG"
sed -i "s|database.pass=.*|database.pass=|"         "$CFG"
echo "  sv1: $(grep 'server.sv1' $CFG)"

echo "[5] WebSocket bridge (0.0.0.0:8080 → 127.0.0.1:14445)..."
mkdir -p ~/bin
cat > ~/bin/ws_bridge.py << 'PYEOF'
#!/usr/bin/env python3
"""WebSocket <-> TCP bridge
APK connects: wss://{CODESPACE_NAME}-8080.app.github.dev
Bridge forwards to Java NRO on TCP 127.0.0.1:14445
"""
import asyncio, websockets, logging, os
logging.basicConfig(level=logging.INFO, format='%(asctime)s %(message)s')

TARGET_HOST, TARGET_PORT = '127.0.0.1', 14445
LISTEN_PORT = 8080

async def handle(ws):
    peer = ws.remote_address
    logging.info(f"Client: {peer}")
    try:
        reader, writer = await asyncio.open_connection(TARGET_HOST, TARGET_PORT)
        async def ws_to_tcp():
            async for msg in ws:
                writer.write(msg if isinstance(msg, bytes) else msg.encode())
                await writer.drain()
        async def tcp_to_ws():
            while True:
                data = await reader.read(4096)
                if not data: break
                await ws.send(data)
        done, pending = await asyncio.wait(
            [asyncio.create_task(ws_to_tcp()), asyncio.create_task(tcp_to_ws())],
            return_when=asyncio.FIRST_COMPLETED)
        for t in pending: t.cancel()
    except Exception as e:
        logging.info(f"Closed [{peer}]: {e}")
    finally:
        try: writer.close()
        except: pass

async def main():
    cs = os.environ.get("CODESPACE_NAME", "localhost")
    logging.info(f"Bridge: wss://{cs}-{LISTEN_PORT}.app.github.dev → TCP:{TARGET_PORT}")
    async with websockets.serve(handle, '0.0.0.0', LISTEN_PORT,
                                ping_interval=20, ping_timeout=60):
        await asyncio.Future()

asyncio.run(main())
PYEOF

pkill -f ws_bridge 2>/dev/null; sleep 1
pip install websockets -q 2>/dev/null || true
nohup python3 ~/bin/ws_bridge.py > $LOG/ws_bridge.log 2>&1 &
sleep 2
pgrep -f ws_bridge >/dev/null && echo "  ✅ ws_bridge running (port 8080)" || { echo "  ❌ ws_bridge failed"; cat $LOG/ws_bridge.log; }

echo "[6] Set port 8080 public..."
gh codespace ports visibility 8080:public -c "$CODESPACE_NAME" 2>/dev/null \
  && echo "  ✅ Port 8080 public" || echo "  ⚠️ Sẽ set sau"

echo "[7] Khởi động Java servers..."
pkill -f "SrcTeam|Login" 2>/dev/null; sleep 2
cd ~/nro/SRC

if [ -f Login.jar ]; then
  nohup java -Xms64m -Xmx256m -jar Login.jar > $LOG/login.log 2>&1 &
  echo "  Login PID: $!"; sleep 3
fi

nohup java -Xms256m -Xmx1g \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=50 \
  -XX:+ParallelRefProcEnabled \
  -XX:+DisableExplicitGC \
  -Djava.net.preferIPv4Stack=true \
  -jar SrcTeam.jar > $LOG/server.log 2>&1 &
GAME_PID=$!
echo "  Game PID: $GAME_PID"; sleep 12

pgrep -f SrcTeam >/dev/null && echo "  ✅ Game server RUNNING" || { echo "  ❌ Game server FAILED"; tail -20 $LOG/server.log; }

CS_NAME="${CODESPACE_NAME:-localhost}"
WS_URL="wss://${CS_NAME}-8080.app.github.dev"
echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "  ✅  NRO SERVER ONLINE — Codespace Network"
echo "  🌐 WS   : $WS_URL"
echo "  📱 APK  : ${CS_NAME}-8080.app.github.dev:443"
echo "  🔧 Bridge: 8080 (public WSS) → 14445 (Java TCP)"
echo "╚══════════════════════════════════════════════════════╝"
echo "$WS_URL" > /tmp/server_ws.txt
echo "${CS_NAME}-8080.app.github.dev:443" > /tmp/server_addr.txt
tail -f $LOG/server.log

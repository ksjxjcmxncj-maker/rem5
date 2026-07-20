#!/bin/bash
# NRO Server Setup — Alpine Linux / GitHub Codespaces
# Flow: APK → WSS Codespace port 8080 → ws_bridge.py → TCP 14445 (Java)
LOG=~/logs
mkdir -p "$LOG" ~/nro/SRC

echo "========================================="
echo "  NRO Server Setup (Codespace Network)"
echo "========================================="

# ── 1. Phụ thuộc ─────────────────────────────
echo "[1] Kiểm tra phụ thuộc..."
command -v java    >/dev/null 2>&1 || { sudo apk update -q; sudo apk add -q openjdk17-jre mariadb mariadb-client; }
python3 -c "import websockets" 2>/dev/null || sudo apk add -q py3-websockets 2>/dev/null || true
echo "  java: $(java -version 2>&1 | head -1)"

# ── 2. MariaDB (Alpine: sudo mariadb + mysqld_safe) ──────
echo "[2] MariaDB..."
if ! sudo mysqladmin ping 2>/dev/null; then
  sudo mysql_install_db --user=mysql --datadir=/var/lib/mysql --skip-test-db >/dev/null 2>&1 || true
  sudo mysqld_safe --user=mysql --datadir=/var/lib/mysql >/dev/null 2>&1 &
  sleep 6
fi
sudo mysqladmin ping 2>/dev/null && echo "  ✅ MariaDB OK" || echo "  ❌ MariaDB FAIL"

# ── 3. Sync files ─────────────────────────────
echo "[3] Sync files..."
REPO_DIR=/workspaces/rem5
cd "$REPO_DIR" && git pull --quiet 2>/dev/null || true
cp -f  "$REPO_DIR/server/SrcTeam.jar"       ~/nro/SRC/SrcTeam.jar       || true
cp -f  "$REPO_DIR/server/_Login/Login.jar"  ~/nro/SRC/Login.jar         || true
cp -rf "$REPO_DIR/server/resources"         ~/nro/SRC/resources         || true
cp -f  "$REPO_DIR/server/Config.properties" ~/nro/SRC/Config.properties || true
echo "  SrcTeam.jar: $(ls -lh ~/nro/SRC/SrcTeam.jar 2>/dev/null | awk '{print $5}')"

# ── 4. Database ───────────────────────────────
echo "[4] Database nro1..."
sudo mariadb -e "CREATE DATABASE IF NOT EXISTS nro1 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
TABLES=$(sudo mariadb nro1 -se "SHOW TABLES;" 2>/dev/null | wc -l)
if [ "$TABLES" -lt 5 ]; then
  sudo mariadb nro1 < "$REPO_DIR/database/srcteam_nro.sql" 2>/dev/null \
    && echo "  ✅ DB imported" || echo "  ⚠️ DB import lỗi"
else
  echo "  ✅ DB sẵn sàng ($TABLES tables)"
fi

# ── 5. Patch config ───────────────────────────
echo "[5] Config..."
CS_NAME="${CODESPACE_NAME:-localhost}"
WS_HOST="${CS_NAME}-8080.app.github.dev"
CFG=~/nro/SRC/Config.properties
SPROP=~/nro/SRC/resources/config/server.properties
sed -i "s|server.sv1=.*|server.sv1=NRO:${WS_HOST}:443:0,0,0|" "$CFG"
sed -i "s|server.local=.*|server.local=false|"      "$CFG"
sed -i "s|database.host=.*|database.host=localhost|" "$CFG"
sed -i "s|database.name=.*|database.name=nro1|"     "$CFG"
sed -i "s|database.user=.*|database.user=root|"     "$CFG"
sed -i "s|database.pass=.*|database.pass=|"          "$CFG"
sed -i "s|server.sv1=.*|server.sv1=NRO:${WS_HOST}:443:0,0,0|" "$SPROP" 2>/dev/null || true
sed -i "s|server.db.name=.*|server.db.name=nro1|"              "$SPROP" 2>/dev/null || true
sed -i "s|server.db.ip=.*|server.db.ip=localhost|"             "$SPROP" 2>/dev/null || true
sed -i "s|server.db.pw=.*|server.db.pw=|"                       "$SPROP" 2>/dev/null || true
echo "  sv1: $(grep 'server.sv1' "$CFG")"

# ── 6. WebSocket bridge ──────────────────────
echo "[6] WebSocket bridge (0.0.0.0:8080 → 127.0.0.1:14445)..."
mkdir -p ~/bin
cat > ~/bin/ws_bridge.py << 'PYEOF'
#!/usr/bin/env python3
"""WebSocket <-> TCP bridge
APK → wss://{CODESPACE_NAME}-8080.app.github.dev (Codespace public port)
Bridge → TCP 127.0.0.1:14445 (Java NRO server, local)
"""
import asyncio, websockets, logging, os
logging.basicConfig(level=logging.INFO, format='%(asctime)s %(message)s')
TARGET_HOST, TARGET_PORT, LISTEN_PORT = '127.0.0.1', 14445, 8080

async def handle(ws):
    peer = ws.remote_address
    logging.info(f"[+] {peer}")
    reader = writer = None
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
        _, pending = await asyncio.wait(
            [asyncio.create_task(ws_to_tcp()), asyncio.create_task(tcp_to_ws())],
            return_when=asyncio.FIRST_COMPLETED)
        for t in pending: t.cancel()
    except Exception as e:
        logging.info(f"[-] {peer}: {e}")
    finally:
        if writer:
            writer.close()
            try: await writer.wait_closed()
            except: pass

async def main():
    cs = os.environ.get('CODESPACE_NAME', 'localhost')
    logging.info(f"Bridge ready  wss://{cs}-{LISTEN_PORT}.app.github.dev → TCP:{TARGET_PORT}")
    async with websockets.serve(handle, '0.0.0.0', LISTEN_PORT,
                                ping_interval=20, ping_timeout=60):
        await asyncio.Future()

asyncio.run(main())
PYEOF

pkill -f ws_bridge.py 2>/dev/null; sleep 1
nohup python3 ~/bin/ws_bridge.py > "$LOG/ws_bridge.log" 2>&1 &
sleep 2
pgrep -f ws_bridge.py > /dev/null \
  && echo "  ✅ ws_bridge running (port 8080)" \
  || { echo "  ❌ ws_bridge failed"; cat "$LOG/ws_bridge.log"; }

# ── 7. Port 8080 public ──────────────────────
echo "[7] Port 8080 public..."
gh codespace ports visibility 8080:public -c "$CODESPACE_NAME" 2>/dev/null \
  && echo "  ✅ Done" || echo "  ⚠️ Sẽ retry sau"

# ── 8. Java servers ───────────────────────────
echo "[8] Java servers..."
pkill -f "SrcTeam.jar" 2>/dev/null
pkill -f "Login.jar"   2>/dev/null
sleep 2
cd ~/nro/SRC
if [ -f Login.jar ]; then
  nohup java -Xms64m -Xmx256m -jar Login.jar > "$LOG/login.log" 2>&1 &
  echo "  Login PID: $!"
  sleep 4
fi
nohup java -Xms256m -Xmx1g \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=50 \
  -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC \
  -Djava.net.preferIPv4Stack=true \
  -jar SrcTeam.jar > "$LOG/server.log" 2>&1 &
GAME_PID=$!
echo "  Game PID: $GAME_PID"
sleep 12
pgrep -f SrcTeam.jar > /dev/null \
  && echo "  ✅ Game server RUNNING" \
  || { echo "  ❌ Game server FAILED"; tail -20 "$LOG/server.log"; }

# ── 9. Done ───────────────────────────────────
WS_URL="wss://${CS_NAME}-8080.app.github.dev"
echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "  ✅  NRO SERVER ONLINE"
echo "  🌐 WSS  : $WS_URL"
echo "  📱 APK  : ${CS_NAME}-8080.app.github.dev:443"
echo "╚══════════════════════════════════════════════════════╝"
echo "$WS_URL"                           > /tmp/server_ws.txt
echo "${CS_NAME}-8080.app.github.dev:443" > /tmp/server_addr.txt

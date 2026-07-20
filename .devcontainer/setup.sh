#!/bin/bash
# NRO SRC-Team Server Setup for GitHub Codespaces
set -e
LOG=~/logs
mkdir -p $LOG ~/nro/SRC

echo "========================================="
echo "  NRO SRC-Team Server Setup"
echo "========================================="

# ── 1. MariaDB ────────────────────────────────
echo "[1] Khởi động MariaDB..."
sudo service mariadb start 2>/dev/null || sudo mysqld_safe --datadir=/var/lib/mysql &
sleep 4
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY ''; FLUSH PRIVILEGES;" 2>/dev/null || true

# ── 2. Sync files từ repo vào ~/nro/SRC ──────
echo "[2] Sync server files từ repo..."
REPO="${CODESPACE_REPO:-/workspaces/rem5}"
[ ! -d "$REPO" ] && REPO="$HOME/workspace/rem5"  # fallback
cd $REPO 2>/dev/null && git pull --quiet 2>/dev/null || true

cp -f $REPO/server/SrcTeam.jar         ~/nro/SRC/SrcTeam.jar        2>/dev/null || true
cp -f $REPO/server/_Login/Login.jar    ~/nro/SRC/Login.jar           2>/dev/null || true
cp -rf $REPO/server/resources          ~/nro/SRC/resources           2>/dev/null || true
cp -rf $REPO/server/src                ~/nro/SRC/src                 2>/dev/null || true

echo "  JAR: $(ls -lh ~/nro/SRC/SrcTeam.jar 2>/dev/null | awk '{print $5}')"

# ── 3. Import database ────────────────────────
echo "[3] Setup database 'nro1'..."
sudo mysql -e "CREATE DATABASE IF NOT EXISTS `nro1` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
TABLES=$(sudo mysql nro1 -se "SHOW TABLES;" 2>/dev/null | wc -l)
if [ "$TABLES" -lt 5 ]; then
  echo "  Import srcteam_nro.sql..."
  sudo mysql nro1 < $REPO/database/srcteam_nro.sql 2>/dev/null && echo "  ✅ DB imported" || echo "  ⚠️ DB import lỗi"
else
  echo "  ✅ DB đã có $TABLES tables"
fi

# ── 4. Cập nhật server.properties ────────────
echo "[4] Cấu hình server..."
CFG=~/nro/SRC/resources/config/server.properties
# Tìm IP public qua frpc hoặc ws
if pgrep -f frpc >/dev/null 2>&1; then
  FRP_IP=$(grep serverAddr /tmp/frpc_nro.toml 2>/dev/null | awk -F'"' '{print $2}')
  FRP_PORT=$(grep remotePort /tmp/frpc_nro.toml 2>/dev/null | head -1 | awk '{print $3}')
  [ -n "$FRP_IP" ] && [ -n "$FRP_PORT" ] && \
    sed -i "s|server.sv1=.*|server.sv1=NRO:$FRP_IP:$FRP_PORT:0,0,0|" "$CFG"
fi
sed -i "s|server.db.ip=.*|server.db.ip=localhost|" "$CFG"
sed -i "s|server.db.pw=.*|server.db.pw=|" "$CFG"
sed -i "s|server.db.name=.*|server.db.name=nro1|" "$CFG"
echo "  sv1: $(grep 'server.sv1' $CFG)"

# ── 5. Khởi động frpc tunnel ─────────────────
echo "[5] Khởi động frpc tunnel (frp.freefrp.net:21445)..."
pkill -f frpc 2>/dev/null; sleep 1
if [ ! -f /tmp/frp/frpc ]; then
  mkdir -p /tmp/frp
  curl -sL "https://github.com/fatedier/frp/releases/download/v0.61.0/frp_0.61.0_linux_amd64.tar.gz" \
    | tar -xz --strip-components=1 -C /tmp/frp/ 2>/dev/null || true
fi
cat > /tmp/frpc_nro.toml << 'EOF'
serverAddr = "frp.freefrp.net"
serverPort = 7000
auth.method = "token"
auth.token = "freefrp.net"

[[proxies]]
name = "nro-game"
type = "tcp"
localIP = "127.0.0.1"
localPort = 14445
remotePort = 21445
EOF
nohup /tmp/frp/frpc -c /tmp/frpc_nro.toml > $LOG/frp.log 2>&1 &
sleep 3
pgrep -f frpc >/dev/null && echo "  ✅ frpc running → frp.freefrp.net:21445" || echo "  ⚠️ frpc failed"

# ── 6. WebSocket bridge ───────────────────────
echo "[6] WebSocket bridge..."
WS_SCRIPT=~/bin/ws_bridge.py
mkdir -p ~/bin
if [ ! -f "$WS_SCRIPT" ]; then
cat > "$WS_SCRIPT" << 'PYEOF'
#!/usr/bin/env python3
"""WebSocket ↔ TCP bridge: port 8080 (WS) → port 14445 (Java NRO)"""
import asyncio, websockets, logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s %(message)s')
TARGET_HOST, TARGET_PORT = '127.0.0.1', 14445

async def handle(ws):
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
        await asyncio.gather(ws_to_tcp(), tcp_to_ws())
    except Exception as e:
        logging.info(f"Bridge closed: {e}")
    finally:
        try: writer.close()
        except: pass

async def main():
    async with websockets.serve(handle, '0.0.0.0', 8080):
        logging.info("WS bridge ready: 0.0.0.0:8080 → 127.0.0.1:14445")
        await asyncio.Future()

asyncio.run(main())
PYEOF
fi
pkill -f ws_bridge 2>/dev/null; sleep 1
pip install websockets -q 2>/dev/null || true
nohup python3 "$WS_SCRIPT" > $LOG/ws_bridge.log 2>&1 &
sleep 2
pgrep -f ws_bridge >/dev/null && echo "  ✅ ws_bridge running (port 8080)" || echo "  ⚠️ ws_bridge failed"

# ── 7. Khởi động Java servers ─────────────────
echo "[7] Khởi động Java servers..."
pkill -f "SrcTeam\|NgocRongOnline\|ServerManager" 2>/dev/null; sleep 2

# Login server (port 8282 mặc định)
if [ -f ~/nro/SRC/Login.jar ]; then
  cd ~/nro/SRC
  nohup java -Xms64m -Xmx256m -jar Login.jar > $LOG/login.log 2>&1 &
  echo "  Login PID: $!"
  sleep 3
fi

# Game server
cd ~/nro/SRC
nohup java -Xms256m -Xmx1g \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=50 \
  -XX:G1HeapRegionSize=4m \
  -XX:+ParallelRefProcEnabled \
  -XX:+DisableExplicitGC \
  -Djava.net.preferIPv4Stack=true \
  -jar SrcTeam.jar > $LOG/server.log 2>&1 &
GAME_PID=$!
echo "  Game PID: $GAME_PID"
sleep 12

# Check
if pgrep -f SrcTeam >/dev/null 2>&1; then
  echo "  ✅ Game server RUNNING"
else
  echo "  ❌ Game server FAILED — xem $LOG/server.log"
  tail -20 $LOG/server.log
fi

# ── 8. Set port 8080 public ───────────────────
echo "[8] Set port 8080 public..."
gh codespace ports visibility 8080:public -c "$CODESPACE_NAME" 2>/dev/null \
  && echo "  ✅ Port 8080 public" || true

CS_NAME=${CODESPACE_NAME:-$(hostname)}
WS_URL="wss://${CS_NAME}-8080.app.github.dev"
FRP_IP=$(grep serverAddr /tmp/frpc_nro.toml 2>/dev/null | awk -F'"' '{print $2}' | head -1)
FRP_PORT=21445

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "  ✅  NRO SRC-TEAM SERVER ONLINE"
echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  🎮 TCP  : $FRP_IP : $FRP_PORT"
echo "  🌐 WS   : $WS_URL"
echo "  🔧 API  : http://localhost:8181"
echo "╚══════════════════════════════════════════════════════╝"

echo "$FRP_IP:$FRP_PORT" > /tmp/server_addr.txt
echo "$WS_URL"           > /tmp/server_ws.txt

tail -f $LOG/server.log

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
# Không cần cập nhật sv1 — dùng WebSocket cloud thay TCP tunnel
sed -i "s|server.db.ip=.*|server.db.ip=localhost|" "$CFG"
sed -i "s|server.db.pw=.*|server.db.pw=|" "$CFG"
sed -i "s|server.db.name=.*|server.db.name=nro1|" "$CFG"
echo "  db name: nro1, WebSocket cloud enabled"

# ── 5+6. WebSocket bridge (thay thế TCP tunnel) ──────────
echo "[5] WebSocket bridge (cloud connection)..."
mkdir -p ~/bin
# Luôn lấy ws_bridge.py mới nhất từ repo
curl -sf "https://raw.githubusercontent.com/ksjxjcmxncj-maker/rem5/main/scripts/ws_bridge.py" \
  -o ~/bin/ws_bridge.py 2>/dev/null || true
pkill -f ws_bridge 2>/dev/null; sleep 1
pip install websockets -q 2>/dev/null || true
nohup python3 ~/bin/ws_bridge.py > $LOG/ws_bridge.log 2>&1 &
sleep 2
pgrep -f ws_bridge >/dev/null && echo "  ✅ ws_bridge running (port 8080)" || echo "  ⚠️ ws_bridge failed"

# ── 7. Khởi động Java servers ─────────────────
echo "[6] Khởi động Java servers..."
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
echo "[7] Set port 8080 public..."
gh codespace ports visibility 8080:public -c "$CODESPACE_NAME" 2>/dev/null \
  && echo "  ✅ Port 8080 public" || true

CS_NAME=${CODESPACE_NAME:-$(hostname)}
WS_URL="wss://${CS_NAME}-8080.app.github.dev"
echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "  ✅  NRO SRC-TEAM SERVER ONLINE"
echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  🌐 WS   : $WS_URL"
echo "  📱 Client proxy: scripts/ws_client_proxy.py"
echo "  🔧 API  : http://localhost:8181"
echo "╚══════════════════════════════════════════════════════╝"

echo "$WS_URL" > /tmp/server_ws.txt

tail -f $LOG/server.log

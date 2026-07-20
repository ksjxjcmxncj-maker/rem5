#!/bin/bash
# NRO Server Start — Codespace Network (no frp/ngrok)
LOG=~/logs
mkdir -p $LOG

echo "[1] MariaDB..."
sudo service mariadb start 2>/dev/null || true
sleep 3
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY ''; FLUSH PRIVILEGES;" 2>/dev/null || true

echo "[2] WebSocket bridge (8080 → 14445)..."
pkill -f ws_bridge 2>/dev/null; sleep 1
if [ -f ~/bin/ws_bridge.py ]; then
  pip install websockets -q 2>/dev/null || true
  nohup python3 ~/bin/ws_bridge.py > $LOG/ws_bridge.log 2>&1 &
  sleep 2
  pgrep -f ws_bridge && echo "  ✅ ws_bridge up" || echo "  ❌ failed"
else
  echo "  ⚠️ Chưa có ws_bridge.py — chạy postCreateCommand trước"
fi

echo "[3] Set port 8080 public..."
gh codespace ports visibility 8080:public -c "$CODESPACE_NAME" 2>/dev/null || true

echo "[4] Java servers..."
pkill -f "SrcTeam|Login" 2>/dev/null; sleep 2
cd ~/nro/SRC
[ -f Login.jar ] && { nohup java -Xms64m -Xmx256m -jar Login.jar > $LOG/login.log 2>&1 & echo "  Login PID: $!"; sleep 3; }
nohup java -Xms256m -Xmx1g \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=50 \
  -XX:+DisableExplicitGC \
  -Djava.net.preferIPv4Stack=true \
  -jar SrcTeam.jar > $LOG/server.log 2>&1 &
echo "  Game PID: $!"
sleep 8

CS_NAME="${CODESPACE_NAME:-localhost}"
WS_URL="wss://${CS_NAME}-8080.app.github.dev"
echo ""
echo "╔══════════════════════════════════╗"
echo "  ✅ NRO STARTED"
echo "  🌐 $WS_URL"
echo "  📱 ${CS_NAME}-8080.app.github.dev:443"
echo "╚══════════════════════════════════╝"
echo "$WS_URL" > /tmp/server_ws.txt
echo "${CS_NAME}-8080.app.github.dev:443" > /tmp/server_addr.txt

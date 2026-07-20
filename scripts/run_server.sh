#!/bin/bash
# Chạy trực tiếp trong Codespace — không phụ thuộc SSH timeout
CS_NAME="vigilant-system-r79x75p7j5p2q55"
LOG=~/logs
mkdir -p "$LOG"

echo "[ws_bridge]"
pkill -f ws_bridge.py 2>/dev/null; sleep 1
nohup python3 ~/bin/ws_bridge.py > "$LOG/ws_bridge.log" 2>&1 &
sleep 2
pgrep -f ws_bridge.py > /dev/null && echo "  OK" || { echo "  FAIL:"; cat "$LOG/ws_bridge.log"; }

echo "[Java servers]"
pkill -f "SrcTeam.jar" 2>/dev/null
pkill -f "Login.jar"   2>/dev/null
sleep 1
cd ~/nro/SRC
nohup java -Xms64m -Xmx256m -jar Login.jar > "$LOG/login.log" 2>&1 &
echo "  Login: $!"
sleep 4
nohup java -Xms256m -Xmx1g \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=50 \
  -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC \
  -Djava.net.preferIPv4Stack=true \
  -jar SrcTeam.jar > "$LOG/server.log" 2>&1 &
echo "  Game: $!"
sleep 15

pgrep -f SrcTeam.jar > /dev/null && GSTATUS="RUNNING" || GSTATUS="FAILED"
pgrep -f Login.jar   > /dev/null && LSTATUS="RUNNING" || LSTATUS="FAILED"
echo "[done] Login=$LSTATUS Game=$GSTATUS"
[ "$GSTATUS" = "FAILED" ] && tail -30 "$LOG/server.log"

WS_URL="wss://${CS_NAME}-8080.app.github.dev"
echo "$WS_URL"                             > /tmp/server_ws.txt
echo "${CS_NAME}-8080.app.github.dev:443"  > /tmp/server_addr.txt

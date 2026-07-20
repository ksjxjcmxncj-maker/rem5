#!/bin/bash
# NRO Keepalive Check — chạy trên Codespace qua GitHub Actions
# Usage: bash keepalive_check.sh
LOG=~/logs
mkdir -p "$LOG"
FIXED=0

chk() { pgrep -f "$1" > /dev/null 2>&1 && { echo "OK  $2"; return 0; }; echo "DEAD $2"; return 1; }

# MariaDB
chk mariadbd "MariaDB" || {
  sudo mariadbd --user=mysql --datadir=/var/lib/mysql \
    --socket=/run/mysqld/mysqld.sock \
    --pid-file=/run/mysqld/mariadbd.pid 2>/dev/null &
  sleep 6; FIXED=1
}

# ws_bridge
chk ws_bridge.py "ws_bridge" || {
  nohup python3 ~/bin/ws_bridge.py >> "$LOG/ws_bridge.log" 2>&1 &
  sleep 2; FIXED=1
}

# cloudflared
chk cloudflared "cloudflared" || {
  > "$LOG/cloudflared.log"
  nohup /usr/local/bin/cloudflared tunnel \
    --url http://localhost:8080 --no-autoupdate \
    >> "$LOG/cloudflared.log" 2>&1 &
  sleep 25
  CF=$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' \
    "$LOG/cloudflared.log" 2>/dev/null | tail -1)
  if [ -n "$CF" ]; then
    WSS="${CF/https:/wss:}"
    echo "$WSS" > /tmp/server_addr.txt
    echo "  CF: $CF"
    source ~/.nro_config 2>/dev/null || true
    [ -n "${REPLIT_API:-}" ] && curl -s -X POST "$REPLIT_API/api/ws-url" \
      -H "Content-Type: application/json" \
      -H "x-update-secret: ${SESSION_SECRET:-}" \
      -d "{\"url\":\"$WSS\"}" && echo "  Replit API updated" || true
  fi
  FIXED=1
}

# server.ini
[ -f ~/nro/SRC/server.ini ] || {
  printf 'server.port=8888\ndb.driver=com.mysql.cj.jdbc.Driver\ndb.host=localhost\ndb.port=3306\ndb.name=nro1\ndb.user=root\ndb.password=\nadmin.mode=0\nwait.login=3\n' \
    > ~/nro/SRC/server.ini
  echo "  server.ini restored"
}

# Login.jar
chk Login.jar "Login.jar" || {
  cd ~/nro/SRC
  nohup java -Xms64m -Xmx256m -jar Login.jar >> "$LOG/login.log" 2>&1 &
  sleep 5; FIXED=1
}

# SrcTeam.jar
chk SrcTeam.jar "SrcTeam" || {
  cd ~/nro/SRC
  nohup java -Xms256m -Xmx1g \
    -Djava.awt.headless=true \
    -XX:+UseG1GC -XX:MaxGCPauseMillis=50 \
    -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
    -XX:+DisableExplicitGC -Djava.net.preferIPv4Stack=true \
    -jar SrcTeam.jar >> "$LOG/server.log" 2>&1 &
  sleep 12; FIXED=1
}

# Tóm tắt
echo ""
echo "=== STATUS ==="
chk mariadbd "MariaDB" && true
chk ws_bridge.py "ws_bridge" && true
chk cloudflared "cloudflared" && true
chk Login.jar "Login.jar" && true
chk SrcTeam.jar "SrcTeam" && true
echo "Fixed=$FIXED | WSS=$(cat /tmp/server_addr.txt 2>/dev/null || echo N/A)"

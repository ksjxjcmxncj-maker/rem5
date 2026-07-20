#!/bin/bash
# NRO Keepalive Check v2 — chay tren Codespace qua GitHub Actions
# ws_bridge.py luon duoc sync tu scripts/ws_bridge.py (v5 full-featured)
LOG=~/logs
mkdir -p "$LOG"
FIXED=0
REPO_RAW="https://raw.githubusercontent.com/ksjxjcmxncj-maker/rem5/main"

# 0. Sync Replit API URL
NEW_REPLIT=$(curl -fsSL "$REPO_RAW/.replit-url" 2>/dev/null | tr -d '[:space:]')
if [ -n "$NEW_REPLIT" ]; then
  OLD=$(grep -oP '(?<=REPLIT_API=).*' ~/.nro_config 2>/dev/null || echo "")
  if [ "$NEW_REPLIT" != "$OLD" ]; then
    echo "REPLIT_API: $OLD -> $NEW_REPLIT"
    sed -i "s|REPLIT_API=.*|REPLIT_API=$NEW_REPLIT|" ~/.nro_config 2>/dev/null || \
      printf "REPLIT_API=$NEW_REPLIT\nSESSION_SECRET=\n" > ~/.nro_config
  fi
fi
source ~/.nro_config 2>/dev/null || true

# Sync ws_bridge.py v5 (luon dung ban moi nhat tu repo)
mkdir -p ~/bin
curl -fsSL "$REPO_RAW/scripts/ws_bridge.py" -o ~/bin/ws_bridge.py 2>/dev/null && \
  chmod +x ~/bin/ws_bridge.py || true

chk() { pgrep -f "$1" > /dev/null 2>&1 && { echo "OK  $2"; return 0; }; echo "DEAD $2"; return 1; }

# 1. MariaDB
chk mariadbd "MariaDB" || {
  sudo service mariadb start 2>/dev/null || \
    (sudo mariadbd --user=mysql --datadir=/var/lib/mysql \
      --socket=/run/mysqld/mysqld.sock --pid-file=/run/mysqld/mariadbd.pid 2>/dev/null &)
  sleep 6; FIXED=1
}

# 2. ws_bridge
chk ws_bridge.py "ws_bridge" || {
  nohup python3 ~/bin/ws_bridge.py >> "$LOG/ws_bridge.log" 2>&1 &
  sleep 2; FIXED=1
}

# 3. cloudflared
chk cloudflared "cloudflared" || {
  > "$LOG/cloudflared.log"
  nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8080 --no-autoupdate \
    >> "$LOG/cloudflared.log" 2>&1 &
  sleep 25
  CF=$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' "$LOG/cloudflared.log" 2>/dev/null | tail -1)
  if [ -n "$CF" ]; then
    WSS="${CF/https:/wss:}"
    echo "$WSS" > /tmp/server_addr.txt
    echo "  CF: $CF"
    if [ -n "${REPLIT_API:-}" ]; then
      curl -s -X POST "$REPLIT_API/api/ws-url" \
        -H "Content-Type: application/json" \
        -H "x-update-secret: ${SESSION_SECRET:-}" \
        -d "{\"url\":\"$WSS\"}" && echo "  Replit updated" || true
    fi
  fi
  FIXED=1
}

# 4. server.ini
[ -f ~/nro/SRC/server.ini ] || {
  printf 'server.port=8888\ndb.driver=com.mysql.cj.jdbc.Driver\ndb.host=localhost\ndb.port=3306\ndb.name=nro1\ndb.user=root\ndb.password=\nadmin.mode=0\nwait.login=3\n' \
    > ~/nro/SRC/server.ini && echo "  server.ini restored"
}

# 5. Login.jar
chk Login.jar "Login.jar" || {
  cd ~/nro/SRC && nohup java -Xms64m -Xmx256m -jar Login.jar >> "$LOG/login.log" 2>&1 &
  sleep 5; FIXED=1
}

# 6. SrcTeam.jar
chk SrcTeam.jar "SrcTeam" || {
  cd ~/nro/SRC
  nohup java -Xms256m -Xmx1g -Djava.awt.headless=true \
    -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=4m \
    -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC -Djava.net.preferIPv4Stack=true \
    -jar SrcTeam.jar >> "$LOG/server.log" 2>&1 &
  sleep 12; FIXED=1
}

echo ""
echo "=== STATUS ==="
chk mariadbd     "MariaDB"     || true
chk ws_bridge.py "ws_bridge"   || true
chk cloudflared  "cloudflared" || true
chk Login.jar    "Login.jar"   || true
chk SrcTeam.jar  "SrcTeam"     || true
echo "Fixed=$FIXED | WSS=$(cat /tmp/server_addr.txt 2>/dev/null || echo N/A)"
echo "REPLIT_API=${REPLIT_API:-N/A}"

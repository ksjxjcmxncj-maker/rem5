#!/bin/bash
# NRO Server Start v4 — Codespace
# postStartCommand: bash start.sh > /tmp/autostart.log 2>&1 &
LOG=~/logs
mkdir -p "$LOG"

log() { echo "[$(date '+%H:%M:%S')] $*" | tee -a "$LOG/autostart.log"; }
log "=== NRO Server Start v4 ==="

# 0. Sync Replit API URL từ GitHub
log "[0] Sync Replit URL..."
NEW_REPLIT=$(curl -fsSL \
  "https://raw.githubusercontent.com/ksjxjcmxncj-maker/rem5/main/.replit-url" \
  2>/dev/null | tr -d '[:space:]')
if [ -n "$NEW_REPLIT" ]; then
  OLD=$(grep -oP '(?<=REPLIT_API=).*' ~/.nro_config 2>/dev/null || echo "")
  if [ "$NEW_REPLIT" != "$OLD" ]; then
    sed -i "s|REPLIT_API=.*|REPLIT_API=$NEW_REPLIT|" ~/.nro_config 2>/dev/null || true
    log "    REPLIT_API: $OLD -> $NEW_REPLIT"
  else
    log "    REPLIT_API không đổi"
  fi
fi
source ~/.nro_config 2>/dev/null || true

# 1. MariaDB
if ! pgrep -f mariadbd > /dev/null 2>&1; then
  log "[1] MariaDB..."
  sudo mariadbd --user=mysql --datadir=/var/lib/mysql \
    --socket=/run/mysqld/mysqld.sock \
    --pid-file=/run/mysqld/mariadbd.pid 2>/dev/null &
  sleep 6
  sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY ''; FLUSH PRIVILEGES;" 2>/dev/null || true
  log "    OK"
else
  log "[1] MariaDB đã chạy"
fi

# 2. server.ini
log "[2] server.ini..."
mkdir -p ~/nro/SRC
printf 'server.port=8888\ndb.driver=com.mysql.cj.jdbc.Driver\ndb.host=localhost\ndb.port=3306\ndb.name=nro1\ndb.user=root\ndb.password=\nadmin.mode=0\nwait.login=3\n' \
  > ~/nro/SRC/server.ini

# 3. ws_bridge
log "[3] ws_bridge..."
pkill -f ws_bridge.py 2>/dev/null || true; sleep 1
nohup python3 ~/bin/ws_bridge.py >> "$LOG/ws_bridge.log" 2>&1 &
sleep 2
pgrep -f ws_bridge.py > /dev/null && log "    OK" || log "    FAIL"
gh codespace ports visibility 8080:public -c "${CODESPACE_NAME:-}" 2>/dev/null || true

# 4. cloudflared
log "[4] cloudflared..."
pkill -f cloudflared 2>/dev/null || true; sleep 1
> "$LOG/cloudflared.log"
nohup /usr/local/bin/cloudflared tunnel \
  --url http://localhost:8080 --no-autoupdate \
  >> "$LOG/cloudflared.log" 2>&1 &

CF_URL=""
for i in $(seq 1 30); do
  CF_URL=$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' \
    "$LOG/cloudflared.log" 2>/dev/null | tail -1)
  [ -n "$CF_URL" ] && break
  sleep 2
done

if [ -n "$CF_URL" ]; then
  WSS_URL="${CF_URL/https:/wss:}"
  echo "$WSS_URL" > /tmp/server_addr.txt
  log "    CF: $CF_URL"
  if [ -n "${REPLIT_API:-}" ]; then
    CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$REPLIT_API/api/ws-url" \
      -H "Content-Type: application/json" \
      -H "x-update-secret: ${SESSION_SECRET:-}" \
      -d "{\"url\": \"$WSS_URL\"}" 2>/dev/null)
    log "    Replit API: $CODE"
  fi
else
  log "    cloudflared timeout"
  WSS_URL="wss://${CODESPACE_NAME:-localhost}-8080.app.github.dev"
  echo "$WSS_URL" > /tmp/server_addr.txt
fi

# 5. Login.jar
log "[5] Login.jar..."
pkill -9 -f Login.jar 2>/dev/null || true; sleep 1
cd ~/nro/SRC
if [ -f Login.jar ]; then
  nohup java -Xms64m -Xmx256m -jar Login.jar >> "$LOG/login.log" 2>&1 &
  sleep 5
  pgrep -f Login.jar > /dev/null && log "    OK" || log "    FAIL"
fi

# 6. SrcTeam.jar
log "[6] SrcTeam.jar..."
pkill -9 -f SrcTeam.jar 2>/dev/null || true; sleep 2
nohup java \
  -Xms256m -Xmx1g \
  -Djava.awt.headless=true \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=50 \
  -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
  -XX:+DisableExplicitGC -Djava.net.preferIPv4Stack=true \
  -jar SrcTeam.jar >> "$LOG/server.log" 2>&1 &
sleep 12
if pgrep -f SrcTeam.jar > /dev/null; then
  log "    OK"
else
  log "    FAIL"; tail -5 "$LOG/server.log" >> "$LOG/autostart.log" 2>/dev/null || true
fi

# 7. Đăng ký cron watchdog nội bộ (mỗi 2 phút check cloudflared)
log "[7] Cron watchdog..."
CRON_LINE="*/2 * * * * bash /workspaces/rem5/scripts/quick_check.sh >> ~/logs/quick_check.log 2>&1"
( crontab -l 2>/dev/null | grep -v quick_check; echo "$CRON_LINE" ) | crontab -
log "    Cron đã đăng ký: $(crontab -l 2>/dev/null | grep quick_check)"

log "=== XONG === WSS: $(cat /tmp/server_addr.txt 2>/dev/null || echo N/A)"

#!/bin/bash
# quick_check.sh v2 — watchdog 2 phut, check TAT CA 5 service
LOG_DIR=~/logs
mkdir -p "$LOG_DIR"
LOG="$LOG_DIR/quick_check.log"
REPO_RAW="https://raw.githubusercontent.com/ksjxjcmxncj-maker/rem5/main"
FIXED=0

source ~/.nro_config 2>/dev/null || true
chk() { pgrep -f "$1" > /dev/null 2>&1; }
ts()  { date '+%H:%M:%S'; }

# cloudflared
if ! chk cloudflared; then
  echo "[$(ts)] cloudflared DEAD — restart..." | tee -a "$LOG"
  > "$LOG_DIR/cloudflared.log"
  nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8080 --no-autoupdate \
    >> "$LOG_DIR/cloudflared.log" 2>&1 &
  CF=""
  for i in $(seq 1 25); do
    CF=$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' "$LOG_DIR/cloudflared.log" 2>/dev/null | tail -1)
    [ -n "$CF" ] && break; sleep 2
  done
  if [ -n "$CF" ]; then
    WSS="${CF/https:/wss:}"
    echo "$WSS" > /tmp/server_addr.txt
    if [ -n "${REPLIT_API:-}" ]; then
      curl -s -X POST "$REPLIT_API/api/ws-url" \
        -H "Content-Type: application/json" -H "x-update-secret: ${SESSION_SECRET:-}" \
        -d "{\"url\":\"$WSS\"}" > /dev/null 2>&1 || true
    fi
    echo "[$(ts)] cloudflared OK: $CF" | tee -a "$LOG"
  else
    echo "[$(ts)] cloudflared FAIL" | tee -a "$LOG"
  fi
  FIXED=1
fi

# ws_bridge
if ! chk ws_bridge.py; then
  echo "[$(ts)] ws_bridge DEAD — restart..." | tee -a "$LOG"
  curl -fsSL "$REPO_RAW/scripts/ws_bridge.py" -o ~/bin/ws_bridge.py 2>/dev/null && chmod +x ~/bin/ws_bridge.py || true
  nohup python3 ~/bin/ws_bridge.py >> "$LOG_DIR/ws_bridge.log" 2>&1 &
  sleep 2
  chk ws_bridge.py && echo "[$(ts)] ws_bridge OK" | tee -a "$LOG" || echo "[$(ts)] ws_bridge FAIL" | tee -a "$LOG"
  FIXED=1
fi

# MariaDB
if ! chk mariadbd; then
  echo "[$(ts)] MariaDB DEAD — restart..." | tee -a "$LOG"
  sudo service mariadb start 2>/dev/null || \
    (sudo mariadbd --user=mysql --datadir=/var/lib/mysql \
      --socket=/run/mysqld/mysqld.sock --pid-file=/run/mysqld/mariadbd.pid 2>/dev/null &)
  sleep 6; FIXED=1
  echo "[$(ts)] MariaDB restarted" | tee -a "$LOG"
fi

# Login.jar
if ! chk Login.jar; then
  echo "[$(ts)] Login.jar DEAD — restart..." | tee -a "$LOG"
  cd ~/nro/SRC && nohup java -Xms64m -Xmx256m -jar Login.jar >> "$LOG_DIR/login.log" 2>&1 &
  sleep 5; FIXED=1
  chk Login.jar && echo "[$(ts)] Login.jar OK" | tee -a "$LOG" || echo "[$(ts)] Login.jar FAIL" | tee -a "$LOG"
fi

# SrcTeam.jar
if ! chk SrcTeam.jar; then
  echo "[$(ts)] SrcTeam DEAD — restart..." | tee -a "$LOG"
  cd ~/nro/SRC
  nohup java -Xms256m -Xmx1g -Djava.awt.headless=true \
    -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=4m \
    -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC -Djava.net.preferIPv4Stack=true \
    -jar SrcTeam.jar >> "$LOG_DIR/server.log" 2>&1 &
  sleep 12; FIXED=1
  chk SrcTeam.jar && echo "[$(ts)] SrcTeam OK" | tee -a "$LOG" || echo "[$(ts)] SrcTeam FAIL" | tee -a "$LOG"
fi

[ "$FIXED" -eq 0 ] && echo "[$(ts)] All OK" >> "$LOG" || true

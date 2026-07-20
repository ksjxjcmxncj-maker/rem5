#!/bin/bash
# NRO Keep-Alive — Codespace Network Edition
# Tự động phát hiện Codespace, ping server, restart nếu chết
# Không dùng frp/ngrok — kết nối qua Codespace WSS port forwarding

GH_BIN="/tmp/gh_2.52.0_linux_amd64/bin/gh"
INTERVAL=1200   # 20 phút

TOKENS=(
  "Main|GITHUB_PERSONAL_ACCESS_TOKEN"
)

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"; }

install_gh() {
  [ -f "$GH_BIN" ] && return 0
  log "Cài gh CLI..."
  curl -sL https://github.com/cli/cli/releases/download/v2.52.0/gh_2.52.0_linux_amd64.tar.gz | tar -xz -C /tmp/
}

get_vn_hour() {
  date -u -d "+7 hours" '+%H' | sed 's/^0*//' | grep -v '^$' || echo "0"
}

is_active_hours() {
  local H=$(get_vn_hour)
  [ "$H" -ge 4 ] && [ "$H" -lt 23 ]
}

# Tìm codespace đang Available cho token
find_codespaces() {
  local TOKEN_VAR="$1"
  export GITHUB_TOKEN=$(printenv "$TOKEN_VAR")
  $GH_BIN codespace list --json name,state 2>/dev/null | python3 -c "
import sys,json
for cs in json.load(sys.stdin):
    if cs.get('state') in ('Available','Starting','Shutdown'):
        print(cs['name']+'|'+cs['state'])
" 2>/dev/null
}

start_cs_if_needed() {
  local CS="$1" STATE="$2"
  [ "$STATE" = "Shutdown" ] && {
    log "[$CS] Shutdown → start..."
    $GH_BIN codespace start -c "$CS" 2>/dev/null && sleep 25
  }
}

ping_and_fix() {
  local CS="$1" TOKEN_VAR="$2"
  export GITHUB_TOKEN=$(printenv "$TOKEN_VAR")

  RESULT=$(timeout 50 $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
pgrep -f "SrcTeam|NgocRongOnline" > /dev/null && echo "ALIVE" || echo "DEAD"
cat /tmp/server_addr.txt 2>/dev/null || true
REMOTE
  )

  if echo "$RESULT" | grep -q "ALIVE"; then
    ADDR=$(echo "$RESULT" | grep -v ALIVE | head -1)
    log "[$CS] ✅ ALIVE | APK endpoint: $ADDR"
    return 0
  fi

  log "[$CS] ❌ DEAD — đang restart..."
  timeout 90 $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE' || true
nohup bash ~/bin/../nro/../start.sh > /tmp/restart.log 2>&1 &
sleep 18
pgrep -f SrcTeam && echo "✅ Restarted OK" || { echo "❌ Restart fail"; tail -10 ~/logs/server.log; }
REMOTE
  return 1
}

install_gh
log "=== NRO Keep-Alive (Codespace Network) started ==="

while true; do
  if ! is_active_hours; then
    log "Ngoài giờ (04:00-23:00 VN) — nghỉ..."
    sleep $INTERVAL; continue
  fi

  FOUND=0
  for ENTRY in "${TOKENS[@]}"; do
    LABEL=$(echo "$ENTRY" | cut -d'|' -f1)
    TOKEN_VAR=$(echo "$ENTRY" | cut -d'|' -f2)
    CS_LIST=$(find_codespaces "$TOKEN_VAR")

    [ -z "$CS_LIST" ] && { log "[$LABEL] Không có Codespace nào"; continue; }

    while IFS='|' read -r CS_NAME CS_STATE; do
      log "[$LABEL] Tìm thấy: $CS_NAME ($CS_STATE)"
      start_cs_if_needed "$CS_NAME" "$CS_STATE"
      ping_and_fix "$CS_NAME" "$TOKEN_VAR"
      FOUND=1; break
    done <<< "$CS_LIST"
    [ "$FOUND" -eq 1 ] && break
  done

  [ "$FOUND" -eq 0 ] && log "⚠️ Không có Codespace khả dụng!"
  log "Nghỉ ${INTERVAL}s..."; sleep $INTERVAL
done

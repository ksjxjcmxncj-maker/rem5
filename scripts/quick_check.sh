#!/bin/bash
# quick_check.sh — Codespace-local cron, chạy mỗi 2 phút
# Chỉ check cloudflared (nguyên nhân chính đổi URL) để phản hồi nhanh
LOG=~/logs/quick_check.log
mkdir -p ~/logs

pgrep -f cloudflared > /dev/null 2>&1 && exit 0

echo "[$(date '+%H:%M:%S')] cloudflared DEAD — restarting..." | tee -a "$LOG"
> ~/logs/cloudflared.log
nohup /usr/local/bin/cloudflared tunnel \
  --url http://localhost:8080 --no-autoupdate \
  >> ~/logs/cloudflared.log 2>&1 &

# Đợi URL (tối đa 50s)
CF=""
for i in $(seq 1 25); do
  CF=$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' \
    ~/logs/cloudflared.log 2>/dev/null | tail -1)
  [ -n "$CF" ] && break
  sleep 2
done

if [ -n "$CF" ]; then
  WSS="${CF/https:/wss:}"
  echo "$WSS" > /tmp/server_addr.txt
  source ~/.nro_config 2>/dev/null || true
  if [ -n "${REPLIT_API:-}" ]; then
    CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$REPLIT_API/api/ws-url" \
      -H "Content-Type: application/json" \
      -H "x-update-secret: ${SESSION_SECRET:-}" \
      -d "{\"url\":\"$WSS\"}" 2>/dev/null)
    echo "[$(date '+%H:%M:%S')] cloudflared OK → $CF (Replit API: $CODE)" | tee -a "$LOG"
  else
    echo "[$(date '+%H:%M:%S')] cloudflared OK → $CF (no REPLIT_API)" | tee -a "$LOG"
  fi
else
  echo "[$(date '+%H:%M:%S')] cloudflared FAIL — no URL" | tee -a "$LOG"
fi

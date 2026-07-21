#!/bin/bash
# Auto-start NRO Game Server + WebSocket Bridge — WebSocket ONLY
# Goi boi devcontainer.json postStartCommand

LOG_DIR="$HOME/logs"
mkdir -p "$LOG_DIR"

# Redirect log nhưng KHÔNG block (không dùng exec >>)
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_DIR/autostart.log"; }

log ""
log "=== Codespace autostart ==="

# 1. MariaDB
if mysql -u root -e "SELECT 1;" > /dev/null 2>&1; then
  log "[OK] MariaDB running"
else
  log "[START] MariaDB..."
  sudo service mariadb start 2>/dev/null &
  sleep 5
  mysql -u root -e "SELECT 1;" > /dev/null 2>&1 && log "[OK] MariaDB OK" || log "[WARN] MariaDB not ready"
fi

# 2. Game Server (TCP 14445)
if pgrep -f NgocRongOnline > /dev/null 2>&1; then
  log "[OK] Game server running ($(pgrep -f NgocRongOnline | head -1))"
else
  NRO_DIR=""
  for d in ~/nro/SRC /home/codespace/nro/SRC; do
    [ -f "$d/NgocRongOnline.jar" ] && NRO_DIR="$d" && break
  done
  if [ -n "$NRO_DIR" ]; then
    cd "$NRO_DIR"
    nohup java -Xms512m -Xmx1g \
      -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
      -Djava.net.preferIPv4Stack=true \
      -jar NgocRongOnline.jar >> "$LOG_DIR/server.log" 2>&1 &
    log "[START] Game server PID=$!"
  else
    # fallback: tìm bất kỳ .jar nào
    JAR=$(find ~/nro -name "*.jar" 2>/dev/null | grep -viE "login|lib/" | head -1)
    JAR_LOGIN=$(find ~/nro -name "*.jar" 2>/dev/null | grep -iE "login" | head -1)
    LIB=$(find ~/nro -name "lib" -type d 2>/dev/null | head -1)
    if [ -n "$JAR_LOGIN" ]; then
      SDIR=$(dirname "$JAR_LOGIN")
      CP="$(basename "$JAR_LOGIN")"; [ -n "$LIB" ] && CP="$CP:$LIB/*"
      cd "$SDIR"
      nohup java -Xms128m -Xmx512m -cp "$CP" Main >> "$LOG_DIR/login.log" 2>&1 &
      log "[START] Login server PID=$!"
    fi
    if [ -n "$JAR" ]; then
      SDIR=$(dirname "$JAR")
      CP="$(basename "$JAR")"; [ -n "$LIB" ] && CP="$CP:$LIB/*"
      cd "$SDIR"
      nohup java -Xms256m -Xmx1g -cp "$CP" Main >> "$LOG_DIR/server.log" 2>&1 &
      log "[START] Game server PID=$!"
    else
      log "[WARN] Không tìm thấy .jar — bỏ qua"
    fi
  fi
fi

# 3. WebSocket Bridge (port 8080 → TCP 14445)
if pgrep -f ws_bridge > /dev/null 2>&1; then
  log "[OK] ws_bridge running"
else
  WS_BRIDGE=""
  for p in ~/bin/ws_bridge.py ~/ws_bridge.py /workspaces/*/scripts/ws_bridge_server.py; do
    [ -f "$p" ] && WS_BRIDGE="$p" && break
  done
  if [ -z "$WS_BRIDGE" ]; then
    WS_BRIDGE="/tmp/ws_bridge.py"
    cat > "$WS_BRIDGE" << 'PYEOF'
#!/usr/bin/env python3
"""WS Bridge: WebSocket 0.0.0.0:8080 -> TCP 127.0.0.1:14445"""
import asyncio, logging, sys, subprocess
logging.basicConfig(level=logging.INFO, format='[%(asctime)s] %(message)s', datefmt='%H:%M:%S')
log = logging.getLogger(__name__)
try:
    import websockets
except ImportError:
    subprocess.run([sys.executable, "-m", "pip", "install", "websockets", "-q"])
    import websockets

async def handle(ws, path=None):
    ip = ws.remote_address[0] if ws.remote_address else "?"
    log.info(f"Client: {ip}")
    try:
        reader, writer = await asyncio.open_connection("127.0.0.1", 14445)
        async def a2b():
            async for d in ws:
                if isinstance(d, bytes): writer.write(d); await writer.drain()
            writer.close()
        async def b2a():
            while True:
                d = await reader.read(65536)
                if not d: break
                await ws.send(d)
            await ws.close()
        await asyncio.gather(a2b(), b2a(), return_exceptions=True)
    except Exception as e:
        log.error(f"Error: {e}")

async def main():
    log.info("WS Bridge 0.0.0.0:8080 -> 127.0.0.1:14445")
    async with websockets.serve(handle, "0.0.0.0", 8080):
        await asyncio.Future()

asyncio.run(main())
PYEOF
  fi
  nohup python3 "$WS_BRIDGE" >> "$LOG_DIR/ws_bridge.log" 2>&1 &
  log "[START] ws_bridge PID=$! script=$WS_BRIDGE"
fi

log "=== Autostart hoàn tất — xem log tại $LOG_DIR ==="
# KHÔNG dùng tail -f ở đây — postStartCommand phải thoát để editor hoạt động

#!/bin/bash
# ════════════════════════════════════════════════════════════════════════════
# NRO Xray Server Setup — Chạy trên GitHub Codespace
# Thiết lập VLESS over WebSocket — dùng với v2rayNG trên Android
#
# Kiến trúc:
#   Android (v2rayNG) → WebSocket/HTTPS → Codespace (Xray) → Game Server :14445
#
# CÁCH DÙNG:
#   bash scripts/setup_xray_codespace.sh
# ════════════════════════════════════════════════════════════════════════════
set -e

XRAY_DIR="$HOME/.xray"
XRAY_BIN="$XRAY_DIR/xray"
XRAY_CONFIG="$XRAY_DIR/config.json"
XRAY_PORT=9000
GAME_PORT=14445
GAME_HOST="127.0.0.1"

G="\033[92m"; Y="\033[93m"; R="\033[91m"; C="\033[96m"; N="\033[0m"; B="\033[1m"
ok()   { echo -e "${G}✅ $1${N}"; }
info() { echo -e "${Y}→  $1${N}"; }
err()  { echo -e "${R}❌ $1${N}"; exit 1; }

echo -e "${C}${B}"
echo "  ╔══════════════════════════════════════════╗"
echo "  ║   NRO Xray Server Setup — Codespace     ║"
echo "  ╚══════════════════════════════════════════╝"
echo -e "${N}"

# ── Tạo UUID ngẫu nhiên cho VLESS
UUID=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || python3 -c "import uuid; print(uuid.uuid4())")
ok "UUID: $UUID"

# ── Download Xray-core nếu chưa có
mkdir -p "$XRAY_DIR"
if [ ! -f "$XRAY_BIN" ]; then
    info "Đang tải Xray-core..."
    ARCH=$(uname -m)
    case $ARCH in
        x86_64)  XRAY_FILE="Xray-linux-64.zip" ;;
        aarch64) XRAY_FILE="Xray-linux-arm64-v8a.zip" ;;
        *)       err "Kiến trúc không hỗ trợ: $ARCH" ;;
    esac
    
    XRAY_VER=$(curl -s https://api.github.com/repos/XTLS/Xray-core/releases/latest \
        | grep '"tag_name"' | cut -d'"' -f4)
    info "Version: $XRAY_VER"
    
    curl -Lo "/tmp/xray.zip" \
        "https://github.com/XTLS/Xray-core/releases/download/$XRAY_VER/$XRAY_FILE"
    
    unzip -o /tmp/xray.zip xray -d "$XRAY_DIR"
    chmod +x "$XRAY_BIN"
    rm /tmp/xray.zip
    ok "Xray đã cài tại $XRAY_BIN"
else
    ok "Xray đã có: $XRAY_BIN"
fi

# ── Lấy Codespace URL
CODESPACE_NAME="${CODESPACE_NAME:-}"
if [ -n "$CODESPACE_NAME" ]; then
    # Đang chạy trong GitHub Codespace
    WS_HOST="${CODESPACE_NAME}-${XRAY_PORT}.app.github.dev"
    WSS_URL="wss://${WS_HOST}/ws"
else
    # Replit hoặc môi trường khác
    WS_HOST="${REPLIT_DEV_DOMAIN:-localhost:${XRAY_PORT}}"
    WSS_URL="wss://${WS_HOST}/ws"
fi

# ── Tạo config Xray (VLESS + WebSocket)
cat > "$XRAY_CONFIG" <<EOF
{
  "log": {
    "loglevel": "info",
    "access": "$XRAY_DIR/access.log",
    "error": "$XRAY_DIR/error.log"
  },
  "inbounds": [
    {
      "listen": "0.0.0.0",
      "port": ${XRAY_PORT},
      "protocol": "vless",
      "settings": {
        "clients": [
          {
            "id": "${UUID}",
            "level": 0,
            "email": "nro@game.local"
          }
        ],
        "decryption": "none"
      },
      "streamSettings": {
        "network": "ws",
        "wsSettings": {
          "path": "/ws",
          "headers": {}
        }
      },
      "sniffing": {
        "enabled": false
      }
    }
  ],
  "outbounds": [
    {
      "tag": "game-server",
      "protocol": "freedom",
      "settings": {
        "domainStrategy": "AsIs"
      }
    }
  ],
  "routing": {
    "rules": [
      {
        "type": "field",
        "network": "tcp,udp",
        "outboundTag": "game-server"
      }
    ]
  }
}
EOF

ok "Config tạo tại $XRAY_CONFIG"

# ── Dừng Xray cũ nếu đang chạy
pkill -f "$XRAY_BIN" 2>/dev/null || true
sleep 1

# ── Khởi động Xray
nohup "$XRAY_BIN" run -c "$XRAY_CONFIG" > "$XRAY_DIR/xray.log" 2>&1 &
XRAY_PID=$!
sleep 2

if kill -0 $XRAY_PID 2>/dev/null; then
    ok "Xray đang chạy! PID: $XRAY_PID"
else
    err "Xray khởi động thất bại. Xem log: cat $XRAY_DIR/xray.log"
fi

# ── Sinh v2rayNG config JSON
V2RAYNGCONFIG=$(cat <<EOF
{
  "v": "2",
  "ps": "NRO-Codespace",
  "add": "${WS_HOST%:*}",
  "port": "443",
  "id": "${UUID}",
  "aid": "0",
  "scy": "none",
  "net": "ws",
  "type": "none",
  "host": "${WS_HOST%:*}",
  "path": "/ws",
  "tls": "tls",
  "sni": "${WS_HOST%:*}",
  "alpn": "",
  "fp": ""
}
EOF
)

# Encode sang vless:// link
V2_JSON_B64=$(echo "$V2RAYNGCONFIG" | base64 -w0)
VLESS_LINK="vless://${UUID}@${WS_HOST%:*}:443?encryption=none&security=tls&sni=${WS_HOST%:*}&type=ws&host=${WS_HOST%:*}&path=%2Fws#NRO-Codespace"

# Tạo QR code link (dùng qrencode nếu có)
echo ""
echo -e "${B}${C}═══════════════════════════════════════════════════════${N}"
echo -e "${B}  📱 CẤU HÌNH v2rayNG (Android)${N}"
echo -e "${C}═══════════════════════════════════════════════════════${N}"
echo ""
echo -e "${G}VLESS Link (copy vào v2rayNG → Add → Import URL):${N}"
echo ""
echo "$VLESS_LINK"
echo ""

# Sinh QR code bằng Python nếu có qrcode
python3 -c "
try:
    import qrcode
    qr = qrcode.QRCode(border=1)
    qr.add_data('$VLESS_LINK')
    qr.make()
    qr.print_ascii(tty=True)
    print()
except ImportError:
    print('(pip install qrcode để xem QR code)')
" 2>/dev/null || true

echo -e "${G}WebSocket URL (dùng cho NRO Bridge APK):${N}"
echo "$WSS_URL"
echo ""
echo -e "${B}${C}═══════════════════════════════════════════════════════${N}"
echo -e "${B}  ⚙️  CẤU HÌNH TRONG GAME${N}"
echo -e "${C}═══════════════════════════════════════════════════════${N}"
echo ""
echo -e "  Khi dùng ${B}v2rayNG${N}:"
echo "  1. Import VLESS link vào v2rayNG"
echo "  2. Bật VPN"
echo "  3. Trong game: nhập IP game server thật + port 14445"
echo ""
echo -e "  Khi dùng ${B}NRO Bridge APK${N}:"
echo "  1. Mở NRO Bridge app, nhập: $WSS_URL"
echo "  2. Bấm Start"
echo "  3. Trong game: Custom Server → 127.0.0.1:14445"
echo ""

# ── Lưu config vào file
cat > "$XRAY_DIR/nro_bridge_config.txt" <<EOF
# NRO Xray Server Config
# Generated: $(date)

UUID: ${UUID}
Xray Port: ${XRAY_PORT}
WebSocket Path: /ws
WSS URL: ${WSS_URL}

VLESS Link:
${VLESS_LINK}

NRO Bridge APK WS URL:
${WSS_URL}

Logs:
  tail -f ${XRAY_DIR}/xray.log
  tail -f ${XRAY_DIR}/access.log
EOF

ok "Config lưu tại: $XRAY_DIR/nro_bridge_config.txt"
echo ""
echo -e "${Y}Để dừng Xray: ${B}pkill -f xray${N}"
echo -e "${Y}Để xem log:   ${B}tail -f $XRAY_DIR/xray.log${N}"
echo ""

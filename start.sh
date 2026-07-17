#!/bin/bash
# =============================================
#  NGOC RONG SERVER + SSH REMOTE
# =============================================
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/server"
LOG_DIR="$SCRIPT_DIR/logs"
mkdir -p "$LOG_DIR"

echo "=== Cài SSH server ==="
sudo apt-get install -y -qq openssh-server 2>/dev/null
sudo mkdir -p /run/sshd
echo "PermitRootLogin yes
PasswordAuthentication yes
ChallengeResponseAuthentication no
UsePAM no
Subsystem sftp /usr/lib/openssh/sftp-server" | sudo tee /etc/ssh/sshd_config > /dev/null
echo "codespace:colab2024" | sudo chpasswd 2>/dev/null || true
sudo service ssh start 2>/dev/null || sudo /usr/sbin/sshd
echo "✅ SSH OK"

echo "=== Khởi động MariaDB ==="
sudo service mariadb start 2>/dev/null || sudo mysqld_safe --skip-grant-tables &
sleep 3

# Import DB nếu chưa có
DB_OK=$(sudo mysql -e "SHOW DATABASES LIKE 'team2026';" 2>/dev/null | grep team2026 || true)
if [ -z "$DB_OK" ]; then
  sudo mysql -e "CREATE DATABASE IF NOT EXISTS team2026 CHARACTER SET utf8mb4;"
  [ -f "$SERVER_DIR/database_team2026.sql" ] && sudo mysql team2026 < "$SERVER_DIR/database_team2026.sql"
  echo "✅ Database ready"
fi

echo "=== Tải bore ==="
if [ ! -f /tmp/bore ]; then
  curl -sLo /tmp/bore.tar.gz "https://github.com/ekzhang/bore/releases/download/v0.5.1/bore-v0.5.1-x86_64-unknown-linux-musl.tar.gz"
  tar -xzf /tmp/bore.tar.gz -C /tmp/
  chmod +x /tmp/bore
fi

echo "=== Mở SSH tunnel ==="
/tmp/bore local 22 --to bore.pub > /tmp/bore_ssh.log 2>&1 &
sleep 3
SSH_PORT=$(grep -o 'remote_port=[0-9]*' /tmp/bore_ssh.log | cut -d= -f2)

echo "=== Khởi động Java Game Server ==="
cd "$SERVER_DIR"
# Cập nhật config
sed -i "s|database.pass=.*|database.pass=|g" Config.properties
sed -i "s|database.user=.*|database.user=root|g" Config.properties

nohup java -Xms256m -Xmx1g \
  -cp "NgocRongOnline.jar:lib/*" \
  Main \
  > "$LOG_DIR/server.log" 2>&1 &
SERVER_PID=$!
echo $SERVER_PID > /tmp/server.pid
echo "✅ Game server PID: $SERVER_PID"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "  ✅ SERVER ĐÃ CHẠY"
echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  🎮 Game port : 14445 (Codespaces forward)"
echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  🔐 SSH TỪ XA (Replit / Termux / máy tính):"
echo "  ssh -o StrictHostKeyChecking=no -p $SSH_PORT codespace@bore.pub"
echo "  Password: colab2024"
echo "  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  📋 ALL-IN-ONE paste vào Replit Shell:"
echo "  ssh -o StrictHostKeyChecking=no -p $SSH_PORT codespace@bore.pub"
echo "╚══════════════════════════════════════════════════════╝"
echo ""
echo "🔄 Log server (Ctrl+C không tắt server):"
tail -f "$LOG_DIR/server.log"

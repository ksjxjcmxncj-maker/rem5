#!/bin/bash
# NRO Server Setup v2 — Debian Bullseye / GitHub Codespaces
# devcontainer: mcr.microsoft.com/devcontainers/java:17-bullseye
LOG=~/logs
mkdir -p "$LOG" ~/nro/SRC ~/bin

echo "========================================="
echo "  NRO Server Setup (Codespace / Debian)"
echo "========================================="

# 1. Kiem tra phu thuoc
echo "[1] Phu thuoc..."
java -version 2>&1 | head -1
if ! command -v mariadbd &>/dev/null && ! command -v mysqld &>/dev/null; then
  echo "  Cai mariadb-server (apt-get)..."
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq mariadb-server
fi
python3 -c "import websockets" 2>/dev/null || {
  pip3 install websockets --quiet 2>/dev/null || \
  sudo apt-get install -y -qq python3-websockets 2>/dev/null || true
}
echo "  websockets: OK"

# 2. MariaDB (Debian: service mariadb start, khong dung mysqld_safe/apk)
echo "[2] MariaDB..."
if ! sudo mysqladmin ping --silent 2>/dev/null; then
  sudo service mariadb start 2>/dev/null || \
    (sudo mariadbd --user=mysql --datadir=/var/lib/mysql \
      --socket=/run/mysqld/mysqld.sock \
      --pid-file=/run/mysqld/mariadbd.pid 2>/dev/null &)
  sleep 6
fi
sudo mysqladmin ping --silent 2>/dev/null && echo "  MariaDB OK" || echo "  MariaDB FAIL (tiep tuc...)"

# 3. Sync files
echo "[3] Sync files..."
REPO_DIR=/workspaces/rem5
cd "$REPO_DIR" && git pull --quiet 2>/dev/null || true
cp -f  "$REPO_DIR/server/SrcTeam.jar"       ~/nro/SRC/SrcTeam.jar       2>/dev/null || true
cp -f  "$REPO_DIR/server/_Login/Login.jar"  ~/nro/SRC/Login.jar         2>/dev/null || true
cp -rf "$REPO_DIR/server/resources"         ~/nro/SRC/resources         2>/dev/null || true
cp -f  "$REPO_DIR/server/Config.properties" ~/nro/SRC/Config.properties 2>/dev/null || true
# QUAN TRONG: luon copy ws_bridge.py v5 (full), khong dung phien ban inline cu
cp -f  "$REPO_DIR/scripts/ws_bridge.py"     ~/bin/ws_bridge.py          2>/dev/null || true
chmod +x ~/bin/ws_bridge.py 2>/dev/null || true
echo "  ws_bridge.py: v5 (tu scripts/)"

# 4. Database nro1
echo "[4] Database..."
sudo mariadb -e "CREATE DATABASE IF NOT EXISTS nro1 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || \
sudo mysql   -e "CREATE DATABASE IF NOT EXISTS nro1 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
TABLES=$(sudo mariadb nro1 -se "SHOW TABLES;" 2>/dev/null | wc -l || echo 0)
if [ "$TABLES" -lt 5 ]; then
  echo "  Import srcteam_nro.sql..."
  sudo mariadb nro1 < "$REPO_DIR/database/srcteam_nro.sql" 2>/dev/null \
    && echo "  DB OK" || {
    echo "  Thu backup..."
    sudo mariadb nro1 < "$REPO_DIR/database/nro1_backup_20260717_2255.sql" 2>/dev/null \
      && echo "  backup OK" || echo "  DB FAIL"
  }
else
  echo "  DB san sang ($TABLES tables)"
fi

# 5. Patch Config.properties + server.properties
echo "[5] Patch config..."
CS_NAME="${CODESPACE_NAME:-localhost}"
WS_HOST="${CS_NAME}-8080.app.github.dev"
CFG=~/nro/SRC/Config.properties
SPROP=~/nro/SRC/resources/config/server.properties
sed -i "s|server.sv1=.*|server.sv1=NRO:${WS_HOST}:443:0,0,0|" "$CFG"
sed -i "s|server.local=.*|server.local=false|"      "$CFG"
sed -i "s|database.host=.*|database.host=localhost|" "$CFG"
sed -i "s|database.name=.*|database.name=nro1|"     "$CFG"
sed -i "s|database.user=.*|database.user=root|"     "$CFG"
sed -i "s|database.pass=.*|database.pass=|"          "$CFG"
sed -i "s|database.driver=com.mysql.jdbc.Driver|database.driver=com.mysql.cj.jdbc.Driver|" "$CFG" 2>/dev/null || true
sed -i "s|server.sv1=.*|server.sv1=NRO:${WS_HOST}:443:0,0,0|"  "$SPROP" 2>/dev/null || true
sed -i "s|server.db.name=.*|server.db.name=nro1|"               "$SPROP" 2>/dev/null || true
sed -i "s|server.db.ip=.*|server.db.ip=localhost|"               "$SPROP" 2>/dev/null || true
sed -i "s|server.db.pw=.*|server.db.pw=|"                        "$SPROP" 2>/dev/null || true
echo "  sv1 OK"

# 6. server.ini cho Login.jar
echo "[6] server.ini..."
printf 'server.port=8888\ndb.driver=com.mysql.cj.jdbc.Driver\ndb.host=localhost\ndb.port=3306\ndb.name=nro1\ndb.user=root\ndb.password=\nadmin.mode=0\nwait.login=3\n' \
  > ~/nro/SRC/server.ini

# 7. Cai cloudflared neu chua co
echo "[7] cloudflared..."
if [ ! -f /usr/local/bin/cloudflared ]; then
  curl -fsSL https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 \
    -o /usr/local/bin/cloudflared 2>/dev/null && chmod +x /usr/local/bin/cloudflared
fi
[ -f /usr/local/bin/cloudflared ] && echo "  cloudflared OK" || echo "  cloudflared FAIL"

echo ""
echo "Setup hoan tat. Chay: bash /workspaces/rem5/start.sh"

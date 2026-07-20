#!/bin/bash
# Bootstrap cho Codespace Alpine Linux — cài deps + chạy setup
export CODESPACE_NAME="${CODESPACE_NAME:-}"
LOG=~/logs; mkdir -p "$LOG"

echo "[1] apk deps..."
sudo apk update -q 2>/dev/null
sudo apk add -q openjdk17-jre mariadb mariadb-client py3-websockets 2>/dev/null
java -version 2>&1 | head -1

echo "[2] MariaDB init..."
sudo mysql_install_db --user=mysql --datadir=/var/lib/mysql --skip-test-db >/dev/null 2>&1 || true
sudo mysqld_safe --user=mysql --datadir=/var/lib/mysql >/dev/null 2>&1 &
sleep 6
mysqladmin -u root ping 2>/dev/null && echo "MariaDB OK" || echo "MariaDB FAIL"

echo "[3] Chạy setup.sh..."
bash /workspaces/rem5/.devcontainer/setup.sh 2>&1 | tee "$LOG/setup.log"

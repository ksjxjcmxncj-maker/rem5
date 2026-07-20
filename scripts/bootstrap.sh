#!/bin/bash
# Bootstrap: cài môi trường + chạy setup NRO
export DEBIAN_FRONTEND=noninteractive
LOG=~/logs
mkdir -p "$LOG"

echo "[1/5] apt update..."
sudo apt-get update -qq

echo "[2/5] Java 17..."
sudo apt-get install -y -qq openjdk-17-jre-headless
java -version 2>&1 | head -1

echo "[3/5] MariaDB..."
sudo apt-get install -y -qq mariadb-server
sudo service mariadb start
sleep 3
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY ''; FLUSH PRIVILEGES;" 2>/dev/null || true
sudo mysql -e "SELECT VERSION();" 2>/dev/null | head -1

echo "[4/5] Python websockets..."
sudo apt-get install -y -qq python3-pip
pip3 install websockets -q

echo "[5/5] gh CLI..."
curl -sL https://github.com/cli/cli/releases/download/v2.52.0/gh_2.52.0_linux_amd64.tar.gz | sudo tar -xz -C /usr/local/ 2>/dev/null
sudo ln -sf /usr/local/gh_2.52.0_linux_amd64/bin/gh /usr/local/bin/gh 2>/dev/null || true

echo "=== Môi trường OK — chạy setup.sh ==="
bash /workspaces/rem5/.devcontainer/setup.sh 2>&1 | tee "$LOG/setup.log"

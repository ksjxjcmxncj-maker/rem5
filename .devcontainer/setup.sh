#!/bin/bash
set -e
echo '=== Install packages ==='
sudo apt-get update -qq
sudo apt-get install -y -qq mariadb-server wget curl p7zip-full unrar-free python3 python3-pip 2>/dev/null || \
  sudo apt-get install -y -qq mariadb-server wget curl p7zip-full python3 python3-pip 2>/dev/null

echo '=== Install websockets (Python) ==='
pip3 install websockets --quiet 2>/dev/null || true

echo '=== Download server from Google Drive ==='
mkdir -p ~/nro && cd ~/nro
wget -q --no-check-certificate \
  'https://drive.usercontent.google.com/download?id=1uH2O2FtuGpIQfIYVAhi9wcuxfDjTQddY&export=download&confirm=t' \
  -O server_pkg.rar
echo "Downloaded: $(ls -lh server_pkg.rar)"

echo '=== Extract ==='
unrar x -y server_pkg.rar . 2>/dev/null || 7z x server_pkg.rar -o. -y 2>/dev/null || true

echo '=== Create dirs ==='
mkdir -p ~/logs ~/backups ~/bin

echo '=== Done — WebSocket-only (no ngrok/playit/frpc needed) ==='

#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
#  TEAMOBI 2026 - TERMUX SERVER MANAGER v1.0
#  github.com/akah3674-glitch/rem5
#  One-liner: curl -fsSL https://raw.githubusercontent.com/akah3674-glitch/rem5/main/setup.sh | bash
# ============================================================

exec </dev/tty

# ── Colors ──────────────────────────────────────────────────
R='\033[0;31m'   G='\033[0;32m'   Y='\033[1;33m'
B='\033[0;34m'   C='\033[0;36m'   M='\033[0;35m'
W='\033[1;37m'   BLD='\033[1m'    NC='\033[0m'
BG_B='\033[44m'  BG_G='\033[42m'  BG_R='\033[41m'

# ── Config ───────────────────────────────────────────────────
INSTALL_DIR="$HOME/teamobi-server"
DRIVE_ID="1uH2O2FtuGpIQfIYVAhi9wcuxfDjTQddY"
DRIVE_URL="https://drive.usercontent.google.com/download?id=${DRIVE_ID}&export=download&authuser=0&confirm=t"
RAR_FILE="$INSTALL_DIR/Teamobi2026.rar"
DB_NAME="teamobi2026"
DB_USER="root"
DB_PASS="teamobi@2026"
GAME_PORT=14445
HTTP_PORT=8080
DB_PORT=3306
SETUP_FLAG="$INSTALL_DIR/.setup_done"
SRV_PID="$INSTALL_DIR/.server.pid"
LOG_DIR="$INSTALL_DIR/logs"
APK_DST="$HOME/storage/downloads/Teamobi2026.apk"
VERSION="1.0"
AUTHOR="akah3674-glitch"

# ── Helpers ──────────────────────────────────────────────────
clear_screen() { clear; }

banner() {
  echo -e "${C}${BLD}"
  echo "  ╔══════════════════════════════════════════════════════╗"
  echo "  ║         🎮  TEAMOBI 2026 SERVER MANAGER  🎮          ║"
  echo "  ║              Termux Private Server v${VERSION}              ║"
  echo "  ╚══════════════════════════════════════════════════════╝${NC}"
}

meolu_art() {
  echo -e "${Y}${BLD}"
  echo "     /\\_____/\\"
  echo "    (  ^   ^ )"
  echo "     (  =ω= )   MÈO LÙ CHÀO BẠN !"
  echo "      )     (    Server đang hoạt động~"
  echo "     (_)-(_)${NC}"
}

port_panel() {
  echo -e "\n${BLD}${W}  ┌─────────────────── BẢNG CỔNG ──────────────────────┐${NC}"

  # Check each port
  local game_status http_status db_status
  if ss -tlnp 2>/dev/null | grep -q ":${GAME_PORT}" || netstat -tlnp 2>/dev/null | grep -q ":${GAME_PORT}"; then
    game_status="${G}●  ONLINE ${NC}"
  else
    game_status="${R}○  OFFLINE${NC}"
  fi

  if ss -tlnp 2>/dev/null | grep -q ":${HTTP_PORT}" || netstat -tlnp 2>/dev/null | grep -q ":${HTTP_PORT}"; then
    http_status="${G}●  ONLINE ${NC}"
  else
    http_status="${R}○  OFFLINE${NC}"
  fi

  if ss -tlnp 2>/dev/null | grep -q ":${DB_PORT}" || netstat -tlnp 2>/dev/null | grep -q ":${DB_PORT}"; then
    db_status="${G}●  ONLINE ${NC}"
  else
    db_status="${R}○  OFFLINE${NC}"
  fi

  echo -e "${W}  │  ${C}[GAME]${NC}  Port  ${Y}${GAME_PORT}${NC}  ←→  ${game_status}${W}│${NC}"
  echo -e "${W}  │  ${C}[HTTP]${NC}  Port  ${Y}${HTTP_PORT}${NC}   ←→  ${http_status}${W} │${NC}"
  echo -e "${W}  │  ${C}[DB]  ${NC}  Port  ${Y}${DB_PORT}${NC}   ←→  ${db_status}${W} │${NC}"

  # Local IP
  local ip
  ip=$(ip route get 8.8.8.8 2>/dev/null | awk '{print $7; exit}') \
    || ip=$(ifconfig wlan0 2>/dev/null | grep 'inet ' | awk '{print $2}') \
    || ip="127.0.0.1"
  echo -e "${W}  │  ${C}[IP]  ${NC}  ${Y}${ip}${NC}                           ${W}│${NC}"
  echo -e "${W}  └──────────────────────────────────────────────────────┘${NC}"

  # Server uptime if running
  if [ -f "$SRV_PID" ]; then
    local pid; pid=$(cat "$SRV_PID" 2>/dev/null)
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      local elapsed; elapsed=$(ps -o etime= -p "$pid" 2>/dev/null | tr -d ' ')
      echo -e "  ${G}▶ Server đang chạy${NC}  PID: ${Y}${pid}${NC}  Uptime: ${Y}${elapsed:-?}${NC}"
    fi
  fi
}

info_line() { echo -e "  ${C}[INFO]${NC} $1"; }
ok_line()   { echo -e "  ${G}[OK]${NC}   $1"; }
err_line()  { echo -e "  ${R}[ERR]${NC}  $1"; }
warn_line() { echo -e "  ${Y}[WARN]${NC} $1"; }

confirm() {
  echo -e -n "  ${Y}$1 [Y/n]:${NC} "
  read -r ans
  [[ "$ans" =~ ^[Yy]?$ ]]
}

db_query() {
  mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "$1" 2>/dev/null
}

db_query_raw() {
  mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" --skip-column-names -e "$1" 2>/dev/null
}

press_enter() {
  echo -e -n "\n  ${C}Nhấn Enter để tiếp tục...${NC}"
  read -r
}

# ── Setup ─────────────────────────────────────────────────────
do_setup() {
  clear_screen; banner
  echo -e "\n${BLD}${C}  ═══ SETUP TEAMOBI 2026 SERVER ═══${NC}\n"

  if [ -f "$SETUP_FLAG" ]; then
    warn_line "Đã setup trước đó. Chạy lại setup sẽ ghi đè!"
    confirm "Tiếp tục?" || return
  fi

  mkdir -p "$INSTALL_DIR" "$LOG_DIR"
  echo Y | termux-setup-storage &>/dev/null

  # ── 1. Packages ──
  info_line "Cài packages cần thiết..."
  pkg update -y &>/dev/null
  for pkg in openjdk-17 mariadb unrar wget curl iproute2 net-tools; do
    info_line "  → Cài $pkg..."
    pkg install -y "$pkg" &>/dev/null && ok_line "$pkg OK" || warn_line "$pkg (tiếp tục)"
  done

  # ── 2. Download RAR ──
  echo ""
  info_line "Tải Teamobi2026.rar (~630MB) từ Google Drive..."
  info_line "URL: drive.usercontent.google.com (ID: ${DRIVE_ID:0:20}...)"
  echo ""

  if [ -f "$RAR_FILE" ] && [ "$(stat -c%s "$RAR_FILE" 2>/dev/null)" -gt 100000000 ]; then
    warn_line "File RAR đã tồn tại ($(du -sh "$RAR_FILE" | cut -f1)), bỏ qua download."
  else
    curl -L --max-redirs 15 --progress-bar \
      --retry 3 --retry-delay 5 \
      -C - \
      "$DRIVE_URL" \
      --output "$RAR_FILE"
    if [ ! -f "$RAR_FILE" ] || [ "$(stat -c%s "$RAR_FILE" 2>/dev/null)" -lt 1000000 ]; then
      err_line "Download thất bại! Kiểm tra kết nối và thử lại."
      press_enter; return
    fi
    ok_line "Download xong: $(du -sh "$RAR_FILE" | cut -f1)"
  fi

  # ── 3. Extract ──
  echo ""
  info_line "Giải nén Teamobi2026.rar..."
  cd "$INSTALL_DIR" || exit 1
  unrar x -o+ "$RAR_FILE" "$INSTALL_DIR/" &>/dev/null \
    || unrar e -o+ "$RAR_FILE" "$INSTALL_DIR/" &>/dev/null

  # Tìm JAR file
  local jar_game jar_login sql_file apk_src
  jar_game=$(find "$INSTALL_DIR" -name "*.jar" | grep -iE "game|srcgame|server|main" | head -1)
  jar_login=$(find "$INSTALL_DIR" -name "*.jar" | grep -iE "login|auth" | head -1)
  sql_file=$(find "$INSTALL_DIR" -name "*.sql" | head -1)
  apk_src=$(find "$INSTALL_DIR" -name "*.apk" | head -1)

  [ -z "$jar_game" ] && jar_game=$(find "$INSTALL_DIR" -name "*.jar" | head -1)

  ok_line "JAR game:  ${jar_game:-không tìm thấy}"
  ok_line "JAR login: ${jar_login:-không tìm thấy}"
  ok_line "SQL:       ${sql_file:-không tìm thấy}"
  ok_line "APK:       ${apk_src:-không tìm thấy}"

  # Lưu path JAR
  echo "JAR_GAME=${jar_game}" > "$INSTALL_DIR/.config"
  echo "JAR_LOGIN=${jar_login}" >> "$INSTALL_DIR/.config"

  # ── 4. MariaDB ──
  echo ""
  info_line "Khởi tạo MariaDB..."
  mysqld_safe --skip-grant-tables &>/dev/null &
  sleep 4

  # Đặt mật khẩu root
  mysql -u root --connect-expired-password 2>/dev/null <<SQL
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED BY '${DB_PASS}';
FLUSH PRIVILEGES;
SQL

  # Tạo DB
  mysql -u"$DB_USER" -p"$DB_PASS" 2>/dev/null <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE \`${DB_NAME}\`;
CREATE TABLE IF NOT EXISTS account (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(64) NOT NULL,
  email VARCHAR(100),
  role INT DEFAULT 0 COMMENT '0=player,1=gm,2=admin',
  status INT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS nhan_vat (
  id INT AUTO_INCREMENT PRIMARY KEY,
  account_id INT,
  name VARCHAR(50),
  level INT DEFAULT 1,
  vang BIGINT DEFAULT 0,
  ngoc BIGINT DEFAULT 0,
  exp BIGINT DEFAULT 0,
  map_id INT DEFAULT 1,
  FOREIGN KEY (account_id) REFERENCES account(id)
);
CREATE TABLE IF NOT EXISTS meo_lu (
  id INT AUTO_INCREMENT PRIMARY KEY,
  account_id INT,
  pet_name VARCHAR(50) DEFAULT 'Mèo Lù',
  pet_level INT DEFAULT 1,
  pet_exp INT DEFAULT 0,
  pet_skill VARCHAR(200) DEFAULT 'Lucky Strike',
  FOREIGN KEY (account_id) REFERENCES account(id)
);
SQL

  # Import SQL gốc nếu có
  if [ -n "$sql_file" ] && [ -f "$sql_file" ]; then
    info_line "Import SQL gốc: $(basename "$sql_file")..."
    mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$sql_file" &>/dev/null \
      && ok_line "Import SQL xong" || warn_line "Import SQL lỗi (tiếp tục)"
  fi

  pkill mysqld 2>/dev/null; sleep 2

  # ── 5. Start scripts ──
  cat > "$INSTALL_DIR/bin/start.sh" <<'STARTSH'
#!/data/data/com.termux/files/usr/bin/bash
INSTALL_DIR="$HOME/teamobi-server"
source "$INSTALL_DIR/.config"
LOG_DIR="$INSTALL_DIR/logs"
mkdir -p "$LOG_DIR"

echo "[$(date)] Starting MariaDB..."
mysqld_safe --datadir="$PREFIX/var/lib/mysql" &>/dev/null &
sleep 3

if [ -n "$JAR_LOGIN" ] && [ -f "$JAR_LOGIN" ]; then
  echo "[$(date)] Starting Login Server..."
  java -Xms128m -Xmx256m -jar "$JAR_LOGIN" \
    > "$LOG_DIR/login.log" 2>&1 &
  echo $! > "$INSTALL_DIR/.login.pid"
  sleep 2
fi

if [ -n "$JAR_GAME" ] && [ -f "$JAR_GAME" ]; then
  echo "[$(date)] Starting Game Server..."
  java -Xms256m -Xmx512m -jar "$JAR_GAME" \
    > "$LOG_DIR/game.log" 2>&1 &
  echo $! > "$INSTALL_DIR/.server.pid"
  echo "[$(date)] Game Server PID: $(cat $INSTALL_DIR/.server.pid)"
fi
echo "[$(date)] All servers started."
STARTSH

  cat > "$INSTALL_DIR/bin/stop.sh" <<'STOPSH'
#!/data/data/com.termux/files/usr/bin/bash
INSTALL_DIR="$HOME/teamobi-server"
for pidfile in "$INSTALL_DIR/.server.pid" "$INSTALL_DIR/.login.pid"; do
  if [ -f "$pidfile" ]; then
    pid=$(cat "$pidfile")
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" && echo "[$(date)] Stopped PID $pid"
    fi
    rm -f "$pidfile"
  fi
done
pkill -f "mysqld_safe" 2>/dev/null
pkill -f "mysqld" 2>/dev/null
echo "[$(date)] All servers stopped."
STOPSH

  mkdir -p "$INSTALL_DIR/bin"
  chmod +x "$INSTALL_DIR/bin/start.sh" "$INSTALL_DIR/bin/stop.sh"

  # ── 6. APK → Downloads ──
  if [ -n "$apk_src" ] && [ -f "$apk_src" ]; then
    mkdir -p "$HOME/storage/downloads/game_download" 2>/dev/null
    cp "$apk_src" "$HOME/storage/downloads/game_download/Teamobi2026.apk" 2>/dev/null \
      && ok_line "APK → ~/storage/downloads/game_download/Teamobi2026.apk"
  fi

  touch "$SETUP_FLAG"
  echo ""
  ok_line "${BLD}Setup hoàn tất! Vào menu để chạy server.${NC}"
  press_enter
}

# ── Start Server ──────────────────────────────────────────────
do_start() {
  clear_screen; banner
  echo -e "\n${G}${BLD}  ═══ CHẠY SERVER ═══${NC}\n"

  if [ ! -f "$SETUP_FLAG" ]; then
    err_line "Chưa setup! Vào Menu 1 để setup trước."
    press_enter; return
  fi

  # Check if already running
  if [ -f "$SRV_PID" ]; then
    local pid; pid=$(cat "$SRV_PID")
    if kill -0 "$pid" 2>/dev/null; then
      warn_line "Server đã chạy! PID: $pid"
      press_enter; return
    fi
  fi

  info_line "Khởi động MariaDB..."
  mysqld_safe --datadir="$PREFIX/var/lib/mysql" &>/dev/null &
  sleep 3; ok_line "MariaDB OK"

  source "$INSTALL_DIR/.config" 2>/dev/null

  if [ -n "$JAR_LOGIN" ] && [ -f "$JAR_LOGIN" ]; then
    info_line "Khởi động Login Server (port: dò tự động)..."
    java -Xms128m -Xmx256m -jar "$JAR_LOGIN" \
      > "$LOG_DIR/login.log" 2>&1 &
    echo $! > "$INSTALL_DIR/.login.pid"
    sleep 2
    ok_line "Login Server PID: $(cat $INSTALL_DIR/.login.pid)"
  fi

  if [ -n "$JAR_GAME" ] && [ -f "$JAR_GAME" ]; then
    info_line "Khởi động Game Server (port: ${GAME_PORT})..."
    java -Xms256m -Xmx512m -jar "$JAR_GAME" \
      > "$LOG_DIR/game.log" 2>&1 &
    echo $! > "$SRV_PID"
    sleep 2
    ok_line "Game Server PID: $(cat $SRV_PID)"
  else
    err_line "Không tìm thấy JAR! Chạy lại Setup."
    press_enter; return
  fi

  echo ""
  ok_line "${BLD}Server đang chạy!${NC}"
  port_panel
  press_enter
}

# ── Stop Server ───────────────────────────────────────────────
do_stop() {
  clear_screen; banner
  echo -e "\n${R}${BLD}  ═══ TẮT SERVER ═══${NC}\n"

  local stopped=0
  for pidfile in "$SRV_PID" "$INSTALL_DIR/.login.pid"; do
    if [ -f "$pidfile" ]; then
      local pid; pid=$(cat "$pidfile")
      if kill -0 "$pid" 2>/dev/null; then
        info_line "Dừng PID $pid..."
        kill "$pid" && ok_line "Đã dừng PID $pid" || err_line "Không dừng được PID $pid"
        stopped=1
      fi
      rm -f "$pidfile"
    fi
  done

  pkill -f "mysqld_safe" 2>/dev/null
  pkill -f "mysqld" 2>/dev/null
  ok_line "MariaDB đã dừng"

  if [ $stopped -eq 0 ]; then
    info_line "Không có server nào đang chạy."
  fi

  press_enter
}

# ── Mèo Lù ────────────────────────────────────────────────────
do_meolu() {
  clear_screen; banner
  meolu_art
  echo ""
  echo -e "${M}${BLD}  ═══ MÈO LÙ MANAGER ═══${NC}"
  echo -e "  ${C}Quản lý pet đặc biệt: Mèo Lù${NC}\n"
  echo -e "  ${Y}1.${NC} Thêm Mèo Lù cho nhân vật"
  echo -e "  ${Y}2.${NC} Xem danh sách Mèo Lù"
  echo -e "  ${Y}3.${NC} Nâng cấp Mèo Lù (level)"
  echo -e "  ${Y}4.${NC} Đổi tên Mèo Lù"
  echo -e "  ${Y}0.${NC} Quay lại"
  echo ""
  echo -e -n "  ${C}Chọn [0-4]:${NC} "
  read -r choice

  case $choice in
    1)
      echo -e -n "  ${C}Nhập tên nhân vật:${NC} "
      read -r char_name
      local acc_id
      acc_id=$(db_query_raw "SELECT a.id FROM account a JOIN nhan_vat n ON n.account_id=a.id WHERE n.name='${char_name}' LIMIT 1;")
      if [ -z "$acc_id" ]; then
        err_line "Không tìm thấy nhân vật: ${char_name}"
      else
        db_query "INSERT INTO meo_lu (account_id, pet_name, pet_level) VALUES (${acc_id}, 'Mèo Lù', 1) ON DUPLICATE KEY UPDATE pet_level=pet_level;"
        ok_line "Đã thêm Mèo Lù cho ${char_name}!"
      fi
      ;;
    2)
      echo -e "\n${BLD}  Danh sách Mèo Lù:${NC}"
      db_query "SELECT m.id, n.name, m.pet_name, m.pet_level, m.pet_skill FROM meo_lu m JOIN nhan_vat n ON n.account_id=m.account_id;" 2>/dev/null || echo "  (chưa có dữ liệu)"
      ;;
    3)
      echo -e -n "  ${C}Nhập tên nhân vật:${NC} "
      read -r char_name
      echo -e -n "  ${C}Level muốn đặt:${NC} "
      read -r lv
      db_query "UPDATE meo_lu m JOIN nhan_vat n ON n.account_id=m.account_id SET m.pet_level=${lv:-1} WHERE n.name='${char_name}';"
      ok_line "Đã nâng Mèo Lù của ${char_name} lên lv ${lv}!"
      ;;
    4)
      echo -e -n "  ${C}Nhập tên nhân vật:${NC} "
      read -r char_name
      echo -e -n "  ${C}Tên mới cho Mèo Lù:${NC} "
      read -r newname
      db_query "UPDATE meo_lu m JOIN nhan_vat n ON n.account_id=m.account_id SET m.pet_name='${newname}' WHERE n.name='${char_name}';"
      ok_line "Đã đổi tên Mèo Lù thành: ${newname}"
      ;;
    0) return ;;
  esac
  press_enter
}

# ── Đăng ký tài khoản ────────────────────────────────────────
do_register() {
  clear_screen; banner
  echo -e "\n${BLD}${C}  ═══ ĐĂNG KÝ TÀI KHOẢN ═══${NC}\n"

  echo -e -n "  ${C}Tên đăng nhập:${NC} "
  read -r username
  if [ -z "$username" ]; then err_line "Không được để trống!"; press_enter; return; fi

  echo -e -n "  ${C}Mật khẩu:${NC} "
  read -rs password; echo
  if [ -z "$password" ]; then err_line "Không được để trống!"; press_enter; return; fi

  echo -e -n "  ${C}Email (có thể bỏ qua):${NC} "
  read -r email

  echo -e "\n  Phân quyền:"
  echo -e "  ${Y}0.${NC} Người chơi (Player)"
  echo -e "  ${Y}1.${NC} Game Master (GM)"
  echo -e "  ${Y}2.${NC} Admin (toàn quyền)"
  echo -e -n "  ${C}Chọn quyền [0-2]:${NC} "
  read -r role
  role=${role:-0}

  # Hash MD5
  local hashed
  hashed=$(echo -n "$password" | md5sum | cut -d' ' -f1)

  info_line "Tạo tài khoản: ${username} | role: ${role}..."
  local result
  result=$(db_query "INSERT INTO account (username, password, email, role) VALUES ('${username}','${hashed}','${email:-}',${role});" 2>&1)

  if echo "$result" | grep -q "Duplicate"; then
    err_line "Tên đăng nhập đã tồn tại!"
  else
    ok_line "Tài khoản '${username}' đã được tạo!"
    [ "$role" -ge 1 ] && ok_line "Quyền: $([ "$role" -eq 2 ] && echo 'ADMIN' || echo 'GM')"

    # Tạo nhân vật mặc định
    echo -e -n "  ${C}Tạo nhân vật mặc định? [Y/n]:${NC} "
    read -r mk_char
    if [[ "$mk_char" =~ ^[Yy]?$ ]]; then
      echo -e -n "  ${C}Tên nhân vật:${NC} "
      read -r charname
      local acc_id
      acc_id=$(db_query_raw "SELECT id FROM account WHERE username='${username}' LIMIT 1;")
      if [ -n "$acc_id" ] && [ -n "$charname" ]; then
        db_query "INSERT INTO nhan_vat (account_id, name, level) VALUES (${acc_id},'${charname}',1);"
        ok_line "Nhân vật '${charname}' đã được tạo!"
      fi
    fi
  fi
  press_enter
}

# ── Thêm vàng ngọc ────────────────────────────────────────────
do_gold_gem() {
  clear_screen; banner
  echo -e "\n${BLD}${Y}  ═══ THÊM VÀNG / NGỌC ═══${NC}\n"

  echo -e -n "  ${C}Nhập tên nhân vật (hoặc 'all' cho tất cả):${NC} "
  read -r target

  if [ "$target" = "all" ]; then
    echo -e -n "  ${C}Số vàng thêm (0 để bỏ qua):${NC} "
    read -r vang
    echo -e -n "  ${C}Số ngọc thêm (0 để bỏ qua):${NC} "
    read -r ngoc

    [ "${vang:-0}" -gt 0 ] && db_query "UPDATE nhan_vat SET vang = vang + ${vang};"
    [ "${ngoc:-0}" -gt 0 ] && db_query "UPDATE nhan_vat SET ngoc = ngoc + ${ngoc};"
    ok_line "Đã thêm ${vang:-0} vàng + ${ngoc:-0} ngọc cho TẤT CẢ nhân vật!"
  else
    # Check nhân vật tồn tại
    local char_id
    char_id=$(db_query_raw "SELECT id FROM nhan_vat WHERE name='${target}' LIMIT 1;")
    if [ -z "$char_id" ]; then
      err_line "Không tìm thấy nhân vật: ${target}"
      press_enter; return
    fi

    local cur_info
    cur_info=$(db_query_raw "SELECT name, level, vang, ngoc FROM nhan_vat WHERE id=${char_id};")
    info_line "Nhân vật hiện tại: ${cur_info}"
    echo ""

    echo -e -n "  ${C}Số vàng thêm (0 để bỏ qua):${NC} "
    read -r vang
    echo -e -n "  ${C}Số ngọc thêm (0 để bỏ qua):${NC} "
    read -r ngoc

    [ "${vang:-0}" -gt 0 ] && db_query "UPDATE nhan_vat SET vang = vang + ${vang} WHERE id=${char_id};"
    [ "${ngoc:-0}" -gt 0 ] && db_query "UPDATE nhan_vat SET ngoc = ngoc + ${ngoc} WHERE id=${char_id};"

    local new_info
    new_info=$(db_query_raw "SELECT name, level, vang, ngoc FROM nhan_vat WHERE id=${char_id};")
    ok_line "Cập nhật xong!"
    info_line "Sau khi thêm: ${new_info}"
  fi
  press_enter
}

# ── Xem log ──────────────────────────────────────────────────
do_logs() {
  clear_screen; banner
  echo -e "\n${BLD}${C}  ═══ LOGS SERVER ═══${NC}\n"
  echo -e "  ${Y}1.${NC} Game Server Log (tail -50)"
  echo -e "  ${Y}2.${NC} Login Server Log (tail -50)"
  echo -e "  ${Y}3.${NC} Xem log realtime (Ctrl+C để thoát)"
  echo -e "  ${Y}0.${NC} Quay lại"
  echo ""
  echo -e -n "  ${C}Chọn [0-3]:${NC} "
  read -r choice
  case $choice in
    1) tail -50 "$LOG_DIR/game.log" 2>/dev/null || echo "Chưa có log game."; press_enter ;;
    2) tail -50 "$LOG_DIR/login.log" 2>/dev/null || echo "Chưa có log login."; press_enter ;;
    3) tail -f "$LOG_DIR/game.log" 2>/dev/null & tail -f "$LOG_DIR/login.log" 2>/dev/null; wait ;;
    0) return ;;
  esac
}

# ── Danh sách tài khoản ──────────────────────────────────────
do_list_accounts() {
  clear_screen; banner
  echo -e "\n${BLD}${C}  ═══ DANH SÁCH TÀI KHOẢN ═══${NC}\n"
  db_query "SELECT id, username, role, status, created_at FROM account ORDER BY id DESC LIMIT 30;" 2>/dev/null \
    || echo "  (Chưa có dữ liệu hoặc DB chưa khởi động)"
  echo ""
  echo -e "  ${Y}1.${NC} Đổi mật khẩu tài khoản"
  echo -e "  ${Y}2.${NC} Đổi quyền tài khoản"
  echo -e "  ${Y}3.${NC} Xóa tài khoản"
  echo -e "  ${Y}0.${NC} Quay lại"
  echo -e -n "\n  ${C}Chọn [0-3]:${NC} "
  read -r sub

  case $sub in
    1)
      echo -e -n "  ${C}Tên tài khoản:${NC} " ; read -r u
      echo -e -n "  ${C}Mật khẩu mới:${NC} " ; read -rs p; echo
      local h; h=$(echo -n "$p" | md5sum | cut -d' ' -f1)
      db_query "UPDATE account SET password='${h}' WHERE username='${u}';"
      ok_line "Đã đổi mật khẩu cho ${u}."
      ;;
    2)
      echo -e -n "  ${C}Tên tài khoản:${NC} " ; read -r u
      echo -e -n "  ${C}Quyền mới (0=player,1=gm,2=admin):${NC} " ; read -r r
      db_query "UPDATE account SET role=${r:-0} WHERE username='${u}';"
      ok_line "Đã đổi quyền ${u} → ${r}."
      ;;
    3)
      echo -e -n "  ${C}Tên tài khoản cần xóa:${NC} " ; read -r u
      confirm "Xác nhận xóa tài khoản '${u}'?" || { press_enter; return; }
      db_query "DELETE FROM account WHERE username='${u}';"
      ok_line "Đã xóa tài khoản ${u}."
      ;;
    0) return ;;
  esac
  press_enter
}

# ── Main Menu ─────────────────────────────────────────────────
main_menu() {
  while true; do
    clear_screen
    banner

    # Setup status
    if [ -f "$SETUP_FLAG" ]; then
      local setup_indicator="${G}● Đã setup${NC}"
    else
      local setup_indicator="${R}○ Chưa setup${NC}"
    fi

    port_panel
    echo ""
    echo -e "  Status: ${setup_indicator}"
    echo ""
    echo -e "  ${BLD}${W}══════════════ MENU ══════════════${NC}"
    echo -e "  ${Y}1.${NC}  Setup Server (lần đầu)"
    echo -e "  ${G}2.${NC}  Chạy Server ▶"
    echo -e "  ${R}3.${NC}  Tắt Server ■"
    echo -e "  ${M}4.${NC}  Mèo Lù 🐱 (Pet Manager)"
    echo -e "  ${C}5.${NC}  Đăng ký Tài khoản + Quyền"
    echo -e "  ${Y}6.${NC}  Thêm Vàng / Ngọc"
    echo -e "  ${B}7.${NC}  Danh sách Tài khoản"
    echo -e "  ${W}8.${NC}  Xem Logs"
    echo -e "  ${W}0.${NC}  Thoát"
    echo -e "  ${BLD}${W}══════════════════════════════════${NC}"
    echo ""
    echo -e -n "  ${C}Chọn [0-8]:${NC} "
    read -r choice

    case $choice in
      1) do_setup ;;
      2) do_start ;;
      3) do_stop ;;
      4) do_meolu ;;
      5) do_register ;;
      6) do_gold_gem ;;
      7) do_list_accounts ;;
      8) do_logs ;;
      0)
        echo -e "\n  ${C}Tạm biệt! Teamobi 2026 Server 🐱${NC}\n"
        exit 0
        ;;
      *)
        warn_line "Lựa chọn không hợp lệ!"
        sleep 1
        ;;
    esac
  done
}

# ── Entry Point ───────────────────────────────────────────────
main_menu

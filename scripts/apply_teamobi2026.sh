#!/bin/bash
# ============================================================
# Script áp dụng tính năng Teamobi2026 lên NRO SRC-Team
# Chạy trên CODESPACE (không phải Replit)
# ============================================================

set -e

NRO_DIR="${NRO_DIR:-/home/codespace/nro/SRC}"
DB_NAME="${DB_NAME:-nro1}"
DB_USER="${DB_USER:-root}"
BACKUP_DIR="/backup/nro_upgrades"

echo "======================================================"
echo " NRO UPGRADE: Teamobi2026 Features"
echo " Server dir: $NRO_DIR"
echo " Database:   $DB_NAME"
echo "======================================================"

# Tạo thư mục backup
mkdir -p "$BACKUP_DIR"

# ===== BƯỚC 0: BACKUP =====
echo ""
echo "[BƯỚC 0] Backup database..."
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/nro1_before_teamobi_${TIMESTAMP}.sql"
mysqldump -u "$DB_USER" "$DB_NAME" > "$BACKUP_FILE"
echo "  ✅ Backup: $BACKUP_FILE ($(du -sh $BACKUP_FILE | cut -f1))"

# ===== BƯỚC 1: Import bảng mới =====
echo ""
echo "[BƯỚC 1] Import bảng database mới từ Teamobi2026..."
mysql -u "$DB_USER" "$DB_NAME" < "$(dirname $0)/../docs/teamobi2026_new_tables.sql"
echo "  ✅ 7 bảng mới: achievement_template, array_head_2_frames, bg_item_template,"
echo "               clan_task_template, data_badges, task_badges_template, radar"

# ===== BƯỚC 2: ALTER TABLE player =====
echo ""
echo "[BƯỚC 2] Thêm cột mới vào bảng player..."
mysql -u "$DB_USER" "$DB_NAME" < "$(dirname $0)/../docs/teamobi2026_alter_player.sql"
echo "  ✅ 40 cột mới thêm vào player (IF NOT EXISTS)"

# ===== BƯỚC 3: Copy Java files =====
echo ""
echo "[BƯỚC 3] Copy Java source files vào $NRO_DIR/src..."

SRC_DIR="$(dirname $0)/../docs/teamobi2026_src"
JAVA_SRC="$NRO_DIR/src/nro/models"

# Event bosses
mkdir -p "$JAVA_SRC/boss/event/Halloween"
mkdir -p "$JAVA_SRC/boss/event_noel"
mkdir -p "$JAVA_SRC/boss/event_tet"
mkdir -p "$JAVA_SRC/boss/event_trung_thu"
mkdir -p "$JAVA_SRC/boss/event_hung_vuong"
mkdir -p "$JAVA_SRC/boss/cumber"
mkdir -p "$JAVA_SRC/boss/Baby"
mkdir -p "$JAVA_SRC/boss/trai_dat"
mkdir -p "$JAVA_SRC/boss/Golden_fireza"
mkdir -p "$JAVA_SRC/consts"

cp "$SRC_DIR/boss_events/BiMa.java"       "$JAVA_SRC/boss/event/Halloween/"
cp "$SRC_DIR/boss_events/Doi.java"         "$JAVA_SRC/boss/event/Halloween/"
cp "$SRC_DIR/boss_events/MaTroi.java"      "$JAVA_SRC/boss/event/Halloween/"
cp "$SRC_DIR/boss_events/OngGiaNoel.java"  "$JAVA_SRC/boss/event_noel/"
cp "$SRC_DIR/boss_events/LanCon.java"      "$JAVA_SRC/boss/event_tet/"
cp "$SRC_DIR/boss_events/KhiDot.java"      "$JAVA_SRC/boss/event_trung_thu/"
cp "$SRC_DIR/boss_events/NguyetThan.java"  "$JAVA_SRC/boss/event_trung_thu/"
cp "$SRC_DIR/boss_events/NhatThan.java"    "$JAVA_SRC/boss/event_trung_thu/"
cp "$SRC_DIR/boss_events/SonTinh.java"     "$JAVA_SRC/boss/event_hung_vuong/"
cp "$SRC_DIR/boss_events/ThuyTinh.java"    "$JAVA_SRC/boss/event_hung_vuong/"
cp "$SRC_DIR/boss_new/Cumber.java"         "$JAVA_SRC/boss/cumber/"
cp "$SRC_DIR/boss_new/Baby.java"           "$JAVA_SRC/boss/Baby/"
cp "$SRC_DIR/boss_bojack/BUJIN.java"       "$JAVA_SRC/boss/trai_dat/"
cp "$SRC_DIR/boss_bojack/KOGU.java"        "$JAVA_SRC/boss/trai_dat/"
cp "$SRC_DIR/boss_bojack/ZANGYA.java"      "$JAVA_SRC/boss/trai_dat/"
cp "$SRC_DIR/boss_bojack/BIDO.java"        "$JAVA_SRC/boss/trai_dat/"
cp "$SRC_DIR/boss_bojack/BOJACK.java"      "$JAVA_SRC/boss/trai_dat/"
cp "$SRC_DIR/boss_bojack/SUPER_BOJACK.java" "$JAVA_SRC/boss/trai_dat/"
cp "$SRC_DIR/boss_golden_frieza/GoldenFrieza.java" "$JAVA_SRC/boss/Golden_fireza/"
cp "$SRC_DIR/boss_golden_frieza/DeathBeam1.java"   "$JAVA_SRC/boss/Golden_fireza/"
cp "$SRC_DIR/const/ConstMap.java"          "$JAVA_SRC/consts/"

echo "  ✅ Đã copy Java files"

# ===== BƯỚC 4: Biên dịch =====
echo ""
echo "[BƯỚC 4] Biên dịch Java files mới..."
cd "$NRO_DIR"

# Biên dịch từng nhóm
mkdir -p "$NRO_DIR/build"
javac -cp "$NRO_DIR/NgocRongOnline.jar:$NRO_DIR/lib/*" -d "$NRO_DIR/build/" \
    "$NRO_DIR/src/nro/models/boss/event/Halloween/BiMa.java" \
    "$NRO_DIR/src/nro/models/boss/event/Halloween/Doi.java" \
    "$NRO_DIR/src/nro/models/boss/event/Halloween/MaTroi.java" \
    "$NRO_DIR/src/nro/models/boss/event_noel/OngGiaNoel.java" \
    "$NRO_DIR/src/nro/models/boss/event_tet/LanCon.java" \
    "$NRO_DIR/src/nro/models/boss/event_trung_thu/KhiDot.java" \
    "$NRO_DIR/src/nro/models/boss/event_trung_thu/NguyetThan.java" \
    "$NRO_DIR/src/nro/models/boss/event_trung_thu/NhatThan.java" \
    "$NRO_DIR/src/nro/models/boss/event_hung_vuong/SonTinh.java" \
    "$NRO_DIR/src/nro/models/boss/event_hung_vuong/ThuyTinh.java" \
    "$NRO_DIR/src/nro/models/boss/cumber/Cumber.java" \
    "$NRO_DIR/src/nro/models/boss/Baby/Baby.java" \
    "$NRO_DIR/src/nro/models/boss/trai_dat/BUJIN.java" \
    "$NRO_DIR/src/nro/models/boss/trai_dat/KOGU.java" \
    "$NRO_DIR/src/nro/models/boss/trai_dat/ZANGYA.java" \
    "$NRO_DIR/src/nro/models/boss/trai_dat/BIDO.java" \
    "$NRO_DIR/src/nro/models/boss/trai_dat/BOJACK.java" \
    "$NRO_DIR/src/nro/models/boss/trai_dat/SUPER_BOJACK.java" \
    "$NRO_DIR/src/nro/models/boss/Golden_fireza/GoldenFrieza.java" \
    "$NRO_DIR/src/nro/models/boss/Golden_fireza/DeathBeam1.java" \
    2>&1 || {
        echo "  ⚠️  Lỗi biên dịch (có thể do class phụ thuộc chưa có)"
        echo "     Xem log trên để biết chi tiết"
        echo "     Bỏ qua lỗi này nếu class phụ thuộc sẽ thêm sau"
    }

# ===== BƯỚC 4b: Update JAR =====
echo ""
echo "[BƯỚC 4b] Update class files vào NgocRongOnline.jar..."
cd "$NRO_DIR"
cp NgocRongOnline.jar NgocRongOnline.jar.bak_$(date +%Y%m%d_%H%M%S)
jar uf NgocRongOnline.jar -C build/ nro/
echo "  ✅ JAR đã cập nhật"

echo ""
echo "======================================================"
echo " HOÀN TẤT! Các bước tiếp theo thủ công:"
echo "======================================================"
echo " 1. Cập nhật BossID.java: thêm constants từ docs/teamobi2026_src/BossID.java"
echo " 2. Cập nhật BossesData.java: merge data từ docs/teamobi2026_src/BossesData.java"
echo " 3. Cập nhật BossManager.java: đăng ký boss mới vào spawn schedule"
echo " 4. Restart server: pkill -f NgocRongOnline; sleep 2; cd $NRO_DIR; nohup java -Xms512m -Xmx1g -XX:+UseG1GC -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &"
echo ""
echo " Đọc docs/NRO_UPGRADE_PLAN_TEAMOBI2026.md để biết chi tiết từng bước."

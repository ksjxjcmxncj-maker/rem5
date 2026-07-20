#!/bin/bash
# =============================================================================
# NRO SERVER UPGRADE SCRIPT — Push SRC-Team data to Codespace
# =============================================================================
# Dùng: bash scripts/nro_push_upgrade.sh [option]
#
# Options:
#   full      — Import toàn bộ game data (mặc định)
#   skills    — Chỉ import skill_template
#   items     — Chỉ import item_template + item_option_template
#   mobs      — Chỉ import mob_template
#   maps      — Chỉ import map_template
#   npcs      — Chỉ import npc_template
#   caitrang  — Chỉ import cai_trang
#   powerlimit — Chỉ import power_limit
#   backup    — Chỉ backup DB hiện tại
#   deploy-jar — Compile và deploy JAR mới
# =============================================================================

set -e

# === CONFIG ===
CODESPACE_HOST="${CODESPACE_HOST:-}"        # Tên Codespace host
DB_NAME="${DB_NAME:-nro1}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
NRO_DIR="${NRO_DIR:-/opt/nro}"
SQL_FILE="docs/nro_upgrade_data.sql"
FULL_SQL="docs/nro_srcteam.sql"

# === COLORS ===
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_err()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# === DETECT Codespace SSH ===
detect_codespace() {
    if [ -n "$CODESPACE_HOST" ]; then
        log_info "Codespace: $CODESPACE_HOST"
        return 0
    fi
    
    # Check GitHub Codespace environment
    if [ -n "$CODESPACE_NAME" ]; then
        CODESPACE_HOST="$CODESPACE_NAME"
        log_info "Detected Codespace: $CODESPACE_HOST"
        return 0
    fi
    
    log_warn "CODESPACE_HOST not set. Running locally or set env var."
    return 1
}

# === RUN on Codespace ===
run_remote() {
    if detect_codespace 2>/dev/null; then
        ssh -o StrictHostKeyChecking=no "$CODESPACE_HOST" "$@"
    else
        # Run locally (on the Codespace itself)
        bash -c "$@"
    fi
}

# === MySQL command ===
mysql_cmd() {
    local sql="$1"
    if [ -n "$DB_PASS" ]; then
        mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "$sql"
    else
        mysql -u"$DB_USER" "$DB_NAME" -e "$sql"
    fi
}

mysql_file() {
    local file="$1"
    if [ -n "$DB_PASS" ]; then
        mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$file"
    else
        mysql -u"$DB_USER" "$DB_NAME" < "$file"
    fi
}

# === BACKUP ===
do_backup() {
    log_info "Backing up database $DB_NAME..."
    local backup_file="backup_nro_$(date +%Y%m%d_%H%M%S).sql"
    
    if [ -n "$DB_PASS" ]; then
        mysqldump -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" > "$backup_file"
    else
        mysqldump -u"$DB_USER" "$DB_NAME" > "$backup_file"
    fi
    
    log_ok "Backup saved: $backup_file ($(du -sh $backup_file | cut -f1))"
}

# === EXTRACT single table SQL ===
extract_table_sql() {
    local table="$1"
    local tmpfile="/tmp/nro_upgrade_${table}.sql"
    
    python3 << EOF
import re
with open('$SQL_FILE', 'r', encoding='utf-8') as f:
    content = f.read()

# Find CREATE TABLE
create = re.search(r'CREATE TABLE \`$table\`.*?;', content, re.DOTALL)
# Find all INSERT INTO
inserts = re.findall(r'INSERT INTO \`$table\`.*?;', content, re.DOTALL)

output = "SET NAMES utf8mb4;\n"
if create:
    output += f"DROP TABLE IF EXISTS \`$table\`;\n"
    output += create.group(0) + "\n"
for ins in inserts:
    output += ins + "\n"

with open('$tmpfile', 'w', encoding='utf-8') as f:
    f.write(output)

print(f"Extracted {len(inserts)} INSERT blocks for table $table")
EOF
    
    echo "$tmpfile"
}

# === IMPORT single table ===
import_table() {
    local table="$1"
    log_info "Importing table: $table"
    local tmpfile=$(extract_table_sql "$table")
    mysql_file "$tmpfile"
    log_ok "Done: $table"
}

# === FULL UPGRADE ===
do_full_upgrade() {
    log_info "Starting FULL upgrade..."
    log_warn "This will DROP and recreate game data tables!"
    echo -n "Continue? (y/N): "
    read -r confirm
    [ "$confirm" != "y" ] && [ "$confirm" != "Y" ] && { log_info "Aborted."; exit 0; }
    
    do_backup
    
    log_info "Importing upgrade SQL ($SQL_FILE)..."
    mysql_file "$SQL_FILE"
    
    log_ok "FULL upgrade complete!"
    show_stats
}

# === SHOW STATS ===
show_stats() {
    log_info "Database stats after upgrade:"
    
    for table in skill_template item_template mob_template npc_template map_template cai_trang mini_pet power_limit; do
        count=$(mysql_cmd "SELECT COUNT(*) FROM $table;" 2>/dev/null | tail -1 || echo "N/A")
        echo "  $table: $count rows"
    done
}

# === DEPLOY JAR ===
do_deploy_jar() {
    log_info "Building JAR from SRC-Team source..."
    
    local src_dir="/tmp/nro_extracted/SRC-Team/Soucre"
    
    if [ ! -d "$src_dir" ]; then
        log_err "Source not found at $src_dir. Please extract the RAR file first."
    fi
    
    cd "$src_dir"
    
    log_info "Running Maven build..."
    mvn clean package -DskipTests -q
    
    local jar_file=$(find target -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" | head -1)
    
    if [ -z "$jar_file" ]; then
        log_err "JAR not found after build"
    fi
    
    log_ok "Built: $jar_file"
    
    # Backup current JAR
    log_info "Backing up current JAR..."
    run_remote "cp ${NRO_DIR}/nro.jar ${NRO_DIR}/nro_backup_$(date +%Y%m%d).jar 2>/dev/null || true"
    
    # Copy new JAR
    log_info "Deploying new JAR..."
    if detect_codespace 2>/dev/null; then
        scp "$jar_file" "$CODESPACE_HOST:${NRO_DIR}/nro.jar"
    else
        cp "$jar_file" "${NRO_DIR}/nro.jar"
    fi
    
    log_ok "JAR deployed! Restart server to apply."
    log_warn "Run: systemctl restart nro  OR  bash scripts/keepalive_codespace.sh"
}

# === MAIN ===
OPTION="${1:-full}"

echo ""
echo "═══════════════════════════════════════════════"
echo "  NRO SERVER UPGRADE — Option: $OPTION"
echo "═══════════════════════════════════════════════"
echo ""

case "$OPTION" in
    full)
        do_full_upgrade
        ;;
    skills)
        import_table "skill_template"
        ;;
    items)
        import_table "item_option_template"
        import_table "part"
        import_table "item_template"
        ;;
    mobs)
        import_table "mob_template"
        ;;
    maps)
        import_table "map_template"
        ;;
    npcs)
        import_table "npc_template"
        ;;
    caitrang)
        import_table "cai_trang"
        ;;
    powerlimit)
        import_table "power_limit"
        ;;
    minipet)
        import_table "mini_pet"
        ;;
    collection)
        import_table "collection_book"
        ;;
    shop)
        import_table "shop"
        import_table "tab_shop"
        import_table "item_shop"
        import_table "item_shop_option"
        ;;
    attribute)
        import_table "attribute_template"
        import_table "attribute_server"
        ;;
    intrinsic)
        import_table "intrinsic"
        ;;
    backup)
        do_backup
        ;;
    stats)
        show_stats
        ;;
    deploy-jar)
        do_deploy_jar
        ;;
    *)
        echo "Usage: $0 [full|skills|items|mobs|maps|npcs|caitrang|powerlimit|minipet|collection|shop|attribute|intrinsic|backup|stats|deploy-jar]"
        exit 1
        ;;
esac

echo ""
log_ok "Done!"

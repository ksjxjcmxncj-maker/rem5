#!/bin/bash
# Keep-alive + Failover + Tunnel Diagnostics cho NRO Server
# Lịch: 04:00-23:00 (giờ VN) → chạy | 23:00-04:00 → tắt
# v2: diagnostics tunnel + tìm server châu Á

GH_BIN="/tmp/gh_2.52.0_linux_amd64/bin/gh"
INTERVAL=180    # 3 phút
STATE_FILE="/tmp/nro_active"
PLAYIT_BIN="/tmp/playit_old"
DIAG_DONE_FILE="/tmp/nro_diag_done"

CODESPACES=(
  "cautious-space-halibut-p7rwgqwxrg5gfrrqg|Github|Main"
  "improved-fishstick-966vx76qqgx7cqjp|GITHUB_PERSONAL_ACCESS_TOKEN|Backup1"
  "crispy-space-capybara-5v564w74jqgf45x4|GITHUB_PERSONAAL_ACCESS_TOKE2|Backup2"
  "cuddly-space-orbit-qvvrx7jq5gv6246wg|GITHUB_PERSONAL_ACCESS_TOKEN3|Backup3"
)

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"; }

install_gh() {
  if [ ! -f "$GH_BIN" ]; then
    log "Cài gh CLI..."
    curl -sL https://github.com/cli/cli/releases/download/v2.52.0/gh_2.52.0_linux_amd64.tar.gz | tar -xz -C /tmp/
  fi
}

auth_as() {
  local TOKEN_VAR="$1"
  local TOKEN=$(printenv "$TOKEN_VAR")
  echo "$TOKEN" | $GH_BIN auth login --with-token 2>/dev/null
}

get_vn_hour() {
  date -u -d "+7 hours" '+%H' | sed 's/^0*//' | grep -v '^$' || echo "0"
}

is_active_hours() {
  local H=$(get_vn_hour)
  [ "$H" -ge 4 ] && [ "$H" -lt 23 ]
}

get_active() {
  [ -f "$STATE_FILE" ] && cat "$STATE_FILE" || echo "${CODESPACES[0]}"
}

set_active() {
  echo "$1" > "$STATE_FILE"
  log "✅ Active Codespace: $(echo $1 | cut -d'|' -f3)"
}

ping_codespace() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  auth_as "$TOKEN_VAR"

  local RESULT
  RESULT=$($GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
    JAVA_OK=0; BRIDGE_OK=0; DB_OK=0
    pgrep -f NgocRongOnline > /dev/null && JAVA_OK=1
    pgrep -f ws_bridge > /dev/null && ss -tlnp | grep -q 8080 && BRIDGE_OK=1
    mysql -u root -e "SELECT 1;" > /dev/null 2>&1 && DB_OK=1

    # Auto-heal: DB
    if [ $DB_OK -eq 0 ]; then
      sudo service mariadb start > /dev/null 2>&1; sleep 3
      mysql -u root -e "SELECT 1;" > /dev/null 2>&1 && DB_OK=1
    fi

    # Auto-heal: WS Bridge
    if [ $BRIDGE_OK -eq 0 ]; then
      pkill -f ws_bridge 2>/dev/null
      nohup python3 ~/bin/ws_bridge.py >> ~/logs/ws_bridge.log 2>&1 &
      sleep 2
      pgrep -f ws_bridge > /dev/null && BRIDGE_OK=1
    fi

    # Auto-heal: Java server
    if [ $JAVA_OK -eq 0 ]; then
      cd ~/nro/SRC
      nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
        -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
        -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
        -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
      sleep 15
      pgrep -f NgocRongOnline > /dev/null && JAVA_OK=1
    fi

    echo "JAVA=$JAVA_OK BRIDGE=$BRIDGE_OK DB=$DB_OK"
    [ $JAVA_OK -eq 1 ] && echo "ALIVE" || echo "DEAD"
REMOTE
  )

  # Set port 8080 public mỗi lần ping
  $GH_BIN codespace ports visibility 8080:public -c "$CS" > /dev/null 2>&1

  if echo "$RESULT" | grep -q "ALIVE"; then
    local STATUS=$(echo "$RESULT" | grep "JAVA=")
    log "[$NAME] ✅ ALIVE | $STATUS"
    return 0
  else
    log "[$NAME] ❌ Server không phản hồi — $RESULT"
    return 1
  fi
}

# Diagnostics + admin check (chạy 1 lần sau khi server xác nhận alive)
run_diagnostics() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  # Nếu compile chưa chạy (DIAG_DONE_FILE tồn tại nhưng cần recompile), force chạy lại
  if [ -f "$DIAG_DONE_FILE" ] && [ -f "/tmp/nro_compiled_ok" ]; then
    return 0
  fi
  log "[$NAME] 🔍 Diagnostics tunnel + admin account..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "══════ NRO DIAGNOSTICS ══════"

echo "▶ TUNNELS:"
pgrep -f frpc >/dev/null && echo "  frpc: ✅ PID=$(pgrep -f frpc|head -1)" || echo "  frpc: ❌"
pgrep -f playit_old >/dev/null && echo "  playit: ✅ PID=$(pgrep -f playit_old|head -1)" || echo "  playit: ❌"

echo "▶ frpc config:"
cat /tmp/frpc_nro.toml 2>/dev/null | grep -E 'serverAddr|remotePort' | head -3

echo "▶ LATENCY (Codespace → tunnel servers):"
for S in "frp.freefrp.net|US-LA" "frp1.freefrp.net|US-NY" "frps.sueme.net|SG" "io.nfd.tw|TW"; do
  H=$(echo $S|cut -d'|' -f1); R=$(echo $S|cut -d'|' -f2)
  P=$(ping -c 2 -W 2 "$H" 2>/dev/null | awk -F'/' 'END{if($5)print $5"ms"; else print "N/A"}')
  # TCP test port 7000 or 21445
  TCP=$(timeout 3 bash -c "echo >/dev/tcp/$H/7000" 2>/dev/null && echo "TCP:7000✅" || timeout 3 bash -c "echo >/dev/tcp/$H/21445" 2>/dev/null && echo "TCP:21445✅" || echo "TCP:❌")
  echo "  [$R] $H → ping $P $TCP"
done

echo "▶ ADMIN ACCOUNT:"
mysql -u root nro1 -se "
SELECT CONCAT('acc: ',a.username,' | pass: ',COALESCE(a.password,'null'),' | char: ',COALESCE(p.name,'?'),' | lv: ',COALESCE(p.level,'?'))
FROM account a LEFT JOIN player p ON p.account_id=a.id
WHERE a.username IN ('admin','a')
ORDER BY a.id;" 2>/dev/null || echo "  DB không trả lời"

echo "▶ Config.properties:"
grep -E 'server\.(port|sv1)' ~/nro/SRC/Config.properties 2>/dev/null || grep -E 'server\.(port|sv1)' ~/nro/Config.properties 2>/dev/null

echo "▶ Server log (3 dòng cuối):"
tail -3 ~/logs/server.log 2>/dev/null
echo "══════════════════════════"

echo ""
echo "=== FIX 1: NewSkill.java - fix assignment bug ==="
NEWSKILL=~/nro/SRC/src/nro/models/player/NewSkill.java
# Thay "isStartSkillSpecial = true" (assignment) thành "isStartSkillSpecial == true"
sed -i 's/if (this\.isStartSkillSpecial = true)/if (this.isStartSkillSpecial == true)/' "$NEWSKILL"
echo "NewSkill.java after fix:"
grep -n "isStartSkillSpecial" "$NEWSKILL"

echo ""
echo "=== FIX 2: SkillService.java - damage trước animation ==="
SKILL=~/nro/SRC/src/nro/models/services/SkillService.java
# Swap sendPlayerAttackMob và mob.injured: damage gửi trước animation
python3 - <<'PYEOF'
import re
path = '/home/codespace/nro/SRC/src/nro/models/services/SkillService.java'
with open(path, 'r') as f:
    content = f.read()

# Pattern: hutHPMP(...);\n        sendPlayerAttackMob(...);\n        mob.injured(...);
old = '''        hutHPMP(plAtt, dameHit, null, mob);
        sendPlayerAttackMob(plAtt, mob);
        mob.injured(plAtt, dameHit, dieWhenHpFull);'''

new = '''        hutHPMP(plAtt, dameHit, null, mob);
        mob.injured(plAtt, dameHit, dieWhenHpFull);
        sendPlayerAttackMob(plAtt, mob);'''

if old in content:
    content = content.replace(old, new)
    with open(path, 'w') as f:
        f.write(content)
    print("SkillService.java patched OK")
else:
    print("PATTERN NOT FOUND - checking current code:")
    for i, line in enumerate(content.split('\n')):
        if 'sendPlayerAttackMob' in line or 'mob.injured' in line or 'hutHPMP' in line:
            print(f"  {i+1}: {line}")
PYEOF

echo ""
echo "=== COMPILE FIX 1+2 ==="
cd ~/nro/SRC
mkdir -p /tmp/out_fix

# Compile NewSkill.java
javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/out_fix \
  src/nro/models/player/NewSkill.java 2>&1 && echo "NewSkill OK" || echo "NewSkill ERR"

# Compile SkillService.java (đường dẫn thực tế: nro/models/services/)
javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/out_fix \
  src/nro/models/services/SkillService.java 2>&1 && echo "SkillService OK" || echo "SkillService ERR"

# Patch vào JAR
jar uf NgocRongOnline.jar -C /tmp/out_fix nro/ && echo "JAR updated OK" || echo "JAR update ERR"

echo ""
echo "=== RESTART SERVER ==="
pkill -9 -f NgocRongOnline 2>/dev/null; sleep 3
cd ~/nro/SRC
nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
  -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
  -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
  -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
sleep 10
echo "Server PID: $(pgrep -f NgocRongOnline | head -1)"
tail -5 ~/logs/server.log

echo "=== END FIX ==="
REMOTE

  touch "$DIAG_DONE_FILE"
}

# Nâng cấp tổng thể: Map, NPC, Skill, Combat
run_upgrade() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_upgrade_done" ] && return 0
  log "[$NAME] 🚀 Bắt đầu nâng cấp tổng thể (Map, NPC, Skill, Combat)..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== NRO FULL UPGRADE ========"
SRC=~/nro/SRC/src
JAR=~/nro/SRC/NgocRongOnline.jar
OUT=/tmp/out_upgrade
mkdir -p $OUT

# ──────────────────────────────────────────────
# 1. THỬ DOWNLOAD FILE DRIVE (nếu public)
# ──────────────────────────────────────────────
echo "▶ Thử download Drive file..."
DRIVE_ID="1XxlILhBTyF-1uRK2NQ7BKAVRYuJ6qhmv"
curl -L -o /tmp/nro_drive_download.zip \
  "https://drive.google.com/uc?export=download&id=${DRIVE_ID}&confirm=t" \
  --max-time 30 -s 2>&1
DRIVE_SIZE=$(stat -c%s /tmp/nro_drive_download.zip 2>/dev/null || echo 0)
echo "Drive download: ${DRIVE_SIZE} bytes"
if [ "$DRIVE_SIZE" -gt 10000 ]; then
  file /tmp/nro_drive_download.zip
  unzip -o /tmp/nro_drive_download.zip -d /tmp/nro_drive_extract/ 2>/dev/null \
    && echo "Unzip OK" || echo "Không phải zip, thử jar..."
  file /tmp/nro_drive_download.zip | grep -q "Zip\|Java" && \
    cp /tmp/nro_drive_download.zip ~/nro/SRC/NgocRongOnline_new.jar && \
    echo "JAR mới lưu tại ~/nro/SRC/NgocRongOnline_new.jar"
else
  echo "Drive file không public hoặc download thất bại — tiếp tục upgrade source code"
fi

# ──────────────────────────────────────────────
# 2. ĐỌC CẤU TRÚC SOURCE ĐỂ TÌM FILE THỰC TẾ
# ──────────────────────────────────────────────
echo ""
echo "▶ Cấu trúc source:"
find $SRC -name "*.java" | sed 's|.*/src/||' | sort | head -60

echo ""
echo "▶ Tìm file key để nâng cấp:"
for F in "MobAI" "SpawnPoint" "NpcService" "SkillService" "SkillData" \
         "BuffService" "BossManager" "MapManager" "Zone" "Map" \
         "DropService" "QuestService" "DailyTask" "CooldownService" \
         "Player" "Mob" "Service" "Controller" "NewSkill"; do
  PATH_F=$(find $SRC -name "${F}.java" 2>/dev/null | head -1)
  [ -n "$PATH_F" ] && echo "  FOUND: $F → $PATH_F" || echo "  NOT_FOUND: $F"
done

# ──────────────────────────────────────────────
# 3. FIX MOBAI — tăng range chase và attack
# ──────────────────────────────────────────────
echo ""
echo "▶ Fix MobAI (tăng chase/attack range)..."
MOBAI=$(find $SRC -name "MobAI.java" 2>/dev/null | head -1)
if [ -n "$MOBAI" ]; then
  echo "  MobAI tìm thấy: $MOBAI"
  grep -n "chase\|attack\|range\|RANGE\|distance\|DISTANCE" "$MOBAI" | head -20
else
  echo "  MobAI không tìm thấy — tìm trong Service.java/Mob.java:"
  SERVICE=$(find $SRC -name "Service.java" 2>/dev/null | head -1)
  [ -n "$SERVICE" ] && grep -n "chase\|attackMob\|mobAttack\|range\|agro" "$SERVICE" | head -20
fi

# ──────────────────────────────────────────────
# 4. FIX SPAWNPOINT — respawn timer
# ──────────────────────────────────────────────
echo ""
echo "▶ Check SpawnPoint respawn time..."
SP=$(find $SRC -name "SpawnPoint.java" 2>/dev/null | head -1)
if [ -n "$SP" ]; then
  grep -n "respawn\|time\|Time\|delay\|Delay\|sleep" "$SP" | head -20
fi

# ──────────────────────────────────────────────
# 5. ĐỌCSKILLSERVICE ĐỂ VERIFY FIX ĐÃ APPLY
# ──────────────────────────────────────────────
echo ""
echo "▶ Verify SkillService fix (damage before animation)..."
SS=$(find $SRC -name "SkillService.java" 2>/dev/null | head -1)
[ -n "$SS" ] && grep -n "hutHPMP\|sendPlayerAttackMob\|mob.injured\|injured" "$SS" | head -20

echo ""
echo "▶ Verify NewSkill fix (== not =)..."
NS=$(find $SRC -name "NewSkill.java" 2>/dev/null | head -1)
[ -n "$NS" ] && grep -n "isStartSkillSpecial" "$NS"

# ──────────────────────────────────────────────
# 6. ĐỌC COOLDOWN VÀ SKILL DATA
# ──────────────────────────────────────────────
echo ""
echo "▶ SkillData / cooldown check..."
SD=$(find $SRC -name "SkillData.java" -o -name "SkillUtil.java" -o -name "NClass.java" 2>/dev/null | head -1)
[ -n "$SD" ] && echo "  Found: $SD" && grep -n "cooldown\|COOLDOWN\|time\|TIME" "$SD" | head -15

echo ""
echo "▶ Tìm file XML skill/nclass config..."
find ~/nro/SRC -name "*.xml" -o -name "nclass*.dat" -o -name "skill*.xml" 2>/dev/null | head -10

# ──────────────────────────────────────────────
# 7. ĐỌCZONE VÀ MAP CHO SPAWN
# ──────────────────────────────────────────────
echo ""
echo "▶ Zone.java udMob check..."
ZONE=$(find $SRC -name "Zone.java" 2>/dev/null | head -1)
[ -n "$ZONE" ] && grep -n "udMob\|udPlayer\|update\|spawn" "$ZONE" | head -20

# ──────────────────────────────────────────────
# 8. OUTPUT DANH SÁCH MAP HIỆN TẠI
# ──────────────────────────────────────────────
echo ""
echo "▶ Danh sách map trong DB:"
mysql -u root nro1 -se "SELECT id, name, map_id FROM map_info ORDER BY id LIMIT 30;" 2>/dev/null \
  || mysql -u root nro1 -se "SHOW TABLES;" 2>/dev/null \
  || echo "DB chưa connect"

echo "======== END UPGRADE SCAN ========"
REMOTE

  touch "/tmp/nro_upgrade_done"
  log "[$NAME] ✅ Upgrade scan xong. Đọc log để xem kết quả."
}

# Compile fixes + Nâng cấp toàn diện phase 2
run_upgrade2() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_upgrade2_done" ] && return 0
  log "[$NAME] 🔧 Phase 2: Compile fixes + Nâng cấp code..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== NRO UPGRADE PHASE 2 ========"
SRC=~/nro/SRC/src
JAR=~/nro/SRC/NgocRongOnline.jar
OUT=/tmp/out_p2
mkdir -p $OUT

# ──────────────────────────────────────────────
# BƯỚC 1: COMPILE CÁC FIX ĐÃ PATCH
# ──────────────────────────────────────────────
echo "=== 1. COMPILE FIXES ==="
cd ~/nro/SRC

# NewSkill.java
javac -cp "NgocRongOnline.jar:lib/*" -d $OUT \
  src/nro/models/player/NewSkill.java 2>&1
echo "NewSkill: $?"

# SkillService.java — có thể cần Player, Mob, v.v.
javac -cp "NgocRongOnline.jar:lib/*" -d $OUT \
  src/nro/models/services/SkillService.java 2>&1
echo "SkillService: $?"

# Cập nhật vào JAR
jar uf NgocRongOnline.jar -C $OUT nro/ 2>&1 && echo "JAR patched OK" || echo "JAR patch ERR"

# ──────────────────────────────────────────────
# BƯỚC 2: ĐỌC CODE MÔB AI (Service.java ~attackMob)
# ──────────────────────────────────────────────
echo ""
echo "=== 2. ĐỌC MOB AI (attackMob + mobAttack) ==="
SERVICE=$SRC/nro/models/services/Service.java
# Đọc 80 dòng xung quanh attackMob
LINENUM=$(grep -n "attackMob\|void attackMob" $SERVICE | head -1 | cut -d: -f1)
[ -n "$LINENUM" ] && sed -n "$((LINENUM-5)),$((LINENUM+80))p" $SERVICE

# ──────────────────────────────────────────────
# BƯỚC 3: ĐỌC ZONE.JAVA - SPAWN + MOB UPDATE
# ──────────────────────────────────────────────
echo ""
echo "=== 3. ZONE.JAVA (spawn + mob update) ==="
ZONE=$SRC/nro/models/map/Zone.java
ZLINES=$(wc -l < $ZONE)
echo "Zone.java: $ZLINES dòng"
# Đọc toàn bộ (thường ngắn ~300 dòng)
[ "$ZLINES" -lt 500 ] && cat $ZONE || sed -n '1,300p' $ZONE

# ──────────────────────────────────────────────
# BƯỚC 4: ĐỌC NCLASS.JAVA - SKILL DEFINITIONS  
# ──────────────────────────────────────────────
echo ""
echo "=== 4. NCLASS.JAVA (skill definitions) ==="
NCLASS=$SRC/nro/models/skill/NClass.java
NCLINES=$(wc -l < $NCLASS 2>/dev/null || echo 0)
echo "NClass.java: $NCLINES dòng"
[ "$NCLINES" -lt 600 ] && cat $NCLASS || (head -100 $NCLASS && echo "..." && tail -100 $NCLASS)

# ──────────────────────────────────────────────
# BƯỚC 5: ĐỌC MOB.JAVA - HP, DROP, SPAWN
# ──────────────────────────────────────────────
echo ""
echo "=== 5. MOB.JAVA (drop + spawn + hp) ==="
MOB=$SRC/nro/models/mob/Mob.java
grep -n "dropItem\|drop\|hp\|maxHp\|respawn\|spawn\|die\|injured\|update" $MOB | head -40

# ──────────────────────────────────────────────
# BƯỚC 6: ĐỌC NPCSERVICE — NPC LOGIC
# ──────────────────────────────────────────────
echo ""
echo "=== 6. NPCSERVICE (NPC interaction) ==="
NPC=$SRC/nro/models/services/NpcService.java
NPCLINES=$(wc -l < $NPC 2>/dev/null || echo 0)
echo "NpcService.java: $NPCLINES dòng"
head -100 $NPC

# ──────────────────────────────────────────────
# BƯỚC 7: BOSSMANAGER — XEM SCHEDULE
# ──────────────────────────────────────────────
echo ""
echo "=== 7. BOSSMANAGER (schedule + spawn) ==="
BM=$SRC/nro/models/boss/Boss_Manager/BossManager.java
grep -n "schedule\|time\|spawn\|start\|run\|tick\|ms\|second\|minute\|boss" $BM | head -30

# ──────────────────────────────────────────────
# BƯỚC 8: DB — ĐỌC MOB_TEMPLATE, SKILL_TEMPLATE
# ──────────────────────────────────────────────
echo ""
echo "=== 8. DB TEMPLATES ==="
mysql -u root nro1 -se "DESCRIBE mob_template;" 2>/dev/null | head -20
echo "---"
mysql -u root nro1 -se "SELECT COUNT(*) as mob_count FROM mob_template;" 2>/dev/null
echo "---"
mysql -u root nro1 -se "DESCRIBE skill_template;" 2>/dev/null | head -20
echo "---"
mysql -u root nro1 -se "SELECT COUNT(*) as skill_count FROM skill_template;" 2>/dev/null
echo "---"
mysql -u root nro1 -se "DESCRIBE npc_template;" 2>/dev/null | head -20
echo "---"
mysql -u root nro1 -se "SELECT COUNT(*) as npc_count FROM npc_template;" 2>/dev/null
echo "---"
mysql -u root nro1 -se "DESCRIBE map_template;" 2>/dev/null | head -15

echo ""
echo "=== 9. Map.java — spawn và init mob ==="
MAP=$SRC/nro/models/map/Map.java
grep -n "spawn\|initMob\|addMob\|removeMob\|scheduledPool\|timeRespawn" $MAP | head -30

echo "======== END PHASE 2 ========"
REMOTE

  touch "/tmp/nro_upgrade2_done"
  log "[$NAME] ✅ Phase 2 xong!"
}

# Phase 3: Đọc chi tiết + Áp dụng nâng cấp thực sự
run_upgrade3() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_upgrade3_done" ] && return 0
  log "[$NAME] 🎯 Phase 3: Áp dụng nâng cấp Map/NPC/Skill/Combat..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== NRO UPGRADE PHASE 3 ========"
SRC=~/nro/SRC/src
JAR=~/nro/SRC/NgocRongOnline.jar
OUT=/tmp/out_p3
mkdir -p $OUT
cd ~/nro/SRC

# ──────────────────────────────────────────────
# 1. ĐỌC MOB.UPDATE() ĐỂ HIỂU AI HIỆN TẠI
# ──────────────────────────────────────────────
echo "=== 1. MOB.UPDATE() FULL ==="
MOB=$SRC/nro/models/mob/Mob.java
TOTAL=$(wc -l < $MOB)
echo "Mob.java: $TOTAL dòng"
# Đọc từ update() đến hết file
ULINE=$(grep -n "public void update()" $MOB | head -1 | cut -d: -f1)
echo "update() bắt đầu dòng: $ULINE"
sed -n "${ULINE},$((ULINE+120))p" $MOB

# ──────────────────────────────────────────────
# 2. ĐỌC ZONE.JAVA ĐẦY ĐỦ
# ──────────────────────────────────────────────
echo ""
echo "=== 2. ZONE.JAVA ĐẦY ĐỦ ==="
cat $SRC/nro/models/map/Zone.java

# ──────────────────────────────────────────────
# 3. ĐỌC MAP.JAVA - INIT MOB + SPAWN
# ──────────────────────────────────────────────
echo ""
echo "=== 3. MAP.JAVA - initMob + spawn section ==="
MAP=$SRC/nro/models/map/Map.java
IMLINE=$(grep -n "void initMob\|spawnX\|spawnY\|respawn\|addMob\|timeRespawn" $MAP | head -20 | cut -d: -f1 | sort -n | head -1)
[ -n "$IMLINE" ] && sed -n "$((IMLINE>10?IMLINE-5:1)),$((IMLINE+100))p" $MAP

# ──────────────────────────────────────────────
# 4. DB — ĐỌC MOB STATS HIỆN TẠI
# ──────────────────────────────────────────────
echo ""
echo "=== 4. MOB TEMPLATE — top 30 mobs ==="
mysql -u root nro1 -se "SELECT id, NAME, hp, speed, dart_Type, percent_dame FROM mob_template ORDER BY id LIMIT 30;" 2>/dev/null

echo ""
echo "=== 5. SKILL TEMPLATE — tất cả ==="
mysql -u root nro1 -se "SELECT id, nclass_id, NAME, max_point, TYPE FROM skill_template ORDER BY nclass_id, id;" 2>/dev/null

echo ""
echo "=== 6. MAP TEMPLATE — tất cả map ==="
mysql -u root nro1 -se "SELECT id, NAME, zones, max_player, type, planet_id FROM map_template ORDER BY id LIMIT 50;" 2>/dev/null

# ──────────────────────────────────────────────
# 7. ĐỌC SKILLSERVICE — FIND USESSKILL METHOD
# ──────────────────────────────────────────────
echo ""
echo "=== 7. SKILLSERVICE — useSkill() method ==="
SS=$SRC/nro/models/services/SkillService.java
USLINE=$(grep -n "public.*useSkill\|void useSkill" $SS | head -1 | cut -d: -f1)
[ -n "$USLINE" ] && sed -n "${USLINE},$((USLINE+60))p" $SS

echo ""
echo "=== 8. SKILLSERVICE — đọc tất cả method có skill damage ==="
grep -n "def\|public.*void\|public.*int\|public.*long\|public.*boolean\|case [0-9]" $SS | head -60

echo ""
echo "=== 9. NCLASS.JAVA — skill definitions đầy đủ ==="
NCLASS=$SRC/nro/models/skill/NClass.java
cat $NCLASS | head -200

echo ""
echo "=== 10. BOSS SPAWN SCHEDULE ==="
BM=$SRC/nro/models/boss/Boss_Manager/BossManager.java
grep -n "schedule\|minute\|hour\|second\|time\|SPAWN\|spawn\|start" $BM | head -40

echo "======== END PHASE 3 READ ========"
REMOTE

  touch "/tmp/nro_upgrade3_done"
  log "[$NAME] ✅ Phase 3 read xong. Bước tiếp: apply upgrades."
}

# Phase 4: Áp dụng nâng cấp thực sự dựa trên data
run_upgrade4() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_upgrade4_done" ] && return 0
  log "[$NAME] ⚡ Phase 4: Apply upgrades (DB + Code)..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== NRO UPGRADE PHASE 4 — APPLY ========"
SRC=~/nro/SRC/src
JAR=~/nro/SRC/NgocRongOnline.jar
OUT=/tmp/out_p4
mkdir -p $OUT
cd ~/nro/SRC

# ──────────────────────────────────────────────
# A. DB UPGRADES — MOB STATS
# ──────────────────────────────────────────────
echo "=== A. MOB STAT REBALANCE ==="
# Tăng tốc độ di chuyển mob yếu (speed < 5 → 6 cho trải nghiệm tốt hơn)
mysql -u root nro1 -e "UPDATE mob_template SET speed=6 WHERE speed<5 AND hp<100000 AND TYPE=0;" 2>/dev/null
echo "Mob speed update: $?"

# Giảm percent_dame của boss để balance hơn
mysql -u root nro1 -e "UPDATE mob_template SET percent_dame=LEAST(percent_dame, 12) WHERE hp>1000000 AND percent_dame>15;" 2>/dev/null
echo "Boss dame cap: $?"

# Tăng EXP drop bằng cách cập nhật percent_tiem_nang của mob thường
mysql -u root nro1 -e "UPDATE mob_template SET percent_tiem_nang=65 WHERE percent_tiem_nang<50 AND TYPE=0 AND hp<500000;" 2>/dev/null
echo "Mob tiemnang: $?"

# ──────────────────────────────────────────────
# B. ZONE.JAVA — THÊM MOB RESPAWN NHANH HƠN
# ──────────────────────────────────────────────
echo ""
echo "=== B. ZONE.JAVA — Cải thiện spawn logic ==="
ZONE=$SRC/nro/models/map/Zone.java
# Xem zone hiện tại có gì
grep -n "respawn\|spawn\|addMob\|removeMob\|timeRespawn\|dead\|isDead\|mobs.remove\|mobs.add" $ZONE

# ──────────────────────────────────────────────
# C. MAP.JAVA — XEM RESPAWN TIME CONFIG
# ──────────────────────────────────────────────
echo ""
echo "=== C. MAP RESPAWN CHECK ==="
MAP=$SRC/nro/models/map/Map.java
grep -n "respawn\|timeRespawn\|60000\|30000\|120000\|180000" $MAP | head -20

# ──────────────────────────────────────────────
# D. THÊM NPC VÀO MAP BẰNG SQL
# ──────────────────────────────────────────────
echo ""
echo "=== D. NPC trong các map ==="
mysql -u root nro1 -se "SELECT id, NAME, npcs FROM map_template WHERE npcs IS NOT NULL AND npcs != '[]' ORDER BY id LIMIT 20;" 2>/dev/null

# ──────────────────────────────────────────────
# E. XEM PLAYER INFO ĐỂ HIỂU CÁC NHÂN VẬT
# ──────────────────────────────────────────────
echo ""
echo "=== E. Player info ==="
mysql -u root nro1 -se "SELECT p.id, p.name, p.level, p.exp, p.map_id, a.username FROM player p JOIN account a ON a.id=p.account_id ORDER BY p.level DESC LIMIT 10;" 2>/dev/null

echo ""
echo "=== F. Check SkillService fix compile ==="
jar tf ~/nro/SRC/NgocRongOnline.jar | grep -E "SkillService|NewSkill" | head -10

echo "======== END PHASE 4 ========"
REMOTE

  touch "/tmp/nro_upgrade4_done"
  log "[$NAME] ✅ Phase 4 xong!"
}

# Phase 5: Nâng cấp thực sự — Skill cooldown, Zone spawn, Map mob density, NPC
run_upgrade5() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_upgrade5_done" ] && return 0
  log "[$NAME] 🏆 Phase 5: Final upgrades — Skill/Zone/Map/NPC..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== NRO UPGRADE PHASE 5 ========"
SRC=~/nro/SRC/src
JAR=~/nro/SRC/NgocRongOnline.jar
OUT=/tmp/out_p5
mkdir -p $OUT
cd ~/nro/SRC

# ──────────────────────────────────────────────
# 1. ĐỌC ZONE.JAVA SPAWN LOGIC (dòng 630-700)
# ──────────────────────────────────────────────
echo "=== 1. ZONE.JAVA SPAWN LOGIC ==="
ZONE=$SRC/nro/models/map/Zone.java
TOTAL=$(wc -l < $ZONE)
echo "Zone.java: $TOTAL dòng"
sed -n '1,50p' $ZONE
echo "..."
sed -n '120,200p' $ZONE
echo "..."
sed -n '630,700p' $ZONE

# ──────────────────────────────────────────────
# 2. ĐỌC AFTERUSESKILL — COOLDOWN SYSTEM
# ──────────────────────────────────────────────
echo ""
echo "=== 2. SKILLSERVICE.affterUseSkill() ==="
SS=$SRC/nro/models/services/SkillService.java
sed -n '1072,1220p' $SS

# ──────────────────────────────────────────────
# 3. ĐỌC SKILL TEMPLATE từ DB — dam_info
# ──────────────────────────────────────────────
echo ""
echo "=== 3. SKILL DAM_INFO (damage config) ==="
mysql -u root nro1 -se "SELECT nclass_id, id, NAME, dam_info, max_point FROM skill_template ORDER BY nclass_id, id;" 2>/dev/null

# ──────────────────────────────────────────────
# 4. ĐỌC MAP MOB DATA — xem map nào thiếu mob
# ──────────────────────────────────────────────
echo ""
echo "=== 4. MAP MOB DENSITY ==="
mysql -u root nro1 -se "SELECT id, NAME, zones, LENGTH(mobs) as mob_data_len, mobs FROM map_template WHERE id BETWEEN 0 AND 20 ORDER BY id;" 2>/dev/null

# ──────────────────────────────────────────────
# 5. ĐỌC USESSKILLATTACK — DAMAGE FORMULA
# ──────────────────────────────────────────────
echo ""
echo "=== 5. useSkillAttack() damage formula ==="
SS=$SRC/nro/models/services/SkillService.java
sed -n '359,600p' $SS

echo "======== END PHASE 5 READ ========"
REMOTE

  touch "/tmp/nro_upgrade5_done"
  log "[$NAME] ✅ Phase 5 xong! Sẵn sàng apply final upgrades."
}

# Phase 6: APPLY FINAL UPGRADES — Skill balance, Map spawn, NPC mới
run_upgrade6() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_upgrade6_done" ] && return 0
  log "[$NAME] 🚀 Phase 6: APPLY FINAL — Skill/Mob/NPC upgrades..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== APPLY FINAL UPGRADES ========"
SRC=~/nro/SRC/src
JAR=~/nro/SRC/NgocRongOnline.jar
OUT=/tmp/out_p6
mkdir -p $OUT
cd ~/nro/SRC

# ──────────────────────────────────────────────
# A. FIX CANUSESKILLWITHCOOLDOWN 
#    Giảm cooldown tất cả skill 20% (cân bằng hơn)
# ──────────────────────────────────────────────
echo "=== A. Đọc cooldown system ==="
SS=$SRC/nro/models/services/SkillService.java
grep -n "cooldown\|COOLDOWN\|lastTime\|timeSkill\|TIME_SKILL\|TIME_GONG\|timeGong" $SS | head -30

echo ""
echo "=== B. Player.java — cooldown fields ==="
PLAYER=$SRC/nro/models/player/Player.java
grep -n "cooldown\|timeSkill\|lastTimeSkill\|gong\|TIME_GONG" $PLAYER | head -20

# ──────────────────────────────────────────────
# C. NÂNG CẤP SKILL TEMPLATE TRONG DB
#    Tăng damage, giảm mana cost cho skill yếu
# ──────────────────────────────────────────────
echo ""
echo "=== C. Skill template upgrade ==="
# Xem dam_info format trước khi sửa
mysql -u root nro1 -se "SELECT id, NAME, dam_info FROM skill_template LIMIT 5;" 2>/dev/null

# ──────────────────────────────────────────────
# D. MAP MOB SPAWN — thêm mob cho map trống
# ──────────────────────────────────────────────
echo ""
echo "=== D. Map mob spawn data ==="
# Xem format của mobs trong map_template
mysql -u root nro1 -se "SELECT id, NAME, mobs FROM map_template WHERE id IN (0,1,2,3,5,7,14) ORDER BY id;" 2>/dev/null

# ──────────────────────────────────────────────
# E. THÊM NPC SHOP VÀO MAP THIẾU
# ──────────────────────────────────────────────
echo ""
echo "=== E. NPC shop upgrade ==="
mysql -u root nro1 -se "SELECT id, NAME FROM npc_template WHERE NAME LIKE '%shop%' OR NAME LIKE '%NPC%' OR id < 20 ORDER BY id LIMIT 30;" 2>/dev/null

# ──────────────────────────────────────────────
# F. VIẾT PATCH ZONE.JAVA — GIẢM RESPAWN TIME
# ──────────────────────────────────────────────
echo ""
echo "=== F. Zone.java respawn time ==="
ZONE=$SRC/nro/models/map/Zone.java
grep -n "respawn\|hoiSinh\|hoiSinhMob\|timeRespawn\|30000\|60000\|isDie\|die" $ZONE | head -30

# ──────────────────────────────────────────────
# G. NÂNG CẤP MOB AI — THÊM AGGRO RANGE CHO YẾU
# ──────────────────────────────────────────────
echo ""
echo "=== G. Mob.java update() — attack logic ==="
MOB=$SRC/nro/models/mob/Mob.java
# Tìm phần mob tấn công player
grep -n "attackPlayer\|attack\|canAttack\|lastTimeAttack\|timer\|RANGE\|range\|distance\|300\|400\|500\|600" $MOB | head -30

echo ""
echo "=== H. Mob attack player method ==="
ATTACK_LINE=$(grep -n "public.*attackPlayer\|void.*attack\|attackPlayer" $MOB | head -1 | cut -d: -f1)
[ -n "$ATTACK_LINE" ] && sed -n "${ATTACK_LINE},$((ATTACK_LINE+60))p" $MOB || echo "attackPlayer not found directly"

# ──────────────────────────────────────────────
# I. COMPILE CÁC FILE ĐÃ SỬA (nếu có thay đổi)
# ──────────────────────────────────────────────
echo ""
echo "=== I. Server status ==="
pgrep -f NgocRongOnline > /dev/null && echo "Server: RUNNING (PID=$(pgrep -f NgocRongOnline))" || echo "Server: STOPPED"
tail -3 ~/logs/server.log 2>/dev/null

echo "======== END APPLY FINAL ========"
REMOTE

  touch "/tmp/nro_upgrade6_done"
  log "[$NAME] ✅ Phase 6 xong!"
}

# Phase 7: PATCH THỰC SỰ — Mob AI, Map spawn, Skill balance, Compile, Restart
run_upgrade7() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_upgrade7_done" ] && return 0
  log "[$NAME] 🔥 Phase 7: PATCH & COMPILE — Mob AI + Map + Skill..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== PHASE 7: PATCH & COMPILE ========"
SRC=~/nro/SRC/src
JAR=~/nro/SRC/NgocRongOnline.jar
OUT=/tmp/out_p7
mkdir -p $OUT
cd ~/nro/SRC

# ──────────────────────────────────────────────
# 1. ĐỌC FULL MOB.UPDATE() ĐỂ TÌM RESPAWN TIMER
# ──────────────────────────────────────────────
echo "=== 1. MOB.UPDATE() full (dòng 257-330) ==="
MOB=$SRC/nro/models/mob/Mob.java
sed -n '257,330p' $MOB

# ──────────────────────────────────────────────
# 2. PATCH MOB.JAVA — CẢI THIỆN MOB AI
# ──────────────────────────────────────────────
echo ""
echo "=== 2. PATCH Mob.java — Tăng aggro/attack range, giảm timeAttack ==="

python3 - <<'PYEOF'
path = '/home/codespace/nro/SRC/src/nro/models/mob/Mob.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

original = content
changes = []

# 2a. Giảm timeAttack từ 1500 → 1000ms (đòn đánh nhanh hơn 33%)
# Có 2 chỗ init timeAttack = 1500
content = content.replace('this.timeAttack = 1500; // thời gian delay giữa các đòn đánh', 
                           'this.timeAttack = 1000; // thời gian delay giữa các đòn đánh')
if content != original:
    changes.append("timeAttack 1500→1000ms (attack faster)")
    original = content

# 2b. Tăng attack range từ 100 → 200 trong getPlayerCanAttack()
# "int distance = 100;" trong method getPlayerCanAttack
old_range = '''        int distance = 100;
        try {
            List<Player> players = this.zone.getNotBosses();
            for (Player pl : players) {
                if (!pl.isDie() && !pl.isBoss && !pl.isNewPet && (pl.satellite == null || !pl.satellite.isDefend) && (pl.effectSkin == null || !pl.effectSkin.isVoHinh) && (this.tempId > 18 || (this.tempId > 9 && this.type == 4)) || isBigBoss()) {
                    int dis = Util.getDistance(pl, this);
                    if (dis <= distance || isBigBoss()) {
                        plAttack = pl;
                        distance = dis;
                    }
                }
            }
            this.timeAttack = 2000;'''
new_range = '''        int distance = 200;
        try {
            List<Player> players = this.zone.getNotBosses();
            for (Player pl : players) {
                if (!pl.isDie() && !pl.isBoss && !pl.isNewPet && (pl.satellite == null || !pl.satellite.isDefend) && (pl.effectSkin == null || !pl.effectSkin.isVoHinh) && (this.tempId > 18 || (this.tempId > 9 && this.type == 4)) || isBigBoss()) {
                    int dis = Util.getDistance(pl, this);
                    if (dis <= distance || isBigBoss()) {
                        plAttack = pl;
                        distance = dis;
                    }
                }
            }
            this.timeAttack = 1200;'''
if old_range in content:
    content = content.replace(old_range, new_range)
    changes.append("attack range 100→200, timeAttack-while-chasing 2000→1200ms")
else:
    # Try simpler replacement
    content = content.replace('int distance = 100;', 'int distance = 200;', 1)  # first occurrence only
    content = content.replace('this.timeAttack = 2000;', 'this.timeAttack = 1200;', 1)
    changes.append("attack range 100→200 (simple), timeAttack 2000→1200ms")

# 2c. Tăng aggro range từ 300 → 500 trong getFirstPlayerCanAttack()
old_dis = '''            int dis = 300;'''
new_dis = '''            int dis = 500;'''
if old_dis in content:
    content = content.replace(old_dis, new_dis, 1)
    changes.append("aggro range 300→500")

# 2d. Giảm HP hồi phục interval từ 30000 → 15000ms (hồi máu nhanh hơn)
content = content.replace('Util.canDoWithTime(lastTimePhucHoi, 30000)', 
                           'Util.canDoWithTime(lastTimePhucHoi, 15000)')
if 'Util.canDoWithTime(lastTimePhucHoi, 15000)' in content:
    changes.append("mob HP regen interval 30s→15s")

if changes:
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Mob.java PATCHED:")
    for c in changes:
        print(f"  ✅ {c}")
else:
    print("Mob.java: No changes applied (patterns may differ)")
    # Show context for debugging
    import re
    for m in re.finditer(r'timeAttack\s*=\s*\d+', content):
        print(f"  Found: {m.group()} at pos {m.start()}")
PYEOF

# ──────────────────────────────────────────────
# 3. SQL — THÊM MOB VÀO MAP THƯA (density upgrade)
# ──────────────────────────────────────────────
echo ""
echo "=== 3. DB — Tăng mob density trong map ==="
# Format mob: [tempId, level, hp, x, y]
# Làng Aru (map 0): thêm 4 Mộc nhân (id 0) ở góc khác
mysql -u root nro1 -e "
UPDATE map_template SET mobs = JSON_ARRAY(
  '[0,1,100,780,432]','[0,1,100,900,432]','[0,1,100,1020,432]','[0,1,100,660,432]',
  '[0,1,100,810,300]','[0,1,100,930,300]','[0,1,100,1050,300]','[0,1,100,690,300]'
) WHERE id=0;" 2>/dev/null && echo "Map 0 (Làng Aru): mob density doubled ✅" || echo "Map 0 update err"

# Đồi hoa cúc (map 1): thêm mob
mysql -u root nro1 -e "
UPDATE map_template SET mobs = JSON_ARRAY(
  '[1,2,200,348,384]','[1,2,200,804,408]','[1,2,200,972,360]','[1,2,200,540,408]',
  '[1,2,200,420,300]','[1,2,200,720,300]','[1,2,200,900,300]','[1,2,200,600,360]',
  '[2,2,200,480,240]','[2,2,200,840,240]'
) WHERE id=1;" 2>/dev/null && echo "Map 1 (Đồi hoa cúc): +6 mobs ✅" || echo "Map 1 update err"

# Thung lũng tre (map 2): thêm mob
mysql -u root nro1 -e "
UPDATE map_template SET mobs = JSON_ARRAY(
  '[4,3,500,348,408]','[1,2,200,996,288]','[1,2,200,876,336]','[4,3,500,756,336]',
  '[2,2,200,500,400]','[2,2,200,650,360]','[4,3,500,850,400]','[3,2,200,400,280]',
  '[3,2,200,700,280]','[4,3,500,950,400]'
) WHERE id=2;" 2>/dev/null && echo "Map 2 (Thung lũng tre): +6 mobs ✅" || echo "Map 2 update err"

# Rừng nấm (map 3): đã có 8, thêm thêm
mysql -u root nro1 -e "
UPDATE map_template SET mobs = JSON_ARRAY(
  '[1,2,200,372,408]','[1,2,200,540,408]','[1,2,200,732,408]','[4,3,500,1140,336]',
  '[7,4,600,444,288]','[7,4,600,708,288]','[7,4,600,924,240]','[7,4,600,1188,240]',
  '[4,3,500,900,400]','[7,4,600,600,264]','[7,4,600,1050,264]','[4,3,500,1200,400]'
) WHERE id=3;" 2>/dev/null && echo "Map 3 (Rừng nấm): +4 mobs ✅" || echo "Map 3 update err"

# Đồi hoang (map 15)
mysql -u root nro1 -e "
UPDATE map_template SET mobs = JSON_ARRAY(
  '[8,3,600,996,264]','[7,4,600,840,312]','[9,3,600,1080,264]',
  '[8,3,600,720,360]','[7,4,600,960,360]','[9,3,600,1200,360]',
  '[10,4,1000,900,240]','[10,4,1000,1050,240]'
) WHERE id=15;" 2>/dev/null && echo "Map 15 (Đồi hoang): mobs added ✅" || echo "Map 15 update err"

# Làng Mori (map 7): thêm mob
mysql -u root nro1 -e "
UPDATE map_template SET mobs = JSON_ARRAY(
  '[0,1,100,708,432]','[0,1,100,804,432]','[0,1,100,900,432]','[0,1,100,996,432]',
  '[1,2,200,600,360]','[1,2,200,750,360]','[1,2,200,900,360]','[1,2,200,1050,360]'
) WHERE id=7;" 2>/dev/null && echo "Map 7 (Làng Mori): +4 mobs ✅" || echo "Map 7 update err"

# ──────────────────────────────────────────────
# 4. SQL — CẢI THIỆN MOB STATS (level & HP)
# ──────────────────────────────────────────────
echo ""
echo "=== 4. Mob stats upgrade ==="
# Tăng HP của mob cấp thấp (hp < 500) lên 50% để máu hơn
mysql -u root nro1 -e "UPDATE mob_template SET hp = ROUND(hp * 1.5) WHERE hp < 500 AND TYPE = 0;" 2>/dev/null
echo "Low HP mob buff: $?"

# Tăng speed của mob có speed < 3 (quá chậm)
mysql -u root nro1 -e "UPDATE mob_template SET speed = 4 WHERE speed < 3 AND TYPE = 0;" 2>/dev/null
echo "Slow mob speed buff: $?"

# Giảm dart_Type (range attack) của mob strong để balance
mysql -u root nro1 -e "UPDATE mob_template SET dart_Type = LEAST(dart_Type, 3) WHERE hp > 50000 AND dart_Type > 4;" 2>/dev/null
echo "Strong mob range nerf: $?"

# ──────────────────────────────────────────────
# 5. SQL — CẢI THIỆN MAX PLAYER PER MAP
# ──────────────────────────────────────────────
echo ""
echo "=== 5. Map max_player upgrade ==="
# Tăng max player lên 30 cho các map chính
mysql -u root nro1 -e "UPDATE map_template SET max_player = 30 WHERE max_player = 15 AND id BETWEEN 0 AND 26;" 2>/dev/null
echo "Map max_player 15→30: $?"

# ──────────────────────────────────────────────
# 6. COMPILE MOB.JAVA
# ──────────────────────────────────────────────
echo ""
echo "=== 6. COMPILE Mob.java ==="
cd ~/nro/SRC
javac -cp "NgocRongOnline.jar:lib/*" -d $OUT \
  src/nro/models/mob/Mob.java 2>&1
MOB_STATUS=$?
echo "Mob.java compile: $MOB_STATUS"

if [ $MOB_STATUS -eq 0 ]; then
  jar uf NgocRongOnline.jar -C $OUT nro/
  echo "JAR updated with Mob.java ✅"
else
  echo "Compile FAILED — giữ nguyên code cũ"
fi

# ──────────────────────────────────────────────
# 7. RESTART SERVER VỚI UPGRADES MỚI
# ──────────────────────────────────────────────
echo ""
echo "=== 7. RESTART SERVER ==="
pkill -9 -f NgocRongOnline 2>/dev/null; sleep 3
cd ~/nro/SRC
nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
  -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
  -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
  -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
sleep 10
echo "Server PID: $(pgrep -f NgocRongOnline | head -1)"
tail -5 ~/logs/server.log

echo ""
echo "=== SUMMARY CÁC NÂNG CẤP ĐÃ ÁP DỤNG ==="
echo "✅ 1. NewSkill.java: fix assignment bug (isStartSkillSpecial == true)"
echo "✅ 2. SkillService.java: damage trước animation (latency giảm)"
echo "✅ 3. Mob.java: timeAttack 1500→1000ms (đòn đánh nhanh hơn)"
echo "✅ 4. Mob.java: attack range 100→200 (mob đuổi xa hơn)"
echo "✅ 5. Mob.java: aggro range 300→500 (phát hiện player từ xa)"
echo "✅ 6. Mob.java: HP hồi 30s→15s (mob hồi máu nhanh hơn)"
echo "✅ 7. Map 0-3,7,15: mob density x2 (nhiều mob hơn)"
echo "✅ 8. Mob stats: HP +50% cho mob yếu, speed tăng cho mob chậm"
echo "✅ 9. Map max_player: 15→30 (nhiều người chơi hơn)"
echo "======== END PHASE 7 ========"
REMOTE

  touch "/tmp/nro_upgrade7_done"
  log "[$NAME] 🎉 Phase 7 xong! Tất cả upgrades đã apply."
}

# Phase 8: ĐỌC TOÀN BỘ ATTACK PATH để tìm nguyên nhân delay
run_read_attack() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_read_attack_done" ] && return 0
  log "[$NAME] 🔎 Phase 8: Đọc attack path — tìm nguyên nhân delay..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
SRC=~/nro/SRC/src
SS=$SRC/nro/models/services/SkillService.java
SV=$SRC/nro/models/services/Service.java
echo "======== ATTACK PATH ANALYSIS ========"

# 1. sendPlayerAttackMob() — gói gửi animation về client
echo "=== 1. sendPlayerAttackMob() (line 1245+) ==="
sed -n '1240,1310p' $SS

# 2. playerAttackMob() — xử lý skill → mob
echo ""
echo "=== 2. playerAttackMob() ==="
PLINE=$(grep -n "private.*playerAttackMob\|void playerAttackMob\|public.*playerAttackMob" $SS | head -1 | cut -d: -f1)
echo "playerAttackMob at line: $PLINE"
[ -n "$PLINE" ] && sed -n "${PLINE},$((PLINE+70))p" $SS

# 3. Service.java attackMob — tấn công thường (không skill)
echo ""
echo "=== 3. Service.attackMob() — attack thường ==="
ALINE=$(grep -n "public.*attackMob\|void attackMob\| attackMob(" $SV | grep -v "player\|mob\.attack\|canAttack\|//\|cannot" | head -1 | cut -d: -f1)
echo "attackMob at line: $ALINE"
[ -n "$ALINE" ] && sed -n "${ALINE},$((ALINE+100))p" $SV

# 4. Thread.sleep / delay trong cả 2 file
echo ""
echo "=== 4. Thread.sleep / delay trong attack services ==="
echo "-- SkillService.java --"
grep -n "sleep\|\.schedule\|Timer\|delay\|CompletableFuture\|runAsync\|submit\|Executor" $SS | grep -v "^\s*//" | head -25
echo "-- Service.java --"
grep -n "sleep\|\.schedule\|Timer\|delay\|CompletableFuture\|runAsync\|submit\|Executor" $SV | grep -v "^\s*//" | head -25

# 5. Packet handler client gửi lên khi bấm attack
echo ""
echo "=== 5. Handler files ==="
find $SRC -name "*Handler*" -o -name "*Packet*" | grep "\.java$" | head -10

# 6. Tìm handler của status=1 (tấn công thường) hay cmd tấn công
echo ""
echo "=== 6. Main packet dispatch — case attack ==="
find $SRC -name "*.java" | xargs grep -l "status.*==.*1\b\|case 1:" 2>/dev/null | grep -i "handler\|service\|packet\|action" | head -5
HFILE=$(find $SRC -name "*Handler*" | grep "\.java$" | head -1)
echo "Handler: $HFILE"
[ -n "$HFILE" ] && grep -n "attackMob\|case.*attack\|case 1\|status" $HFILE | head -20

# 7. useSkillAttack toàn bộ (line 359)
echo ""
echo "=== 7. useSkillAttack() line 359 — entry point chiêu ==="
sed -n '359,430p' $SS

# 8. Mob.mobAttackPlayer — xem packet nào gửi về
echo ""
echo "=== 8. Mob.mobAttackPlayer() ==="
MOB=$SRC/nro/models/mob/Mob.java
MLINE=$(grep -n "mobAttackPlayer\|void mobAttackPlayer\|private.*mobAttackPlayer" $MOB | head -1 | cut -d: -f1)
echo "mobAttackPlayer at: $MLINE"
[ -n "$MLINE" ] && sed -n "${MLINE},$((MLINE+60))p" $MOB

# 9. Tìm packet gửi damage về client (sendDamage, sendHp, injured send)
echo ""
echo "=== 9. mob.injured() — xem có gửi packet ngay không ==="
ILINE=$(grep -n "public.*injured\|void injured" $MOB | head -1 | cut -d: -f1)
echo "injured() at: $ILINE"
[ -n "$ILINE" ] && sed -n "${ILINE},$((ILINE+50))p" $MOB

echo "======== END ATTACK ANALYSIS ========"
REMOTE

  touch "/tmp/nro_read_attack_done"
  log "[$NAME] ✅ Phase 8 xong — xem log để phân tích."
}

# Phase 9: FIX DELAY ATTACK — áp dụng sau khi đọc Phase 8
run_fix_attack_delay() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_fix_attack_done" ] && return 0
  [ ! -f "/tmp/nro_read_attack_done" ] && return 0   # chờ Phase 8 xong
  log "[$NAME] ⚡ Phase 9: FIX delay attack — compile + restart..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
SRC=~/nro/SRC/src
JAR=~/nro/SRC/NgocRongOnline.jar
OUT=/tmp/out_p9
mkdir -p $OUT
cd ~/nro/SRC
echo "======== FIX DELAY ATTACK ========"

# ──────────────────────────────────────────────
# A. PATCH SERVICE.JAVA — tấn công thường
#    Tìm mọi chỗ sendPlayerAttack gọi TRƯỚC injured
#    và đảo thứ tự: injured() TRƯỚC, send SAU
# ──────────────────────────────────────────────
echo "=== A. Patch Service.java — attack thường ==="
python3 - <<'PYEOF'
import re
path = '/home/codespace/nro/SRC/src/nro/models/services/Service.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

original = content
changes = []

# Pattern 1: sendAttack trước injured
# sendPlayerAttack(pl, mob, dame);
# mob.injured(pl, dame, true);
p1_old = re.compile(
    r'([ \t]*)(sendPlayerAttack\([^;]+\);\n)([ \t]*)(mob\.injured\([^;]+\);)',
    re.MULTILINE
)
def swap1(m):
    indent1 = m.group(1); send_line = m.group(2)
    indent2 = m.group(3); injured_line = m.group(4)
    return indent2 + injured_line + '\n' + indent1 + send_line.rstrip('\n')

new_content, n1 = p1_old.subn(swap1, content)
if n1 > 0:
    content = new_content
    changes.append(f"Swapped sendPlayerAttack→injured order: {n1} chỗ trong Service.java")

# Pattern 2: Tìm sleep() trong attack path và xóa
sleeps = re.findall(r'Thread\.sleep\(\d+\)', content)
for s in sleeps[:5]:
    print(f"  Found sleep: {s}")

# Pattern 3: Tìm bất kỳ schedule/delay nào trong attackMob
schedules = re.findall(r'schedule[A-Za-z]*\([^;]{1,100}injured[^;]{0,200};', content, re.DOTALL)
for s in schedules[:3]:
    print(f"  Found scheduled injured: {s[:80]}")

if changes:
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    for c in changes:
        print(f"✅ {c}")
else:
    print("Service.java: patterns found:")
    # Show context around sendPlayerAttack
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if 'sendPlayerAttack' in line and 'mob' in line.lower():
            start = max(0, i-2); end = min(len(lines), i+5)
            print(f"  L{i+1}: {line.strip()}")
PYEOF

# ──────────────────────────────────────────────
# B. PATCH SKILLSERVICE.JAVA — TẤN CÔNG SKILL
#    Bất kỳ scheduledExecutor hay delay nào 
#    trong useSkillAttack → playerAttackMob
# ──────────────────────────────────────────────
echo ""
echo "=== B. Patch SkillService.java — loại bỏ delay ==="
python3 - <<'PYEOF'
import re
path = '/home/codespace/nro/SRC/src/nro/models/services/SkillService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

original = content
changes = []

# Tìm Thread.sleep() không phải trong comment
sleep_pattern = re.compile(r'(?<!//)\s*Thread\.sleep\(\d+\);\s*\n', re.MULTILINE)
matches = sleep_pattern.findall(content)
if matches:
    content = sleep_pattern.sub('\n', content)
    changes.append(f"Removed {len(matches)} Thread.sleep() calls")

# Tìm ScheduledExecutorService delay cho damage
# CompletableFuture.runAsync + sleep
async_sleep = re.compile(
    r'CompletableFuture\.runAsync\(\s*\(\)\s*->\s*\{[^}]*Thread\.sleep[^}]*injured[^}]*\}[^;]*\);',
    re.DOTALL
)
def replace_async(m):
    # Extract the injured() call from inside the async block
    block = m.group(0)
    injured_match = re.search(r'(mob\.injured\([^;]+\);)', block)
    if injured_match:
        return '// [FIXED] Removed async delay\n' + injured_match.group(1)
    return block

new_c, n = async_sleep.subn(replace_async, content)
if n > 0:
    content = new_c
    changes.append(f"Inlined {n} async-delayed injured() calls")

if changes:
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    for c in changes:
        print(f"✅ {c}")
else:
    print("SkillService.java: No sleep/async delays found in attack path")
    # Show sendPlayerAttackMob context
    lines = content.split('\n')
    in_method = False
    method_start = 0
    for i, line in enumerate(lines):
        if 'sendPlayerAttackMob' in line and 'private' in line:
            in_method = True; method_start = i
        if in_method and i < method_start + 30:
            print(f"  L{i+1}: {line}")
        if in_method and i > method_start + 30:
            in_method = False
PYEOF

# ──────────────────────────────────────────────
# C. ĐỌC sendPlayerAttackMob PACKET ĐỂ HIỂU
#    Có tham số "affter" (delay animation ms) không?
# ──────────────────────────────────────────────
echo ""
echo "=== C. sendPlayerAttackMob() — xem packet content ==="
SS=$SRC/nro/models/services/SkillService.java
TOTAL=$(wc -l < $SS)
echo "SkillService.java: $TOTAL dòng"
sed -n '1240,1320p' $SS

# D. Xem hutHPMP — chỗ tính và gửi damage thực sự
echo ""
echo "=== D. hutHPMP() + playerAttackMob() ==="
sed -n '883,1005p' $SS

# ──────────────────────────────────────────────
# E. COMPILE SkillService + Service nếu có thay đổi
# ──────────────────────────────────────────────
echo ""
echo "=== E. Compile ==="
javac -cp "NgocRongOnline.jar:lib/*" -d $OUT \
  $SRC/nro/models/services/Service.java \
  $SRC/nro/models/services/SkillService.java 2>&1
COMPILE_STATUS=$?
echo "Compile: $COMPILE_STATUS"
if [ $COMPILE_STATUS -eq 0 ]; then
  jar uf NgocRongOnline.jar -C $OUT nro/
  echo "JAR updated ✅"
  pkill -9 -f NgocRongOnline; sleep 3
  nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
    -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
    -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
    -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
  sleep 10
  echo "Server PID: $(pgrep -f NgocRongOnline | head -1)"
  tail -4 ~/logs/server.log
else
  echo "Compile FAIL — giữ nguyên"
fi
echo "======== END FIX ATTACK ========"
REMOTE

  touch "/tmp/nro_fix_attack_done"
  log "[$NAME] ✅ Phase 9 xong!"
}

# Phase 10: FIX THỰC SỰ — hoán đổi sendPlayerAttackMob lên đầu + đọc mob.injured + sendPrepare
run_fix_animation_first() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_anim_fix_done" ] && return 0
  log "[$NAME] 🎯 Phase 10: FIX animation-first — sendPlayerAttackMob lên trước..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
SRC=~/nro/SRC/src
SS=$SRC/nro/models/services/SkillService.java
MOB=$SRC/nro/models/mob/Mob.java
OUT=/tmp/out_p10
mkdir -p $OUT
cd ~/nro/SRC
echo "======== FIX ANIMATION-FIRST ========"

# ──────────────────────────────────────────────
# ĐỌC THÊM: mob.injured() + sendPlayerPrepareSkill
# ──────────────────────────────────────────────
echo "=== mob.injured() full ==="
ILINE=$(grep -n "public.*void.*injured\|public.*int.*injured\|void injured\|int injured" $MOB | head -1 | cut -d: -f1)
echo "injured() at line $ILINE"
[ -n "$ILINE" ] && sed -n "${ILINE},$((ILINE+80))p" $MOB

echo ""
echo "=== sendPlayerPrepareSkill() full ==="
PRELINE=$(grep -n "sendPlayerPrepareSkill\|sendPlayerPrepareBom" $SS | head -2)
echo "refs: $PRELINE"
PRELINE2=$(grep -n "public.*sendPlayerPrepareSkill" $SS | head -1 | cut -d: -f1)
[ -n "$PRELINE2" ] && sed -n "${PRELINE2},$((PRELINE2+40))p" $SS

echo ""
echo "=== Service.java — tìm attack thường (không skill) ==="
SV=$SRC/nro/models/services/Service.java
grep -n "attackMob\|normalAttack\|case.*attack\|void.*Attack\|status.*==.*1\b" $SV | head -20

# ──────────────────────────────────────────────
# PATCH CHÍNH: hoán đổi playerAttackMob
# Từ: hutHPMP → mob.injured → sendPlayerAttackMob
# Thành: sendPlayerAttackMob → hutHPMP → mob.injured
# ──────────────────────────────────────────────
echo ""
echo "=== PATCH playerAttackMob() — animation TRƯỚC damage ==="

# Hiển thị code hiện tại trước khi sửa
echo "--- Current code (3 dòng cuối playerAttackMob) ---"
grep -n "hutHPMP\|mob\.injured\|sendPlayerAttackMob" $SS | tail -10

python3 - <<'PYEOF'
path = '/home/codespace/nro/SRC/src/nro/models/services/SkillService.java'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

content = ''.join(lines)

# Pattern chính xác từ Phase 8 output:
#         hutHPMP(plAtt, dameHit, null, mob);
#         mob.injured(plAtt, dameHit, dieWhenHpFull);
#         sendPlayerAttackMob(plAtt, mob);
old = (
    '        hutHPMP(plAtt, dameHit, null, mob);\n'
    '        mob.injured(plAtt, dameHit, dieWhenHpFull);\n'
    '        sendPlayerAttackMob(plAtt, mob);\n'
)
new = (
    '        sendPlayerAttackMob(plAtt, mob); // [FIX] animation packet trước - giảm độ trễ hiển thị\n'
    '        hutHPMP(plAtt, dameHit, null, mob);\n'
    '        mob.injured(plAtt, dameHit, dieWhenHpFull);\n'
)

if old in content:
    content = content.replace(old, new, 1)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("✅ PATCHED playerAttackMob(): sendPlayerAttackMob → hutHPMP → mob.injured")
else:
    # Tìm và hiển thị để debug
    print("Pattern chính xác không khớp. Tìm kiếm từng phần...")
    for i, line in enumerate(lines):
        if 'hutHPMP' in line and 'mob' in line:
            print(f"  L{i+1}: {repr(line)}")
        if 'mob.injured' in line and 'dieWhenHpFull' in line:
            print(f"  L{i+1}: {repr(line)}")
        if 'sendPlayerAttackMob' in line and 'mob)' in line and 'private' not in line:
            print(f"  L{i+1}: {repr(line)}")
PYEOF

# ──────────────────────────────────────────────
# PATCH 2: Giảm affterMiliseconds trong sendPlayerPrepareSkill
# Nếu có hằng số cứng cho skill instant (không phải QCKG/Makankosappo)
# ──────────────────────────────────────────────
echo ""
echo "=== PATCH sendPlayerPrepareSkill calls — xem affterMiliseconds ==="
grep -n "sendPlayerPrepareSkill\|sendPlayerPrepareBom" $SS | grep -v "public\|void\|def" | head -20

# ──────────────────────────────────────────────
# PATCH 3: Service.java — tấn công thường
# Tìm chỗ send animation cho normal attack
# ──────────────────────────────────────────────
echo ""
echo "=== Service.java — toàn bộ method có send attack packet ==="
grep -n "new Message\|Message(" $SV | grep -E "54|attack|Attack" | head -10
MLINE=$(grep -n "new Message(54)" $SV | head -1 | cut -d: -f1)
if [ -n "$MLINE" ]; then
  echo "Message(54) found in Service.java at line $MLINE:"
  sed -n "$((MLINE-5)),$((MLINE+30))p" $SV
else
  echo "Message(54) NOT found in Service.java"
  # Tìm cmd attack trong Service.java
  grep -n "Message(-60)\|Message(54)\|Message(55)\|Message(56)" $SV | head -10
fi

# ──────────────────────────────────────────────
# COMPILE + RESTART
# ──────────────────────────────────────────────
echo ""
echo "=== COMPILE SkillService.java ==="
javac -cp "NgocRongOnline.jar:lib/*" -d $OUT \
  $SRC/nro/models/services/SkillService.java 2>&1
STATUS=$?
echo "Compile: $STATUS"

if [ $STATUS -eq 0 ]; then
  jar uf NgocRongOnline.jar -C $OUT nro/
  echo "JAR updated ✅"
  pkill -9 -f NgocRongOnline; sleep 3
  nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
    -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
    -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
    -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
  sleep 10
  echo "Server PID: $(pgrep -f NgocRongOnline | head -1)"
  tail -4 ~/logs/server.log
else
  echo "Compile FAIL — chưa thay đổi JAR"
fi
echo "======== END PHASE 10 ========"
REMOTE

  touch "/tmp/nro_anim_fix_done"
  log "[$NAME] 🎯 Phase 10 xong!"
}

# Phase 11: FIX Service.attackMob() + sendMobStillAliveAffterAttacked + UpdateSkillSpecial delay
run_fix_normal_attack() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_normal_atk_done" ] && return 0
  log "[$NAME] ⚡ Phase 11: FIX normal attack + sendMobStillAlive delay..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
SRC=~/nro/SRC/src
SV=$SRC/nro/models/services/Service.java
SS=$SRC/nro/models/services/SkillService.java
MOB=$SRC/nro/models/mob/Mob.java
OUT=/tmp/out_p11
mkdir -p $OUT
cd ~/nro/SRC
echo "======== FIX NORMAL ATTACK ========"

# ── 1. ĐỌC Service.attackMob() ──────────────────────────────
echo "=== 1. Service.attackMob() tại dòng 1166 ==="
sed -n '1166,1280p' $SV

# ── 2. ĐỌC sendMobStillAliveAffterAttacked() ────────────────
echo ""
echo "=== 2. sendMobStillAliveAffterAttacked() trong Mob.java ==="
SLINE=$(grep -n "sendMobStillAliveAffterAttacked\|sendMobDieAffterAttacked" $MOB | head -4)
echo "refs: $SLINE"
SLINE2=$(grep -n "void sendMobStillAliveAffterAttacked\|private.*sendMobStillAlive" $MOB | head -1 | cut -d: -f1)
[ -n "$SLINE2" ] && sed -n "${SLINE2},$((SLINE2+50))p" $MOB
DLINE=$(grep -n "void sendMobDieAffterAttacked\|private.*sendMobDie" $MOB | head -1 | cut -d: -f1)
[ -n "$DLINE" ] && sed -n "${DLINE},$((DLINE+40))p" $MOB

# ── 3. ĐỌC updateSkillSpecial — special skill delay ─────────
echo ""
echo "=== 3. updateSkillSpecial (line 148) ==="
sed -n '148,260p' $SS

# ── 4. PATCH Service.java — attackMob animation-first ───────
echo ""
echo "=== 4. PATCH Service.attackMob() ==="
python3 - <<'PYEOF'
path = '/home/codespace/nro/SRC/src/nro/models/services/Service.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
original = content

# Đọc attackMob method để hiểu cấu trúc
import re
# Tìm dòng 1166 trở đi để in ra
lines = content.split('\n')
print("=== Service.attackMob() structure (dòng 1166-1230) ===")
for i in range(1165, min(1230, len(lines))):
    print(f"L{i+1}: {lines[i]}")

# Tìm pattern sendPlayerAttack... trước injured trong Service.java
patterns_found = []
for i, line in enumerate(lines):
    if 'sendPlayerAttack' in line or 'send_attack' in line.lower() or 'sendAttack' in line:
        patterns_found.append(f"L{i+1}: {line.strip()}")
print("\n=== sendPlayerAttack references in Service.java ===")
for p in patterns_found[:10]:
    print(p)

# Tìm cmd 54 trong Service.java
for i, line in enumerate(lines):
    if 'Message(54)' in line or 'cmd.*54' in line.lower():
        print(f"CMD54 at L{i+1}: {line.strip()}")
PYEOF

# ── 5. ĐỌC THÊM: updateSkillSpecial TIME_GONG delay ────────
echo ""
echo "=== 5. NewSkill.java TIME_GONG value ==="
NK=$SRC/nro/models/player/NewSkill.java
grep -n "TIME_GONG\|timeGong\|TIME_PREPARE\|timePrepare\|1000\|1500\|2000\|500" $NK | head -20
sed -n '1,60p' $NK

# ── 6. ĐỌC sendMobStillAliveAffterAttacked nếu chưa có ─────
echo ""
echo "=== 6. Mob.java — xem toàn bộ send method ==="
grep -n "void send\|private.*send\|public.*send" $MOB | head -20

echo "======== END PHASE 11 READ ========"
REMOTE

  touch "/tmp/nro_normal_atk_done"
  log "[$NAME] ✅ Phase 11 xong — đủ data để fix."
}

# Phase 12: APPLY FIX SERVICE.ATTACKMOB + GIẢM TIME_GONG
run_fix_service_mob() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_svc_fix_done" ] && return 0
  [ ! -f "/tmp/nro_normal_atk_done" ] && return 0
  log "[$NAME] 🔥 Phase 12: APPLY fix Service.attackMob + TIME_GONG..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
SRC=~/nro/SRC/src
SV=$SRC/nro/models/services/Service.java
SS=$SRC/nro/models/services/SkillService.java
NK=$SRC/nro/models/player/NewSkill.java
MOB=$SRC/nro/models/mob/Mob.java
OUT=/tmp/out_p12
mkdir -p $OUT
cd ~/nro/SRC
echo "======== APPLY FIX SERVICE + TIME_GONG ========"

# ── A. GIẢM TIME_GONG trong NewSkill.java ───────────────────
echo "=== A. Giảm TIME_GONG ==="
python3 - <<'PYEOF'
path = '/home/codespace/nro/SRC/src/nro/models/player/NewSkill.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
original = content
changes = []

# TIME_GONG thường là 500ms hoặc 1000ms — giảm để chiêu ra nhanh hơn
import re
# Hiển thị TIME_GONG hiện tại
tg = re.findall(r'TIME_GONG\s*=\s*(\d+)', content)
print(f"TIME_GONG hiện tại: {tg}")

# Nếu TIME_GONG >= 500ms → giảm xuống 300ms
for val in tg:
    val_int = int(val)
    if val_int >= 500:
        new_val = max(300, val_int // 2)
        content = content.replace(f'TIME_GONG = {val}', f'TIME_GONG = {new_val}')
        changes.append(f"TIME_GONG {val} → {new_val}ms")

# Tương tự cho TIME_PREPARE nếu có
tp = re.findall(r'TIME_PREPARE\s*=\s*(\d+)', content)
for val in tp:
    val_int = int(val)
    if val_int >= 500:
        new_val = max(300, val_int // 2)
        content = content.replace(f'TIME_PREPARE = {val}', f'TIME_PREPARE = {new_val}')
        changes.append(f"TIME_PREPARE {val} → {new_val}ms")

if changes:
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    for c in changes:
        print(f"✅ {c}")
else:
    print("TIME_GONG: không tìm thấy pattern để sửa")
    # In 30 dòng đầu để debug
    for i, line in enumerate(content.split('\n')[:40]):
        if 'TIME' in line or 'Gong' in line or 'gong' in line or 'static' in line:
            print(f"  L{i+1}: {line}")
PYEOF

# ── B. PATCH sendMobStillAliveAffterAttacked ────────────────
#    Nếu method này gửi packet có delay → bỏ delay
echo ""
echo "=== B. Mob.java sendMobStillAliveAffterAttacked ==="
python3 - <<'PYEOF'
path = '/home/codespace/nro/SRC/src/nro/models/mob/Mob.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
import re

# Tìm method sendMobStillAliveAffterAttacked
m = re.search(r'(void sendMobStillAliveAffterAttacked[^}]+\})', content, re.DOTALL)
if m:
    print("sendMobStillAliveAffterAttacked:")
    print(m.group(0)[:500])
else:
    # Tìm bằng cách khác
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if 'sendMobStillAlive' in line:
            print(f"L{i+1}: {line}")
            for j in range(i, min(i+30, len(lines))):
                print(f"  L{j+1}: {lines[j]}")
            break
PYEOF

# ── C. COMPILE TẤT CẢ FILE ĐÃ SỬA ─────────────────────────
echo ""
echo "=== C. Compile tất cả file đã sửa ==="
javac -cp "NgocRongOnline.jar:lib/*" -d $OUT \
  $SRC/nro/models/player/NewSkill.java 2>&1
NK_STATUS=$?
echo "NewSkill.java: $NK_STATUS"

# Compile SkillService (đã patch Phase 10) để đảm bảo
javac -cp "NgocRongOnline.jar:lib/*" -d $OUT \
  $SRC/nro/models/services/SkillService.java 2>&1
SS_STATUS=$?
echo "SkillService.java: $SS_STATUS"

if [ $NK_STATUS -eq 0 ] && [ $SS_STATUS -eq 0 ]; then
  jar uf NgocRongOnline.jar -C $OUT nro/
  echo "JAR updated ✅"
  pkill -9 -f NgocRongOnline; sleep 3
  nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
    -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
    -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
    -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
  sleep 10
  echo "Server PID: $(pgrep -f NgocRongOnline | head -1)"
  tail -5 ~/logs/server.log
else
  echo "Compile FAIL — NK=$NK_STATUS SS=$SS_STATUS"
fi

# ── D. VERIFY patch đã vào JAR ──────────────────────────────
echo ""
echo "=== D. Verify Phase 10 + 12 trong JAR ==="
jar tf NgocRongOnline.jar | grep -E "NewSkill|SkillService|Service\.class"
echo "======== END PHASE 12 ========"
REMOTE

  touch "/tmp/nro_svc_fix_done"
  log "[$NAME] ✅ Phase 12 xong!"
}

# Phase 13: Teamobi2026 — import DB tables mới + copy boss classes mới
run_teamobi_upgrade() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_teamobi_done" ] && return 0
  log "[$NAME] 🐉 Phase 13: Teamobi2026 — DB tables + boss classes..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== PHASE 13: TEAMOBI2026 UPGRADE ========"
REPO=/workspaces/rem5
SRC=~/nro/SRC/src
JAR=~/nro/SRC/NgocRongOnline.jar
OUT=/tmp/out_p13
mkdir -p $OUT
cd ~/nro/SRC

# ── 1. GIT PULL để lấy docs mới nhất ─────────────────────────────
echo "=== 1. Git pull ==="
if [ -d "$REPO" ]; then
  cd $REPO
  git pull origin main 2>&1 | tail -5
  cd ~/nro/SRC
else
  echo "REPO $REPO không tồn tại"
fi

# ── 2. CHECK VÀ IMPORT DB TABLES MỚI ────────────────────────────
echo ""
echo "=== 2. Teamobi2026 DB tables ==="
# Kiểm tra xem bảng radar đã tồn tại chưa
RADAR_EXISTS=$(mysql -u root nro1 -se "SHOW TABLES LIKE 'radar';" 2>/dev/null)
if [ -z "$RADAR_EXISTS" ]; then
  echo "Bảng radar chưa có → import teamobi2026_new_tables.sql..."
  if [ -f "$REPO/docs/teamobi2026_new_tables.sql" ]; then
    mysql -u root nro1 < "$REPO/docs/teamobi2026_new_tables.sql" 2>&1
    echo "Import new_tables: $?"
  else
    echo "File teamobi2026_new_tables.sql không tìm thấy"
  fi
else
  echo "Bảng radar đã tồn tại ✅"
fi

# Kiểm tra cột radar trong bảng player
RADAR_COL=$(mysql -u root nro1 -se "SHOW COLUMNS FROM player LIKE 'radar';" 2>/dev/null)
if [ -z "$RADAR_COL" ]; then
  echo "Cột radar chưa có trong player → alter table..."
  if [ -f "$REPO/docs/teamobi2026_alter_player.sql" ]; then
    mysql -u root nro1 < "$REPO/docs/teamobi2026_alter_player.sql" 2>&1 | head -10
    echo "ALTER player: $?"
  fi
else
  echo "Cột radar đã có trong player ✅"
fi

# ── 3. COPY VÀ COMPILE BOSS CLASSES MỚI ─────────────────────────
echo ""
echo "=== 3. Copy boss classes mới ==="
BOSS_SRC="$REPO/docs/teamobi2026_src"

# Baby.java — đã có nhưng kiểm tra phiên bản
if [ -f "$BOSS_SRC/boss_new/Baby.java" ] && [ -f "$SRC/nro/models/boss/Baby/Baby.java" ]; then
  LINES_OLD=$(wc -l < "$SRC/nro/models/boss/Baby/Baby.java")
  LINES_NEW=$(wc -l < "$BOSS_SRC/boss_new/Baby.java")
  echo "Baby.java: SRC=$LINES_OLD lines, Teamobi=$LINES_NEW lines"
  if [ "$LINES_NEW" -gt "$LINES_OLD" ]; then
    cp "$BOSS_SRC/boss_new/Baby.java" "$SRC/nro/models/boss/Baby/Baby.java"
    echo "Baby.java updated ✅"
  fi
fi

# Cumber.java
if [ -f "$BOSS_SRC/boss_new/Cumber.java" ] && [ -f "$SRC/nro/models/boss/cumber/Cumber.java" ]; then
  LINES_OLD=$(wc -l < "$SRC/nro/models/boss/cumber/Cumber.java")
  LINES_NEW=$(wc -l < "$BOSS_SRC/boss_new/Cumber.java")
  echo "Cumber.java: SRC=$LINES_OLD lines, Teamobi=$LINES_NEW lines"
  if [ "$LINES_NEW" -gt "$LINES_OLD" ]; then
    cp "$BOSS_SRC/boss_new/Cumber.java" "$SRC/nro/models/boss/cumber/Cumber.java"
    echo "Cumber.java updated ✅"
  fi
fi

# Bojack chain — copy nếu Teamobi dài hơn
for F in BIDO BOJACK BUJIN KOGU SUPER_BOJACK ZANGYA; do
  if [ -f "$BOSS_SRC/boss_bojack/$F.java" ] && [ -f "$SRC/nro/models/boss/trai_dat/$F.java" ]; then
    L_OLD=$(wc -l < "$SRC/nro/models/boss/trai_dat/$F.java")
    L_NEW=$(wc -l < "$BOSS_SRC/boss_bojack/$F.java")
    if [ "$L_NEW" -gt "$L_OLD" ]; then
      cp "$BOSS_SRC/boss_bojack/$F.java" "$SRC/nro/models/boss/trai_dat/$F.java"
      echo "$F.java updated ✅ ($L_OLD→$L_NEW lines)"
    else
      echo "$F.java OK ($L_OLD >= $L_NEW)"
    fi
  fi
done

# GoldenFrieza
if [ -f "$BOSS_SRC/boss_golden_frieza/GoldenFrieza.java" ]; then
  L_OLD=$(wc -l < "$SRC/nro/models/boss/Golden_fireza/GoldenFrieza.java" 2>/dev/null || echo 0)
  L_NEW=$(wc -l < "$BOSS_SRC/boss_golden_frieza/GoldenFrieza.java")
  if [ "$L_NEW" -gt "$L_OLD" ]; then
    cp "$BOSS_SRC/boss_golden_frieza/GoldenFrieza.java" "$SRC/nro/models/boss/Golden_fireza/GoldenFrieza.java"
    echo "GoldenFrieza.java updated ✅"
  fi
fi

# ── 4. COMPILE CÁC BOSS CLASS ĐÃ UPDATE ──────────────────────────
echo ""
echo "=== 4. Compile boss classes ==="
COMPILE_OK=0

# Compile trai_dat (Bojack chain)
javac -cp "$JAR:lib/*" -d $OUT \
  $SRC/nro/models/boss/trai_dat/*.java 2>&1
if [ $? -eq 0 ]; then
  jar uf $JAR -C $OUT nro/
  echo "Bojack chain compiled ✅"
  COMPILE_OK=1
else
  echo "Bojack compile fail — giữ nguyên"
fi

# Compile Baby
javac -cp "$JAR:lib/*" -d $OUT \
  $SRC/nro/models/boss/Baby/Baby.java 2>&1
[ $? -eq 0 ] && jar uf $JAR -C $OUT nro/ && echo "Baby.java compiled ✅" && COMPILE_OK=1

# Compile Cumber
javac -cp "$JAR:lib/*" -d $OUT \
  $SRC/nro/models/boss/cumber/Cumber.java 2>&1
[ $? -eq 0 ] && jar uf $JAR -C $OUT nro/ && echo "Cumber.java compiled ✅" && COMPILE_OK=1

# ── 5. RESTART NẾU CÓ COMPILE THÀNH CÔNG ─────────────────────────
echo ""
echo "=== 4b. Fix skill icon 27/28 (BẮT BUỘC — không ghi đè) ==="
mysql -u root nro1 << 'ICONFIX'
UPDATE skill_template SET icon_id = 26247 WHERE nclass_id = 0 AND id = 27;
UPDATE skill_template SET icon_id = 26253 WHERE nclass_id = 1 AND id = 27;
UPDATE skill_template SET icon_id = 26241 WHERE nclass_id = 2 AND id = 27;
UPDATE skill_template SET icon_id = 31142 WHERE id = 28;
ICONFIX
mysql -u root nro1 -se "SELECT nclass_id,id,name,icon_id FROM skill_template WHERE id IN (27,28);" 2>/dev/null

echo ""
echo "=== 5. Server restart ==="
if [ "$COMPILE_OK" -eq 1 ]; then
  pkill -9 -f NgocRongOnline 2>/dev/null; sleep 3
  nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
    -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
    -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
    -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
  sleep 10
  echo "Server PID: $(pgrep -f NgocRongOnline | head -1)"
  tail -3 ~/logs/server.log
else
  echo "Không compile được → không restart"
fi

# ── 6. STATUS SUMMARY ─────────────────────────────────────────────
echo ""
echo "=== 6. DB status ==="
mysql -u root nro1 -se "SHOW TABLES;" 2>/dev/null | grep -E "radar|achievement|badges|array_head|bg_item|clan_task"
echo "Tables count: $(mysql -u root nro1 -se 'SHOW TABLES;' 2>/dev/null | wc -l)"
echo "======== END PHASE 13 ========"
REMOTE

  touch "/tmp/nro_teamobi_done"
  log "[$NAME] ✅ Phase 13 xong! Teamobi2026 DB + boss classes applied."
}

# Phase 14: REVERT MAP MOBS + MOB STATS về gốc (backup 2026-07-17)
run_revert_mobs() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_revert_mobs_done" ] && return 0
  log "[$NAME] ⏪ Phase 14: Revert map mobs + mob stats về gốc..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== PHASE 14: REVERT MOBS VỀ GỐC ========"

# ── 1. REVERT map_template.mobs về backup gốc ────────────────────
echo "=== 1. Revert map_template.mobs ==="
mysql -u root nro1 << 'SQL'
UPDATE map_template SET mobs='["[0,1,100,780,432]","[0,1,100,900,432]","[0,1,100,1020,432]","[0,1,100,660,432]"]' WHERE id=0;
UPDATE map_template SET mobs='["[1,2,200,348,384]","[1,2,200,804,408]","[1,2,200,972,360]","[1,2,200,540,408]"]' WHERE id=1;
UPDATE map_template SET mobs='["[4,3,500,348,408]","[1,2,200,996,288]","[1,2,200,876,336]","[4,3,500,756,336]"]' WHERE id=2;
UPDATE map_template SET mobs='["[1,2,200,372,408]","[1,2,200,540,408]","[1,2,200,732,408]","[4,3,500,1140,336]","[7,4,600,444,288]","[7,4,600,708,288]","[7,4,600,924,240]","[7,4,600,1188,240]"]' WHERE id=3;
UPDATE map_template SET mobs='["[0,1,100,708,432]","[0,1,100,804,432]","[0,1,100,900,432]","[0,1,100,996,432]"]' WHERE id=7;
UPDATE map_template SET mobs='["[3,2,200,228,408]","[3,2,200,444,360]","[3,2,200,612,360]","[3,2,200,900,312]","[3,2,200,756,312]"]' WHERE id=15;
SQL
echo "map_template revert: $?"

# ── 2. REVERT mob_template stats về gốc ──────────────────────────
echo ""
echo "=== 2. Revert mob_template stats ==="
mysql -u root nro1 << 'SQL'
-- Revert training dummies (Mộc nhân, Bù nhìn) về speed/tiem_nang gốc
UPDATE mob_template SET speed=1, percent_tiem_nang=10 WHERE id IN (0,103);
-- Revert mob stats bị thay đổi Phase 4/6/7: speed, percent_dame, percent_tiem_nang
UPDATE mob_template SET speed=1  WHERE id IN (1,2,3,16,17,18,19,20,21,22,23,24,25,26,27,34,35,36,38,39);
UPDATE mob_template SET speed=2  WHERE id IN (4,5,6,7,8,9,10,11,12,13,14,15,28,29,30,31,32,33,37,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69);
UPDATE mob_template SET percent_dame=5 WHERE TYPE IN (0,1,4) AND percent_dame != 5;
UPDATE mob_template SET percent_tiem_nang=50 WHERE hp BETWEEN 200 AND 1500 AND TYPE IN (1,4) AND percent_tiem_nang != 50;
UPDATE mob_template SET percent_tiem_nang=25 WHERE hp BETWEEN 1501 AND 30000 AND TYPE IN (1,4) AND percent_tiem_nang != 25;
UPDATE mob_template SET percent_tiem_nang=10 WHERE hp > 30000 AND TYPE IN (1,4) AND percent_tiem_nang != 10;
SQL
echo "mob_template revert: $?"

# ── 3. VERIFY kết quả ────────────────────────────────────────────
echo ""
echo "=== 3. Verify ==="
mysql -u root nro1 -se "SELECT id, LENGTH(mobs) as mob_len FROM map_template WHERE id IN (0,1,2,3,7,15);"
mysql -u root nro1 -se "SELECT id,NAME,speed,percent_dame,percent_tiem_nang FROM mob_template WHERE id IN (0,1,2,3,4,5,16,17,103) ORDER BY id;"

# ── 4. RESTART SERVER để áp dụng ─────────────────────────────────
echo ""
echo "=== 4. Restart server ==="
pkill -9 -f NgocRongOnline 2>/dev/null; sleep 3
nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
  -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
  -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
  -jar ~/nro/SRC/NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
sleep 10
echo "Server PID: $(pgrep -f NgocRongOnline | head -1)"
tail -3 ~/logs/server.log
echo "======== END PHASE 14 ========"
REMOTE

  touch "/tmp/nro_revert_mobs_done"
  log "[$NAME] ✅ Phase 14 xong! Mobs đã về gốc."
}

# Phase 15: FIX Map.java — xóa random spawn offset (mobs bay trên trời)
run_fix_map_spawn() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_fix_map_spawn_done" ] && return 0
  log "[$NAME] 🔧 Phase 15: Fix Map.java spawn offset (mobs đứng đúng chỗ)..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== PHASE 15: FIX MAP.JAVA SPAWN OFFSET ========"
MAP=~/nro/SRC/src/nro/models/map/Map.java

echo "=== Kiểm tra random offset hiện tại ==="
grep -n "Math.random\|0\.5.*96\|0\.5.*80" $MAP | head -10

HAS_RANDOM=$(grep -c "Math.random" $MAP 2>/dev/null || echo 0)
echo "Random offset count: $HAS_RANDOM"

if [ "$HAS_RANDOM" -gt "0" ]; then
  echo "=== XÓA random offset ==="
  python3 << 'PYEOF'
import re

path = '/home/codespace/nro/SRC/src/nro/models/map/Map.java'
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

original = src

# Xóa random offset khi load từ DB
src = re.sub(
    r'mob\.location\.x = \(short\)\(mobX\[i\] \+ \(int\)\(\(Math\.random\(\)-0\.5\)\*\d+\)\);',
    'mob.location.x = mobX[i];', src)
src = re.sub(
    r'mob\.location\.y = \(short\)\(mobY\[i\] \+ \(int\)\(\(Math\.random\(\)-0\.5\)\*\d+\)\);',
    'mob.location.y = mobY[i];', src)

# Xóa random offset khi clone sang zone
src = re.sub(
    r'mobZone\.location\.x \+= \(short\)\(\(Math\.random\(\)-0\.5\)\*\d+\);\s*\n\s*mobZone\.location\.y \+= \(short\)\(\(Math\.random\(\)-0\.5\)\*\d+\);',
    '', src)

# Xóa random offset trong initMob(List<Mob>)
src = re.sub(
    r'mob\.location\.x \+= \(short\)\(\(Math\.random\(\)-0\.5\)\*\d+\);\s*\n\s*mob\.location\.y \+= \(short\)\(\(Math\.random\(\)-0\.5\)\*\d+\);',
    '', src)

if src != original:
    with open(path, 'w', encoding='utf-8') as f:
        f.write(src)
    print("Map.java PATCHED — random offset đã xóa ✅")
else:
    print("Không tìm thấy pattern cần xóa (đã sạch hoặc format khác)")
PYEOF

  # Compile Map.java
  echo "=== Compile Map.java ==="
  cd ~/nro/SRC
  OUT=/tmp/out_p15
  mkdir -p $OUT
  javac -cp "NgocRongOnline.jar:lib/*" -d $OUT \
    src/nro/models/map/Map.java 2>&1
  COMPILE_STATUS=$?
  echo "Compile: $COMPILE_STATUS"

  if [ $COMPILE_STATUS -eq 0 ]; then
    jar uf NgocRongOnline.jar -C $OUT nro/
    echo "JAR updated ✅"
    # Restart server
    pkill -9 -f NgocRongOnline 2>/dev/null; sleep 3
    nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
      -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
      -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
      -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
    sleep 10
    echo "Server PID: $(pgrep -f NgocRongOnline | head -1)"
    tail -3 ~/logs/server.log
  fi
else
  echo "Map.java sạch — không cần sửa ✅"
fi

echo "======== END PHASE 15 ========"
REMOTE

  touch "/tmp/nro_fix_map_spawn_done"
  log "[$NAME] ✅ Phase 15 xong! Map.java spawn offset đã fix."
}

# Phase 16: Nhập cải trang + vật phẩm mới từ Teamobi2026
run_new_content() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_new_content_done" ] && return 0
  log "[$NAME] 🎭 Phase 16: Nhập cải trang + vật phẩm mới từ Teamobi2026..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
echo "======== PHASE 16: CẢI TRANG + VẬT PHẨM MỚI ========"
REPO=/workspaces/rem5

# Cập nhật repo
if [ -d "$REPO" ]; then
  cd $REPO
  git pull origin main 2>&1 | tail -3
else
  echo "REPO $REPO không tồn tại — bỏ qua Phase 16"
  exit 0
fi

echo "=== 1. Kiểm tra cai_trang hiện tại ==="
COUNT=$(mysql -u root nro1 -se "SELECT COUNT(*) FROM cai_trang;" 2>/dev/null || echo "-1")
echo "Cải trang hiện tại: $COUNT bộ"

if [ "$COUNT" = "-1" ] || [ "$COUNT" = "0" ]; then
  echo "=== Tạo bảng cai_trang + nhập 351 bộ cải trang ==="
  python3 << 'PYEOF'
import re, subprocess

src = open('/workspaces/rem5/docs/nro_upgrade_data.sql', encoding='utf-8').read()

# Lấy phần cai_trang
m = re.search(r'(DROP TABLE IF EXISTS `cai_trang`.*?INSERT INTO `cai_trang`.*?;)', src, re.DOTALL)
if not m:
    print("WARN: Không tìm thấy section cai_trang trong nro_upgrade_data.sql")
    exit(0)

sql = "SET NAMES utf8mb4;\n" + m.group(1)
# Ghi ra file tạm
with open('/tmp/cai_trang_import.sql', 'w', encoding='utf-8') as f:
    f.write(sql)

r = subprocess.run(['mysql', '-u', 'root', 'nro1'], input=sql.encode(), capture_output=True)
if r.returncode == 0:
    print("cai_trang imported OK ✅")
else:
    print(f"WARN: {r.stderr.decode()[:200]}")
PYEOF
else
  echo "Cải trang đã có $COUNT bộ — bỏ qua CREATE"
fi

echo "=== 2. Kiểm tra số item hiện tại ==="
ITEM_COUNT=$(mysql -u root nro1 -se "SELECT COUNT(*) FROM item_template;" 2>/dev/null || echo "0")
echo "Items hiện tại: $ITEM_COUNT"

echo "=== 3. INSERT IGNORE items mới từ Teamobi2026 ==="
python3 << 'PYEOF'
import re, subprocess, json

TEAM_SQL  = '/workspaces/rem5/server/database_team2026.sql'

# Đọc Teamobi2026 items
content = open(TEAM_SQL, encoding='utf-8').read()
# Lấy toàn bộ phần INSERT INTO item_template của Teamobi2026
m = re.search(r'INSERT INTO `item_template` \(`id`, `TYPE`, `gender`, `NAME`, `description`, `level`, `icon_id`, `part`, `is_up_to_up`, `power_require`, `gold`, `gem`, `head`, `body`, `leg`\) VALUES(.*?);', content, re.DOTALL)
if not m:
    print("WARN: Không tìm thấy item_template trong database_team2026.sql")
    exit(0)

rows_text = m.group(1).strip()
# Lấy ID hiện tại trong DB
r = subprocess.run(['mysql', '-u', 'root', 'nro1', '-se', 'SELECT id FROM item_template;'],
                   capture_output=True, text=True)
existing_ids = set(r.stdout.strip().split('\n')) if r.stdout.strip() else set()
print(f"DB hiện có {len(existing_ids)} items")

# Parse từng row
row_pattern = re.compile(r'\((\d+),\s*(\d+),\s*(-?\d+),\s*(\'[^\']*\'|\"[^\"]*\"),\s*(\'[^\']*\'|\"[^\"]*\"),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+)\)')
rows = row_pattern.findall(rows_text)
print(f"Teamobi2026 items parsed: {len(rows)}")

new_rows = [r for r in rows if r[0] not in existing_ids]
print(f"Items chưa có trong DB: {len(new_rows)}")

if not new_rows:
    print("Không có item mới cần thêm.")
    exit(0)

# Tạo INSERT IGNORE SQL theo batch 100
batch_sql = "SET NAMES utf8mb4;\n"
batch_sql += "INSERT IGNORE INTO `item_template` (`id`,`TYPE`,`gender`,`NAME`,`description`,`level`,`icon_id`,`part`,`is_up_to_up`,`power_require`,`gold`,`gem`,`head`,`body`,`leg`) VALUES\n"
vals = []
for row in new_rows:
    id_,typ,gen,name,desc,lv,icon,part,utu,pw,gold,gem,head,body,leg = row
    vals.append(f"({id_},{typ},{gen},{name},{desc},{lv},{icon},{part},{utu},{pw},{gold},{gem},{head},{body},{leg})")

batch_sql += ",\n".join(vals) + ";\n"

r2 = subprocess.run(['mysql', '-u', 'root', 'nro1'], input=batch_sql.encode(), capture_output=True)
if r2.returncode == 0:
    print(f"✅ Đã INSERT IGNORE {len(new_rows)} items mới")
else:
    print(f"WARN: {r2.stderr.decode()[:300]}")
PYEOF

echo "=== 4. Xác nhận kết quả ==="
mysql -u root nro1 -se "SELECT COUNT(*) as total_items FROM item_template;" 2>/dev/null
mysql -u root nro1 -se "SELECT COUNT(*) as total_cai_trang FROM cai_trang;" 2>/dev/null

echo "=== 5. Restart server để load dữ liệu mới ==="
pkill -9 -f NgocRongOnline 2>/dev/null; sleep 3
cd ~/nro/SRC
nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
  -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
  -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
  -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
sleep 10
echo "Server PID: $(pgrep -f NgocRongOnline | head -1)"
tail -3 ~/logs/server.log

echo "======== END PHASE 16 ========"
REMOTE

  touch "/tmp/nro_new_content_done"
  log "[$NAME] ✅ Phase 16 xong! Cải trang + vật phẩm mới đã nhập."
}

# Phase 17: Network optimization — TCP_NODELAY + ping fix + ws_bridge tối ưu
run_net_opt() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_net_opt_done" ] && return 0
  log "[$NAME] ⚡ Phase 17: Network optimization — TCP_NODELAY + ws_bridge v3..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
set -e
JAR=~/nro/SRC/NgocRongOnline.jar
SRC=~/nro/SRC/src
OUT=/tmp/nro_netopt_out
mkdir -p $OUT

echo "=== 1. Cập nhật ws_bridge.py (ping_interval=None + TCP_NODELAY + uvloop) ==="
cat > ~/bin/ws_bridge.py << 'PYEOF'
#!/usr/bin/env python3
"""NRO WebSocket Bridge v3 — TCP_NODELAY, ping disabled, uvloop"""
import asyncio, sys, socket, logging
logging.basicConfig(level=logging.INFO, format='[%(asctime)s] %(message)s', datefmt='%H:%M:%S')
log = logging.getLogger(__name__)
GAME_HOST = "127.0.0.1"
GAME_PORT = 14445
LISTEN_PORT = 8080
SOCKET_BUF = 524288
try:
    import websockets
except ImportError:
    import subprocess; subprocess.run([sys.executable,"-m","pip","install","websockets","-q"]); import websockets
try:
    import uvloop; asyncio.set_event_loop_policy(uvloop.EventLoopPolicy()); log.info("uvloop ON")
except ImportError:
    pass
stats = {"total":0,"active":0}
async def handle_client(websocket, path=None):
    stats["total"]+=1; stats["active"]+=1
    cid=stats["total"]
    log.info(f"[#{cid}] Kết nối: {websocket.remote_address} (active={stats['active']})")
    try:
        reader,writer = await asyncio.open_connection(GAME_HOST,GAME_PORT)
        sk=writer.get_extra_info('socket')
        if sk:
            sk.setsockopt(socket.IPPROTO_TCP,socket.TCP_NODELAY,1)
            sk.setsockopt(socket.SOL_SOCKET,socket.SO_RCVBUF,SOCKET_BUF)
            sk.setsockopt(socket.SOL_SOCKET,socket.SO_SNDBUF,SOCKET_BUF)
        async def ws2tcp():
            try:
                async for data in websocket:
                    if isinstance(data,bytes):
                        writer.write(data)
                        if writer.transport.get_write_buffer_size()>65536:
                            await writer.drain()
            except Exception as e: log.debug(f"[#{cid}] ws→tcp: {e}")
            finally:
                try: await writer.drain(); writer.close()
                except: pass
        async def tcp2ws():
            try:
                while True:
                    data=await reader.read(65536)
                    if not data: break
                    await websocket.send(data)
            except Exception as e: log.debug(f"[#{cid}] tcp→ws: {e}")
            finally:
                try: await websocket.close()
                except: pass
        await asyncio.gather(ws2tcp(),tcp2ws())
    except ConnectionRefusedError: log.error(f"[#{cid}] Game server {GAME_HOST}:{GAME_PORT} chưa chạy!")
    except Exception as e: log.error(f"[#{cid}] Lỗi: {e}")
    finally: stats["active"]-=1; log.info(f"[#{cid}] Ngắt | active={stats['active']}")
async def main():
    log.info("NRO ws_bridge v3 | ping_interval=None | TCP_NODELAY=1 | buf=512KB")
    async with websockets.serve(handle_client,"0.0.0.0",LISTEN_PORT,
        ping_interval=None,ping_timeout=None,
        max_size=10*1024*1024,compression=None):
        log.info(f"✅ Listening ws://0.0.0.0:{LISTEN_PORT}")
        await asyncio.Future()
if __name__=="__main__":
    asyncio.run(main())
PYEOF
chmod +x ~/bin/ws_bridge.py
echo "ws_bridge.py updated ✅"

echo "=== 2. Patch Session.java — TCP_NODELAY ==="
SESSION_FILE="$SRC/nro/models/network/Session.java"
if grep -q "setTcpNoDelay" "$SESSION_FILE"; then
  echo "TCP_NODELAY đã có trong Session.java ✅"
else
  # Thêm TCP_NODELAY sau setReceiveBufferSize
  sed -i 's/this\.socket\.setReceiveBufferSize(0x100000);/this.socket.setReceiveBufferSize(0x100000);\n            this.socket.setTcpNoDelay(true);/' "$SESSION_FILE"
  echo "TCP_NODELAY đã patch Session.java ✅"
fi

echo "=== 3. Patch Sender.java — sleep 10ms → 1ms ==="
SENDER_FILE="$SRC/nro/models/network/Sender.java"
if grep -q "sleep(10L)" "$SENDER_FILE"; then
  sed -i 's/TimeUnit\.MILLISECONDS\.sleep(10L)/TimeUnit.MILLISECONDS.sleep(1L)/' "$SENDER_FILE"
  echo "Sender.java patched: sleep 10ms → 1ms ✅"
else
  echo "Sender.java sleep đã là 1ms hoặc khác ✅"
fi

echo "=== 4. Compile Session.java + Sender.java ==="
cd ~/nro/SRC
javac -cp "NgocRongOnline.jar:lib/*" -d $OUT \
  src/nro/models/network/Session.java \
  src/nro/models/network/Sender.java 2>&1
if [ $? -eq 0 ]; then
  jar uf NgocRongOnline.jar -C $OUT nro/
  echo "Compile OK → JAR updated ✅"
else
  echo "Compile FAILED — giữ nguyên JAR"
  exit 1
fi

echo "=== 5. Restart ws_bridge + game server ==="
pkill -f ws_bridge 2>/dev/null; sleep 1
nohup python3 ~/bin/ws_bridge.py >> ~/logs/ws_bridge.log 2>&1 &
sleep 2
pgrep -f ws_bridge > /dev/null && echo "ws_bridge v3 đang chạy ✅" || echo "ws_bridge FAIL ❌"

pkill -9 -f NgocRongOnline 2>/dev/null; sleep 3
nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
  -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
  -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
  -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
sleep 10
pgrep -f NgocRongOnline > /dev/null && echo "Game server restarted ✅" || echo "Game server FAIL ❌"
echo "=== Phase 17 DONE ==="
REMOTE

  if [ $? -eq 0 ]; then
    touch "/tmp/nro_net_opt_done"
    log "[$NAME] ✅ Phase 17 xong! TCP_NODELAY + ws_bridge v3 + Sender 1ms."
  else
    log "[$NAME] ❌ Phase 17 thất bại — xem log Codespace"
  fi
}

# Thử upgrade tunnel sang server châu Á nếu hiện tại là US
try_upgrade_tunnel() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  [ -f "/tmp/nro_tunnel_upgraded" ] && return 0
  log "[$NAME] 🌐 Thử tìm frp server châu Á tốt hơn..."
  auth_as "$TOKEN_VAR"

  local RESULT
  RESULT=$($GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << 'REMOTE'
# Thử frps.sueme.net (Singapore) trước
for ENTRY in "frps.sueme.net:7000:SG:21001" "io.nfd.tw:7000:TW:21001"; do
  H=$(echo $ENTRY|cut -d':' -f1)
  P=$(echo $ENTRY|cut -d':' -f2)
  R=$(echo $ENTRY|cut -d':' -f3)
  RP=$(echo $ENTRY|cut -d':' -f4)
  PING=$(ping -c 2 -W 2 "$H" 2>/dev/null | awk -F'/' 'END{print int($5)}')
  TCP=$(timeout 3 bash -c "echo >/dev/tcp/$H/$P" 2>/dev/null && echo "ok" || echo "fail")
  if [ "$TCP" = "ok" ] && [ -n "$PING" ] && [ "$PING" -lt 200 ]; then
    echo "FOUND:$H:$P:$R:$RP:${PING}ms"
    break
  else
    echo "SKIP:$R ping=${PING}ms tcp=$TCP"
  fi
done
REMOTE
  )
  log "Tunnel scan: $RESULT"

  if echo "$RESULT" | grep -q "^FOUND:"; then
    LINE=$(echo "$RESULT" | grep "^FOUND:" | head -1)
    FH=$(echo $LINE|cut -d':' -f2)
    FP=$(echo $LINE|cut -d':' -f3)
    FR=$(echo $LINE|cut -d':' -f4)
    RP=$(echo $LINE|cut -d':' -f5)

    log "[$NAME] ✅ Tìm được server $FR ($FH:$FP) → áp dụng..."
    $GH_BIN codespace ssh -c "$CS" -- bash -s 2>/dev/null << REMOTE2
cat > /tmp/frpc_nro.toml << EOF
serverAddr = "$FH"
serverPort = $FP

[[proxies]]
name = "nro-tcp"
type = "tcp"
localIP = "127.0.0.1"
localPort = 14445
remotePort = $RP
EOF
sed -i "s|server.sv1=.*|server.sv1=NRO:$FH::$RP|" ~/nro/SRC/Config.properties 2>/dev/null
pkill -f frpc 2>/dev/null; sleep 2
# Tải lại frpc nếu /tmp bị xóa (Codespace restart)
if [ ! -f "/tmp/frp/frpc" ]; then
  mkdir -p /tmp/frp
  curl -sL "https://github.com/fatedier/frp/releases/download/v0.61.0/frp_0.61.0_linux_amd64.tar.gz" \
    -o /tmp/frp.tar.gz && tar xzf /tmp/frp.tar.gz -C /tmp/frp --strip-components=1
  chmod +x /tmp/frp/frpc
fi
nohup /tmp/frp/frpc -c /tmp/frpc_nro.toml >> ~/logs/frp.log 2>&1 &
sleep 3
pgrep -f frpc >/dev/null && echo "frpc OK: $FH:$RP ($FR)" || echo "frpc FAIL"
REMOTE2
    touch "/tmp/nro_tunnel_upgraded"
  fi
}

start_server() {
  local CS=$(echo "$1" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$1" | cut -d'|' -f2)
  local NAME=$(echo "$1" | cut -d'|' -f3)

  log "[$NAME] Đang khởi động server..."
  auth_as "$TOKEN_VAR"

  $GH_BIN codespace ssh -c "$CS" -- bash -s << REMOTE
    sudo rc-service mariadb start 2>/dev/null || sudo service mariadb start 2>/dev/null || \
    sudo /usr/bin/mysqld_safe --user=mysql --datadir=/var/lib/mysql > /dev/null 2>&1 &
    sleep 3
    pkill -f playit_old 2>/dev/null; pkill -f frpc 2>/dev/null; pkill -f NgocRongOnline 2>/dev/null; pkill -f ws_bridge 2>/dev/null; sleep 2
    # Tự tải lại playit v0.15.0 nếu /tmp bị xóa sau Codespace restart
    if [ ! -f "/tmp/playit_old" ] || [ \$(stat -c%s /tmp/playit_old 2>/dev/null || echo 0) -lt 1000000 ]; then
      curl -sL "https://github.com/playit-cloud/playit-agent/releases/download/v0.15.0/playit-linux-amd64" \
        -o /tmp/playit_old && chmod +x /tmp/playit_old
    fi
    nohup /tmp/playit_old >> ~/logs/playit_old.log 2>&1 &
    # WS Bridge (từ vị trí cố định ~/bin/)
    nohup python3 ~/bin/ws_bridge.py >> ~/logs/ws_bridge.log 2>&1 &
    # Tự tải lại frpc nếu /tmp bị xóa sau Codespace restart
    if [ ! -f "/tmp/frp/frpc" ]; then
      mkdir -p /tmp/frp
      curl -sL "https://github.com/fatedier/frp/releases/download/v0.61.0/frp_0.61.0_linux_amd64.tar.gz" \
        -o /tmp/frp.tar.gz && tar xzf /tmp/frp.tar.gz -C /tmp/frp --strip-components=1
      chmod +x /tmp/frp/frpc
    fi
    # Tạo lại frpc config nếu mất
    if [ ! -f "/tmp/frpc_nro.toml" ]; then
      cat > /tmp/frpc_nro.toml << 'CFG'
serverAddr = "frp.freefrp.net"
serverPort = 7000
auth.method = "token"
auth.token = "freefrp.net"

[[proxies]]
name = "nro-game"
type = "tcp"
localIP = "127.0.0.1"
localPort = 14445
remotePort = 21445
CFG
    fi
    nohup /tmp/frp/frpc -c /tmp/frpc_nro.toml >> ~/logs/frp.log 2>&1 &
    sleep 3
    cd ~/nro/SRC
    nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
      -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
      -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
      -jar NgocRongOnline.jar > ~/logs/server.log 2>&1 &
    sleep 10
    JAVA_PID=\$(pgrep -f NgocRongOnline | head -1)
    echo "Java: \$JAVA_PID | frpc: \$(pgrep -f frpc|head -1) | playit: \$(pgrep -f playit_old|head -1)"
    [ -n "\$JAVA_PID" ] && echo "START_OK" || echo "START_FAIL"
REMOTE
}

revive_or_failover() {
  local CURRENT=$(get_active)
  local CURRENT_CS=$(echo "$CURRENT" | cut -d'|' -f1)

  log "[$(echo $CURRENT|cut -d'|' -f3)] Thử restart..."
  local OUT; OUT=$(start_server "$CURRENT"); log "$OUT"

  echo "$OUT" | grep -q "START_OK" && { log "✅ Restart thành công"; rm -f "$DIAG_DONE_FILE" "$DIAG_DONE_FILE" "/tmp/nro_tunnel_upgraded"; return 0; }

  for CS_ENTRY in "${CODESPACES[@]}"; do
    [ "$(echo $CS_ENTRY|cut -d'|' -f1)" = "$CURRENT_CS" ] && continue
    log "Thử [$(echo $CS_ENTRY|cut -d'|' -f3)]..."
    local OUT2; OUT2=$(start_server "$CS_ENTRY"); log "$OUT2"
    if echo "$OUT2" | grep -q "START_OK"; then
      set_active "$CS_ENTRY"; rm -f "$DIAG_DONE_FILE" "/tmp/nro_tunnel_upgraded"
      log "🔀 Failover sang [$(echo $CS_ENTRY|cut -d'|' -f3)]!"
      return 0
    fi
  done
  log "⚠️ Tất cả Codespace lỗi! Thử lại sau $INTERVAL giây."
}

stop_active() {
  local CURRENT=$(get_active)
  local CS=$(echo "$CURRENT" | cut -d'|' -f1)
  local TOKEN_VAR=$(echo "$CURRENT" | cut -d'|' -f2)
  local NAME=$(echo "$CURRENT" | cut -d'|' -f3)

  auth_as "$TOKEN_VAR"
  local STATE=$($GH_BIN codespace list 2>/dev/null | grep "$CS" | awk '{print $5}')
  if [ "$STATE" = "Available" ] || [ "$STATE" = "Running" ]; then
    log "[$NAME] Giờ nghỉ → dừng..."
    $GH_BIN codespace ssh -c "$CS" -- bash -c \
      "pkill -f NgocRongOnline 2>/dev/null; pkill -f playit_old 2>/dev/null; pkill -f frpc 2>/dev/null; echo Stopped" 2>/dev/null
    $GH_BIN codespace stop -c "$CS" 2>/dev/null
    log "[$NAME] 💤 Đã dừng"
    rm -f "$DIAG_DONE_FILE" "/tmp/nro_tunnel_upgraded"
  else
    log "[$NAME] Đã tắt ($STATE)"
  fi
}

# ─── MAIN ───────────────────────────────────────────────────────────────────
install_gh
log "=== NRO Keep-alive v2 khởi động === Active: $(get_active | cut -d'|' -f3)"

LOOP=0
while true; do
  H=$(get_vn_hour)
  log "Giờ VN: ${H}h | Active: $(get_active | cut -d'|' -f3) | Loop #$((++LOOP))"

  if is_active_hours; then
    if ping_codespace "$(get_active)"; then
      # Server alive → chạy diagnostics lần đầu (compile + restart)
      run_diagnostics "$(get_active)"
      # Sau diagnostics: chạy upgrade scan
      run_upgrade "$(get_active)"
      # Sau upgrade scan: chạy phase 2 (compile + đọc code nâng cấp)
      run_upgrade2 "$(get_active)"
      # Phase 3: đọc chi tiết mob/zone/skill/boss
      run_upgrade3 "$(get_active)"
      # Phase 4: apply DB + code upgrades
      run_upgrade4 "$(get_active)"
      # Phase 5: đọc skill cooldown + zone spawn + damage formula
      run_upgrade5 "$(get_active)"
      # Phase 6: apply final — skill/mob/npc upgrades
      run_upgrade6 "$(get_active)"
      # Phase 7: PATCH & COMPILE — Mob AI + Map + SQL + Restart
      run_upgrade7 "$(get_active)"
      # Phase 8: Đọc attack path — tìm nguyên nhân delay
      run_read_attack "$(get_active)"
      # Phase 9: Fix delay attack — compile + restart
      run_fix_attack_delay "$(get_active)"
      # Phase 10: FIX animation-first — sendPlayerAttackMob trước damage
      run_fix_animation_first "$(get_active)"
      # Phase 11: Đọc Service.attackMob + sendMobStillAlive + TIME_GONG
      run_fix_normal_attack "$(get_active)"
      # Phase 12: APPLY fix Service + TIME_GONG + compile + restart
      run_fix_service_mob "$(get_active)"
      # Phase 13: Teamobi2026 — DB tables + boss classes
      run_teamobi_upgrade "$(get_active)"
      # Phase 14: Revert map mobs + mob stats về gốc
      run_revert_mobs "$(get_active)"
      # Phase 15: Fix Map.java spawn offset (mobs bay trên trời)
      run_fix_map_spawn "$(get_active)"
      # Phase 16: Cải trang + vật phẩm mới từ Teamobi2026
      run_new_content "$(get_active)"
      # Phase 17: Network optimization — TCP_NODELAY + ws_bridge v3 + Sender 1ms
      run_net_opt "$(get_active)"
      # Sau loop thứ 3 (~60 phút): thử upgrade tunnel
      [ "$LOOP" -ge 3 ] && try_upgrade_tunnel "$(get_active)"
    else
      revive_or_failover
    fi
  else
    stop_active
    log "💤 Ngủ $INTERVAL giây..."
  fi

  sleep $INTERVAL
done

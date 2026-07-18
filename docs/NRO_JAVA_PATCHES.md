# 🛠️ NRO JAVA PATCHES — Hướng dẫn nâng cấp code

## Tổng quan
Source SRC-Team (`/tmp/nro_extracted/SRC-Team/Soucre/`) là Maven project hoàn chỉnh với 417 file Java.
Các patch này ghi lại những class quan trọng và cách áp dụng lên server.

---

## 1. Compile SRC-Team từ đầu

```bash
cd /tmp/nro_extracted/SRC-Team/Soucre/

# Build JAR
mvn clean package -DskipTests

# Tìm JAR output
ls target/*.jar

# Deploy
cp target/nro-*.jar /opt/nro/nro.jar
```

---

## 2. Class quan trọng và chức năng

### 2.1 Skill System

#### `nro/consts/Skill.java` — Skill IDs
```java
// Constants chính
DRAGON = 0           // Chiêu đấm Dragon (Trái đất)
KAMEJOKO = 1         // Kamejoko (Trái đất)
DEMON = 2            // Chiêu đấm Demon (Namếc)
MASENKO = 3          // Masenko (Namếc)
GALICK = 4           // Galick (Xayda)
ANTOMIC = 5          // Antomic (Xayda)
THAI_DUONG_HA_SAN = 6
TAI_TAO_NL = 8       // Tái tạo NL (Xayda)
KAIOKEN = 9
QUA_CAU_KENH_KHI = 10
MAKANKOSAPPO = 11
DE_TRUNG = 12        // Đẻ trứng (Namếc)
BIEN_KHI = 13        // Biến Khỉ (Xayda)
TU_SAT = 14          // Tự phát nổ (Xayda)
LIEN_HOAN = 17
SOCOLA = 18          // Biến sôcôla (Namếc)
KHIEN_NL = 19        // Khiên năng lượng (tất cả)
DCTT = 20            // Dịch chuyển tức thời (Trái đất)
HUYT_SAO = 21        // Huýt sáo (Xayda)
THOI_MIEN = 22       // Thôi miên (Trái đất)
TROI = 23            // Trói (Xayda)
SUPER_KAME = 24      // Super Kamejoko (TD)
CADICH_LHC = 25      // Cađíc liên hoàn chưởng (Xayda)
MA_PHONG_BA = 26     // Ma Phong Ba (Namếc)
BIEN_HINH = 27       // Biến Hình (tất cả 3 tộc)
PHAN_THAN = 28       // Phân thân (tất cả 3 tộc)
```

#### `nro/services/SkillService.java` — Logic skill
```java
// Entry point
useSkill(Player player, Player plTarget, Mob mobTarget, Message message)

// Phân loại theo skill type
// type 1 = attack: useSkillAttack(player, plTarget, mobTarget)
// type 2 = buff/heal: useSkillBuffToPlayer(player, plTarget)
// type 3 = alone (buff self): useSkillAlone(player)
// type 4 = special: userSkillSpecial(player, st, skillId, dx, dy, dir, x, y)

// Biến Hình được gọi từ useSkillAlone (type=3)
// Phân Thân được gọi từ userSkillSpecial (type=4)
// Super Kame và CadicLHC cũng type=4

// Thời gian hiệu ứng:
// Sôcôla: getTimeSocola() = 30000 ms
// Thôi miên: getTimeThoiMien(level) = (level + 4) * 1000 ms
// Trói: getTimeTroi(level) = level * 5000 ms
// DCTT choáng: getTimeDCTT(level) = (level + 1) * 500 ms
// Khiên: getTimeShield(level) = (level + 2) * 5000 ms
// Ma Phong Ba: getTimeBinh(level) = 10000 ms
// Thái Dương choáng: getTimeStun(level) = (level + 2) * 1000 ms
```

#### `nro/utils/SkillUtil.java` — Utility
```java
isUseSkillBoom(Player)  // Check xem có đang dùng Tự Phát Nổ không
getTimeMonkey(level)    // Thời gian Biến Khỉ: (level + 5) * 10000 ms
getPercentHpMonkey(level) // Bonus HP Biến Khỉ: (level + 3) * 10 %
getPercentHpBienHinh(level) // Bonus HP Biến Hình: (level + 1) * 10 %
getTimeStun(level), getTimeSocola(), getTimeBinh(level)
getTimeShield(level), getTimeTroi(level)
getTimeDCTT(level), getTimeThoiMien(level)
```

### 2.2 Stat System

#### `nro/services/NPoint.java` — Tính stat player
```java
// Công thức stat cơ bản
hpMax = baseHP + equipBonus + setBonus + buffBonus + petBonus + limitBonus
mpMax = ...
dame  = baseDame * kaiokenMulti * bienhinhMulti * petMulti

// Set bonuses:
// Piccolo set = +HP, cộng thêm khi đủ bộ
// Nappa set = +dame
// Buff: Kaioken, Monkey (biến khỉ), Bien Hinh (biến hình)
// Pet bonus: Pet hợp thể
// Item thời gian: ItemTime trong itemsBody slot

// Power Limit: MAX_LIMIT = 11
// Nếu player đạt tier > 11, không cộng thêm từ power_limit table

// BlackBall war: x2 multiplier khi đang war
```

### 2.3 Player System

#### `nro/models/player/Player.java` — Model player (300+ fields)
```java
// Các system quan trọng
playerSkill    // Skill đang chọn, skill list
effectSkill    // Hiệu ứng skill đang active
inventory      // Đồ trang bị + túi đồ
nPoint         // Stat HP/KI/Dame/Def/Crit
clone          // Phân thân (PlayerClone)
pet            // Pet hợp thể
miniPet        // Mini pet theo sau
fusion         // Trạng thái hợp thể

// DanhHieu (Danh hiệu)
dh1, dh2, dh3, dh4, dh5  // ChiSoHP, ChiSoKI, ChiSoSD
chiSoHP, chiSoKI, chiSoSD

// War fields
isBlackBallWar, isNamekBallWar, isMabuWar

// Mini-game
taiXiuBet, chonAiDayCurrent, conSoMayMan

// PVP
pkMode        // NON_PK=0, PK_PVP=3, PK_ALL=5
pvpTarget

// Clan
clanId, clanRank
```

### 2.4 Mob System

#### `nro/models/mob/Mob.java`
```java
// Khi mob bị đánh
synchronized injured(Player attacker, int damage)
// → gọi MobService.dropItemTask(mob, attacker)
// → gọi TaskService.checkDone(attacker)
// → tính EXP: getTiemNangForPlayer(player)

// EXP theo level diff (nếu player quá mạnh hơn mob → ít EXP)
getTiemNangForPlayer(player)
```

#### `nro/models/mob/MobMe.java`
- Mob thuộc sở hữu player (pet mob từ skill Đẻ Trứng)
- Theo player, tấn công target của player

### 2.5 Boss System

#### `nro/models/boss/Boss.java` — Base class
```java
// Fields quan trọng
int id              // ID unique
Zone zone           // Vị trí hiện tại
Location location   // Tọa độ x, y
boolean isDie()     // Check còn sống không

// Lifecycle
update()            // Gọi mỗi tick từ BossManager.updateAllBoss()
dispose()           // Cleanup khi chết
```

#### `nro/models/boss/BossFactory.java` — Tạo boss
```java
// Static factory tạo các loại boss
createBill()
createBroly()
createCells()  // Tạo XenBoHung + XenCon
// ...
```

### 2.6 Map System

#### `nro/services/MapService.java`
```java
// Capsule maps (16 map đặc biệt)
getMapCapsule()  // Trả về list map capsule

// Planet naming
getPlanetName(mapId)

// War maps
isBlackBallWarMap(mapId)
isNamekBallWarMap(mapId)

// Map types (ConstMap)
MAP_NORMAL=0, MAP_OFFLINE=1, MAP_DOANH_TRAI=2
MAP_BLACK_BALL_WAR=3, MAP_BAN_DO_KHO_BAU=4, MAP_KHI_GAS_HUY_DIET=5

// Change map
changeMap(player, zone, x, y)  // Dùng ChangeMapService.gI()
```

### 2.7 NPC System

#### `nro/services/NpcService.java`
```java
// Tạo menu dialog
createMenuConMeo(player, indexMenu, avatar, npcSay, menuSelect...)
createMenuRongThieng(player, indexMenu, npcSay, menuSelect...)
createTutorial(player, avatar, npcSay)

// Xử lý kết quả menu (CMD 38 response)
// indexMenu = ConstNpc.xxx constants để phân biệt menu nào
```

#### `nro/consts/ConstNpc.java` — Menu indices
```java
MAKE_MATCH_PVP = ?   // PvP challenge
REVENGE = ?          // Revenge PvP
RONG_THIENG = 24     // Rồng thiêng NPC id
CON_MEO = 5          // Con mèo NPC id
```

### 2.8 Item System

#### `nro/services/ItemService.java`
```java
createNewItem(itemTemplateId) // Tạo item mới
copyItem(item)                // Copy item
DoThienSu(item)               // Random stats cho đồ thiên sứ
createItemFromItemMap(...)     // Tạo item từ map drop
```

#### `nro/services/ShopService.java`
```java
// Mua đồ
buyItem(player, shopId, tabId, slotId)  // Tốn Gold/Gem/Ruby
// Kiểm tra inventory space trước khi mua
// Học skill từ sách

// ConsignmentShop (ký gửi)
postItem(player, item, price)
buyConsignmentItem(player, itemId)
```

---

## 3. Patch biến hình - Chi tiết implement

Biến hình (skill id 27) khi dùng sẽ:
1. Check đủ power không (`power_require` trong skill JSON)
2. Tiêu mana (`mana_use`)
3. Bắt đầu cooldown (`cool_down = 120000ms = 2 phút`)
4. Set player sprite → HEADBIENHINH[classId][level], BODYBIENHINH[classId], LEGBIENHINH[classId]
5. Set aura → AURABIENHINH[classId][level]
6. Tính thêm `%HP`: getPercentHpBienHinh(level) = (level+1)*10%
7. Gọi `Send_Caitrang(player)` để update cho map

Khi hết thời gian biến hình (nếu có) hoặc player chết:
- Reset về sprite gốc
- Remove buff biến hình

---

## 4. Thứ tự deploy an toàn

```
1. Dừng NRO server
2. Backup DB:  bash scripts/nro_push_upgrade.sh backup
3. Import SQL: bash scripts/nro_push_upgrade.sh full
4. Build JAR:  cd /tmp/nro_extracted/SRC-Team/Soucre && mvn clean package
5. Deploy JAR: bash scripts/nro_push_upgrade.sh deploy-jar
6. Start server
7. Test:
   - Login thử
   - Kiểm tra skill list trong game
   - Kiểm tra shop
   - Thử dùng Biến Hình nếu đủ power
```

---

## 5. Debug thường gặp

### Skill không xuất hiện
- Kiểm tra `skill_template.slot` — slot phải đúng thứ tự
- Kiểm tra `nclass_id` phải khớp với class player
- Kiểm tra `power_require` ở point=1 (level đầu) có hợp lý không

### Biến hình crash
- Check `ConstPlayer.HEADBIENHINH[classId][level-1]` không out of bounds
- Level 1-5, array index 0-4

### Boss không spawn
- Kiểm tra `BossFactory` có gọi đúng boss không
- Check map zone đủ player slot không
- Check `BossManager.BOSSES_IN_GAME` list

### Map không load
- JSON trong `map_template.data` phải valid
- `waypoints`, `mobs`, `npcs` arrays phải đúng format

---

*Generated from SRC-Team source analysis — July 2026*

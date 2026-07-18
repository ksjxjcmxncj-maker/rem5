# 🚀 KẾ HOẠCH NÂNG CẤP NRO — Teamobi2026 → SRC-Team

> **Ngày lập:** July 18, 2026  
> **Nguồn:** Teamobi2026.rar (548 Java files, 629MB)  
> **Mục tiêu:** Port tính năng phong phú từ Teamobi2026 vào NRO SRC-Team đang chạy

---

## 📋 TỔNG QUAN CÁC BƯỚC

| # | Bước | Loại | Ưu tiên | Thời gian ước | Rủi ro |
|---|---|---|---|---|---|
| 1 | Import bảng DB mới | SQL | 🔴 Cao | 15 phút | Thấp |
| 2 | ALTER TABLE player | SQL | 🔴 Cao | 10 phút | Trung bình |
| 3 | Thêm Boss classes event | Java | 🔴 Cao | 1-2h | Thấp |
| 4 | Thêm Boss classes mới | Java | 🟠 Vừa | 1h | Thấp |
| 5 | Cập nhật BossID.java | Java | 🔴 Cao | 30 phút | Thấp |
| 6 | Cập nhật BossesData.java | Java | 🔴 Cao | 2h | Thấp |
| 7 | Cập nhật BossManager | Java | 🟠 Vừa | 1h | Trung bình |
| 8 | Thêm ConstMap.java | Java | 🟡 Thấp | 30 phút | Thấp |
| 9 | Biên dịch & deploy | Build | 🔴 Cao | 30 phút | Cao |

---

## BƯỚC 1 — Import Bảng Database Mới

**Script:** `docs/teamobi2026_new_tables.sql`  
**Chạy:** `mysql -u root nro1 < docs/teamobi2026_new_tables.sql`

### Bảng sẽ được tạo mới:

| Bảng | Dòng dữ liệu | Mô tả |
|---|---|---|
| `achievement_template` | 20 | Thành tích người chơi |
| `array_head_2_frames` | 52 | Animation đầu nhân vật 2 frame |
| `bg_item_template` | ~10 | Item trang trí background |
| `clan_task_template` | ~20 | Nhiệm vụ bang hội |
| `data_badges` | ~10 | Huy hiệu + effect |
| `task_badges_template` | ~10 | Nhiệm vụ để mở huy hiệu |
| `radar` | ~25 | Thẻ bài (card collection) |

> ⚠️ **LƯU Ý**: Script dùng `DROP TABLE IF EXISTS` — bảng cũ sẽ bị xóa nếu trùng tên.  
> Bảng `super_rank` không import (có data player cụ thể của Teamobi server).

---

## BƯỚC 2 — ALTER TABLE player

**Script:** `docs/teamobi2026_alter_player.sql`  
**Chạy:** `mysql -u root nro1 < docs/teamobi2026_alter_player.sql`

### Cột mới được thêm (40 cột):

```
BoughtSkill, LearnSkill           — Skill đã mua/học
bandokhobau, conduongrandoc       — Dungeon tracking
baovetaikhoan, captcha            — Bảo vệ tài khoản
clan_id                           — ID bang (Teamobi dùng 1 server)
dailyGift                         — Quà hàng ngày
dataBadges, dataTaskBadges        — Huy hiệu system
data_achievement                  — Thành tích
data_card                         — Thẻ bài (10000 chars)
data_clan_task                    — Nhiệm vụ bang
data_duahau_egg                   — Sự kiện dưa hấu
data_event                        — Event data
data_item_event                   — Item sự kiện
data_luyentap                     — Luyện tập offline
data_vip                          — VIP tier + thời hạn
doanhtrai                         — Doanh trại progress
giftcode                          — Gift code đã dùng
items_daban                       — Đặt bán item
lasttimepkcommeson, rongxuong     — PK tracking
masterDoesNotAttack               — Thầy không tấn công
nhanthoivang, ruonggo, sieuthanthuy — Mini features
nhiem_vu_kol                      — Nhiệm vụ KOL
notify                            — Thông báo inbox
pet                               — Pet system (Teamobi style)
point_sukien, point_sukien1/2     — Điểm sự kiện đa mùa
point_maydam, total_damage_maydam — Máy đấm stats
rank                              — Super Rank
thachdauwhis                      — Thách đấu Whis
thanhTichCDRD, thanhTichKhiGas   — Thành tích dungeon
vodaisinhtu                       — Võ đài sinh tử
```

> ⚠️ **Dùng `ADD COLUMN IF NOT EXISTS`** — an toàn, không lỗi nếu đã có cột.  
> Server chạy MariaDB 10.4+ hoặc MySQL 8.0+ thì hỗ trợ cú pháp này.

---

## BƯỚC 3 — Thêm Boss Classes Sự Kiện

Teamobi2026 có **5 nhóm boss sự kiện** hoàn chỉnh:

### 3a. Halloween Event Bosses

Copy từ `SRC/src/nro/models/boss/event/Halloween/`:
```
BiMa.java    (4.2KB) — Bí Ma boss
Doi.java               — Boss Dơi
MaTroi.java            — Ma Trời
```
**Package đích:** `nro.models.boss.event.Halloween`  
**File reference:** `docs/teamobi2026_src/boss_events/BiMa.java`

### 3b. Giáng Sinh (Noel) Bosses

Copy từ `SRC/src/nro/models/boss/event_noel/`:
```
OngGiaNoel.java (6.6KB) — Ông Già Noel boss
```
**Package đích:** `nro.models.boss.event_noel`  
**File reference:** `docs/teamobi2026_src/boss_events/OngGiaNoel.java`

OngGiaNoel đặc biệt:
- Mỗi 60 giây thả 3 hộp quà (item 648) xuống map
- Chat "Hô hô hô" khi thả quà
- Không bị giết (injured trả về 0, reward rỗng)
- Tự rời map sau 1-5 phút

### 3c. Tết Nguyên Đán Bosses

Copy từ `SRC/src/nro/models/boss/event_tet/`:
```
LanCon.java (8.1KB) — Lân Con boss Tết
```
**Package đích:** `nro.models.boss.event_tet`

### 3d. Trung Thu Bosses

Copy từ `SRC/src/nro/models/boss/event_trung_thu/`:
```
KhiDot.java      (2.1KB)
NguyetThan.java
NhatThan.java
```
**Package đích:** `nro.models.boss.event_trung_thu`

### 3e. Hùng Vương Bosses

Copy từ `SRC/src/nro/models/boss/event_hung_vuong/`:
```
SonTinh.java  (7.1KB)
ThuyTinh.java
```
**Package đích:** `nro.models.boss.event_hung_vuong`

---

## BƯỚC 4 — Thêm Boss Classes Mới

### 4a. Cumber (Dragon Ball Super)

Copy `SRC/src/nro/models/boss/cumber/Cumber.java`  
**File reference:** `docs/teamobi2026_src/boss_new/Cumber.java`

Drop table của Cumber:
- `190` (ngọc) × 20,000-30,000
- 5% drop đồ trang bị (Áo/Quần/Giày hoặc Găng/Rada) với stars 1-6
- 10% drop ngọc rồng (IDs: 15-20, 992)
- 5% drop item level với random stats ×1.0-1.15

### 4b. Baby (Saga Baby)

Copy `SRC/src/nro/models/boss/Baby/Baby.java`  
**File reference:** `docs/teamobi2026_src/boss_new/Baby.java`

Drop của Baby:
- Giảm sát thương nhận vào ×0.35 (damage × 0.7 / 2)
- 1% drop trang phục đặc biệt (IDs: 1785, 1786, 1788) với stat cực cao

### 4c. Bojack Team Chain

Copy từ `SRC/src/nro/models/boss/trai_dat/`:
```
BUJIN.java → KOGU.java → ZANGYA.java → BIDO.java → BOJACK.java → SUPER_BOJACK.java
```

### 4d. GoldenFrieza + DeathBeam Chain

Copy từ `SRC/src/nro/models/boss/Golden_fireza/`:
```
GoldenFrieza.java
DeathBeam1.java → DeathBeam2.java → DeathBeam3.java → DeathBeam4.java → DeathBeam5.java
```

### 4e. Yardrat Dungeon Bosses (18 classes!)

Copy từ `SRC/src/nro/models/boss/yardrat/`:
```
TAPSU0.java  → TAPSU4.java  (5 classes — Tập Sự)
TANBINH0.java → TANBINH5.java (6 classes — Tân Binh)
CHIENBINH0.java → CHIENBINH5.java (6 classes — Chiến Binh)
DOITRUONG5.java (1 class — Đội Trưởng boss)
Yardart.java (base class)
```

### 4f. Android Series

Copy từ `SRC/src/nro/models/boss/Android/`:
```
Android13.java, Android14.java, Android15.java
Android19.java, DrKore.java, KingKong.java, Pic.java, Poc.java
```

### 4g. Mini-Boss Series

Copy từ `SRC/src/nro/models/boss/Boss_mini/`:
```
AnTrom.java      — Trộm xuất hiện ngẫu nhiên
MatTroi.java     — Ma Trời (mini)
Odo.java         — Odo mini-boss
RongNhi.java     — Rồng Nhí
SoiHecQuyn.java  — Sói Héc Quyn
Virut.java       — Virút
```

### 4h. Boss Auto-Training (luyện tập tự động)

Copy từ `SRC/src/nro/models/boss/luyen_tap_tu_dong/`:
```
Karin.java, KhiBubbles.java, MrPoPo.java, TauPayPay.java
ThanVuTru.java, ThuongDe.java, ToSuKaio.java
TrainingBoss.java, Whis.java, Yajiro.java
```

---

## BƯỚC 5 — Cập Nhật BossID.java

Thêm vào file `BossID.java` của SRC-Team các constant còn thiếu:

```java
// === Teamobi2026 additions ===
// Cumber
public static final int CUMBER = -203999;
public static final int SUPER_CUMBER = -204000;

// Baby
public static final int BABY = -925;
public static final int BABY_2 = -926;
public static final int BABY_3 = -927;

// Bojack chain
public static final int BUJIN = -316;
public static final int KOGU = -317;
public static final int ZANGYA = -318;
public static final int BIDO = -319;
public static final int BOJACK = -320;
public static final int SUPER_BOJACK = -321;

// GoldenFrieza chain
public static final int GOLDEN_FRIEZA = -502;
public static final int DEATH_BEAM_1 = -609;
public static final int DEATH_BEAM_2 = -610;
public static final int DEATH_BEAM_3 = -611;
public static final int DEATH_BEAM_4 = -612;
public static final int DEATH_BEAM_5 = -613;

// Event bosses
public static final int ONG_GIA_NOEL = -353;   // Noel
public static final int LAN_CON = -371;          // Tết
public static final int BI_MA = -351;            // Halloween
public static final int DOI = -350;              // Halloween
public static final int MA_TROI = -349;          // Halloween
public static final int KHIDOT = -344;           // Trung Thu
public static final int NGUYET_THAN = -345;      // Trung Thu
public static final int NHAT_THAN = -346;        // Trung Thu
public static final int SON_TINH = -354;         // Hùng Vương
public static final int THUY_TINH = -355;        // Hùng Vương

// Android series
public static final int ANDROID_13 = -32;
public static final int ANDROID_14 = -33;
public static final int ANDROID_15 = -34;
public static final int PIC = -35;
public static final int POC = -36;
public static final int KING_KONG = -37;
```

**File reference:** `docs/teamobi2026_src/BossID.java` (đầy đủ)

---

## BƯỚC 6 — Cập Nhật BossesData.java

`BossesData.java` của Teamobi2026 là **123KB** (vs nhỏ hơn nhiều ở SRC-Team).

**Chiến lược:** Merge từng block boss data vào SRC-Team `BossesData.java`.

Các data blocks ưu tiên cao nhất cần merge:

```
BossesData.CUMBER / SUPER_CUMBER
BossesData.BABY / BABY_2 / BABY_3
BossesData.ONG_GIA_NOEL
BossesData.LAN_CON
BossesData.BI_MA / DOI / MA_TROI
BossesData.KHIDOT / NGUYET_THAN / NHAT_THAN
BossesData.SON_TINH / THUY_TINH
BossesData.GOLDEN_FRIEZA / DEATH_BEAM_1-5
BossesData.BUJIN / KOGU / ZANGYA / BIDO / BOJACK / SUPER_BOJACK
BossesData.TAPSU0-4, TANBINH0-5, CHIENBINH0-5, DOITRUONG5
```

**File reference:** `docs/teamobi2026_src/BossesData.java` (full 123KB)

---

## BƯỚC 7 — Cập Nhật BossManager

`BossManager.java` quyết định boss nào spawn lúc nào và ở đâu.

Các manager classes mới cần đăng ký vào `BossManager` hoặc khởi động riêng:
```
AnTromManager.java    — Mini-boss trộm ngẫu nhiên
ChristmasEventManager.java — Quản lý Noel
HalloweenEventManager.java — Quản lý Halloween
HungVuongEventManager.java — Quản lý Hùng Vương
LunarNewYearEventManager.java — Quản lý Tết
TrungThuEventManager.java — Quản lý Trung Thu
YardartManager.java   — Quản lý Yardrat dungeon
OtherBossManager.java — Quản lý boss khác
SnakeWayManager.java  — Con đường rắn độc
TreasureUnderSeaManager.java — Kho báu biển
RedRibbonHQManager.java — Doanh trại Ribbon
GasDestroyManager.java — Khí gas hủy diệt
SkillSummonedManager.java — Skill triệu hồi boss
```

**File reference:** `docs/teamobi2026_src/BossManager.java`

---

## BƯỚC 8 — Thêm ConstMap.java

Teamobi2026 có `ConstMap.java` rất đầy đủ (160+ constants).

SRC-Team hiện sử dụng số literal trực tiếp. Thêm file này sẽ cải thiện maintainability.

**Copy file:** `docs/teamobi2026_src/const/ConstMap.java`  
**Package đích:** `nro.models.consts.ConstMap`

---

## BƯỚC 9 — Biên Dịch & Deploy

Server NRO đang chạy trên **Codespace** qua tunnel **frp.freefrp.net:21445**.

### Cách biên dịch

```bash
# Trên Codespace
cd /workspace/NRO

# Biên dịch tất cả Java files mới
javac -cp 20.jar -d build/ \
  src/nro/models/boss/cumber/Cumber.java \
  src/nro/models/boss/Baby/Baby.java \
  src/nro/models/boss/event_noel/OngGiaNoel.java \
  src/nro/models/boss/event_tet/LanCon.java \
  src/nro/models/boss/event/Halloween/BiMa.java \
  src/nro/models/boss/event/Halloween/Doi.java \
  src/nro/models/boss/event/Halloween/MaTroi.java \
  src/nro/models/boss/event_trung_thu/KhiDot.java \
  src/nro/models/boss/event_trung_thu/NguyetThan.java \
  src/nro/models/boss/event_trung_thu/NhatThan.java \
  src/nro/models/boss/event_hung_vuong/SonTinh.java \
  src/nro/models/boss/event_hung_vuong/ThuyTinh.java \
  ...

# Sau đó pack vào JAR
jar -uf 20.jar -C build/ .

# Restart server
pkill -f "java.*20.jar" && sleep 2 && java -Xmx512m -jar 20.jar &
```

### Cách push file lên Codespace (từ Replit)

```bash
# Dùng script push có sẵn
bash scripts/nro_push_upgrade.sh
```

---

## ⚠️ ĐIỀU CẦN CHÚ Ý KHI PORT CODE

### 1. Import khác nhau

Teamobi2026 dùng `BadgesTaskService`, `ConstTaskBadges` — SRC-Team không có các class này.  
→ Khi copy Cumber.java, tạm bỏ phần `BadgesTaskService.updateCountBagesTask(...)` hoặc tạo stub.

### 2. BossType enum

Teamobi2026 dùng `CHRISTMAS_EVENT`, `HALLOWEEN_EVENT`, v.v. trong BossType enum.  
→ Cần thêm các enum values này vào `BossType.java` của SRC-Team.

### 3. Player object reference

Một số class dùng `player.bossBabyDefeatParticipationCount` — field không có ở SRC-Team.  
→ Thêm field vào Player hoặc thay bằng field tương đương.

### 4. event.addEventPoint()

Teamobi2026 dùng `plKill.event.addEventPoint(diem)` — SRC-Team dùng `plKill.event_point++`.  
→ Adapt theo format của SRC-Team.

### 5. SQL Backup bắt buộc

```bash
# Backup toàn bộ trước khi làm
mysqldump -u root nro1 > /backup/nro1_before_teamobi_$(date +%Y%m%d_%H%M%S).sql
```

---

## 📁 File Output Của Quá Trình Phân Tích

```
docs/
├── TEAMOBI2026_ANALYSIS.md          ← Phân tích chi tiết tính năng
├── NRO_UPGRADE_PLAN_TEAMOBI2026.md  ← File này
├── teamobi2026_new_tables.sql       ← Import 7 bảng mới (885 dòng)
├── teamobi2026_alter_player.sql     ← ALTER TABLE player (40 cột)
└── teamobi2026_src/
    ├── BossID.java                  ← Full BossID constants (7.8KB)
    ├── BossesData.java              ← Full boss data (123KB)
    ├── BossManager.java             ← Boss manager logic (18KB)
    ├── const/
    │   └── ConstMap.java            ← Map ID constants (7.6KB)
    ├── boss_events/
    │   ├── BiMa.java               ← Halloween boss
    │   ├── KhiDot.java             ← Trung Thu boss
    │   ├── LanCon.java             ← Tết boss
    │   ├── OngGiaNoel.java         ← Noel boss
    │   ├── SonTinh.java            ← Hùng Vương boss
    │   └── ThuyTinh.java           ← Hùng Vương boss
    └── boss_new/
        ├── Baby.java               ← Baby boss
        └── Cumber.java             ← Cumber boss
```

---

*Tạo bởi phân tích Teamobi2026.rar — July 18, 2026*

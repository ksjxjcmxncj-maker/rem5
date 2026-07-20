# 📦 Phân Tích Teamobi2026.rar — Tính Năng Có Thể Áp Dụng

> **File gốc:** `Teamobi2026.rar` (629MB) — server NRO private của team Mobi 2026  
> **Codebase:** 548 Java files + SQL + data  
> **So sánh với:** NRO SRC-Team (417 Java files)  
> **Phân tích bởi:** Agent — July 18, 2026

---

## 📁 Cấu Trúc RAR

```
Teamobi2026.rar
├── SRC/
│   ├── 20.jar              ← Base NRO server JAR
│   ├── Config.properties   ← Cấu hình server (IP, DB)
│   ├── sql/nro1.sql        ← Database schema + data đầy đủ
│   ├── data/               ← Map tiles, mob sprites, effect data
│   └── src/                ← 548 Java source files
├── MOD/
│   └── XUNGLORDLOCAL.exe   ← Client NRO modded (test local)
├── admin.docx              ← Tài liệu admin
└── item.xlsx               ← Bảng item
```

---

## 🆕 Tính Năng Teamobi2026 CÓ — SRC-Team KHÔNG CÓ

### 1. 👹 Boss System — Cực Phong Phú

#### 1.1 Boss mới hoàn toàn

| Boss | ID | HP | Maps | Ghi chú |
|---|---|---|---|---|
| **Cumber** | -203999 | Rất cao | - | Nhân vật Dragon Ball Super |
| **Baby** | -925 | Cao | - | Saga Baby |
| **Broly thường** | -1822 | - | - | Boss solo riêng |
| **Super Broly** | -82282 | - | - | Dạng 2 của Broly |
| **Black Goku** | -203 | Cao | - | Tương lai |
| **GoldenFrieza** | -502 | Cực cao | - | Boss 21h |
| **Cooler** | -29 | Cao | - | Cold family |
| **Hit** | -204 | - | - | Dragon Ball Super |
| **Hatchiyack** | -207 | - | - | Movie boss |
| **Dr. Lychee** | -208 | - | - | Movie boss |
| **King Kong** | -37 | - | - | Android team |

#### 1.2 Boss chain (Bojack team)
```
BUJIN (-316) → KOGU (-317) → ZANGYA (-318) → BIDO (-319) 
→ BOJACK (-320) → SUPER_BOJACK (-321)
```

#### 1.3 Boss chain (GoldenFrieza 21h)
```
GOLDEN_FRIEZA (-502)
→ DEATH_BEAM_1 (-609) → DEATH_BEAM_2 (-610) 
→ DEATH_BEAM_3 (-611) → DEATH_BEAM_4 (-612) → DEATH_BEAM_5 (-613)
```

#### 1.4 Yardrat Dungeon — boss chain 3 tầng
```
TẬP SỰ (TAPSU0-4)     → 5 loại tập sự
TÂN BINH (TANBINH0-5) → 6 loại tân binh  
CHIẾN BINH (CHIENBINH0-5) → 6 loại chiến binh
ĐỘI TRƯỞNG (DOITRUONG5) → Boss đội trưởng
```

#### 1.5 Android series đầy đủ
```
ANDROID_19 (-30), DR_KORE (-31)
ANDROID_13 (-32), ANDROID_14 (-33), ANDROID_15 (-34)
PIC (-35), POC (-36), KING_KONG (-37)
```

#### 1.6 Big Boss xuất hiện trên map (mob_bigboss)
| Boss | Map | Đặc điểm |
|---|---|---|
| Hirudegarn | - | Di chuyển trên map |
| Piano | - | Xuất hiện ngẫu nhiên |
| GaChinCua | - | Map-specific |
| GauTuongCuop | -926 | Có Boss ID riêng |
| NguaChinLmao | - | - |
| RobotBaoVe | - | - |
| VoiChinNga | - | - |
| VuaBachTuoc | - | Boss biển |

#### 1.7 Trùm Mabư 12h (boss chain đầy đủ)
```
DRABURA (-233) → BUI_BUI (-234) → YA_CON (-235) 
→ MABU (-236/12h) → DRABURA_2 (-237) → BUI_BUI_2 (-238)
→ GOKU (-341) → CADIC (-342) → DRABURA_3 (-343)
```

#### 1.8 BossesData.java (123KB) — Dữ liệu chi tiết

Mỗi boss trong `BossesData.java` có đầy đủ:
- **Tên hiển thị** tiếng Việt
- **Outfit** (head, body, leg, bag, aura, eff)
- **Sát thương** (damage)
- **HP table** (mảng, tăng theo số player)
- **Danh sách map** được spawn
- **Skill table** `{skillId, level, cooldown}`
- **3 mảng hội thoại**: chat thường, chat chiến đấu, chat khi chết
- **AppearType**: riêng lẻ, theo nhóm, etc.

---

### 2. 🎉 Sự Kiện Mùa (Seasonal Events)

| Sự kiện | Manager class | Boss |
|---|---|---|
| **Halloween** | HalloweenEventManager | BiMa (-351), Doi (-350), MaTroi (-349) |
| **Giáng Sinh (Noel)** | ChristmasEventManager | OngGiaNoel (-353) |
| **Tết Nguyên Đán** | LunarNewYearEventManager | LanCon (-371) |
| **Trung Thu** | TrungThuEventManager | KhiDot (-344), NguyetThan (-345), NhatThan (-346) |
| **Hùng Vương** | HungVuongEventManager | SonTinh (-354), ThuyTinh (-355) |

Mỗi sự kiện có:
- Boss xuất hiện riêng
- Drop đặc biệt
- Boss với dialog tương ứng lễ hội

---

### 3. 🗺️ Hệ Thống Map (ConstMap.java)

Teamobi2026 có **ConstMap.java** với tên hằng số cho 160+ maps:

```java
MAP_NORMAL = 0, MAP_OFFLINE = 1, MAP_DOANH_TRAI = 2
MAP_BLACK_BALL_WAR = 3, MAP_BAN_DO_KHO_BAU = 4, MAP_MA_BU = 5
MAP_CON_DUONG_RAN_DOC = 6, MAP_KHI_GAS_HUY_DIET = 7
MAP_TAY_KARIN = 8, MAP_MABU_14H = 9
```

Maps đặc biệt chỉ Teamobi2026 có tên constant:
- `HANH_TINH_YARDART` (131, 132, 133)
- `DONG_HAI_TAC` (135), `HANG_BACH_TUOC` (136), `DONG_KHO_BAU` (137), `CANG_HAI_TAC` (138)
- `HANH_TINH_POTAUFEU` (139), `HANG_DONG_POTAUFEU` (140)
- `CON_DUONG_RAN_DOC` (141-143)
- `VO_DAI_SIEU_CAP` (145), `TAY_KARIN` (146)
- `HANH_TINH_BILL` (154), `HANH_TINH_NGUC_TU` (155)
- `KHU_HANG_DONG` (160-163)

---

### 4. 🏆 Hệ Thống Radar/Thẻ Bài (Collection Card)

Database Teamobi2026 có **radar table** với:
- **25+ loại thẻ** với lore đầy đủ tiếng Việt
- **6 rank** (0-6): từ thẻ thường → Oozaru/Rồng thần Namek
- **3 stat/thẻ** (tăng theo mức độ hoàn thành)
- Thẻ mob (rank 0-3), Thẻ boss (rank 4-5), Thẻ siêu hiếm (rank 6)

Ví dụ thẻ:
```
Thẻ Khủng long (rank 0) → +Vàng
Thẻ Lợn lòi (rank 0) → +Vàng
Thẻ Ninja Tím (rank 4) → +Def, +Crit, +HP
Thẻ Rồng Thần Namek (rank 5) → +Exp, +Vàng, +HP
Thẻ Oozaru (rank 6) → +Def, +Exp, +Vàng, +HP, +Crit
```

---

### 5. 🎖️ Hệ Thống Huy Hiệu (Badges)

**data_badges table**:
```sql
(1, eff=218, item=1289, 'Đại gia mới nhú', +Exp 15%, +Luck 30%)
(2, eff=219, item=1290, 'Trùm ước rồng', +Vàng 6%, +Luck 30%)  
(3, eff=220, item=1291, 'Trùm săn boss', +Exp 5%, +Luck 30%)
```

Kết hợp với **task_badges_template** và **BadgesTask.java**:
- Nhiệm vụ để unlock badge
- Badge cộng effect + item đặc biệt

---

### 6. 🏅 Thành Tích (Achievement)

**achievement_template** với 40+ thành tích:
```
1. Gia nhập Vệ Binh     → Đạt cấp Vệ Binh     → 10,000 vàng
2. Sức mạnh siêu cấp    → Đạt cấp %1           → 50 vàng/cấp
3. Nông dân chăm chỉ    → Cây đậu thần cấp 5   → thưởng
4. Trăm trận trăm thắng → Thắng 100 người       → thưởng
5. Nội công cao cường   → Chưởng 2000 phát      → thưởng
6. Khinh công thành thạo → Bay 20,000 mét       → thưởng
...
```

---

### 7. ⚔️ Super Rank (PvP Ranking)

**super_rank table** — hệ thống rank PvP:
- `rank`: cấp độ rank
- `ticket`: vé thi đấu
- `win/lose`: thắng/thua
- `history`: lịch sử trận đấu (JSON)
- `info`: thông tin mùa giải

---

### 8. 🛒 Shop Ký Gửi (Consignment Shop)

**shop_ky_gui table**:
- Player đăng bán item
- Mua theo vàng hoặc gem
- Quantity tracking
- `isUpTop`: ưu tiên hiển thị

---

### 9. 📋 Nhiệm Vụ Bang (Clan Task)

**clan_task_template** — 5 cấp độ nhiệm vụ:
```
Hạ %1 khủng long: 1-20 | 20-100 | 100-500 | 500-2000 | 2000-5000
Hạ %1 lợn lòi:   1-20 | ...
Hạ %1 quỷ đất:   1-20 | ...
```

---

### 10. 🎭 Background Item System

**bg_item_template** — item trang trí nền:
```sql
(0, image=0, layer=1, dx=-17, dy=-3)
(1, image=1, layer=4, dx=1, dy=5)
(2, image=2, layer=1, dx=19, dy=-2)
```

---

### 11. 👤 Avatar 2 Frame

**array_head_2_frames** — avatar đầu có 2 frame animation:
```sql
(0, '[965,978]'), (1, '[979,980]'), (2, '[981,982]')...
```
Hệ thống animation đầu mới hơn SRC-Team.

---

### 12. 🔧 Player Fields Mới

Player.java của Teamobi2026 có thêm:
- `vip` / `timevip` — VIP tier và thời hạn
- `point_sukien`, `point_sukien1`, `point_sukien2` — event points đa mùa
- `thachdauwhis` — thử thách Whis
- `point_vuahung` — điểm Hùng Vương event
- `point_maydam` / `total_damage_maydam` — máy đấm damage tracking
- `DuaHau` — sự kiện dưa hấu
- `baovetaikhoan` — bảo vệ tài khoản
- `mbv` / `mbvtime` — Mabư protection
- `levelLuyenTap` — cấp độ luyện tập offline
- `dangKyTapTuDong` — đăng ký tập tự động
- `thanhTichBang` / `thanhTichKhiGas` / `thanhTichCDRD` — thành tích bang hội
- `superRank` — Super Rank PvP object
- `badges` / `badgesTask` — huy hiệu system
- `binhChonHatMit` — sự kiện bình chọn hạt mít
- `timesPerDayCuuSat` — giới hạn cứu sát/ngày
- `tayThong` — đặc biệt
- `tradeWVP` / `itemsTradeWVP` / `goldTradeWVP` — giao dịch VIP

---

## 📊 So Sánh Database

| Bảng | SRC-Team | Teamobi2026 | Hành động |
|---|---|---|---|
| `achievement_template` | `achivements` (khác) | ✅ đầy đủ 40+ | **Migrate/merge** |
| `array_head_2_frames` | ❌ không có | ✅ có | **Import mới** |
| `bg_item_template` | ❌ không có | ✅ có | **Import mới** |
| `clan_task_template` | ❌ không có | ✅ có | **Import mới** |
| `data_badges` | ❌ không có | ✅ có | **Import mới** |
| `radar` | `collection_book` (khác) | ✅ nâng cấp | **Merge/replace** |
| `shop_ky_gui` | `consignment_shop` | ✅ tương tự | **So sánh schema** |
| `super_rank` | ❌ không có | ✅ có | **Import mới** |
| `cai_trang` | ✅ 351 dòng | ❌ không thấy | **Giữ nguyên SRC** |
| `mini_pet` | ✅ 28 pet | ❌ không thấy | **Giữ nguyên SRC** |
| `collection_book` | ✅ có | ❌ | **Migrate → radar** |
| `attribute_template` | ✅ có | ❌ | **Giữ nguyên SRC** |
| `nclass` | ✅ có | ❌ | **Giữ nguyên SRC** |

---

## 🎯 Thứ Tự Ưu Tiên Áp Dụng

### 🔴 Ưu tiên 1 — Boss Data (Tác động lớn nhất)

1. **Bổ sung BossID** cho boss mới: Cumber, Baby, Bojack team, GoldenFrieza+DeathBeam, Yardrat bosses
2. **Cập nhật BossesData** với HP/skill/dialog phong phú từ Teamobi
3. **Thêm Boss classes**: Cumber.java, Baby.java, Bojack team, Yardrat bosses
4. **Cập nhật BossManager.loadBoss()**: spawn đúng boss đúng lúc

### 🟠 Ưu tiên 2 — Seasonal Events

5. **Halloween bosses**: BiMa, Doi, MaTroi + HalloweenEventManager
6. **Noel boss**: OngGiaNoel + ChristmasEventManager  
7. **Tết boss**: LanCon + LunarNewYearEventManager
8. **Trung Thu bosses**: KhiDot, NguyetThan, NhatThan + TrungThuEventManager
9. **Hùng Vương bosses**: SonTinh, ThuyTinh + HungVuongEventManager

### 🟡 Ưu tiên 3 — Database Tables Mới

10. **Import radar table** (25+ thẻ bài với lore đầy đủ)
11. **Import achievement_template** (40+ thành tích)
12. **Import array_head_2_frames** (animation mới)
13. **Import bg_item_template** (background items)
14. **Import data_badges** + task_badges_template
15. **Import clan_task_template** (nhiệm vụ bang)
16. **Import super_rank** (PvP ranking)

### 🟢 Ưu tiên 4 — Code/Logic

17. **Thêm ConstMap.java** (named constants cho map IDs)
18. **Big Boss mobs**: Hirudegarn, Piano, GauTuongCuop trên map
19. **AnTrom system**: mini-boss xuất hiện ngẫu nhiên
20. **Player fields mới**: VIP, badges, event points (cần ALTER TABLE player)
21. **Badges system**: BadgesTask, BadgesTaskService, data_badges table
22. **Shop ký gửi**: so sánh và merge với consignment_shop hiện có

---

## ⚠️ Lưu Ý Khi Áp Dụng

1. **Boss classes cần decompile** từ `20.jar` (các file .class trong build/) vì source không có tất cả
2. **Seasonal event managers** trong Teamobi2026 là stub rỗng (14 dòng) — cần implement logic
3. **Radar table** format khác `collection_book` — cần viết migration
4. **Player table** cần ALTER TABLE để thêm các cột mới — **backup trước**
5. **MOD client** (`XUNGLORDLOCAL.exe`) là client test của họ, không dùng trực tiếp
6. **item.xlsx** có thể chứa item mới — cần extract và so sánh với item_template hiện có

---

## 📂 File Đã Extract Để Phân Tích

```
/tmp/teamobi_src/
├── BossID.java        (7.8KB)  - ID tất cả boss
├── BossesData.java    (123KB)  - Dữ liệu boss chi tiết  
├── BossManager.java   (18KB)   - Quản lý spawn boss
├── Boss.java          (32KB)   - Base class boss
├── Player.java        (74KB)   - Player object
├── SkillService.java  (59KB)   - Xử lý skill
├── Map.java           (17KB)   - Map system
├── ConstMap.java      (7.5KB)  - Map ID constants
├── NClass.java        (1.2KB)  - Tộc người
├── Skill.java         (2.5KB)  - Skill constants
├── Template.java      (5.9KB)  - Data templates
├── nro1.sql           (1.1MB)  - Full database
└── Config.properties  (494B)   - Server config
```

---

*Phân tích hoàn tất — July 18, 2026*

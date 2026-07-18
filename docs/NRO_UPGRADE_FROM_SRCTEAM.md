# 📋 NRO UPGRADE GUIDE — SRC-Team Source Analysis
> Phân tích toàn diện source code SRC-Team và hướng dẫn nâng cấp server NRO
> **File gốc:** `SRC-Team/nro.sql` (12.744 dòng, 64 bảng) + 417 file Java

---

## 🗂️ MỤC LỤC
1. [Tổng quan hệ thống](#1-tổng-quan-hệ-thống)
2. [Skill (Chiêu)](#2-skill-chiêu)
3. [Nhân vật & Tộc người](#3-nhân-vật--tộc-người)
4. [Cải trang](#4-cải-trang)
5. [Map & Waypoint](#5-map--waypoint)
6. [Item & Shop](#6-item--shop)
7. [NPC](#7-npc)
8. [Mob & Boss](#8-mob--boss)
9. [Hệ thống đặc biệt](#9-hệ-thống-đặc-biệt)
10. [Power Limit](#10-power-limit)
11. [Script nâng cấp DB](#11-script-nâng-cấp-db)
12. [Java Patches cần thiết](#12-java-patches-cần-thiết)

---

## 1. Tổng quan hệ thống

| Thành phần | SRC-Team | Ghi chú |
|---|---|---|
| Bảng DB | 64 bảng | account, player, skill_template, item_template... |
| File Java | 417 files | services, models, bosses, consts... |
| Skill | 33 skill (11/tộc) | Bao gồm Biến Hình, Phân Thân mới |
| Item | ~2.115 item | 3 block INSERT |
| Map | 149 map | Đầy đủ waypoints, mobs, NPCs |
| Mob | 84 loại | Từ Mộc nhân đến Hirudegarn |
| NPC | 79 loại | Bill, Whis, Champa... |
| Cải trang | 351 bộ | head/body/leg/bag |
| Mini pet | 28 con | |
| Boss | 40+ boss class | Bill, Broly, Cell các dạng, Chill, Cooler, Cadich... |
| Collection Book | 18 thẻ | |
| Power Limit | 12 cấp | 18B → 180B power |
| Item Option | 119+ loại | |
| Shop tabs | 46 tab | |

---

## 2. Skill (Chiêu)

### 2.1 Trái Đất (nclass_id = 0) — 11 skill
| Slot | ID | Tên | Max Point | Loại | Yêu cầu power |
|---|---|---|---|---|---|
| 0 | 0 | Chiêu đấm Dragon | 7 | Tấn công cận | 1.000 → 1.8M |
| 1 | 1 | Chiêu Kamejoko | 7 | Tấn công xa | 10K → 4.8M |
| 2 | 6 | Thái Dương Hạ San | 7 | Buff gây choáng | 150K → ... |
| 3 | 9 | Kaioken | 7 | Buff tấn công | 1.5M → ... |
| 4 | 10 | Quả cầu kênh khí | 7 | AOE | |
| 5 | 19 | Khiên năng lượng | 7 | Shield | |
| 6 (slot trống) | — | — | — | — | — |
| 7 | 20 | Dịch chuyển tức thời | 7 | Gây choáng | |
| — | 22 | Thôi miên | 7 | CC/ngủ | |
| 8 | 24 | **Super Kamejoko** | 9/10 | Siêu chiêu | 60B |
| 9 | 27 | **Biến Hình Trái Đất** | 5 | Transform | 250B → 3.000B |
| 10 | 28 | **Phân thân** | 7 | Clone | 1.000B |

### 2.2 Namếc (nclass_id = 1) — 11 skill
| Slot | ID | Tên | Max Point | Loại |
|---|---|---|---|---|
| 0 | 2 | Chiêu đấm Demon | 7 | Cận chiến |
| 1 | 3 | Chiêu Masenko | 7 | Tấn công xa |
| 2 | 7 | Trị thương | 7 | Heal đồng đội |
| 3 | 11 | Makankosappo | 7 | Xuyên mục tiêu |
| 4 | 12 | Đẻ trứng | 7 | Tạo mob hỗ trợ |
| 5 | 17 | Liên hoàn | 7 | Chuỗi tấn công |
| 6 | 18 | Biến Sôcôla | 7 | Biến quái thành chocolate |
| 7 | 19 | Khiên năng lượng | 7 | Shield |
| 8 | 26 | **Ma Phong Ba** | 9 | Siêu chiêu (gây dame mỗi giây) |
| 9 | 27 | **Biến Hình Namếc** | 5 | Transform |
| 10 | 28 | **Phân thân** | 7 | Clone |

### 2.3 Xayda (nclass_id = 2) — 11 skill
| Slot | ID | Tên | Max Point | Loại |
|---|---|---|---|---|
| 0 | 4 | Chiêu đấm Galick | 7 | Cận chiến |
| 1 | 5 | Chiêu Antomic | 7 | Tấn công xa |
| 2 | 8 | Tái tạo năng lượng | 7 | Tự hồi HP/KI |
| 3 | 13 | Biến Khỉ | 7 | Buff HP+Dame+Speed |
| 4 | 14 | Tự phát nổ | 7 | Hy sinh gây dame |
| 5 | 19 | Khiên năng lượng | 7 | Shield |
| 6 | 21 | Huýt sáo | 7 | Tăng HP cho mọi người |
| 7 | 23 | Trói | 7 | CC/trói |
| 8 | 25 | **Cađíc liên hoàn chưởng** | 9 | Siêu chiêu |
| 9 | 27 | **Biến hình Saijan** | 5 | Transform |
| 10 | 28 | **Phân thân** | 7 | Clone |

### 2.4 Thông số Biến Hình (ConstPlayer.java)
```java
// Aura theo cấp biến hình (level 1-5)
Trái Đất: aura = {7, 7, 13, 6, 73}
Namếc:    aura = {4, 5, 13, 6, 74}
Xayda:    aura = {8, 8, 13, 5, 69}

// Head sprite biến hình (5 cấp)
Trái Đất head: {1463, 1443, 1444, 1445, 1446}
Namếc head:    {1449, 1450, 1451, 1452, 1453}
Xayda head:    {1456, 1457, 1458, 1459, 1460}

// Body/Leg biến hình
Trái Đất body=1461, leg=1462
Namếc    body=1447, leg=1448
Xayda    body=1454, leg=1455

// % bonus HP mỗi cấp biến hình
getPercentHpBienHinh(level) = (level + 1) * 10 %

// Biến Khỉ (Xayda):
HEADMONKEY = {198, 198, 198, 198, 198, 198, 198} (7 level)
getPercentHpMonkey(level) = (level + 3) * 10 %
getTimeMonkey(level) = (level + 5) * 10000 ms
```

### 2.5 Thời gian hiệu ứng skill (SkillUtil.java)
```
Choáng Thái Dương Hạ San: (level + 2) * 1000 ms
Sôcôla: 30.000 ms (cố định)
Ma Phong Ba: 10.000 ms
Khiên: (level + 2) * 5.000 ms
Trói: level * 5.000 ms
DCTT choáng: (level + 1) * 500 ms
Thôi miên: (level + 4) * 1.000 ms
```

---

## 3. Nhân vật & Tộc người

### 3.1 Classes (nclass)
```sql
-- 3 tộc người
(0, 'Trái Đất', ...),
(1, 'Namếc', ...),
(2, 'Xayda', ...)
```

### 3.2 Fusion System (ConstPlayer.java)
```java
NON_FUSION   = 0
LƯỠNG LONG NHẤT THỂ = 4   // Fusion dạng 1
HỢP THỂ POTARA  = 6        // Fusion dạng 2
HỢP THỂ POTARA2 = 8        // Fusion dạng 3
```

### 3.3 PVP Types
```java
NON_PK = 0
PK_PVP = 3    // PvP thông thường
PK_ALL = 5    // PK tất cả
```

### 3.4 DanhHieu (Danh hiệu) - Player.java
- DH1 → DH5: mỗi danh hiệu có ChiSoHP, ChiSoKI, ChiSoSD riêng
- Cộng vào stat của player

---

## 4. Cải trang

### 4.1 Số lượng: **351 bộ cải trang**
Mỗi bộ gồm: `tempId, head, body, leg, bag`

### 4.2 Model (CaiTrang.java)
```java
public class CaiTrang {
    public int tempId;
    public int[] id; // [head, body, leg, bag]
}
```

### 4.3 Trigger cải trang trong code
- `Service.getInstance().Send_Caitrang(player)` — gửi outfit cho player
- Được gọi trong Ma Phong Ba effect, khi dùng Sôcôla, khi biến hình
- `SkillService` gọi `Send_Caitrang` sau nhiều hiệu ứng skill

### 4.4 Cải trang nâng cao (item_option_template)
```
Option 40: 'Siêu cải trang # đá ngũ sắc'  — cải trang premium
```

---

## 5. Map & Waypoint

### 5.1 Tổng: **149 map** (map_template)
Cấu trúc mỗi map: `id, name, data[flags], zones, max_player, waypoints[], mobs[], npcs[], effects[]`

### 5.2 Map quan trọng (ConstMap.java)
```
0=Làng Aru, 1=Đồi Hoa Cúc, 2=Thung Lũng Tre, 3=Rừng Nam
5=Đảo Kame, 6=Động Karin, 7=Làng Moori
10=Thung Lũng Namếc, 14=Làng Kakarot, 16=Làng Plant
19=Thành Phố Vegeta, 46=Tháp Karin, 48=Hành tinh Kaio
49=Phòng tập thời gian, 51=Đấu trường, 52=Đại Hội Võ Thuật
```

### 5.3 Map types đặc biệt (ConstMap.java)
```java
MAP_NORMAL = 0
MAP_OFFLINE = 1          // Map offline training
MAP_DOANH_TRAI = 2       // Dungeon doanh trại
MAP_BLACK_BALL_WAR = 3   // Chiến tranh Ngọc Đen
MAP_BAN_DO_KHO_BAU = 4   // Bản đồ kho báu
MAP_KHI_GAS_HUY_DIET = 5 // Dungeon khí gas hủy diệt
```

### 5.4 Capsule Maps (MapService.getMapCapsule)
- 16 map capsule đặc biệt
- Vào từ waypoint CHANGE_CAPSULE (500)
- Có mob xếp theo nhóm, drop item xịn

### 5.5 War systems
```java
CHANGE_BLACK_BALL = 501  // Chiến tranh Ngọc Đen
CHECK_BOSS = 502         // Tìm boss
BlackBallWar, NamekBallWar, MabuWar (14h)
```

---

## 6. Item & Shop

### 6.1 Tổng: **2.115 item** (3 block INSERT)
| Block | Số item | Loại |
|---|---|---|
| Block 1 | 632 | item cơ bản (áo, quần, găng, giày, radar, ngọc rồng) |
| Block 2 | 643 | item cao cấp, sự kiện |
| Block 3 | 840 | item mới nhất |

### 6.2 Item types
```
TYPE 0 = Áo (trang bị ngực)
TYPE 1 = Quần
TYPE 2 = Găng tay
TYPE 3 = Giày
TYPE 4 = Rada (cộng crit)
TYPE 6 = Đậu thần
TYPE 12 = Ngọc rồng (7 loại, 1-7 sao)
TYPE 15 = FlagBag (túi cờ - XienCa, KiemZ...)
TYPE ... = Set bonus (Piccolo set, Nappa set)
```

### 6.3 Item Options (119+ loại)
```
0  = Tấn công +#
2  = HP, KI +#000
5  = +#% sức đánh chí mạng
6  = HP +#
7  = KI +#
14 = Chí mạng +#%
15 = Phản đòn cận chiến +#
16 = Tốc độ di chuyển +#%
41 = Chỉ số thưởng +#: (set bonus)
42 = Tấn công +#% lên quái bay
43 = Tấn công +#% lên quái khỉ
44 = Tấn công +#% lên quái mặt đất
47 = Giáp +#
49 = Tấn công +#%
88 = Cộng #% exp khi đánh quái
94 = Giáp #%
95 = Biến #% tấn công thành HP
97 = Phản #% sát thương
98 = Xuyên giáp #% chưởng
100 = +#% vàng rơi
101 = +#% TN,SM
231 = (xương cho Super Kame / Ma Phong Ba)
```

### 6.4 Đồ thiên sứ (DoThienSu - ItemService)
- Item random stats cao
- `createNewItem` + `DoThienSu()` random option
- Có các bộ set: Piccolo set (+HP, cộng khi full bộ), Nappa set (+dame)

### 6.5 Shop Structure (tab_shop)
```
Shop 1,2,3 = Shop trang bị (Áo/Quần, Phụ kiện, Đặc biệt) — 3 tộc
Shop 4     = Shop sách võ (Sách Võ, Sách Chưởng, Sách ĐB, Phụ kiện)
Shop 5     = Tiệm hớt tóc
Shop 11    = Shop cải trang (Trái Đất, Namếc, Xayda)
Shop 12    = Shop cải trang 2
Shop 13,35 = Đặc biệt
Shop 14    = Sự kiện
Shop 20    = Cửa hàng Whis
Shop 24    = Cải Trang / Đeo Lưng / Linh Thú / Trang bị
Shop 23    = Newbie
```

---

## 7. NPC

### 7.1 Tổng: **79 NPC** (npc_template)
```
0  = Ông Gôhan         (18, 19, 20)
1  = Ông Paragus       (24, 25, 26)
2  = Ông Moori         (21, 22, 23)
3  = Rương đồ          (74, 75, 265)
4  = Đậu thần
5  = Con mèo           — dùng cho popup menu
6  = Khu vực           — teleport zone
7  = Bunma             (42, 43, 44)
8  = Dende             (45, 46, 47)
9  = Appule            (3, 4, 5)
10 = Dr. Brief         (784, 785, 786)
13 = Quy Lão Kame      (33, 34, 35)
14 = Trưởng lão Guru   (39, 40, 41)
15 = Vua Vegeta        (36, 37, 38)
18 = Thần Mèo Karin    (89, 90, 91)
19 = Thượng Đế         (86, 87, 88)
20 = Thần Vũ Trụ       (98, 99, 100)
24 = Rồng Thiêng       (103, 104, 105)
27 = Rồng Thiêng Namếc
28 = Cửa hàng ký gửi   (120, 121, 122)
29 = Rồng Omega        (204, 205, 206)
30-36 = Rồng 2-7 sao + 1 sao
37 = Bunma thay đổi    (267, 268, 269)
38 = Ca Lích           (270, 271, 272)
39 = Santa             (300, 301, 302)
40 = Mabư mập          (297, 298, 299)
43 = Tổ Sư Kaio        (448, 449, 450)
45 = Kibit             (436, 437, 438)
46 = Babiđây           (430, 431, 432)
48 = Ngộ Không         (462, 470, 471)
50 = Quả trứng         (MabuEgg NPC)
54 = GoKu Tài Xỉu      (mini-game)
55 = Bill              (508, 509, 510)
56 = Whis              (505, 506, 507)
57 = Champa            (511, 512, 513)
58 = Vados             (530, 531, 532)
64 = Cađíc             (645, 646, 647)
70 = Bardock           (1012, 1013, 1014)
72 = ToriBot           (1143, 1144, 1145)
73 = Sự Kiện           (1173, 1174, 1175)
74 = Fide              (1062, 1063, 1064)
77 = Daishinkan        (703, 704, 705)
78 = Lý Tiểu Nương     (487, 488, 489)
```

### 7.2 NPC Menu System (NpcService.java)
```java
// 2 loại menu chính
createMenuConMeo(player, indexMenu, avatar, npcSay, menuSelect...)
createMenuRongThieng(player, indexMenu, npcSay, menuSelect...)

// Tutorial
createTutorial(player, avatar, npcSay)

// CMD 32 = hiện menu dialog
// CMD 38 = hiện tutorial dialog
```

---

## 8. Mob & Boss

### 8.1 Mob (84 loại, mob_template)
| Range | Tên | HP |
|---|---|---|
| 0-3 | Mộc nhân, Khủng long, Lợn lòi, Quỷ đất | 100-200 |
| 4-12 | Dạng mẹ + bay | 500-1.000 |
| 13-27 | Ốc mượn hồn, Heo Xayda, Tambourine, Drum... | 1.500-20.000 |
| 28-38 | Không tặc, Robot bay, Nappa, Soldier | 30.000-50.000 |
| 39-53 | Appule → Lính vũ trụ | 60K-170K |
| 54-65 | Khỉ các loại, Xên con 1-8 | 300K-550K |
| 66-69 | Tai tím, Abo, Kado, Da xanh | 350K-500K |
| 70 | **Hirudegarn** | 40.000.000 |
| 71 | **Vua Bạch Tuộc** | 1.500.000 |
| 72 | **Rôbốt bảo vệ** | 1.000.000 |
| 77 | **Gấu tướng cướp** | 2.000.000.000 |
| 78-83 | Khỉ xanh, Taburine Đỏ, Cabira, Tobi... | - |

**Mob TYPE:**
- 0 = Mộc nhân (đứng yên)
- 1 = Mob mặt đất
- 4 = Mob bay
- Boss types: riêng từng class

### 8.2 Boss Classes (40+ boss)

#### Bill system
- `Bill.java`, `Whis.java`
- Bill thay đổi HP theo giờ server

#### Broly system
- `Broly.java`, `SuperBroly.java`, `Pet_Broly.java`

#### Cell system
- `XenBoHung.java` (Cell cơ bản)
- `XenBoHung1.java`, `XenBoHung2.java` (Cell dạng 1, 2)
- `XenBoHungHoanThien.java` (Cell hoàn thiện)
- `SieuBoHung.java` (Super Cell)
- `XenMax.java`, `XenCon.java`

#### Chill & Cooler system
- `Chill.java`, `Chill2.java`
- `Cooler.java`, `Cooler2.java`

#### Cadich system
- `Cadich.java`, `Nadic.java`, `Saibamen.java`
- `CBoss.java` (base class)

#### Tương lai system
- `Blackgoku.java`, `Superblackgoku.java`

#### Đại Hội Võ Thuật (DHVT)
- `JackyChun.java`, `LiuLiu.java`, `ChaPa.java`, `ChanXu.java`
- `ODo.java`, `PonPut.java`, `SoiHecQuyn.java`
- `TauPayPay.java`, `ThienXinHang.java`, `ThienXinHangClone.java`, `Xinbato.java`

#### Dungeon bosses
- DoanhTrai: `NinjaAoTim.java`, `TrungUyThep.java`, `TrungUyTrang.java`, `TrungUyXanhLo.java`, `RobotVeSi.java`
- BanDoKhoBau: `BossBanDoKhoBau.java`, `TrungUyXanhLo.java`
- KhiGasHuyDiet: `DrLychee.java`, `BossKhiGasHuyDiet.java`

### 8.3 BossManager.java
```java
// Lưu tất cả boss đang active
List<Boss> BOSSES_IN_GAME

// API chính
updateAllBoss()           // Gọi mỗi tick game
getBossById(int id)
addBoss(Boss boss)
removeBoss(Boss boss)
FindBoss(Player, bossId)  // Teleport player đến boss
getBossTau77ByPlayer(Player) // Boss riêng theo player
```

---

## 9. Hệ thống đặc biệt

### 9.1 Mini Pet (28 con, mini_pet)
Mỗi pet: `id_temp, head, body, leg`
```
Pet IDs: 916, 917, 918, 919, 1046, 936, 892, 893, 942, 943, 944,
         967, 1039, 1040, 1114, 1188, 1202, 1203, 1224, 1225,
         1243, 1244, 1107, 1207, 1226, 1273, 1274, 1275
```

### 9.2 Collection Book (18 thẻ)
Bộ sưu tập thẻ mob — thu thập để nhận thưởng

### 9.3 Attribute System (attribute_template)
5 attribute server-wide:
- TNSM (tiềm năng sức mạnh)
- Vàng
- KI
- HP
- Sức Đánh

Giá trị áp dụng toàn server từ `attribute_server`

### 9.4 Intrinsic System
- Nội tại item — stat cố định built-in vào item
- Bảng `intrinsic` riêng

### 9.5 Siêu Hàng (SieuHang)
- Hệ thống hàng siêu cấp premium
- `CloneSieuHang.java` — clone item siêu hàng

### 9.6 Cửa hàng ký gửi (ConsignmentShop)
- Player bán đồ cho nhau
- Bảng `consignment_shop`
- NPC id=28 "Cửa hàng kí gửi"

### 9.7 PVP System (PVPService.java)
- Cược vàng: 1M, 10M, 100M
- `ChallengePVP`, `RevengePVP`
- Revenge: tốn 10 hồng ngọc, tìm 5 phút

### 9.8 War Systems
- **BlackBall War** — Chiến tranh Ngọc Đen (MAP_TYPE 3)
- **NamekBall War** — Chiến tranh Hành Tinh Namếc
- **MabuWar 14H** — Chiến tranh 14 giờ hàng ngày

### 9.9 Đại Hội Võ Thuật (DaiHoiVoThuat)
- Map ID 52 = `DAI_HOI_VO_THUAT`
- Boss DHVT: JackyChun, LiuLiu, ChaPa, ChanXu, ODo, PonPut...
- Giải thưởng riêng

### 9.10 Bản Đồ Kho Báu (BanDoKhoBau)
- MAP_TYPE 4
- Boss: TrungUyXanhLo, BossBanDoKhoBau
- TopBanDoKhoBau — xếp hạng

### 9.11 Khí Gas Hủy Diệt (KhiGasHuyDiet)
- MAP_TYPE 5
- Boss: DrLychee, BossKhiGasHuyDiet
- Phòng thử thách theo level

### 9.12 Mini-Games
- **Tài Xỉu** — NPC GoKu Tài Xỉu (id 54)
- **Chọn Ai Đây** (ChonAiDay) — ai đó trong xích lô
- **Con Số May Mắn** (ConSoMayMan)
- **Vòng Quay May Mắn** (LuckyRound) — Gem/Gold

### 9.13 Achievement System (AchiveManager)
- Bảng `achivements`
- `ConstAchive` constants

### 9.14 Pet Follow (pet_follow)
- Pet đi theo player trên map
- Khác với Mini Pet (trang bị)

---

## 10. Power Limit

### 10.1 12 cấp Power Limit
| Cấp | Power yêu cầu | HP Max | KI Max | Dame | Def | Crit |
|---|---|---|---|---|---|---|
| 1 | 17.999.999.999 | 215.000 | 215.000 | 8.600 | 550 | 5 |
| 2 | 18.999.999.999 | 239.000 | 239.000 | 9.565 | 600 | 6 |
| 3 | 20.999.999.999 | 265.000 | 265.000 | 10.628 | 700 | 7 |
| 4 | 24.999.999.999 | 295.000 | 295.000 | 11.809 | 800 | 8 |
| 5 | 30.999.999.999 | 328.000 | 328.000 | 13.122 | 1.000 | 9 |
| 6 | 40.999.999.999 | 364.000 | 364.000 | 22.000 | 1.200 | 10 |
| 7 | 60.999.999.999 | 500.000 | 500.000 | 14.580 | 1.400 | 11 |
| 8 | 80.999.999.999 | 405.000 | 405.000 | 16.200 | 1.600 | 12 |
| 9 | 100.999.999.999 | 450.000 | 450.000 | 18.000 | 1.700 | 13 |
| 10 | 140.000.000.000 | 500.000 | 500.000 | 25.000 | 1.800 | 14 |
| 11 | 160.000.000.000 | 550.000 | 550.000 | 26.000 | 1.900 | 15 |
| 12 | 180.000.000.000 | 600.000 | 600.000 | 28.000 | 2.100 | 16 |

### 10.2 MAX_LIMIT = 11 (NPoint.java)
- Server giới hạn tối đa tier 11 khi tính stat

---

## 11. Script nâng cấp DB

### 11.1 Files đã tạo
- `docs/nro_upgrade_data.sql` — SQL upgrade toàn bộ game data (7.482 dòng)
- `docs/nro_srcteam.sql` — SQL gốc đầy đủ (12.744 dòng)
- `scripts/nro_push_upgrade.sh` — Script push lên Codespace

### 11.2 Cách dùng

```bash
# Option 1: Chạy script tự động
bash scripts/nro_push_upgrade.sh

# Option 2: Import thủ công trên Codespace
mysql -u root -p nro1 < docs/nro_upgrade_data.sql

# Option 3: Import từng bảng cụ thể
mysql -u root -p nro1 << 'EOF'
-- Chỉ update skill_template
DROP TABLE IF EXISTS skill_template;
SOURCE docs/nro_upgrade_data.sql
EOF
```

### 11.3 Thứ tự import an toàn
```
1. item_option_template  (không phụ thuộc)
2. nclass               (không phụ thuộc)
3. skill_template       (phụ thuộc nclass)
4. part                 (không phụ thuộc)
5. item_template        (phụ thuộc part)
6. mob_template         (không phụ thuộc)
7. npc_template         (không phụ thuộc)
8. map_template         (phụ thuộc mob, npc)
9. cai_trang            (không phụ thuộc)
10. mini_pet            (không phụ thuộc)
11. collection_book     (không phụ thuộc)
12. power_limit         (không phụ thuộc)
13. shop, tab_shop, item_shop (phụ thuộc item)
14. attribute_template  (không phụ thuộc)
15. intrinsic           (phụ thuộc item)
```

---

## 12. Java Patches cần thiết

### 12.1 ✅ Đã có trong SRC-Team, cần compile và deploy

#### Skill Biến Hình + Phân Thân
- File: `SkillService.java` — `useSkillAlone()`, `userSkillSpecial()`
- File: `ConstPlayer.java` — AURABIENHINH, HEADBIENHINH, BODYBIENHINH, LEGBIENHINH
- Logic: khi dùng skill id 27/28, thay đổi sprite + stat

#### Boss System
- Tất cả boss trong `models/boss/` — cần deploy JAR mới
- `BossManager.java` — quản lý boss lifecycle

#### War Systems
- `MapService.java` — BlackBallWar, NamekBallWar, MabuWar14H
- Cần config map war trong map_template

#### PVP System
- `PVPServcice.java` — cược vàng PvP
- Cần NPC ConMeo (id=5) trong map

#### Siêu Hàng + ConsignmentShop
- `ShopService.java` — ConsignmentShop
- NPC id=28 trong map

### 12.2 🛠️ Cần làm trước khi deploy

```
1. Compile toàn bộ Maven project trong SRC-Team/Soucre/
2. Build JAR: mvn clean package
3. Copy JAR vào Codespace: scp target/nro.jar user@codespace:/opt/nro/
4. Restart server
```

### 12.3 📋 Config cần thêm (nro.properties / config)
```
# Thêm các setting cho hệ thống mới
BILL_TIMER=...         # Giờ Bill xuất hiện
MABU_WAR_TIME=14:00    # MabuWar 14h
BLACKBALL_WAR_TIME=...
DOANH_TRAI_RESET=...   # Reset dungeon mỗi ngày
```

---

## 📌 Lưu ý quan trọng

1. **Backup DB trước khi import** — `mysqldump -u root -p nro1 > backup_$(date +%Y%m%d).sql`
2. **Nếu server đang chạy** — dừng server trước khi import schema changes
3. **item_template** có 3 block INSERT — import theo thứ tự từ trên xuống
4. **map_template** chứa JSON phức tạp — không edit tay, import nguyên từ SQL
5. **player table** KHÔNG có trong upgrade SQL — dữ liệu người chơi an toàn
6. **account table** KHÔNG có trong upgrade SQL — tài khoản an toàn

---

*Tài liệu tạo từ phân tích SRC-Team source — July 2026*

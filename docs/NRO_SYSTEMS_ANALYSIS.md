# 🔬 NRO SYSTEMS ANALYSIS — Phân tích chi tiết từng hệ thống

## Nguồn: SRC-Team source (`nro.sql` + 417 Java files)

---

## 1. Database Schema — 64 Bảng

### Bảng Game Data (cần import)
| Bảng | Rows | Mục đích |
|---|---|---|
| `skill_template` | 33 | Skill cho 3 tộc |
| `item_template` | 2115 | Toàn bộ item |
| `item_option_template` | 119+ | Loại stat item |
| `item_shop` | nhiều | Shop items |
| `item_shop_option` | nhiều | Shop item options |
| `mob_template` | 84 | Mob/quái |
| `npc_template` | 79 | NPC |
| `map_template` | 149 | Map + spawn |
| `cai_trang` | 351 | Cải trang |
| `mini_pet` | 28 | Mini pet |
| `part` | 98 | Skin parts |
| `collection_book` | 18 | Thẻ bài |
| `power_limit` | 12 | Power tier |
| `attribute_template` | 5 | Server buff types |
| `attribute_server` | - | Giá trị buff server |
| `nclass` | 3 | Tộc người |
| `head_avatar` | nhiều | Avatar đầu |
| `shop` | nhiều | Shop info |
| `tab_shop` | 46 | Tab shop |
| `flag_bag` | - | FlagBag data |
| `intrinsic` | nhiều | Nội tại item |
| `consignment_shop` | - | Ký gửi |
| `task_main_template` | - | Nhiệm vụ chính |
| `side_task_template` | - | Nhiệm vụ phụ |
| `task_sub_template` | - | Sub task |
| `pet_follow` | - | Pet follow |

### Bảng Player Data (KHÔNG import — giữ nguyên)
| Bảng | Mục đích |
|---|---|
| `account` | Tài khoản |
| `player` | Dữ liệu nhân vật |
| `clan_sv1`, `clan_sv2` | Băng hội |
| `super`, `super_history`, `super_top` | Siêu hàng |

### Bảng ATM/Payment (không liên quan)
`atm_acb`, `atm_bank`, `atm_check`, `atm_lichsu`, `mbbank`, `napthe`, `topup`, `trans_log`

### Bảng Web/Forum (cho website NRO)
`baiviet_hoangvietdung`, `category`, `comment`, `comments`, `cpanel`, `forum_baiviet`, `img_by_name`, `notifications`, `phongchat`, `post`, `posts`

---

## 2. Skill Analysis

### 2.1 Phân bố skill type
| Type | Mô tả | Skills |
|---|---|---|
| type=1 | Tấn công (cần target) | Dragon, Kamejoko, Demon, Masenko, Galick, Antomic, Kaioken, Quả cầu, Makankosappo, Liên hoàn, Sôcôla, DCTT, Trói |
| type=2 | Buff đồng đội | Trị thương |
| type=3 | Buff bản thân (không cần target) | Thái Dương Hạ San, Tái tạo NL, Biến Khỉ, Tự phát nổ, Khiên NL, Huýt sáo, Biến Hình |
| type=4 | Siêu chiêu (multi-step) | Super Kamejoko, Cađíc LHC, Ma Phong Ba, Phân thân |

### 2.2 Mana use type
| mana_use_type | Mô tả |
|---|---|
| 0 | Tốn MP cố định |
| 1 | Tốn % MP max |
| 2 | Tốn theo cách khác |

### 2.3 Skill progression (damage scaling)
```
Dragon: 100% → 110% → 120% → 130% → 140% → 150% → 160%
Kamejoko: 150% → 200% → 250% → 300% → 350% → 400% → 450%
Super Kamejoko: 550% → 600% → ... → 1000% (10 cấp, cần 60B power)
Biến Hình: 5% → 10% → 15% → 20% → 30% (tăng % stat, 5 cấp, cần 250B → 3000B)
Phân thân: 10% → 20% → 30% → 40% → 50% → 60% → 70% sức (7 cấp, cần 1000B)
```

---

## 3. Item Analysis

### 3.1 Item type mapping
| TYPE | Slot | Stat chính |
|---|---|---|
| 0 | Áo (chest) | Defense |
| 1 | Quần (legs) | HP |
| 2 | Găng (hands) | Attack |
| 3 | Giày (feet) | KI/MP |
| 4 | Rada | Crit |
| 6 | Đậu thần | HP/KI restore |
| 7 | Vũ khí/Đặc biệt | - |
| 12 | Ngọc rồng | Wish |
| 13 | FlagBag | Đặc biệt |
| 14 | Sách skill | Học skill |
| 15 | Bùa | Buff thời gian |

### 3.2 Set bonuses (NPoint.java)
```
Piccolo set (Namếc set): 
  Áo: icon 394, Quần: 397, Găng: 412, Giày: 406
  → Khi đủ 4 món: +HP bonus đặc biệt

Nappa set (Xayda set):
  → +Attack damage bonus khi đủ bộ
```

### 3.3 Item thời gian (ItemTime)
- Item slot 7 (special) có thể là item thời gian
- Hiệu ứng bao gồm: bay miễn phí, +exp, tàng hình, hút HP/KI
- Option 1 = "Thời gian sử dụng # phút"
- Xử lý trong `ItemTimeService.gI().sendItemTime(player, itemId, time)`

### 3.4 Shop structure
```
Shop 1 = Trái đất shop (áo/quần, phụ kiện, đặc biệt)
Shop 2 = Namếc shop
Shop 3 = Xayda shop
Shop 4 = Sách skill (Võ, Chưởng, ĐB, Phụ kiện)
Shop 5 = Tiệm hớt tóc
Shop 11 = Cải trang Trái đất
Shop 12 = Cải trang Namếc/Xayda
Shop 13,35 = Đặc biệt
Shop 14 = Event shop
Shop 20 = Shop Whis (premium)
Shop 23 = Newbie shop
Shop 24 = Siêu cải trang (Đeo Lưng, Linh Thú)
```

---

## 4. Map Analysis

### 4.1 Map data format
```json
// map_template.data: [type, ?, ?, bgMusic, ?]
// map_template.zones: số zone
// map_template.max_player: max player/zone
// map_template.waypoints: [[name,x1,y1,x2,y2,mapType,mapId,checkBoss,toX,toY]]
// map_template.mobs: [[mobId, mobId2, maxCount, x, y]]
// map_template.npcs: [[npcId, x, y, direction]]
// map_template.effect: [[effType, params...]]
```

### 4.2 Zone system
```
Mỗi map có thể có nhiều zone (zone = instance riêng)
Zone được tạo khi đủ người → server tạo zone mới
max_player = số player tối đa/zone
```

### 4.3 Waypoint format
```
[name, x1,y1, x2,y2, toMapType, toMapId, checkBoss, toX, toY]
- Đi qua vùng (x1,y1)→(x2,y2) sẽ teleport đến toMapId:toX,toY
- checkBoss: 0=bình thường, 502=check boss trước khi vào
```

### 4.4 Mob spawn format
```
[mobTemplateId, mobTemplateId2, maxCount, spawnX, spawnY]
- mobTemplateId2 = mob mẹ (khi đủ số mob thường thì sinh mẹ)
- Hệ thống auto respawn mob sau khi chết
```

---

## 5. Boss System Analysis

### 5.1 Boss lifecycle
```
1. Spawn: BossFactory.createXxx() → BossManager.addBoss(boss)
2. Active: BossManager.updateAllBoss() gọi boss.update() mỗi tick
3. Die: boss.injured() → hp <= 0 → dropReward → BossManager.removeBoss()
4. Respawn: theo timer riêng của từng boss
```

### 5.2 Boss ID system
```
ID > 0: Boss thường (Bill id=1, Broly id=2...)
ID < 0: Boss riêng cho player
getBossTau77ByPlayer(player) → id = -251003 - player.id - 2000
```

### 5.3 Boss types và đặc điểm
```
Bill: xuất hiện theo giờ, HP cố định, cần nhiều player
Broly: solo boss, reward tốt
Cell các dạng: chain battle (XenBoHung → dạng 1 → dạng 2 → Hoàn Thiện)
Chill/Cooler: mid-tier boss
Cadich: theo cặp với Nadic/Saibamen
Blackgoku: boss tương lai, cực mạnh
DHVT bosses: trong sự kiện Đại Hội Võ Thuật
Dungeon bosses: trong dungeon riêng
```

---

## 6. War Systems

### 6.1 BlackBall War
```
- Chiến tranh ngọc đen (MAP_BLACK_BALL_WAR = type 3)
- Server broadcast khi war bắt đầu
- Nhân x2 stats cho tất cả trong vùng war
- Drop ngọc đặc biệt
```

### 6.2 NamekBall War
```
- Chiến tranh hành tinh Namếc
- Tương tự BlackBall nhưng map khác
- Drop ngọc Namếc
```

### 6.3 MabuWar 14H
```
- Diễn ra lúc 14:00 hàng ngày
- NPC Mabư mập (id=40) liên quan
- Trứng Mabư (NPC id=50) cần bảo vệ hoặc phá
- MabuEgg model đặc biệt
```

---

## 7. Mini-Game Systems

### 7.1 Tài Xỉu
```
- NPC GoKu Tài Xỉu (id=54, sprite: head=101, body=57, leg=66)
- Đặt cược vàng
- Chọn Tài/Xỉu, quay 3 xúc xắc
- Xử lý trong TaiXiuService hoặc Player.taiXiuBet
```

### 7.2 Chọn Ai Đây (ChonAiDay)
```
- 3 xích lô, 1 cái có Goku ẩn
- Chọn đúng → nhận thưởng
- Player.chonAiDayCurrent
```

### 7.3 Con Số May Mắn
```
- Đoán số
- Player.conSoMayMan
```

### 7.4 Lucky Round (Vòng Quay)
```
- LuckyRound item → quay vòng
- LuckyRoundGem → quay bằng gem
- LuckyRoundGold → quay bằng gold
- Kết quả random từ prize table
```

---

## 8. Attribute System

### 8.1 attribute_template (5 loại)
```sql
-- Server-wide buff applies to all players
TNSM     -- Tiềm năng sức mạnh (exp rate)
Vàng     -- Gold drop rate
KI       -- KI bonus
HP       -- HP bonus  
Sức Đánh -- Attack bonus
```

### 8.2 attribute_server
```
-- Current values active on server
-- Admin có thể thay đổi qua panel
-- Affect all players immediately
```

---

## 9. Collection Book System

### 9.1 18 loại thẻ
- Mỗi loại thẻ tương ứng 1 mob
- Tiêu diệt mob để nhận thẻ (% drop)
- Thu đủ bộ thẻ → nhận reward đặc biệt

### 9.2 Implementation
```java
// Khi mob chết:
Mob.injured() → MobService.dropItemTask() 
→ check collection_book table
→ nếu mob có thẻ → random drop

// Khi đủ bộ:
TaskService.checkCollectionBook(player)
→ reward thưởng
```

---

## 10. Intrinsic System

### 10.1 Nội tại item
- Stat built-in cố định của item (không random)
- Tách biệt với item_option (option có thể random/nâng cấp)
- Ví dụ: Đồ thiên sứ có nội tại cố định + option random

### 10.2 Bảng intrinsic
```sql
-- id, item_id, stat_type, value
-- stat_type: HP, KI, dame, def, crit, ...
```

---

## 11. Fusion System

### 11.1 Loại fusion (ConstPlayer.java)
```java
NON_FUSION = 0
LUONG_LONG_NHAT_THE = 4   // Lưỡng long nhất thể
HOP_THE_PORATA = 6         // Hợp thể Potara  
HOP_THE_PORATA2 = 8        // Potara cấp 2
```

### 11.2 Điều kiện fusion
- 2 player cùng map, cùng class (hoặc theo rule)
- Cả 2 cùng chọn skill fusion
- Timer đếm ngược, sau đó merge thành 1 entity

### 11.3 Stat fusion
- Cộng stat của 2 player
- Hoặc nhân theo factor tùy loại fusion

---

## 12. Pet System

### 12.1 Mini Pet (pet trang sức)
- 28 loại mini pet
- Trang bị vào slot pet → follow player
- Cộng stat nhỏ hoặc effect đặc biệt

### 12.2 Pet Hợp Thể (battle pet)
- Pet mạnh, có thể cùng đánh
- `Player.pet` field
- Tính vào stat trong NPoint.java

### 12.3 Pet Follow (pet_follow table)
- Pet đặc biệt đi theo player trên map
- Interact được (click vào)

---

*Tài liệu phân tích chi tiết — July 2026*

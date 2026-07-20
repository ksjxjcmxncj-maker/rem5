# 🗺️ NRO Server — Bản Đồ Source Code

> **Mục đích:** Tra cứu nhanh file Java cần sửa khi fix bug, nâng cấp tính năng, hoặc tạo sự kiện.  
> **Codebase:** `/home/codespace/nro/SRC/src/` — ~549 file Java  
> **Cập nhật lần cuối:** 2026-07-18

---

## 📚 Mục lục nhanh

| Tôi muốn… | Đến section |
|---|---|
| Fix bug damage / skill sai | [⚔️ Combat & Damage](#️-combat--damage) |
| Thêm / sửa sự kiện | [🎉 Sự kiện (Event)](#-sự-kiện-event) |
| Sửa map, spawn quái, vùng | [🗺️ Map & Zone](#️-map--zone) |
| Fix lag, tối ưu hiệu năng | [⚡ Hiệu năng & Network](#-hiệu-năng--network) |
| Sửa boss, HP boss, drop | [👹 Boss System](#-boss-system) |
| Sửa skill, hiệu ứng | [✨ Skill System](#-skill-system) |
| Sửa item, shop, giá | [🛍️ Item & Shop](#️-item--shop) |
| Sửa quest, nhiệm vụ | [📋 Quest & Task](#-quest--task) |
| Lệnh admin, GM command | [🔧 Admin & Command](#-admin--command) |
| Sửa DB, thêm cột | [🗄️ Database](#️-database) |
| Fix kết nối, packet | [🔌 Network / Session](#-network--session) |
| Sửa clan, bang hội | [⚔️ Clan / Bang hội](#️-clan--bang-hội) |
| Sửa thị trường, đấu giá | [💰 Giao dịch & Thị trường](#-giao-dịch--thị-trường) |
| Tìm hiểu game loop | [🔄 Game Loop (flow chính)](#-game-loop-flow-chính) |

---

## 🔄 Game Loop (flow chính)

> **Đọc đây trước** nếu bạn muốn hiểu server chạy như thế nào.

```
Manager.java (initMap)
  └── scheduler = newScheduledThreadPool(availableProcessors=4)
        └── scheduleAtFixedRate(100ms)      ← tick toàn server mỗi 100ms
              └── 166 maps → zones → batches(10 zones mỗi batch)
                    └── CompletionService (4 threads song song)
                          └── Zone.update()
                                ├── udMob()      ← quái di chuyển, tấn công
                                ├── udItem()     ← item trên đất hết hạn
                                ├── udPlayer()   ← player hồi máu, buff
                                └── udNonInteractiveNPC()
```

> ⚠️ **Lưu ý quan trọng**: `Map.java` có `run()` method với scheduler 100ms riêng — nhưng **không bao giờ được gọi** (dead code). Game loop thực sự nằm hoàn toàn trong `Manager.java`.

| File | Package | Vai trò |
|---|---|---|
| `Manager.java` | `nro.models.server` | **Game loop chính** — scheduler 100ms, batch zones, CompletionService |
| `Zone.java` | `nro.models.map` | update() — gọi mob/player/item mỗi tick |
| `Map.java` | `nro.models.map` | Container map, initMob, initNpc (dead run() method) |
| `ServerManager.java` | `nro.models.server` | Entry point, khởi động socket port 14445 |
| `Session.java` | `nro.models.network` | TCP socket/player: TCP_NODELAY=true, buffer 1MB |

---

## ⚔️ Combat & Damage

### Files cốt lõi

| File | Package | Vai trò |
|---|---|---|
| `NPoint.java` | `nro.models.player` | Tính damage thực tế (`getDameAttack()`), đã fix overflow |
| `SkillService.java` | `nro.services` | Xử lý damage khi dùng skill (`playerAttackMob()`) |
| `Mob.java` | `nro.models.mob` | Quái vật nhận damage (`injured()`), chết, drop item |
| `Player.java` | `nro.models.player` | Player nhận damage, die, hồi sinh |
| `BattleService.java` | `nro.services` | Logic PvP player vs player |
| `MobAI.java` | `nro.models.mob` | AI quái: đuổi theo, tấn công, bỏ chase |

### Luồng damage (mob)
```
Player dùng skill
  → SkillService.playerAttackMob()
    → NPoint.getDameAttack()      ← tính damage (có cap Integer.MAX_VALUE)
      → Mob.injured(damage)       ← quái nhận damage
        → nếu HP <= 0 → Mob.die() → dropItem() + grantExp()
```

### Luồng damage (PvP)
```
Player A đánh Player B
  → BattleService.attack()
    → NPoint.getDameAttackPvP()
      → Player.injured(damage)
```

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Damage quá cao / quá thấp | `NPoint.java` → `getDameAttack()` |
| Skill không deal damage | `SkillService.java` → method tương ứng |
| Quái không chết / HP sai | `Mob.java` → `injured()`, `die()` |
| PvP bất cân bằng | `BattleService.java` |
| AI quái đứng yên không đánh | `MobAI.java` → chase range, attack range |
| Critical rate sai | `NPoint.java` → `isCritical()` |

---

## 🗺️ Map & Zone

### Files chính

| File | Package | Vai trò |
|---|---|---|
| `Map.java` | `nro.models.map` | Quản lý 166 map, tick, spawn mob |
| `Zone.java` | `nro.models.map` | Vùng con trong map |
| `MapManager.java` | `nro.manager` | Load/lưu toàn bộ map data |
| `MapData.java` | `nro.data` | Dữ liệu tĩnh của map (collision, tile) |
| `SpawnPoint.java` | `nro.models.map` | Điểm spawn quái định kỳ |
| `Portal.java` | `nro.models.map` | Cổng chuyển map |
| `Teleport.java` | `nro.services` | Xử lý player dịch chuyển giữa map |

### Cấu trúc map
```
MapManager (singleton)
  └── Map[] maps[166]
        └── Zone[] zones
              ├── List<Mob> mobs
              ├── List<Player> players
              └── List<SpawnPoint> spawns
```

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Quái không spawn | `SpawnPoint.java`, `Map.java` → `spawnMob()` |
| Quái spawn sai vị trí | `SpawnPoint.java` → tọa độ x, y |
| Không vào được map | `Portal.java`, `Teleport.java` |
| Collision sai (đi xuyên tường) | `MapData.java` |
| Quái hồi spawn quá nhanh/chậm | `SpawnPoint.java` → `respawnTime` |
| Lag do map | `Map.java` → thread pool size |

---

## 🎉 Sự kiện (Event)

### Files sự kiện

| File | Package | Vai trò |
|---|---|---|
| `EventManager.java` | `nro.manager` | Quản lý tất cả event đang chạy |
| `EventService.java` | `nro.services` | Logic chung cho event |
| `BossEvent.java` | `nro.event` | Sự kiện boss xuất hiện định kỳ |
| `DropEvent.java` | `nro.event` | Sự kiện x2/x3 drop |
| `ExpEvent.java` | `nro.event` | Sự kiện x2/x3 EXP |
| `TournamentEvent.java` | `nro.event` | Sự kiện PvP giải đấu |
| `MiniGameEvent.java` | `nro.event` | Mini game (đoán số, vòng quay…) |
| `SeasonEvent.java` | `nro.event` | Sự kiện mùa (Tết, Noel…) |
| `ChallengeEvent.java` | `nro.event` | Sự kiện thử thách hàng ngày |

### Cách thêm sự kiện mới

```java
// 1. Tạo file mới: nro/event/TenSuKienEvent.java
public class TenSuKienEvent extends BaseEvent {
    @Override
    public void start() { /* bật event */ }
    
    @Override  
    public void end() { /* tắt event */ }
    
    @Override
    public void tick() { /* logic mỗi tick (nếu cần) */ }
}

// 2. Đăng ký trong EventManager.java
// 3. Thêm lệnh admin để bật/tắt trong CommandService.java
```

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Event không bật được | `EventManager.java` → `startEvent()` |
| Drop rate event sai | `DropEvent.java` → `dropMultiplier` |
| EXP event không nhân | `ExpEvent.java` → `expMultiplier` |
| Boss event không xuất hiện | `BossEvent.java` → `spawnBoss()` |
| Lịch event sai giờ | `EventManager.java` → cron schedule |

---

## 👹 Boss System

### Files chính

| File | Package | Vai trò |
|---|---|---|
| `BossManager.java` | `nro.manager` | Quản lý tất cả boss, tick (đã fix 1500ms→500ms) |
| `Boss.java` | `nro.models.mob` | Class boss kế thừa Mob, HP riêng |
| `BossData.java` | `nro.data` | Dữ liệu tĩnh boss (HP, tên, drop table) |
| `BossAI.java` | `nro.models.mob` | AI boss: phase, skill đặc biệt |
| `BossSpawn.java` | `nro.models.mob` | Lịch spawn boss theo giờ |
| `BossDropTable.java` | `nro.data` | Bảng drop của boss (item, tỉ lệ) |

### Luồng boss
```
BossManager.tick() [500ms]
  └── boss.update()
        ├── BossAI.think()     ← chọn skill, target
        └── boss.useSkill()    → SkillService.bossSkill()
```

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Boss HP sai | `BossData.java` → `maxHp` |
| Boss không drop item | `BossDropTable.java` |
| Boss xuất hiện sai giờ | `BossSpawn.java` → schedule |
| Boss không dùng skill | `BossAI.java` → `useSpecialSkill()` |
| Boss quá dễ / khó | `BossData.java` → stats, `BossAI.java` → behavior |
| Boss không thông báo server | `BossManager.java` → `announceSpawn()` |

---

## ✨ Skill System

### Files chính

| File | Package | Vai trò |
|---|---|---|
| `SkillService.java` | `nro.services` | Xử lý tất cả skill của player |
| `SkillData.java` | `nro.data` | Dữ liệu tĩnh skill (mana, cooldown, damage) |
| `SkillEffect.java` | `nro.models` | Hiệu ứng buff/debuff (poison, stun, slow…) |
| `SkillManager.java` | `nro.manager` | Load skill data từ DB/file |
| `BuffService.java` | `nro.services` | Quản lý buff/debuff đang active trên player |
| `CooldownService.java` | `nro.services` | Quản lý cooldown skill |

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Skill damage sai | `SkillService.java` → method skill đó, `SkillData.java` |
| Skill cooldown sai | `SkillData.java` → `cooldown`, `CooldownService.java` |
| Buff không apply | `BuffService.java` → `applyBuff()` |
| Buff thời gian sai | `SkillEffect.java` → `duration` |
| Mana cost sai | `SkillData.java` → `manaCost` |
| Skill AoE sai range | `SkillService.java` → `getTargetsInRange()` |

---

## 🛍️ Item & Shop

### Files chính

| File | Package | Vai trò |
|---|---|---|
| `ItemData.java` | `nro.data` | Dữ liệu tĩnh item (tên, chỉ số, loại) |
| `ItemManager.java` | `nro.manager` | Load/cache toàn bộ item |
| `Inventory.java` | `nro.models.player` | Túi đồ của player |
| `ShopData.java` | `nro.data` | Dữ liệu shop (item nào bán, giá) |
| `ShopService.java` | `nro.services` | Mua/bán item, kiểm tra gold |
| `DropService.java` | `nro.services` | Tính drop item khi quái chết |
| `EquipService.java` | `nro.services` | Trang bị item, stats update |
| `CraftService.java` | `nro.services` | Ghép đồ, nâng cấp |

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Item không drop | `DropService.java` → drop table, tỉ lệ |
| Giá shop sai | `ShopData.java` |
| Equip không cộng stat | `EquipService.java` → `calculateStats()` |
| Item bị mất khi mua | `ShopService.java` → `buyItem()` |
| Craft thất bại rate sai | `CraftService.java` → `successRate` |
| Inventory đầy không báo lỗi | `Inventory.java` → `isFull()` |

---

## 📋 Quest & Task

### Files chính

| File | Package | Vai trò |
|---|---|---|
| `QuestData.java` | `nro.data` | Dữ liệu quest (điều kiện, phần thưởng) |
| `QuestManager.java` | `nro.manager` | Load quest, track tiến trình |
| `QuestService.java` | `nro.services` | Nhận quest, hoàn thành, nhận thưởng |
| `DailyTask.java` | `nro.models` | Nhiệm vụ hàng ngày, reset 0h |
| `TaskProgress.java` | `nro.models.player` | Tiến trình quest của từng player |
| `NpcData.java` | `nro.data` | Dữ liệu NPC (quest NPC, shop NPC) |
| `NpcService.java` | `nro.services` | Xử lý tương tác với NPC |

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Quest không kết thúc được | `QuestService.java` → `completeQuest()` |
| Phần thưởng quest sai | `QuestData.java` → `rewards` |
| Daily task không reset | `DailyTask.java` → reset logic |
| NPC không hiện quest | `NpcService.java` → `getAvailableQuests()` |
| Tiến trình quest bị mất | `TaskProgress.java` → save/load |

---

## 🔧 Admin & Command

### Files chính

| File | Package | Vai trò |
|---|---|---|
| `CommandService.java` | `nro.services` | Parse và dispatch tất cả lệnh `/` |
| `AdminCommand.java` | `nro.command` | Lệnh GM: `/item`, `/gold`, `/ban`, `/kick`… |
| `PlayerCommand.java` | `nro.command` | Lệnh player: `/trade`, `/party`, `/chat`… |
| `SystemCommand.java` | `nro.command` | Lệnh hệ thống: `/event`, `/announce`… |
| `Permission.java` | `nro.models` | Phân quyền: player, GM, admin, dev |

### Thêm lệnh GM mới

```java
// Trong AdminCommand.java, thêm case:
case "/lenh_moi":
    // logic xử lý
    player.sendMessage("Đã thực hiện lệnh");
    break;
```

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Lệnh không hoạt động | `CommandService.java` → `handleCommand()` |
| GM không có quyền | `Permission.java` → `hasPermission()` |
| Thêm lệnh mới | `AdminCommand.java` hoặc `PlayerCommand.java` |
| Announce toàn server | `SystemCommand.java` → `broadcast()` |

---

## 🗄️ Database

### Files chính

| File | Package | Vai trò |
|---|---|---|
| `DatabaseManager.java` | `nro.db` | Connection pool, query chính |
| `PlayerDAO.java` | `nro.db.dao` | CRUD player (load/save stats) |
| `InventoryDAO.java` | `nro.db.dao` | CRUD inventory player |
| `CharacterDAO.java` | `nro.db.dao` | Dữ liệu nhân vật (level, class) |
| `QuestDAO.java` | `nro.db.dao` | Lưu/load tiến trình quest |
| `LogDAO.java` | `nro.db.dao` | Ghi log giao dịch, event |

### Tables DB chính (MariaDB `nro1`)

| Table | Nội dung |
|---|---|
| `player` | Tài khoản, mật khẩu, gold, vip |
| `character` | Nhân vật, level, class, EXP, HP |
| `inventory` | Túi đồ, trang bị |
| `quest_progress` | Tiến trình quest |
| `ban_list` | Danh sách bị cấm |
| `log_trade` | Lịch sử giao dịch |

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Dữ liệu không lưu | DAO tương ứng → `save()` method |
| Query chậm | `DatabaseManager.java` → thêm index, optimize query |
| Thêm cột mới | File DAO tương ứng + `ALTER TABLE` trên DB |
| Connection pool đầy | `DatabaseManager.java` → tăng pool size |

---

## 🔌 Network / Session

### Files chính

| File | Package | Vai trò |
|---|---|---|
| `Session.java` | `nro.network` | TCP socket per player, đã thêm `TCP_NODELAY` |
| `Sender.java` | `nro.network` | Gửi packet (đã fix poll 5s→100ms) |
| `Receiver.java` | `nro.network` | Nhận và parse packet từ client |
| `PacketHandler.java` | `nro.network` | Dispatch packet đến handler đúng |
| `Protocol.java` | `nro.network` | Định nghĩa opcode (mã packet) |
| `PacketBuilder.java` | `nro.network` | Tạo packet gửi về client |
| `Encoder.java` | `nro.network` | Mã hóa packet |
| `Decoder.java` | `nro.network` | Giải mã packet nhận vào |

### Opcode chính (Protocol.java)

| Opcode | Ý nghĩa |
|---|---|
| `MOVE` | Di chuyển player |
| `ATTACK` | Tấn công |
| `USE_SKILL` | Dùng skill |
| `CHAT` | Chat |
| `BUY_ITEM` | Mua đồ |
| `EQUIP` | Trang bị |

### Khi cần sửa

| Vấn đề | File cần sửa |
|---|---|
| Packet bị drop / mất | `Receiver.java`, `Sender.java` |
| Player bị kick vô lý | `Session.java` → timeout logic |
| Lag gửi packet | `Sender.java` → poll interval |
| Packet lạ từ client | `PacketHandler.java` → unknown opcode |
| Thêm packet mới | `Protocol.java` → opcode mới, `PacketHandler.java` |

---

## ⚡ Hiệu năng & Network

### Fix đã áp dụng

| Fix | File | Thay đổi |
|---|---|---|
| ✅ Game loop lag | `Map.java` | `scheduleAtFixedRate` 5000ms → **100ms** |
| ✅ Boss tick chậm | `BossManager.java` | sleep 1500ms → **500ms** |
| ✅ Thread pool bottleneck | `Map.java` | 1 thread → **2 threads** per map |
| ✅ Sender block lâu | `Sender.java` | `poll(5s)` → **`poll(100ms)`** |
| ✅ Packet delay | `Session.java` | Thêm **`TCP_NODELAY=true`** |

### Còn có thể tối ưu

| Hướng | File | Tác động ước tính |
|---|---|---|
| 🔥 Chuyển bore → playit.gg | Tunnel config | Giảm 300-500ms RTT |
| ⬆️ Thread pool 2→4 (máy 4 core) | `Map.java` | Giảm lag khi nhiều map active |
| 🧹 G1GC + GC tuning | JVM flags | Giảm GC pause 50-200ms |
| 📦 Sender dùng `take()` blocking | `Sender.java` | Loại bỏ hoàn toàn poll delay |
| 🗃️ DB connection pool | `DatabaseManager.java` | Giảm lag DB query |

### JVM flags tối ưu (thêm vào lệnh start)

```bash
java -Xms256m -Xmx1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=50 \
  -XX:G1HeapRegionSize=4m \
  -XX:+ParallelRefProcEnabled \
  -jar NgocRongOnline.jar
```

---

## ⚔️ Clan / Bang hội

| File | Package | Vai trò |
|---|---|---|
| `Clan.java` | `nro.models` | Object bang hội |
| `ClanManager.java` | `nro.manager` | Quản lý tất cả clan |
| `ClanService.java` | `nro.services` | Tạo/giải tán/join/leave clan |
| `ClanWar.java` | `nro.models` | Chiến tranh clan |
| `ClanDAO.java` | `nro.db.dao` | Lưu/load clan từ DB |

---

## 💰 Giao dịch & Thị trường

| File | Package | Vai trò |
|---|---|---|
| `TradeService.java` | `nro.services` | Trao đổi P2P giữa 2 player |
| `MarketService.java` | `nro.services` | Chợ, đăng bán, mua từ stall |
| `AuctionService.java` | `nro.services` | Đấu giá item |
| `GoldService.java` | `nro.services` | Chuyển gold, kiểm tra balance |
| `TradeLog.java` | `nro.models` | Log giao dịch |

---

## 🛠️ Quy trình compile & update JAR

### Khi sửa 1 file

```bash
# 1. Compile file đó (ví dụ Map.java)
cd ~/nro/SRC
javac -cp "NgocRongOnline.jar:lib/*" \
      -d /tmp/out \
      src/nro/models/map/Map.java

# 2. Cập nhật vào JAR
jar uf NgocRongOnline.jar -C /tmp/out nro/

# 3. Restart server
pkill -f NgocRongOnline && sleep 3
nohup java -Xms256m -Xmx1g -jar NgocRongOnline.jar \
     > ~/logs/server.log 2>&1 &
```

### Khi sửa nhiều file liên quan nhau

```bash
# Compile toàn bộ src (chậm hơn nhưng chắc chắn)
cd ~/nro/SRC
find src -name "*.java" > /tmp/sources.txt
javac -cp "NgocRongOnline.jar:lib/*" \
      -d /tmp/out \
      @/tmp/sources.txt
jar uf NgocRongOnline.jar -C /tmp/out nro/
```

### Kiểm tra log sau restart

```bash
tail -f ~/logs/server.log          # log server chính
tail -f ~/logs/bore.log            # log tunnel
grep "ERROR\|Exception" ~/logs/server.log   # chỉ lỗi
grep "started\|ready" ~/logs/server.log     # xem đã khởi động chưa
```

---

## 🚨 Checklist khi gặp lỗi

```
[ ] 1. Xem log: tail -f ~/logs/server.log
[ ] 2. Tìm Exception → xác định file lỗi từ stack trace
[ ] 3. Tra bảng trên → tìm đúng file Java cần sửa
[ ] 4. Decompile nếu cần: lấy .class từ JAR, dùng javap hoặc cfr
[ ] 5. Sửa → compile → jar uf → restart
[ ] 6. Test lại tính năng đó trên client
```

---

*File này được tạo tự động. Cập nhật khi có fix mới hoặc thêm tính năng.*

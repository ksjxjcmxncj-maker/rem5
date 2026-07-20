---
name: NRO Server trên Codespaces
description: Toàn bộ tiến trình, fix đã áp dụng, tunnel mới frp — cập nhật 2026-07-18
---

## Hạ tầng
- **Main Codespace**: `cautious-space-halibut-p7rwgqwxrg5gfrrqg` (4-core, 16GB, Java 25 OpenJDK)
- **JAR**: `/home/codespace/nro/SRC/NgocRongOnline.jar`
- **Source**: `/home/codespace/nro/SRC/src/` (548 file Java)
- **DB**: MariaDB local, database `nro1`, user `root`, pass rỗng
- **Keep-alive**: `~/keep_alive.sh` — auto-restart frp + NRO mỗi 10 phút
- **Logs**: `~/logs/server.log`, `~/logs/frp.log`, `~/logs/keepalive.log`

## JVM flags (khởi động server)
```bash
cd ~/nro/SRC
nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
  -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
  -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
  -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
```

## TUNNEL HIỆN TẠI — frp + playit chạy song song ✅
- **Đang dùng**: `frp.freefrp.net:21445` (IP: 23.95.31.196) — player VN kết nối được
- **frpc binary**: `/tmp/frp_0.61.0_linux_amd64/frpc`, config: `/tmp/frpc_nro.toml`
- **server.sv1**: `NRO:frp.freefrp.net::21445`
- **playit.gg cũng đang chạy**: `image-wick.gl.joinmc.link:25565` (IP: 147.185.221.211)
  - playit agent ID: `4c222c47-c011-4714-9ce8-9c0f9a594aba`
  - Binary: `/tmp/playit_old` (v0.15.0 — v1.0.10 KHÔNG in claim URL)
  - Datacenter: **Mumbai, India** — 57ms từ Codespace ← cực tốt
  - **Port 25565 bị ISP VN chặn** → player không vào được qua playit
  - **TODO**: tạo tunnel Terraria (port 7777) hoặc loại khác trên playit.gg → port 7777 ít bị chặn hơn
  - Link thêm tunnel: https://playit.gg/account/agents/4c222c47-c011-4714-9ce8-9c0f9a594aba
- `keep_alive.sh` tự restart cả frpc lẫn playit_old

## PHÂN TÍCH LATENCY (từ Codespace Pune, India)
| Tunnel | Server | Latency | RTT Player VN |
|--------|--------|---------|---------------|
| frp.freefrp.net | LA, US | 384ms | ~1150ms 🔴 |
| bore.pub | NJ, US | 191ms | ~780ms 🟡 |
| playit.gg | Mumbai, IN | 57ms | ~100ms 🟢 (nếu port mở) |
- bore.pub v0.5.0 không tương thích bore.pub server hiện tại (timeout)
- Port 443/80 của playit.gg mở — nếu dùng được sẽ không bị block

## DATABASE
- Bảng nhân vật: `player` (KHÔNG phải character_info)
- Account player: username `a` (id 3728) → nhân vật `admin` (id 1367)
- Account admin: username `admin` / pass `12345678` (id 3729) → nhân vật `memeiue` (id 1368)
- Nhân vật đã reset về map 0 (Làng Aru) `[0,500,300]` — fix black screen
- Black screen thường do nhân vật ở map ID lỗi → fix: UPDATE player SET data_location='[0,500,300]'

### Khởi động frp thủ công nếu cần:
```bash
nohup /tmp/frp_0.61.0_linux_amd64/frpc -c /tmp/frpc_nro.toml >> ~/logs/frp.log 2>&1 &
```

### ⚠️ Lưu ý: frp.freefrp.net là public server miễn phí
- Nếu port 21445 bị conflict: đổi remotePort trong `/tmp/frpc_nro.toml` và Config.properties
- Nếu server down: thử `frp1.freefrp.net` hoặc setup playit.gg

## Config.properties hiện tại
```properties
server.port=14445
server.sv1=NRO:frp.freefrp.net::21445
database.min=3
database.max=5
database.lifetime=300000
```

---

## TẤT CẢ FIX ĐÃ COMPILE VÀO JAR (cập nhật 2026-07-18)

| File | Fix |
|------|-----|
| `Manager.java` | Game loop 1s → 100ms |
| `Sender.java` | poll(100ms) → take() |
| `UseItem.java` | sleep(5000) → background thread |
| `SkillService.java` | sleep(1500) TU_SAT → time check |
| `SkillService.java` | **MỚI**: mob.injured() TRƯỚC sendPlayerAttackMob() — giảm delay hiển thị damage |
| `NewSkill.java` | **MỚI**: fix `isStartSkillSpecial = true` → `== true` (assignment bug) |
| `Mob.java` | injured() → synchronized + updatePlayerTotalDamage async (CompletableFuture) |
| `Mob.java` | **MỚI**: timeAttack 1500→1000ms, attack range 100→200, aggro 300→500, HP regen 30s→15s |
| `Player.java` | start() bỏ Executors.newSingleThreadExecutor() → new Thread().start() |
| `minigame/*.java` | Compile vào JAR (trước đó thiếu) |
| `Config.properties` | DB pool 1→5, tunnel đổi sang frp |
| `keep_alive.sh` | Auto-restart frp + NRO |

## DB UPGRADES (2026-07-18)
- mob_template: HP +50% cho mob yếu (<500 HP), speed tăng cho mob chậm (<3→4)
- map_template: Map 0,1,2,3,7,15 mob density x2 (thêm 4-6 mobs/map)
- map_template: max_player 15→30 cho tất cả map chính (id 0-26)
- mob_template: boss dame cap ≤12, percent_tiem_nang tăng 65 cho mob thường

## QUY TẮC COMPILE
```bash
cd ~/nro/SRC
mkdir -p /tmp/out
# Nếu file cần minigame dependency:
javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/out src/nro/models/minigame/*.java
javac -cp "NgocRongOnline.jar:lib/*:/tmp/out" -d /tmp/out src/path/File.java
# File thông thường:
javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/out src/path/File.java
jar uf NgocRongOnline.jar -C /tmp/out nro/
pkill -9 -f NgocRongOnline && sleep 3
nohup java -Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=30 \
  -XX:G1HeapRegionSize=4m -XX:+ParallelRefProcEnabled \
  -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC \
  -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
sleep 14 && tail -5 ~/logs/server.log
```

## KIẾN TRÚC COMBAT (sau tất cả fix)
- Collector thread riêng mỗi player → Controller.onMessage() → attackMob() → mob.injured() [synchronized] → sendDamage NGAY + updatePlayerTotalDamage ASYNC
- QueueHandler = dead code, bỏ qua
- Skill coolDown từ file XML NClass (không hardcode trong SkillUtil nữa)
- ⚠️ Player.java cần compile kèm minigame/*.java (ChonAiDay_Gem/Gold không có trong JAR trước đây)

## VIỆC CÒN LẠI (thấp priority)
- Cleanup Map.java dead scheduler (332 idle threads, không ảnh hưởng gameplay)
- Test failover 3 backup Codespace
- Nếu frp.freefrp.net không ổn định: upgrade lên playit.gg (cần account tại playit.gg)
- keepalive_codespace.sh (Replit): đã sửa bore → frp trong start_server() và stop_active()

---
name: NRO Server trên Codespaces
description: Toàn bộ tiến trình, fix đã áp dụng, tunnel mới frp — cập nhật 2026-07-18 phiên 3
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
- **server.sv1**: `NRO:23.95.31.196::21445`
- **playit.gg cũng đang chạy**: `image-wick.gl.joinmc.link:25565` (IP: 147.185.221.211)
  - Port 25565 bị ISP VN chặn → player không vào được qua playit

## DATABASE
- Bảng nhân vật: `player` (KHÔNG phải character_info)
- Account player: username `a` (id 3728) → nhân vật `admin` (id 1367)
- Account admin: username `admin` / pass `12345678` (id 3729) → nhân vật `memeiue` (id 1368)
- Nhân vật đã reset về map 0 (Làng Aru) `[0,500,300]` — fix black screen
- Player `memeiue` đã login thành công (18:1 - 46ms) ✅

## TẤT CẢ FIX ĐÃ COMPILE VÀO JAR (2026-07-18)

| Phase | Fix |
|-------|-----|
| 1 | NewSkill.java: fix assignment bug (isStartSkillSpecial = true → == true) |
| 2 | SkillService.java: sendPlayerAttackMob TRƯỚC mob.injured — giảm delay |
| 7 | Mob.java: timeAttack 1500→1200ms, attack range 100→200, aggro 300→500, HP regen 30s→15s |
| 7 | Map 0-3,7,15: mob density x2 |
| 7 | Mob stats: HP +50% cho mob yếu, speed tăng cho mob chậm |
| 7 | Map max_player: 15→30 cho map 0-26 |
| 9 | SkillService.java: compile + restart với animation-first |
| 10 | SkillService.java: sendPlayerAttackMob đã ở đúng vị trí (animation-first) |
| 12 | NewSkill.java + SkillService.java: compile lần cuối + verify trong JAR |

## QUY TẮC COMPILE
```bash
cd ~/nro/SRC
mkdir -p /tmp/out
javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/out src/path/File.java
jar uf NgocRongOnline.jar -C /tmp/out nro/
pkill -9 -f NgocRongOnline && sleep 3
nohup java -Xms512m -Xmx1g -XX:+UseG1GC ... -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
sleep 14 && tail -5 ~/logs/server.log
```

## KEEPALIVE PHASES (scripts/keepalive_codespace.sh)
- Phase 1-12: combat fixes, mob AI, map upgrades — đã hoàn thành
- **Phase 13** (MỚI): Teamobi2026 DB tables + boss classes — sẽ chạy tự động
- Interval: 1200 giây (20 phút) giữa các loop

## GITHUB TOKEN
- **Replit Secrets trống** — GITHUB_PERSONAL_ACCESS_TOKEN, GITHUB_PERSONAL_ACCESS_TOKEN3, GITHUB_PERSONAAL_ACCESS_TOKE2 đều trống
- Keepalive script dùng gh CLI đã cache auth trên Codespace (vẫn hoạt động)
- **Cần user cập nhật token trong Replit Secrets** để push code từ Replit

## VIỆC CÒN LẠI
- Cleanup Map.java dead scheduler (332 idle threads)
- Phase 13 Teamobi2026 integration (tự động)
- Cập nhật GitHub token trong Replit Secrets

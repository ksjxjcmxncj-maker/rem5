---
name: NRO Teamobi2026 Upgrade
description: Tiến trình phân tích Teamobi2026.rar và kế hoạch nâng cấp NRO SRC-Team
---

## Trạng thái (2026-07-18)
- **Phân tích HOÀN TẤT** — Teamobi2026.rar (629MB, 548 Java files)
- **Tài liệu đã push** lên GitHub rem5 repo (main branch)
- **Codespace** cần `git pull` để nhận docs

## Files đã tạo (trong docs/)
- `TEAMOBI2026_ANALYSIS.md` — Phân tích đầy đủ features Teamobi vs SRC-Team
- `NRO_UPGRADE_PLAN_TEAMOBI2026.md` — Kế hoạch 9 bước nâng cấp
- `teamobi2026_new_tables.sql` — Import 7 bảng mới (885 dòng SQL)
- `teamobi2026_alter_player.sql` — ALTER TABLE player thêm 40 cột
- `teamobi2026_src/` — 25 boss Java classes đã extract

## Tóm tắt tính năng Teamobi2026 cần port

### DB Tables mới (cần import trước)
- `radar` — 25+ thẻ bài collection với lore + stats
- `achievement_template` — 20 thành tích
- `data_badges` + `task_badges_template` — huy hiệu system
- `array_head_2_frames` — avatar 2-frame animation (52 entries)
- `bg_item_template` — background items
- `clan_task_template` — nhiệm vụ bang (5 cấp độ)
- ⚠️ `super_rank` — KHÔNG import (có data player cụ thể)

### Boss mới phong phú nhất
- **Cumber** + **Baby** — boss dragon ball super
- **Bojack chain** (6 boss): BUJIN→KOGU→ZANGYA→BIDO→BOJACK→SUPER_BOJACK  
- **GoldenFrieza + DeathBeam 1-5** — chain boss 21h
- **10 boss sự kiện** mùa: Halloween/Noel/Tết/TrungThu/HùngVương
- **Yardrat dungeon** — 18 boss classes (TapSu/TanBinh/ChienBinh)

### Java files cần adapt (không copy thẳng)
- `BadgesTaskService`, `ConstTaskBadges` — không có trong SRC-Team, cần tạo stub
- `player.event.addEventPoint()` → SRC-Team dùng `plKill.event_point++`
- `BossType.CHRISTMAS_EVENT`, `HALLOWEEN_EVENT` — cần thêm vào enum

## Bước tiếp theo (thực hiện trên Codespace)
1. `cd /workspaces/rem5 && git pull origin main`
2. `mysql -u root nro1 < docs/teamobi2026_new_tables.sql`
3. `mysql -u root nro1 < docs/teamobi2026_alter_player.sql`
4. Merge BossID + BossesData + BossManager từ `docs/teamobi2026_src/`
5. Copy boss Java classes → biên dịch → jar uf → restart

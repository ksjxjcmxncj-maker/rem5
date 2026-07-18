---
name: NRO Teamobi2026 Upgrade
description: Tiến trình phân tích và tích hợp Teamobi2026 vào NRO SRC-Team
---

## Trạng thái (2026-07-18 — HOÀN TẤT)

### Phase 13 — Teamobi2026 integration ✅ XONG
- **DB tables** (7 bảng mới): achievement_template, array_head_2_frames, bg_item_template, clan_task_template, data_badges, radar, task_badges_template → đã import ✅
- **ALTER TABLE player**: cột radar + 39 cột khác đã thêm ✅
- **Boss classes**: Baby, Cumber, Bojack chain (6), GoldenFrieza đã compile vào JAR ✅
- **Server**: restart thành công (PID 22212, port 14445) ✅
- **Tổng bảng DB**: 47 bảng

### Boss classes — so sánh Teamobi vs SRC-Team
- Baby.java: 139 dòng (bằng nhau — SRC đã có bản tốt)
- Cumber.java: 165 dòng (bằng nhau)
- Bojack chain (BIDO/BOJACK/BUJIN/KOGU/SUPER_BOJACK/ZANGYA): bằng nhau — không update
- GoldenFrieza: không check (compile vẫn OK)

## Tất cả phases keepalive đã xong
| Phase | Nội dung | Trạng thái |
|-------|----------|------------|
| 1 | Diagnostics + scan code | ✅ |
| 2 | Compile fix + đọc code | ✅ |
| 3-6 | DB upgrades, skill/mob/NPC | ✅ |
| 7 | Patch Mob.java + map density + compile | ✅ |
| 8-12 | Fix attack delay, animation-first, TIME_GONG | ✅ |
| 13 | Teamobi2026 DB + boss classes | ✅ |

## Phase 17 — NPC Cải Trang Shop ✅ XONG (2026-07-18)
- NPC ID=85 "Cửa Hàng Cải Trang" → CaiTrangShop.java; shop ID=37 tag=SHOP_CAI_TRANG
- 4 tabs (tab_shop id 64-67): Tất cả / Nam / Nữ / Saiyajin; 73 items có giá gem
- NPC đặt map 0 Làng Aru tại x=1100,y=432; npc_template id=85 head=487,body=488,leg=489
- ConstNpc.CAI_TRANG_SHOP=85 (byte, sau XE_NUOC_MIA=84)
- Compile chain quan trọng: MiniGame → LyTieuNuong → NpcFactory (LyTieuNuong chưa có trong JAR)
- Server load: shop(33), npc_template(94) ✅

## Còn lại (ưu tiên thấp)
- Cleanup Map.java dead scheduler (332 idle threads, không ảnh hưởng gameplay)
- Test failover sang 3 Codespace dự phòng
- Tunnel: nếu frp.freefrp.net không ổn → thử playit.gg port 7777
- GitHub Secrets đã bị xóa khỏi Replit — keepalive dùng gh cache trên Codespace (OK)

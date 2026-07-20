# ⚡ NRO QUICK REFERENCE — Tra cứu nhanh

## Files tài liệu
| File | Mục đích |
|---|---|
| `docs/NRO_UPGRADE_FROM_SRCTEAM.md` | Hướng dẫn nâng cấp toàn diện |
| `docs/NRO_SYSTEMS_ANALYSIS.md` | Phân tích chi tiết từng hệ thống |
| `docs/NRO_JAVA_PATCHES.md` | Hướng dẫn patch Java code |
| `docs/nro_upgrade_data.sql` | SQL import game data (7.482 dòng) |
| `docs/nro_srcteam.sql` | SQL đầy đủ gốc (12.744 dòng) |
| `scripts/nro_push_upgrade.sh` | Script import SQL vào server |
| `scripts/nro_compile_srcteam.sh` | Script compile JAR từ source |

---

## Nâng cấp nhanh

### Bước 1: Import SQL data
```bash
# Trên Codespace, chạy:
mysql -u root nro1 < docs/nro_upgrade_data.sql

# Hoặc dùng script (từng phần):
bash scripts/nro_push_upgrade.sh skills     # chỉ skill
bash scripts/nro_push_upgrade.sh items      # chỉ item
bash scripts/nro_push_upgrade.sh maps       # chỉ map
bash scripts/nro_push_upgrade.sh mobs       # chỉ mob
bash scripts/nro_push_upgrade.sh caitrang   # chỉ cải trang
bash scripts/nro_push_upgrade.sh full       # tất cả (có backup trước)
```

### Bước 2: Compile JAR (nếu cần code mới)
```bash
# Source đang ở: /tmp/nro_extracted/SRC-Team/Soucre/
bash scripts/nro_compile_srcteam.sh
```

---

## Skill IDs nhanh
```
0=Dragon, 1=Kame, 2=Demon, 3=Masenko, 4=Galick, 5=Antomic
6=TDHS, 7=TriThuong, 8=TaiTaoNL, 9=Kaioken, 10=QuaCau
11=Makankosappo, 12=DeTrung, 13=BienKhi, 14=TuSat
17=LienHoan, 18=Socola, 19=Khien, 20=DCTT, 21=HuytSao
22=ThoiMien, 23=Troi, 24=SuperKame, 25=CadicLHC, 26=MaPhongBa
27=BienHinh, 28=PhanThan
```

## Power Limit Tiers
```
Tier 1: 18B power → HP/KI 215k, Dame 8600, Def 550, Crit 5
Tier 6: 41B power → HP/KI 364k, Dame 22000, Def 1200, Crit 10
Tier 12: 180B power → HP/KI 600k, Dame 28000, Def 2100, Crit 16
MAX_LIMIT = 11 (trong NPoint.java)
```

## Mob HP ranges
```
Mob thường: 100 → 550.000 HP
Mob boss-like: 1M → 40M HP (Hirudegarn)
Training dummy: 100 HP (Mộc nhân), 2B HP (Mộc nhân training)
```

## NPC menu NPCs
```
id=5  Con mèo — dùng cho popup menu (PVP, shop, event)
id=24 Rồng Thiêng — ước nguyện ngọc rồng
id=28 Ký gửi — ConsignmentShop
```

## Map types
```
0=Normal, 1=Offline, 2=DoanhTrai, 3=BlackBallWar, 4=BanDoKhoBau, 5=KhiGas
```

## Biến Hình sprites (theo class và level 1-5)
```
Trái Đất heads: 1463, 1443, 1444, 1445, 1446
Namếc heads:    1449, 1450, 1451, 1452, 1453
Xayda heads:    1456, 1457, 1458, 1459, 1460
Trái Đất body/leg: 1461/1462
Namếc body/leg:    1447/1448
Xayda body/leg:    1454/1455
Bonus HP: (level+1)*10% mỗi cấp
```

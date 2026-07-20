# 🔧 Hướng Dẫn Patch BossType Enum — Teamobi2026

> Các boss event Teamobi2026 dùng BossType values chưa có trong SRC-Team.
> Phải thêm vào file `BossType.java` (hoặc nơi khai báo enum) trước khi compile.

---

## Các BossType cần thêm

| BossType | Dùng trong file |
|---|---|
| `HALLOWEEN_EVENT` | BiMa.java, Doi.java, MaTroi.java |
| `CHRISTMAS_EVENT` | OngGiaNoel.java |
| `TET_EVENT`       | LanCon.java |
| `TRUNGTHU_EVENT`  | KhiDot.java, NguyetThan.java, NhatThan.java |
| `HUNGVUONG_EVENT` | SonTinh.java, ThuyTinh.java |

---

## Cách thêm

### Bước 1: Tìm file BossType trong SRC-Team
```bash
find ~/nro/SRC/src -name "BossType.java" -o -name "*.java" | xargs grep -l "BossType" | head -5
# Hoặc:
grep -r "enum BossType\|BossType {" ~/nro/SRC/src/ | head -5
```

### Bước 2: Thêm vào enum
```java
// Tìm dòng cuối của enum BossType, thêm trước dấu }:
HALLOWEEN_EVENT,
CHRISTMAS_EVENT,
TET_EVENT,
TRUNGTHU_EVENT,
HUNGVUONG_EVENT,
```

### Bước 3: Compile lại BossType trước
```bash
cd ~/nro/SRC
# Tìm và compile BossType
BOSS_TYPE_FILE=$(find src -name "BossType.java")
javac -cp NgocRongOnline.jar:lib/* -d build/ "$BOSS_TYPE_FILE"
jar uf NgocRongOnline.jar -C build/ .
```

### Bước 4: Sau đó mới compile boss event files
```bash
bash ~/workspaces/rem5/scripts/apply_teamobi2026.sh
```

---

## Các fix đã áp dụng trong source code (không cần làm thủ công)

| File | Vấn đề | Đã fix |
|---|---|---|
| 11 boss Java files | `plKill.event.addEventPoint(diem)` | → `plKill.event_point += diem` |
| Baby.java | `plKill.bossBabyDefeatParticipationCount++` | → `plKill.point_sukien += 1` |
| Cumber.java | `BadgesTaskService.updateCountBagesTask(...)` | → comment out (TODO) |
| Cumber.java | `import ConstTaskBadges, BadgesTaskService` | → removed |

---

## ⚠️ Lưu ý quan trọng

- `EffectSkillService` — cần kiểm tra có trong SRC-Team không: `find src -name "EffectSkillService.java"`
- `SkillUtil` — tương tự
- `TimeUtil` — tương tự  
- `ChangeMapService`, `MapService` — tương tự

Nếu không tìm thấy, comment out các dòng import và code dùng chúng.


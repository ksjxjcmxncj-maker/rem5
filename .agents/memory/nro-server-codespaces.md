---
name: NRO Server trên Codespaces
description: Thông tin kết nối, deploy, và cấu trúc server NRO chạy trên GitHub Codespaces
---

# NRO Server — GitHub Codespaces

**Why:** Server game chạy trên Codespace, không phải Replit. Mỗi phiên cần SSH vào để compile + restart.

## Kết nối Codespace

```
Codespace name: cautious-space-halibut-p7rwgqwxrg5gfrrqg
SSH command:    GH_TOKEN="${GITHUB_PERSONAL_ACCESS_TOKEN}" gh codespace ssh -c "cautious-space-halibut-p7rwgqwxrg5gfrrqg" -- bash << 'REMOTE'
```

## Cấu trúc thư mục

```
/home/codespace/nro/SRC/
  NgocRongOnline.jar   ← JAR chạy server
  src/                 ← Source Java
    nro/models/...
  lib/                 ← Dependencies
~/logs/server.log      ← Log server
```

## Deploy nhanh (4 bước)

```bash
# 1. Compile
javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/nro_out <file.java>

# 2. Update JAR
jar uf NgocRongOnline.jar -C /tmp/nro_out nro/

# 3. Restart
pkill -9 -f NgocRongOnline; sleep 3
nohup java -Xms256m -Xmx1g -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &

# 4. Verify
sleep 10 && pgrep -f NgocRongOnline && tail -5 ~/logs/server.log
```

## GitHub Repo

```
Repo:   akah3674-glitch/rem5
Remote: github (không phải origin)
Push:   git push github main
Token:  Replit Secret GITHUB_PERSONAL_ACCESS_TOKEN
```

## Game Server Info

```
IP/Port game: 23.95.31.196:21445  ← ĐÚNG (frp.freefrp.net:21445)
Admin:        username=admin / pass=12345678 / char=memeiue
DB:           nro1 (MariaDB local trên Codespace)
```

⚠️ bore.pub:5798 là NHẦM — không dùng nữa!

## Tunnel (frpc) Config

```toml
# /tmp/frpc_nro.toml trên Codespace
serverAddr = "frp.freefrp.net"
serverPort = 7000
auth.token = "freefrp.net"

[[proxies]]
name = "nro-game"
localPort = 14445  →  remotePort = 21445   ← game client kết nối vào đây
[[proxies]]
name = "nro-register"
localPort = 8090   →  remotePort = 28090
```

Restart tunnel:
```bash
pkill -9 -f frpc; sleep 2
nohup /tmp/frp_0.61.0_linux_amd64/frpc -c /tmp/frpc_nro.toml >> ~/logs/frp.log 2>&1 &
tail -5 ~/logs/frp.log   # phải thấy "start proxy success"
```

## Phase keepalive đã hoàn thành: 1→16 ✅ — ĐỪNG CHỈNH NỮA
- Phase 16: cai_trang 453 bộ + items INSERT IGNORE
- Phase 15: fix Map.java spawn offset (mobs bay trên trời)
- Phase 13: icon skill 27/28 = 26247/26253/26241/31142 (khóa trong keepalive)
- Xem chi tiết tại: `docs/NRO_UPGRADE_PLAN_TEAMOBI2026.md`

## Keepalive frpc paths — ĐÃ THỐNG NHẤT (2026-07-19)
- **start_server()**: dùng `/tmp/frp/frpc` ✅ (tự download nếu mất)
- **upgrade_tunnel()**: đã sửa sang `/tmp/frp/frpc` ✅ (trước đây dùng old path gây lỗi)
- **⚠️ Không được dùng `/tmp/frp_0.61.0_linux_amd64/frpc`** — path cũ, /tmp bị xóa sau restart

## GitHub push — Lưu ý quan trọng
- Commit `ab2a28cdb` chứa token lộ trong .replit — NOT ancestor of main (đã rebase)
- .replit hiện tại đã sạch (xóa [userenv.shared] với token)
- Nếu push bị block lại: dùng `GH_TOKEN="${GITHUB_PERSONAL_ACCESS_TOKEN}" git push github main`

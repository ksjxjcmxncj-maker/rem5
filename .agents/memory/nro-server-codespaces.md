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

## Tunnel Setup — ĐÃ THỐNG NHẤT (2026-07-19)
### playit.gg (MAIN — game port)
- Binary: `/tmp/playit_old` = v0.15.0 (chạy không args, đọc `~/.config/playit_gg/playit.toml`)
- Secret: lưu trong `~/.config/playit_gg/playit.toml` key `secret_key`
- Tunnel: `147.185.221.211:52286` → `127.0.0.1:14445` (RTT ~72ms vs frp 245ms)
- Domain: `image-wick.gl.joinmc.link` (tunnel ID: 8d23638b-f6ac-43c5-b6e2-984c0a446c3d)
- **⚠️ Chỉ dùng v0.15.0** — v0.15.26/v1.0.10 bị lỗi IPC socket trên Codespace
- Keepalive tự download nếu mất: `https://github.com/playit-cloud/playit-agent/releases/download/v0.15.0/playit-linux-amd64`

### frpc (BACKUP — register port)
- Binary: `/tmp/frp/frpc` v0.61.0 (tự download nếu /tmp mất)
- **⚠️ Không dùng `/tmp/frp_0.61.0_linux_amd64/frpc`** — path cũ
- **⚠️ frpc v0.61.0 KHÔNG hỗ trợ `transport.useCompression`** — bỏ field đó
- Game port 21445 giữ trong config nhưng playit là tunnel chính
- Register port 28090: vẫn dùng frpc

### LINK_IP_PORT trong DataGame.java
- Hiện tại: `"NRO Private:147.185.221.211:52286:0"` (playit.gg)
- Codespace location: Pune, India (Azure) — Codespace network = 11MB/s ✅

## Icon cache — DataGame.java (2026-07-19)
- Đã thêm `ICON_CACHE = new ConcurrentHashMap<>()` vào DataGame.java
- `sendIcon()` kiểm tra cache trước khi đọc disk → giảm disk I/O lag
- icon_id trong skill_template = file path: `data/icon/x{zoom}/{id}.png` (trên server)
- icon 26247/26253/26241/31142 KHÔNG tồn tại trong SRC Team icon folder (max ID ~19008)
- Cần dùng icon ID thực tế tồn tại trong `~/nro/SRC/data/icon/x2/`

## Câu hỏi network — Ghi nhớ
- Codespace: NAT Azure → không nhận inbound TCP trực tiếp → phải dùng tunnel
- GitHub Codespace port forwarding: HTTPS only, không phải raw TCP
- Replit: HTTP/HTTPS only, không expose raw TCP port
- GCP e2-micro US Free: xa VN hơn → lag hơn (không nên dùng làm relay)
- **Oracle Cloud Singapore Free Tier**: tốt nhất nếu cần VPS relay riêng (4 OCPU Ampere A1, 24GB RAM)

## GitHub push — Lưu ý quan trọng
- Commit `ab2a28cdb` chứa token lộ trong .replit — NOT ancestor of main (đã rebase)
- .replit hiện tại đã sạch (xóa [userenv.shared] với token)
- Nếu push bị block lại: dùng `GH_TOKEN="${GITHUB_PERSONAL_ACCESS_TOKEN}" git push github main`

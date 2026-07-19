---
name: NRO No-TCP Bridge
description: APK WSS bridge working solution — arch, ports, tunnel, keepalive, lag notes
---

## Kiến trúc hoạt động (v2.3.0) ✅

Phone → BridgeService TCP:15000 → WSS Cloudflare Anycast → ws_bridge.py Codespace:8080 → Java server:14445

**Quan trọng:** BridgeService PHẢI dùng port 15000, KHÔNG dùng 14445 — Srcnrofree mod chiếm port 14445 → BridgeService không bind được → silent fail.

## Files chính
- `android-bridge-inject/src/BridgeService.java` — LOCAL_PORT = 15000
- `android-bridge-inject/src/BridgePreference.java` — XOR encode "LocalHost:127.0.0.1:15000:0,0,0"
- `android-bridge-inject/BridgeProvider.smali` — startForegroundService, gọi trước Unity Activity
- `.github/workflows/inject-apk.yml` — CI build, dispatch với ws_url + release_tag
- `~/bin/ws_bridge.py` trên Codespace — Python WSS bridge port 8080 → localhost:14445
- `~/bin/keepalive_tunnels.sh` — keepalive tất cả service (frpc + cloudflared + ws_bridge)

## Tunnel options
- **Cloudflare quick tunnel** (primary, ít lag): `cloudflared tunnel --url http://localhost:8080` → `*.trycloudflare.com`; URL thay đổi sau mỗi restart Codespace → cần named tunnel để ổn định
- **frp backup** (laggy, LA server): token=`freefrp.net`, server=23.95.31.196:7000, remote port 27000; port 21445/25000/26000 bị chiếm

## Lag so sánh
- frp via LA: ~200-300ms thêm ❌
- Cloudflare Anycast (SG edge): ~50-80ms ✅ — user xác nhận đỡ lag hơn
- Vẫn còn lag: chuyển map chậm, sát thương delay → do Codespace ở CentralIndia; nên dùng SoutheastAsia (Singapore)

## Codespace hiện tại
- Name: `cautious-space-halibut-p7rwgqwxrg5gfrrqg`
- Region: **CentralIndia** (không tối ưu cho VN user — nên migrate sang SoutheastAsia)

## Cloudflare named tunnel (TODO)
- Token `cfat_GYQf1C56FQe6JWBEgCBq2n9oGQVifjtGYVveRVA173cba810` — INVALID (hết hạn)
- Cần token mới tại dash.cloudflare.com → API Tokens → Cloudflare Tunnel: Edit

## APK build
- Trigger: `gh api POST /repos/akah3674-glitch/rem5/actions/workflows/inject-apk.yml/dispatches -f ref=main -f inputs[ws_url]=wss://... -f inputs[release_tag]=vX.X.X`
- Latest: v2.3.0

---
name: NRO No-TCP Architecture Research
description: Kết quả phân tích APK client.apk và 3 hướng loại bỏ TCP relay
---

## APK Analysis (client.apk — 84MB)

**Engine:** Unity IL2CPP — socket code compiled sang native ARM binary (libil2cpp.so)
**Implication:** Không thể sửa socket code bằng jadx (chỉ thấy Java wrapper, không có game logic)

### Key classes (từ global-metadata.dat):
- `GameMidlet`: PORT, PORT2, PROVIDER, LANGUAGE, VERSION fields
- `Session` (network class): host, NetworkInit, doConnect, isConnected, isMainSession, msgHandler, dataStream, cleanNetwork
- Server selection class: ListIP, linkDefault, ipSelect, nameServer, GetServerList, TryCreateCustomServer, ShowInputServer, CustomServerInputKey

### URL phát hiện trong metadata:
- `http://dragonball.indonaga.com/coda/?username=` — server list URL

### Port 14445 trong binary:
- 9 occurrences trong armeabi-v7a/libil2cpp.so (LE u16)
- Offset ví dụ: 0x32dafc, 0x45848c, 0x49848c
- Các byte này nằm trong ARM BL instructions — patch trực tiếp corrupt code

## Ba hướng triển khai

### A. v2rayNG (đề xuất nhanh nhất)
- Android app từ Play Store, không sửa APK game
- VLESS over WebSocket → Cloudflare VN PoP (5-10ms) → Codespace/Replit
- Setup: `scripts/setup_xray_codespace.sh` sinh VLESS link + QR code

### B. Bridge APK (đề xuất tốt nhất cho user phổ thông)
- Android app riêng: background service TCP 127.0.0.1:14445 → WebSocket
- Source tại `android-bridge/`
- Build tự động: `.github/workflows/build-nro-bridge.yml` → download từ Actions
- Người chơi nhập Custom Server = 127.0.0.1:14445 trong game
- Sau reboot: tự khởi động lại (BootReceiver)

### C. Inject smali vào game APK (sau khi B hoạt động)
- apktool decompile → thêm BridgeService.smali → repackage + sign
- 1 APK duy nhất

## Files tạo ra:
- `docs/APK_RESEARCH_NO_TCP.md` — phân tích đầy đủ
- `android-bridge/` — Android project source hoàn chỉnh (Java + OkHttp WebSocket)
- `.github/workflows/build-nro-bridge.yml` — GitHub Actions build APK
- `scripts/setup_xray_codespace.sh` — Xray server setup + v2rayNG link
- `scripts/ws_bridge_replit.py` — WebSocket bridge nâng cấp cho Replit

**Why:** Bridge APK thay thế hoàn toàn Termux ws_bridge_client.py — không cần Termux, chạy nền trên Android.

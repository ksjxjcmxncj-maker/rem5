# Nghiên cứu: Loại bỏ TCP khỏi kiến trúc NRO

> Ngày: 2026-07-19 | Trạng thái: Phân tích hoàn tất

---

## 1. Kết quả phân tích APK

### APK là gì?
- **Unity Engine** + **IL2CPP compilation**
- Toàn bộ logic game (kể cả socket TCP) được biên dịch sang native ARM binary: `libil2cpp.so`
- Không phải Java thuần — không thể đọc/sửa code dễ dàng như app Java thông thường

### Cấu trúc file APK:
```
client.apk
├── classes.dex              — Java wrapper (Unity bootstrap, JNI bridge)
├── assets/bin/Data/
│   ├── global-metadata.dat  — Metadata IL2CPP (tên class/method)
│   └── data.unity3d         — Asset bundle Unity
└── lib/
    ├── arm64-v8a/libil2cpp.so   (16MB) — Game code native ARM64
    └── armeabi-v7a/libil2cpp.so (15MB) — Game code native ARM32
```

### Các class networking phát hiện (từ global-metadata.dat):

**Class `GameMidlet`** (class chính):
```
PORT, PORT2, PROVIDER, LANGUAGE, VERSION
gameCanvas, isConnect2, server, serverScreen
```

**Class Session/Network**:
```
host, NetworkInit, doConnect, doSendMessage
isConnected, isMainSession, connected, connecting
msgHandler, dataStream, cleanNetwork
isCompareIPConnect, isCheckConnect
```

**Class ServerSelect** (màn hình chọn server):
```
ListIP, linkDefault, nameServer, serverPriority
hasConnected, ipSelect, flagServer
GetServerList, LoadIP, SaveIP, SaveIPNew
TryCreateCustomServer, ShowInputServer      ← QUAN TRỌNG
CustomServerRmsKey, CustomServerInputKey
```

### URL server list phát hiện:
```
http://dragonball.indonaga.com/coda/?username=
```
Game download danh sách server từ URL này, sau đó kết nối TCP tới IP:PORT được chọn.

### Port 14445 trong binary:
- Tìm thấy 9 vị trí trong `armeabi-v7a/libil2cpp.so`
- Các byte offset: `0x32dafc`, `0x45848c`, `0x49848c`, `0x64fc34`, `0x7984e8`, v.v.
- **Vấn đề**: Các offset này nằm trong ARM instructions (BL/jump), patch trực tiếp sẽ corrupt code

---

## 2. Tại sao không thể "chỉ sửa libil2cpp.so"

```
libil2cpp.so = C# game code → biên dịch → ARM machine code
                               (IL2CPP compiler)
```

Để đổi TCP → WebSocket trong libil2cpp.so cần:
1. Tìm đúng function `doConnect` trong ARM disassembly (~15MB code)
2. Thay toàn bộ logic `System.Net.Sockets.TcpClient` bằng WebSocket
3. WebSocket là protocol phức tạp hơn nhiều TCP thuần (HTTP Upgrade, framing, masking)
4. Cần thêm code mới vào binary — không gian hạn chế

→ **Không khả thi** khi không có source C#

---

## 3. Ba hướng thực tế

### 🅐 v2rayNG (KHÔNG cần sửa APK)

```
Game APK [TCP 14445]
    ↓
v2rayNG VPN (Android app, Play Store)  ← intercept TCP
    ↓ WebSocket/HTTPS
Cloudflare Edge (VN PoP, 5-10ms)
    ↓
GitHub Codespace / Replit
    ↓ TCP 127.0.0.1:14445
Game Server (Java)
```

**Ưu điểm:**
- Không sửa APK game
- Ping VN→Cloudflare 5-10ms (thay vì 180ms qua bore.pub/LA)
- Loại bỏ hoàn toàn bore.pub
- v2rayNG miễn phí, Play Store

**Cần làm:**
- Cài Xray-core server trên Codespace (script tự động)
- Config v2rayNG trên điện thoại (1 lần, import QR code)

**Giao thức:** VLESS over WebSocket over TLS (qua Cloudflare hoặc thẳng HTTPS)

---

### 🅑 Bridge APK riêng (KHÔNG sửa APK game)

```
Game APK [TCP → 127.0.0.1:14445]
    ↓ localhost TCP
NRO Bridge APK (background service)  ← chạy song song
    ↓ WebSocket/HTTPS
Codespace / Replit
    ↓ TCP 127.0.0.1:14445
Game Server (Java)
```

**Cơ chế:** Android apps khác nhau SHARE cùng network namespace → app A có thể kết nối localhost của app B

**Ưu điểm:**
- Không sửa APK game
- Không cần app bên thứ 3 (v2rayNG)
- Ta tự build, kiểm soát hoàn toàn

**Cần làm:**
- Build Android APK nhỏ (~100KB) bằng Java/Kotlin
- APK chứa 1 background Service: TCP server 14445 → WebSocket forward
- User nhập server = `127.0.0.1` trong game

---

### 🅒 Inject service vào game APK (1 APK duy nhất)

```
Decompile APK (apktool)
    ↓
Thêm BridgeService.smali
    ↓
Sửa AndroidManifest.xml (đăng ký service)
    ↓
Recompile + Sign
    ↓
1 APK duy nhất, tự lo bridge khi start
```

**Ưu điểm:** Người chơi chỉ cần 1 APK

**Nhược điểm:** Phức tạp hơn, cần sign APK

---

## 4. So sánh

| Tiêu chí | v2rayNG | Bridge APK | Inject smali |
|----------|---------|------------|--------------|
| Khó implement | Thấp ⭐ | Trung bình ⭐⭐ | Cao ⭐⭐⭐ |
| Sửa APK game | Không | Không | Không* |
| Ping | 5-10ms | 5-10ms | 5-10ms |
| Phụ thuộc app ngoài | v2rayNG | Không | Không |
| Kiểm soát | Thấp | Cao | Cao |
| Phù hợp cho player khác | Khó (cần config) | Dễ (cài 2 APK) | Dễ nhất (1 APK) |

---

## 5. Đề xuất

**Bước 1 ngay bây giờ:** Hướng **🅑 Bridge APK**
- Build APK đơn giản bằng Java (không cần Android Studio)
- Server-side đã có `ws_bridge_server.py` sẵn rồi
- Test nhanh nhất

**Bước 2 (nếu bridge APK hoạt động):** Hướng **🅒 Inject**
- Gộp vào 1 APK, dùng `apktool` + smali

**Hướng v2rayNG:** Dùng nếu muốn bỏ qua build APK, nhưng player cần config thêm app.

---

## 6. Server-side đã sẵn sàng

`scripts/ws_bridge_server.py` đã có:
```python
# Chạy trên Codespace port 8080
# WebSocket → TCP 127.0.0.1:14445
```

Chỉ cần client-side thay đổi (hiện tại = Termux + ws_bridge_client.py).

---

## 7. Build Bridge APK — Kế hoạch kỹ thuật

```java
// NROBridgeService.java — Android Service
public class NROBridgeService extends Service {
    // 1. Tạo TCP server trên localhost:14445
    ServerSocket serverSocket = new ServerSocket(14445, 0, 
        InetAddress.getByName("127.0.0.1"));
    
    // 2. Mỗi kết nối từ game → mở WebSocket đến Codespace
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder()
        .url("wss://YOUR-CODESPACE-8080.app.github.dev")
        .build();
    WebSocket ws = client.newWebSocket(request, listener);
    
    // 3. Relay dữ liệu bidirectional
    // TCP socket ↔ WebSocket
}
```

**Dependency:** OkHttp (WebSocket) — phổ biến, nhỏ gọn

**Build tool:** Gradle (có thể chạy CLI không cần Android Studio)

**Signed:** debug key (tự động) hoặc keystore tùy chỉnh

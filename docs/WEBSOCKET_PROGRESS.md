# WebSocket Upgrade — Tiến Trình
**Ngày bắt đầu:** 2026-07-21  
**Repo:** ksjxjcmxncj-maker/rem5  
**Tham khảo kế hoạch:** attached_assets/bao_cao_websocket_*.txt  

---

## ✅ HOÀN THÀNH — Bước 1-4 (tạo file + sửa file)

### File MỚI đã tạo:
| File | Package | Mô tả |
|------|---------|--------|
| `server/src/nro/models/network/WebSocketSession.java` | `nro.models.network` | ISession dùng Netty Channel thay Socket |
| `server/src/nro/models/network/WsMySession.java` | `nro.models.network` | Kế thừa WebSocketSession, xử lý login + decode frame |
| `server/src/nro/netty/WsGameHandler.java` | `nro.netty` | Netty handler: decode WS frame → gọi Controller |
| `server/src/nro/netty/WsServerInitializer.java` | `nro.netty` | Pipeline: HTTP upgrade → WebSocket tại `/game` |
| `server/src/nro/netty/NettyWsServer.java` | `nro.netty` | Thread Netty server trên port 14446 |

### File HIỆN TẠI đã sửa:
| File | Thay đổi |
|------|----------|
| `server/src/nro/models/server/ServerManager.java` | +import NettyWsServer, +WS_PORT=14446, +start WS sau TCP |
| `server/src/nro/models/server/Manager.java` | +đọc server.wsport → ServerManager.WS_PORT |
| `server/Config.properties` | +server.wsport=14446 |

---

## 🔄 Bước 5 — CHƯA THỰC HIỆN: Build & Test

### Compile trên Codespace:
```bash
cd ~/nro/SRC

# Compile 5 file mới
javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/nro_out \
  src/nro/models/network/WebSocketSession.java \
  src/nro/models/network/WsMySession.java \
  src/nro/netty/WsGameHandler.java \
  src/nro/netty/WsServerInitializer.java \
  src/nro/netty/NettyWsServer.java \
  src/nro/models/server/ServerManager.java \
  src/nro/models/server/Manager.java

# Nếu compile OK, update JAR
jar uf NgocRongOnline.jar -C /tmp/nro_out .

# Restart server
pkill -9 -f NgocRongOnline; sleep 3
nohup java -Xms256m -Xmx1g -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &

# Kiểm tra log
sleep 10 && tail -20 ~/logs/server.log
# Phải thấy: "WebSocket server started on port 14446"
```

### Test kết nối WebSocket:
```bash
# Test từ Codespace shell (cần wscat hoặc python)
python3 -c "
import asyncio, websockets
async def test():
    async with websockets.connect('ws://localhost:14446/game') as ws:
        data = await ws.recv()
        print('Received key frame:', data.hex())
asyncio.run(test())
"
```

---

## Kiến trúc sau upgrade:

```
Port 14445 (GIỮ NGUYÊN): TCP → MySession → game logic
Port 14446 (MỚI):        WebSocket ws://host:14446/game → WsMySession → game logic
```

**Giao thức binary KHÔNG thay đổi:**  
`[2 bytes length][payload XOR với KEYS]` — hoạt động trong cả TCP lẫn WebSocket frame

**APK client kết nối WebSocket:**  
- BridgeService sẽ kết nối trực tiếp ws://host:14446/game  
- Không cần ws_bridge.py proxy nữa  
- Cần cập nhật BridgeService.java để dùng WebSocket thay TCP sau khi server chạy OK

---

## Lưu ý cho Agent tiếp theo:
- Netty 4.1.24.Final đã có sẵn trong `lib/` — KHÔNG cần thêm dependency
- `nro.models.interfaces.IMessageHandler` và `IKeySessionHandler` — kiểm tra import đúng package  
- Nếu compile lỗi `IMessageHandler not found` → kiểm tra `nro.models.interfaces` package
- Sau khi server WS chạy OK, cần expose port 14446 qua tunnel (playit.gg hoặc cloudflare)
- frpc config cần thêm proxy cho port 14446 nếu muốn expose ra ngoài

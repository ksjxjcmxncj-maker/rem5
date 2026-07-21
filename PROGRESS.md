# WebSocket Migration Progress
Last updated: 2026-07-21

## Trạng thái: Đã hoàn thành bước 1-4/5

## Những gì đã làm

### Agent 1 (agent kia) đã làm:
- `server/src/nro/netty/NettyWsServer.java` ✅ created
- `server/src/nro/netty/WsServerInitializer.java` ✅ created  
- `server/src/nro/netty/WsGameHandler.java` ✅ created
- `server/src/nro/models/network/WebSocketSession.java` ✅ created
- `server/src/nro/models/server/ServerManager.java` ✅ modified:
  - Added `import nro.netty.NettyWsServer`
  - Added `public static int WS_PORT = 14446`
  - Added `new NettyWsServer(WS_PORT).start()` in `run()`

### Agent 2 (agent này) đã làm:
- `server/src/nro/models/network/WsMySession.java` ✅ **REWRITE** - fixed 4 bugs:
  - BUG 1 (CRITICAL): Changed `extends WebSocketSession` → `extends MySession`
    - Controller.java cast `(MySession) s` sẽ ClassCastException với WebSocketSession
    - Fix: dùng fakeSocket() anonymous class để satisfy MySession(Socket) constructor
  - BUG 2: `new Message(byte[])` không tồn tại
    - Fix: `payload[0]` = cmd, `Arrays.copyOfRange(payload,1,len)` = body
    - Gọi đúng: `Controller.gI().onMessage(this, new Message(cmd, body))`
  - BUG 3: `sendMessage()` gửi raw getData() không có framing
    - Fix: gửi `[cmd][2-byte length][data]` matching TCP wire format
  - BUG 4: `accumulator.release()` thiếu trong disconnect() → ByteBuf leak
- `server/Config.properties` ✅ added `server.wsport=14446`

## Bước còn lại

### Bước 5: Build và test (cần làm trên Codespace/server)
```bash
cd server

# Compile 5 WebSocket files mới
javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/nro_out \
  src/nro/netty/NettyWsServer.java \
  src/nro/netty/WsServerInitializer.java \
  src/nro/netty/WsGameHandler.java \
  src/nro/models/network/WebSocketSession.java \
  src/nro/models/network/WsMySession.java

# Nếu compile thành công, update JAR
jar uf NgocRongOnline.jar -C /tmp/nro_out .

# Test: khởi động server, kiểm tra log có dòng:
# "WebSocket server started on port 14446"
# "NettyWsServer started on port 14446 (ws://host:14446/game)"
```

## Kiến trúc sau khi hoàn thành

```
Port 14445 (TCP giữ nguyên):
  Client cũ → Network.java (NIO) → MySession → Controller

Port 14446 (WebSocket mới):
  Client mới → Netty → WsGameHandler
    → WsMySession (extends MySession)
      → Controller (KHÔNG thay đổi, cast MySession OK vì kế thừa)
```

## Lưu ý cho agent sau nếu cần debug

1. WsMySession dùng `fakeSocket(ip)` anonymous Socket để bypass constructor
   - Session.java bắt SocketException nên setSendBufferSize etc. không crash
   - Sender/Collector threads được tạo nhưng KHÔNG start (override no-op)
   
2. Wire format client→server:
   - `[2 bytes length][cmd byte][data bytes]` trong WS binary frame
   - length = 1 + len(data)
   
3. Wire format server→client:
   - `[cmd byte][2 bytes length][data bytes]` trong WS binary frame
   - Matching TCP sender format (no XOR vì KEYS={0})

4. WebSocketSession.java vẫn còn trong repo nhưng không được dùng nữa
   - WsMySession extends MySession (không extends WebSocketSession)
   - Có thể xóa WebSocketSession.java sau khi test OK

5. Netty dependency đã có sẵn trong pom.xml (netty-all 4.1.24.Final)
   - Không cần thêm dependency

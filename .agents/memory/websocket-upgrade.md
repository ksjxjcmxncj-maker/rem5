---
name: WebSocket Upgrade Progress
description: Trạng thái nâng cấp TCP → WebSocket cho NRO Java server (port 14446)
---

## Trạng thái: ✅ Code đã push — Chờ compile & test trên Codespace

## Files đã tạo/sửa (commit websocket-upgrade):
- NEW: `server/src/nro/models/network/WebSocketSession.java` — ISession via Netty
- NEW: `server/src/nro/models/network/WsMySession.java` — login + decode binary frame
- NEW: `server/src/nro/netty/WsGameHandler.java` — Netty ChannelHandler
- NEW: `server/src/nro/netty/WsServerInitializer.java` — HTTP→WS pipeline tại `/game`
- NEW: `server/src/nro/netty/NettyWsServer.java` — daemon thread port 14446
- MOD: `server/src/nro/models/server/ServerManager.java` — +WS_PORT=14446, +NettyWsServer.start()
- MOD: `server/src/nro/models/server/Manager.java` — +đọc server.wsport từ Config.properties
- MOD: `server/Config.properties` — +server.wsport=14446

## Bước tiếp theo — Compile trên Codespace:
```bash
cd ~/nro/SRC
javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/nro_out \
  src/nro/models/network/WebSocketSession.java \
  src/nro/models/network/WsMySession.java \
  src/nro/netty/WsGameHandler.java \
  src/nro/netty/WsServerInitializer.java \
  src/nro/netty/NettyWsServer.java \
  src/nro/models/server/ServerManager.java \
  src/nro/models/server/Manager.java
jar uf NgocRongOnline.jar -C /tmp/nro_out .
pkill -9 -f NgocRongOnline; sleep 3
nohup java -Xms256m -Xmx1g -jar NgocRongOnline.jar >> ~/logs/server.log 2>&1 &
sleep 10 && tail -20 ~/logs/server.log
# Phải thấy: "WebSocket server started on port 14446"
```

## Kiến trúc:
- Port 14445: TCP (giữ nguyên) → MySession
- Port 14446: WebSocket ws://host:14446/game → WsMySession → Controller (không đổi)
- Giao thức binary: [2 bytes length][payload XOR KEYS] — giống nhau cả 2 port

**Why:** Netty đã có sẵn trong lib/, không cần thêm dependency. ISession interface giữ nguyên nên game logic 100% không đổi.

## Lưu ý compile:
- Nếu lỗi `cannot find symbol IMessageHandler` → package là `nro.models.interfaces.IMessageHandler`
- Nếu lỗi `IMessageSendCollect` → package là `nro.models.interfaces.IMessageSendCollect`
- Compile tất cả 7 file cùng lúc để tránh dependency error

## Sau khi server WS chạy OK:
1. Expose port 14446 qua tunnel (thêm vào playit.gg hoặc frpc)
2. Cập nhật BridgeService.java kết nối WebSocket trực tiếp thay vì TCP qua ws_bridge.py
3. Xem chi tiết tại `docs/WEBSOCKET_PROGRESS.md`

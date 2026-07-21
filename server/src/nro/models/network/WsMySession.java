package nro.models.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import nro.models.data.DataGame;
import nro.models.database.MrBlue;
import nro.models.player.Player;
import nro.models.player_system.AntiLogin;
import nro.models.server.Client;
import nro.models.server.Controller;
import nro.models.server.Maintenance;
import nro.models.server.Manager;
import nro.models.services.Service;
import nro.models.utils.Logger;
import nro.models.utils.TimeUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * WsMySession — WebSocket session ke thua thang MySession.
 *
 * Ly do extend MySession thay vi WebSocketSession:
 *   Controller.onMessage() cast (MySession) s — neu WsMySession khong phai
 *   MySession thi ClassCastException ngay khi co goi tin dau tien.
 *
 * Cach pass qua constructor MySession(Socket):
 *   Tao fake Socket an danh override getInetAddress() va
 *   getRemoteSocketAddress() de Session lay IP dung.
 *   Sender/Collector threads duoc tao nhung KHONG duoc start (overrides no-op).
 *
 * Giao thuc client -> server (trong WS binary frame):
 *   [2 bytes unsigned-short: total-payload-length][cmd byte][data bytes] XOR KEYS
 *   total-payload-length = 1 (cmd) + len(data)
 *
 * Giao thuc server -> client (trong WS binary frame):
 *   [cmd byte][2 bytes unsigned-short: data-length][data bytes]
 *   (giong format TCP, KEYS={0} nen XOR la no-op)
 */
public class WsMySession extends MySession {

    // Anti-login map rieng cho WebSocket connections
    private static final Map<String, AntiLogin> ANTILOGIN = new HashMap<>();

    private final ChannelHandlerContext ctx;
    private volatile boolean wsConnected = true;
    private final String wsIp;

    /** Tich luy bytes chua du 1 message trong frame */
    private final ByteBuf accumulator = Unpooled.buffer();

    // ─── Fake Socket ─────────────────────────────────────────────────────────

    /**
     * Tao Socket gia de pass vao MySession(Socket) ma khong can TCP that.
     * Session bat tat ca SocketException; getOutputStream/getInputStream tra null
     * (Sender/Collector catch IOException va skip).
     */
    private static Socket fakeSocket(final String ip) {
        return new Socket() {
            @Override
            public java.net.InetAddress getInetAddress() {
                try {
                    return java.net.InetAddress.getByName(ip);
                } catch (Exception e) {
                    return java.net.InetAddress.getLoopbackAddress();
                }
            }

            @Override
            public java.net.SocketAddress getRemoteSocketAddress() {
                try {
                    return new InetSocketAddress(java.net.InetAddress.getByName(ip), 0);
                } catch (Exception e) {
                    return new InetSocketAddress(0);
                }
            }
        };
    }

    // ─── Constructor ──────────────────────────────────────────────────────────

    public WsMySession(ChannelHandlerContext ctx, String ip) {
        super(fakeSocket(ip));   // MySession(Socket) -> Session(Socket) OK
        this.ctx       = ctx;
        this.wsIp      = ip;
        this.ipAddress = ip;    // ghi de ipAddress (public field tu MySession)
    }

    // ─── ISession I/O Override: dung Netty thay Socket ───────────────────────

    /**
     * Gui message qua WebSocket binary frame.
     * Format: [cmd byte][2 bytes data-length][data bytes]
     */
    @Override
    public void sendMessage(Message msg) {
        if (!wsConnected || msg == null) return;
        try {
            byte[] data    = msg.getData();
            int    dataLen = (data != null) ? data.length : 0;
            ByteBuf buf    = ctx.alloc().buffer(3 + dataLen);
            buf.writeByte(msg.command & 0xFF);
            buf.writeShort(dataLen);
            if (dataLen > 0) {
                buf.writeBytes(data);
            }
            ctx.writeAndFlush(new BinaryWebSocketFrame(buf));
            msg.cleanup();
        } catch (Exception e) {
            Logger.error("WsMySession.sendMessage error: " + e.getMessage());
        }
    }

    @Override
    public void doSendMessage(Message msg) throws Exception {
        sendMessage(msg);
    }

    @Override
    public void disconnect() {
        if (!wsConnected) return;
        wsConnected = false;
        accumulator.release();
        ctx.close();
        SessionManager.gI().removeSession(this);
    }

    @Override
    public boolean isConnected() {
        return wsConnected && ctx.channel().isActive();
    }

    @Override
    public String getIP() { return wsIp; }

    /** KHONG khoi dong TCP Sender/Collector — Netty event loop lo I/O */
    @Override public ISession start()        { return this; }
    @Override public ISession startSend()    { return this; }
    @Override public ISession startCollect() { return this; }

    // ─── WebSocket-specific ───────────────────────────────────────────────────

    /**
     * Gui session key ngay sau khi client ket noi WebSocket.
     * cmd = -27, data = [key-count][key-bytes XOR prev]
     * Tuong duong MyKeyHandler.sendKey() tren TCP.
     */
    public void sendSessionKey() {
        Message msg = new Message(-27);
        try {
            msg.writer().writeByte(KEYS.length);
            msg.writer().writeByte(KEYS[0]);
            for (int i = 1; i < KEYS.length; i++) {
                msg.writer().writeByte(KEYS[i] ^ KEYS[i - 1]);
            }
            sendMessage(msg);
            msg.cleanup();
            setSentKey(true);
        } catch (IOException e) {
            Logger.error("WsMySession.sendSessionKey error: " + e.getMessage());
        }
    }

    /**
     * Xu ly raw bytes tu WebSocket binary frame.
     *
     * Client gui format: [2 bytes unsigned-short length][cmd byte][data] XOR KEYS
     * length = 1 (cmd) + len(data)
     *
     * Buffer tich luy de xu ly frame bi split hoac nhieu message trong 1 frame.
     */
    public void handleRawBytes(byte[] data) {
        accumulator.writeBytes(data);
        while (accumulator.readableBytes() >= 2) {
            accumulator.markReaderIndex();
            int length = accumulator.readUnsignedShort();
            if (accumulator.readableBytes() < length) {
                accumulator.resetReaderIndex();
                break;
            }
            if (length < 1) {
                continue; // payload rong — bo qua
            }
            byte[] payload = new byte[length];
            accumulator.readBytes(payload);

            // XOR decrypt voi KEYS (KEYS={0} => no-op, de san neu doi key sau)
            byte[] key = getKey();
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= key[i % key.length];
            }

            try {
                byte   cmd  = payload[0];
                byte[] body = Arrays.copyOfRange(payload, 1, payload.length);
                // Message(byte cmd, byte[] data) — constructor dung
                Controller.gI().onMessage(this, new Message(cmd, body));
            } catch (Exception e) {
                Logger.error("WsMySession.handleRawBytes error: " + e.getMessage());
            }
        }
        if (accumulator.readableBytes() == 0) {
            accumulator.clear();
        }
    }

    // ─── Login override: WS-specific log ─────────────────────────────────────

    @Override
    public void login(String username, String password) {
        AntiLogin al = ANTILOGIN.computeIfAbsent(this.ipAddress, k -> new AntiLogin());
        if (!al.canLogin()) {
            Service.gI().sendThongBaoOK(this, al.getNotifyCannotLogin());
            return;
        }
        if (Manager.LOCAL) {
            Service.gI().sendThongBaoOK(this, "Server nay chi de luu du lieu");
            return;
        }
        if (Maintenance.isRunning) {
            Service.gI().sendThongBaoOK(this, "Server dang bao tri");
            return;
        }
        if (!this.isAdmin && Client.gI().getPlayers().size() >= Manager.MAX_PLAYER) {
            Service.gI().sendThongBaoOK(this, "May chu dang qua tai");
            return;
        }
        if (this.player == null) {
            Player pl = null;
            try {
                this.uu = username;
                this.pp = password;
                pl = MrBlue.login(this, al);
                if (pl != null) {
                    DataGame.sendSmallVersion(this);
                    DataGame.sendBgItemVersion(this);
                    this.timeWait = 0;
                    this.joinedGame = true;
                    pl.nPoint.calPoint();
                    pl.nPoint.setHp(pl.nPoint.hp);
                    pl.nPoint.setMp(pl.nPoint.mp);
                    pl.zone.addPlayer(pl);
                    pl.setSession(this);
                    Client.gI().put(pl);
                    this.player = pl;
                    DataGame.sendVersionGame(this);
                    DataGame.sendDataItemBG(this);
                    Controller.gI().sendInfo(this);
                    Logger.warning("[" + TimeUtil.getCurrHour() + ":" + TimeUtil.getCurrMin()
                            + "] - [WS] Player Login: " + this.player.name + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (pl != null) pl.dispose();
            }
        }
    }
}

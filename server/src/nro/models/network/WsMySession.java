package nro.models.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import nro.models.data.DataGame;
import nro.models.database.MrBlue;
import nro.models.item.Item;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WsMySession — tương đương MySession nhưng kế thừa WebSocketSession.
 * Xử lý login, sendKey, và decode binary frame theo giao thức NRO:
 *   [2 bytes length][payload XOR-encrypted với KEYS]
 */
public class WsMySession extends WebSocketSession {

    private static final Map<String, AntiLogin> ANTILOGIN = new HashMap<>();

    public Player player;
    public byte timeWait = 100;
    public static final byte[] KEYS = {0};
    public byte curR, curW;

    public String ipAddress;
    public boolean isAdmin;
    public int userId;
    public String uu, pp;
    public int typeClient;
    public byte zoomLevel;
    public long lastTimeLogout;
    public boolean joinedGame;
    public long lastTimeReadMessage;
    public boolean actived;
    public int version;

    public boolean check;
    public int goldBar;
    public long gold;
    public int eventPoint;
    public List<Item> itemsReward;
    public String dataReward;
    public boolean is_gift_box;
    public double bdPlayer;

    // Buffer tích lũy bytes chưa đủ 1 message
    private final ByteBuf accumulator = Unpooled.buffer();

    public WsMySession(ChannelHandlerContext ctx, String ip) {
        super(ctx, ip);
        this.ipAddress = ip;
    }

    /**
     * Gửi session key cho client — giống MyKeyHandler.sendKey().
     * Giao thức: cmd=-27, 1 byte số lượng keys, rồi các key byte.
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
            Logger.error("WsMySession sendSessionKey error: " + e.getMessage());
        }
    }

    /**
     * Nhận raw bytes từ WebSocket frame, ghép buffer, decode và đẩy vào Controller.
     * Giao thức: [2 bytes length][payload XOR với KEYS]
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
            byte[] payload = new byte[length];
            accumulator.readBytes(payload);
            // XOR decrypt
            byte[] key = getKey();
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= key[i % key.length];
            }
            try {
                Message msg = new Message(payload);
                byte cmd = msg.reader().readByte();
                byte[] body = msg.reader().readBytes(msg.reader().available());
                Controller.gI().onMessage(this, cmd, body);
            } catch (Exception e) {
                Logger.error("WsMySession handleRawBytes error: " + e.getMessage());
            }
        }
        if (accumulator.readableBytes() == 0) {
            accumulator.clear();
        }
    }

    public void login(String username, String password) {
        AntiLogin al = ANTILOGIN.computeIfAbsent(this.ipAddress, k -> new AntiLogin());
        if (!al.canLogin()) {
            Service.gI().sendThongBaoOK(this, al.getNotifyCannotLogin());
            return;
        }
        if (Manager.LOCAL) {
            Service.gI().sendThongBaoOK(this, "Server này chỉ để lưu dữ liệu");
            return;
        }
        if (Maintenance.isRunning) {
            Service.gI().sendThongBaoOK(this, "Server đang bảo trì");
            return;
        }
        if (!this.isAdmin && Client.gI().getPlayers().size() >= Manager.MAX_PLAYER) {
            Service.gI().sendThongBaoOK(this, "Máy chủ đang quá tải");
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
                            + "] - WS Player Login: " + this.player.name + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (pl != null) pl.dispose();
            }
        }
    }
}

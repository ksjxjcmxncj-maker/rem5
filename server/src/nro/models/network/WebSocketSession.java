package nro.models.network;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import nro.models.interfaces.IKeySessionHandler;
import nro.models.interfaces.IMessageHandler;
import nro.models.interfaces.IMessageSendCollect;
import nro.models.interfaces.ISession;

/**
 * WebSocket session thay thế Socket-based Session.
 * Dùng Netty ChannelHandlerContext để gửi dữ liệu.
 * Game logic (Controller, Service, Client) KHÔNG thay đổi.
 */
public class WebSocketSession implements ISession {

    private static int ID_INIT = 100000;

    private final int id = ID_INIT++;
    private final ChannelHandlerContext ctx;
    private final String ip;
    private byte[] KEYS = "NRO".getBytes();
    private boolean sentKey;
    private boolean connected = true;
    private IMessageHandler messageHandler;
    private IKeySessionHandler keyHandler;

    public WebSocketSession(ChannelHandlerContext ctx, String ip) {
        this.ctx = ctx;
        this.ip = ip;
    }

    @Override
    public void sendMessage(Message msg) {
        if (!connected || msg == null) return;
        byte[] data = msg.getData();
        ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
        msg.cleanup();
    }

    @Override
    public void doSendMessage(Message msg) throws Exception {
        sendMessage(msg);
    }

    @Override
    public void disconnect() {
        this.connected = false;
        this.sentKey = false;
        ctx.close();
        SessionManager.gI().removeSession(this);
    }

    @Override
    public ISession start() { return this; }

    @Override
    public ISession startSend() { return this; }

    @Override
    public ISession startCollect() { return this; }

    @Override
    public ISession setSendCollect(IMessageSendCollect collect) { return this; }

    @Override
    public ISession setMessageHandler(IMessageHandler handler) {
        this.messageHandler = handler;
        return this;
    }

    @Override
    public ISession setKeyHandler(IKeySessionHandler handler) {
        this.keyHandler = handler;
        return this;
    }

    @Override
    public void sendKey() throws Exception {
        if (keyHandler != null) keyHandler.sendKey(this);
    }

    @Override public void setSentKey(boolean sent) { this.sentKey = sent; }
    @Override public boolean sentKey() { return sentKey; }
    @Override public boolean isConnected() { return connected; }
    @Override public long getID() { return id; }
    @Override public String getIP() { return ip; }
    @Override public byte[] getKey() { return KEYS; }
    @Override public int getNumMessages() { return connected ? 0 : -1; }
    @Override public void dispose() { disconnect(); }

    public IMessageHandler getMessageHandler() { return messageHandler; }
    public ChannelHandlerContext getCtx() { return ctx; }
}

package nro.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import nro.models.network.SessionManager;
import nro.models.network.WsMySession;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty handler cho WebSocket game server.
 * Map channel ID -> WsMySession, decode frame, gọi game logic.
 * Game logic (Controller, Service, Client) KHÔNG thay đổi gì cả.
 */
@ChannelHandler.Sharable
public class WsGameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Map<ChannelId, WsMySession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String ip = ((InetSocketAddress) ctx.channel().remoteAddress())
                .getAddress().getHostAddress();
        WsMySession session = new WsMySession(ctx, ip);
        SESSIONS.put(ctx.channel().id(), session);
        SessionManager.gI().putSession(session);
        session.sendSessionKey();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        WsMySession session = SESSIONS.get(ctx.channel().id());
        if (session == null) return;

        if (frame instanceof BinaryWebSocketFrame) {
            byte[] data = new byte[frame.content().readableBytes()];
            frame.content().readBytes(data);
            session.handleRawBytes(data);
        } else if (frame instanceof CloseWebSocketFrame) {
            session.disconnect();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        WsMySession session = SESSIONS.remove(ctx.channel().id());
        if (session != null) session.disconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        WsMySession session = SESSIONS.remove(ctx.channel().id());
        if (session != null) session.disconnect();
        ctx.close();
    }
}

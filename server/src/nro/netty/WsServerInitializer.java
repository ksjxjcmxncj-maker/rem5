package nro.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;

/**
 * Pipeline cho WebSocket server:
 *   HTTP codec -> HTTP aggregator -> WS compression -> WS protocol -> game handler
 * Upgrade HTTP -> WebSocket tại path "/game".
 */
public class WsServerInitializer extends ChannelInitializer<SocketChannel> {

    // Sharable — một instance dùng cho tất cả channel
    private static final WsGameHandler HANDLER = new WsGameHandler();

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new WebSocketServerCompressionHandler());
        pipeline.addLast(new WebSocketServerProtocolHandler("/game", null, true));
        pipeline.addLast(HANDLER);
    }
}

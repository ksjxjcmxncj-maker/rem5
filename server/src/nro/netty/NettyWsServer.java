package nro.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import nro.models.utils.Logger;

/**
 * Netty WebSocket server chạy song song với TCP server trên port 14446.
 * Khởi động dưới dạng daemon thread — tự tắt khi JVM thoát.
 *
 * Compile & update JAR (trên Codespace):
 *   javac -cp "NgocRongOnline.jar:lib/*" -d /tmp/nro_out \
 *         src/nro/netty/NettyWsServer.java \
 *         src/nro/netty/WsServerInitializer.java \
 *         src/nro/netty/WsGameHandler.java \
 *         src/nro/models/network/WebSocketSession.java \
 *         src/nro/models/network/WsMySession.java
 *   jar uf NgocRongOnline.jar -C /tmp/nro_out .
 */
public class NettyWsServer extends Thread {

    private final int port;

    public NettyWsServer(int port) {
        super("NettyWsServer-" + port);
        this.setDaemon(true);
        this.port = port;
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ChannelFuture future = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new WsServerInitializer())
                    .bind(port)
                    .sync();
            Logger.success("NettyWsServer started on port " + port + " (ws://host:" + port + "/game)\n");
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        Logger.warning("NettyWsServer on port " + port + " stopped.\n");
    }
}

package network.socks5;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;

public class Socks5Server
{
    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private int port;

    public Socks5Server(EventLoopGroup boss, EventLoopGroup worker, int port) {
        this.boss = boss;
        this.worker = worker;
        this.port = port;
    }

    public void start() throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.group(boss, worker)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 ch.pipeline().addLast(
                         new Socks5CommandRequestDecoder(),
                         new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 8, 0, 8),
                         new LengthFieldPrepender(8),
                         SocksHandler.INSTANCE
                 );
             }
         }).bind(port).sync().channel();
    }
}
package network.socks5;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

public class ConnectHandler extends SimpleChannelInboundHandler<SocksMessage>
{
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage msg) {
        if (msg instanceof Socks5CommandRequest) {
            Socks5CommandRequest request = (Socks5CommandRequest) msg;
            Channel inboundChannel = ctx.channel();
            Bootstrap b = new Bootstrap();
            b.group(inboundChannel.eventLoop())
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(
                             new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 8, 0, 8),
                             new LengthFieldPrepender(8),
                             new RelayHandler(inboundChannel));
                 }
             });
            b.connect(request.dstAddr(), request.dstPort())
             .addListener(new ChannelFutureListener() {
                 @Override
                 public void operationComplete(ChannelFuture future) {
                     if (future.isSuccess()) {
                         ctx.pipeline().addLast(new RelayHandler(future.channel()));
                         ctx.pipeline().remove(ConnectHandler.this);
                     }
                 }
             });
        }
    }
}
package network.socks5;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.*;

@ChannelHandler.Sharable
public class SocksHandler extends SimpleChannelInboundHandler<SocksMessage>
{
    public static final SocksHandler INSTANCE = new SocksHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage msg) {
        switch (msg.version()) {
            case SOCKS5:
                if (msg instanceof Socks5InitialRequest) {
                    ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                    ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
                } else

                if (msg instanceof Socks5PasswordAuthRequest) {
                    ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                    ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
                } else

                if (msg instanceof Socks5CommandRequest) {
                    Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) msg;
                    if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
                        ctx.pipeline().addLast(new ConnectHandler());
                        ctx.pipeline().remove(this);
                        ctx.fireChannelRead(msg);
                    } else {
                        ctx.close();
                    }
                }

                else {
                    ctx.close();
                }
                break;
            default:
                ctx.close();
                break;
        }
    }
}
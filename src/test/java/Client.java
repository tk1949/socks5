import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.socksx.v5.*;
import network.socks5.Socks5Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {

    public static void main(String[] args) throws InterruptedException {
        // 启动 client
        Client client = new Client(new NioEventLoopGroup(), "127.0.0.1", 19888);
        client.start();

        Thread.sleep(1000);

        // 发送 socks5 协议
        DefaultSocks5CommandRequest request =
                new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.IPv4, "127.0.0.1", 8080);
        client.submission(request);

        // 发送信息
        Scanner sc = new Scanner(System.in);
        while (true) {
            client.submission(sc.nextLine().getBytes());
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private EventLoopGroup boos;
    private String ip;
    private int port;

    private Bootstrap boot;
    private int reconnection;

    private Channel channel;

    public Client(EventLoopGroup boss, String ip, int port) {
        this.boos = boss;
        this.ip = ip;
        this.port = port;

        this.boot = new Bootstrap();
        this.reconnection = 0;
    }

    public void start() {
        try {
            boot.group(boos)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                Socks5ClientEncoder.DEFAULT,
                                new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 8, 0, 8),
                                new LengthFieldPrepender(8),
                                new SocketFrameHandler());
                    }
                });
            channel = boot.connect(ip, port).sync().channel();
        } catch (Exception e) {
            logger.error("Client -> start {}", e.getMessage());
            reconnection = 8;
        }
    }

    public void stop() {
        channel.close();
        boos.shutdownGracefully();
    }

    public void submission(Object msg) {
        if (reconnection == 0) {
            channel.writeAndFlush(msg);
        } else {
            logger.error("Client -> submission error");
        }
    }

    public void submission(byte[] msg) {
        if (reconnection == 0) {
            channel.writeAndFlush(Unpooled.wrappedBuffer(msg));
        } else {
            logger.error("Client -> submission error");
        }
    }

    public boolean isActive() {
        return reconnection == 0;
    }

    private class SocketFrameHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            try {
                int length = msg.readableBytes();
                byte[] code = new byte[length];
                msg.getBytes(msg.readerIndex(), code, 0, length);
                msg.clear();

                System.out.println(ctx.channel().remoteAddress() + " : " + new String(code));
            } catch (Exception e) {
                logger.error("Client -> channelRead0 {}", e.getMessage());
            }
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) {
            if (reconnection++ < 10) {
                ctx.channel().eventLoop().schedule(() -> {
                    try {
                        channel = boot.connect().addListener((ChannelFutureListener) future -> {
                            if (future.cause() == null) {
                                reconnection = 0;
                            }
                        }).sync().channel();
                    } catch (InterruptedException e) {
                        logger.error("Client -> channelUnregistered", e);
                    }
                }, 5000, TimeUnit.MILLISECONDS);
            } else {
                stop();
            }
        }
    }
}

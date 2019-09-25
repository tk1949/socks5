import io.netty.channel.nio.NioEventLoopGroup;
import network.socks5.Socks5Server;

public class Server {
    public static void main(String[] args) throws InterruptedException {
        // 启动 server
        new Socks5Server(new NioEventLoopGroup(), new NioEventLoopGroup(), 19888).start();
    }
}
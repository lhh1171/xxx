package Server;
import initlazaiter.PipelineManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server {
    public static void run(int port){
        EventLoopGroup boss = new NioEventLoopGroup(2);
        NioEventLoopGroup worket = new NioEventLoopGroup();
        try {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss,worket)
                .channel(NioServerSocketChannel.class)
                .childHandler(new PipelineManager());
            Channel channel = bootstrap.bind(port).sync().channel();
            channel.closeFuture().sync();
            //返回ChannelFuture ，当此通道关闭时将收到通知。 此方法始终返回相同的future实例。
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            worket.shutdownGracefully();
        }
    }
    public static void main(String[] args) {
        run(12581);
    }
}

package netty.c3.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * @description 用户在客户端不断输入信息让服务器接收  当输入q的时候断开连接(优雅断开)
 */
@Slf4j
public class CloseFutureClient {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        ChannelFuture channelFuture = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override // 在连接建立后被调用
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                .connect(new InetSocketAddress("localhost", 8080));
        System.out.println(channelFuture.getClass());
        Channel channel = channelFuture.sync().channel();


        //新起一个线程用于接收System.in用户输入
        new Thread(()->{
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine();
                if ("q".equals(line)) {
                    channel.close(); // close 异步操作  交给其他线程关闭 假设经过网络波动1s 之后才真正关闭
                    //log.debug("处理关闭之后的操作"); // 所以不能在这里处理channel关闭后的逻辑  会瞬间执行这里的代码。此时channel还没关闭
                    break;
                }
                channel.writeAndFlush(line);
            }
        }, "input").start();

        // 获取 CloseFuture 对象， 专门在channel关闭之后处理剩余逻辑    1) 同步处理关闭， 2) 异步处理关闭
        ChannelFuture closeFuture = channel.closeFuture();
        System.out.println(closeFuture.getClass());
        closeFuture.sync();
        log.debug("按下q之后真正执行到此  处理关闭之后的操作");

        //closeFuture.addListener(new ChannelFutureListener() {
        //    @Override
        //    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        //        log.debug("处理关闭之后的操作");
        //        //发完剩余 不再接收其他  优雅停止所有nio线程
        //        group.shutdownGracefully();
        //    }
        //});
    }
}

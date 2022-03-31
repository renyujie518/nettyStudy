package netty.c3.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
/**
 * @description 学习channel
 */
@Slf4j
public class EventLoopClient {
    public static void main(String[] args) throws InterruptedException {
        // 带有 Future，Promise 的类型都是和异步方法配套使用，用来处理结果
        ChannelFuture channelFuture = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override // 在连接建立后被调用
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                // 1. 连接到服务器
                // 异步非阻塞, main 发起了connect调用，真正执行 connect 是 一个nio 线程
                .connect(new InetSocketAddress("localhost", 8080)); // 假设网络波动1s 秒后才会建立

        // 1.使用 sync 方法同步处理结果（建立连接的是nio线程   拿到结果的是main线程 ）
        //作用是阻塞住当前线程，直到nio线程连接建立完毕  否则网络波动1s，但是connect异步的，不等返回结果的
        //此时如果没有.sync()  会直接执行channelFuture.channel()  这时候的channel是未建立好连接的 消息就发不出去

        //channelFuture.sync();
        //Channel channel = channelFuture.channel();
        //log.debug("{}", channel);
        //channel.writeAndFlush("hello, world");

        // 2 使用 addListener(回调对象) 方法异步处理结果（建立连接和接收结果的事都交给nio线程）

        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            // 在 nio 线程连接建立好之后，会调用 operationComplete
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel channel = future.channel();
                log.debug("{}", channel);
                channel.writeAndFlush("hello, world");
            }
        });
    }
}

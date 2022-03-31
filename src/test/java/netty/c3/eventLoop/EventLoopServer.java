package netty.c3.eventLoop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;


/**
 * @description 学习EventLoop
 */

@Slf4j
public class EventLoopServer {
    public static void main(String[] args) {
        // 细分2：创建一个独立的 EventLoopGroup
        EventLoopGroup defaultEventLoopGroupOnlyForRead = new DefaultEventLoopGroup();
        new ServerBootstrap()
                // 细分1：boss 和 2个worker
                // boss 只负责 ServerSocketChannel 上 accept 事件     worker 只负责 socketChannel 上的读写
                .group(new NioEventLoopGroup(), new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("handler1", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.debug("这是使用new NioEventLoopGroup(2)处理的read事件" + buf.toString(Charset.defaultCharset()));
                                ctx.fireChannelRead(msg); // 让消息传递给下一个handler
                            }
                        })
                                .addLast(defaultEventLoopGroupOnlyForRead, "handler2", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.debug("这是使用单独的defaultEventLoopGroupOnlyForRead处理的read事件" + buf.toString(Charset.defaultCharset()));
                            }
                        });
                    }
                })
                .bind(8080);
    }
}

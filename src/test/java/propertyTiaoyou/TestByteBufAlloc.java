package propertyTiaoyou;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
/**
 * @description ALLOCATOR
 *
 * * 属于 SocketChannal 参数
 * * 用来分配 ByteBuf， ctx.alloc()
 * 具体是pooled  unpolled可以在在VM中-Dio.netty.allocator.type=pooled  配置
 * 具体是不是直接内存   可以在在VM中-Dio.netty.noPreferDirect=true  是true  改为堆内存
 *
 *
 * RCVBUF_ALLOCATOR
 *
 * * 属于 SocketChannal 参数
 * * 控制 netty 接收缓冲区大小
 * * 负责入站数据的分配，决定入站缓冲区的大小（并可动态调整），统一采用 direct 直接内存，具体池化还是非池化由 allocator 决定
 */
@Slf4j
public class TestByteBufAlloc {
    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new LoggingHandler());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                //ByteBuf buf = ctx.alloc().buffer();
                                //log.debug("alloc buf {}", buf);

                                log.debug("receive buf {}", msg);
                                System.out.println("");
                            }
                        });
                    }
                }).bind(8080);
    }
}

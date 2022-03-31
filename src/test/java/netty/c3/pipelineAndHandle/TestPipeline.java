package netty.c3.pipelineAndHandle;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

/**
 * @description
 * 入站处理器通常是 ChannelInboundHandlerAdapter 的子类，主要用来读取客户端数据，写回结果
 * 出站处理器通常是 ChannelOutboundHandlerAdapter 的子类，主要对写回结果进行加工
 * 入站处理器是按照 addLast 的顺序执行的，而 出站处理器是按照 addLast 的逆序执行的。
 *
 * ch.writeAndFlush 跳到尾部tail开始触发 倒着执行出站handle
 * ctx.writeAndFlush 从本handle作为起点，会向上找一个出站handle  如果上面没有就断了
 */
@Slf4j
public class TestPipeline {
    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        // 1. 通过 channel 拿到 pipeline
                        ChannelPipeline pipeline = ch.pipeline();
                        // 2. 添加处理器 head ->  h1 -> h2 ->  h4 -> h3 -> h5 -> h6 -> tail
                        pipeline.addLast("h1", new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                String name = buf.toString(Charset.defaultCharset());
                                log.debug("入站handle1");
                                super.channelRead(ctx, name);
                            }
                        });
                        pipeline.addLast("h2", new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object name) throws Exception {
                                Student student = new Student(name.toString());
                                log.debug("入站handle2");
                                super.channelRead(ctx, student); // 将数据传递给下个 handler，如果不调用，调用链会断开 或者调用 ctx.fireChannelRead(student);
                            }
                        });

                        pipeline.addLast("h3", new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                log.debug("入站handle3 结果{},类型{}", msg, msg.getClass());
                                //没必要再唤醒channelRead  因为没有入站handle了
                                //下面的出站handle只有往chanel中写了数据才会触发
                                ch.writeAndFlush(ctx.alloc().buffer().writeBytes("server...".getBytes()));
                                //ctx.writeAndFlush(ctx.alloc().buffer().writeBytes("server...".getBytes()));
                            }
                        });
                        pipeline.addLast("h4", new ChannelOutboundHandlerAdapter(){
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                log.debug("出站handle1");
                                super.write(ctx, msg, promise);
                            }
                        });
                        pipeline.addLast("h5", new ChannelOutboundHandlerAdapter(){
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                log.debug("出站handle2");
                                super.write(ctx, msg, promise);
                            }
                        });
                        pipeline.addLast("h6", new ChannelOutboundHandlerAdapter(){
                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                log.debug("出站handle3");
                                super.write(ctx, msg, promise);
                            }
                        });
                    }
                })
                .bind(8080);
    }
    @Data
    @AllArgsConstructor
    static class Student {
        private String name;
    }
}

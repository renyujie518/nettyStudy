package netty.c2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LoggingHandler;
/**
 * @description netty基本使用
 */
public class HelloServer {
    public static void main(String[] args) {
        // 1. 启动器，负责组装 netty 组件，启动服务器
        new ServerBootstrap()
            // 2. selector+thread=loop, group 多组负责处理不同的事件 一开始关注accept事件（netty做好了）
            .group(new NioEventLoopGroup())
            // 3. 选择 服务器的 ServerSocketChannel 实现
            .channel(NioServerSocketChannel.class) // 还可以选OIO（就是BIO）和epoll
            // 4. child 负责处理读写，决定了child能执行哪些操作（handler）
            .childHandler(
                    // 5. channel 代表和客户端进行数据读写的通道 Initializer 初始化，负责添加别的 handler
                new ChannelInitializer<NioSocketChannel>() {
                @Override// 在连接建立后被调用
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    // 6. 添加具体 handler
                    ch.pipeline().addLast(new LoggingHandler());
                    ch.pipeline().addLast(new StringDecoder()); // 将 ByteBuf 转换为字符串
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                        //自定义handle  重写读事件 打印上一步转换好的字符串
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            System.out.println(msg);
                        }
                    });
                }
            })
            // 7. 绑定监听端口
            .bind(8080);
    }
}

package propertyTiaoyou;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
/**
 * @description SO_BACKLOG
 * 属于 ServerSocketChannal 参数  详见.md课堂讲解
 */
public class TestBacklogServer {
    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                // 应该设置的大一点 1024
                // 这里通过`io.netty.channel.nio.NioEventLoop#processSelectedKey`打断点模拟netty accept不了而导致连接放到全连接队列里打满
                .option(ChannelOption.SO_BACKLOG, 2)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new LoggingHandler());
                    }
                }).bind(8080);
    }
}

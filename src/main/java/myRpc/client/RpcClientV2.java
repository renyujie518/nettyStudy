package myRpc.client;


import ChatHome.protocol.MessageCodecSharable;
import ChatHome.protocol.ProcotolFrameDecoder;
import ChatHome.protocol.SequenceIdGenerator;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;
import myRpc.handler.RpcResponseMessageHandler;
import myRpc.message.RpcRequestMessage;
import myRpc.service.HelloService;

import java.lang.reflect.Proxy;

/**
 * @description 第一版本的远程调用入参是写死的
 * 第二版新增：
 * channel 异步关闭
 * 单例创建唯一channel
 * 代理（让调用者像调用本地方法一样去调用远程方法）
 */
@Slf4j
public class RpcClientV2 {

    public static void main(String[] args) {
        //代理（让调用者像调用本地方法一样去调用远程方法）
        HelloService service = getProxyService(HelloService.class);
        System.out.println(service.sayHello("zhangsan"));
        System.out.println(service.sayHello("lisi"));

    }

    /**
     * @description 创建代理类
     */
    public static <T> T getProxyService(Class<T> serviceClass) {
        ClassLoader loader = serviceClass.getClassLoader();
        Class<?>[] interfaces = new Class[]{serviceClass};
        //类加载器  被调用的接口  行为（代理对象  方法  实际参数）
        Object o = Proxy.newProxyInstance(loader, interfaces, (proxy, method, args) -> {
            // 1. 将方法调用转换为 消息对象RpcRequestMessage
            int sequenceId = SequenceIdGenerator.nextId();
            RpcRequestMessage msg = new RpcRequestMessage(
                    sequenceId,
                    serviceClass.getName(),
                    method.getName(),
                    method.getReturnType(),
                    method.getParameterTypes(),
                    args
            );
            // 2. 将消息对象发送出去
            getChannel().writeAndFlush(msg);

            // 3. 准备一个空 Promise 对象（可以在RpcClientV2.main线程和RpcResponseMessageHandler的nio线程间通信），来接收服务器的响应消息
            // channel().eventLoop()会得到一个nio线程  用来 promise 异步接收结果所使用的的线程  即采用promise.addListener的方式时采用的线程
            DefaultPromise<Object> promise = new DefaultPromise<>(getChannel().eventLoop());
            RpcResponseMessageHandler.PROMISES.put(sequenceId, promise);

            //promise.addListener(future -> {
            //
            //});

            // 4. 阻塞等待 promise 结果
            promise.await();
            if(promise.isSuccess()) {
                // 调用正常 拿到结果返回
                return promise.getNow();
            } else {
                // 调用失败
                throw new RuntimeException(promise.cause());
            }
        });
        return (T) o;
    }




    private static Channel channel = null;
    private static final Object LOCK = new Object();
    /**
     * @description 初始化 channel 方法
     */
    private static void initChannel() {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        RpcResponseMessageHandler RPC_HANDLER = new RpcResponseMessageHandler();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(group);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ProcotolFrameDecoder());
                ch.pipeline().addLast(LOGGING_HANDLER);
                ch.pipeline().addLast(MESSAGE_CODEC);
                ch.pipeline().addLast(RPC_HANDLER);
            }
        });
        try {
            channel = bootstrap.connect("localhost", 8080).sync().channel();
            channel.closeFuture().addListener(future -> {
                group.shutdownGracefully();
            });
        } catch (Exception e) {
            log.error("client error", e);
        }
    }


    /**
     * @description 这里rpc是长链接  所以所有的通信只需要一个channel
     * 单例模式(双重检查锁)  获取唯一的 channel 对象
     */
    public static Channel getChannel() {
        if (channel != null) {
            return channel;
        }
        synchronized (LOCK) {
            if (channel != null) {
                return channel;
            }
            //这里channel.closeFuture().addListener的关闭必须是这种异步实现 否则 initChannel()会一直阻塞等待关闭信号，就会导致getChannel()永远结束不了
            initChannel();
            return channel;
        }
    }
}

package ChatHome.client;


import ChatHome.message.*;
import ChatHome.protocol.MessageCodecSharable;
import ChatHome.protocol.ProcotolFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ChatClient {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        //自定义聊天室的编解码器
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        //4.1采用的是nio的channelRead  而4.2中的"输入"线程在channelActive下开在了main线程下 两线程通信采用倒计时锁  减为0接触await阻塞
        CountDownLatch WAIT_FOR_LOGIN = new CountDownLatch(1);
        AtomicBoolean LOGINFLAG = new AtomicBoolean(false);
        AtomicBoolean EXITFLAG = new AtomicBoolean(false);
        Scanner scanner = new Scanner(System.in);


        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    /** 1. 先正确分割协议（处理黏包/半包问题）入站时首先经过FrameDecoder
                     * 再利用自定义聊天协议解码消息（InboundHandler完成后出站向上找到这个MESSAGE_CODEC的encode成自定义的bytebuf）
                     * 注意这里没加LoggingHandle  信息太多扰乱 仅在编写调试时使用**/
                    ch.pipeline().addLast(new ProcotolFrameDecoder());
                    //ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(MESSAGE_CODEC);


                    /** 2. IdleStateHandler空闲状态检测器 用来判断是不是假死 读空闲时间过长，或写空闲时间过长
                     50s 内如果没有收到 channel 的数据，会触发一个 IdleState#WRITER_IDLE  事件 再用特殊事件handler userEventTriggered关闭channel **/
                    ch.pipeline().addLast(new IdleStateHandler(0, 50, 0));

                    /** 3. 心跳包
                     * 客户端可以定时向服务器端发送数据，只要这个时间间隔小于服务器定义的空闲检测的时间间隔，那么就能防止误判**/
                    ch.pipeline().addLast(new ChannelDuplexHandler() {
                        // 用来触发特殊事件
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception{
                            IdleStateEvent event = (IdleStateEvent) evt;
                            // 触发了写空闲事件
                            if (event.state() == IdleState.WRITER_IDLE) {
                                //log.debug("50s 没有写数据了，发送一个心跳包数据  但是服务器还是收到了PingMessage  不会触发读空闲而导致close");
                                ctx.writeAndFlush(new PingMessage());
                            }
                        }
                    });

                    /** 4. 创建聊天室业务的自定义handler**/
                    ch.pipeline().addLast("client handler", new ChannelInboundHandlerAdapter() {
                        /** 4.1  接收服务器传回的登录的响应消息 LoginResponseMessage**/
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            log.debug("loginResponseMsgFromServer: {}", msg);
                            if ((msg instanceof LoginResponseMessage)) {
                                LoginResponseMessage response = (LoginResponseMessage) msg;
                                if (response.isSuccess()) {
                                    // 如果登录成功
                                    LOGINFLAG.set(true);
                                }
                                //计数-1 变为0   唤醒 system in 线程
                                WAIT_FOR_LOGIN.countDown();
                            }
                        }
                        /** 4.2  在连接建立后触发 active 事件  避免由于Scanner(System.in)把nio线程阻塞住  所以单开一个线程
                         * 这里面有很多return和break   在此说明下   return :直接结束`system in`线程    break:结束switch **/
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            // 负责接收用户在控制台的输入，负责向服务器发送各种消息
                            new Thread(() -> {

                                System.out.println("请输入用户名:");
                                String username = scanner.nextLine();
                                if(EXITFLAG.get()){
                                    return;
                                }
                                System.out.println("请输入密码:");
                                String password = scanner.nextLine();
                                if(EXITFLAG.get()){
                                    return;
                                }
                                // 构造消息对象（省略校验）
                                LoginRequestMessage LoginMessage = new LoginRequestMessage(username, password);
                                System.out.println("构建的登录对象是" + LoginMessage);
                                // 发送用户登录消息给服务器  服务器会返回LoginResponseMessage 在4.1中对成功与否先做判断
                                ctx.writeAndFlush(LoginMessage);


                                try {
                                    WAIT_FOR_LOGIN.await();
                                    System.out.println("登录成功  等待倒计时锁减为0 唤醒`system in`线程");
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                // 如果登录失败  关闭channel 继而触发 channel.closeFuture().sync()
                                if (!LOGINFLAG.get()) {
                                    System.out.println("登录失败   关闭channel  结束`system in`线程");
                                    ctx.channel().close();
                                    return;
                                }

                                //实际的聊天
                                while (true) {
                                    System.out.println("==============打印功能命令菜单====================");
                                    System.out.println("send [username] [content]");
                                    System.out.println("gsend [group name] [content]");
                                    System.out.println("gcreate [group name] [m1,m2,m3...]");
                                    System.out.println("gmembers [group name]");
                                    System.out.println("gjoin [group name]");
                                    System.out.println("gquit [group name]");
                                    System.out.println("quit");
                                    System.out.println("==================================");
                                    String command = null;
                                    try {
                                        command = scanner.nextLine();
                                    } catch (Exception e) {
                                        break;
                                    }
                                    if(EXITFLAG.get()){
                                        return;
                                    }
                                    //解析输入的命令  按照空格分割 commandIn[0]就是命令本身 比如send gsend...
                                    String[] commandIn = command.split(" ");
                                    switch (commandIn[0]){
                                        case "send":
                                            ctx.writeAndFlush(new ChatRequestMessage(username, commandIn[1], commandIn[2]));
                                            break;
                                        case "gsend":
                                            ctx.writeAndFlush(new GroupChatRequestMessage(username, commandIn[1], commandIn[2]));
                                            break;
                                        case "gcreate":
                                            Set<String> set = new HashSet<>(Arrays.asList(commandIn[2].split(",")));
                                            set.add(username); // 加入自己
                                            ctx.writeAndFlush(new GroupCreateRequestMessage(commandIn[1], set));
                                            break;
                                        case "gmembers":
                                            ctx.writeAndFlush(new GroupMembersRequestMessage(commandIn[1]));
                                            break;
                                        case "gjoin":
                                            ctx.writeAndFlush(new GroupJoinRequestMessage(username, commandIn[1]));
                                            break;
                                        case "gquit":
                                            ctx.writeAndFlush(new GroupQuitRequestMessage(username, commandIn[1]));
                                            break;
                                        case "quit":
                                            ctx.channel().close();
                                            return;
                                    }
                                }
                            }, "system in").start();
                        }

                        // 在连接断开时触发
                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                            log.debug("连接已经断开，按任意键退出..");
                            EXITFLAG.set(true);
                        }

                        // 在出现异常时触发
                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            log.debug("连接已经断开，按任意键退出..{}", cause.getMessage());
                            EXITFLAG.set(true);
                        }
                    });
                }
            });
            Channel channel = bootstrap.connect("localhost", 8080).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("client error", e);
        } finally {
            group.shutdownGracefully();
        }
    }
}

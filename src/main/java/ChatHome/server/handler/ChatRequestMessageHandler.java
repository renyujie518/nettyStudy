package ChatHome.server.handler;


import ChatHome.message.ChatRequestMessage;
import ChatHome.message.ChatResponseMessage;
import ChatHome.server.session.SessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @description 入站 读  服务器处理客户端发来的chat消息  仅关注ChatRequestMessage
 */
@ChannelHandler.Sharable
public class ChatRequestMessageHandler extends SimpleChannelInboundHandler<ChatRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatRequestMessage msg) throws Exception {
        //用户1 传来msg
        String to = msg.getTo();
        Channel channelForTo = SessionFactory.getSession().getChannel(to);
        // 用户2在线  直接往用户2所属的channel发消息
        if (channelForTo != null) {
            channelForTo.writeAndFlush(new ChatResponseMessage(msg.getFrom(), msg.getContent()));
        }
        // 不在线  向msg的来源ctx(用户1)退回失败消息
        else {
            ctx.writeAndFlush(new ChatResponseMessage(false, "对方用户不存在或者不在线"));
        }
    }
}

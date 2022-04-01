package ChatHome.server.handler;


import ChatHome.message.GroupChatRequestMessage;
import ChatHome.message.GroupChatResponseMessage;
import ChatHome.server.session.GroupSessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;
/**
 * @description 入站 读  服务器处理客户端发来的群chat消息  仅关注GroupChatRequestMessage
 */
@ChannelHandler.Sharable
public class GroupChatRequestMessageHandler extends SimpleChannelInboundHandler<GroupChatRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupChatRequestMessage msg) throws Exception {
        //获取所有在线组成员的channel
        List<Channel> channelsInGroupOnline = GroupSessionFactory.getGroupSession()
                .getMembersChannel(msg.getGroupName());

        for (Channel channel : channelsInGroupOnline) {
            channel.writeAndFlush(new GroupChatResponseMessage(msg.getFrom(), msg.getContent()));
        }
    }
}

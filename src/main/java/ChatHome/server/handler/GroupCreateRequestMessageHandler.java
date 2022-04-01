package ChatHome.server.handler;


import ChatHome.message.GroupCreateRequestMessage;
import ChatHome.message.GroupCreateResponseMessage;
import ChatHome.server.session.Group;
import ChatHome.server.session.GroupSession;
import ChatHome.server.session.GroupSessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;
import java.util.Set;

/**
 * @description 入站 读  服务器处理客户端发来的创建群消息  仅关注GroupCreateRequestMessage
 */
@ChannelHandler.Sharable
public class GroupCreateRequestMessageHandler extends SimpleChannelInboundHandler<GroupCreateRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupCreateRequestMessage msg) throws Exception {
        String groupName = msg.getGroupName();
        Set<String> members = msg.getMembers();
        // 群管理器
        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        Group group = groupSession.createGroup(groupName, members);
        if (group == null) {
            // 发生成功消息
            ctx.writeAndFlush(new GroupCreateResponseMessage(true, groupName + "群创建成功"));
            // 发送拉群消息
            List<Channel> channelsInGroupOnline = groupSession.getMembersChannel(groupName);//获取所有在线组成员的channel
            for (Channel channel : channelsInGroupOnline) {
                channel.writeAndFlush(new GroupCreateResponseMessage(true, "您已被拉入" + groupName));
            }
        } else {
            ctx.writeAndFlush(new GroupCreateResponseMessage(false, groupName + "已经存在"));
        }
    }
}

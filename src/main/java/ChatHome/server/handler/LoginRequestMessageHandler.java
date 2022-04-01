package ChatHome.server.handler;

import ChatHome.message.LoginRequestMessage;
import ChatHome.message.LoginResponseMessage;
import ChatHome.server.service.UserServiceFactory;
import ChatHome.server.session.SessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @description 入站 读  服务器处理客户端发来的登录消息  仅关注LoginRequestMessage
 */
@ChannelHandler.Sharable
public class LoginRequestMessageHandler extends SimpleChannelInboundHandler<LoginRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestMessage msg) throws Exception {
        String username = msg.getUsername();
        String password = msg.getPassword();
        boolean isLogin = UserServiceFactory.getUserService().login(username, password);
        LoginResponseMessage message;
        if (isLogin) {
            //将channel和用户名对应起来   将来可以根据用户名获得channel
            SessionFactory.getSession().bind(ctx.channel(), username);
            message = new LoginResponseMessage(true, "登录成功");
        } else {
            message = new LoginResponseMessage(false, "用户名或密码不正确");
        }
        ctx.writeAndFlush(message);
    }
}

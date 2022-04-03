package myRpc.handler;



import myRpc.service.HelloService;
import myRpc.service.ServicesFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import myRpc.message.RpcRequestMessage;
import myRpc.message.RpcResponseMessage;
import java.lang.reflect.Method;

/**
 * @description 服务器  读到rpc请求消息 最终发给客户端
 */
@Slf4j
@ChannelHandler.Sharable
public class RpcRequestMessageHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage message) {
        RpcResponseMessage response = new RpcResponseMessage();
        //为保持一次完整的rpc调用  服务器要给client返回response的时候要保证收发过程中 的SequenceId一致
        response.setSequenceId(message.getSequenceId());
        try {
            //通过接口名称获取实现类的对象
            HelloService service = (HelloService)
                    ServicesFactory.getService(Class.forName(message.getInterfaceName()));
            //反射调用方法
            Method method = service.getClass().getMethod(message.getMethodName(), message.getParameterTypes());
            Object invoke = method.invoke(service, message.getParameterValue());
            //包装成RpcResponseMessage
            response.setReturnValue(invoke);
        } catch (Exception e) {
            e.printStackTrace();
            response.setExceptionValue(new Exception("远程调用出错:" + e.getCause().getMessage()));
        }
        ctx.writeAndFlush(response);
    }

}

package myrpc;

import myRpc.message.RpcRequestMessage;
import myRpc.service.HelloService;
import myRpc.service.ServicesFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 对于服务器  处理rpc请求消息  测试服务器读到RpcRequestMessage后反射调用方法查看结果是否正确
 */
public class TestInvoke2rpcReponse {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RpcRequestMessage message = new RpcRequestMessage(
                1,
                "myRpc.service.HelloService",
                "sayHello",
                String.class,
                new Class[]{String.class},
                new Object[]{"张三"}
        );

        //通过接口名称获取实现类的对象
        HelloService service = (HelloService)
                ServicesFactory.getService(Class.forName(message.getInterfaceName()));
        //反射调用方法
        Method method = service.getClass().getMethod(message.getMethodName(), message.getParameterTypes());
        Object invoke = method.invoke(service, message.getParameterValue());
        //查看结果是否正确
        System.out.println(invoke);
    }
}

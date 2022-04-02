package ChatHome;


import ChatHome.config.Config;
import ChatHome.message.LoginRequestMessage;
import ChatHome.message.Message;
import ChatHome.protocol.MessageCodecSharable;
import ChatHome.protocol.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @description 测试在聊天室中实现的的序列化算法
 */
public class TestSerializer {

    public static void main(String[] args)  {
        MessageCodecSharable CODEC = new MessageCodecSharable();
        LoggingHandler LOGGING = new LoggingHandler();
        EmbeddedChannel channel = new EmbeddedChannel(LOGGING, CODEC, LOGGING);

        LoginRequestMessage message = new LoginRequestMessage("zhangsan", "123");
        System.out.println("测试encode是否正确  出站msg编码  调用序列化");
        channel.writeOutbound(message);

        System.out.println("测试decode是否正确  入站msg解码  调用反序列化");
        //先获得msg序列化后的ByteBuf
        ByteBuf buf = messageToByteBuf(message);
        channel.writeInbound(buf);
    }


    /**
     * @description 模仿ChatHome.protocol.MessageCodecSharable#encode方法
     * 在MessageCodecSharable中实现的时候借助重写MessageToMessageCodec  是一个void方法  不好测
     * 在这里改造一下  入站接收的参数是ByteBuf  所以直接返回"在自定义协议下 msg经由配置文件选定的序列化算法序列化后的ByteBuf"
     */
    public static ByteBuf messageToByteBuf(Message msg) {
        //利用枚举的序号  0代表jdk序列化算法   1代表json序列化算法
        int algorithm = Config.getSerializerAlgorithm().ordinal();
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        out.writeBytes(new byte[]{1, 2, 3, 4});
        out.writeByte(1);
        out.writeByte(algorithm);
        out.writeByte(msg.getMessageType());
        out.writeInt(msg.getSequenceId());
        out.writeByte(0xff);
        byte[] bytes = Serializer.Algorithm.values()[algorithm].serialize(msg);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
        return out;
    }
}

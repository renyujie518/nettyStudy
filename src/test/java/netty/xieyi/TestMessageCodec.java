package netty.xieyi;


import ChatHome.message.LoginRequestMessage;
import ChatHome.protocol.MessageCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;

/**
 * @description 测试聊天室中自己的编解码协议
 */
public class TestMessageCodec {
    public static void main(String[] args) throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(
                new LoggingHandler(),
                new LengthFieldBasedFrameDecoder(
                        1024, 12, 4, 0, 0),
                new MessageCodec()
        );
        // encode
        LoginRequestMessage message = new LoginRequestMessage("zhangsan", "123");
        System.out.println("==========客户端encode把一个具体的message写入(以登录message为例)===========");
        channel.writeOutbound(message);

        // decode
        ByteBuf buf1 = ByteBufAllocator.DEFAULT.buffer(512);
        new MessageCodec().encode(null, message, buf1);
        System.out.println("==========服务器decode解码得到的完整的数据===========");
        channel.writeInbound(buf1);

        System.out.println("==========buf长度是212  这里用切片（从100切会把完整buf切断）模拟半包问题下LengthFieldBasedFrameDecoder的意义===========");
        ByteBuf buf2 = ByteBufAllocator.DEFAULT.buffer(512);
        new MessageCodec().encode(null, message, buf2);
        ByteBuf s1 = buf2.slice(0, 100);
        ByteBuf s2 = buf2.slice(100, buf2.readableBytes() - 100);
        s1.retain();
        channel.writeInbound(s1); // 小坑 writeInbound会调用release 零拷贝  所以在此之前引用计数+1
        //由于LengthFieldBasedFrameDecoder的限制  不把s2发完 会一直阻塞等待(LengthFieldBasedFrameDecoder已经计算出实际的context大小)，直到接收到的大小满足，才会传递到 MessageCodec()这个handler
        channel.writeInbound(s2);
    }
}

package netty.nianBaobanBao;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @description  模拟黏包 半包
在发送消息前，先约定用定长字节表示接下来数据的长度
// 最大长度，长度字段偏移量，长度字段占用字节，长度字段调整(长度字段为基准，再过后几个字节是内容)，（从头开始）剥离字节数
ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 1, 0, 1));
 */
public class TestLengthFieldDecoder {
    public static void main(String[] args) {
        //测试编解码专用channel
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(
                        1024, 0, 4, 1,4),
                new LoggingHandler(LogLevel.DEBUG)
        );

        //  模拟客户端    4 个字节的内容长度为实际内容
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        send(buffer, "Hello, world");
        send(buffer, "Hi!");
        //实际发出去
        embeddedChannel.writeInbound(buffer);
    }

    
    /**
     * @description 往buffer中加入content
     */
    private static void send(ByteBuf buffer, String content) {
        byte[] bytes = content.getBytes(); // 实际内容
        int length = bytes.length; // 实际内容长度
        //填充head  int 4字节  所以lengthFieldLength为4  同时接收端不想要  所以initialBytesToStrip去除了4字节
        buffer.writeInt(length);
        //模拟在head后再加入版本号（除内容外额外的东西）  所以设置lengthAdjustment=1  长度字段后再1个字节为真正内容
        buffer.writeByte(1);
        //真正的内容
        buffer.writeBytes(bytes);
    }
}


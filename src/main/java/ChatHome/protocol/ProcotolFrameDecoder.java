package ChatHome.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;


/**
 * @description 由于已经设计了聊天的协议 在处理黏包半包的这几个参数就是固定的了 在这里单独提出来  但注意不能@ChannelHandler.Sharable
 */
public class ProcotolFrameDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * @description 在server和client中直接new这个无参构造
     */
    public ProcotolFrameDecoder() {
        this(1024, 12, 4, 0, 0);
    }

    public ProcotolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
}

package netty.c3.byteBuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

import static netty.c3.byteBuf.TestByteBuf.log;

/**
 * @description  CompositeByteBuf
 * 【零拷贝】的体现之一，可以将多个 ByteBuf 合并为一个逻辑上的 ByteBuf，避免拷贝
 */
public class TestCompositeByteBuf {
    public static void main(String[] args) {
        ByteBuf buf1 = ByteBufAllocator.DEFAULT.buffer();
        buf1.writeBytes(new byte[]{1, 2, 3, 4, 5});

        ByteBuf buf2 = ByteBufAllocator.DEFAULT.buffer();
        buf2.writeBytes(new byte[]{6, 7, 8, 9, 10});


        //深拷贝的方式   发生真的数据复制
        //ByteBuf bufferDeep = ByteBufAllocator.DEFAULT.buffer();
        //bufferDeep.writeBytes(buf1).writeBytes(buf2);
        //log(bufferDeep);

        //零拷贝的方式   合并为一个逻辑上的 ByteBuf，避免拷贝
        CompositeByteBuf bufferComposite = ByteBufAllocator.DEFAULT.compositeBuffer();
        bufferComposite.addComponents(true, buf1, buf2);
        log(bufferComposite);
        //最后也要记得release
        bufferComposite.release();

    }
}

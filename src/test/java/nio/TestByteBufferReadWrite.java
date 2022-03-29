package nio;

import java.nio.ByteBuffer;

import static nio.ByteBufferUtil.debugAll;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName TestByteBufferReadWrite.java
 * @Description TODO
 * @createTime 2022年03月29日 14:22:00
 */
public class TestByteBufferReadWrite {
    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put((byte) 0x61); // 'a'
        debugAll(buffer);
        buffer.put(new byte[]{0x62, 0x63, 0x64}); // b  c  d
        debugAll(buffer);
//        System.out.println(buffer.get());
        buffer.flip();
        System.out.println(buffer.get());
        debugAll(buffer);
        buffer.compact();
        debugAll(buffer);
        buffer.put(new byte[]{0x65, 0x66});
        debugAll(buffer);
    }
}

package netty.c3.byteBuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static netty.c3.byteBuf.TestByteBuf.log;

/**
 * @description netty的零拷贝
 * 对原始 ByteBuf 进行切片成多个 ByteBuf，切片后的 ByteBuf 并没有发生内存复制
 * 还是使用原始 ByteBuf 的内存，切片后的 ByteBuf 维护独立的 read，write 指针
 * duplicate是截取了原始 ByteBuf 所有内容，并且没有 max capacity 的限制，也是与原始 ByteBuf 使用同一块底层内存，只是读写指针是独立的
 *
 * 深拷贝：
 * copy  会将底层内存数据进行深拷贝，因此无论读写，都与原始 ByteBuf 无关
 */
public class TestSlice {
    public static void main(String[] args) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(10);
        buf.writeBytes(new byte[]{'a','b','c','d','e','f','g','h','i','j'});
        log(buf);

        // 在切片过程中，没有发生数据复制
        ByteBuf f1 = buf.slice(0, 5);
        f1.retain();
        ByteBuf f2 = buf.slice(5, 5);
        f2.retain();
        log(f1);
        log(f2);
        System.out.println("==========验证是使用原始 ByteBuf 的内存==============");
        f1.setByte(0, 'x');
        log(f1);
        log(buf);

        //切片后的 max capacity 被固定为这个区间的大小，因此不能追加 write
        //System.out.println("==========验证切片后的不能追加 write==============");
        //f1.writeByte('x');  //如果执行，会报 IndexOutOfBoundsException 异常

        System.out.println("==============释放原有 byteBuf 内存 小切片有也没了 可以对小切片用retain 计数器+1=================");
        buf.release();
        log(f1);

        System.out.println("==============最后用完小切片后记得release=================");
        f1.release();
        f2.release();

    }
}

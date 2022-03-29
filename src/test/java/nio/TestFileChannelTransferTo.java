package nio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName TestFileChannelTransferTo.java
 * @Description 文件拷贝
 * @createTime 2022年03月29日 15:03:00
 */
public class TestFileChannelTransferTo {
    public static void main(String[] args) {
        try {
            FileChannel from = new FileInputStream("/Users/renyujie/Desktop/nettyStudy/src/test/java/nio/from.txt").getChannel();
            FileChannel to = new FileOutputStream("/Users/renyujie/Desktop/nettyStudy/src/test/java/nio/to.txt").getChannel();
            // 效率高，底层会利用操作系统的零拷贝进行优化, 注意：直接调用transferTo上限2g 数据 所以改造多次传输
            // left 变量代表还剩余多少字节  transferTo返回值为传输的字节数  left每次减去已传输完的字节数
            long size = from.size();
            for (long left = size; left > 0; ) {
                System.out.println("position起点:" + (size - left) + " left剩余的字节数:" + left);
                left -= from.transferTo((size - left), left, to);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

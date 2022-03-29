package nio.selectorTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName MultiThreadClient.java
 * @Description TODO
 * @createTime 2022年03月29日 22:21:00
 */
public class MultiThreadClient {
    public static void main(String[] args) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost", 8088));
        sc.write(Charset.defaultCharset().encode("012345"));
        System.in.read();
    }
}

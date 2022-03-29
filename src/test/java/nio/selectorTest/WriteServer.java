package nio.selectorTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName WriteServer.java
 * @Description TODO
 * @createTime 2022年03月29日 20:50:00
 */
public class WriteServer {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        ssc.bind(new InetSocketAddress(8081));
        while (true) {
            selector.select();
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    SelectionKey sckey = sc.register(selector, 0, null);
                    //针对53行做测试用  实际本案例没有用到读模式
                    sckey.interestOps(SelectionKey.OP_READ);
                    // 1  模拟向客户端发送大量数据  所以要分段写入
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 5000000; i++) {
                        sb.append("a");
                    }
                    ByteBuffer buffer = Charset.defaultCharset().encode(sb.toString());

                    // 2. 先向客户端写一次   返回值代表实际写入的字节数
                    int write = sc.write(buffer);
                    System.out.println("第一次  本轮实际写入的字节数" + write);

                    // 3. 判断是否有剩余内容
                    if (buffer.hasRemaining()) {
                        // 4. 关注可写事件   在37行特地还在sckey上模拟关注了读事件 为了避免覆盖  用+或|把两者拼接 代表可读可写
                        sckey.interestOps(sckey.interestOps() + SelectionKey.OP_WRITE);
                        //sckey.interestOps(sckey.interestOps() | SelectionKey.OP_WRITE);
                        // 5. 把未写完的数据挂到 sckey 上
                        sckey.attach(buffer);
                    }
                } else if (key.isWritable()) {
                    ByteBuffer buffer = (ByteBuffer) key.attachment();
                    SocketChannel sc = (SocketChannel) key.channel();
                    int write = sc.write(buffer);
                    System.out.println("客户端缓存满了  进入elseif继续写入  本轮实际写入的字节数" + write);
                    // 6. 清理buffer
                    if (!buffer.hasRemaining()) {
                        key.attach(null); // buffer空了  需要清除buffer
                        key.interestOps(key.interestOps() - SelectionKey.OP_WRITE);//buffer空了   不需关注可写事件
                    }
                }
            }
        }
    }
}

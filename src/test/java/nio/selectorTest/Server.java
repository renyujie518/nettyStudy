package nio.selectorTest;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;

import static nio.ByteBufferUtil.debugAll;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName Server.java
 * @Description TODO
 * @createTime 2022年03月29日 16:53:00
 */
@Slf4j
public class Server {
    public static void main(String[] args) throws IOException {
        /** 1.创建 selector, 管理多个 channel **/
        Selector selector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        //socket通道设置为非阻塞模式
        ssc.configureBlocking(false);
        /** 2. 建立 selector 和 channel 的联系（注册）**/
        // SelectionKey 就是将来事件发生后，通过它可以知道事件和哪个channel的事件  这里设置sscKey只关注 accept 事件
        SelectionKey sscKey = ssc.register(selector, 0, null);
        sscKey.interestOps(SelectionKey.OP_ACCEPT);
        log.debug("ServerSocketChannel注册到selector后的sscKey:  {}", sscKey);
        /** 3.绑定监听端口 **/
        ssc.bind(new InetSocketAddress(8080));
        while (true) {
            /** 4. select 方法, 没有事件发生，线程阻塞，有事件，线程才会恢复运行(不会让这个whiletrue一直白白占用cpu资源)
             select 在事件(针对key,即channel.accept()或者channel.read())未处理时，它不会阻塞
             事件发生后要么处理，要么取消(key.cancel())，不能置之不理**/
            selector.select();

            /** 5.处理事件, selectedKeys 内部包含了所有发生的事件  使用迭代器 因为在遍历的时候可能会对遍历元素操作 **/
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                log.debug("keyFromIter: {}  ", key);
                // 处理key 时，要从 selectedKeys 集合中删除，否则下次处理还会遍历到这个key ，还会尝试梳理这key的事件 而实际上这个事件之前消费过了  就会空指针异常
                iter.remove();
                /** 6.区分事件类型 accept   read**/
                if (key.isAcceptable()) {
                    /** 6.1    accept类型  将ServerSocketChannel取出来注册到selectors（一个就够了）上  但此时的模式是read **/
                    //由于在第二步的时候  实际上只注册了ServerSocketChannel 所以这里强转为
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);

                    ByteBuffer initBuffer = ByteBuffer.allocate(16);
                    SelectionKey scKey = sc.register(selector, 0, initBuffer);
                    scKey.interestOps(SelectionKey.OP_READ);
                    log.debug("执行accept后的SocketChannel+{}", sc);
                    log.debug("再往这个SocketChannel注册read模式后的scKey:{}", scKey);

                } else if (key.isReadable()) {
                    /** 6.2    read类型  获取SocketChannel  利用read方法读 **/
                    try {
                        log.debug("readable下获取的Key:{}", key);
                        // 拿到触发事件的channel
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        log.debug("readable下的socketChannel+{}", socketChannel);

                        //处理边界问题并扩容
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        // 如果客户端是正常断开，read 的方法的返回值是 -1
                        int read = socketChannel.read(buffer);
                        if(read == -1) {
                            key.cancel();
                        } else {
                            split(buffer);
                            if (buffer.position() == buffer.limit()) {
                                ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                                buffer.flip();
                                newBuffer.put(buffer);
                                key.attach(newBuffer);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        // 假如客户端宕机了,因此需要将 key 取消（从 selector 的 keys 集合中真正删除 key）
                        key.cancel();
                    }
                }
            }
        }
    }

    /**
     * @description 数据之间使用 \n 进行分隔
     */
    private static void split(ByteBuffer source) {
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            // 找到一条完整消息
            if (source.get(i) == '\n') {
                int length = i + 1 - source.position();
                // 把这条完整消息存入新的 ByteBuffer
                ByteBuffer target = ByteBuffer.allocate(length);
                // 从 source 读，向 target 写
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                debugAll(target);
                System.out.println(Charset.defaultCharset().decode(target));
            }
        }
        source.compact(); // 0123456789abcdef  position 16 limit 16
    }
}

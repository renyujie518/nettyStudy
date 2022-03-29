package nio.selectorTest;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static nio.ByteBufferUtil.debugAll;

@Slf4j
public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        Thread.currentThread().setName("boss");
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector boss = Selector.open();
        SelectionKey bossKey = ssc.register(boss, 0, null);
        bossKey.interestOps(SelectionKey.OP_ACCEPT);
        ssc.bind(new InetSocketAddress(8088));
        // 1. 创建固定数量的 worker 并初始化  cpu的核心数
        Worker[] workers = new Worker[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker("worker-" + i);
        }
        AtomicInteger index = new AtomicInteger();
        while(true) {
            boss.select();
            Iterator<SelectionKey> iter = boss.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    log.debug("connected...{}", sc.getRemoteAddress());
                    // 2. 关联 selector
                    log.debug("before register...{}", sc.getRemoteAddress());
                    // round robin 轮询
                    // boss线程中 调用register2Channel方法达到初始化worker的目的 即open selector , run worker-0线程
                    workers[index.getAndIncrement() % workers.length].register2Channel(sc);
                    log.debug("after register...{}", sc.getRemoteAddress());
                }
            }
        }
    }
    static class Worker implements Runnable{
        private Thread thread;
        private Selector selector;
        private String name;
        private volatile boolean start = false; // 还未初始化
        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        public Worker(String name) {
            this.name = name;
        }

        // 初始化线程并开启， 打开selector并唤醒
        public void register2Channel(SocketChannel sc) throws IOException {
            if(!start) {
                //设置标志位  只在第一次初始化的时候 初始化线程和打开selector
                selector = Selector.open();
                thread = new Thread(this, name);
                thread.start();
                start = true;
            }
            //不管是不是第一次执行register 都唤醒 selector
            //wakeup()是一种一次性方法  只要selector监听到事件的发生 不论唤醒是在sc.register前还是后  都会被"检票"
            selector.wakeup();
            //注意！！！ sc是从boss线程中传来的  所以可能会发生select()方法由于阻塞无法监听读事件  wakeup就是解决此的
            sc.register(selector, SelectionKey.OP_READ, null);
        }

        @Override
        public void run() {
            while(true) {
                try {
                    //select()方法, 没有事件发生，线程阻塞，有事件，线程才会恢复运行
                    selector.select(); // 一旦进入run了  就是在worker-0 线程中 但由于在进入run()前wakeup()过 所以一定会正确监听读事件
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        //只负责读
                        if (key.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocate(16);
                            SocketChannel channel = (SocketChannel) key.channel();
                            log.debug("read...{}", channel.getRemoteAddress());
                            channel.read(buffer);
                            buffer.flip();
                            debugAll(buffer);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

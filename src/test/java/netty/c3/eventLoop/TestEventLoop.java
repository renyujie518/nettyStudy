package netty.c3.eventLoop;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.NettyRuntime;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
/**
 * @description 学习EventLoop
 * group.next()的真正作用是得到一个具体的线程  这个线程可以用作异步去execute/submit执行其他任务   也可以执行一个定时任务
 */
@Slf4j
public class TestEventLoop {
    public static void main(String[] args) {
        // 1. 创建事件循环组（默认是电脑cpu核心数*2）这里指定为2
        EventLoopGroup group = new NioEventLoopGroup(2); // io 事件
        System.out.println("本机的cpu核心数"+ NettyRuntime.availableProcessors());//8
        //EventLoopGroup group = new DefaultEventLoopGroup(); // 普通任务，定时任务
        // 2. 获取下一个事件循环对象 类似轮询 由于值设定了2个线程  所以这里的第三次.next会重新得到第一个thread
        System.out.println(group.next());
        System.out.println(group.next());
        System.out.println(group.next());
        System.out.println(group.next());

        // 3. 执行普通任务(在执行异步线程时使用)
        group.next().execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("这是普通异步任务");
        });

        // 4. 执行定时任务  初始时间0  1s执行一次
        group.next().scheduleAtFixedRate(() -> {
            log.debug("这是定时任务");
        }, 0, 1, TimeUnit.SECONDS);

        log.debug("main");
    }
}

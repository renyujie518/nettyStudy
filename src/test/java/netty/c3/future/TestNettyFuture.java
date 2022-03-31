package netty.c3.future;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Slf4j
public class TestNettyFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        EventLoop eventLoop = group.next();
        Future<Integer> future = eventLoop.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                log.debug("现在有一段复杂运算，执行计算需要1s");
                Thread.sleep(1000);
                return 70;
            }
        });
        //1.同步的方式
        //log.debug("等待结果");
        //log.debug("同步处理 由main线程来接收结果：{}", future.get());

        //2.异步的方式
        future.addListener(new GenericFutureListener<Future<? super Integer>>(){
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                log.debug("异步处理 由call()执行的那个线程来接收结果:{}", future.getNow());
            }
        });
    }
}

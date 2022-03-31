package netty.c3.future;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class TestJdkFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 1. 线程池
        ExecutorService service = Executors.newFixedThreadPool(2);
        // 2. 提交任务
        Future<Integer> future = service.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                log.debug("现在有一段复杂运算，执行计算需要1s");
                Thread.sleep(1000);
                return 50;
            }
        });
        // 3. 主线程通过 future 来获取结果
        log.debug("等待结果 内部过程是get()方法阻塞  直到call()方法返回值被取到");
        log.debug("结果是 {}", future.get());
        log.debug("get()取到值  结束阻塞等待 唤醒main ");
    }
}

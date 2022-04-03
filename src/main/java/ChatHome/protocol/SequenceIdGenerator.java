package ChatHome.protocol;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description 为保持一次完整的rpc调用  服务器要给client返回response的时候要保证收发过程中 的SequenceId一致
 * 这里自增id
 */
public abstract class SequenceIdGenerator {
    private static final AtomicInteger id = new AtomicInteger();

    public static int nextId() {
        return id.incrementAndGet();
    }
}

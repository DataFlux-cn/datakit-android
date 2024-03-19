package com.ft.sdk.garble.threadpool;

/**
 * 负责缓存日志数据的消费
 */
public class LogConsumerThreadPool extends BaseThreadPool {
    private final static int CORE_POOL_SIZE = 1;

    private static LogConsumerThreadPool threadPoolUtils;


    private LogConsumerThreadPool() {
        super(CORE_POOL_SIZE, "FTLogCsr", 4);
    }


    public static LogConsumerThreadPool get() {
        synchronized (LogConsumerThreadPool.class) {
            if (threadPoolUtils == null) {
                threadPoolUtils = new LogConsumerThreadPool();
            }
            return threadPoolUtils;
        }
    }

}

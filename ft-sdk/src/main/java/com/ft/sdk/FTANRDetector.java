package com.ft.sdk;

import com.ft.sdk.garble.threadpool.ANRDetectThreadPool;
import com.ft.sdk.internal.anr.ANRDetectRunnable;


/**
 * ANR 事件监测
 */
public class FTANRDetector {


    private static class SingletonHolder {
        private static final FTANRDetector INSTANCE = new FTANRDetector();
    }

    public static FTANRDetector get() {
        return FTANRDetector.SingletonHolder.INSTANCE;
    }

    private ANRDetectRunnable runnable;

    /**
     * 配置初始化
     *
     * @param config
     */
    void init(FTRUMConfig config) {
        if (config.isEnableTrackAppANR()) {
            runnable = new ANRDetectRunnable();
            ANRDetectThreadPool.get().execute(runnable);
        }
    }

    /**
     * 释放 ANR对应 资源
     */
    void release() {
        ANRDetectThreadPool.get().shutDown();
        if(runnable!=null){
            runnable.shutdown();
        }
    }
}

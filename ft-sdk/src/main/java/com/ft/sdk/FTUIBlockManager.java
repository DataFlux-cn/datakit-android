package com.ft.sdk;

import android.view.Choreographer;

import com.ft.sdk.FTAutoTrack;
import com.ft.sdk.FTSDKConfig;
import com.ft.sdk.FTTrackInner;
import com.ft.sdk.garble.FTRUMConfig;
import com.ft.sdk.garble.bean.LogBean;
import com.ft.sdk.garble.bean.Status;
import com.ft.sdk.garble.manager.FTMainLoopLogMonitor;
import com.ft.sdk.garble.utils.Utils;


public class FTUIBlockManager {

    static Choreographer.FrameCallback callback = new Choreographer.FrameCallback() {

        @Override
        public void doFrame(long frameTimeNanos) {

            FTMainLoopLogMonitor.getInstance().removeMonitor();

            FTMainLoopLogMonitor.getInstance().startMonitor();

            if (isStop) return;

            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    private static boolean isStop = true;


    public static void start(FTSDKConfig config) {
        if (!config.isEnableTrackAppUIBlock()) {
            return;
        }

        isStop = false;

        FTMainLoopLogMonitor.getInstance().setLogCallBack(log -> {

            if (FTRUMConfig.get().isRumEnable()) {
                FTAutoTrack.PutRUMuiBlock(log);

            } else {
                LogBean logBean = new LogBean("------ UIBlock  ------\n " + log, Utils.getCurrentNanoTime());
                logBean.setStatus(Status.CRITICAL);
                logBean.setEnv(config.getEnv());
                logBean.setServiceName(config.getServiceName());
                FTTrackInner.getInstance().logBackground(logBean);
            }

        });


        Choreographer.getInstance().postFrameCallback(callback);
    }

    public static void release() {
        FTMainLoopLogMonitor.release();
        isStop = true;
    }
}
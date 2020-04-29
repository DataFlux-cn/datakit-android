package com.ft.sdk.garble.service;

import com.ft.sdk.garble.FTMonitorConfig;
import com.ft.sdk.garble.http.HttpBuilder;
import com.ft.sdk.garble.http.RequestMethod;
import com.ft.sdk.garble.http.ResponseData;
import com.ft.sdk.garble.manager.SyncDataManager;
import com.ft.sdk.garble.utils.Constants;
import com.ft.sdk.garble.utils.LogUtils;

import java.net.HttpURLConnection;

/**
 * create: by huangDianHua
 * time: 2020/4/28 15:00:02
 * description:
 */
public class FTMonitorManager {
    //轮训周期
    private int period = 10;
    private static FTMonitorManager instance;
    private MonitorThread mThread;

    private FTMonitorManager() {
    }

    public static FTMonitorManager install(int period) {
        if (instance == null) {
            instance = new FTMonitorManager();
        }
        instance.period = period;
        instance.stopMonitor();
        instance.startMonitor();
        return instance;
    }

    public static FTMonitorManager get() {
        return instance;
    }

    /**
     * 开启监控
     */
    public void startMonitor() {
        if(FTMonitorConfig.get().getMonitorType() == 0){
            LogUtils.e("没有设置监控项，无法启用监控");
        }else {
            mThread = new MonitorThread("监控轮训", period);
            mThread.start();
            LogUtils.d("监控轮训线程启动...");
        }
    }

    /**
     * 停止监控
     */
    private void stopMonitor() {
        if (mThread != null && mThread.isAlive()) {
            LogUtils.d("关闭监控轮训线程");
            mThread.interrupt();
            mThread = null;
        }
    }

    public void release() {
        stopMonitor();
        instance = null;
    }

    static class MonitorThread extends Thread {
        //轮训周期
        private int period;

        public MonitorThread(String name, int period) {
            setName(name);
            this.period = period;
        }

        @Override
        public void run() {
            super.run();
            try {
                while (true) {
                    Thread.sleep(period * 1000);
                    try {
                        String body = SyncDataManager.getMonitorUploadData();
                        SyncDataManager.printUpdateData(body);
                        body = body.replaceAll(Constants.SEPARATION_PRINT,Constants.SEPARATION);
                        ResponseData result = HttpBuilder.Builder()
                                .setMethod(RequestMethod.POST)
                                .setBodyString(body).executeSync(ResponseData.class);
                        if (result.getHttpCode() != HttpURLConnection.HTTP_OK) {
                            LogUtils.d("监控轮训线程上传数据出错(message：" + result.getData() + ")");
                        }
                    } catch (Exception e) {
                        LogUtils.d("监控轮训线程执行错误(message：" + e.getMessage() + ")");
                    }
                }
            } catch (InterruptedException e) {
                LogUtils.d("监控轮训线程被关闭");
            }
        }
    }
}
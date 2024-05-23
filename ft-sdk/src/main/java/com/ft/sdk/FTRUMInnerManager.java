package com.ft.sdk;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ft.sdk.garble.bean.ActionBean;
import com.ft.sdk.garble.bean.ActiveActionBean;
import com.ft.sdk.garble.bean.ActiveViewBean;
import com.ft.sdk.garble.bean.AppState;
import com.ft.sdk.garble.bean.ErrorSource;
import com.ft.sdk.garble.bean.ErrorType;
import com.ft.sdk.garble.bean.NetStatusBean;
import com.ft.sdk.garble.bean.ResourceBean;
import com.ft.sdk.garble.bean.ResourceParams;
import com.ft.sdk.garble.bean.ViewBean;
import com.ft.sdk.garble.db.FTDBManager;
import com.ft.sdk.garble.threadpool.EventConsumerThreadPool;
import com.ft.sdk.garble.threadpool.RunnerCompleteCallBack;
import com.ft.sdk.garble.utils.BatteryUtils;
import com.ft.sdk.garble.utils.Constants;
import com.ft.sdk.garble.utils.DeviceUtils;
import com.ft.sdk.garble.utils.LogUtils;
import com.ft.sdk.garble.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HttpsURLConnection;

/**
 * RUM 数据管理，记录 Action，View，LongTask，Error，并统计
 * {@link Constants#KEY_RUM_VIEW_ACTION_COUNT }
 * {@link Constants#KEY_RUM_ACTION_ERROR_COUNT}
 * ，可以通过<a href="https://docs.guance.com/real-user-monitoring/explorer/">查看器</a>
 */
public class FTRUMInnerManager {
    private static final String TAG = Constants.LOG_TAG_PREFIX + "RUMGlobalManager";
    /**
     * 间断操作（中途休眠） Session 重置事件为 15分钟
     */
    static final long MAX_RESTING_TIME = 900000000000L;
    /**
     * 持续 Session 最大重置事件，4小时
     */
    static final long SESSION_EXPIRE_TIME = 14400000000000L;
//    static final long SESSION_EXPIRE_TIME = 5000000000L;//5秒
    /**
     * Session 最大存储数值
     */
    static final long SESSION_FILTER_CAPACITY = 5;
    private final ConcurrentHashMap<String, ResourceBean> resourceBeanMap = new ConcurrentHashMap<>();

    private final ArrayList<String> viewList = new ArrayList<>();

    private FTRUMInnerManager() {

    }

    private static class SingletonHolder {
        private static final FTRUMInnerManager INSTANCE = new FTRUMInnerManager();
    }

    public static FTRUMInnerManager get() {
        return FTRUMInnerManager.SingletonHolder.INSTANCE;
    }

    /**
     * {@link Constants#KEY_RUM_SESSION_ID}
     */
    private String sessionId = Utils.randomUUID();


    /**
     * 不收集
     */
    private final ArrayList<String> notCollectMap = new ArrayList<>();

    /**
     * 当前激活 View
     */
    private ActiveViewBean activeView;

    /**
     * 当前激活 Action
     */
    private ActiveActionBean activeAction;


    /**
     * 记录 activity 的创建时，消耗的时间
     */
    private final ConcurrentHashMap<String, Long> preActivityDuration = new ConcurrentHashMap<>();

    /**
     * 最近 Session 时间，单位纳秒
     */
    private long lastSessionTime = Utils.getCurrentNanoTime();

    /**
     * 最近 Action 时间
     * <p>
     * 注意 ：AndroidTest 会调用这个方法 {@link com.ft.test.base.FTBaseTest#setSessionExpire()}
     */
    private long lastActionTime = lastSessionTime;

    /**
     * 采样率，{@link FTRUMConfig#samplingRate}
     */
    private float sampleRate = 1f;

    String getSessionId() {
        return sessionId;
    }

    /**
     * 检测重置 session_id
     *
     * @return
     */
    private void checkSessionRefresh(boolean checkRefreshView) {
        long now = Utils.getCurrentNanoTime();
        boolean longResting = now - lastActionTime > MAX_RESTING_TIME;
        boolean longTimeSession = now - lastSessionTime > SESSION_EXPIRE_TIME;
        if (longTimeSession || longResting) {
            lastSessionTime = now;

            sessionId = Utils.randomUUID();
            checkSessionKeep(sessionId, sampleRate);

            if (checkRefreshView) {
                if (activeView != null && !activeView.isClose()) {
                    activeView.close();
                    closeView(activeView);

                    String viewName = activeView.getViewName();
                    String viewReferrer = activeView.getViewReferrer();
                    HashMap<String, Object> property = activeView.getProperty();
                    long loadTime = activeView.getLoadTime();

                    activeView = new ActiveViewBean(viewName, viewReferrer, loadTime, sessionId);
                    if (property != null) {
                        activeView.getProperty().putAll(property);
                    }
                    FTMonitorManager.get().addMonitor(activeView.getId());
                    FTMonitorManager.get().attachMonitorData(activeView);
                    initView(activeView);
                    LogUtils.d(TAG, "New sessionId:" + activeView.getSessionId() + ",viewId:" + activeView.getId());
                }
            }

        }
    }

    private String getActionId() {
        return activeAction == null ? null : activeAction.getId();
    }

    void addAction(String actionName, String actionType, long duration, long startTime) {
        String viewId = activeView != null ? activeView.getId() : null;
        String viewName = activeView != null ? activeView.getViewName() : null;
        String viewReferrer = activeView != null ? activeView.getViewReferrer() : null;
        checkSessionRefresh(true);

        ActiveActionBean activeAction = new ActiveActionBean(actionName, actionType,
                sessionId, viewId, viewName, viewReferrer, false);
        activeAction.setClose(true);
        activeAction.setDuration(duration);
        activeAction.setStartTime(startTime);
        initAction(activeAction);
        this.lastActionTime = activeAction.getStartTime();
    }

    /**
     * 添加 action
     *
     * @param actionName action 名称
     * @param actionType action 类型
     */
    void startAction(String actionName, String actionType) {
        startAction(actionName, actionType, false);
    }

    /**
     * 添加 action
     *
     * @param actionName action 名称
     * @param actionType action 类型
     * @param property   附加属性参数
     */
    void startAction(String actionName, String actionType, HashMap<String, Object> property) {
        startAction(actionName, actionType, false, property);
    }

    /**
     * 添加 action
     *
     * @param actionName action 名称
     * @param actionType action 类型
     * @param needWait   是否需要等待
     */
    void startAction(String actionName, String actionType, boolean needWait) {
        startAction(actionName, actionType, needWait, null);
    }

    /**
     * action 起始
     *
     * @param actionName action 名称
     * @param actionType action 类型
     * @param needWait   是否需要等待
     * @param property   附加属性参数
     */
    void startAction(String actionName, String actionType, boolean needWait, HashMap<String, Object> property) {

        String viewId = activeView != null ? activeView.getId() : null;
        String viewName = activeView != null ? activeView.getViewName() : null;
        String viewReferrer = activeView != null ? activeView.getViewReferrer() : null;
        checkSessionRefresh(true);
        checkActionClose();
        if (activeAction == null || activeAction.isClose()) {
            activeAction = new ActiveActionBean(actionName, actionType, sessionId, viewId, viewName, viewReferrer, needWait);
            if (property != null) {
                activeAction.getProperty().putAll(property);
            }
            initAction(activeAction);
            this.lastActionTime = activeAction.getStartTime();

            mHandler.removeCallbacks(mActionRecheckRunner);
            mHandler.postDelayed(mActionRecheckRunner, 5000);
        }
    }

    /**
     * 检测 action 是否需要关闭
     */
    private void checkActionClose() {
        if (activeAction == null) return;
        long now = Utils.getCurrentNanoTime();
        long lastActionTime = activeAction.getStartTime();
        boolean waiting = activeAction.isNeedWaitAction() && (activeView != null && !activeView.isClose());
        boolean timeOut = now - lastActionTime > ActiveActionBean.ACTION_NEED_WAIT_TIME_OUT;
        boolean needClose = !waiting
                && (now - lastActionTime > ActiveActionBean.ACTION_NORMAL_TIME_OUT)
                || timeOut || (activeView != null && !activeView.getId().equals(activeAction.getViewId()));
        if (needClose) {
            if (!activeAction.isClose()) {
                activeAction.close();
                closeAction(activeAction, timeOut);
            }
        }
    }

    /**
     * 停止 action
     */
    void stopAction() {
        if (activeAction.isNeedWaitAction()) {
            activeAction.close();
        }
    }

    /**
     * resource 起始
     *
     * @param resourceId 资源 Id
     */
    void startResource(String resourceId) {
        startResource(resourceId, null);
    }

    /**
     * resource 起始
     *
     * @param resourceId 资源 Id
     */
    void startResource(String resourceId, HashMap<String, Object> property) {
        LogUtils.d(TAG, "startResource:" + resourceId);
        checkSessionRefresh(true);
        ResourceBean bean = new ResourceBean();
        if (property != null) {
            bean.property.putAll(property);
        }
        attachRUMRelativeForResource(bean);
        synchronized (resourceBeanMap) {
            resourceBeanMap.put(resourceId, bean);
        }
        final String actionId = bean.actionId;
        final String viewId = bean.viewId;
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().increaseViewPendingResource(viewId);
                FTDBManager.get().increaseActionPendingResource(actionId);
            }
        });
    }

    /**
     * resource 终止
     *
     * @param resourceId 资源 Id
     */
    void stopResource(String resourceId) {
        LogUtils.d(TAG, "stopResource:" + resourceId);
        stopResource(resourceId, null);
    }

    /**
     * @param resourceId
     * @param property   附加属性参数
     */
    void stopResource(final String resourceId, HashMap<String, Object> property) {
        ResourceBean bean = resourceBeanMap.get(resourceId);
        if (bean != null) {
            if (property != null) {
                bean.property.putAll(property);
            }
            final String actionId = bean.actionId;
            final String viewId = bean.viewId;
            bean.endTime = Utils.getCurrentNanoTime();
            increaseResourceCount(viewId, actionId);
            EventConsumerThreadPool.get().execute(new Runnable() {
                @Override
                public void run() {
                    FTDBManager.get().reduceViewPendingResource(viewId);
                    FTDBManager.get().updateViewUpdateTime(viewId, System.currentTimeMillis());
                    FTDBManager.get().reduceActionPendingResource(actionId);
                    FTTraceManager.get().removeByStopResource(resourceId);
                }
            });
        }
    }

    /**
     * 创建 view
     *
     * @param viewName 界面名称
     * @param loadTime 加载事件，单位毫秒 ms
     */
    void onCreateView(String viewName, long loadTime) {
        preActivityDuration.put(viewName, loadTime);
    }


    /**
     * view 起始
     *
     * @param viewName 当前页面名称
     */
    void startView(String viewName) {
        startView(viewName, null);
    }

    /**
     * view 起始
     *
     * @param viewName 当前页面名称
     * @param property 附加属性参数
     */
    void startView(String viewName, HashMap<String, Object> property) {
        if (viewList.isEmpty() || !viewList.get(viewList.size() - 1).equals(viewName)) {
            viewList.add(viewName);
            if (viewList.size() > 2) {
                viewList.remove(0);
            }
        }

        checkSessionRefresh(false);
        if (activeView != null && !activeView.isClose()) {
            activeView.close();
            closeView(activeView);
        }


        long loadTime = -1;
        if (preActivityDuration.get(viewName) != null) {
            loadTime = preActivityDuration.get(viewName);
            preActivityDuration.remove(viewName);
        }
        String viewReferrer = getLastView();
        activeView = new ActiveViewBean(viewName, viewReferrer, loadTime, sessionId);
        if (property != null) {
            activeView.getProperty().putAll(property);
        }
        FTMonitorManager.get().addMonitor(activeView.getId());
        FTMonitorManager.get().attachMonitorData(activeView);
        initView(activeView);

    }

    /**
     * 获取上一页面
     *
     * @return
     */
    String getLastView() {
        LogUtils.d(TAG, "getLastView:" + viewList);
        if (viewList.size() > 1) {
            String viewName = viewList.get(viewList.size() - 2);
            if (viewName != null) {
                return viewName;
            } else {
                return Constants.VIEW_NAME_ROOT;
            }
        } else {
            return Constants.VIEW_NAME_ROOT;
        }
    }

    /**
     * view 结束
     */
    void stopView() {
        stopView(null, null);
    }

    /**
     * view 结束
     *
     * @param property 附加属性参数
     */
    void stopView(HashMap<String, Object> property, RunnerCompleteCallBack callBack) {
        if (activeView == null) {
            if (callBack != null) {
                callBack.onComplete();
            }
            return;
        }
        checkActionClose();
        if (property != null) {
            activeView.getProperty().putAll(property);
        }
        FTMonitorManager.get().attachMonitorData(activeView);
        FTMonitorManager.get().removeMonitor(activeView.getId());
        activeView.close();
        closeView(activeView, callBack);
    }

    /**
     * 初始化 action
     *
     * @param activeActionBean 当前激活的操作
     */
    private void initAction(ActiveActionBean activeActionBean) {
        final ActionBean bean = activeActionBean.convertToActionBean();
        increaseAction(bean.getViewId());
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().initSumAction(bean);
            }
        });

    }

    /**
     * 初始化 view
     *
     * @param activeViewBean 当前激活的页面
     */
    private void initView(ActiveViewBean activeViewBean) {
        final ViewBean bean = activeViewBean.convertToViewBean();
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().initSumView(bean);
            }
        });
    }

    /**
     * 增加 Resource 数量
     * {@link Constants#FT_MEASUREMENT_RUM_ACTION} 数据增加 {@link Constants#KEY_RUM_ACTION_RESOURCE_COUNT}
     * {@link Constants#FT_MEASUREMENT_RUM_VIEW} 数据增加 {@link Constants#KEY_RUM_VIEW_ERROR_COUNT}
     *
     * @param viewId   view 唯一 id
     * @param actionId action 唯一 id
     */
    private void increaseResourceCount(final String viewId, final String actionId) {
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().increaseViewResource(viewId);
                FTDBManager.get().increaseActionResource(actionId);
                FTRUMInnerManager.this.checkActionClose();
            }
        });

    }


    /**
     * 增加 Error 数量
     * {@link Constants#FT_MEASUREMENT_RUM_ACTION} 数据增加 {@link Constants#KEY_RUM_ACTION_ERROR_COUNT}
     * {@link Constants#FT_MEASUREMENT_RUM_VIEW} 数据增加 {@link Constants#KEY_RUM_VIEW_ERROR_COUNT}
     *
     * @param tags
     */
    private void increaseError(JSONObject tags) {

        final String actionId = tags.optString(Constants.KEY_RUM_ACTION_ID);
        final String viewId = tags.optString(Constants.KEY_RUM_VIEW_ID);
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().increaseActionError(actionId);
                FTDBManager.get().increaseViewError(viewId);
                FTRUMInnerManager.this.checkActionClose();
            }
        });
    }

    /**
     * 添加错误信息
     *
     * @param log       日志
     * @param message   消息
     * @param errorType 错误类型
     * @param state     程序运行状态
     */
    void addError(String log, String message, String errorType, AppState state) {
        addError(log, message, Utils.getCurrentNanoTime(), errorType, state, null);
    }


    /**
     * 添加错误信息
     *
     * @param log       日志
     * @param message   消息
     * @param errorType 错误类型
     * @param state     程序运行状态
     * @param property  附加属性
     */
    void addError(String log, String message, String errorType, AppState state, HashMap<String, Object> property) {
        addError(log, message, Utils.getCurrentNanoTime(), errorType, state, property, null);
    }

    /**
     * 添加错误
     *
     * @param log       日志
     * @param message   消息
     * @param errorType 错误类型
     * @param state     程序运行状态
     * @param dateline  发生时间，纳秒
     * @param callBack
     */
    void addError(String log, String message, long dateline, String errorType, AppState state, RunnerCompleteCallBack callBack) {
        addError(log, message, dateline, errorType, state, null, callBack);
    }

    /**
     * 添加错误
     *
     * @param log       日志
     * @param message   消息
     * @param errorType 错误类型
     * @param state     程序运行状态
     * @param dateline  发生时间，纳秒
     * @param callBack  线程池调用结束回调
     */
    public void addError(String log, String message, long dateline, String errorType,
                         AppState state, HashMap<String, Object> property, RunnerCompleteCallBack callBack) {

        try {
            checkSessionRefresh(true);

            final JSONObject tags = FTRUMConfigManager.get().getRUMPublicDynamicTags();
            attachRUMRelative(tags, true);
            final JSONObject fields = new JSONObject();
            tags.put(Constants.KEY_RUM_ERROR_TYPE, errorType);
            tags.put(Constants.KEY_RUM_ERROR_SOURCE, ErrorSource.LOGGER.toString());
            tags.put(Constants.KEY_RUM_ERROR_SITUATION, state.toString());

            if (property != null) {
                for (Map.Entry<String, Object> entry : property.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    fields.put(key, value);
                }
            }

            fields.put(Constants.KEY_RUM_ERROR_MESSAGE, message);
            fields.put(Constants.KEY_RUM_ERROR_STACK, log);

            EventConsumerThreadPool.get().execute(new Runnable() {
                @Override
                public void run() {
                    try {

                        //---- 获取设备信息，资源耗时操作----->
                        tags.put(Constants.KEY_DEVICE_LOCALE, Locale.getDefault());
                        tags.put(Constants.KEY_DEVICE_CARRIER, DeviceUtils.getCarrier(FTApplication.getApplication()));

                        if (FTMonitorManager.get().isErrorMonitorType(ErrorMonitorType.MEMORY)) {
                            double[] memory = DeviceUtils.getRamData(FTApplication.getApplication());
                            tags.put(Constants.KEY_MEMORY_TOTAL, memory[0] + "GB");
                            fields.put(Constants.KEY_MEMORY_USE, memory[1]);
                        }

                        if (FTMonitorManager.get().isErrorMonitorType(ErrorMonitorType.CPU)) {
                            fields.put(Constants.KEY_CPU_USE, DeviceUtils.getCpuUsage());
                        }
                        if (FTMonitorManager.get().isErrorMonitorType(ErrorMonitorType.BATTERY)) {
                            fields.put(Constants.KEY_BATTERY_USE, (float) BatteryUtils.getBatteryInfo(FTApplication.getApplication()).getBr());

                        }
                        //<--------
                        FTTrackInner.getInstance().rum(dateline, Constants.FT_MEASUREMENT_RUM_ERROR, tags, fields, new RunnerCompleteCallBack() {
                            @Override
                            public void onComplete() {
                                increaseError(tags);
                                if (callBack != null) {
                                    // Java Crash，Native Crash 需要记录当下状态的崩溃
                                    stopView(null, callBack);
                                }
                            }
                        });

                    } catch (Exception e) {
                        LogUtils.e(TAG, Log.getStackTraceString(e));
                    }
                }
            });

        } catch (Exception e) {
            LogUtils.e(TAG, Log.getStackTraceString(e));
        }
    }

    /**
     * 添加长任务
     *
     * @param log      日志内容
     * @param duration 持续时间，纳秒
     */
    void addLongTask(String log, long duration, HashMap<String, Object> property) {
        try {
            checkSessionRefresh(true);
            JSONObject tags = FTRUMConfigManager.get().getRUMPublicDynamicTags();
            attachRUMRelative(tags, true);
            JSONObject fields = new JSONObject();
            fields.put(Constants.KEY_RUM_LONG_TASK_DURATION, duration);
            fields.put(Constants.KEY_RUM_LONG_TASK_STACK, log);

            if (property != null) {
                for (Map.Entry<String, Object> entry : property.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    fields.put(key, value);
                }
            }

            FTTrackInner.getInstance().rum(Utils.getCurrentNanoTime() - duration, Constants.FT_MEASUREMENT_RUM_LONG_TASK, tags, fields, null);
            increaseLongTask(tags);

        } catch (Exception e) {
            LogUtils.e(TAG, Log.getStackTraceString(e));
        }
    }

    /**
     * 添加长任务
     *
     * @param log      日志内容
     * @param duration 持续时间，纳秒
     */
    void addLongTask(String log, long duration) {
        addLongTask(log, duration, null);
    }

    /**
     * 传输网络连接指标参数
     *
     * @param resourceId
     * @param netStatusBean
     */
    void setNetState(String resourceId, NetStatusBean netStatusBean) {
        LogUtils.d(TAG, "setNetState:" + resourceId);

        ResourceBean bean = resourceBeanMap.get(resourceId);

        if (bean == null) {
            LogUtils.e(TAG, "setNetState:" + resourceId + ",bean null");
            return;
        }
        bean.resourceDNS = netStatusBean.getDNSTime();
        bean.resourceSSL = netStatusBean.getSSLTime();
        bean.resourceTCP = netStatusBean.getTcpTime();

        bean.resourceTrans = netStatusBean.getResponseTime();
        bean.resourceTTFB = netStatusBean.getTTFB();
        long resourceLoad = netStatusBean.getHoleRequestTime();
        bean.resourceLoad = resourceLoad > 0 ? resourceLoad : bean.endTime - bean.startTime;
        bean.resourceFirstByte = netStatusBean.getFirstByteTime();
        bean.netStateSet = true;
        checkToAddResource(resourceId, bean);
    }

    /**
     * 资源加载性能
     *
     * @param resourceId 资源 id
     */
    void putRUMResourcePerformance(final String resourceId) {
        LogUtils.d(TAG, "putRUMResourcePerformance:" + resourceId);
        ResourceBean bean = resourceBeanMap.get(resourceId);

        if (bean == null) {
            LogUtils.e(TAG, "putRUMResourcePerformance:" + resourceId + ",bean null");
            return;
        }
//        if (bean.resourceStatus < HttpsURLConnection.HTTP_OK) {
//            EventConsumerThreadPool.get().execute(new Runnable() {
//                @Override
//                public void run() {
//                    synchronized (resourceBeanMap) {
//                        LogUtils.d(TAG, "net error remove id:" + resourceId);
//                        resourceBeanMap.remove(resourceId);
//                    }
//                    FTTraceManager.get().removeByAddResource(resourceId);
//                }
//            });
//            return;
//        }
        long time = Utils.getCurrentNanoTime();
        String actionId = bean.actionId;
        String viewId = bean.viewId;
        String actionName = bean.actionName;
        String viewName = bean.viewName;
        String viewReferrer = bean.viewReferrer;
        String sessionId = bean.sessionId;

        try {
            JSONObject tags = FTRUMConfigManager.get().getRUMPublicDynamicTags();

            tags.put(Constants.KEY_RUM_ACTION_ID, actionId);
            tags.put(Constants.KEY_RUM_ACTION_NAME, actionName);
            tags.put(Constants.KEY_RUM_VIEW_ID, viewId);
            tags.put(Constants.KEY_RUM_VIEW_NAME, viewName);
            tags.put(Constants.KEY_RUM_VIEW_REFERRER, viewReferrer);
            tags.put(Constants.KEY_RUM_SESSION_ID, sessionId);

            JSONObject fields = new JSONObject();

            tags.put(Constants.KEY_RUM_RESOURCE_URL_HOST, bean.urlHost);

            tags.put(Constants.KEY_RUM_RESOURCE_TYPE, "network");
            tags.put(Constants.KEY_RUM_RESPONSE_CONNECTION, bean.responseConnection);
            tags.put(Constants.KEY_RUM_RESPONSE_CONTENT_TYPE, bean.responseContentType);
            tags.put(Constants.KEY_RUM_RESPONSE_CONTENT_ENCODING, bean.responseContentEncoding);
            tags.put(Constants.KEY_RUM_RESOURCE_METHOD, bean.resourceMethod);
            tags.put(Constants.KEY_RUM_RESOURCE_TRACE_ID, bean.traceId);
            tags.put(Constants.KEY_RUM_RESOURCE_SPAN_ID, bean.spanId);

            int resourceStatus = bean.resourceStatus;
            String resourceStatusGroup = "";
            tags.put(Constants.KEY_RUM_RESOURCE_STATUS, resourceStatus);
            if (resourceStatus > 0) {
                long statusGroupPrefix = bean.resourceStatus / 100;
                resourceStatusGroup = statusGroupPrefix + "xx";
                tags.put(Constants.KEY_RUM_RESOURCE_STATUS_GROUP, resourceStatusGroup);
            }

            if (bean.resourceSize > 0) {
                fields.put(Constants.KEY_RUM_RESOURCE_SIZE, bean.resourceSize);
            }
            if (bean.resourceLoad > 0) {
                fields.put(Constants.KEY_RUM_RESOURCE_DURATION, bean.resourceLoad);
            }

            if (bean.resourceDNS > 0) {
                fields.put(Constants.KEY_RUM_RESOURCE_DNS, bean.resourceDNS);
            }
            if (bean.resourceTCP > 0) {
                fields.put(Constants.KEY_RUM_RESOURCE_TCP, bean.resourceTCP);
            }
            if (bean.resourceSSL > 0) {
                fields.put(Constants.KEY_RUM_RESOURCE_SSL, bean.resourceSSL);
            }
            if (bean.resourceTTFB > 0) {
                fields.put(Constants.KEY_RUM_RESOURCE_TTFB, bean.resourceTTFB);
            }

            if (bean.resourceTrans > 0) {
                fields.put(Constants.KEY_RUM_RESOURCE_TRANS, bean.resourceTrans);
            }

            if (bean.resourceFirstByte > 0) {
                fields.put(Constants.KEY_RUM_RESOURCE_FIRST_BYTE, bean.resourceFirstByte);

            }
            String urlPath = bean.urlPath;
            String urlPathGroup = "";

            if (!urlPath.isEmpty()) {
                urlPathGroup = urlPath.replaceAll("\\/([^\\/]*)\\d([^\\/]*)", "/?");
                tags.put(Constants.KEY_RUM_RESOURCE_URL_PATH, urlPath);
                tags.put(Constants.KEY_RUM_RESOURCE_URL_PATH_GROUP, urlPathGroup);
            }


            tags.put(Constants.KEY_RUM_RESOURCE_URL, bean.url);
            for (Map.Entry<String, Object> entry : bean.property.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                fields.put(key, value);
            }
            fields.put(Constants.KEY_RUM_REQUEST_HEADER, bean.requestHeader);
            fields.put(Constants.KEY_RUM_RESPONSE_HEADER, bean.responseHeader);

            FTTrackInner.getInstance().rum(time,
                    Constants.FT_MEASUREMENT_RUM_RESOURCE, tags, fields, null);


            if (bean.resourceStatus >= HttpsURLConnection.HTTP_BAD_REQUEST) {
                JSONObject errorTags = FTRUMConfigManager.get().getRUMPublicDynamicTags();
                JSONObject errorField = new JSONObject();
                errorTags.put(Constants.KEY_RUM_ERROR_TYPE, ErrorType.NETWORK.toString());
                errorTags.put(Constants.KEY_RUM_ERROR_SOURCE, ErrorSource.NETWORK.toString());
                errorTags.put(Constants.KEY_RUM_ERROR_SITUATION, AppState.RUN.toString());
                errorTags.put(Constants.KEY_RUM_ACTION_ID, actionId);
                errorTags.put(Constants.KEY_RUM_ACTION_NAME, actionName);
                errorTags.put(Constants.KEY_RUM_VIEW_ID, viewId);
                errorTags.put(Constants.KEY_RUM_VIEW_NAME, viewName);
                errorTags.put(Constants.KEY_RUM_VIEW_REFERRER, viewReferrer);
                errorTags.put(Constants.KEY_RUM_SESSION_ID, sessionId);

                if (resourceStatus > 0) {
                    errorTags.put(Constants.KEY_RUM_RESOURCE_STATUS, resourceStatus);
                    errorTags.put(Constants.KEY_RUM_RESOURCE_STATUS_GROUP, resourceStatusGroup);
                }
                errorTags.put(Constants.KEY_RUM_RESOURCE_URL, bean.url);
                errorTags.put(Constants.KEY_RUM_RESOURCE_URL_HOST, bean.urlHost);
                errorTags.put(Constants.KEY_RUM_RESOURCE_METHOD, bean.resourceMethod);

                if (!urlPath.isEmpty()) {
                    errorTags.put(Constants.KEY_RUM_RESOURCE_URL_PATH, urlPath);
                    errorTags.put(Constants.KEY_RUM_RESOURCE_URL_PATH_GROUP, urlPathGroup);
                }
                String errorMsg = "[" + bean.resourceStatus + "]" + "[" + bean.url + "]";

                errorField.put(Constants.KEY_RUM_ERROR_MESSAGE, errorMsg);
                errorField.put(Constants.KEY_RUM_ERROR_STACK, bean.errorStack);

                FTTrackInner.getInstance().rum(time, Constants.FT_MEASUREMENT_RUM_ERROR, errorTags, errorField, new RunnerCompleteCallBack() {
                    @Override
                    public void onComplete() {
                        increaseError(tags);
                    }
                });
            }
        } catch (Exception e) {
            LogUtils.e(TAG, Log.getStackTraceString(e));
        }

        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                synchronized (resourceBeanMap) {
                    LogUtils.d(TAG, "final remove id:" + resourceId);
                    resourceBeanMap.remove(resourceId);
                }
                FTTraceManager.get().removeByAddResource(resourceId);
            }
        });
    }

    /**
     * 设置网络传输内容
     *
     * @param resourceId    资源 id
     * @param params
     * @param netStatusBean
     */
    public void addResource(String resourceId, ResourceParams params, NetStatusBean netStatusBean) {
        setTransformContent(resourceId, params);
        setNetState(resourceId, netStatusBean);
    }

    /**
     * 设置网络传输内容
     *
     * @param resourceId 资源 id
     * @param params
     */
    void setTransformContent(String resourceId, ResourceParams params) {

        FTTraceHandler handler = FTTraceManager.get().getHandler(resourceId);
        String spanId = "";
        String traceId = "";
        if (handler != null) {
            spanId = handler.getSpanID();
            traceId = handler.getTraceID();
        }

        ResourceBean bean = resourceBeanMap.get(resourceId);

        if (bean == null) {
            LogUtils.d(TAG, "setTransformContent bean null");
            return;
        }

//        if (params.resourceStatus < HttpsURLConnection.HTTP_OK) {
//            LogUtils.d(TAG, "setTransformContent code < 200");
//            return;
//        }
        try {
            URL url = Utils.parseFromUrl(params.url);
            bean.url = params.url;
            bean.urlHost = url.getHost();
            bean.urlPath = url.getPath();
            bean.resourceUrlQuery = url.getQuery();

        } catch (MalformedURLException | URISyntaxException e) {
            LogUtils.e(TAG, Log.getStackTraceString(e));
        }

        bean.requestHeader = params.requestHeader;
        bean.responseHeader = params.responseHeader;
        bean.responseContentType = params.responseContentType;
        bean.responseConnection = params.responseConnection;
        bean.resourceMethod = params.resourceMethod;
        bean.responseContentEncoding = params.responseContentEncoding;
        bean.resourceType = bean.responseContentType;
        bean.resourceStatus = params.resourceStatus;
        bean.resourceSize = params.responseContentLength;
        if (bean.resourceStatus >= HttpsURLConnection.HTTP_BAD_REQUEST) {
            bean.errorStack = params.responseBody == null ? "" : params.responseBody;
        }

        if (params.property != null) {
            bean.property.putAll(params.property);
        }

        if (FTTraceConfigManager.get().isEnableLinkRUMData()) {
            bean.traceId = traceId;
            bean.spanId = spanId;
        }
        bean.contentSet = true;
        checkToAddResource(resourceId, bean);
    }


    /**
     * LongTask 自增
     * {@link Constants#KEY_RUM_ACTION_LONG_TASK_COUNT}
     * {@link Constants#KEY_RUM_VIEW_LONG_TASK_COUNT}
     *
     * @param tags
     */
    private void increaseLongTask(JSONObject tags) {
        final String actionId = tags.optString(Constants.KEY_RUM_ACTION_ID);
        final String viewId = tags.optString(Constants.KEY_RUM_VIEW_ID);
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().increaseActionLongTask(actionId);
                FTDBManager.get().increaseViewLongTask(viewId);
                FTRUMInnerManager.this.checkActionClose();

            }
        });
    }

    /**
     * action 数量自增
     * {@link Constants#KEY_RUM_VIEW_ACTION_COUNT}
     *
     * @param viewId
     */
    private void increaseAction(final String viewId) {
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().increaseViewAction(viewId);

            }
        });

    }

    private void closeView(ActiveViewBean activeViewBean) {
        closeView(activeViewBean, null);
    }

    /**
     * 关闭 View，计算 {@link ViewBean#timeSpent},{@link ViewBean#isClose} 为 true，并更新
     * {@link FTSQL#RUM_DATA_UPDATE_TIME}
     *
     * @param activeViewBean
     * @param callBack
     */
    private void closeView(ActiveViewBean activeViewBean, RunnerCompleteCallBack callBack) {
        final ViewBean viewBean = activeViewBean.convertToViewBean();
        final String viewId = viewBean.getId();
        final long timeSpent = viewBean.getTimeSpent();
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().closeView(viewId, timeSpent, viewBean.getAttrJsonString());
                FTDBManager.get().updateViewUpdateTime(viewId, System.currentTimeMillis());
                if (callBack != null) {
                    callBack.onComplete();
                }
            }
        });
        generateRumData();
    }

    /**
     * 关闭 Action，计算 {@link ActionBean#duration},{@link ActionBean#isClose} 为 true
     *
     * @param bean
     * @param force 强制关闭
     */
    private void closeAction(ActiveActionBean bean, final boolean force) {
        final String actionId = bean.getId();
        final long duration = bean.getDuration();
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().closeAction(actionId, duration, force);
            }
        });
        generateRumData();
    }


    private String getViewId() {
        if (activeView == null) {
            return null;
        }
        return activeView.getId();
    }

    private String getViewName() {
        if (activeView == null) {
            return null;
        }
        return activeView.getViewName();
    }

    private String getViewReferrer() {
        if (activeView == null) {
            return null;
        }
        return activeView.getViewReferrer();
    }

    private String getActionName() {
        return activeAction == null ? null : activeAction.getActionName();
    }

    /**
     * 设置 RUM 全局 view session action 关联数据
     *
     * @param bean
     */
    private void attachRUMRelativeForResource(ResourceBean bean) {
        bean.viewId = getViewId();
        bean.viewName = getViewName();
        bean.viewReferrer = getViewReferrer();
        bean.sessionId = getSessionId();
        if (activeAction != null && !activeAction.isClose()) {
            bean.actionId = getActionId();
            bean.actionName = getActionName();
        }
    }

    /**
     * 添加界面 RUM 相关界面属性
     *
     * @param tags
     * @param withAction
     */
    void attachRUMRelative(JSONObject tags, boolean withAction) {
        try {
            tags.put(Constants.KEY_RUM_VIEW_ID, getViewId());

            tags.put(Constants.KEY_RUM_VIEW_NAME, getViewName());
            tags.put(Constants.KEY_RUM_VIEW_REFERRER, getViewReferrer());
            tags.put(Constants.KEY_RUM_SESSION_ID, sessionId);
            if (withAction) {
                if (activeAction != null && !activeAction.isClose()) {
                    tags.put(Constants.KEY_RUM_ACTION_ID, getActionId());
                    tags.put(Constants.KEY_RUM_ACTION_NAME, getActionName());
                }
            }
        } catch (JSONException e) {
            LogUtils.e(TAG, Log.getStackTraceString(e));
        }
    }


    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRUMGenerateRunner = new Runnable() {
        @Override
        public void run() {
            try {
                final JSONObject tags = FTRUMConfigManager.get().getRUMPublicDynamicTags();
                EventConsumerThreadPool.get().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FTRUMInnerManager.this.generateActionSum(tags);
                            FTRUMInnerManager.this.generateViewSum(tags);
                        } catch (JSONException e) {
                            LogUtils.e(TAG, Log.getStackTraceString(e));
                        }
                    }
                });
            } catch (Exception e) {
                LogUtils.e(TAG, Log.getStackTraceString(e));

            }
        }
    };

    private final Runnable mActionRecheckRunner = new Runnable() {
        @Override
        public void run() {
            FTRUMInnerManager.this.checkActionClose();
        }
    };


    private static final int LIMIT_SIZE = 50;

    private void generateRumData() {
        //避免过于频繁的刷新
        mHandler.removeCallbacks(mRUMGenerateRunner);
        mHandler.postDelayed(mRUMGenerateRunner, 100);
    }

    /**
     * @param globalTags
     * @throws JSONException
     */
    private void generateActionSum(JSONObject globalTags) throws JSONException {
        ArrayList<ActionBean> beans;
        do {
            beans = FTDBManager.get().querySumAction(LIMIT_SIZE);
            for (ActionBean bean : beans) {
                JSONObject fields = new JSONObject();
                JSONObject tags = new JSONObject(globalTags.toString());
                try {
                    tags.put(Constants.KEY_RUM_VIEW_NAME, bean.getViewName());
                    tags.put(Constants.KEY_RUM_VIEW_REFERRER, bean.getViewReferrer());
                    tags.put(Constants.KEY_RUM_VIEW_ID, bean.getViewId());
                    tags.put(Constants.KEY_RUM_ACTION_NAME, bean.getActionName());
                    tags.put(Constants.KEY_RUM_ACTION_ID, bean.getId());
                    tags.put(Constants.KEY_RUM_ACTION_TYPE, bean.getActionType());
                    tags.put(Constants.KEY_RUM_SESSION_ID, bean.getSessionId());
                    for (Map.Entry<String, Object> entry : bean.getProperty().entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        fields.put(key, value);
                    }
                    fields.put(Constants.KEY_RUM_ACTION_LONG_TASK_COUNT, bean.getLongTaskCount());
                    fields.put(Constants.KEY_RUM_ACTION_RESOURCE_COUNT, bean.getResourceCount());
                    fields.put(Constants.KEY_RUM_ACTION_ERROR_COUNT, bean.getErrorCount());
                    fields.put(Constants.KEY_RUM_ACTION_DURATION, bean.getDuration());

                    FTTrackInner.getInstance().rum(bean.getStartTime(),
                            Constants.FT_MEASUREMENT_RUM_ACTION, tags, fields, null);
                } catch (JSONException e) {
                    LogUtils.e(TAG, Log.getStackTraceString(e));
                }
            }
            FTDBManager.get().cleanCloseActionData();
        } while (beans.size() >= LIMIT_SIZE);
    }

    private void generateViewSum(JSONObject globalTags) throws JSONException {
        ArrayList<ViewBean> beans;
        do {
            beans = FTDBManager.get().querySumView(LIMIT_SIZE);
            for (ViewBean bean : beans) {
                JSONObject fields = new JSONObject();
                JSONObject tags = new JSONObject(globalTags.toString());

                try {
                    tags.put(Constants.KEY_RUM_SESSION_ID, bean.getSessionId());
                    tags.put(Constants.KEY_RUM_VIEW_NAME, bean.getViewName());
                    tags.put(Constants.KEY_RUM_VIEW_REFERRER, bean.getViewReferrer());
                    tags.put(Constants.KEY_RUM_VIEW_ID, bean.getId());
                    for (Map.Entry<String, Object> entry : bean.getProperty().entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        fields.put(key, value);
                    }
                    if (bean.getLoadTime() > 0) {
                        fields.put(Constants.KEY_RUM_VIEW_LOAD, bean.getLoadTime());
                    }
                    fields.put(Constants.KEY_RUM_VIEW_ACTION_COUNT, bean.getActionCount());
                    fields.put(Constants.KEY_RUM_VIEW_RESOURCE_COUNT, bean.getResourceCount());
                    fields.put(Constants.KEY_RUM_VIEW_ERROR_COUNT, bean.getErrorCount());
                    if (bean.isClose()) {
                        fields.put(Constants.KEY_RUM_VIEW_TIME_SPENT, bean.getTimeSpent());
                    } else {
                        fields.put(Constants.KEY_RUM_VIEW_TIME_SPENT, Utils.getCurrentNanoTime() - bean.getStartTime());
                    }
                    fields.put(Constants.KEY_RUM_VIEW_LONG_TASK_COUNT, bean.getLongTaskCount());
                    fields.put(Constants.KEY_RUM_VIEW_IS_ACTIVE, !bean.isClose());
                    fields.put(Constants.KEY_SDK_VIEW_UPDATE_TIME, bean.getViewUpdateTime());

                    if (FTMonitorManager.get().isDeviceMetricsMonitorType(DeviceMetricsMonitorType.CPU)) {
                        double cpuTickCountPerSecond = bean.getCpuTickCountPerSecond();
                        long cpuTickCount = bean.getCpuTickCount();
                        if (cpuTickCountPerSecond > -1) {
                            fields.put(Constants.KEY_CPU_TICK_COUNT_PER_SECOND, cpuTickCountPerSecond);
                        }
                        if (cpuTickCount > -1) {
                            fields.put(Constants.KEY_CPU_TICK_COUNT, cpuTickCount);
                        }
                    }
                    if (FTMonitorManager.get().isDeviceMetricsMonitorType(DeviceMetricsMonitorType.MEMORY)) {
                        fields.put(Constants.KEY_MEMORY_MAX, bean.getMemoryMax());
                        fields.put(Constants.KEY_MEMORY_AVG, bean.getMemoryAvg());
                    }
                    if (FTMonitorManager.get().isDeviceMetricsMonitorType(DeviceMetricsMonitorType.BATTERY)) {
                        fields.put(Constants.KEY_BATTERY_CURRENT_AVG, bean.getBatteryCurrentAvg());
                        fields.put(Constants.KEY_BATTERY_CURRENT_MAX, bean.getBatteryCurrentMax());
                    }
                    if (FTMonitorManager.get().isDeviceMetricsMonitorType(DeviceMetricsMonitorType.FPS)) {
                        fields.put(Constants.KEY_FPS_AVG, bean.getFpsAvg());
                        fields.put(Constants.KEY_FPS_MINI, bean.getFpsMini());
                    }
                } catch (JSONException e) {
                    LogUtils.e(TAG, Log.getStackTraceString(e));
                }

                FTTrackInner.getInstance().rum(bean.getStartTime(),
                        Constants.FT_MEASUREMENT_RUM_VIEW, tags, fields, new RunnerCompleteCallBack() {
                            @Override
                            public void onComplete() {
                                FTDBManager.get().updateViewUploadTime(bean.getId(), System.currentTimeMillis());
                            }
                        });
            }

            FTDBManager.get().cleanCloseViewData();
        } while (beans.size() >= LIMIT_SIZE);
    }

    void initParams(FTRUMConfig config) {
        sampleRate = config.getSamplingRate();
        checkSessionKeep(sessionId, sampleRate);
        EventConsumerThreadPool.get().execute(new Runnable() {
            @Override
            public void run() {
                FTDBManager.get().closeAllActionAndView();
            }
        });

    }

    /**
     * 根据采样率确认这个 session_id 是否需要被采集
     *
     * @param sessionId
     * @param sampleRate
     */
    private void checkSessionKeep(String sessionId, float sampleRate) {
        boolean collect = Utils.enableTraceSamplingRate(sampleRate);
        if (!collect) {
            synchronized (notCollectMap) {
                if (notCollectMap.size() + 1 > SESSION_FILTER_CAPACITY) {
                    try {
                        notCollectMap.remove(0);
                    } catch (Exception e) {
                        LogUtils.d(TAG, Log.getStackTraceString(e));
                    }
                }
                notCollectMap.add(sessionId);
            }
            LogUtils.d(TAG, "根据 FTRUMConfig SampleRate 采样率计算，当前 session 不被采集，session_id:" + sessionId);
        }
    }

    /**
     * 确认 session 是否需要被采集
     *
     * @param sessionId
     * @return
     */
    public boolean checkSessionWillCollect(String sessionId) {
        return !notCollectMap.contains(sessionId);
    }

    public void release() {
        mHandler.removeCallbacks(mRUMGenerateRunner);
        activeAction = null;
        activeView = null;
        viewList.clear();
    }

    /**
     * 检测追加 AddResource 数据时间
     *
     * @param key
     * @param bean
     */
    void checkToAddResource(String key, ResourceBean bean) {
        LogUtils.d(TAG, "checkToAddResource:" + key + ",header：" + bean.requestHeader + "," + bean.url);
        if (bean.contentSet && bean.netStateSet) {
            putRUMResourcePerformance(key);
        }
    }

}

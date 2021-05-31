package com.ft.sdk;

import com.ft.sdk.garble.utils.DeviceUtils;

import java.util.List;

import static com.ft.sdk.garble.utils.Constants.DEFAULT_LOG_SERVICE_NAME;

/**
 * BY huangDianHua
 * DATE:2019-12-06 11:40
 * Description:
 */
public class FTSDKConfig {
    //服务器地址
    private String serverUrl;
    private boolean useOAID;
    //是否是Debug
    private boolean isDebug;

    private boolean autoTrack = true;
    //是否需要绑定用户数据
//    private boolean needBindUser;
    //崩溃采集数据附加类型
    private int monitorType;
    //以下三个为白名单
//    private int enableAutoTrackType;
//    private List<Class<?>> whiteActivityClass;
//    private List<Class<?>> whiteViewClass;
//
//    //以下三个为设置黑名单
//    private int disableAutoTrackType;
//    private List<Class<?>> blackActivityClass;
//    private List<Class<?>> blackViewClass;
//    private boolean trackNetTime;

    //    //页面别名对应 map
//    private Map<String, String> pageDescMap;
//    //事件别名对应 map
//    private Map<String, String> vtpDescMap;
//    //页面和视图树是否显示描述
//    private boolean pageVtpDescEnabled;
    //设置是否需要采集崩溃日志
    private boolean enableTrackAppCrash;
    //设置是否检测 UI 卡顿
    private boolean enableTrackAppUIBlock;
    //设置是否检测 ANR
    private boolean enableTrackAppANR;

    //设置采样率
    private float samplingRate = 1;
    //崩溃日志的环境
    private EnvType env = EnvType.PROD;
    //是否开启网络日志上报
    private boolean networkTrace;
    //崩溃日志的 __serviceName
    private String serviceName = DEFAULT_LOG_SERVICE_NAME;
    //是否开启用户数据追踪
    private boolean enableTraceUserAction;
    //是否开启系统日志的上报功能
    private boolean traceConsoleLog;
    // openTrace 使用类型
    private TraceType traceType = TraceType.ZIPKIN;
    //支持的采集类型
    private List<String> traceContentType;
    // SDK 是否只支持在主进程中初始化
    private boolean onlySupportMainProcess = true;
    //日志数据数据库存储策略
    private LogCacheDiscard logCacheDiscardStrategy = LogCacheDiscard.DISCARD;

    private String rumAppId;

    /**
     * 构建 SDK 必要的配置参数
     *
     * @param metricsUrl 服务器地址
     * @return
     */
    public static FTSDKConfig builder(String metricsUrl) {
        return new FTSDKConfig(metricsUrl);
    }


    /**
     * SDK 配置项构造方法
     *
     * @param serverUrl
     */
    private FTSDKConfig(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getRUMAppId() {
        return rumAppId;
    }

    /**
     * 设置 RUM AppId
     * @param rumAppId
     * @return
     */
    public FTSDKConfig setRumAppId(String rumAppId) {
        this.rumAppId = rumAppId;
        return this;
    }


    public boolean isUseOAID() {
        return useOAID;
    }

    public boolean isDebug() {
        return isDebug;
    }



    public boolean isAutoTrack() {
        return autoTrack;
    }



    public int getMonitorType() {
        return monitorType;
    }



    public boolean isEnableTrackAppCrash() {
        return enableTrackAppCrash;
    }

    public float getSamplingRate() {
        return samplingRate;
    }

    public EnvType getEnv() {
        return env;
    }

    public boolean isNetworkTrace() {
        return networkTrace;
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isEnableTraceUserAction() {
        return enableTraceUserAction;
    }

    public boolean isTraceConsoleLog() {
        return traceConsoleLog;
    }

    public List<String> getTraceContentType() {
        return traceContentType;
    }

    public boolean isOnlySupportMainProcess() {
        return onlySupportMainProcess;
    }


    /**
     * 是否使用 UseOAID 作为设备唯一识别号的替代字段
     *
     * @param useOAID
     * @return
     */
    public FTSDKConfig setUseOAID(boolean useOAID) {
        this.useOAID = useOAID;
        return this;
    }

    /**
     * 设置数据采集端的名称
     *
     * @param uuid
     * @return
     */
    public FTSDKConfig setXDataKitUUID(String uuid) {
        DeviceUtils.setSDKUUid(uuid);
        return this;
    }

    /**
     * 是否开启Debug，开启后将显示 SDK 运行日志
     *
     * @param debug
     * @return
     */
    public FTSDKConfig setDebug(boolean debug) {
        isDebug = debug;
        return this;
    }


    /**
     * 设置采集数据附加类型
     *
     * @param monitorType
     * @return
     */
    public FTSDKConfig setMonitorType(int monitorType) {
        this.monitorType = monitorType;
        return this;
    }



    /**
     * 设置是否需要采集崩溃日志
     *
     * @param enableTrackAppCrash
     * @return
     */
    public FTSDKConfig setEnableTrackAppCrash(boolean enableTrackAppCrash) {
        this.enableTrackAppCrash = enableTrackAppCrash;
        return this;
    }

    /**
     * 设置采样率
     *
     * @param samplingRate
     * @return
     */
    public FTSDKConfig setSamplingRate(float samplingRate) {
        this.samplingRate = samplingRate;
        return this;
    }

    /**
     * 设置数据传输的环境
     *
     * @param env
     * @return
     */
    public FTSDKConfig setEnv(EnvType env) {
        if (env != null) {
            this.env = env;
        }
        return this;
    }

    /**
     * 是否开启网络日志上报
     *
     * @param networkTrace
     * @return
     */
    public FTSDKConfig setNetworkTrace(boolean networkTrace) {
        this.networkTrace = networkTrace;
        return this;
    }

    /**
     * 设置崩溃日志的 serviceName
     *
     * @param serviceName
     * @return
     */
    public FTSDKConfig setServiceName(String serviceName) {
        if (serviceName != null) {
            this.serviceName = serviceName;
        }
        return this;
    }

    /**
     * 是否开启流程图日志
     *
     * @param enableTraceUserAction
     */
    public FTSDKConfig setEnableTraceUserAction(boolean enableTraceUserAction) {
        this.enableTraceUserAction = enableTraceUserAction;
        return this;
    }

    /**
     * 是否开启系统日志上报功能
     *
     * @param traceConsoleLog
     * @return
     */
    public FTSDKConfig setTraceConsoleLog(boolean traceConsoleLog) {
        this.traceConsoleLog = traceConsoleLog;
        return this;
    }

    /**
     * trace 类型
     *
     * @param traceType
     * @return
     */
    public FTSDKConfig setTraceType(TraceType traceType) {
        this.traceType = traceType;
        return this;
    }

    /**
     * 获取 trace 类型
     *
     * @return
     */
    public TraceType getTraceType() {
        return traceType;
    }

    /**
     * 设置支持的采集类型
     *
     * @param traceContentType
     * @return
     */
    public FTSDKConfig setTraceContentType(List<String> traceContentType) {
        this.traceContentType = traceContentType;
        return this;
    }

    /**
     * 是否只支持在主进程中初始化 SDK
     *
     * @param onlySupportMainProcess
     * @return
     */
    public FTSDKConfig setOnlySupportMainProcess(boolean onlySupportMainProcess) {
        this.onlySupportMainProcess = onlySupportMainProcess;
        return this;
    }

    public LogCacheDiscard getLogCacheDiscardStrategy() {
        return logCacheDiscardStrategy;
    }

    /**
     * 设置数据库数据存储策略
     *
     * @param logCacheDiscardStrategy
     * @return
     */
    public FTSDKConfig setLogCacheDiscardStrategy(LogCacheDiscard logCacheDiscardStrategy) {
        this.logCacheDiscardStrategy = logCacheDiscardStrategy;
        return this;
    }

    public boolean isEnableTrackAppUIBlock() {
        return enableTrackAppUIBlock;
    }

    public FTSDKConfig setEnableTrackAppUIBlock(boolean enableTrackAppUIBlock) {
        this.enableTrackAppUIBlock = enableTrackAppUIBlock;
        return this;
    }

    public boolean isEnableTrackAppANR() {
        return enableTrackAppANR;
    }

    public FTSDKConfig setEnableTrackAppANR(boolean enableTrackAppANR) {
        this.enableTrackAppANR = enableTrackAppANR;
        return this;
    }
}

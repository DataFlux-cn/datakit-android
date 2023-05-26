package com.ft.sdk;

import androidx.annotation.NonNull;

import com.ft.sdk.garble.utils.DeviceUtils;
import com.ft.sdk.garble.utils.Constants;

import java.util.HashMap;

/**
 * BY huangDianHua
 * DATE:2019-12-06 11:40
 * Description:
 */
public class FTSDKConfig {
    //服务器地址
    private final String metricsUrl;
    //是否是Debug
    private boolean isDebug;

    /**
     * 是否可访问 Android ID
     */
    private boolean enableAccessAndroidID = true;

    /**
     * 应用服务名
     */
    private String serviceName = Constants.DEFAULT_SERVICE_NAME;

    /**
     * 数据上传环境
     */
    private EnvType env = EnvType.PROD;

    /**
     * SDK 是否只支持在主进程中初始化
     * {@link FTSDKConfig#setOnlySupportMainProcess(boolean)}
     */
    private boolean onlySupportMainProcess = true;

    /**
     * 全局参数，例如 {@link Constants#KEY_APP_VERSION_NAME} 等固定配置参数，
     * 或通过 {@link FTSDKConfig#addGlobalContext(String, String)} 用户自定义添加的变量参数
     */
    private final HashMap<String, Object> globalContext = new HashMap<>();

    /**
     * 构建 SDK 必要的配置参数
     *
     * @param metricsUrl 服务器地址
     * @return {@link FTRUMConfig} SDK 配置
     */
    public static FTSDKConfig builder(String metricsUrl) {
        return new FTSDKConfig(metricsUrl);
    }


    /**
     * SDK 配置项构造方法
     *
     * @param metricsUrl datakit 上传地址
     */
    private FTSDKConfig(String metricsUrl) {
        this.metricsUrl = metricsUrl;
    }

    /**
     * 获取 datakit 数据上报地址
     *
     * @return
     */
    public String getMetricsUrl() {
        return metricsUrl;
    }


    /**
     * @return 是否处于 debug 状态
     */
    public boolean isDebug() {
        return isDebug;
    }

    /**
     * @return 获取环境变量请问
     */
    public EnvType getEnv() {
        return env;
    }

    /**
     * @return 是否只支持主进程
     */
    public boolean isOnlySupportMainProcess() {
        return onlySupportMainProcess;
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
     * 设置是否获取 Android ID
     * <p>
     * 当为 false ，device_uuid 字段不再获取
     *
     * @param enableAccessAndroidID
     * @return
     */
    public FTSDKConfig setEnableAccessAndroidID(boolean enableAccessAndroidID) {
        this.enableAccessAndroidID = enableAccessAndroidID;
        return this;
    }

    public boolean isEnableAccessAndroidID() {
        return enableAccessAndroidID;
    }

    /**
     * 是否只支持在主进程中初始化 SDK
     *
     * @param onlySupportMainProcess true，wei
     * @return
     */
    public FTSDKConfig setOnlySupportMainProcess(boolean onlySupportMainProcess) {
        this.onlySupportMainProcess = onlySupportMainProcess;
        return this;
    }

    /**
     * 添加全局属性
     *
     * @param key   键名
     * @param value jian
     * @return
     */
    public FTSDKConfig addGlobalContext(@NonNull String key, @NonNull String value) {
        this.globalContext.put(key, value);
        return this;
    }


    /**
     * 获取全局属性
     *
     * @return
     */
    public HashMap<String, Object> getGlobalContext() {
        return this.globalContext;
    }

    public String getServiceName() {
        return serviceName;
    }

    public FTSDKConfig setServiceName(String serviceName) {
        if (serviceName != null && !serviceName.isEmpty()) {
            this.serviceName = serviceName;
        }
        return this;
    }
}

package com.ft.sdk.garble.bean;

import com.ft.sdk.garble.utils.Constants;

import java.util.Arrays;

/**
 * BY huangDianHua
 * DATE:2019-12-02 14:00
 * Description:
 */
public enum OP {
    //页面事件
    LANC("lanc"),
    CLK("clk"),
    CSTM("cstm"),
    //    FLOW_CHART("flow_chart"),
    OPEN("open"),
    OPEN_ACT("opn_act"),
    OPEN_FRA("open_fra"),
    CLS_ACT("cls_act"),
    CLS_FRA("cls_fra"),

    //错误事件
    BLOCK("block"),
    CRASH("crash"),
    ANR("anr"),


    //webview 事件
    WEBVIEW_LOADING("webview_loading"),
    WEBVIEW_LOAD_COMPLETED("webview_load_completed"),

    //客户端使用事件
    CLIENT_ACTIVATED_TIME("client_activated_time"),

    //网络情况事件
    HTTP_CLIENT("http_client"),
    HTTP_WEBVIEW("http_webview");

    public String value;

    OP(String value) {
        this.value = value;
    }


    @Override
    public String toString() {
        return value;
    }

    public String toEventName() {
        return Constants.OP_EVENT_MAPS.get(this);
    }

    public static OP fromValue(String value) {
        for (OP op : values()) {
            if (op.value.equals(value)) {
                return op;
            }
        }
        return null;
    }

    public boolean needMonitorData() {
        return Arrays.asList(Constants.MERGE_MONITOR_EVENTS).contains(this);
    }

    public boolean isUserRelativeOp() {
        return Arrays.asList(Constants.USER_ACTION_EVENTS).contains(this);
    }
}

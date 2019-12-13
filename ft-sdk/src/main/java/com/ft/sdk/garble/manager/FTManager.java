package com.ft.sdk.garble.manager;

import com.ft.sdk.garble.db.FTDBManager;

/**
 * BY huangDianHua
 * DATE:2019-12-06 15:42
 * Description:
 */
public class FTManager {

    public static SyncTaskManager getSyncTaskManager(){
        return SyncTaskManager.get();
    }

    public static FTActivityManager getFTActivityManager(){
        return FTActivityManager.get();
    }

    public static FTDBManager getFTDBManager(){
        return FTDBManager.get();
    }
}

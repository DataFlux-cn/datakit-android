package com.ft.tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static com.ft.AllTests.hasPrepare;

import android.content.Context;
import android.os.Looper;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ft.AccountUtils;
import com.ft.BaseTest;
import com.ft.DebugMainActivity;
import com.ft.R;
import com.ft.application.MockApplication;
import com.ft.sdk.DeviceMetricsMonitorType;
import com.ft.sdk.EnvType;
import com.ft.sdk.FTRUMConfig;
import com.ft.sdk.FTSDKConfig;
import com.ft.sdk.FTSdk;
import com.ft.sdk.garble.bean.DataType;
import com.ft.sdk.garble.bean.SyncJsonData;
import com.ft.sdk.garble.db.FTDBManager;
import com.ft.sdk.garble.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class RUMViewDeviceMetricsTest extends BaseTest {

    @Rule
    public ActivityScenarioRule<DebugMainActivity> rule = new ActivityScenarioRule<>(DebugMainActivity.class);


    @BeforeClass
    public static void settingBeforeLaunch() throws Exception {
        if (!hasPrepare) {
            Looper.prepare();
            hasPrepare = true;
        }

        stopSyncTask();

        Context context = MockApplication.getContext();
        FTSDKConfig ftSDKConfig = FTSDKConfig
                .builder(AccountUtils.getProperty(context, AccountUtils.ACCESS_SERVER_URL))
                .setDebug(true)//设置是否是 debug
                .setEnv(EnvType.GRAY);
        FTSdk.install(ftSDKConfig);

        FTSdk.initRUMWithConfig(new FTRUMConfig()
                .setRumAppId(AccountUtils.getProperty(context, AccountUtils.RUM_APP_ID))
                .setDeviceMetricsMonitorType(DeviceMetricsMonitorType.ALL)
                .setEnableTraceUserView(true)
        );

    }


    @Test
    public void viewDeviceMetricsTest() throws Exception {
        invokeGenerateRumData();
        Thread.sleep(3000);
        onView(ViewMatchers.withId(R.id.main_view_loop_test)).perform(ViewActions.scrollTo()).perform(click());
        Thread.sleep(3000);
        List<SyncJsonData> recordDataList = FTDBManager.get().queryDataByDataByTypeLimitDesc(0,
                DataType.RUM_APP);

        for (SyncJsonData recordData : recordDataList) {
            try {
                JSONObject json = new JSONObject(recordData.getDataString());
                JSONObject fields = json.optJSONObject("fields");
                String measurement = json.optString("measurement");
                if ("view".equals(measurement)) {
                    if (fields != null) {
                        if (fields.optBoolean(Constants.KEY_RUM_VIEW_IS_ACTIVE, false)) {
                            Assert.assertTrue(fields.optLong(Constants.KEY_CPU_TICK_COUNT_MAX) > 0);
                            Assert.assertTrue(fields.optLong(Constants.KEY_CPU_TICK_COUNT_AVG) > 0);
                            Assert.assertTrue(fields.optLong(Constants.KEY_MEMORY_MAX) > 0);
                            Assert.assertTrue(fields.optLong(Constants.KEY_MEMORY_AVG) > 0);
                            Assert.assertTrue(fields.optLong(Constants.KEY_BATTERY_CURRENT_MAX) > 0);
                            Assert.assertTrue(fields.optLong(Constants.KEY_BATTERY_CURRENT_AVG) > 0);
//                            fields.optDouble(Constants.KEY_FPS_MINI);
//                            fields.optDouble(Constants.KEY_FPS_AVG);
                        }

                    }


                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


    }
}

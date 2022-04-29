package com.ft;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ft.sdk.garble.http.RequestMethod;
import com.ft.sdk.garble.reflect.ReflectUtils;
import com.ft.sdk.garble.utils.LogUtils;
import com.ft.sdk.garble.utils.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    boolean logThreadRun = false;
    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();

    public Request requestUrl(@NonNull String url) {
        Request.Builder builder = new Request.Builder().url(url)
                .method(RequestMethod.GET.name(), null);
        Request request = null;
        try {
            Response response = client.newCall(builder.build()).execute();
            request = response.request();

            ResponseBody responseBody = response.body();
            String string = "";
            if (responseBody != null) {
                string = responseBody.string();
            }
            LogUtils.d(TAG, "url:" + url + "\n" + string);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return request;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_PHONE_STATE
            }, 1);
        }


        Button logThread = findViewById(R.id.main_mock_period_data_btn);
        logThread.setOnClickListener(v -> {
            logThreadRun = !logThreadRun;
            AtomicLong atomicLong = new AtomicLong(0);
            if (logThreadRun) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (logThreadRun) {
                            Log.d("LogManager", "data=======" + atomicLong.getAndIncrement());
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
            if (logThreadRun) {
                logThread.setText("周期产生日志数据--运行");
            } else {
                logThread.setText("周期产生日志数据--结束");
            }
        });

        findViewById(R.id.main_mock_one_data_btn).setOnClickListener(v -> {
            Log.d("LogManager", "测试日志数据=======当前时间为" + Utils.getCurrentNanoTime());
        });
        findViewById(R.id.main_mock_crash_btn).setOnClickListener(v -> {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int i = 1 / 0;
                }
            }).start();
        });
        findViewById(R.id.main_mock_crash_native_btn).setOnClickListener(v -> {
            ReflectUtils.reflectCrashAndGetExceptionMessage();
        });
        findViewById(R.id.main_mock_page_jump_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, WebViewActivity.class));
        });
        findViewById(R.id.main_mock_click_btn).setOnClickListener(v -> {
        });

        findViewById(R.id.main_mock_okhttp_btn).setOnClickListener(v -> {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //通过查看请求头查看是否替换调用 OkHttpClient.Builder.build 方法成功
                    Request request = requestUrl("http://testing-ft2x-api.cloudcare.cn/api/v1/account/permissions");
                    LogUtils.d(TAG, "header=" + request.headers().toString());
                }
            }).start();
        });

        findViewById(R.id.main_mock_ui_block_btn).setOnClickListener(v -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        findViewById(R.id.main_mock_anr_btn).setOnClickListener(v -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        });
        findViewById(R.id.main_view_loop_test).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FirstActivity.class));

        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int grantResult : grantResults) {
                if (grantResult == PERMISSION_DENIED) {
                    finish();
                }
            }
        }
    }

}

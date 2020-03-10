package com.ft;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.amitshekhar.DebugDB;
import com.amitshekhar.debug.encrypt.sqlite.DebugDBEncryptFactory;
import com.amitshekhar.debug.sqlite.DebugDBFactory;
import com.bumptech.glide.Glide;
import com.ft.sdk.FTAutoTrack;
import com.ft.sdk.FTSdk;
import com.ft.sdk.garble.bean.ActivityFromWay;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    private Button showKotlinActivity;
    private Button btn_lam;
    private CheckBox checkbox;
    private RatingBar ratingbar;
    private SeekBar seekbar;
    private RadioGroup radioGroup;
    private Button showDialog;
    private Button bindUser;
    private Button unbindUser;
    private Button changeUser;
    private ImageView iv_glide;
    private Button flowChartTacker;
    private Tab1Fragment tab1Fragment = new Tab1Fragment();
    private Tab2Fragment tab2Fragment = new Tab2Fragment();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!DebugDB.isServerRunning()) {
            DebugDB.initialize(DemoApplication.getContext(), new DebugDBFactory());
            DebugDB.initialize(DemoApplication.getContext(), new DebugDBEncryptFactory());
        }
        requestPermissions(new String[]{Manifest.permission.CAMERA
                , Manifest.permission.ACCESS_FINE_LOCATION
                , Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        setTitle("FT-SDK使用Demo");
        FTSdk.get().setGpuRenderer(findViewById(R.id.ll));
        showKotlinActivity = findViewById(R.id.showKotlinActivity);
        btn_lam = findViewById(R.id.btn_lam);
        checkbox = findViewById(R.id.checkbox);
        ratingbar = findViewById(R.id.ratingbar);
        seekbar = findViewById(R.id.seekbar);
        radioGroup = findViewById(R.id.radioGroup);
        showDialog = findViewById(R.id.showDialog);
        iv_glide = findViewById(R.id.iv_glide);
        bindUser = findViewById(R.id.bindUser);
        unbindUser = findViewById(R.id.unbindUser);
        changeUser = findViewById(R.id.changeUser);
        flowChartTacker = findViewById(R.id.flowChartTacker);
        addFragment();
        showKotlinActivity.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, Main2Activity.class)));
        btn_lam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changeFragment(isChecked);
        });
        ratingbar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
        });
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
        });
        showDialog.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("这是一个弹框标题");
            builder.setItems(new String[]{"hhh", "dddd", "iiiii", "oooooo"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setNegativeButton("取消", (dialog, which) -> {

            });
            builder.setPositiveButton("确定", (dialog, which) -> {

            });
            builder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        });
        Glide.with(this)
                .load("https://github.com/bumptech/glide/raw/master/static/glide_logo.png")
                .into(iv_glide);
        bindUser.setOnClickListener(v -> {
            JSONObject exts = new JSONObject();
            try {
                exts.put("sex", "male");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            FTSdk.get().bindUserData("jack", "007", exts);
        });
        unbindUser.setOnClickListener(v -> {
            FTSdk.get().unbindUserData();
        });
        changeUser.setOnClickListener(v -> {
            JSONObject exts = new JSONObject();
            try {
                exts.put("sex", "female");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            FTSdk.get().bindUserData("Rose", "000", exts);
        });
        flowChartTacker.setOnClickListener(v -> {

        });

    }

    public void addFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment, tab1Fragment);
        fragmentTransaction.commit();
    }

    public void changeFragment(boolean change) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (change) {
            fragmentTransaction.replace(R.id.fragment, tab1Fragment);
        } else {
            fragmentTransaction.replace(R.id.fragment, tab2Fragment);
        }
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void startActivityForResult(@RequiresPermission Intent intent, int requestCode,
                                       @Nullable Bundle options) {
        FTAutoTrack.startActivityByWay(ActivityFromWay.ACTIVITY,intent);
    }
}

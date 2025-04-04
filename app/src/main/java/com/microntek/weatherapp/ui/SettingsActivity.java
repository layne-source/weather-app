package com.microntek.weatherapp.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.microntek.weatherapp.R;
import com.microntek.weatherapp.MainActivity;
import com.microntek.weatherapp.util.ThemeHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    
    private static final String PREFS_NAME = "weather_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    
    private SharedPreferences preferences;
    private SwitchCompat switchDarkMode;
    private TextView tvVersion;
    private BottomNavigationView bottomNavigationView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 初始化工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // 初始化控件
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        tvVersion = findViewById(R.id.tv_version);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // 初始化设置
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        
        // 设置开关状态
        switchDarkMode.setChecked(isDarkMode);
        
        // 设置开关监听器
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemeHelper.setDarkMode(this, isChecked);
            recreate();
        });
        
        // 显示版本号
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("未知");
        }
        
        // 设置底部导航栏
        setupBottomNavigation();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 设置底部导航选中状态为设置
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_settings);
        }
    }
    
    /**
     * 设置底部导航栏
     */
    private void setupBottomNavigation() {
        // 设置选中状态为设置
        bottomNavigationView.setSelectedItemId(R.id.navigation_settings);
        
        // 设置导航栏项目点击事件
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }
    
    /**
     * 应用主题设置（静态方法，可在应用启动时调用）
     */
    public static void applyTheme(Context context) {
        ThemeHelper.applyTheme(context);
    }
    
    @Override
    public void finish() {
        // 在返回MainActivity之前，设置标志位
        Intent intent = new Intent();
        intent.putExtra("fromOtherActivity", true);
        setResult(RESULT_OK, intent);
        super.finish();
    }
    
    /**
     * 底部导航栏点击事件
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.navigation_home:
                // 返回主页并设置标志
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("fromOtherActivity", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
                return true;
                
            case R.id.navigation_city:
                // 切换到城市管理
                intent = new Intent(this, CityManagerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
                return true;
                
            case R.id.navigation_air:
                // 切换到空气质量页面
                intent = new Intent(this, com.microntek.weatherapp.ui.AirQualityActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
                return true;
                
            case R.id.navigation_settings:
                // 已经在设置页面，不需要处理
                return true;
        }
        return false;
    }
} 
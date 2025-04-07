package com.microntek.weatherapp.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.microntek.weatherapp.R;
import com.microntek.weatherapp.MainActivity;
import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.util.CityPreferences;
import com.microntek.weatherapp.util.MessageManager;
import com.microntek.weatherapp.util.ThemeHelper;
import com.microntek.weatherapp.util.WeatherDataCache;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class SettingsActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    
    private static final String PREFS_NAME = "weather_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    
    private SharedPreferences preferences;
    private SwitchCompat switchDarkMode;
    private TextView tvVersion;
    private BottomNavigationView bottomNavigationView;
    private View verifyCacheItem;
    private View clearCacheItem;
    
    // 缓存操作所需
    private CityPreferences cityPreferences;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
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
        verifyCacheItem = findViewById(R.id.verify_cache_item);
        clearCacheItem = findViewById(R.id.clear_cache_item);
        
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
        
        // 初始化数据
        cityPreferences = new CityPreferences(this);
        
        // 设置缓存管理点击事件
        setupCacheManagement();
        
        // 设置底部导航栏
        setupBottomNavigation();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 关闭资源，防止泄漏
        if (!isChangingConfigurations()) { 
            // 只有在应用完全退出时才关闭资源，避免因旋转屏幕等配置变化导致的临时销毁
            try {
                // 关闭executor
                if (executor instanceof ExecutorService) {
                    ((ExecutorService) executor).shutdown();
                    Log.i("SettingsActivity", "已关闭ExecutorService");
                }
                
                // 关闭全局缓存管理器
                WeatherDataCache.shutdown();
                Log.i("SettingsActivity", "已关闭WeatherDataCache");
            } catch (Exception e) {
                Log.e("SettingsActivity", "关闭资源失败: " + e.getMessage());
            }
        }
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
     * 设置缓存管理功能
     */
    private void setupCacheManagement() {
        // 验证缓存点击事件
        verifyCacheItem.setOnClickListener(v -> verifyAndRepairCache());
        
        // 清除缓存点击事件
        clearCacheItem.setOnClickListener(v -> showClearCacheConfirmDialog());
    }
    
    /**
     * 验证并修复当前城市的缓存
     */
    private void verifyAndRepairCache() {
        City currentCity = cityPreferences.getCurrentCity();
        if (currentCity == null) {
            MessageManager.showError(this, "没有选择的城市");
            return;
        }
        
        MessageManager.showMessage(this, "正在验证缓存数据...");
        
        // 在后台线程验证缓存
        executor.execute(() -> {
            try {
                boolean repaired = WeatherApi.verifyAndRepairCacheByLocation(
                        SettingsActivity.this, currentCity.getLatitude(), currentCity.getLongitude());
                
                mainHandler.post(() -> {
                    if (repaired) {
                        MessageManager.showSuccess(SettingsActivity.this, "已修复部分缓存数据");
                    } else {
                        MessageManager.showSuccess(SettingsActivity.this, "缓存数据验证完成，未发现问题");
                    }
                });
            } catch (Exception e) {
                Log.e("SettingsActivity", "验证缓存失败: " + e.getMessage());
                mainHandler.post(() -> {
                    MessageManager.showError(SettingsActivity.this, "验证缓存失败: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * 显示清除缓存确认对话框
     */
    private void showClearCacheConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清除缓存")
                .setMessage("确定要清除所有缓存的天气数据吗？这将移除所有离线可用的数据。")
                .setPositiveButton("确定", (dialog, which) -> clearAllCache())
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 清除所有缓存
     */
    private void clearAllCache() {
        // 在后台线程清除缓存
        executor.execute(() -> {
            try {
                // 初始化缓存管理器
                WeatherApi.initCache(SettingsActivity.this);
                // 获取缓存管理器实例并清除所有缓存
                WeatherDataCache.getInstance(SettingsActivity.this).clearAllCache();
                
                mainHandler.post(() -> {
                    MessageManager.showSuccess(SettingsActivity.this, "所有缓存已清除");
                });
            } catch (Exception e) {
                Log.e("SettingsActivity", "清除缓存失败: " + e.getMessage());
                mainHandler.post(() -> {
                    MessageManager.showError(SettingsActivity.this, "清除缓存失败: " + e.getMessage());
                });
            }
        });
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
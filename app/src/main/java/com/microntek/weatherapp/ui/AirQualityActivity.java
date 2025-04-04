package com.microntek.weatherapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.microntek.weatherapp.R;
import com.microntek.weatherapp.MainActivity;
import com.microntek.weatherapp.CityManagerActivity;
import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;
import com.microntek.weatherapp.util.CityPreferences;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;

/**
 * 空气质量详情页面
 */
public class AirQualityActivity extends AppCompatActivity {

    // UI组件
    private MaterialToolbar toolbar;
    private TextView tvCityName;
    private TextView tvAqiCircle;
    private TextView tvAqiStatus;
    private TextView tvPm25Detail;
    private TextView tvPm10Detail;
    private TextView tvO3;
    private TextView tvCo;
    private TextView tvSo2;
    private TextView tvNo2;
    private FrameLayout aqiCircleContainer;
    private BottomNavigationView bottomNavigationView;
    
    // 数据处理
    private CityPreferences cityPreferences;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_quality);
        
        // 初始化UI组件
        initViews();
        
        // 初始化数据
        cityPreferences = new CityPreferences(this);
        
        // 设置底部导航栏
        setupBottomNavigation();
        
        // 加载空气质量数据
        loadAirQualityData();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 设置底部导航选中状态为空气质量
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_air);
        }
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvCityName = findViewById(R.id.tv_city_name);
        tvAqiCircle = findViewById(R.id.tv_aqi_circle);
        tvAqiStatus = findViewById(R.id.tv_aqi_status);
        tvPm25Detail = findViewById(R.id.tv_pm25_detail);
        tvPm10Detail = findViewById(R.id.tv_pm10_detail);
        tvO3 = findViewById(R.id.tv_o3);
        tvCo = findViewById(R.id.tv_co);
        tvSo2 = findViewById(R.id.tv_so2);
        tvNo2 = findViewById(R.id.tv_no2);
        aqiCircleContainer = findViewById(R.id.aqi_circle_container);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // 设置工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.air_quality);
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    /**
     * 设置底部导航栏
     */
    private void setupBottomNavigation() {
        // 设置选中状态为空气质量
        bottomNavigationView.setSelectedItemId(R.id.navigation_air);
        
        // 设置导航栏项目点击事件
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    // 导航到主页
                    intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.navigation_city:
                    // 导航到城市管理
                    intent = new Intent(this, CityManagerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.navigation_air:
                    // 已经在空气质量页面，无需处理
                    return true;
                case R.id.navigation_settings:
                    // 导航到设置页面
                    intent = new Intent(this, SettingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(0, 0);
                    return true;
            }
            return false;
        });
    }
    
    /**
     * 加载空气质量数据
     */
    private void loadAirQualityData() {
        // 获取当前选中的城市
        City currentCity = cityPreferences.getCurrentCity();
        
        if (currentCity == null) {
            // 如果没有选中的城市，使用默认城市（北京）
            currentCity = new City("北京", "101010100", "北京市", 39.9042, 116.4074);
            cityPreferences.setCurrentCity(currentCity);
        }
        
        final City city = currentCity;
        tvCityName.setText(city.getName());
        
        // 在后台线程加载数据
        executor.execute(() -> {
            try {
                // 获取当前天气数据（包括空气质量）
                final Weather weather = WeatherApi.getCurrentWeatherByLocation(
                        city.getLatitude(), city.getLongitude());
                
                // 在主线程更新UI
                mainHandler.post(() -> updateUI(weather));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(AirQualityActivity.this, 
                        "数据加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
    
    /**
     * 更新UI显示
     */
    private void updateUI(Weather weather) {
        // 更新城市名称
        tvCityName.setText(weather.getCityName());
        
        // 更新AQI圆形指示器
        tvAqiCircle.setText(String.valueOf(weather.getAqi()));
        
        // 设置AQI指示器背景颜色（可以根据AQI值设置不同颜色）
        setAqiBackgroundColor(weather.getAqi());
        
        // 更新空气质量详细指标
        tvPm25Detail.setText(String.valueOf(weather.getPm25()));
        tvPm10Detail.setText(String.valueOf(weather.getPm10()));
        tvO3.setText(String.valueOf(weather.getO3()));
        tvCo.setText(String.format("%.1f", weather.getCo()));
        tvSo2.setText(String.valueOf(weather.getSo2()));
        tvNo2.setText(String.valueOf(weather.getNo2()));
    }
    
    /**
     * 根据AQI值设置背景颜色
     */
    private void setAqiBackgroundColor(int aqi) {
        // 获取圆形背景drawable
        GradientDrawable drawable = (GradientDrawable) aqiCircleContainer.getBackground();
        
        // 根据国际标准的AQI等级设置背景颜色
        if (aqi <= 50) {
            // 优良 (0-50)
            drawable.setColor(getResources().getColor(R.color.good_air_quality));
            tvAqiStatus.setText("优");
        } else if (aqi <= 100) {
            // 中等 (51-100)
            drawable.setColor(getResources().getColor(R.color.moderate_air_quality));
            tvAqiStatus.setText("良");
        } else if (aqi <= 150) {
            // 对敏感人群不健康 (101-150)
            drawable.setColor(getResources().getColor(R.color.sensitive_group_air_quality));
            tvAqiStatus.setText("轻度污染");
        } else if (aqi <= 200) {
            // 不健康 (151-200)
            drawable.setColor(getResources().getColor(R.color.unhealthy_air_quality));
            tvAqiStatus.setText("中度污染");
        } else if (aqi <= 300) {
            // 非常不健康 (201-300)
            drawable.setColor(getResources().getColor(R.color.very_unhealthy_air_quality));
            tvAqiStatus.setText("重度污染");
        } else {
            // 危险 (301+)
            drawable.setColor(getResources().getColor(R.color.hazardous_air_quality));
            tvAqiStatus.setText("严重污染");
        }
    }
} 
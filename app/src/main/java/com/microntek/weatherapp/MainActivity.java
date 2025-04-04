package com.microntek.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;
import com.microntek.weatherapp.util.AirPollutionUtil;
import com.microntek.weatherapp.util.CityPreferences;
import com.microntek.weatherapp.util.WeatherBackgroundUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    
    // UI组件
    private TextView tvCityName;
    private TextView tvCurrentTemp;
    private ImageView ivWeatherIcon;
    private TextView tvWeatherDesc;
    private TextView tvTempRange;
    private TextView tvAirQuality;
    private TextView tvAqi;
    private TextView tvPm25;
    private TextView tvPm10;
    private LinearLayout forecastContainer;
    private TextView tvWind;
    private TextView tvHumidity;
    private TextView tvSunrise;
    private TextView tvSunset;
    private BottomNavigationView bottomNavigationView;
    
    // 数据处理
    private CityPreferences cityPreferences;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用主题设置
        com.microntek.weatherapp.util.ThemeHelper.applyTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化UI组件
        initViews();
        
        // 初始化数据
        cityPreferences = new CityPreferences(this);
        
        // 设置底部导航栏
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        
        // 加载天气数据
        loadWeatherData();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到页面时重新加载数据
        loadWeatherData();
        
        // 重置底部导航栏选中状态为首页
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        tvCityName = findViewById(R.id.tv_city_name);
        tvCurrentTemp = findViewById(R.id.tv_current_temp);
        ivWeatherIcon = findViewById(R.id.iv_weather_icon);
        tvWeatherDesc = findViewById(R.id.tv_weather_desc);
        tvTempRange = findViewById(R.id.tv_temp_range);
        tvAirQuality = findViewById(R.id.tv_air_quality);
        tvAqi = findViewById(R.id.tv_aqi);
        tvPm25 = findViewById(R.id.tv_pm25);
        tvPm10 = findViewById(R.id.tv_pm10);
        
        forecastContainer = findViewById(R.id.forecast_container);
        tvWind = findViewById(R.id.tv_wind);
        tvHumidity = findViewById(R.id.tv_humidity);
        tvSunrise = findViewById(R.id.tv_sunrise);
        tvSunset = findViewById(R.id.tv_sunset);
        
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // 设置点击事件
        tvCityName.setOnClickListener(v -> navigateToCityManager());
    }
    
    /**
     * 加载天气数据
     */
    private void loadWeatherData() {
        // 获取当前选中的城市
        City currentCity = cityPreferences.getCurrentCity();
        
        if (currentCity == null) {
            // 如果没有选中的城市，使用默认城市（北京）
            currentCity = new City("北京", "39.9042,116.4074", "北京市", 39.9042, 116.4074);
            cityPreferences.setCurrentCity(currentCity);
        }
        
        final City city = currentCity;
        tvCityName.setText(city.getName());
        
        // 在后台线程加载数据
        executor.execute(() -> {
            try {
                // 获取当前天气
                final Weather currentWeather = WeatherApi.getCurrentWeatherByLocation(
                        city.getLatitude(), city.getLongitude());
                
                // 获取天气预报 - 使用经纬度而不是ID
                final Weather forecastWeather = WeatherApi.getForecastByLocation(
                        city.getLatitude(), city.getLongitude());
                
                // 合并天气预报数据到当前天气对象
                if (forecastWeather != null && forecastWeather.getDailyForecasts() != null) {
                    // 更新当天的最高最低温度
                    currentWeather.setHighTemp(forecastWeather.getHighTemp());
                    currentWeather.setLowTemp(forecastWeather.getLowTemp());
                    
                    // 如果预报中的日出日落时间存在，也更新它们
                    if (forecastWeather.getSunrise() != null && !forecastWeather.getSunrise().isEmpty()) {
                        currentWeather.setSunrise(forecastWeather.getSunrise());
                    }
                    if (forecastWeather.getSunset() != null && !forecastWeather.getSunset().isEmpty()) {
                        currentWeather.setSunset(forecastWeather.getSunset());
                    }
                    
                    // 更新预报列表
                    currentWeather.setDailyForecasts(forecastWeather.getDailyForecasts());
                    
                    // 如果当天预报数据中有更详细的天气描述，也可以更新
                    if (forecastWeather.getDailyForecasts().size() > 0) {
                        Weather.DailyForecast todayForecast = forecastWeather.getDailyForecasts().get(0);
                        if (currentWeather.getWeatherDesc() == null || currentWeather.getWeatherDesc().isEmpty()) {
                            currentWeather.setWeatherDesc(todayForecast.getWeatherDesc());
                        }
                    }
                }
                
                // 在主线程更新UI
                mainHandler.post(() -> updateUI(currentWeather));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(MainActivity.this, 
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
        
        // 设置动态背景
        View weatherMainView = findViewById(R.id.weather_main);
        String weatherCode = getWeatherCodeFromIcon(weather.getWeatherIconResource());
        if (weatherMainView != null && weatherCode != null) {
            weatherMainView.setBackground(WeatherBackgroundUtil.getWeatherBackground(this, weatherCode));
        }
        
        // 更新当前天气
        tvCurrentTemp.setText(String.format("%d°", weather.getCurrentTemp()));
        ivWeatherIcon.setImageResource(weather.getWeatherIconResource());
        tvWeatherDesc.setText(weather.getWeatherDesc());
        tvTempRange.setText(String.format("今日: %d°C ~ %d°C", weather.getLowTemp(), weather.getHighTemp()));
        
        // 更新空气质量
        tvAirQuality.setText(weather.getAirQuality());
        tvAqi.setText(String.valueOf(weather.getAqi()));
        tvPm25.setText(String.valueOf(weather.getPm25()));
        if (tvPm10 != null) {
            tvPm10.setText(String.valueOf(weather.getPm10()));
        }
        
        // 设置空气质量颜色
        setAqiTextColor(weather.getAqi());
        
        // 更新详细信息
        tvWind.setText(weather.getWind());
        tvHumidity.setText(String.format("%d%%", weather.getHumidity()));
        tvSunrise.setText(weather.getSunrise());
        tvSunset.setText(weather.getSunset());
        
        // 更新生活指数 - 使用新的网格布局
        updateLifeIndices(weather);
        
        // 更新天气预报
        updateForecast(weather.getDailyForecasts());
    }
    
    /**
     * 从图标资源ID中提取天气代码
     * @param iconResourceId 图标资源ID
     * @return 天气代码
     */
    private String getWeatherCodeFromIcon(int iconResourceId) {
        // 通过资源名称获取天气代码
        try {
            String resourceName = getResources().getResourceEntryName(iconResourceId);
            if (resourceName.startsWith("icon_")) {
                return resourceName.substring(5); // 去掉"icon_"前缀
            }
            return resourceName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 更新生活指数显示
     */
    private void updateLifeIndices(Weather weather) {
        // 穿衣指数
        updateLifeIndexItem(R.id.index_clothes, "穿衣", "👕", weather.getClothesCategory());
        
        // 运动指数
        updateLifeIndexItem(R.id.index_sport, "运动", "🏃", weather.getSportCategory());
        
        // 紫外线指数
        updateLifeIndexItem(R.id.index_uv, "紫外线", "☂️", weather.getUvCategory());
        
        // 洗车指数
        updateLifeIndexItem(R.id.index_car_wash, "洗车", "🚗", weather.getWashCarCategory());
        
        // 旅游指数
        updateLifeIndexItem(R.id.index_travel, "旅游", "🏖️", weather.getTravelCategory());
        
        // 舒适度指数
        updateLifeIndexItem(R.id.index_comfort, "舒适度", "😊", weather.getComfortCategory());
        
        // 感冒指数
        updateLifeIndexItem(R.id.index_flu, "感冒", "🤧", weather.getFluCategory());
        
        // 空气污染指数 - 使用工具类转换描述
        String convertedDescription = AirPollutionUtil.convertDescription(weather.getAirPollutionCategory());
        updateLifeIndexItem(R.id.index_air_pollution, "空气污染", "🌬️", convertedDescription);
        
        // 交通指数
        updateLifeIndexItem(R.id.index_traffic, "交通", "🚦", weather.getTrafficCategory());
    }
    
    /**
     * 更新单个生活指数项目
     * @param viewId 项目视图ID
     * @param name 指数名称
     * @param icon 指数图标
     * @param category 指数简短描述
     */
    private void updateLifeIndexItem(int viewId, String name, String icon, String category) {
        View indexView = findViewById(viewId);
        if (indexView != null) {
            ImageView ivIcon = indexView.findViewById(R.id.iv_index_icon);
            TextView tvName = indexView.findViewById(R.id.tv_index_name);
            TextView tvCategory = indexView.findViewById(R.id.tv_index_category);
            
            // 设置图标
            if (ivIcon != null) {
                // 根据不同的指数类型设置不同的图标
                int iconRes = getIconResourceForIndex(name);
                if (iconRes != 0) {
                    ivIcon.setImageResource(iconRes);
                }
            }
            
            // 设置名称
            if (tvName != null) {
                tvName.setText(name);
            }
            
            // 设置类别/描述
            if (tvCategory != null && category != null) {
                tvCategory.setText(category);
            }
        }
    }
    
    /**
     * 获取生活指数对应的图标资源ID
     * @param indexName 指数名称
     * @return 图标资源ID
     */
    private int getIconResourceForIndex(String indexName) {
        switch (indexName) {
            case "穿衣":
                return R.drawable.ic_clothes;
            case "运动":
                return R.drawable.ic_sport;
            case "紫外线":
                return R.drawable.ic_uv;
            case "洗车":
                return R.drawable.ic_car_wash;
            case "旅游":
                return R.drawable.ic_travel;
            case "舒适度":
                return R.drawable.ic_comfort;
            case "感冒":
                return R.drawable.ic_flu;
            case "空气污染":
                return R.drawable.ic_air;
            case "交通":
                return R.drawable.ic_traffic;
            default:
                return R.drawable.ic_index_default;
        }
    }
    
    /**
     * 更新天气预报区域
     */
    private void updateForecast(List<Weather.DailyForecast> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            return;
        }
        
        // 清空预报容器
        forecastContainer.removeAllViews();
        
        // 填充预报数据
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < forecasts.size(); i++) {
            Weather.DailyForecast forecast = forecasts.get(i);
            View forecastView = inflater.inflate(R.layout.item_forecast, forecastContainer, false);
            
            // 设置日期
            TextView tvDay = forecastView.findViewById(R.id.tv_day);
            if (i == 0) {
                tvDay.setText(R.string.today);
            } else if (i == 1) {
                tvDay.setText(R.string.tomorrow);
            } else if (i == 2) {
                tvDay.setText(R.string.day_after_tomorrow);
            } else {
                tvDay.setText(forecast.getDayOfWeek());
            }
            
            // 设置天气图标和温度
            ImageView ivForecastIcon = forecastView.findViewById(R.id.iv_weather_icon);
            ivForecastIcon.setImageResource(forecast.getWeatherIconResource());
            
            TextView tvTemperature = forecastView.findViewById(R.id.tv_temperature);
            tvTemperature.setText(String.format("%d°/%d°", forecast.getHighTemp(), forecast.getLowTemp()));
            
            // 添加到容器
            forecastContainer.addView(forecastView);
        }
    }
    
    /**
     * 底部导航栏点击事件
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.navigation_home:
                // 已经在主页，无需处理
                return true;
                
            case R.id.navigation_city:
                // 使用无动画切换方式
                intent = new Intent(this, CityManagerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
                
            case R.id.navigation_air:
                // 使用无动画切换方式
                intent = new Intent(this, com.microntek.weatherapp.ui.AirQualityActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
                
            case R.id.navigation_settings:
                // 使用无动画切换方式
                intent = new Intent(this, com.microntek.weatherapp.ui.SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
        }
        return false;
    }
    
    /**
     * 跳转到城市管理页面
     */
    private void navigateToCityManager() {
        Intent intent = new Intent(this, CityManagerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
    
    private void setAqiTextColor(int aqi) {
        // 创建文字阴影以提高可读性
        float shadowRadius = 2.0f;
        float shadowDx = 0.5f;
        float shadowDy = 0.5f;
        int shadowColor = Color.parseColor("#80000000"); // 半透明黑色阴影
        
        tvAirQuality.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
        tvAqi.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
        
        // 根据AQI值设置tvAirQuality和tvAqi文本的颜色
        if (aqi <= 50) {
            tvAirQuality.setTextColor(getResources().getColor(R.color.good_air_quality));
            tvAqi.setTextColor(getResources().getColor(R.color.good_air_quality));
        } else if (aqi <= 100) {
            tvAirQuality.setTextColor(getResources().getColor(R.color.moderate_air_quality));
            tvAqi.setTextColor(getResources().getColor(R.color.moderate_air_quality));
        } else if (aqi <= 150) {
            tvAirQuality.setTextColor(getResources().getColor(R.color.sensitive_group_air_quality));
            tvAqi.setTextColor(getResources().getColor(R.color.sensitive_group_air_quality));
        } else if (aqi <= 200) {
            tvAirQuality.setTextColor(getResources().getColor(R.color.unhealthy_air_quality));
            tvAqi.setTextColor(getResources().getColor(R.color.unhealthy_air_quality));
        } else if (aqi <= 300) {
            tvAirQuality.setTextColor(getResources().getColor(R.color.very_unhealthy_air_quality));
            tvAqi.setTextColor(getResources().getColor(R.color.very_unhealthy_air_quality));
        } else {
            tvAirQuality.setTextColor(getResources().getColor(R.color.hazardous_air_quality));
            tvAqi.setTextColor(getResources().getColor(R.color.hazardous_air_quality));
        }
    }
} 
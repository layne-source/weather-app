package com.microntek.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;
import com.microntek.weatherapp.util.CityPreferences;
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
    private TextView tvWeatherIcon;
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
    private TextView tvClothesIndex;
    private TextView tvSportIndex;
    private TextView tvUvIndex;
    private TextView tvWashCarIndex;
    private TextView tvTravelIndex;
    private TextView tvComfortIndex;
    private TextView tvAirPollutionIndex;
    private TextView tvTrafficIndex;
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
        tvWeatherIcon = findViewById(R.id.tv_weather_icon);
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
        tvClothesIndex = findViewById(R.id.tv_clothes_index);
        tvSportIndex = findViewById(R.id.tv_sport_index);
        tvUvIndex = findViewById(R.id.tv_uv_index);
        tvWashCarIndex = findViewById(R.id.tv_wash_car_index);
        tvTravelIndex = findViewById(R.id.tv_travel_index);
        tvComfortIndex = findViewById(R.id.tv_comfort_index);
        tvAirPollutionIndex = findViewById(R.id.tv_air_pollution_index);
        tvTrafficIndex = findViewById(R.id.tv_traffic_index);
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
        
        // 更新当前天气
        tvCurrentTemp.setText(String.format("%d°", weather.getCurrentTemp()));
        tvWeatherIcon.setText(weather.getWeatherIcon());
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
        
        // 更新生活指数
        tvClothesIndex.setText(weather.getClothesIndex());
        tvSportIndex.setText(weather.getSportIndex());
        tvUvIndex.setText(weather.getUvIndex());
        
        // 更新额外生活指数（如果UI中有对应控件）
        if (tvWashCarIndex != null) {
            tvWashCarIndex.setText(weather.getWashCarIndex());
        }
        if (tvTravelIndex != null) {
            tvTravelIndex.setText(weather.getTravelIndex());
        }
        if (tvComfortIndex != null) {
            tvComfortIndex.setText(weather.getComfortIndex());
        }
        if (tvAirPollutionIndex != null) {
            tvAirPollutionIndex.setText(weather.getAirPollutionIndex());
        }
        if (tvTrafficIndex != null) {
            tvTrafficIndex.setText(weather.getTrafficIndex());
        }
        
        // 更新天气预报
        updateForecast(weather.getDailyForecasts());
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
            TextView tvWeatherIcon = forecastView.findViewById(R.id.tv_weather_icon);
            tvWeatherIcon.setText(forecast.getWeatherIcon());
            
            TextView tvTemperature = forecastView.findViewById(R.id.tv_temperature);
            tvTemperature.setText(String.format("%d°/%d°", forecast.getHighTemp(), forecast.getLowTemp()));
            
            // 添加到容器
            forecastContainer.addView(forecastView);
        }
    }
    
    /**
     * 跳转到城市管理页面
     */
    private void navigateToCityManager() {
        Intent intent = new Intent(this, CityManagerActivity.class);
        startActivity(intent);
    }
    
    /**
     * 底部导航栏点击事件
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_home:
                // 已经在主页，无需处理
                return true;
            case R.id.navigation_city:
                navigateToCityManager();
                return true;
            case R.id.navigation_air:
                navigateToAirQuality();
                return true;
            case R.id.navigation_settings:
                navigateToSettings();
                return true;
        }
        return false;
    }
    
    private void setAqiTextColor(int aqi) {
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
    
    public void navigateToAirQuality() {
        Intent airIntent = new Intent(this, com.microntek.weatherapp.ui.AirQualityActivity.class);
        startActivity(airIntent);
    }
    
    public void navigateToSettings() {
        Intent settingsIntent = new Intent(this, com.microntek.weatherapp.ui.SettingsActivity.class);
        startActivity(settingsIntent);
    }
} 
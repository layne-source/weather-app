package com.microntek.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.microntek.weatherapp.ui.CityManagerActivity;
import com.microntek.weatherapp.util.AirPollutionUtil;
import com.microntek.weatherapp.util.CityPreferences;
import com.microntek.weatherapp.util.WeatherBackgroundUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.microntek.weatherapp.util.WeatherDataCache;
import com.microntek.weatherapp.util.LocationHelper;
import com.microntek.weatherapp.service.WeatherDataService;
import com.microntek.weatherapp.util.NetworkMonitor;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import com.google.android.material.snackbar.Snackbar;

import android.content.BroadcastReceiver;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    
    private static final String TAG = "MainActivity";
    
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
    
    // 添加新的UI组件
    private TextView tvUpdateTime;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    // 数据处理
    private CityPreferences cityPreferences;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 添加请求码常量
    private static final int REQUEST_CODE_CITY_MANAGER = 1001;
    private static final int REQUEST_CODE_AIR_QUALITY = 1002;
    private static final int REQUEST_CODE_SETTINGS = 1003;
    
    // 添加用于记录返回键按下时间和首次启动标志
    private static boolean isFirstLaunch = true;
    private long lastBackPressTime = 0;
    private static final long EXIT_TIMEOUT = 2000; // 2秒内连按两次返回键退出
    
    // 添加位置工具类
    private LocationHelper locationHelper;
    
    // 网络状态广播接收器
    private BroadcastReceiver networkReceiver;
    
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
        
        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshWeatherData);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        
        // 初始化位置工具类
        locationHelper = new LocationHelper(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationSuccess(double latitude, double longitude) {
                handleLocationSuccess(latitude, longitude);
            }

            @Override
            public void onLocationFailed(String error) {
                Log.e("MainActivity", "定位失败: " + error);
                // 定位失败时直接显示错误，不再自动跳转
                Toast.makeText(MainActivity.this, "定位失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 验证当前城市的缓存完整性
        City currentCity = cityPreferences.getCurrentCity();
        if (currentCity != null) {
            String locationId = currentCity.getLongitude() + "," + currentCity.getLatitude();
            // 在后台线程验证并修复缓存
            executor.execute(() -> {
                try {
                    boolean repaired = WeatherApi.verifyAndRepairCacheByLocation(
                            MainActivity.this, currentCity.getLatitude(), currentCity.getLongitude());
                    if (repaired) {
                        Log.i("MainActivity", "已修复城市 " + currentCity.getName() + " 的部分缓存数据");
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "验证缓存时出错: " + e.getMessage());
                }
            });
        }
        
        // 检查是否需要自动定位
        List<City> savedCities = cityPreferences.getSavedCities();
        if (savedCities.isEmpty()) {
            // 如果没有保存的城市，直接跳转到城市管理页面
            navigateToCityManager();
        } else {
            // 加载天气数据
            loadWeatherData();
        }
        
        // 检查启动标志
        if (getIntent().getBooleanExtra("fromOtherActivity", false)) {
            isFirstLaunch = false;
        }
        
        // 注册网络恢复广播接收器
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NetworkMonitor.ACTION_NETWORK_RESTORED.equals(intent.getAction())) {
                    Log.i(TAG, "网络已恢复，刷新界面数据");
                    // 延迟2秒执行，避免与服务更新冲突
                    mainHandler.postDelayed(() -> {
                        if (!isFinishing()) {
                            // 显示网络恢复提示
                            Snackbar.make(findViewById(R.id.weather_main), 
                                    "网络已恢复连接，正在更新天气数据", 
                                    Snackbar.LENGTH_LONG).show();
                            
                            // 加载天气数据，但不触发后台刷新
                            loadWeatherData(false);
                        }
                    }, 2000);
                }
            }
        };
        IntentFilter networkFilter = new IntentFilter(NetworkMonitor.ACTION_NETWORK_RESTORED);
        registerReceiver(networkReceiver, networkFilter);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        // 如果是从其他Activity返回
        if (intent.getBooleanExtra("fromOtherActivity", false)) {
            isFirstLaunch = false;
        }
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
        
        // 初始化新组件
        tvUpdateTime = findViewById(R.id.tv_update_time);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        
        // 设置点击事件
        tvCityName.setOnClickListener(v -> navigateToCityManager());
    }
    
    /**
     * 加载天气数据
     * @param refreshInBackground 是否在后台执行刷新操作
     */
    private void loadWeatherData(boolean refreshInBackground) {
        City currentCity = cityPreferences.getCurrentCity();
        if (currentCity == null) {
            // 如果没有当前城市，显示提示并跳转到城市管理页面
            Toast.makeText(this, "请先添加城市", Toast.LENGTH_SHORT).show();
            navigateToCityManager();
            return;
        }
        
        final City city = currentCity;
        tvCityName.setText(city.getName());
        
        // 检查是否离线模式
        boolean isOffline = !isNetworkAvailable();
        if (isOffline) {
            // 显示离线模式提示
            Toast.makeText(this, "网络连接不可用，显示缓存数据", Toast.LENGTH_SHORT).show();
        }
        
        // 在后台线程加载数据
        executor.execute(() -> {
            try {
                final String locationId = city.getLongitude() + "," + city.getLatitude();
                
                // 使用带缓存的API获取天气数据
                Weather currentWeather;
                try {
                    currentWeather = WeatherApi.getCurrentWeatherByLocationWithCache(
                            MainActivity.this, city.getLatitude(), city.getLongitude());
                } catch (Exception e) {
                    Log.e("MainActivity", "获取当前天气失败，尝试修复缓存: " + e.getMessage());
                    // 尝试修复缓存
                    WeatherApi.verifyAndRepairCacheByLocation(
                            MainActivity.this, city.getLatitude(), city.getLongitude());
                    // 重试获取数据
                    currentWeather = WeatherApi.getCurrentWeatherByLocationWithCache(
                            MainActivity.this, city.getLatitude(), city.getLongitude());
                }
                
                if (currentWeather == null) {
                    mainHandler.post(() -> {
                        Toast.makeText(MainActivity.this, 
                                "无可用的天气数据，请连接网络后重试", Toast.LENGTH_LONG).show();
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                    return;
                }
                
                // 获取天气预报 - 使用带缓存的API
                Weather forecastWeather;
                try {
                    forecastWeather = WeatherApi.getForecastByLocationWithCache(
                            MainActivity.this, city.getLatitude(), city.getLongitude());
                } catch (Exception e) {
                    Log.e("MainActivity", "获取天气预报失败: " + e.getMessage());
                    forecastWeather = null;
                }
                
                final Weather finalCurrentWeather = currentWeather;
                
                // 合并天气预报数据到当前天气对象
                if (forecastWeather != null && forecastWeather.getDailyForecasts() != null) {
                    try {
                        // 合并天气预报
                        WeatherApi.mergeWeatherData(finalCurrentWeather, forecastWeather);
                    } catch (Exception e) {
                        Log.e("MainActivity", "合并天气预报数据失败: " + e.getMessage());
                    }
                }
                
                // 尝试从缓存获取空气质量数据
                try {
                    WeatherApi.getAirQualityWithCache(MainActivity.this, locationId, finalCurrentWeather);
                } catch (Exception e) {
                    Log.e("MainActivity", "获取空气质量数据失败: " + e.getMessage());
                }
                
                // 尝试从缓存获取生活指数数据
                try {
                    WeatherApi.getLifeIndicesWithCache(MainActivity.this, locationId, finalCurrentWeather);
                } catch (Exception e) {
                    Log.e("MainActivity", "获取生活指数数据失败: " + e.getMessage());
                }
                
                // 在主线程更新UI
                mainHandler.post(() -> {
                    updateUI(finalCurrentWeather);
                    
                    // 如果是下拉刷新，停止刷新动画
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    
                    // 如果不是离线模式且需要后台刷新，在后台刷新数据
                    if (!isOffline && refreshInBackground) {
                        refreshWeatherDataInBackground(city);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, 
                            "数据加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    
                    // 如果是下拉刷新，停止刷新动画
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }
    
    /**
     * 加载天气数据（默认启用后台刷新）
     */
    private void loadWeatherData() {
        loadWeatherData(true);
    }
    
    /**
     * 在后台刷新天气数据
     */
    private void refreshWeatherDataInBackground(City city) {
        executor.execute(() -> {
            try {
                // 从API获取最新数据并更新缓存
                WeatherApi.refreshWeatherDataByLocation(
                        MainActivity.this, city.getLatitude(), city.getLongitude());
                
                // 在主线程中重新加载更新后的数据，但不再触发后台刷新
                mainHandler.post(() -> {
                    loadWeatherData(false);
                });
            } catch (IOException | JSONException e) {
                // 仅记录错误，不向用户显示（因为已经显示了缓存数据）
                Log.e("MainActivity", "后台刷新天气数据失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 手动刷新天气数据
     */
    private void refreshWeatherData() {
        City currentCity = cityPreferences.getCurrentCity();
        if (currentCity == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        // 检查网络连接
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "网络连接不可用，无法刷新数据", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        executor.execute(() -> {
            try {
                // 忽略缓存，直接从API获取最新数据
                final Weather updatedWeather = WeatherApi.refreshWeatherDataByLocation(
                        MainActivity.this, currentCity.getLatitude(), currentCity.getLongitude());
                
                mainHandler.post(() -> {
                    if (updatedWeather != null) {
                        // 加载更新后的数据，但不触发后台刷新
                        loadWeatherData(false);
                        Toast.makeText(MainActivity.this, "天气数据已更新", Toast.LENGTH_SHORT).show();
                        
                        // 通知WeatherDataService数据已更新，以便向第三方应用广播最新数据
                        Log.i(TAG, "通知天气服务更新第三方应用的天气数据");
                        Intent updateIntent = new Intent(WeatherDataService.ACTION_REQUEST_UPDATE);
                        sendBroadcast(updateIntent);
                    } else {
                        Toast.makeText(MainActivity.this, "更新失败，请稍后重试", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(MainActivity.this, 
                            "刷新失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * 检查网络连接状态
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
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
        
        // 更新时间显示
        if (tvUpdateTime != null && weather.getUpdateTimestamp() > 0) {
            tvUpdateTime.setText("更新时间: " + weather.getUpdateTimeString());
        }
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
                startActivityForResult(intent, REQUEST_CODE_CITY_MANAGER);
                overridePendingTransition(0, 0);
                return true;
                
            case R.id.navigation_air:
                // 使用无动画切换方式
                intent = new Intent(this, com.microntek.weatherapp.ui.AirQualityActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(intent, REQUEST_CODE_AIR_QUALITY);
                overridePendingTransition(0, 0);
                return true;
                
            case R.id.navigation_settings:
                // 使用无动画切换方式
                intent = new Intent(this, com.microntek.weatherapp.ui.SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(intent, REQUEST_CODE_SETTINGS);
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
        startActivityForResult(intent, REQUEST_CODE_CITY_MANAGER);
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
    
    // 修改返回键处理
    @Override
    public void onBackPressed() {
        // 如果不是首次启动或者最近2秒内按过返回键，则直接退出
        if (!isFirstLaunch || System.currentTimeMillis() - lastBackPressTime < EXIT_TIMEOUT) {
            finish();
            return;
        }
        
        // 记录时间并提示用户再按一次退出
        lastBackPressTime = System.currentTimeMillis();
        Toast.makeText(this, "再按一次返回键退出应用", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 处理Activity返回结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // 如果有返回数据且包含标志位
        if (resultCode == RESULT_OK && data != null && data.getBooleanExtra("fromOtherActivity", false)) {
            // 设置当前Activity的标志位
            getIntent().putExtra("fromOtherActivity", true);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 确保天气数据广播服务已启动
        ensureWeatherServiceRunning();
        
        // 设置底部导航选中状态为首页
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
        
        // 同步所有城市的缓存数据
        if (cityPreferences != null && !swipeRefreshLayout.isRefreshing()) {
            executor.execute(() -> {
                try {
                    // 首先同步当前城市
                    City currentCity = cityPreferences.getCurrentCity();
                    if (currentCity != null) {
                        // 预加载并更新当前城市的数据
                        cityPreferences.preloadCityWeatherData(currentCity, true);
                    }
                    
                    // 然后同步所有城市
                    cityPreferences.synchronizeAllCitiesCache();
                    
                    // 如果是从其他Activity返回，刷新天气数据UI
                    boolean fromOtherActivity = getIntent().getBooleanExtra("fromOtherActivity", false);
                    if (fromOtherActivity) {
                        // 清除标记，避免多次刷新
                        getIntent().removeExtra("fromOtherActivity");
                        
                        // 在主线程更新UI
                        mainHandler.post(this::loadWeatherData);
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "同步城市缓存时出错: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * 确保天气数据广播服务已启动
     */
    private void ensureWeatherServiceRunning() {
        Intent serviceIntent = new Intent(this, WeatherDataService.class);
        startService(serviceIntent);
    }
    
    /**
     * 通知服务城市已更改
     * 在城市切换后调用
     */
    private void notifyServiceCityChanged() {
        Intent intent = new Intent(WeatherDataService.ACTION_CITY_CHANGED);
        sendBroadcast(intent);
    }
    
    @Override
    protected void onDestroy() {
        // 注销网络恢复广播接收器
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (Exception e) {
                Log.e(TAG, "注销网络接收器失败: " + e.getMessage());
            }
        }
        
        // 清理资源
        if (cityPreferences != null) {
            cityPreferences.onDestroy();
        }
        
        // 关闭线程池
        if (executor instanceof ExecutorService) {
            try {
                ((ExecutorService) executor).shutdown();
                Log.i("MainActivity", "已关闭主线程池");
            } catch (Exception e) {
                Log.e("MainActivity", "关闭线程池出错: " + e.getMessage());
            }
        }
        
        // 释放位置工具类资源
        if (locationHelper != null) {
            locationHelper.onDestroy();
        }
        
        super.onDestroy();
    }
    
    /**
     * 处理位置定位成功
     */
    private void handleLocationSuccess(double latitude, double longitude) {
        // 在后台线程获取城市信息
        executor.execute(() -> {
            try {
                // 获取当前城市信息
                City city = WeatherApi.getCityByLocation(latitude, longitude);
                
                // 切换到主线程添加城市
                mainHandler.post(() -> {
                    // 添加城市并设置为当前城市
                    boolean added = cityPreferences.addCity(city);
                    if (added || cityPreferences.getSavedCities().contains(city)) {
                        cityPreferences.setCurrentCity(city);
                        // 通知服务城市已变更
                        notifyServiceCityChanged();
                        // 加载天气数据
                        loadWeatherData();
                    } else {
                        // 添加失败，跳转到城市管理页面
                        navigateToCityManager();
                    }
                });
            } catch (Exception e) {
                Log.e("MainActivity", "获取城市信息失败: " + e.getMessage());
                mainHandler.post(this::navigateToCityManager);
            }
        });
    }
} 
package com.microntek.weatherapp.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;
import com.microntek.weatherapp.util.CityPreferences;
import com.microntek.weatherapp.util.ExecutorManager;

/**
 * 天气数据广播服务 - 系统级服务，开机自启动，常驻后台
 * 负责定时广播天气数据，供第三方应用使用
 */
public class WeatherDataService extends Service {
    private static final String TAG = "WeatherDataService";
    
    // 广播Action定义
    public static final String ACTION_WEATHER_BROADCAST = "com.microntek.weatherapp.WEATHER_DATA";
    public static final String ACTION_REQUEST_UPDATE = "com.microntek.weatherapp.REQUEST_UPDATE";
    public static final String ACTION_CITY_CHANGED = "com.microntek.weatherapp.CITY_CHANGED";
    public static final String ACTION_SERVICE_STATUS = "com.microntek.weatherapp.SERVICE_STATUS";
    
    // 更新间隔 (1小时)
    private static final long UPDATE_INTERVAL = 60 * 60 * 1000;
    
    // 最近一次广播的天气数据，用于快速响应新连接的客户端
    private Intent lastBroadcastIntent = null;
    
    // 更新线程
    private Thread updateThread;
    private final Object threadLock = new Object();
    private boolean shouldContinue = true;
    
    // 接收更新请求的广播接收器
    private final BroadcastReceiver updateRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_REQUEST_UPDATE.equals(action)) {
                // 检查是否请求状态信息
                boolean statusOnly = intent.getBooleanExtra("status_only", false);
                if (statusOnly) {
                    // 只返回服务状态，不更新天气
                    sendServiceStatus();
                } else {
                    // 正常请求更新天气
                    Log.d(TAG, "收到天气更新请求，开始更新数据");
                    forceUpdateWeatherData();
                }
            } else if (ACTION_CITY_CHANGED.equals(action)) {
                Log.d(TAG, "当前城市已更改，更新天气数据");
                updateWeatherBroadcast();
            }
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "天气数据服务已启动");
        
        // 注册接收更新请求广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REQUEST_UPDATE);
        filter.addAction(ACTION_CITY_CHANGED);
        registerReceiver(updateRequestReceiver, filter);
        
        // 启动时立即广播一次当前天气
        updateWeatherBroadcast();
        
        // 设置定时更新
        startUpdateThread();
    }
    
    private void startUpdateThread() {
        synchronized (threadLock) {
            if (updateThread != null && updateThread.isAlive()) {
                return;
            }
            
            shouldContinue = true;
            updateThread = new Thread(() -> {
                while (shouldContinue) {
                    try {
                        Thread.sleep(UPDATE_INTERVAL);
                        updateWeatherBroadcast();
                    } catch (InterruptedException e) {
                        Log.i(TAG, "更新线程被中断");
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "更新过程中发生错误: " + e.getMessage(), e);
                        try {
                            // 出错后短暂延迟再试
                            Thread.sleep(30000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
            }, "WeatherUpdateThread");
            
            updateThread.start();
        }
    }
    
    private void stopUpdateThread() {
        synchronized (threadLock) {
            shouldContinue = false;
            if (updateThread != null) {
                updateThread.interrupt();
                updateThread = null;
            }
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 捕获异常，防止服务崩溃
        try {
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                if (ACTION_REQUEST_UPDATE.equals(action)) {
                    forceUpdateWeatherData();
                } else if (ACTION_SERVICE_STATUS.equals(action)) {
                    sendServiceStatus();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "处理Intent时发生错误: " + e.getMessage(), e);
        }
        
        // 确保服务被杀死后自动重启
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(updateRequestReceiver);
            stopUpdateThread();
        } catch (Exception e) {
            Log.e(TAG, "服务销毁时发生错误: " + e.getMessage(), e);
        }
        super.onDestroy();
        
        // 服务被意外杀死时，尝试重启
        Intent restartIntent = new Intent(this, WeatherDataService.class);
        startService(restartIntent);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * 发送服务状态信息
     */
    private void sendServiceStatus() {
        Intent statusIntent = new Intent(ACTION_SERVICE_STATUS);
        statusIntent.putExtra("is_running", true);
        statusIntent.putExtra("last_update_time", System.currentTimeMillis());
        statusIntent.putExtra("has_weather_data", lastBroadcastIntent != null);
        sendBroadcast(statusIntent);
        
        // 如果有缓存的天气数据，也一并发送
        if (lastBroadcastIntent != null) {
            sendBroadcast(lastBroadcastIntent);
        }
    }
    
    /**
     * 更新并广播天气数据
     */
    private void updateWeatherBroadcast() {
        ExecutorManager.executeSingle(() -> {
            try {
                CityPreferences preferences = new CityPreferences(this);
                City currentCity = preferences.getCurrentCity();
                
                if (currentCity == null) {
                    Log.w(TAG, "当前无选定城市，无法广播天气数据");
                    return;
                }
                
                // 获取当前城市的天气数据（优先使用缓存）
                Weather weather;
                if (currentCity.isCurrentLocation()) {
                    // 如果是定位城市，使用经纬度获取
                    weather = WeatherApi.getCurrentWeatherByLocationWithCache(
                        this,
                        currentCity.getLatitude(),
                        currentCity.getLongitude()
                    );
                } else {
                    // 普通城市，使用城市ID获取
                    weather = WeatherApi.getCurrentWeatherWithCache(this, currentCity.getId());
                }
                
                // 天气数据获取成功，发送广播
                if (weather != null) {
                    sendWeatherBroadcast(currentCity, weather);
                }
            } catch (Exception e) {
                Log.e(TAG, "天气数据更新失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 强制从网络刷新天气数据
     */
    private void forceUpdateWeatherData() {
        ExecutorManager.executeSingle(() -> {
            try {
                CityPreferences preferences = new CityPreferences(this);
                City currentCity = preferences.getCurrentCity();
                
                if (currentCity == null) {
                    Log.w(TAG, "当前无选定城市，无法更新天气数据");
                    return;
                }
                
                // 强制从网络刷新数据
                Weather weather;
                if (currentCity.isCurrentLocation()) {
                    weather = WeatherApi.refreshWeatherDataByLocation(
                        this,
                        currentCity.getLatitude(),
                        currentCity.getLongitude()
                    );
                } else {
                    weather = WeatherApi.refreshWeatherData(this, currentCity.getId());
                }
                
                // 刷新完成后发送广播
                if (weather != null) {
                    sendWeatherBroadcast(currentCity, weather);
                }
            } catch (Exception e) {
                Log.e(TAG, "强制更新天气数据失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 发送天气数据广播
     */
    private void sendWeatherBroadcast(City city, Weather weather) {
        Intent intent = new Intent(ACTION_WEATHER_BROADCAST);
        
        // 城市信息
        intent.putExtra("city_name", city.getName());
        intent.putExtra("city_id", city.getId());
        intent.putExtra("is_current_location", city.isCurrentLocation());
        
        // 天气数据
        intent.putExtra("current_temp", weather.getCurrentTemp());
        intent.putExtra("high_temp", weather.getHighTemp());
        intent.putExtra("low_temp", weather.getLowTemp());
        intent.putExtra("weather_desc", weather.getWeatherDesc());
        intent.putExtra("weather_icon", weather.getWeatherIcon());
        intent.putExtra("weather_code", weather.getWeatherCode());
        intent.putExtra("humidity", weather.getHumidity());
        intent.putExtra("wind_direction", weather.getWindDirection());
        intent.putExtra("wind_speed", weather.getWindSpeed());
        
        // 空气质量数据
        intent.putExtra("aqi", weather.getAqi());
        intent.putExtra("air_quality", weather.getAirQuality());
        
        // 其他有用信息
        intent.putExtra("feels_like_temp", weather.getFeelsLikeTemp());
        intent.putExtra("last_update_time", weather.getUpdateTime());
        intent.putExtra("update_time", System.currentTimeMillis());
        
        // 发送广播
        sendBroadcast(intent);
        
        // 缓存最近一次广播内容
        lastBroadcastIntent = new Intent(intent);
        
        Log.i(TAG, "已发送天气广播: " + city.getName() + ", " + weather.getWeatherDesc() + ", " + weather.getCurrentTemp() + "°C");
    }
} 
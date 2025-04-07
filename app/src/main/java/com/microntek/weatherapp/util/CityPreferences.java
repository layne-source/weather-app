package com.microntek.weatherapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.microntek.weatherapp.model.City;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 城市偏好设置工具类，用于管理用户保存的城市列表
 */
public class CityPreferences {
    private static final String TAG = "CityPreferences";
    private static final String PREFS_NAME = "weather_prefs";
    private static final String KEY_CITIES = "saved_cities";
    private static final String KEY_CURRENT_CITY = "current_city";
    
    private final SharedPreferences preferences;
    private final Gson gson;
    private final Context context;
    
    // 缓存管理器引用
    private WeatherDataCache weatherDataCache;
    // 线程池用于后台预加载
    private final ExecutorService executor;
    
    public CityPreferences(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        // 初始化缓存管理器
        weatherDataCache = WeatherDataCache.getInstance(context);
        // 创建有界线程池，避免过多任务积压
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 清理资源，在Activity或应用销毁时调用
     */
    public void onDestroy() {
        try {
            if (executor != null && !executor.isShutdown()) {
                // 关闭线程池但允许已提交任务完成
                executor.shutdown();
                Log.i(TAG, "城市线程池已正常关闭");
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭城市线程池出错: " + e.getMessage());
        }
    }
    
    /**
     * 获取保存的城市列表
     */
    public List<City> getSavedCities() {
        String citiesJson = preferences.getString(KEY_CITIES, null);
        if (TextUtils.isEmpty(citiesJson)) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<List<City>>(){}.getType();
        List<City> cities = gson.fromJson(citiesJson, type);
        return cities != null ? cities : new ArrayList<>();
    }
    
    /**
     * 保存城市列表
     */
    public void saveCities(List<City> cities) {
        String citiesJson = gson.toJson(cities);
        preferences.edit().putString(KEY_CITIES, citiesJson).apply();
    }
    
    /**
     * 添加城市到列表
     * @param city 要添加的城市
     * @return 是否成功添加
     */
    public boolean addCity(City city) {
        if (city == null) {
            Log.e(TAG, "尝试添加空城市");
            return false;
        }
        
        List<City> cities = getSavedCities();
        
        // 检查是否已存在相同城市
        boolean exists = false;
        for (City existingCity : cities) {
            if (existingCity.getId().equals(city.getId())) {
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            // 添加到列表
            cities.add(city);
            saveCities(cities);
            
            // 在后台预加载城市天气数据并缓存
            preloadCityWeatherData(city, true);
            
            // 如果没有当前城市，则设置该城市为当前城市
            if (getCurrentCity() == null) {
                setCurrentCity(city);
            }
            
            Log.i(TAG, "成功添加城市: " + city.getName());
            return true;
        } else {
            Log.w(TAG, "城市 " + city.getName() + " 已存在，无需添加");
            return false;
        }
    }
    
    /**
     * 删除城市
     * @param city 要删除的城市
     * @return 是否成功删除
     */
    public boolean removeCity(City city) {
        if (city == null) {
            Log.e(TAG, "尝试删除空城市");
            return false;
        }
        
        List<City> cities = getSavedCities();
        boolean removed = false;
        
        for (int i = 0; i < cities.size(); i++) {
            if (cities.get(i).getId().equals(city.getId())) {
                cities.remove(i);
                removed = true;
                break;
            }
        }
        
        if (removed) {
            saveCities(cities);
            
            // 在后台清除被删除城市的所有缓存数据
            clearCityCache(city);
            
            // 如果删除的是当前选中的城市，则清除当前城市或选择新的当前城市
            City currentCity = getCurrentCity();
            if (currentCity != null && currentCity.getId().equals(city.getId())) {
                if (cities.isEmpty()) {
                    clearCurrentCity();
                } else {
                    // 如果还有其他城市，选择第一个作为当前城市
                    setCurrentCity(cities.get(0));
                }
            }
            
            Log.i(TAG, "成功删除城市: " + city.getName());
            return true;
        } else {
            Log.w(TAG, "城市 " + city.getName() + " 不存在，无法删除");
            return false;
        }
    }
    
    /**
     * 获取当前选中的城市
     */
    public City getCurrentCity() {
        String cityJson = preferences.getString(KEY_CURRENT_CITY, null);
        if (TextUtils.isEmpty(cityJson)) {
            return null;
        }
        
        return gson.fromJson(cityJson, City.class);
    }
    
    /**
     * 设置当前选中的城市
     * @param city 要设置为当前城市的城市对象
     * @return 是否设置成功
     */
    public boolean setCurrentCity(City city) {
        if (city == null) {
            Log.e(TAG, "尝试设置空城市为当前城市");
            return false;
        }
        
        String cityJson = gson.toJson(city);
        preferences.edit().putString(KEY_CURRENT_CITY, cityJson).apply();
        
        // 优先预加载当前城市的天气数据
        preloadCityWeatherData(city, true);
        
        Log.i(TAG, "已设置 " + city.getName() + " 为当前城市");
        return true;
    }
    
    /**
     * 清除当前选中的城市
     */
    public void clearCurrentCity() {
        preferences.edit().remove(KEY_CURRENT_CITY).apply();
        Log.i(TAG, "已清除当前城市");
    }
    
    /**
     * 预加载城市天气数据并缓存
     * @param city 城市对象
     * @param highPriority 是否高优先级
     */
    public void preloadCityWeatherData(City city, boolean highPriority) {
        if (city == null) {
            Log.e(TAG, "尝试预加载空城市数据");
            return;
        }
        
        Runnable preloadTask = () -> {
            try {
                String cityId = city.getId();
                double lat = city.getLatitude();
                double lon = city.getLongitude();
                
                Log.i(TAG, "开始预加载城市 " + city.getName() + " 的天气数据");
                
                // 根据城市类型选择加载方法
                if (city.isCurrentLocation() || cityId.contains(",")) {
                    // 使用经纬度获取天气数据
                    String locationId = lon + "," + lat;
                    
                    // 从API获取当前天气数据
                    com.microntek.weatherapp.api.WeatherApi.refreshWeatherDataByLocation(
                            context, lat, lon);
                } else {
                    // 使用城市ID获取天气数据
                    com.microntek.weatherapp.api.WeatherApi.refreshWeatherData(
                            context, cityId);
                }
                
                Log.i(TAG, "城市 " + city.getName() + " 的天气数据预加载完成");
            } catch (Exception e) {
                Log.e(TAG, "预加载城市 " + city.getName() + " 数据失败: " + e.getMessage());
                // 预加载失败不影响用户操作，仅记录错误
            }
        };
        
        // 高优先级任务直接执行，低优先级任务提交到线程池
        if (highPriority) {
            executor.submit(preloadTask);
        } else {
            executor.execute(preloadTask);
        }
    }
    
    /**
     * 清除城市的所有缓存数据
     * @param city 城市对象
     */
    private void clearCityCache(City city) {
        if (city == null) {
            Log.e(TAG, "尝试清除空城市的缓存");
            return;
        }
        
        try {
            String cityId = city.getId();
            
            // 如果是经纬度类型的城市ID，使用经纬度格式
            if (city.isCurrentLocation() || cityId.contains(",")) {
                String locationId = city.getLongitude() + "," + city.getLatitude();
                weatherDataCache.clearCache(locationId);
            } else {
                weatherDataCache.clearCache(cityId);
            }
            
            Log.i(TAG, "已清除城市 " + city.getName() + " 的缓存数据");
        } catch (Exception e) {
            Log.e(TAG, "清除城市缓存失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步所有城市的缓存数据
     * 该方法可在应用启动或从后台恢复时调用，确保所有城市数据为最新
     */
    public void synchronizeAllCitiesCache() {
        List<City> cities = getSavedCities();
        if (cities.isEmpty()) {
            return;
        }
        
        // 获取当前城市，优先同步
        City currentCity = getCurrentCity();
        if (currentCity != null) {
            // 高优先级预加载当前城市
            preloadCityWeatherData(currentCity, true);
            
            // 从列表中移除当前城市，避免重复加载
            for (int i = 0; i < cities.size(); i++) {
                if (cities.get(i).getId().equals(currentCity.getId())) {
                    cities.remove(i);
                    break;
                }
            }
        }
        
        // 低优先级加载其他城市
        for (City city : cities) {
            preloadCityWeatherData(city, false);
        }
        
        Log.i(TAG, "已开始同步所有城市缓存数据");
    }
    
    /**
     * 验证并修复所有城市的缓存数据
     */
    public void verifyAndRepairAllCitiesCache() {
        executor.execute(() -> {
            try {
                List<City> cities = getSavedCities();
                for (City city : cities) {
                    String cityId = city.getId();
                    
                    // 如果是经纬度类型的城市ID，使用经纬度格式
                    if (city.isCurrentLocation() || cityId.contains(",")) {
                        double lat = city.getLatitude();
                        double lon = city.getLongitude();
                        com.microntek.weatherapp.api.WeatherApi.verifyAndRepairCacheByLocation(
                                context, lat, lon);
                    } else {
                        com.microntek.weatherapp.api.WeatherApi.verifyAndRepairCache(
                                context, cityId);
                    }
                }
                Log.i(TAG, "所有城市缓存验证和修复完成");
            } catch (Exception e) {
                Log.e(TAG, "验证修复城市缓存时出错: " + e.getMessage());
            }
        });
    }
} 
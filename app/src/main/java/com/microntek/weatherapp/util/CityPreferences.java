package com.microntek.weatherapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.microntek.weatherapp.model.City;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;

/**
 * 城市偏好设置工具类，用于管理用户保存的城市列表
 */
public class CityPreferences {
    private static final String PREFS_NAME = "weather_prefs";
    private static final String KEY_CITIES = "saved_cities";
    private static final String KEY_CURRENT_CITY = "current_city";
    
    private final SharedPreferences preferences;
    private final Gson gson;
    private final Context context;
    
    // 添加缓存管理器引用
    private WeatherDataCache weatherDataCache;
    // 添加线程池用于后台预加载
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public CityPreferences(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        // 初始化缓存管理器
        weatherDataCache = WeatherDataCache.getInstance(context);
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
     */
    public void addCity(City city) {
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
            cities.add(city);
            saveCities(cities);
            
            // 新增：预加载城市天气数据并缓存
            preloadCityWeatherData(city);
        }
    }
    
    /**
     * 删除城市
     */
    public void removeCity(City city) {
        List<City> cities = getSavedCities();
        
        for (int i = 0; i < cities.size(); i++) {
            if (cities.get(i).getId().equals(city.getId())) {
                cities.remove(i);
                break;
            }
        }
        
        saveCities(cities);
        
        // 新增：清除被删除城市的所有缓存数据
        clearCityCache(city);
        
        // 如果删除的是当前选中的城市，则清除当前城市
        City currentCity = getCurrentCity();
        if (currentCity != null && currentCity.getId().equals(city.getId())) {
            clearCurrentCity();
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
     */
    public void setCurrentCity(City city) {
        String cityJson = gson.toJson(city);
        preferences.edit().putString(KEY_CURRENT_CITY, cityJson).apply();
    }
    
    /**
     * 清除当前选中的城市
     */
    public void clearCurrentCity() {
        preferences.edit().remove(KEY_CURRENT_CITY).apply();
    }
    
    /**
     * 预加载城市天气数据并缓存
     */
    private void preloadCityWeatherData(City city) {
        executor.execute(() -> {
            try {
                String cityId = city.getId();
                double lat = city.getLatitude();
                double lon = city.getLongitude();
                
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
            } catch (Exception e) {
                Log.e("CityPreferences", "预加载城市数据失败: " + e.getMessage());
                // 预加载失败不影响用户操作，仅记录错误
            }
        });
    }
    
    /**
     * 清除城市的所有缓存数据
     */
    private void clearCityCache(City city) {
        String cityId = city.getId();
        
        // 如果是经纬度类型的城市ID，使用经纬度格式
        if (city.isCurrentLocation() || cityId.contains(",")) {
            String locationId = city.getLongitude() + "," + city.getLatitude();
            weatherDataCache.clearCache(locationId);
        } else {
            weatherDataCache.clearCache(cityId);
        }
        
        Log.i("CityPreferences", "已清除城市 " + city.getName() + " 的缓存数据");
    }
} 
package com.microntek.weatherapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 天气数据缓存管理器
 * 负责管理天气应用所有数据的本地缓存
 */
public class WeatherDataCache {
    // 缓存常量定义
    private static final String CACHE_PREFS_NAME = "weather_cache";
    private static final String KEY_PREFIX_CURRENT = "current_";
    private static final String KEY_PREFIX_FORECAST = "forecast_";
    private static final String KEY_PREFIX_AIR = "air_";
    private static final String KEY_PREFIX_INDICES = "indices_";
    private static final String KEY_PREFIX_CITY_SEARCH = "city_search_";
    private static final String KEY_PREFIX_GEO = "geo_";
    private static final String KEY_PREFIX_TIMESTAMP = "timestamp_";
    
    // 缓存有效期设置（毫秒）
    private static final long CACHE_DURATION_CURRENT = 30 * 60 * 1000;     // 30分钟
    private static final long CACHE_DURATION_FORECAST = 3 * 60 * 60 * 1000; // 3小时
    private static final long CACHE_DURATION_AIR = 60 * 60 * 1000;          // 1小时
    private static final long CACHE_DURATION_INDICES = 6 * 60 * 60 * 1000;  // 6小时
    private static final long CACHE_DURATION_CITY_SEARCH = 7 * 24 * 60 * 60 * 1000; // 7天
    private static final long CACHE_DURATION_GEO = 30 * 24 * 60 * 60 * 1000;       // 30天
    
    // 存储组件
    private final SharedPreferences cachePreferences;
    private final Gson gson;
    
    // 内存缓存
    private final LruCache<String, Object> memoryCache;
    
    // 单例实现
    private static WeatherDataCache instance;
    private final Context context;
    
    private WeatherDataCache(Context context) {
        this.context = context.getApplicationContext();
        cachePreferences = this.context.getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        
        // 设置内存缓存大小，最多缓存20个城市的数据
        final int cacheSize = 20;
        memoryCache = new LruCache<>(cacheSize);
        
        // 启动时自动清理过期缓存
        cleanExpiredCache();
    }
    
    /**
     * 获取WeatherDataCache单例实例
     */
    public static synchronized WeatherDataCache getInstance(Context context) {
        if (instance == null) {
            instance = new WeatherDataCache(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 缓存当前天气数据
     */
    public void cacheCurrentWeather(String cityId, Weather weather) {
        if (weather == null) return;
        
        weather.setUpdateTimestamp(System.currentTimeMillis());
        String key = KEY_PREFIX_CURRENT + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 同时更新内存缓存和磁盘缓存
        memoryCache.put(key, weather);
        
        String weatherJson = gson.toJson(weather);
        long timestamp = System.currentTimeMillis();
        
        cachePreferences.edit()
                .putString(key, weatherJson)
                .putLong(timestampKey, timestamp)
                .apply();
    }
    
    /**
     * 获取缓存的当前天气
     */
    public Weather getCachedCurrentWeather(String cityId) {
        String key = KEY_PREFIX_CURRENT + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 先检查内存缓存
        Object cachedWeather = memoryCache.get(key);
        if (cachedWeather instanceof Weather) {
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            if (!isCacheExpired(timestamp, CACHE_DURATION_CURRENT)) {
                return (Weather) cachedWeather;
            }
        }
        
        // 内存缓存不存在或已过期，检查磁盘缓存
        String weatherJson = cachePreferences.getString(key, null);
        long timestamp = cachePreferences.getLong(timestampKey, 0);
        
        if (weatherJson != null && !isCacheExpired(timestamp, CACHE_DURATION_CURRENT)) {
            Weather weather = gson.fromJson(weatherJson, Weather.class);
            memoryCache.put(key, weather); // 更新内存缓存
            return weather;
        }
        
        return null; // 缓存不存在或已过期
    }
    
    /**
     * 缓存天气预报数据
     */
    public void cacheForecastWeather(String cityId, Weather weather) {
        if (weather == null) return;
        
        weather.setUpdateTimestamp(System.currentTimeMillis());
        String key = KEY_PREFIX_FORECAST + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 更新内存缓存
        memoryCache.put(key, weather);
        
        String weatherJson = gson.toJson(weather);
        long timestamp = System.currentTimeMillis();
        
        cachePreferences.edit()
                .putString(key, weatherJson)
                .putLong(timestampKey, timestamp)
                .apply();
    }
    
    /**
     * 获取缓存的天气预报
     */
    public Weather getCachedForecastWeather(String cityId) {
        String key = KEY_PREFIX_FORECAST + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 先检查内存缓存
        Object cachedWeather = memoryCache.get(key);
        if (cachedWeather instanceof Weather) {
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            if (!isCacheExpired(timestamp, CACHE_DURATION_FORECAST)) {
                return (Weather) cachedWeather;
            }
        }
        
        // 内存缓存不存在或已过期，检查磁盘缓存
        String weatherJson = cachePreferences.getString(key, null);
        long timestamp = cachePreferences.getLong(timestampKey, 0);
        
        if (weatherJson != null && !isCacheExpired(timestamp, CACHE_DURATION_FORECAST)) {
            Weather weather = gson.fromJson(weatherJson, Weather.class);
            memoryCache.put(key, weather); // 更新内存缓存
            return weather;
        }
        
        return null;
    }
    
    /**
     * 缓存空气质量数据
     */
    public void cacheAirQuality(String cityId, Weather weather) {
        if (weather == null) return;
        
        // 只保存空气质量相关数据
        Weather airData = new Weather();
        airData.setUpdateTimestamp(System.currentTimeMillis());
        airData.setAirQuality(weather.getAirQuality());
        airData.setAqi(weather.getAqi());
        airData.setPm25(weather.getPm25());
        airData.setPm10(weather.getPm10());
        airData.setCo(weather.getCo());
        airData.setSo2(weather.getSo2());
        airData.setNo2(weather.getNo2());
        airData.setO3(weather.getO3());
        
        String key = KEY_PREFIX_AIR + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 更新内存缓存
        memoryCache.put(key, airData);
        
        String airJson = gson.toJson(airData);
        long timestamp = System.currentTimeMillis();
        
        cachePreferences.edit()
                .putString(key, airJson)
                .putLong(timestampKey, timestamp)
                .apply();
    }
    
    /**
     * 获取缓存的空气质量数据
     */
    public Weather getCachedAirQuality(String cityId) {
        String key = KEY_PREFIX_AIR + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 先检查内存缓存
        Object cachedAir = memoryCache.get(key);
        if (cachedAir instanceof Weather) {
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            if (!isCacheExpired(timestamp, CACHE_DURATION_AIR)) {
                return (Weather) cachedAir;
            }
        }
        
        // 内存缓存不存在或已过期，检查磁盘缓存
        String airJson = cachePreferences.getString(key, null);
        long timestamp = cachePreferences.getLong(timestampKey, 0);
        
        if (airJson != null && !isCacheExpired(timestamp, CACHE_DURATION_AIR)) {
            Weather air = gson.fromJson(airJson, Weather.class);
            memoryCache.put(key, air); // 更新内存缓存
            return air;
        }
        
        return null;
    }
    
    /**
     * 缓存生活指数数据
     */
    public void cacheLifeIndices(String cityId, Weather weather) {
        if (weather == null) return;
        
        // 只保存生活指数相关数据
        Weather indicesData = new Weather();
        indicesData.setUpdateTimestamp(System.currentTimeMillis());
        indicesData.setClothesIndex(weather.getClothesIndex());
        indicesData.setClothesCategory(weather.getClothesCategory());
        indicesData.setSportIndex(weather.getSportIndex());
        indicesData.setSportCategory(weather.getSportCategory());
        indicesData.setUvIndex(weather.getUvIndex());
        indicesData.setUvCategory(weather.getUvCategory());
        indicesData.setWashCarIndex(weather.getWashCarIndex());
        indicesData.setWashCarCategory(weather.getWashCarCategory());
        indicesData.setTravelIndex(weather.getTravelIndex());
        indicesData.setTravelCategory(weather.getTravelCategory());
        indicesData.setComfortIndex(weather.getComfortIndex());
        indicesData.setComfortCategory(weather.getComfortCategory());
        indicesData.setAirPollutionIndex(weather.getAirPollutionIndex());
        indicesData.setAirPollutionCategory(weather.getAirPollutionCategory());
        indicesData.setTrafficIndex(weather.getTrafficIndex());
        indicesData.setTrafficCategory(weather.getTrafficCategory());
        indicesData.setFluIndex(weather.getFluIndex());
        indicesData.setFluCategory(weather.getFluCategory());
        
        String key = KEY_PREFIX_INDICES + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 更新内存缓存
        memoryCache.put(key, indicesData);
        
        String indicesJson = gson.toJson(indicesData);
        long timestamp = System.currentTimeMillis();
        
        cachePreferences.edit()
                .putString(key, indicesJson)
                .putLong(timestampKey, timestamp)
                .apply();
    }
    
    /**
     * 获取缓存的生活指数数据
     */
    public Weather getCachedLifeIndices(String cityId) {
        String key = KEY_PREFIX_INDICES + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 先检查内存缓存
        Object cachedIndices = memoryCache.get(key);
        if (cachedIndices instanceof Weather) {
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            if (!isCacheExpired(timestamp, CACHE_DURATION_INDICES)) {
                return (Weather) cachedIndices;
            }
        }
        
        // 内存缓存不存在或已过期，检查磁盘缓存
        String indicesJson = cachePreferences.getString(key, null);
        long timestamp = cachePreferences.getLong(timestampKey, 0);
        
        if (indicesJson != null && !isCacheExpired(timestamp, CACHE_DURATION_INDICES)) {
            Weather indices = gson.fromJson(indicesJson, Weather.class);
            memoryCache.put(key, indices); // 更新内存缓存
            return indices;
        }
        
        return null;
    }
    
    /**
     * 缓存城市搜索结果
     */
    public void cacheCitySearchResult(String query, List<City> cities) {
        if (cities == null || cities.isEmpty()) return;
        
        String key = KEY_PREFIX_CITY_SEARCH + query.toLowerCase();
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 更新内存缓存
        memoryCache.put(key, cities);
        
        String citiesJson = gson.toJson(cities);
        long timestamp = System.currentTimeMillis();
        
        cachePreferences.edit()
                .putString(key, citiesJson)
                .putLong(timestampKey, timestamp)
                .apply();
    }
    
    /**
     * 获取缓存的城市搜索结果
     */
    public List<City> getCachedCitySearchResult(String query) {
        String key = KEY_PREFIX_CITY_SEARCH + query.toLowerCase();
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 先检查内存缓存
        Object cachedCities = memoryCache.get(key);
        if (cachedCities instanceof List<?>) {
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            if (!isCacheExpired(timestamp, CACHE_DURATION_CITY_SEARCH)) {
                try {
                    @SuppressWarnings("unchecked")
                    List<City> cities = (List<City>) cachedCities;
                    return cities;
                } catch (ClassCastException e) {
                    memoryCache.remove(key);
                }
            }
        }
        
        // 内存缓存不存在或已过期，检查磁盘缓存
        String citiesJson = cachePreferences.getString(key, null);
        long timestamp = cachePreferences.getLong(timestampKey, 0);
        
        if (citiesJson != null && !isCacheExpired(timestamp, CACHE_DURATION_CITY_SEARCH)) {
            Type type = new TypeToken<List<City>>(){}.getType();
            List<City> cities = gson.fromJson(citiesJson, type);
            if (cities != null) {
                memoryCache.put(key, cities); // 更新内存缓存
                return cities;
            }
        }
        
        return null;
    }
    
    /**
     * 缓存地理编码信息（城市名称）
     */
    public void cacheGeoInfo(String locationId, String cityName) {
        if (TextUtils.isEmpty(cityName)) return;
        
        String key = KEY_PREFIX_GEO + locationId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 更新内存缓存
        memoryCache.put(key, cityName);
        
        long timestamp = System.currentTimeMillis();
        
        cachePreferences.edit()
                .putString(key, cityName)
                .putLong(timestampKey, timestamp)
                .apply();
    }
    
    /**
     * 获取缓存的地理编码信息
     */
    public String getCachedGeoInfo(String locationId) {
        String key = KEY_PREFIX_GEO + locationId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        // 先检查内存缓存
        Object cachedName = memoryCache.get(key);
        if (cachedName instanceof String) {
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            if (!isCacheExpired(timestamp, CACHE_DURATION_GEO)) {
                return (String) cachedName;
            }
        }
        
        // 内存缓存不存在或已过期，检查磁盘缓存
        String cityName = cachePreferences.getString(key, null);
        long timestamp = cachePreferences.getLong(timestampKey, 0);
        
        if (cityName != null && !isCacheExpired(timestamp, CACHE_DURATION_GEO)) {
            memoryCache.put(key, cityName); // 更新内存缓存
            return cityName;
        }
        
        return null;
    }
    
    /**
     * 清除指定城市的所有缓存
     */
    public void clearCache(String cityId) {
        String[] types = {
            KEY_PREFIX_CURRENT, 
            KEY_PREFIX_FORECAST, 
            KEY_PREFIX_AIR, 
            KEY_PREFIX_INDICES
        };
        
        for (String prefix : types) {
            String key = prefix + cityId;
            String timestampKey = KEY_PREFIX_TIMESTAMP + key;
            
            // 删除内存缓存
            memoryCache.remove(key);
            
            // 删除磁盘缓存
            cachePreferences.edit()
                    .remove(key)
                    .remove(timestampKey)
                    .apply();
        }
        
        Log.i("WeatherDataCache", "已清除城市ID: " + cityId + " 的所有缓存");
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        // 清除内存缓存
        memoryCache.evictAll();
        
        // 清除磁盘缓存
        cachePreferences.edit().clear().apply();
        
        Log.i("WeatherDataCache", "已清除所有缓存");
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanExpiredCache() {
        Map<String, ?> allPrefs = cachePreferences.getAll();
        SharedPreferences.Editor editor = cachePreferences.edit();
        long now = System.currentTimeMillis();
        int cleanedCount = 0;
        
        for (String key : allPrefs.keySet()) {
            if (key.startsWith(KEY_PREFIX_TIMESTAMP)) {
                long timestamp = (long) allPrefs.get(key);
                String dataKey = key.substring(KEY_PREFIX_TIMESTAMP.length());
                
                // 根据不同类型的数据，应用不同的过期策略
                long duration = getCacheDuration(dataKey);
                
                // 超过有效期的数据自动清理
                if (now - timestamp > duration) {
                    editor.remove(key);
                    editor.remove(dataKey);
                    
                    // 从内存缓存中移除
                    memoryCache.remove(dataKey);
                    cleanedCount++;
                }
            }
        }
        
        editor.apply();
        Log.i("WeatherDataCache", "清理过期缓存完成，共清理 " + cleanedCount + " 条");
    }
    
    /**
     * 获取所有已缓存城市的ID
     */
    public List<String> getAllCachedCityIds() {
        List<String> cityIds = new ArrayList<>();
        Map<String, ?> allPrefs = cachePreferences.getAll();
        
        for (String key : allPrefs.keySet()) {
            if (key.startsWith(KEY_PREFIX_CURRENT) && !key.startsWith(KEY_PREFIX_TIMESTAMP)) {
                // 从键中提取城市ID
                String cityId = key.substring(KEY_PREFIX_CURRENT.length());
                cityIds.add(cityId);
            }
        }
        
        return cityIds;
    }
    
    /**
     * 获取指定城市的缓存状态
     * @return 一个包含不同类型缓存状态的Map
     */
    public Map<String, Boolean> getCityCacheStatus(String cityId) {
        Map<String, Boolean> status = new HashMap<>();
        
        status.put("current", isCacheValid(KEY_PREFIX_CURRENT + cityId, CACHE_DURATION_CURRENT));
        status.put("forecast", isCacheValid(KEY_PREFIX_FORECAST + cityId, CACHE_DURATION_FORECAST));
        status.put("air", isCacheValid(KEY_PREFIX_AIR + cityId, CACHE_DURATION_AIR));
        status.put("indices", isCacheValid(KEY_PREFIX_INDICES + cityId, CACHE_DURATION_INDICES));
        
        return status;
    }
    
    /**
     * 检查网络连接状态
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    /**
     * 检查指定类型的缓存是否有效
     */
    private boolean isCacheValid(String dataKey, long duration) {
        if (!cachePreferences.contains(dataKey)) {
            return false;
        }
        
        String timestampKey = KEY_PREFIX_TIMESTAMP + dataKey;
        long timestamp = cachePreferences.getLong(timestampKey, 0);
        return !isCacheExpired(timestamp, duration);
    }
    
    /**
     * 检查缓存是否过期
     */
    private boolean isCacheExpired(long timestamp, long duration) {
        return System.currentTimeMillis() - timestamp > duration;
    }
    
    /**
     * 根据数据键前缀获取对应的缓存有效期
     */
    private long getCacheDuration(String dataKey) {
        if (dataKey.startsWith(KEY_PREFIX_CURRENT)) {
            return CACHE_DURATION_CURRENT;
        } else if (dataKey.startsWith(KEY_PREFIX_FORECAST)) {
            return CACHE_DURATION_FORECAST;
        } else if (dataKey.startsWith(KEY_PREFIX_AIR)) {
            return CACHE_DURATION_AIR;
        } else if (dataKey.startsWith(KEY_PREFIX_INDICES)) {
            return CACHE_DURATION_INDICES;
        } else if (dataKey.startsWith(KEY_PREFIX_CITY_SEARCH)) {
            return CACHE_DURATION_CITY_SEARCH;
        } else if (dataKey.startsWith(KEY_PREFIX_GEO)) {
            return CACHE_DURATION_GEO;
        }
        return CACHE_DURATION_CURRENT; // 默认
    }
} 
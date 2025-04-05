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
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
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
    private static final String TAG = "WeatherDataCache";
    
    // 缓存常量定义
    private static final String CACHE_PREFS_NAME = "weather_cache";
    private static final String BACKUP_PREFS_NAME = "weather_cache_backup"; // 备份缓存
    private static final String KEY_PREFIX_CURRENT = "current_";
    private static final String KEY_PREFIX_FORECAST = "forecast_";
    private static final String KEY_PREFIX_AIR = "air_";
    private static final String KEY_PREFIX_INDICES = "indices_";
    private static final String KEY_PREFIX_CITY_SEARCH = "city_search_";
    private static final String KEY_PREFIX_GEO = "geo_";
    private static final String KEY_PREFIX_TIMESTAMP = "timestamp_";
    private static final String KEY_PREFIX_ERROR_COUNT = "error_count_"; // 错误计数前缀

    // 错误恢复常量
    private static final int MAX_ERROR_COUNT = 3; // 最大错误次数，超过此次数将重置缓存
    private static final long BACKUP_INTERVAL = 24 * 60 * 60 * 1000; // 备份间隔（24小时）
    private static final String KEY_LAST_BACKUP_TIME = "last_backup_time"; // 上次备份时间
    
    // 缓存有效期设置（毫秒）
    private static final long CACHE_DURATION_CURRENT = 30 * 60 * 1000;     // 30分钟
    private static final long CACHE_DURATION_FORECAST = 3 * 60 * 60 * 1000; // 3小时
    private static final long CACHE_DURATION_AIR = 60 * 60 * 1000;          // 1小时
    private static final long CACHE_DURATION_INDICES = 6 * 60 * 60 * 1000;  // 6小时
    private static final long CACHE_DURATION_CITY_SEARCH = 7 * 24 * 60 * 60 * 1000; // 7天
    private static final long CACHE_DURATION_GEO = 30 * 24 * 60 * 60 * 1000;       // 30天
    
    // 存储组件
    private final SharedPreferences cachePreferences;
    private final SharedPreferences backupPreferences; // 备份缓存存储
    private final Gson gson;
    
    // 内存缓存
    private final LruCache<String, Object> memoryCache;
    
    // 单例实现
    private static WeatherDataCache instance;
    private final Context context;
    private final ExecutorService backupExecutor; // 备份线程池
    
    private WeatherDataCache(Context context) {
        this.context = context.getApplicationContext();
        cachePreferences = this.context.getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE);
        backupPreferences = this.context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        backupExecutor = Executors.newSingleThreadExecutor();
        
        // 设置内存缓存大小，最多缓存20个城市的数据
        final int cacheSize = 20;
        memoryCache = new LruCache<>(cacheSize);
        
        // 启动时自动清理过期缓存
        cleanExpiredCache();
        
        // 检查是否需要创建备份
        checkAndCreateBackup();
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
        
        try {
            // 先检查内存缓存
            Object cachedWeather = memoryCache.get(key);
            if (cachedWeather instanceof Weather) {
                long timestamp = cachePreferences.getLong(timestampKey, 0);
                if (!isCacheExpired(timestamp, CACHE_DURATION_CURRENT)) {
                    // 成功获取缓存，重置错误计数
                    resetErrorCount(key);
                    return (Weather) cachedWeather;
                }
            }
            
            // 内存缓存不存在或已过期，检查磁盘缓存
            String weatherJson = cachePreferences.getString(key, null);
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            
            if (weatherJson != null && !isCacheExpired(timestamp, CACHE_DURATION_CURRENT)) {
                try {
                    Weather weather = gson.fromJson(weatherJson, Weather.class);
                    memoryCache.put(key, weather); // 更新内存缓存
                    // 成功获取缓存，重置错误计数
                    resetErrorCount(key);
                    return weather;
                } catch (JsonSyntaxException e) {
                    // 解析失败，尝试从备份恢复
                    Log.e(TAG, "缓存数据解析失败: " + e.getMessage());
                    incrementErrorCount(key);
                    Weather weather = restoreFromBackup(key, Weather.class);
                    if (weather != null) {
                        return weather;
                    }
                }
            }
            
            return null; // 缓存不存在或已过期
        } catch (Exception e) {
            Log.e(TAG, "获取缓存异常: " + e.getMessage());
            incrementErrorCount(key);
            return restoreFromBackup(key, Weather.class);
        }
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
        
        try {
            // 先检查内存缓存
            Object cachedWeather = memoryCache.get(key);
            if (cachedWeather instanceof Weather) {
                long timestamp = cachePreferences.getLong(timestampKey, 0);
                if (!isCacheExpired(timestamp, CACHE_DURATION_FORECAST)) {
                    // 成功获取缓存，重置错误计数
                    resetErrorCount(key);
                    return (Weather) cachedWeather;
                }
            }
            
            // 内存缓存不存在或已过期，检查磁盘缓存
            String weatherJson = cachePreferences.getString(key, null);
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            
            if (weatherJson != null && !isCacheExpired(timestamp, CACHE_DURATION_FORECAST)) {
                try {
                    Weather weather = gson.fromJson(weatherJson, Weather.class);
                    memoryCache.put(key, weather); // 更新内存缓存
                    // 成功获取缓存，重置错误计数
                    resetErrorCount(key);
                    return weather;
                } catch (JsonSyntaxException e) {
                    // 解析失败，尝试从备份恢复
                    Log.e(TAG, "缓存数据解析失败: " + e.getMessage());
                    incrementErrorCount(key);
                    Weather weather = restoreFromBackup(key, Weather.class);
                    if (weather != null) {
                        return weather;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "获取缓存异常: " + e.getMessage());
            incrementErrorCount(key);
            return restoreFromBackup(key, Weather.class);
        }
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
        
        try {
            // 先检查内存缓存
            Object cachedAir = memoryCache.get(key);
            if (cachedAir instanceof Weather) {
                long timestamp = cachePreferences.getLong(timestampKey, 0);
                if (!isCacheExpired(timestamp, CACHE_DURATION_AIR)) {
                    // 成功获取缓存，重置错误计数
                    resetErrorCount(key);
                    return (Weather) cachedAir;
                }
            }
            
            // 内存缓存不存在或已过期，检查磁盘缓存
            String airJson = cachePreferences.getString(key, null);
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            
            if (airJson != null && !isCacheExpired(timestamp, CACHE_DURATION_AIR)) {
                try {
                    Weather air = gson.fromJson(airJson, Weather.class);
                    memoryCache.put(key, air); // 更新内存缓存
                    // 成功获取缓存，重置错误计数
                    resetErrorCount(key);
                    return air;
                } catch (JsonSyntaxException e) {
                    // 解析失败，尝试从备份恢复
                    Log.e(TAG, "缓存数据解析失败: " + e.getMessage());
                    incrementErrorCount(key);
                    Weather air = restoreFromBackup(key, Weather.class);
                    if (air != null) {
                        return air;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "获取缓存异常: " + e.getMessage());
            incrementErrorCount(key);
            return restoreFromBackup(key, Weather.class);
        }
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
        
        try {
            // 先检查内存缓存
            Object cachedIndices = memoryCache.get(key);
            if (cachedIndices instanceof Weather) {
                long timestamp = cachePreferences.getLong(timestampKey, 0);
                if (!isCacheExpired(timestamp, CACHE_DURATION_INDICES)) {
                    // 成功获取缓存，重置错误计数
                    resetErrorCount(key);
                    return (Weather) cachedIndices;
                }
            }
            
            // 内存缓存不存在或已过期，检查磁盘缓存
            String indicesJson = cachePreferences.getString(key, null);
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            
            if (indicesJson != null && !isCacheExpired(timestamp, CACHE_DURATION_INDICES)) {
                try {
                    Weather indices = gson.fromJson(indicesJson, Weather.class);
                    memoryCache.put(key, indices); // 更新内存缓存
                    // 成功获取缓存，重置错误计数
                    resetErrorCount(key);
                    return indices;
                } catch (JsonSyntaxException e) {
                    // 解析失败，尝试从备份恢复
                    Log.e(TAG, "缓存数据解析失败: " + e.getMessage());
                    incrementErrorCount(key);
                    Weather indices = restoreFromBackup(key, Weather.class);
                    if (indices != null) {
                        return indices;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "获取缓存异常: " + e.getMessage());
            incrementErrorCount(key);
            return restoreFromBackup(key, Weather.class);
        }
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
    
    /**
     * 检查是否需要创建备份
     */
    private void checkAndCreateBackup() {
        long lastBackupTime = backupPreferences.getLong(KEY_LAST_BACKUP_TIME, 0);
        long now = System.currentTimeMillis();
        
        if (now - lastBackupTime > BACKUP_INTERVAL) {
            backupExecutor.execute(new BackupTask());
        }
    }
    
    /**
     * 备份任务
     */
    private class BackupTask implements Runnable {
        @Override
        public void run() {
            try {
                Log.i(TAG, "开始备份缓存");
                int backupCount = 0;
                
                // 获取所有缓存数据
                Map<String, ?> allCache = cachePreferences.getAll();
                SharedPreferences.Editor backupEditor = backupPreferences.edit();
                
                // 备份重要数据类型
                for (Map.Entry<String, ?> entry : allCache.entrySet()) {
                    String key = entry.getKey();
                    // 只备份实际数据，不备份时间戳和错误计数
                    if (!key.startsWith(KEY_PREFIX_TIMESTAMP) && !key.startsWith(KEY_PREFIX_ERROR_COUNT)) {
                        // 备份数据
                        if (entry.getValue() instanceof String) {
                            backupEditor.putString(key, (String) entry.getValue());
                            backupCount++;
                        } else if (entry.getValue() instanceof Long) {
                            backupEditor.putLong(key, (Long) entry.getValue());
                        }
                        
                        // 同时备份对应的时间戳
                        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
                        if (allCache.containsKey(timestampKey) && allCache.get(timestampKey) instanceof Long) {
                            backupEditor.putLong(timestampKey, (Long) allCache.get(timestampKey));
                        }
                    }
                }
                
                // 应用备份
                backupEditor.apply();
                
                Log.i(TAG, "备份完成，共备份 " + backupCount + " 条数据");
            } catch (Exception e) {
                Log.e(TAG, "备份失败", e);
            } finally {
                // 更新备份时间
                backupPreferences.edit()
                        .putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis())
                        .apply();
            }
        }
    }
    
    /**
     * 手动创建备份
     */
    public void createBackup() {
        backupExecutor.execute(new BackupTask());
    }
    
    /**
     * 检查缓存是否被损坏并尝试修复
     * @param cityId 城市ID
     * @return 是否有缓存被修复
     */
    public boolean checkAndRepairCache(String cityId) {
        boolean repaired = false;
        String[] prefixes = {
                KEY_PREFIX_CURRENT,
                KEY_PREFIX_FORECAST,
                KEY_PREFIX_AIR,
                KEY_PREFIX_INDICES
        };
        
        for (String prefix : prefixes) {
            String key = prefix + cityId;
            String cacheData = cachePreferences.getString(key, null);
            
            if (cacheData != null) {
                try {
                    // 尝试解析JSON，验证数据完整性
                    if (prefix.equals(KEY_PREFIX_CURRENT) || 
                        prefix.equals(KEY_PREFIX_FORECAST) || 
                        prefix.equals(KEY_PREFIX_AIR) || 
                        prefix.equals(KEY_PREFIX_INDICES)) {
                        gson.fromJson(cacheData, Weather.class);
                    }
                } catch (JsonSyntaxException e) {
                    // 数据损坏，尝试修复
                    Log.w(TAG, "检测到损坏的缓存: " + key);
                    if (repairCorruptedCache(key)) {
                        repaired = true;
                    }
                }
            }
        }
        
        return repaired;
    }
    
    /**
     * 检查并修复损坏的缓存数据
     * @param key 缓存键
     * @return 是否已修复
     */
    private boolean repairCorruptedCache(String key) {
        try {
            // 检查是否存在备份数据
            String backupData = backupPreferences.getString(key, null);
            if (backupData != null) {
                // 从备份恢复
                cachePreferences.edit().putString(key, backupData).apply();
                
                // 同时恢复时间戳
                String timestampKey = KEY_PREFIX_TIMESTAMP + key;
                long backupTimestamp = backupPreferences.getLong(timestampKey, System.currentTimeMillis());
                cachePreferences.edit().putLong(timestampKey, backupTimestamp).apply();
                
                Log.i(TAG, "已修复损坏的缓存数据: " + key);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "修复缓存失败: " + e.getMessage());
        }
        
        // 修复失败，清除损坏的数据
        clearCorruptedCache(key);
        return false;
    }
    
    /**
     * 清除损坏的缓存数据
     */
    private void clearCorruptedCache(String key) {
        try {
            String timestampKey = KEY_PREFIX_TIMESTAMP + key;
            cachePreferences.edit()
                    .remove(key)
                    .remove(timestampKey)
                    .apply();
            
            // 从内存缓存移除
            memoryCache.remove(key);
            
            Log.i(TAG, "已清除损坏的缓存: " + key);
        } catch (Exception e) {
            Log.e(TAG, "清除损坏缓存失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录错误次数
     */
    private void incrementErrorCount(String key) {
        String errorKey = KEY_PREFIX_ERROR_COUNT + key;
        int errorCount = cachePreferences.getInt(errorKey, 0) + 1;
        cachePreferences.edit().putInt(errorKey, errorCount).apply();
        
        // 如果错误次数超过阈值，尝试修复或清除
        if (errorCount >= MAX_ERROR_COUNT) {
            if (!repairCorruptedCache(key)) {
                clearCorruptedCache(key);
            }
            // 重置错误计数
            resetErrorCount(key);
        }
    }
    
    /**
     * 重置错误计数
     */
    private void resetErrorCount(String key) {
        String errorKey = KEY_PREFIX_ERROR_COUNT + key;
        cachePreferences.edit().putInt(errorKey, 0).apply();
    }
    
    /**
     * 从备份恢复数据
     * @param key 缓存键
     * @param classOfT 数据类型
     * @return 恢复的数据，如果恢复失败则返回null
     */
    private <T> T restoreFromBackup(String key, Class<T> classOfT) {
        try {
            String backupData = backupPreferences.getString(key, null);
            if (backupData != null) {
                // 从备份恢复到缓存
                repairCorruptedCache(key);
                
                // 返回恢复的数据
                return gson.fromJson(backupData, classOfT);
            }
        } catch (Exception e) {
            Log.e(TAG, "从备份恢复失败: " + e.getMessage());
        }
        return null;
    }
} 
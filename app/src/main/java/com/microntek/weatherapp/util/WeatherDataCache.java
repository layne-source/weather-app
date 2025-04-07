package com.microntek.weatherapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;

import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private static final String KEY_APP_USAGE_COUNT = "app_usage_count"; // 应用使用次数
    private static final String KEY_IMPORTANT_DATA_MODIFIED = "important_data_modified"; // 重要数据修改标记

    // 错误恢复常量
    private static final int MAX_ERROR_COUNT = 3; // 最大错误次数，超过此次数将重置缓存
    private static final long MIN_BACKUP_INTERVAL = 30 * 60 * 1000; // 最小备份间隔（30分钟）
    private static final long MAX_BACKUP_INTERVAL = 24 * 60 * 60 * 1000; // 最大备份间隔（24小时）
    private static final long BACKUP_INTERVAL = 12 * 60 * 60 * 1000; // 默认备份间隔（12小时）
    private static final String KEY_LAST_BACKUP_TIME = "last_backup_time"; // 上次备份时间
    
    // 缓存有效期设置（毫秒）
    private static final long CACHE_DURATION_CURRENT = 30 * 60 * 1000;     // 30分钟
    private static final long CACHE_DURATION_FORECAST = 3 * 60 * 60 * 1000; // 3小时
    private static final long CACHE_DURATION_AIR = 60 * 60 * 1000;          // 1小时
    private static final long CACHE_DURATION_INDICES = 6 * 60 * 60 * 1000;  // 6小时
    private static final long CACHE_DURATION_CITY_SEARCH = 7 * 24 * 60 * 60 * 1000; // 7天
    private static final long CACHE_DURATION_GEO = 30 * 24 * 60 * 60 * 1000;       // 30天
    private static final long DEFAULT_CACHE_DURATION = 60 * 60 * 1000;      // 默认缓存时间1小时
    
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
    private static final Object LOCK = new Object(); // 用于同步的锁对象
    
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
     * 关闭缓存管理器，释放资源
     * 在应用退出时调用
     */
    public static synchronized void shutdown() {
        if (instance != null && instance.backupExecutor != null) {
            try {
                // 执行最后一次备份
                instance.createBackup();
                
                // 等待备份任务完成并关闭线程池
                instance.backupExecutor.shutdown();
                if (!instance.backupExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    instance.backupExecutor.shutdownNow();
                }
                Log.i(TAG, "缓存管理器已关闭");
            } catch (Exception e) {
                Log.e(TAG, "关闭缓存管理器失败: " + e.getMessage());
                if (instance.backupExecutor != null && !instance.backupExecutor.isShutdown()) {
                    instance.backupExecutor.shutdownNow();
                }
            }
            instance = null;
        }
    }
    
    /**
     * 缓存当前天气数据
     */
    public synchronized void cacheCurrentWeather(String cityId, Weather weather) {
        if (cityId == null || weather == null) {
            Log.w(TAG, "尝试缓存无效的当前天气数据");
            return;
        }
        
        String key = KEY_PREFIX_CURRENT + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        try {
            weather.setUpdateTimestamp(System.currentTimeMillis());
            long timestamp = System.currentTimeMillis();
            String weatherJson = gson.toJson(weather);
            
            // 保存到磁盘缓存
            cachePreferences.edit()
                    .putString(key, weatherJson)
                    .putLong(timestampKey, timestamp)
                    .apply();
            
            // 保存到内存缓存
            memoryCache.put(key, weather);
            
            // 标记重要数据已修改
            cachePreferences.edit()
                .putBoolean(KEY_IMPORTANT_DATA_MODIFIED, true)
                .apply();
            
            // 增加应用使用计数
            incrementAppUsageCount();
            
            // 重置错误计数
            resetErrorCount(key);
            
            // 检查是否需要创建备份
            checkAndCreateBackup();
            
            Log.d(TAG, "已缓存城市ID: " + cityId + " 的当前天气数据");
        } catch (Exception e) {
            Log.e(TAG, "缓存天气数据失败: " + e.getMessage());
            incrementErrorCount(key);
        }
    }
    
    /**
     * 获取缓存的当前天气
     */
    public synchronized Weather getCachedCurrentWeather(String cityId) {
        if (cityId == null) return null;
        
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
    public synchronized void cacheForecastWeather(String cityId, Weather weather) {
        if (cityId == null || weather == null) {
            Log.w(TAG, "尝试缓存无效的天气预报数据");
            return;
        }
        
        String key = KEY_PREFIX_FORECAST + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        try {
            weather.setUpdateTimestamp(System.currentTimeMillis());
            long timestamp = System.currentTimeMillis();
            String weatherJson = gson.toJson(weather);
            
            // 保存到磁盘缓存
            cachePreferences.edit()
                    .putString(key, weatherJson)
                    .putLong(timestampKey, timestamp)
                    .apply();
            
            // 保存到内存缓存
            memoryCache.put(key, weather);
            
            // 标记重要数据已修改
            cachePreferences.edit()
                .putBoolean(KEY_IMPORTANT_DATA_MODIFIED, true)
                .apply();
            
            // 增加应用使用计数
            incrementAppUsageCount();
            
            // 重置错误计数
            resetErrorCount(key);
            
            // 检查是否需要创建备份
            checkAndCreateBackup();
            
            Log.d(TAG, "已缓存城市ID: " + cityId + " 的天气预报数据");
        } catch (Exception e) {
            Log.e(TAG, "缓存预报数据失败: " + e.getMessage());
            incrementErrorCount(key);
        }
    }
    
    /**
     * 获取缓存的天气预报
     */
    public synchronized Weather getCachedForecastWeather(String cityId) {
        if (cityId == null) return null;
        
        String key = KEY_PREFIX_FORECAST + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        try {
            // 先检查内存缓存
            Object cachedWeather = memoryCache.get(key);
            if (cachedWeather instanceof Weather) {
                long timestamp = cachePreferences.getLong(timestampKey, 0);
                if (!isCacheExpired(timestamp, CACHE_DURATION_FORECAST)) {
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
                    resetErrorCount(key);
                    return weather;
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "预报缓存数据解析失败: " + e.getMessage());
                    incrementErrorCount(key);
                    Weather weather = restoreFromBackup(key, Weather.class);
                    if (weather != null) {
                        return weather;
                    }
                }
            }
            
            return null; // 缓存不存在或已过期
        } catch (Exception e) {
            Log.e(TAG, "获取预报缓存异常: " + e.getMessage());
            incrementErrorCount(key);
            return restoreFromBackup(key, Weather.class);
        }
    }
    
    /**
     * 缓存空气质量数据
     */
    public synchronized void cacheAirQuality(String cityId, Weather weather) {
        if (cityId == null || weather == null) {
            Log.w(TAG, "尝试缓存无效的空气质量数据");
            return;
        }
        
        String key = KEY_PREFIX_AIR + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        try {
            weather.setUpdateTimestamp(System.currentTimeMillis());
            long timestamp = System.currentTimeMillis();
            String weatherJson = gson.toJson(weather);
            
            // 更新内存缓存
            memoryCache.put(key, weather);
            
            // 更新磁盘缓存
            cachePreferences.edit()
                    .putString(key, weatherJson)
                    .putLong(timestampKey, timestamp)
                    .apply();
                    
            // 标记重要数据已修改
            cachePreferences.edit()
                .putBoolean(KEY_IMPORTANT_DATA_MODIFIED, true)
                .apply();
            
            // 增加应用使用计数
            incrementAppUsageCount();
            
            // 重置错误计数
            resetErrorCount(key);
            
            Log.d(TAG, "已缓存城市ID: " + cityId + " 的空气质量数据");
        } catch (Exception e) {
            Log.e(TAG, "缓存空气质量数据失败: " + e.getMessage());
            incrementErrorCount(key);
        }
    }
    
    /**
     * 获取缓存的空气质量数据
     */
    public synchronized Weather getCachedAirQuality(String cityId) {
        if (cityId == null) return null;
        
        String key = KEY_PREFIX_AIR + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        try {
            // 先检查内存缓存
            Object cachedWeather = memoryCache.get(key);
            if (cachedWeather instanceof Weather) {
                long timestamp = cachePreferences.getLong(timestampKey, 0);
                if (!isCacheExpired(timestamp, CACHE_DURATION_AIR)) {
                    resetErrorCount(key);
                    return (Weather) cachedWeather;
                }
            }
            
            // 内存缓存不存在或已过期，检查磁盘缓存
            String weatherJson = cachePreferences.getString(key, null);
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            
            if (weatherJson != null && !isCacheExpired(timestamp, CACHE_DURATION_AIR)) {
                try {
                    Weather weather = gson.fromJson(weatherJson, Weather.class);
                    memoryCache.put(key, weather); // 更新内存缓存
                    resetErrorCount(key);
                    return weather;
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "空气质量缓存数据解析失败: " + e.getMessage());
                    incrementErrorCount(key);
                    Weather weather = restoreFromBackup(key, Weather.class);
                    if (weather != null) {
                        return weather;
                    }
                }
            }
            
            return null; // 缓存不存在或已过期
        } catch (Exception e) {
            Log.e(TAG, "获取空气质量缓存异常: " + e.getMessage());
            incrementErrorCount(key);
            return restoreFromBackup(key, Weather.class);
        }
    }
    
    /**
     * 缓存生活指数数据
     */
    public synchronized void cacheLifeIndices(String cityId, Weather weather) {
        if (cityId == null || weather == null) {
            Log.w(TAG, "尝试缓存无效的生活指数数据");
            return;
        }
        
        String key = KEY_PREFIX_INDICES + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        try {
            weather.setUpdateTimestamp(System.currentTimeMillis());
            long timestamp = System.currentTimeMillis();
            String weatherJson = gson.toJson(weather);
            
            // 更新内存缓存
            memoryCache.put(key, weather);
            
            // 更新磁盘缓存
            cachePreferences.edit()
                    .putString(key, weatherJson)
                    .putLong(timestampKey, timestamp)
                    .apply();
                    
            // 标记重要数据已修改
            cachePreferences.edit()
                .putBoolean(KEY_IMPORTANT_DATA_MODIFIED, true)
                .apply();
            
            // 增加应用使用计数
            incrementAppUsageCount();
            
            // 重置错误计数
            resetErrorCount(key);
            
            Log.d(TAG, "已缓存城市ID: " + cityId + " 的生活指数数据");
        } catch (Exception e) {
            Log.e(TAG, "缓存生活指数数据失败: " + e.getMessage());
            incrementErrorCount(key);
        }
    }
    
    /**
     * 获取缓存的生活指数数据
     */
    public synchronized Weather getCachedLifeIndices(String cityId) {
        if (cityId == null) return null;
        
        String key = KEY_PREFIX_INDICES + cityId;
        String timestampKey = KEY_PREFIX_TIMESTAMP + key;
        
        try {
            // 先检查内存缓存
            Object cachedWeather = memoryCache.get(key);
            if (cachedWeather instanceof Weather) {
                long timestamp = cachePreferences.getLong(timestampKey, 0);
                if (!isCacheExpired(timestamp, CACHE_DURATION_INDICES)) {
                    resetErrorCount(key);
                    return (Weather) cachedWeather;
                }
            }
            
            // 内存缓存不存在或已过期，检查磁盘缓存
            String weatherJson = cachePreferences.getString(key, null);
            long timestamp = cachePreferences.getLong(timestampKey, 0);
            
            if (weatherJson != null && !isCacheExpired(timestamp, CACHE_DURATION_INDICES)) {
                try {
                    Weather weather = gson.fromJson(weatherJson, Weather.class);
                    memoryCache.put(key, weather); // 更新内存缓存
                    resetErrorCount(key);
                    return weather;
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "生活指数缓存数据解析失败: " + e.getMessage());
                    incrementErrorCount(key);
                    Weather weather = restoreFromBackup(key, Weather.class);
                    if (weather != null) {
                        return weather;
                    }
                }
            }
            
            return null; // 缓存不存在或已过期
        } catch (Exception e) {
            Log.e(TAG, "获取生活指数缓存异常: " + e.getMessage());
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
     * 清除指定城市的所有缓存
     */
    public synchronized void clearCache(String cityId) {
        String[] types = {
            KEY_PREFIX_CURRENT, 
            KEY_PREFIX_FORECAST, 
            KEY_PREFIX_AIR, 
            KEY_PREFIX_INDICES
        };
        
        // 启动编辑事务
        SharedPreferences.Editor editor = cachePreferences.edit();
        
        for (String prefix : types) {
            String key = prefix + cityId;
            String timestampKey = KEY_PREFIX_TIMESTAMP + key;
            
            // 删除内存缓存
            memoryCache.remove(key);
            
            // 删除磁盘缓存
            editor.remove(key).remove(timestampKey);
        }
        
        // 使用commit立即提交更改
        editor.commit();
        
        // 标记重要数据已修改
        cachePreferences.edit()
            .putBoolean(KEY_IMPORTANT_DATA_MODIFIED, true)
            .apply();
        
        Log.i(TAG, "已清除城市ID: " + cityId + " 的所有缓存");
    }
    
    /**
     * 清除所有缓存
     */
    public synchronized void clearAllCache() {
        // 清除内存缓存
        memoryCache.evictAll();
        
        // 清除磁盘缓存并立即提交
        cachePreferences.edit().clear().commit();
        
        // 标记重要数据已修改
        cachePreferences.edit()
            .putBoolean(KEY_IMPORTANT_DATA_MODIFIED, true)
            .apply();
        
        Log.i(TAG, "已清除所有缓存");
    }
    
    /**
     * 清理过期缓存
     */
    public synchronized void cleanExpiredCache() {
        Log.d(TAG, "开始清理过期缓存");
        
        try {
            Map<String, ?> allCache = cachePreferences.getAll();
            SharedPreferences.Editor editor = cachePreferences.edit();
            int cleanedCount = 0;
            
            for (Map.Entry<String, ?> entry : allCache.entrySet()) {
                String key = entry.getKey();
                
                // 只处理时间戳键
                if (key.startsWith(KEY_PREFIX_TIMESTAMP)) {
                    long timestamp = 0;
                    if (entry.getValue() instanceof Long) {
                        timestamp = (Long) entry.getValue();
                    }
                    
                    String originalKey = key.substring(KEY_PREFIX_TIMESTAMP.length());
                    long cacheDuration = getCacheDurationForKey(originalKey);
                    
                    // 如果缓存已过期，删除相关的所有缓存条目
                    if (isCacheExpired(timestamp, cacheDuration)) {
                        editor.remove(originalKey);
                        editor.remove(key);
                        cleanedCount++;
                        
                        // 同时从内存缓存中移除
                        memoryCache.remove(originalKey);
                    }
                }
            }
            
            // 提交更改
            editor.apply();
            
            Log.i(TAG, "清理了 " + cleanedCount + " 项过期缓存");
            
            // 如果有清理操作，标记重要数据已修改
            if (cleanedCount > 0) {
                cachePreferences.edit()
                    .putBoolean(KEY_IMPORTANT_DATA_MODIFIED, true)
                    .apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "清理过期缓存失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查缓存是否过期
     */
    private boolean isCacheExpired(long timestamp, long duration) {
        return System.currentTimeMillis() - timestamp > duration;
    }
    
    /**
     * 检查是否需要创建备份
     */
    private void checkAndCreateBackup() {
        long lastBackupTime = backupPreferences.getLong(KEY_LAST_BACKUP_TIME, 0);
        long now = System.currentTimeMillis();
        
        // 获取应用使用频率
        int appUsageCount = cachePreferences.getInt(KEY_APP_USAGE_COUNT, 0);
        boolean importantDataModified = cachePreferences.getBoolean(KEY_IMPORTANT_DATA_MODIFIED, false);
        
        // 计算动态备份间隔 - 使用频率越高，备份间隔越短
        long dynamicInterval = calculateBackupInterval(appUsageCount);
        
        // 如果有重要数据修改或超过备份间隔，则创建备份
        if (importantDataModified || (now - lastBackupTime > dynamicInterval)) {
            // 检查设备存储空间
            if (hasEnoughStorage()) {
                backupExecutor.execute(new BackupTask());
                
                // 重置重要数据修改标记
                cachePreferences.edit()
                    .putBoolean(KEY_IMPORTANT_DATA_MODIFIED, false)
                    .apply();
            } else {
                Log.w(TAG, "存储空间不足，跳过备份");
            }
        }
    }
    
    /**
     * 根据使用频率计算备份间隔
     * 使用频率越高，备份间隔越短
     */
    private long calculateBackupInterval(int usageCount) {
        if (usageCount <= 5) {
            return MAX_BACKUP_INTERVAL; // 低频用户使用最长间隔
        } else if (usageCount >= 30) {
            return MIN_BACKUP_INTERVAL; // 高频用户使用最短间隔
        } else {
            // 线性插值计算动态间隔
            return MAX_BACKUP_INTERVAL - 
                   ((usageCount - 5) * (MAX_BACKUP_INTERVAL - MIN_BACKUP_INTERVAL) / 25);
        }
    }
    
    /**
     * 检查设备是否有足够存储空间进行备份
     */
    private boolean hasEnoughStorage() {
        try {
            File cacheDir = context.getCacheDir();
            StatFs stats = new StatFs(cacheDir.getAbsolutePath());
            long availableBytes = stats.getAvailableBytes();
            
            // 确保至少有10MB可用空间
            return availableBytes >= 10 * 1024 * 1024;
        } catch (Exception e) {
            Log.e(TAG, "检查存储空间失败: " + e.getMessage());
            return true; // 默认允许备份
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
    public synchronized boolean checkAndRepairCache(String cityId) {
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
        
        // 如果进行了修复，立即创建新的备份
        if (repaired) {
            createBackup();
        }
        
        return repaired;
    }
    
    /**
     * 清除损坏的缓存
     */
    private boolean clearCorruptedCache(String key) {
        try {
            if (key == null || key.isEmpty()) {
                return false;
            }
            
            Log.w(TAG, "正在清除损坏的缓存: " + key);
            
            // 从内存缓存中移除
            memoryCache.remove(key);
            
            // 从磁盘缓存中移除
            String timestampKey = KEY_PREFIX_TIMESTAMP + key;
            cachePreferences.edit()
                .remove(key)
                .remove(timestampKey)
                .commit(); // 使用commit确保立即生效
                
            // 删除相关错误计数
            resetErrorCount(key);
            
            // 标记重要数据已修改
            cachePreferences.edit()
                .putBoolean(KEY_IMPORTANT_DATA_MODIFIED, true)
                .apply();
                
            Log.i(TAG, "已成功清除损坏的缓存: " + key);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "清除损坏缓存失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 尝试修复损坏的缓存
     */
    private boolean repairCorruptedCache(String key) {
        try {
            if (key == null || key.isEmpty()) {
                return false;
            }
            
            Log.i(TAG, "尝试修复损坏的缓存: " + key);
            
            // 先尝试从备份恢复
            Object restoredData = null;
            
            if (key.startsWith(KEY_PREFIX_CURRENT) || 
                key.startsWith(KEY_PREFIX_FORECAST) || 
                key.startsWith(KEY_PREFIX_AIR) || 
                key.startsWith(KEY_PREFIX_INDICES)) {
                restoredData = restoreFromBackup(key, Weather.class);
            }
            
            if (restoredData != null) {
                // 如果成功恢复，更新缓存
                memoryCache.put(key, restoredData);
                
                // 更新磁盘缓存
                String timestampKey = KEY_PREFIX_TIMESTAMP + key;
                long timestamp = System.currentTimeMillis();
                
                if (restoredData instanceof Weather) {
                    cachePreferences.edit()
                        .putString(key, gson.toJson(restoredData))
                        .putLong(timestampKey, timestamp)
                        .commit(); // 使用commit确保立即生效
                    
                    // 清除错误计数
                    resetErrorCount(key);
                    
                    Log.i(TAG, "已成功修复损坏的缓存: " + key);
                    return true;
                }
            }
            
            Log.w(TAG, "无法修复损坏的缓存: " + key);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "修复损坏缓存失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 增加应用使用计数
     */
    private void incrementAppUsageCount() {
        int count = cachePreferences.getInt(KEY_APP_USAGE_COUNT, 0);
        cachePreferences.edit()
                .putInt(KEY_APP_USAGE_COUNT, count + 1)
                .apply();
    }

    /**
     * 增加指定键的错误计数
     */
    private void incrementErrorCount(String key) {
        try {
            String errorKey = KEY_PREFIX_ERROR_COUNT + key;
            int currentCount = cachePreferences.getInt(errorKey, 0);
            // 使用commit()立即写入错误计数，确保不会因为应用崩溃而丢失
            cachePreferences.edit()
                .putInt(errorKey, currentCount + 1)
                .commit();
            
            if (currentCount + 1 >= MAX_ERROR_COUNT) {
                Log.w(TAG, "键 " + key + " 已达到错误阈值: " + MAX_ERROR_COUNT);
                
                // 如果错误次数超过阈值，尝试修复或清除
                if (!repairCorruptedCache(key)) {
                    clearCorruptedCache(key);
                }
                // 重置错误计数
                resetErrorCount(key);
            }
        } catch (Exception e) {
            Log.e(TAG, "增加错误计数异常: " + e.getMessage());
        }
    }
    
    /**
     * 重置指定键的错误计数
     */
    private void resetErrorCount(String key) {
        try {
            String errorKey = KEY_PREFIX_ERROR_COUNT + key;
            // 检查是否需要重置错误计数
            int currentCount = cachePreferences.getInt(errorKey, 0);
            if (currentCount > 0) {
                // 使用commit()立即写入，确保不会丢失
                cachePreferences.edit()
                    .putInt(errorKey, 0)
                    .commit();
                Log.d(TAG, "已重置键 " + key + " 的错误计数");
            }
        } catch (Exception e) {
            Log.e(TAG, "重置错误计数异常: " + e.getMessage());
        }
    }
    
    /**
     * 验证并修复缓存文件的完整性
     * @return 是否有问题被修复
     */
    public synchronized boolean verifyAndRepairCache() {
        Log.i(TAG, "开始验证和修复缓存...");
        boolean anyRepaired = false;
        
        try {
            Map<String, ?> allCache = cachePreferences.getAll();
            SharedPreferences.Editor editor = cachePreferences.edit();
            int totalEntries = 0;
            int corruptedEntries = 0;
            int repairedEntries = 0;
            
            // 检查所有weather类型的条目
            for (Map.Entry<String, ?> entry : allCache.entrySet()) {
                String key = entry.getKey();
                
                // 只验证天气数据，跳过时间戳和错误计数
                if (key.startsWith(KEY_PREFIX_CURRENT) || 
                    key.startsWith(KEY_PREFIX_FORECAST) || 
                    key.startsWith(KEY_PREFIX_AIR) || 
                    key.startsWith(KEY_PREFIX_INDICES)) {
                    
                    totalEntries++;
                    
                    if (entry.getValue() instanceof String) {
                        String value = (String) entry.getValue();
                        try {
                            // 验证JSON格式
                            gson.fromJson(value, Weather.class);
                            
                            // 附加验证：检查数据内容的有效性
                            Weather weather = gson.fromJson(value, Weather.class);
                            if (weather != null) {
                                // 检查天气数据的基本有效性
                                boolean isValid = true;
                                
                                // 检查温度是否在合理范围内（-100到+100摄氏度）
                                if (key.startsWith(KEY_PREFIX_CURRENT)) {
                                    int temp = weather.getCurrentTemp();
                                    if (temp < -100 || temp > 100) {
                                        isValid = false;
                                    }
                                }
                                
                                if (!isValid) {
                                    corruptedEntries++;
                                    if (repairCorruptedCache(key)) {
                                        repairedEntries++;
                                        anyRepaired = true;
                                    }
                                }
                            }
                        } catch (JsonSyntaxException e) {
                            // 发现格式错误的JSON
                            corruptedEntries++;
                            if (repairCorruptedCache(key)) {
                                repairedEntries++;
                                anyRepaired = true;
                            }
                        }
                    }
                }
            }
            
            Log.i(TAG, String.format("缓存验证完成: 共%d项, %d项损坏, %d项已修复", 
                    totalEntries, corruptedEntries, repairedEntries));
            
            // 如果有任何修复，创建一个新的备份
            if (anyRepaired) {
                createBackup();
            }
        } catch (Exception e) {
            Log.e(TAG, "验证缓存时发生错误: " + e.getMessage());
        }
        
        return anyRepaired;
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

    /**
     * 根据键获取对应的缓存持续时间
     */
    private long getCacheDurationForKey(String key) {
        if (key.startsWith(KEY_PREFIX_CURRENT)) {
            return CACHE_DURATION_CURRENT;
        } else if (key.startsWith(KEY_PREFIX_FORECAST)) {
            return CACHE_DURATION_FORECAST;
        } else if (key.startsWith(KEY_PREFIX_AIR)) {
            return CACHE_DURATION_AIR;
        } else if (key.startsWith(KEY_PREFIX_INDICES)) {
            return CACHE_DURATION_INDICES;
        } else if (key.startsWith(KEY_PREFIX_CITY_SEARCH)) {
            return CACHE_DURATION_CITY_SEARCH;
        } else if (key.startsWith(KEY_PREFIX_GEO)) {
            return CACHE_DURATION_GEO;
        } else {
            return DEFAULT_CACHE_DURATION;
        }
    }
} 
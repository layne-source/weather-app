package com.microntek.weatherapp.util;

import android.content.Context;
import android.util.Log;

import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 天气数据帮助类
 * 统一处理天气数据获取和城市信息更新
 */
public class WeatherDataHelper {
    private static final String TAG = "WeatherDataHelper";

    /**
     * 获取城市的天气数据
     * @param context 上下文
     * @param city 城市对象
     * @param useCache 是否优先使用缓存
     * @return 天气数据
     */
    public static Weather getCityWeather(Context context, City city, boolean useCache) {
        try {
            if (city == null) {
                Log.e(TAG, "城市对象为空");
                return null;
            }

            // 先尝试使用缓存，如果设置了优先使用缓存
            if (useCache) {
                try {
                    Weather cachedWeather = fetchWeatherFromCache(context, city);
                    if (cachedWeather != null) {
                        return cachedWeather;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "从缓存获取天气数据失败: " + e.getMessage());
                    // 失败后继续尝试网络请求
                }
            }

            // 从网络获取天气数据
            return fetchWeatherFromNetwork(city);
        } catch (Exception e) {
            Log.e(TAG, "获取天气数据失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从缓存获取天气数据
     */
    private static Weather fetchWeatherFromCache(Context context, City city) throws Exception {
        if (city.isCurrentLocation() || city.getId().contains(",")) {
            return WeatherApi.getCurrentWeatherByLocationWithCache(
                    context, city.getLatitude(), city.getLongitude());
        } else {
            return WeatherApi.getCurrentWeatherWithCache(context, city.getId());
        }
    }

    /**
     * 从网络获取天气数据
     */
    private static Weather fetchWeatherFromNetwork(City city) throws Exception {
        if (city.isCurrentLocation() || city.getId().contains(",")) {
            return WeatherApi.getCurrentWeatherByLocation(
                    city.getLatitude(), city.getLongitude());
        } else {
            return WeatherApi.getCurrentWeather(city.getId());
        }
    }

    /**
     * 使用天气数据更新城市对象
     * @param city 要更新的城市对象
     * @param weather 天气数据
     */
    public static void updateCityWithWeatherData(City city, Weather weather) {
        if (city != null && weather != null) {
            city.setTemperature(weather.getCurrentTemp());
            city.setWeatherDesc(weather.getWeatherDesc());
            city.setWeatherIcon(weather.getWeatherIcon());
            city.setAirQuality(weather.getAirQuality());
            city.setAqi(weather.getAqi());
        }
    }

    /**
     * 对城市列表进行排序，将当前城市放在首位
     * @param cities 要排序的城市列表
     * @param currentCity 当前城市
     * @return 排序后的城市列表
     */
    public static List<City> sortCitiesList(List<City> cities, City currentCity) {
        if (cities == null || cities.isEmpty()) {
            return new ArrayList<>();
        }

        List<City> sortedCities = new ArrayList<>();

        // 先添加当前城市
        if (currentCity != null) {
            for (City city : cities) {
                if (city.getId().equals(currentCity.getId())) {
                    city.setCurrentLocation(true);
                    sortedCities.add(city);
                    break;
                }
            }
        }

        // 再添加其他城市
        for (City city : cities) {
            if (currentCity == null || !city.getId().equals(currentCity.getId())) {
                city.setCurrentLocation(false);
                sortedCities.add(city);
            }
        }

        return sortedCities;
    }

    /**
     * 加载多个城市的天气数据
     * @param context 上下文
     * @param cities 要加载天气数据的城市列表
     * @param useCache 是否优先使用缓存
     * @param callback 完成回调
     */
    public static void loadCitiesWeather(
            Context context, 
            List<City> cities, 
            boolean useCache, 
            WeatherDataCallback callback) {
        
        if (cities == null || cities.isEmpty()) {
            if (callback != null) {
                callback.onDataLoaded(new ArrayList<>());
            }
            return;
        }

        final String taskId = "LOAD_CITIES_WEATHER";
        if (!TaskManager.canExecuteTask(taskId)) {
            if (callback != null) {
                callback.onError("任务正在执行中");
            }
            return;
        }

        ExecutorManager.executeParallel(() -> {
            try {
                List<City> updatedCities = new ArrayList<>();
                
                for (City city : new ArrayList<>(cities)) {
                    try {
                        Weather weather = getCityWeather(context, city, useCache);
                        if (weather != null) {
                            updateCityWithWeatherData(city, weather);
                        }
                        updatedCities.add(city);
                    } catch (Exception e) {
                        Log.e(TAG, "加载城市 " + city.getName() + " 的天气失败: " + e.getMessage());
                        // 即使失败也保留城市
                        updatedCities.add(city);
                    }
                }

                // 完成后在主线程回调
                final List<City> finalCities = updatedCities;
                ExecutorManager.executeOnMain(() -> {
                    if (callback != null) {
                        callback.onDataLoaded(finalCities);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "加载城市天气失败", e);
                ExecutorManager.executeOnMain(() -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
            } finally {
                TaskManager.completeTask(taskId);
            }
        });
    }

    /**
     * 天气数据回调接口
     */
    public interface WeatherDataCallback {
        void onDataLoaded(List<City> cities);
        void onError(String errorMessage);
    }
} 
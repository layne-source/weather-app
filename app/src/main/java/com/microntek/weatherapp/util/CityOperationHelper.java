package com.microntek.weatherapp.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.model.Weather;
import com.microntek.weatherapp.service.WeatherDataService;

import java.util.ArrayList;
import java.util.List;

/**
 * 城市操作辅助类
 * 封装城市添加、删除、切换等复杂操作
 */
public class CityOperationHelper {
    private static final String TAG = "CityOperationHelper";
    
    // 防止重复操作的任务ID前缀
    private static final String TASK_ADD_PREFIX = "ADD_CITY_";
    private static final String TASK_DELETE_PREFIX = "DELETE_CITY_";
    private static final String TASK_SWITCH_PREFIX = "SWITCH_CITY_";
    
    private final Context context;
    private final CityPreferences cityPreferences;
    private final View rootView;
    
    public CityOperationHelper(Context context, View rootView) {
        this.context = context;
        this.cityPreferences = new CityPreferences(context);
        this.rootView = rootView;
    }
    
    /**
     * 处理位置定位结果
     */
    public void processLocationResult(double latitude, double longitude, 
                                     OperationCallback<City> callback) {
        // 在后台线程获取城市信息
        ExecutorManager.getThreadPoolExecutor().execute(() -> {
            try {
                // 获取当前城市信息
                City city = WeatherApi.getCityByLocation(latitude, longitude);
                
                // 在添加城市前获取空气质量信息
                try {
                    String locationId = city.getLongitude() + "," + city.getLatitude();
                    // 获取城市天气信息
                    Weather weather = WeatherApi.getCurrentWeatherByLocation(latitude, longitude);
                    // 获取空气质量信息
                    weather = WeatherApi.getAirQuality(locationId, weather);
                    
                    // 将空气质量信息和天气信息设置到城市对象
                    city.setAirQuality(weather.getAirQuality());
                    city.setAqi(weather.getAqi());
                    city.setTemperature(weather.getCurrentTemp());
                    city.setWeatherDesc(weather.getWeatherDesc());
                    city.setWeatherIcon(weather.getWeatherIcon());
                } catch (Exception e) {
                    Log.e(TAG, "获取空气质量数据失败: " + e.getMessage());
                    // 即使获取空气质量失败，仍然继续添加城市
                }
                
                // 返回结果到主线程
                final City resultCity = city;
                ExecutorManager.executeOnMain(() -> {
                    if (callback != null) {
                        callback.onSuccess(resultCity);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "获取城市信息失败: " + e.getMessage());
                ExecutorManager.executeOnMain(() -> {
                    if (callback != null) {
                        callback.onError("获取城市信息失败: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 加载城市天气数据
     */
    public void loadCitiesWeather(List<City> cities, OperationCallback<List<City>> callback) {
        if (cities == null || cities.isEmpty()) {
            if (callback != null) {
                callback.onError("城市列表为空");
            }
            return;
        }
        
        // 使用WeatherDataHelper加载城市天气数据
        WeatherDataHelper.loadCitiesWeather(
                context,
                new ArrayList<>(cities),
                true, // 优先使用缓存
                new WeatherDataHelper.WeatherDataCallback() {
                    @Override
                    public void onDataLoaded(List<City> updatedCities) {
                        // 获取当前城市
                        City currentCity = cityPreferences.getCurrentCity();
                        
                        // 通知服务城市可能已变更
                        Intent cityChangedIntent = new Intent(WeatherDataService.ACTION_CITY_CHANGED);
                        context.sendBroadcast(cityChangedIntent);
                        
                        // 对城市列表进行排序
                        List<City> sortedCities = WeatherDataHelper.sortCitiesList(
                                updatedCities, currentCity);
                        
                        // 回调结果
                        if (callback != null) {
                            callback.onSuccess(sortedCities);
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "加载城市天气失败: " + errorMessage);
                        if (callback != null) {
                            callback.onError(errorMessage);
                        }
                    }
                });
    }
    
    /**
     * 添加城市
     */
    public void addCity(City city, OperationCallback<List<City>> callback) {
        // 检查输入
        if (city == null) {
            if (callback != null) {
                callback.onError("城市对象为空");
            }
            return;
        }
        
        // 防止重复添加操作
        final String taskId = TASK_ADD_PREFIX + city.getId();
        if (!TaskManager.canExecuteTask(taskId)) {
            return;
        }
        
        // 检查城市数量限制
        if (cityPreferences.getSavedCities().size() >= 5) {
            MessageManager.showError(context, "最多只能添加5个城市");
            TaskManager.completeTask(taskId);
            return;
        }
        
        ExecutorManager.executeSingle(() -> {
            try {
                // 检查城市是否已存在
                boolean cityExists = false;
                for (City existingCity : cityPreferences.getSavedCities()) {
                    if (existingCity.getId().equals(city.getId())) {
                        cityExists = true;
                        break;
                    }
                }
                
                if (cityExists) {
                    ExecutorManager.executeOnMain(() -> {
                        MessageManager.showMessage(context, "该城市已添加");
                        TaskManager.completeTask(taskId);
                        if (callback != null) {
                            callback.onError("该城市已添加");
                        }
                    });
                    return;
                }
                
                // 检查是否是第一个城市
                boolean isFirstCity = cityPreferences.getSavedCities().isEmpty();
                
                // 添加城市到偏好设置（异步预加载城市数据）
                boolean success = cityPreferences.addCity(city);
                
                if (!success) {
                    ExecutorManager.executeOnMain(() -> {
                        MessageManager.showError(context, "添加城市失败");
                        TaskManager.completeTask(taskId);
                        if (callback != null) {
                            callback.onError("添加城市失败");
                        }
                    });
                    return;
                }
                
                // 如果是第一个城市，自动设置为当前城市
                if (isFirstCity) {
                    cityPreferences.setCurrentCity(city);
                    
                    // 通知服务城市已变更
                    Intent cityChangedIntent = new Intent(WeatherDataService.ACTION_CITY_CHANGED);
                    context.sendBroadcast(cityChangedIntent);
                }
                
                // 获取并排序城市列表
                List<City> allCities = cityPreferences.getSavedCities();
                List<City> sortedCities = WeatherDataHelper.sortCitiesList(
                        allCities, cityPreferences.getCurrentCity());
                
                ExecutorManager.executeOnMain(() -> {
                    // 显示成功提示
                    String message = isFirstCity ? 
                            "已添加城市: " + city.getName() + "，并设为当前城市" :
                            "已添加城市: " + city.getName();
                    
                    if (rootView != null) {
                        MessageManager.showActionMessage(rootView, message, "确定", null);
                    } else {
                        MessageManager.showSuccess(context, message);
                    }
                    
                    // 完成任务并回调
                    TaskManager.completeTask(taskId);
                    if (callback != null) {
                        callback.onSuccess(sortedCities);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "添加城市时出错: " + e.getMessage());
                ExecutorManager.executeOnMain(() -> {
                    if (rootView != null) {
                        MessageManager.showActionMessage(rootView, 
                                "添加城市失败: " + e.getMessage(), 
                                "重试", v -> addCity(city, callback));
                    } else {
                        MessageManager.showError(context, 
                                "添加城市失败: " + e.getMessage());
                    }
                    
                    TaskManager.completeTask(taskId);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 删除城市
     */
    public void deleteCity(City city, OperationCallback<List<City>> callback) {
        // 检查输入
        if (city == null) {
            if (callback != null) {
                callback.onError("城市对象为空");
            }
            return;
        }
        
        // 防止重复删除操作
        final String taskId = TASK_DELETE_PREFIX + city.getId();
        if (!TaskManager.canExecuteTask(taskId)) {
            return;
        }
        
        ExecutorManager.executeSingle(() -> {
            try {
                // 获取当前城市
                City currentCity = cityPreferences.getCurrentCity();
                
                // 如果是当前城市，不能删除
                if (currentCity != null && city.getId().equals(currentCity.getId())) {
                    ExecutorManager.executeOnMain(() -> {
                        MessageManager.showError(context, 
                                "不能删除当前选中的城市");
                        TaskManager.completeTask(taskId);
                        if (callback != null) {
                            callback.onError("不能删除当前选中的城市");
                        }
                    });
                    return;
                }
                
                // 从偏好设置中删除城市（同时会清除相关缓存）
                boolean success = cityPreferences.removeCity(city);
                
                if (!success) {
                    ExecutorManager.executeOnMain(() -> {
                        MessageManager.showError(context, 
                                "删除城市失败");
                        TaskManager.completeTask(taskId);
                        if (callback != null) {
                            callback.onError("删除城市失败");
                        }
                    });
                    return;
                }
                
                // 通知服务城市可能已变更
                Intent cityChangedIntent = new Intent(WeatherDataService.ACTION_CITY_CHANGED);
                context.sendBroadcast(cityChangedIntent);
                
                // 刷新城市列表
                List<City> updatedCities = cityPreferences.getSavedCities();
                
                // 对城市列表进行排序
                List<City> sortedCities = WeatherDataHelper.sortCitiesList(
                        updatedCities, cityPreferences.getCurrentCity());
                
                ExecutorManager.executeOnMain(() -> {
                    if (rootView != null) {
                        MessageManager.showActionMessage(rootView, 
                                "已删除城市: " + city.getName(), 
                                "撤销", null);
                    } else {
                        MessageManager.showSuccess(context, 
                                "已删除城市: " + city.getName());
                    }
                    
                    TaskManager.completeTask(taskId);
                    if (callback != null) {
                        callback.onSuccess(sortedCities);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "删除城市时出错: " + e.getMessage());
                ExecutorManager.executeOnMain(() -> {
                    if (rootView != null) {
                        MessageManager.showActionMessage(rootView, 
                                "删除城市失败: " + e.getMessage(), 
                                "重试", v -> deleteCity(city, callback));
                    } else {
                        MessageManager.showError(context, 
                                "删除城市失败: " + e.getMessage());
                    }
                    
                    TaskManager.completeTask(taskId);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 切换当前城市
     */
    public void switchCurrentCity(City city, boolean shouldReturnToMain, 
                                 OperationCallback<List<City>> callback) {
        // 检查输入
        if (city == null) {
            if (callback != null) {
                callback.onError("城市对象为空");
            }
            return;
        }
        
        // 防止重复切换操作
        final String taskId = TASK_SWITCH_PREFIX + city.getId();
        if (!TaskManager.canExecuteTask(taskId)) {
            return;
        }
        
        ExecutorManager.executeSingle(() -> {
            try {
                // 检查是否已经是当前城市
                City current = cityPreferences.getCurrentCity();
                if (current != null && current.getId().equals(city.getId())) {
                    ExecutorManager.executeOnMain(() -> {
                        MessageManager.showMessage(context, 
                                city.getName() + " 已是当前城市");
                        TaskManager.completeTask(taskId);
                        if (callback != null) {
                            callback.onError("已是当前城市");
                        }
                    });
                    return;
                }
                
                // 设置当前城市（同时会预加载天气数据）
                boolean success = cityPreferences.setCurrentCity(city);
                
                if (!success) {
                    ExecutorManager.executeOnMain(() -> {
                        MessageManager.showError(context, "切换城市失败");
                        TaskManager.completeTask(taskId);
                        if (callback != null) {
                            callback.onError("切换城市失败");
                        }
                    });
                    return;
                }
                
                // 通知服务城市已变更
                Intent cityChangedIntent = new Intent(WeatherDataService.ACTION_CITY_CHANGED);
                context.sendBroadcast(cityChangedIntent);
                
                // 获取并排序城市列表
                List<City> allCities = cityPreferences.getSavedCities();
                List<City> sortedCities = WeatherDataHelper.sortCitiesList(
                        allCities, city);
                
                ExecutorManager.executeOnMain(() -> {
                    if (rootView != null) {
                        MessageManager.showActionMessage(rootView, 
                                "已切换到城市: " + city.getName(), 
                                "确定", null);
                    } else {
                        MessageManager.showSuccess(context, 
                                "已切换到城市: " + city.getName());
                    }
                    
                    // 完成任务
                    TaskManager.completeTask(taskId);
                    
                    // 回调结果
                    if (callback != null) {
                        callback.onSuccess(sortedCities);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "切换城市时出错: " + e.getMessage());
                ExecutorManager.executeOnMain(() -> {
                    if (rootView != null) {
                        MessageManager.showActionMessage(rootView, 
                                "切换城市失败: " + e.getMessage(), 
                                "重试", v -> switchCurrentCity(city, shouldReturnToMain, callback));
                    } else {
                        MessageManager.showError(context, 
                                "切换城市失败: " + e.getMessage());
                    }
                    
                    TaskManager.completeTask(taskId);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 操作回调接口
     */
    public interface OperationCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
} 
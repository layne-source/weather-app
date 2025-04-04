package com.microntek.weatherapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.microntek.weatherapp.model.City;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 城市偏好设置工具类，用于管理用户保存的城市列表
 */
public class CityPreferences {
    private static final String PREFS_NAME = "weather_prefs";
    private static final String KEY_CITIES = "saved_cities";
    private static final String KEY_CURRENT_CITY = "current_city";
    
    private final SharedPreferences preferences;
    private final Gson gson;
    
    public CityPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
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
} 
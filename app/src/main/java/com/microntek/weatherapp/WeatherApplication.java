package com.microntek.weatherapp;

import android.app.Application;
import android.util.Log;

import com.microntek.weatherapp.util.ExecutorManager;
import com.microntek.weatherapp.util.TaskManager;

/**
 * 应用Application类
 * 用于管理全局资源和初始化
 */
public class WeatherApplication extends Application {
    private static final String TAG = "WeatherApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "应用启动");
        
        // 可以在这里进行全局初始化
    }
    
    @Override
    public void onTerminate() {
        // 关闭所有执行器
        ExecutorManager.shutdownAll();
        TaskManager.clearAll();
        
        Log.i(TAG, "应用关闭，已清理资源");
        super.onTerminate();
    }
} 
package com.microntek.weatherapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.microntek.weatherapp.util.ExecutorManager;
import com.microntek.weatherapp.util.TaskManager;
import com.microntek.weatherapp.util.NetworkMonitor;

/**
 * 应用Application类
 * 用于管理全局资源和初始化
 */
public class WeatherApplication extends Application {
    private static final String TAG = "WeatherApplication";
    private static Context appContext;
    
    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        Log.i(TAG, "应用启动");
        
        // 初始化网络监控
        NetworkMonitor.getInstance(this).startMonitoring();
        
        // 可以在这里进行其他全局初始化
    }
    
    /**
     * 获取应用上下文
     * @return 应用上下文
     */
    public static Context getAppContext() {
        return appContext;
    }
    
    @Override
    public void onTerminate() {
        // 停止网络监控
        NetworkMonitor.getInstance(this).stopMonitoring();
        
        // 关闭所有执行器
        ExecutorManager.shutdownAll();
        TaskManager.clearAll();
        
        Log.i(TAG, "应用关闭，已清理资源");
        super.onTerminate();
    }
} 
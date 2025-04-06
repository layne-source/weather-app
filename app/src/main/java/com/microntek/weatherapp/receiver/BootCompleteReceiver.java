package com.microntek.weatherapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.microntek.weatherapp.service.WeatherDataService;

/**
 * 开机启动接收器 - 自动启动天气服务
 */
public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            
            Log.i(TAG, "系统启动完成，启动天气数据服务");
            
            // 启动天气数据服务
            Intent serviceIntent = new Intent(context, WeatherDataService.class);
            context.startService(serviceIntent);
        }
    }
} 
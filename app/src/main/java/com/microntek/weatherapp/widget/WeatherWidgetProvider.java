package com.microntek.weatherapp.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.microntek.weatherapp.MainActivity;
import com.microntek.weatherapp.R;
import com.microntek.weatherapp.api.WeatherApi;

/**
 * 天气小部件提供者
 * 负责接收天气数据广播并更新小部件显示
 */
public class WeatherWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "WeatherWidgetProvider";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 更新所有小部件
        for (int appWidgetId : appWidgetIds) {
            // 创建RemoteViews
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);
            
            // 设置点击事件
            Intent mainIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
            
            // 更新小部件
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        
        // 请求天气数据更新
        Intent updateIntent = new Intent("com.microntek.weatherapp.REQUEST_UPDATE");
        context.sendBroadcast(updateIntent);
        Log.d(TAG, "发送天气数据更新请求");
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        // 接收天气数据广播
        if ("com.microntek.weatherapp.WEATHER_DATA".equals(intent.getAction())) {
            Log.d(TAG, "收到天气数据广播");
            
            // 获取广播中的天气数据
            String cityName = intent.getStringExtra("city_name");
            int currentTemp = intent.getIntExtra("current_temp", 0);
            int highTemp = intent.getIntExtra("high_temp", 0);
            int lowTemp = intent.getIntExtra("low_temp", 0);
            String weatherDesc = intent.getStringExtra("weather_desc");
            String weatherIcon = intent.getStringExtra("weather_icon");
            int aqi = intent.getIntExtra("aqi", 0);
            String airQuality = intent.getStringExtra("air_quality");
            
            // 更新所有小部件
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, WeatherWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            
            for (int appWidgetId : appWidgetIds) {
                // 创建RemoteViews
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);
                
                try {
                    // 更新UI
                    views.setTextViewText(R.id.widget_city, cityName);
                    views.setTextViewText(R.id.widget_current_temp, currentTemp + "°");
                    views.setTextViewText(R.id.widget_temp_range, highTemp + "°/" + lowTemp + "°");
                    views.setTextViewText(R.id.widget_weather_desc, weatherDesc);
                    views.setTextViewText(R.id.widget_air_quality, "空气质量: " + airQuality + "(" + aqi + ")");
                    views.setImageViewResource(R.id.widget_weather_icon, 
                        WeatherApi.getWeatherIconResource(weatherIcon));
                    
                    // 更新小部件
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                    
                    Log.d(TAG, "小部件更新成功: " + appWidgetId);
                } catch (Exception e) {
                    Log.e(TAG, "更新小部件失败: " + e.getMessage());
                }
            }
        }
    }
} 
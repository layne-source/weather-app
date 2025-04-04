package com.microntek.weatherapp.util;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;

import com.microntek.weatherapp.R;

import java.util.Calendar;

/**
 * 天气背景工具类
 * 根据天气代码和当前时间返回对应的背景
 */
public class WeatherBackgroundUtil {

    /**
     * 根据天气代码和当前时间获取背景Drawable
     * @param context 上下文
     * @param weatherCode 天气代码
     * @return 对应的背景Drawable
     */
    public static Drawable getWeatherBackground(Context context, String weatherCode) {
        // 检查是否是夜间
        boolean isNight = isNightTime() || isNightWeatherCode(weatherCode);
        
        // 创建渐变背景
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, // 左上到右下的渐变
                getWeatherColors(context, weatherCode, isNight)
        );
        
        return gradientDrawable;
    }
    
    /**
     * 判断当前是否是夜间
     * @return 是否是夜间时段
     */
    private static boolean isNightTime() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        
        // 晚上7点到早上6点认为是夜间
        return hour >= 19 || hour < 6;
    }
    
    /**
     * 判断天气代码是否是夜间天气
     * @param weatherCode 天气代码
     * @return 是否是夜间天气
     */
    private static boolean isNightWeatherCode(String weatherCode) {
        // 和风天气夜间天气代码通常以1、5或9结尾
        if (weatherCode == null || weatherCode.isEmpty()) {
            return false;
        }
        
        // 判断夜间代码
        return weatherCode.endsWith("1") || weatherCode.endsWith("5") || weatherCode.endsWith("9") ||
               (weatherCode.length() == 3 && (weatherCode.startsWith("15") || weatherCode.startsWith("35")));
    }
    
    /**
     * 根据天气代码获取对应的背景颜色
     * @param context 上下文
     * @param weatherCode 天气代码
     * @param isNight 是否是夜间
     * @return 背景颜色数组
     */
    private static int[] getWeatherColors(Context context, String weatherCode, boolean isNight) {
        if (isNight) {
            return new int[]{
                    context.getResources().getColor(R.color.night_start),
                    context.getResources().getColor(R.color.night_end)
            };
        }
        
        if (weatherCode == null || weatherCode.isEmpty()) {
            // 默认使用晴天背景
            return new int[]{
                    context.getResources().getColor(R.color.sunny_start),
                    context.getResources().getColor(R.color.sunny_end)
            };
        }
        
        // 去除夜间标志，获取基础天气代码
        String baseCode = weatherCode;
        if (weatherCode.endsWith("1") || weatherCode.endsWith("5") || weatherCode.endsWith("9")) {
            baseCode = weatherCode.substring(0, weatherCode.length() - 1) + "0";
        }
        
        int code = 0;
        try {
            code = Integer.parseInt(baseCode);
        } catch (NumberFormatException e) {
            // 解析失败，使用默认背景
            return new int[]{
                    context.getResources().getColor(R.color.sunny_start),
                    context.getResources().getColor(R.color.sunny_end)
            };
        }
        
        // 根据天气代码判断天气类型
        if (code >= 300 && code < 500) {
            // 雨天
            return new int[]{
                    context.getResources().getColor(R.color.rainy_start),
                    context.getResources().getColor(R.color.rainy_end)
            };
        } else if (code >= 400 && code < 500) {
            // 雪天 - 也使用雨天的颜色
            return new int[]{
                    context.getResources().getColor(R.color.rainy_start),
                    context.getResources().getColor(R.color.rainy_end)
            };
        } else if (code >= 500 && code < 600) {
            // 雾霾天
            return new int[]{
                    context.getResources().getColor(R.color.cloudy_start),
                    context.getResources().getColor(R.color.cloudy_end)
            };
        } else if (code == 100 || code == 150) {
            // 晴天
            return new int[]{
                    context.getResources().getColor(R.color.sunny_start),
                    context.getResources().getColor(R.color.sunny_end)
            };
        } else {
            // 多云等其他天气
            return new int[]{
                    context.getResources().getColor(R.color.cloudy_start),
                    context.getResources().getColor(R.color.cloudy_end)
            };
        }
    }
} 
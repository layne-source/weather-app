package com.microntek.weatherapp.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.microntek.weatherapp.model.City;

/**
 * 城市列表项的通用适配器基类
 * 提取CityListAdapter和SearchResultAdapter的共同逻辑
 */
public abstract class BaseCityAdapter<VH extends RecyclerView.ViewHolder> 
        extends RecyclerView.Adapter<VH> {
    
    protected Context context;
    
    public BaseCityAdapter(Context context) {
        this.context = context;
    }
    
    /**
     * 设置空气质量UI显示
     */
    protected void setupAirQualityView(TextView tvAirQuality, City city) {
        if (city.getAirQuality() != null && !city.getAirQuality().isEmpty()) {
            // 格式：空气+质量等级+空格+AQI数值
            String airQualityText = "空气" + city.getAirQuality() + " " + city.getAqi();
            tvAirQuality.setText(airQualityText);
            tvAirQuality.setVisibility(View.VISIBLE);
            
            // 根据AQI值设置背景颜色
            if (city.getAqi() > 0) {
                // 设置圆角背景
                tvAirQuality.setBackgroundResource(city.getAqiColorRes());
                // 设置背景颜色
                tvAirQuality.getBackground().setColorFilter(
                    context.getResources().getColor(city.getAqiColorValue()), 
                    android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } else {
            tvAirQuality.setVisibility(View.GONE);
        }
    }
    
    /**
     * 设置天气信息UI显示
     */
    protected void setupWeatherView(TextView tvWeather, TextView tvTemp, City city) {
        // 显示天气信息
        if (city.getWeatherDesc() != null && !city.getWeatherDesc().isEmpty()) {
            tvWeather.setText(city.getWeatherDesc());
            tvWeather.setVisibility(View.VISIBLE);
            
            if (city.getTemperature() != 0) {
                tvTemp.setText(String.format("%d°", city.getTemperature()));
                tvTemp.setVisibility(View.VISIBLE);
            } else {
                tvTemp.setVisibility(View.INVISIBLE);
            }
        } else {
            tvWeather.setVisibility(View.GONE);
            tvTemp.setVisibility(View.INVISIBLE);
        }
    }
} 
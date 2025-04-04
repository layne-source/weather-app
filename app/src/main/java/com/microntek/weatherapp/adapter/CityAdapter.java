package com.microntek.weatherapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.microntek.weatherapp.R;
import com.microntek.weatherapp.model.City;

import java.util.ArrayList;
import java.util.List;

/**
 * 城市列表适配器，用于显示已保存的城市和搜索结果
 */
public class CityAdapter extends RecyclerView.Adapter<CityAdapter.CityViewHolder> {
    
    private List<City> cities; // 已保存的城市列表
    private List<City> searchResults; // 搜索结果列表
    private final OnCityClickListener listener;
    private boolean isShowingSearchResults = false;
    
    /**
     * 城市点击事件监听器接口
     */
    public interface OnCityClickListener {
        void onCityClick(City city, boolean isSavedCity);
        boolean onCityLongClick(City city);
    }
    
    public CityAdapter(List<City> cities, OnCityClickListener listener) {
        this.cities = cities;
        this.searchResults = new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_city, parent, false);
        return new CityViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CityViewHolder holder, int position) {
        City city;
        boolean isSavedCity;
        
        if (isShowingSearchResults) {
            city = searchResults.get(position);
            isSavedCity = false;
        } else {
            city = cities.get(position);
            isSavedCity = true;
        }
        
        holder.bind(city, isSavedCity);
    }
    
    @Override
    public int getItemCount() {
        return isShowingSearchResults ? searchResults.size() : cities.size();
    }
    
    /**
     * 更新城市列表
     */
    public void updateCities(List<City> newCities) {
        this.cities = newCities;
        this.isShowingSearchResults = false;
        notifyDataSetChanged();
    }
    
    /**
     * 更新搜索结果
     */
    public void updateSearchResults(List<City> results) {
        this.searchResults = results;
        this.isShowingSearchResults = true;
        notifyDataSetChanged();
    }
    
    /**
     * 切换回已保存的城市列表
     */
    public void showSavedCities() {
        this.isShowingSearchResults = false;
        notifyDataSetChanged();
    }
    
    /**
     * 城市视图持有者内部类
     */
    class CityViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCityName;
        private final TextView tvCurrentTag;
        private final TextView tvWeatherDesc;
        private final TextView tvWeatherIcon;
        private final TextView tvTemperature;
        
        public CityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCityName = itemView.findViewById(R.id.tv_city_name);
            tvCurrentTag = itemView.findViewById(R.id.tv_current_tag);
            tvWeatherDesc = itemView.findViewById(R.id.tv_weather_desc);
            tvWeatherIcon = itemView.findViewById(R.id.tv_weather_icon);
            tvTemperature = itemView.findViewById(R.id.tv_temperature);
        }
        
        /**
         * 绑定城市数据到视图
         */
        public void bind(final City city, final boolean isSavedCity) {
            // 设置城市名称
            String displayName = city.getName();
            if (!city.getProvince().isEmpty() && !city.getProvince().equals(city.getName())) {
                displayName += ", " + city.getProvince();
            }
            tvCityName.setText(displayName);
            
            // 设置当前位置标记
            if (city.isCurrentLocation() && isSavedCity) {
                tvCurrentTag.setVisibility(View.VISIBLE);
            } else {
                tvCurrentTag.setVisibility(View.GONE);
            }
            
            // 设置天气信息（如果有）
            if (city.getWeatherDesc() != null && !city.getWeatherDesc().isEmpty()) {
                tvWeatherDesc.setText(city.getWeatherDesc());
                tvWeatherDesc.setVisibility(View.VISIBLE);
            } else {
                tvWeatherDesc.setVisibility(View.GONE);
            }
            
            if (city.getWeatherIcon() != null && !city.getWeatherIcon().isEmpty()) {
                tvWeatherIcon.setText(city.getWeatherIcon());
                tvWeatherIcon.setVisibility(View.VISIBLE);
            } else {
                tvWeatherIcon.setVisibility(View.INVISIBLE);
            }
            
            if (city.getTemperature() != 0) {
                tvTemperature.setText(String.format("%d°", city.getTemperature()));
                tvTemperature.setVisibility(View.VISIBLE);
            } else {
                tvTemperature.setVisibility(View.INVISIBLE);
            }
            
            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCityClick(city, isSavedCity);
                }
            });
            
            // 设置长按事件（仅对已保存的城市有效）
            if (isSavedCity) {
                itemView.setOnLongClickListener(v -> {
                    return listener != null && listener.onCityLongClick(city);
                });
            } else {
                itemView.setOnLongClickListener(null);
            }
        }
    }
} 
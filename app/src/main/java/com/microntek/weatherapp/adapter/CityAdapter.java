package com.microntek.weatherapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.microntek.weatherapp.R;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.util.MessageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 城市列表和搜索结果适配器
 * 两种模式共用一个适配器，通过模式标志区分
 */
public class CityAdapter extends BaseCityAdapter<CityAdapter.ViewHolder> {
    
    private List<City> cities;
    private List<City> searchResults;
    private City currentCity;
    private CityClickListener cityClickListener;
    private CityDeleteListener cityDeleteListener;
    private boolean isSearchMode = false;
    
    public CityAdapter(Context context) {
        super(context);
        this.cities = new ArrayList<>();
        this.searchResults = new ArrayList<>();
    }
    
    public void setCurrentCity(City currentCity) {
        this.currentCity = currentCity;
        notifyDataSetChanged();
    }
    
    public void setCities(List<City> cities) {
        this.cities.clear();
        if (cities != null) {
            this.cities.addAll(cities);
        }
        if (!isSearchMode) {
            notifyDataSetChanged();
        }
    }
    
    public List<City> getCities() {
        return new ArrayList<>(cities);
    }
    
    public void setSearchResults(List<City> results) {
        this.searchResults.clear();
        if (results != null) {
            this.searchResults.addAll(results);
        }
        if (isSearchMode) {
            notifyDataSetChanged();
        }
    }
    
    public void setSearchMode(boolean searchMode) {
        if (this.isSearchMode != searchMode) {
            this.isSearchMode = searchMode;
            notifyDataSetChanged();
        }
    }
    
    public boolean isSearchMode() {
        return isSearchMode;
    }
    
    public void setCityClickListener(CityClickListener listener) {
        this.cityClickListener = listener;
    }
    
    public void setCityDeleteListener(CityDeleteListener listener) {
        this.cityDeleteListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_city, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final City city = isSearchMode ? searchResults.get(position) : cities.get(position);
        
        // 设置城市名称
        if (isSearchMode) {
            // 搜索结果显示城市+省份
            if (city.getProvince() != null && !city.getProvince().isEmpty()) {
                holder.tvCityName.setText(city.getName() + ", " + city.getProvince());
            } else {
                holder.tvCityName.setText(city.getName());
            }
        } else {
            // 城市列表使用getDisplayName方法
            holder.tvCityName.setText(city.getDisplayName());
        }
        
        // 设置天气信息
        setupWeatherView(holder.tvWeather, holder.tvTemp, city);
        
        // 设置空气质量信息
        setupAirQualityView(holder.tvAirQuality, city);
        
        // 设置"当前城市"标签
        boolean isCurrentSelectedCity = currentCity != null && 
                currentCity.getId().equals(city.getId());
        
        if (holder.tvCurrentTag != null) {
            holder.tvCurrentTag.setVisibility(
                    (!isSearchMode && isCurrentSelectedCity) ? View.VISIBLE : View.GONE);
        }
        
        // 设置点击事件 - 根据不同模式
        holder.itemView.setOnClickListener(v -> {
            if (cityClickListener != null) {
                cityClickListener.onCityClick(city, !isSearchMode);
            }
        });
        
        // 处理删除按钮/定位图标
        if (isSearchMode) {
            // 搜索结果不显示删除按钮
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setVisibility(View.VISIBLE);
            
            if (city.isCurrentLocation()) {
                // 如果是定位城市，显示导航图标
                holder.btnDelete.setImageResource(R.drawable.ic_location);
                holder.btnDelete.setOnClickListener(v -> {
                    MessageManager.showMessage(context, 
                            "这是您的当前位置城市");
                });
            } else {
                // 如果不是定位城市，显示删除图标
                holder.btnDelete.setImageResource(R.drawable.ic_delete);
                holder.btnDelete.setOnClickListener(v -> {
                    // 无法删除当前选中的城市
                    if (isCurrentSelectedCity) {
                        MessageManager.showError(context, 
                                "不能删除当前选中的城市");
                        return;
                    }
                    
                    // 触发删除监听器
                    if (cityDeleteListener != null) {
                        cityDeleteListener.onCityDelete(city, holder.getAdapterPosition());
                    }
                });
            }
        }
    }
    
    @Override
    public int getItemCount() {
        return isSearchMode ? searchResults.size() : cities.size();
    }
    
    /**
     * ViewHolder类定义
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCityName;
        TextView tvWeather;
        TextView tvTemp;
        TextView tvAirQuality;
        ImageButton btnDelete;
        TextView tvCurrentTag;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvCityName = itemView.findViewById(R.id.tv_city_name);
            tvWeather = itemView.findViewById(R.id.tv_weather);
            tvTemp = itemView.findViewById(R.id.tv_temp);
            tvAirQuality = itemView.findViewById(R.id.tv_air_quality);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            tvCurrentTag = itemView.findViewById(R.id.tv_current_tag);
        }
    }
    
    /**
     * 城市点击事件监听接口
     */
    public interface CityClickListener {
        void onCityClick(City city, boolean isSavedCity);
    }
    
    /**
     * 城市删除事件监听接口
     */
    public interface CityDeleteListener {
        void onCityDelete(City city, int position);
    }
} 
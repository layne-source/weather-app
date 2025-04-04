package com.microntek.weatherapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.microntek.weatherapp.MainActivity;
import com.microntek.weatherapp.R;
import com.microntek.weatherapp.adapter.CityAdapter;
import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.util.CityPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CityManagerActivity extends AppCompatActivity implements CityAdapter.OnCityClickListener, BottomNavigationView.OnNavigationItemSelectedListener {
    
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private View loadingView;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigationView;
    
    private CityAdapter cityAdapter;
    private CityPreferences cityPreferences;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private City currentCity;
    private List<City> cities = new ArrayList<>();
    private CityListAdapter cityListAdapter;
    private List<City> searchResults = new ArrayList<>();
    private SearchResultAdapter searchResultAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_manager);
        
        // 初始化UI组件
        recyclerView = findViewById(R.id.recycler_view);
        searchEditText = findViewById(R.id.et_search);
        loadingView = findViewById(R.id.loading_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // 设置工具栏
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // 初始化数据
        cityPreferences = new CityPreferences(this);
        
        // 设置城市列表
        setupCityList();
        
        // 初始化搜索结果适配器
        searchResultAdapter = new SearchResultAdapter();
        
        // 设置搜索栏
        setupSearchBar();
        
        // 设置底部导航栏
        setupBottomNavigation();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 设置底部导航选中状态为城市
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_city);
        }
    }
    
    /**
     * 设置底部导航栏
     */
    private void setupBottomNavigation() {
        // 设置选中状态为城市
        bottomNavigationView.setSelectedItemId(R.id.navigation_city);
        
        // 设置导航栏项目点击事件
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }
    
    /**
     * 底部导航栏点击事件
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.navigation_home:
                // 返回主页并设置标志
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("fromOtherActivity", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
                return true;
                
            case R.id.navigation_city:
                // 已经在城市管理页面，不需要处理
                return true;
                
            case R.id.navigation_air:
                // 切换到空气质量页面
                intent = new Intent(this, com.microntek.weatherapp.ui.AirQualityActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
                return true;
                
            case R.id.navigation_settings:
                // 切换到设置页面
                intent = new Intent(this, com.microntek.weatherapp.ui.SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
                return true;
        }
        return false;
    }
    
    /**
     * 设置城市列表
     */
    private void setupCityList() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // 获取保存的城市列表
        List<City> savedCities = cityPreferences.getSavedCities();
        cities.clear();
        
        // 如果没有保存的城市，添加默认城市（北京）
        if (savedCities.isEmpty()) {
            City beijing = new City("北京", "39.9042,116.4074", "北京市", 39.9042, 116.4074);
            beijing.setCurrentLocation(true);
            savedCities.add(beijing);
            cityPreferences.addCity(beijing);
            cityPreferences.setCurrentCity(beijing);
        }
        
        // 获取当前城市
        currentCity = cityPreferences.getCurrentCity();
        
        // 重新排序城市列表，将当前城市排在第一位
        List<City> sortedCities = new ArrayList<>();
        
        // 首先添加当前城市
        for (City city : savedCities) {
            if (currentCity != null && city.getId().equals(currentCity.getId())) {
                city.setCurrentLocation(true);
                sortedCities.add(city);
                break;
            }
        }
        
        // 然后添加其他城市
        for (City city : savedCities) {
            if (currentCity == null || !city.getId().equals(currentCity.getId())) {
                city.setCurrentLocation(false);
                sortedCities.add(city);
            }
        }
        
        cities.addAll(sortedCities);
        
        // 设置适配器
        cityListAdapter = new CityListAdapter();
        recyclerView.setAdapter(cityListAdapter);
        
        // 加载每个城市的天气数据
        loadCitiesWeather();
    }
    
    /**
     * 设置搜索栏
     */
    private void setupSearchBar() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                // 这里可以实现实时搜索功能
            }
        });
        
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchCity(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 搜索城市
     */
    private void searchCity(String cityName) {
        if (TextUtils.isEmpty(cityName)) {
            return;
        }

        // 隐藏键盘
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        showLoading();

        String finalCityName = cityName;
        executor.execute(() -> {
            List<City> results = new ArrayList<>();
            try {
                results = WeatherApi.searchCity(finalCityName);
            } catch (Exception e) {
                e.printStackTrace();
            }

            List<City> exactMatches = new ArrayList<>();
            // 精确匹配城市名
            for (City city : results) {
                if (city.getName().equals(finalCityName)) {
                    exactMatches.add(city);
                    break;
                }
            }

            // 如果有精确匹配则使用精确匹配结果，否则使用第一个结果
            final List<City> finalResults = !exactMatches.isEmpty() ? exactMatches : 
                                          !results.isEmpty() ? Collections.singletonList(results.get(0)) : 
                                          new ArrayList<>();

            runOnUiThread(() -> {
                hideLoading();
                // 清空搜索框
                searchEditText.setText("");
                searchResultAdapter.setData(finalResults);
            });
        });
    }
    
    /**
     * 显示搜索结果
     */
    private void showSearchResults() {
        recyclerView.setAdapter(searchResultAdapter);
        searchResultAdapter.notifyDataSetChanged();
    }
    
    /**
     * 隐藏搜索结果，显示城市列表
     */
    private void hideSearchResult() {
        recyclerView.setAdapter(cityListAdapter);
    }
    
    /**
     * 加载城市的天气数据
     */
    private void loadCitiesWeather() {
        if (cities.isEmpty()) {
            return;
        }
        
        // 在后台线程加载数据
        executor.execute(() -> {
            List<City> updatedCities = new ArrayList<>();
            
            for (City city : new ArrayList<>(cities)) {
                try {
                    // 获取当前天气
                    final com.microntek.weatherapp.model.Weather weather = 
                            WeatherApi.getCurrentWeatherByLocation(city.getLatitude(), city.getLongitude());
                    
                    // 更新城市的天气信息
                    city.setTemperature(weather.getCurrentTemp());
                    city.setWeatherDesc(weather.getWeatherDesc());
                    city.setWeatherIcon(weather.getWeatherIcon());
                    
                    updatedCities.add(city);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 出错时仍保留城市，但不更新天气
                    updatedCities.add(city);
                }
            }
            
            // 在主线程更新UI
            final List<City> finalCities = updatedCities;
            mainHandler.post(() -> {
                // 获取当前城市
                currentCity = cityPreferences.getCurrentCity();
                
                // 重新排序城市列表，将当前城市排在第一位
                List<City> sortedCities = new ArrayList<>();
                
                // 首先添加当前城市
                for (City c : finalCities) {
                    if (currentCity != null && c.getId().equals(currentCity.getId())) {
                        c.setCurrentLocation(true);
                        sortedCities.add(c);
                        break;
                    }
                }
                
                // 然后添加其他城市
                for (City c : finalCities) {
                    if (currentCity == null || !c.getId().equals(currentCity.getId())) {
                        c.setCurrentLocation(false);
                        sortedCities.add(c);
                    }
                }
                
                cities.clear();
                cities.addAll(sortedCities);
                cityListAdapter.notifyDataSetChanged();
            });
        });
    }
    
    /**
     * 添加城市
     */
    private void addCity(City city) {
        // 添加城市到偏好设置
        cityPreferences.addCity(city);
        
        // 刷新城市列表
        List<City> updatedCities = cityPreferences.getSavedCities();
        
        // 获取当前城市
        currentCity = cityPreferences.getCurrentCity();
        
        // 重新排序城市列表，将当前城市排在第一位
        List<City> sortedCities = new ArrayList<>();
        
        // 首先添加当前城市
        for (City c : updatedCities) {
            if (currentCity != null && c.getId().equals(currentCity.getId())) {
                c.setCurrentLocation(true);
                sortedCities.add(c);
                break;
            }
        }
        
        // 然后添加其他城市
        for (City c : updatedCities) {
            if (currentCity == null || !c.getId().equals(currentCity.getId())) {
                c.setCurrentLocation(false);
                sortedCities.add(c);
            }
        }
        
        cities.clear();
        cities.addAll(sortedCities);
        cityListAdapter.notifyDataSetChanged();
        
        // 加载新添加城市的天气
        loadCitiesWeather();
        
        Toast.makeText(this, "已添加城市: " + city.getName(), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 删除城市
     */
    private void deleteCity(City city) {
        // 从偏好设置中删除城市
        cityPreferences.removeCity(city);
        
        // 刷新城市列表
        cities.clear();
        cities.addAll(cityPreferences.getSavedCities());
        cityListAdapter.notifyDataSetChanged();
        
        Toast.makeText(this, "已删除城市: " + city.getName(), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 切换当前城市，并返回主页
     */
    private void switchCurrentCity(City city) {
        // 设置当前城市
        cityPreferences.setCurrentCity(city);
        currentCity = city;
        
        // 刷新城市列表，更新当前城市标记
        List<City> updatedCities = cityPreferences.getSavedCities();
        
        // 重新排序城市列表，将当前城市排在第一位
        List<City> sortedCities = new ArrayList<>();
        
        // 首先添加当前城市
        for (City c : updatedCities) {
            if (c.getId().equals(city.getId())) {
                c.setCurrentLocation(true);
                sortedCities.add(c);
                break;
            }
        }
        
        // 然后添加其他城市
        for (City c : updatedCities) {
            if (!c.getId().equals(city.getId())) {
                c.setCurrentLocation(false);
                sortedCities.add(c);
            }
        }
        
        cities.clear();
        cities.addAll(sortedCities);
        cityListAdapter.notifyDataSetChanged();
        
        Toast.makeText(this, "已切换到城市: " + city.getName(), Toast.LENGTH_SHORT).show();
        
        // 返回主页并设置标志位
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("fromOtherActivity", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
    
    /**
     * 城市点击事件回调
     */
    @Override
    public void onCityClick(City city, boolean isSavedCity) {
        if (isSavedCity) {
            // 点击已保存的城市，切换为当前城市
            switchCurrentCity(city);
        } else {
            // 点击搜索结果中的城市，添加到保存列表
            addCity(city);
        }
    }
    
    /**
     * 城市长按事件回调（仅用于已保存的城市）
     */
    @Override
    public boolean onCityLongClick(City city) {
        // 长按删除城市（不能删除当前选中的城市）
        if (city.isCurrentLocation()) {
            Toast.makeText(this, "不能删除当前选中的城市", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        deleteCity(city);
        return true;
    }
    
    /**
     * 城市列表适配器
     */
    private class CityListAdapter extends RecyclerView.Adapter<CityListAdapter.ViewHolder> {
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCityName;
            TextView tvWeather;
            TextView tvTemp;
            ImageButton btnDelete;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvCityName = itemView.findViewById(R.id.tv_city_name);
                tvWeather = itemView.findViewById(R.id.tv_weather);
                tvTemp = itemView.findViewById(R.id.tv_temp);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final City city = cities.get(position);
            
            // 使用getDisplayName()方法显示带有省份和区县信息的城市名称
            holder.tvCityName.setText(city.getDisplayName());
            
            // 显示天气信息
            if (city.getWeatherDesc() != null) {
                holder.tvWeather.setText(city.getWeatherDesc());
                holder.tvTemp.setText(String.format("%d°", city.getTemperature()));
            }
            
            // 设置选中状态
            boolean isCurrentCity = currentCity != null && 
                    currentCity.getId().equals(city.getId());
            holder.itemView.setBackgroundResource(isCurrentCity ? 
                    R.color.selected_bg : R.color.normal_bg);
            
            // 显示或隐藏"当前"标签
            TextView tvCurrentTag = holder.itemView.findViewById(R.id.tv_current_tag);
            if (tvCurrentTag != null) {
                tvCurrentTag.setVisibility(isCurrentCity ? View.VISIBLE : View.GONE);
            }
            
            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                // 设置为当前选中的城市
                cityPreferences.setCurrentCity(city);
                currentCity = city;
                
                // 刷新列表
                notifyDataSetChanged();
                
                // 返回主页
                finish();
            });
            
            // 设置删除按钮点击事件
            holder.btnDelete.setOnClickListener(v -> {
                // 无法删除当前选中的城市
                if (isCurrentCity) {
                    Toast.makeText(CityManagerActivity.this, 
                            "不能删除当前选中的城市", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 从列表和偏好设置中删除城市
                cityPreferences.removeCity(city);
                cities.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, cities.size() - position);
            });
        }

        @Override
        public int getItemCount() {
            return cities.size();
        }
    }
    
    /**
     * 搜索城市结果的适配器
     */
    private class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCityName;
            TextView tvWeather;
            TextView tvTemp;
            ImageButton btnDelete;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvCityName = itemView.findViewById(R.id.tv_city_name);
                tvWeather = itemView.findViewById(R.id.tv_weather);
                tvTemp = itemView.findViewById(R.id.tv_temp);
                btnDelete = itemView.findViewById(R.id.btn_delete);
                
                // 隐藏删除按钮
                btnDelete.setVisibility(View.GONE);
            }
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final City city = searchResults.get(position);
            
            // 显示城市名称，不包含县级信息
            if (city.getProvince() != null && !city.getProvince().isEmpty()) {
                holder.tvCityName.setText(city.getName() + ", " + city.getProvince());
            } else {
                holder.tvCityName.setText(city.getName());
            }
            
            // 清空天气信息（搜索结果不显示天气）
            holder.tvWeather.setText("");
            holder.tvTemp.setText("");
            
            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                // 检查是否已存在该城市
                boolean cityExists = false;
                for (City existingCity : cities) {
                    if (existingCity.getId().equals(city.getId())) {
                        cityExists = true;
                        break;
                    }
                }
                
                if (cityExists) {
                    Toast.makeText(CityManagerActivity.this, 
                            "该城市已添加", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 添加城市
                cityPreferences.addCity(city);
                cities.add(city);
                
                // 隐藏搜索结果
                hideSearchResult();
                
                // 刷新城市列表
                cityListAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return searchResults.size();
        }

        void setData(List<City> results) {
            searchResults = results;
            notifyDataSetChanged();
        }
    }

    /**
     * 显示加载中状态
     */
    private void showLoading() {
        // 如果布局中有loading_view，则显示
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        } else {
            // 使用Toast作为备选方案
            Toast.makeText(this, "正在搜索...", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 隐藏加载中状态
     */
    private void hideLoading() {
        // 如果布局中有loading_view，则隐藏
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
        // 显示搜索结果
        showSearchResults();
    }

    @Override
    public void finish() {
        // 在返回MainActivity之前，设置标志位
        Intent intent = new Intent();
        intent.putExtra("fromOtherActivity", true);
        setResult(RESULT_OK, intent);
        super.finish();
    }
} 
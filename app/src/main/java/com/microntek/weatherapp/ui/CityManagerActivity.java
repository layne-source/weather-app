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
import android.util.Log;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.microntek.weatherapp.MainActivity;
import com.microntek.weatherapp.R;
import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.util.CityPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class CityManagerActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    
    private static final String TAG = "CityManagerActivity";
    
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private View loadingView;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigationView;
    
    // 添加缺失的UI组件变量
    private TextView noResultsText;
    private ImageButton clearSearchButton;

    private CityPreferences cityPreferences;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private City currentCity;
    private List<City> cities = new ArrayList<>();
    private CityListAdapter cityListAdapter;
    private List<City> searchResults = new ArrayList<>();
    private SearchResultAdapter searchResultAdapter;
    
    // 用于后台任务处理
    private final Executor backgroundExecutor = Executors.newCachedThreadPool();
    
    // 用于防止重复操作
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    private final AtomicBoolean isSearching = new AtomicBoolean(false);
    
    // 搜索模式标志
    private boolean isSearchMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_manager);
        
        // 初始化UI组件
        recyclerView = findViewById(R.id.recycler_view);
        searchEditText = findViewById(R.id.et_search);
        loadingView = findViewById(R.id.loading_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // 初始化搜索相关UI组件
        noResultsText = findViewById(R.id.tv_no_results);
        clearSearchButton = findViewById(R.id.btn_clear_search);

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
        
        // 确保不在搜索模式
        isSearchMode = false;
        
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
        
        // 设置清除搜索按钮点击事件
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            hideSearchResult();
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

        // 设置搜索模式标志
        isSearchMode = true;
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
                                          
            // 加载每个搜索结果城市的天气数据
            for (City city : finalResults) {
                try {
                    // 获取当前天气
                    final com.microntek.weatherapp.model.Weather weather = 
                            WeatherApi.getCurrentWeatherByLocation(city.getLatitude(), city.getLongitude());
                    
                    // 更新城市的天气信息
                    city.setTemperature(weather.getCurrentTemp());
                    city.setWeatherDesc(weather.getWeatherDesc());
                    city.setWeatherIcon(weather.getWeatherIcon());
                    
                    // 更新空气质量信息
                    city.setAirQuality(weather.getAirQuality());
                    city.setAqi(weather.getAqi());
                } catch (Exception e) {
                    e.printStackTrace();
                    // 出错时继续处理其他城市
                }
            }

            runOnUiThread(() -> {
                hideLoading();
                // 清空搜索框
                searchEditText.setText("");
                
                // 更新搜索结果
                searchResults.clear();
                searchResults.addAll(finalResults);
                searchResultAdapter.setData(finalResults);
                
                // 执行UI更新，包括无结果提示的显示/隐藏
                showSearchResults();
            });
        });
    }
    
    /**
     * 显示搜索结果
     */
    private void showSearchResults() {
        recyclerView.setAdapter(searchResultAdapter);
        searchResultAdapter.notifyDataSetChanged();
        isSearchMode = true;
        
        // 根据搜索结果显示或隐藏无结果提示
        if (searchResults.isEmpty()) {
            noResultsText.setVisibility(View.VISIBLE);
        } else {
            noResultsText.setVisibility(View.GONE);
        }
        
        // 显示清除搜索按钮
        clearSearchButton.setVisibility(View.VISIBLE);
        
    }
    
    /**
     * 隐藏搜索结果
     */
    private void hideSearchResult() {
        isSearchMode = false;
        searchResults.clear();
        recyclerView.setAdapter(cityListAdapter);
        
        // 更新适配器以显示城市列表
        cityListAdapter.notifyDataSetChanged();
        
        // 隐藏结果提示和清除按钮
        noResultsText.setVisibility(View.GONE);
        clearSearchButton.setVisibility(View.GONE);
        
    }
    
    /**
     * 加载城市的天气数据
     */
    private void loadCitiesWeather() {
        if (cities.isEmpty()) {
            return;
        }
        
        // 确保不在搜索模式
        isSearchMode = false;
        
        showLoading();
        
        // 在后台线程加载数据
        executor.execute(() -> {
            List<City> updatedCities = new ArrayList<>();
            
            for (City city : new ArrayList<>(cities)) {
                try {
                    // 使用缓存方式获取当前天气
                    com.microntek.weatherapp.model.Weather weather;
                    try {
                        if (city.isCurrentLocation() || city.getId().contains(",")) {
                            // 使用经纬度获取天气
                            weather = WeatherApi.getCurrentWeatherByLocationWithCache(
                                    getApplicationContext(), 
                                    city.getLatitude(), 
                                    city.getLongitude());
                        } else {
                            // 使用城市ID获取天气
                            weather = WeatherApi.getCurrentWeatherWithCache(
                                    getApplicationContext(), 
                                    city.getId());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "使用缓存获取天气失败，尝试从网络获取: " + e.getMessage());
                        
                        // 如果缓存获取失败，尝试直接从API获取
                        if (city.isCurrentLocation() || city.getId().contains(",")) {
                            weather = WeatherApi.getCurrentWeatherByLocation(
                                    city.getLatitude(), 
                                    city.getLongitude());
                        } else {
                            weather = WeatherApi.getCurrentWeather(city.getId());
                        }
                    }
                    
                    // 更新城市的天气信息
                    city.setTemperature(weather.getCurrentTemp());
                    city.setWeatherDesc(weather.getWeatherDesc());
                    city.setWeatherIcon(weather.getWeatherIcon());
                    
                    // 更新空气质量信息
                    city.setAirQuality(weather.getAirQuality());
                    city.setAqi(weather.getAqi());
                    
                    updatedCities.add(city);
                } catch (Exception e) {
                    Log.e(TAG, "加载城市 " + city.getName() + " 的天气失败: " + e.getMessage());
                    // 出错时仍保留城市，但不更新天气
                    updatedCities.add(city);
                }
            }
            
            // 在主线程更新UI
            final List<City> finalCities = updatedCities;
            mainHandler.post(() -> {
                hideLoading();
                
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
        // 检查输入
        if (city == null) {
            return;
        }
        
        // 防止重复添加操作
        if (isLoading.getAndSet(true)) {
            return;
        }
        
        // 检查城市数量限制
        if (cityPreferences.getSavedCities().size() >= 5) {
            Toast.makeText(this, "最多只能添加5个城市", Toast.LENGTH_SHORT).show();
            isLoading.set(false);
            return;
        }
        
        // 显示加载状态
        showLoading();
        
        executor.execute(() -> {
            try {
                // 检查城市是否已存在
                boolean cityExists = false;
                for (City existingCity : cityPreferences.getSavedCities()) {
                    if (existingCity.getId().equals(city.getId())) {
                        cityExists = true;
                        break;
                    }
                }
                
                if (cityExists) {
                    mainHandler.post(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "该城市已添加", Toast.LENGTH_SHORT).show();
                        isLoading.set(false);
                    });
                    return;
                }
                
                // 添加城市到偏好设置（异步预加载城市数据）
                boolean success = cityPreferences.addCity(city);
                
                if (!success) {
                    mainHandler.post(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "添加城市失败", Toast.LENGTH_SHORT).show();
                        isLoading.set(false);
                    });
                    return;
                }
                
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
                
                final List<City> finalSortedCities = sortedCities;
                mainHandler.post(() -> {
                    hideLoading();
                    
                    cities.clear();
                    cities.addAll(finalSortedCities);
                    
                    // 切换回城市列表界面
                    hideSearchResult();
                    
                    // 刷新适配器
                    cityListAdapter.notifyDataSetChanged();
                    
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "已添加城市: " + city.getName(), 
                            Snackbar.LENGTH_LONG).show();
                    
                    isLoading.set(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "添加城市时出错: " + e.getMessage());
                mainHandler.post(() -> {
                    hideLoading();
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "添加城市失败: " + e.getMessage(), 
                            Snackbar.LENGTH_LONG)
                            .setAction("重试", v -> addCity(city))
                            .show();
                    isLoading.set(false);
                });
            }
        });
    }
    
    /**
     * 删除城市
     */
    private void deleteCity(City city) {
        // 检查输入
        if (city == null) {
            return;
        }
        
        // 防止重复删除操作
        if (isLoading.getAndSet(true)) {
            return;
        }
        
        // 显示加载状态
        showLoading();
        
        executor.execute(() -> {
            try {
                // 如果是当前城市，不能删除
                if (currentCity != null && city.getId().equals(currentCity.getId())) {
                    mainHandler.post(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "不能删除当前选中的城市", Toast.LENGTH_SHORT).show();
                        isLoading.set(false);
                    });
                    return;
                }
                
                // 从偏好设置中删除城市（同时会清除相关缓存）
                boolean success = cityPreferences.removeCity(city);
                
                if (!success) {
                    mainHandler.post(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "删除城市失败", Toast.LENGTH_SHORT).show();
                        isLoading.set(false);
                    });
                    return;
                }
                
                // 刷新城市列表
                List<City> updatedCities = cityPreferences.getSavedCities();
                
                // 获取当前城市（可能已经更新）
                currentCity = cityPreferences.getCurrentCity();
                
                final List<City> finalUpdatedCities = updatedCities;
                mainHandler.post(() -> {
                    hideLoading();
                    
                    cities.clear();
                    cities.addAll(finalUpdatedCities);
                    
                    cityListAdapter.notifyDataSetChanged();
                    
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "已删除城市: " + city.getName(), 
                            Snackbar.LENGTH_LONG).show();
                    
                    isLoading.set(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "删除城市时出错: " + e.getMessage());
                mainHandler.post(() -> {
                    hideLoading();
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "删除城市失败: " + e.getMessage(), 
                            Snackbar.LENGTH_LONG)
                            .setAction("重试", v -> deleteCity(city))
                            .show();
                    isLoading.set(false);
                });
            }
        });
    }
    
    /**
     * 切换当前城市，并返回主页
     */
    private void switchCurrentCity(City city) {
        // 检查输入
        if (city == null) {
            return;
        }
        
        // 防止重复切换操作
        if (isLoading.getAndSet(true)) {
            return;
        }
        
        // 显示加载状态
        showLoading();
        
        executor.execute(() -> {
            try {
                // 检查是否已经是当前城市
                City current = cityPreferences.getCurrentCity();
                if (current != null && current.getId().equals(city.getId())) {
                    mainHandler.post(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                city.getName() + " 已是当前城市", Toast.LENGTH_SHORT).show();
                        isLoading.set(false);
                    });
                    return;
                }
                
                // 设置当前城市（同时会预加载天气数据）
                boolean success = cityPreferences.setCurrentCity(city);
                
                if (!success) {
                    mainHandler.post(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "切换城市失败", Toast.LENGTH_SHORT).show();
                        isLoading.set(false);
                    });
                    return;
                }
                
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
                
                mainHandler.post(() -> {
                    hideLoading();
                    
                    cities.clear();
                    cities.addAll(sortedCities);
                    cityListAdapter.notifyDataSetChanged();
                    
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "已切换到城市: " + city.getName(), 
                            Snackbar.LENGTH_SHORT).show();
                    
                    // 延迟500ms，让用户看到切换成功的提示
                    new Handler().postDelayed(() -> {
                        // 返回主页并设置标志位
                        Intent intent = new Intent(CityManagerActivity.this, MainActivity.class);
                        intent.putExtra("fromOtherActivity", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }, 500);
                    
                    isLoading.set(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "切换城市时出错: " + e.getMessage());
                mainHandler.post(() -> {
                    hideLoading();
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "切换城市失败: " + e.getMessage(), 
                            Snackbar.LENGTH_LONG)
                            .setAction("重试", v -> switchCurrentCity(city))
                            .show();
                    isLoading.set(false);
                });
            }
        });
    }
    
    /**
     * 城市列表适配器
     */
    private class CityListAdapter extends RecyclerView.Adapter<CityListAdapter.ViewHolder> {
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCityName;
            TextView tvWeather;
            TextView tvTemp;
            TextView tvAirQuality;
            ImageButton btnDelete;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvCityName = itemView.findViewById(R.id.tv_city_name);
                tvWeather = itemView.findViewById(R.id.tv_weather);
                tvTemp = itemView.findViewById(R.id.tv_temp);
                tvAirQuality = itemView.findViewById(R.id.tv_air_quality);
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
            
            // 显示空气质量信息
            if (city.getAirQuality() != null && !city.getAirQuality().isEmpty()) {
                // 格式：空气+质量等级+空格+AQI数值
                String airQualityText = "空气" + city.getAirQuality() + " " + city.getAqi();
                holder.tvAirQuality.setText(airQualityText);
                holder.tvAirQuality.setVisibility(View.VISIBLE);
                
                // 根据AQI值设置背景颜色
                if (city.getAqi() > 0) {
                    // 设置圆角背景
                    holder.tvAirQuality.setBackgroundResource(city.getAqiColorRes());
                    // 设置背景颜色
                    holder.tvAirQuality.getBackground().setColorFilter(
                        getResources().getColor(city.getAqiColorValue()), 
                        android.graphics.PorterDuff.Mode.SRC_IN);
                }
            } else {
                holder.tvAirQuality.setVisibility(View.GONE);
            }
            
            // 设置选中状态
            boolean isCurrentCity = currentCity != null && 
                    currentCity.getId().equals(city.getId());

            // 显示或隐藏"当前"标签
            TextView tvCurrentTag = holder.itemView.findViewById(R.id.tv_current_tag);
            if (tvCurrentTag != null) {
                tvCurrentTag.setVisibility(isCurrentCity ? View.VISIBLE : View.GONE);
            }
            
            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                onCityClick(city, true);
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
            TextView tvAirQuality;
            ImageButton btnDelete;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvCityName = itemView.findViewById(R.id.tv_city_name);
                tvWeather = itemView.findViewById(R.id.tv_weather);
                tvTemp = itemView.findViewById(R.id.tv_temp);
                tvAirQuality = itemView.findViewById(R.id.tv_air_quality);
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
            
            // 显示天气信息（如果有）
            if (city.getWeatherDesc() != null && !city.getWeatherDesc().isEmpty()) {
                holder.tvWeather.setText(city.getWeatherDesc());
                holder.tvWeather.setVisibility(View.VISIBLE);
            } else {
                holder.tvWeather.setVisibility(View.GONE);
            }
            
            // 显示温度信息（如果有）
            if (city.getTemperature() != 0) {
                holder.tvTemp.setText(String.format("%d°", city.getTemperature()));
                holder.tvTemp.setVisibility(View.VISIBLE);
            } else {
                holder.tvTemp.setVisibility(View.INVISIBLE);
            }
            
            // 显示空气质量信息（如果有）
            if (city.getAirQuality() != null && !city.getAirQuality().isEmpty()) {
                // 格式：空气+质量等级+空格+AQI数值
                String airQualityText = "空气" + city.getAirQuality() + " " + city.getAqi();
                holder.tvAirQuality.setText(airQualityText);
                holder.tvAirQuality.setVisibility(View.VISIBLE);
                
                // 根据AQI值设置背景颜色
                if (city.getAqi() > 0) {
                    // 设置圆角背景
                    holder.tvAirQuality.setBackgroundResource(city.getAqiColorRes());
                    // 设置背景颜色
                    holder.tvAirQuality.getBackground().setColorFilter(
                        getResources().getColor(city.getAqiColorValue()), 
                        android.graphics.PorterDuff.Mode.SRC_IN);
                }
            } else {
                holder.tvAirQuality.setVisibility(View.GONE);
            }
            
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
                
                // 检查是否已达到城市数量上限
                if (cityPreferences.getSavedCities().size() >= 5) {
                    Toast.makeText(CityManagerActivity.this, 
                            "最多只能添加5个城市", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 添加城市
                onCityClick(city, false);
            });
        }

        @Override
        public int getItemCount() {
            return searchResults.size();
        }

        void setData(List<City> results) {
            // 不再在这里直接修改searchResults，因为在调用前已经更新
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
        
        // 只有在搜索模式下才显示搜索结果
        if (isSearchMode) {
            showSearchResults();
        } else {
            // 确保城市列表显示
            if (recyclerView.getAdapter() != cityListAdapter) {
                recyclerView.setAdapter(cityListAdapter);
                cityListAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void finish() {
        // 在返回MainActivity之前，设置标志位
        Intent intent = new Intent();
        intent.putExtra("fromOtherActivity", true);
        setResult(RESULT_OK, intent);
        super.finish();
    }

    /**
     * 城市点击事件监听器接口
     */
    public interface OnCityClickListener {
        void onCityClick(City city, boolean isSavedCity);
    }
    
    /**
     * 城市点击事件回调
     */
    public void onCityClick(City city, boolean isSavedCity) {
        if (isSavedCity) {
            // 点击已保存的城市，切换为当前城市
            switchCurrentCity(city);
        } else {
            // 点击搜索结果中的城市，添加到保存列表
            addCity(city);
        }
    }

    @Override
    protected void onDestroy() {
        // 清理资源
        if (cityPreferences != null) {
            cityPreferences.onDestroy();
        }
        
        // 取消所有后台任务
        if (executor instanceof java.util.concurrent.ExecutorService) {
            try {
                ((java.util.concurrent.ExecutorService) executor).shutdown();
                Log.i(TAG, "已关闭后台任务执行器");
            } catch (Exception e) {
                Log.e(TAG, "关闭执行器失败: " + e.getMessage());
            }
        }
        
        super.onDestroy();
    }
} 
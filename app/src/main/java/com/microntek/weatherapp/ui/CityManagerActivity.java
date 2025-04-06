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
import com.microntek.weatherapp.model.Weather;
import com.microntek.weatherapp.util.CityPreferences;
import com.microntek.weatherapp.util.WeatherDataHelper;
import com.microntek.weatherapp.util.ExecutorManager;
import com.microntek.weatherapp.util.TaskManager;
import com.microntek.weatherapp.util.LocationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class CityManagerActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    
    private static final String TAG = "CityManagerActivity";
    
    // 城市数据
    private List<City> cities = new ArrayList<>();
    private City currentCity;
    
    // UI组件
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private View loadingView;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigationView;
    
    // 添加缺失的UI组件变量
    private TextView noResultsText;
    private ImageButton clearSearchButton;
    private ImageButton locationButton;

    private CityPreferences cityPreferences;
    private CityListAdapter cityListAdapter;
    private List<City> searchResults = new ArrayList<>();
    private SearchResultAdapter searchResultAdapter;
    
    // 添加位置工具类
    private LocationHelper locationHelper;
    
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
        locationButton = findViewById(R.id.btn_location);

        // 设置工具栏
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // 初始化数据
        cityPreferences = new CityPreferences(this);
        
        // 初始化位置工具类
        locationHelper = new LocationHelper(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationSuccess(double latitude, double longitude) {
                processLocationResult(latitude, longitude);
            }

            @Override
            public void onLocationFailed(String error) {
                hideLoading();
                Toast.makeText(CityManagerActivity.this, getString(R.string.location_failed) + ": " + error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 设置城市列表
        setupCityList();
        
        // 初始化搜索结果适配器
        searchResultAdapter = new SearchResultAdapter();
        
        // 设置搜索栏
        setupSearchBar();
        
        // 设置底部导航栏
        setupBottomNavigation();
        
        // 设置定位按钮
        setupLocationButton();
    }
    
    /**
     * 设置定位按钮
     */
    private void setupLocationButton() {
        locationButton.setOnClickListener(v -> {
            // 检查并请求位置权限
            if (!locationHelper.hasLocationPermission()) {
                locationHelper.requestLocationPermission(this);
                return;
            }
            
            // 检查位置服务是否开启
            if (!locationHelper.isLocationEnabled()) {
                Snackbar.make(findViewById(android.R.id.content), 
                        "位置服务未开启，请开启后再试", 
                        Snackbar.LENGTH_LONG)
                        .setAction("设置", view -> locationHelper.openLocationSettings(this))
                        .show();
                return;
            }
            
            // 显示正在定位提示
            Toast.makeText(this, R.string.locating, Toast.LENGTH_SHORT).show();
            showLoading();
            
            // 开始定位
            locationHelper.getCurrentLocation();
        });
    }
    
    /**
     * 处理位置定位结果
     */
    private void processLocationResult(double latitude, double longitude) {
        // 在后台线程获取城市信息
        ExecutorManager.getThreadPoolExecutor().execute(() -> {
            try {
                // 获取当前城市信息
                City city = WeatherApi.getCityByLocation(latitude, longitude);
                
                // 在添加城市前获取空气质量信息
                try {
                    String locationId = city.getLongitude() + "," + city.getLatitude();
                    // 获取城市天气信息
                    Weather weather = WeatherApi.getCurrentWeatherByLocation(latitude, longitude);
                    // 获取空气质量信息
                    weather = WeatherApi.getAirQuality(locationId, weather);
                    
                    // 将空气质量信息和天气信息设置到城市对象
                    city.setAirQuality(weather.getAirQuality());
                    city.setAqi(weather.getAqi());
                    city.setTemperature(weather.getCurrentTemp());
                    city.setWeatherDesc(weather.getWeatherDesc());
                    city.setWeatherIcon(weather.getWeatherIcon());
                } catch (Exception e) {
                    Log.e(TAG, "获取空气质量数据失败: " + e.getMessage());
                    // 即使获取空气质量失败，仍然继续添加城市
                }
                
                // 切换到主线程添加城市
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(CityManagerActivity.this, R.string.location_successful, Toast.LENGTH_SHORT).show();
                    // 添加城市，和普通添加城市的逻辑一样
                    addCity(city);
                });
            } catch (Exception e) {
                Log.e(TAG, "获取城市信息失败: " + e.getMessage());
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(CityManagerActivity.this, 
                            "获取城市信息失败: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
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
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.REQUEST_LOCATION_PERMISSION) {
            // 检查是否获得了权限
            if (locationHelper.hasLocationPermission()) {
                // 权限已获得，检查位置服务并开始定位
                if (locationHelper.isLocationEnabled()) {
                    Toast.makeText(this, R.string.locating, Toast.LENGTH_SHORT).show();
                    showLoading();
                    locationHelper.getCurrentLocation();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), 
                            "位置服务未开启，请开启后再试", 
                            Snackbar.LENGTH_LONG)
                            .setAction("设置", view -> locationHelper.openLocationSettings(this))
                            .show();
                }
            } else {
                // 权限被拒绝
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_SHORT).show();
            }
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
        
        // 删除自动添加北京的逻辑，保留现有城市
        cities.addAll(savedCities);
        
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
        
        cities.clear();
        cities.addAll(sortedCities);
        
        // 设置适配器
        cityListAdapter = new CityListAdapter();
        recyclerView.setAdapter(cityListAdapter);
        
        // 确保不在搜索模式
        isSearchMode = false;
        
        // 如果城市列表为空，显示提示信息；否则不处理
        if (!cities.isEmpty()) {
            // 加载每个城市的天气数据
            loadCitiesWeather();
        }
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

        // 使用TaskManager执行搜索任务
        final String taskId = "SEARCH_CITY_" + cityName;
        com.microntek.weatherapp.util.TaskManager.executeParallelTask(taskId, () -> {
            List<City> results = new ArrayList<>();
            try {
                results = WeatherApi.searchCity(cityName);
            } catch (Exception e) {
                e.printStackTrace();
                com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "搜索城市失败: " + e.getMessage(),
                            Snackbar.LENGTH_LONG)
                            .setAction("重试", v -> searchCity(cityName))
                            .show();
                });
                return;
            }

            List<City> exactMatches = new ArrayList<>();
            // 精确匹配城市名
            for (City city : results) {
                if (city.getName().equals(cityName)) {
                    exactMatches.add(city);
                    break;
                }
            }

            // 如果有精确匹配则使用精确匹配结果，否则使用第一个结果
            final List<City> finalResults = !exactMatches.isEmpty() ? exactMatches : 
                                          !results.isEmpty() ? Collections.singletonList(results.get(0)) : 
                                          new ArrayList<>();
            
            if (finalResults.isEmpty()) {
                com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    // 更新搜索结果
                    searchResults.clear();
                    searchResultAdapter.notifyDataSetChanged();
                    showSearchResults();
                });
                return;
            }
            
            // 使用WeatherDataHelper加载每个搜索结果城市的天气数据
            com.microntek.weatherapp.util.WeatherDataHelper.loadCitiesWeather(
                    getApplicationContext(),
                    finalResults,
                    true, // 优先使用缓存
                    new com.microntek.weatherapp.util.WeatherDataHelper.WeatherDataCallback() {
                        @Override
                        public void onDataLoaded(List<City> updatedCities) {
                            // 更新搜索结果
                            searchResults.clear();
                            searchResults.addAll(updatedCities);
                            
                            // 执行UI更新
                            hideLoading();
                            searchResultAdapter.notifyDataSetChanged();
                            showSearchResults();
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            // 即使加载天气失败，也显示搜索结果
                            searchResults.clear();
                            searchResults.addAll(finalResults);
                            
                            hideLoading();
                            searchResultAdapter.notifyDataSetChanged();
                            showSearchResults();
                            
                            Log.e(TAG, "加载搜索城市天气失败: " + errorMessage);
                        }
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
        
        // 使用WeatherDataHelper加载城市天气数据
        com.microntek.weatherapp.util.WeatherDataHelper.loadCitiesWeather(
                getApplicationContext(),
                new ArrayList<>(cities),
                true, // 优先使用缓存
                new com.microntek.weatherapp.util.WeatherDataHelper.WeatherDataCallback() {
                    @Override
                    public void onDataLoaded(List<City> updatedCities) {
                        // 获取当前城市
                        currentCity = cityPreferences.getCurrentCity();
                        
                        // 对城市列表进行排序
                        List<City> sortedCities = com.microntek.weatherapp.util.WeatherDataHelper.sortCitiesList(
                                updatedCities, currentCity);
                        
                        // 更新UI
                        cities.clear();
                        cities.addAll(sortedCities);
                        hideLoading();
                        cityListAdapter.notifyDataSetChanged();
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        hideLoading();
                        Log.e(TAG, "加载城市天气失败: " + errorMessage);
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "加载城市天气失败: " + errorMessage,
                                Snackbar.LENGTH_LONG)
                                .setAction("重试", v -> loadCitiesWeather())
                                .show();
                    }
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
        final String taskId = "ADD_CITY_" + city.getId();
        if (!com.microntek.weatherapp.util.TaskManager.canExecuteTask(taskId)) {
            return;
        }
        
        // 检查城市数量限制
        if (cityPreferences.getSavedCities().size() >= 5) {
            Toast.makeText(this, "最多只能添加5个城市", Toast.LENGTH_SHORT).show();
            com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
            return;
        }
        
        // 显示加载状态
        showLoading();
        
        com.microntek.weatherapp.util.ExecutorManager.executeSingle(() -> {
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
                    com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "该城市已添加", Toast.LENGTH_SHORT).show();
                        com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
                    });
                    return;
                }
                
                // 检查是否是第一个城市
                boolean isFirstCity = cityPreferences.getSavedCities().isEmpty();
                
                // 添加城市到偏好设置（异步预加载城市数据）
                boolean success = cityPreferences.addCity(city);
                
                if (!success) {
                    com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "添加城市失败", Toast.LENGTH_SHORT).show();
                        com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
                    });
                    return;
                }
                
                // 如果是第一个城市，自动设置为当前城市
                if (isFirstCity) {
                    cityPreferences.setCurrentCity(city);
                    currentCity = city;
                }
                
                // 获取并排序城市列表
                List<City> allCities = cityPreferences.getSavedCities();
                List<City> sortedCities = com.microntek.weatherapp.util.WeatherDataHelper.sortCitiesList(
                        allCities, cityPreferences.getCurrentCity());
                
                com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    
                    cities.clear();
                    cities.addAll(sortedCities);
                    
                    // 切换回城市列表界面
                    hideSearchResult();
                    
                    // 清空搜索框
                    searchEditText.setText("");
                    
                    // 刷新适配器
                    cityListAdapter.notifyDataSetChanged();
                    
                    // 根据是否是第一个城市显示不同的提示
                    if (isFirstCity) {
                        Snackbar.make(
                                findViewById(android.R.id.content), 
                                "已添加城市: " + city.getName() + "，并设为当前城市", 
                                Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(
                                findViewById(android.R.id.content), 
                                "已添加城市: " + city.getName(), 
                                Snackbar.LENGTH_LONG).show();
                    }
                    
                    com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
                });
            } catch (Exception e) {
                Log.e(TAG, "添加城市时出错: " + e.getMessage());
                com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "添加城市失败: " + e.getMessage(), 
                            Snackbar.LENGTH_LONG)
                            .setAction("重试", v -> addCity(city))
                            .show();
                    com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
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
        final String taskId = "DELETE_CITY_" + city.getId();
        if (!com.microntek.weatherapp.util.TaskManager.canExecuteTask(taskId)) {
            return;
        }
        
        // 显示加载状态
        showLoading();
        
        com.microntek.weatherapp.util.ExecutorManager.executeSingle(() -> {
            try {
                // 如果是当前城市，不能删除
                if (currentCity != null && city.getId().equals(currentCity.getId())) {
                    com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "不能删除当前选中的城市", Toast.LENGTH_SHORT).show();
                        com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
                    });
                    return;
                }
                
                // 从偏好设置中删除城市（同时会清除相关缓存）
                boolean success = cityPreferences.removeCity(city);
                
                if (!success) {
                    com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "删除城市失败", Toast.LENGTH_SHORT).show();
                        com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
                    });
                    return;
                }
                
                // 刷新城市列表
                List<City> updatedCities = cityPreferences.getSavedCities();
                
                // 获取当前城市（可能已经更新）
                currentCity = cityPreferences.getCurrentCity();
                
                // 对城市列表进行排序
                List<City> sortedCities = com.microntek.weatherapp.util.WeatherDataHelper.sortCitiesList(
                        updatedCities, currentCity);
                
                com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    
                    cities.clear();
                    cities.addAll(sortedCities);
                    
                    cityListAdapter.notifyDataSetChanged();
                    
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "已删除城市: " + city.getName(), 
                            Snackbar.LENGTH_LONG).show();
                    
                    com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
                });
            } catch (Exception e) {
                Log.e(TAG, "删除城市时出错: " + e.getMessage());
                com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "删除城市失败: " + e.getMessage(), 
                            Snackbar.LENGTH_LONG)
                            .setAction("重试", v -> deleteCity(city))
                            .show();
                    com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
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
        final String taskId = "SWITCH_CITY_" + city.getId();
        if (!com.microntek.weatherapp.util.TaskManager.canExecuteTask(taskId)) {
            return;
        }
        
        // 显示加载状态
        showLoading();
        
        com.microntek.weatherapp.util.ExecutorManager.executeSingle(() -> {
            try {
                // 检查是否已经是当前城市
                City current = cityPreferences.getCurrentCity();
                if (current != null && current.getId().equals(city.getId())) {
                    com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                city.getName() + " 已是当前城市", Toast.LENGTH_SHORT).show();
                        com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
                    });
                    return;
                }
                
                // 设置当前城市（同时会预加载天气数据）
                boolean success = cityPreferences.setCurrentCity(city);
                
                if (!success) {
                    com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                        hideLoading();
                        Toast.makeText(CityManagerActivity.this, 
                                "切换城市失败", Toast.LENGTH_SHORT).show();
                        com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
                    });
                    return;
                }
                
                currentCity = city;
                
                // 获取并排序城市列表
                List<City> allCities = cityPreferences.getSavedCities();
                List<City> sortedCities = com.microntek.weatherapp.util.WeatherDataHelper.sortCitiesList(
                        allCities, city);
                
                com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    
                    cities.clear();
                    cities.addAll(sortedCities);
                    cityListAdapter.notifyDataSetChanged();
                    
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "已切换到城市: " + city.getName(), 
                            Snackbar.LENGTH_SHORT).show();
                    
                    // 延迟500ms，让用户看到切换成功的提示
                    com.microntek.weatherapp.util.ExecutorManager.executeOnMainDelayed(() -> {
                        // 返回主页并设置标志位
                        Intent intent = new Intent(CityManagerActivity.this, MainActivity.class);
                        intent.putExtra("fromOtherActivity", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                        
                        com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
                    }, 500);
                });
            } catch (Exception e) {
                Log.e(TAG, "切换城市时出错: " + e.getMessage());
                com.microntek.weatherapp.util.ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    Snackbar.make(
                            findViewById(android.R.id.content), 
                            "切换城市失败: " + e.getMessage(), 
                            Snackbar.LENGTH_LONG)
                            .setAction("重试", v -> switchCurrentCity(city))
                            .show();
                    com.microntek.weatherapp.util.TaskManager.completeTask(taskId);
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
        
        // 释放位置工具类资源
        if (locationHelper != null) {
            locationHelper.onDestroy();
            locationHelper = null;
        }
        
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // 检查当前是否在搜索模式
        if (isSearchMode) {
            // 如果在搜索模式，先返回到城市列表
            hideSearchResult();
            searchEditText.setText("");
        } else {
            // 检查是否有保存的城市
            if (cityPreferences.getSavedCities().isEmpty()) {
                // 如果没有城市，直接退出应用
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                // 有城市，正常处理返回行为
                super.onBackPressed();
            }
        }
    }
} 
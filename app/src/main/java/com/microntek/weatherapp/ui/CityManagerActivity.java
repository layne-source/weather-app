package com.microntek.weatherapp.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.microntek.weatherapp.MainActivity;
import com.microntek.weatherapp.R;
import com.microntek.weatherapp.adapter.CityAdapter;
import com.microntek.weatherapp.api.WeatherApi;
import com.microntek.weatherapp.model.City;
import com.microntek.weatherapp.util.CityOperationHelper;
import com.microntek.weatherapp.util.CityPreferences;
import com.microntek.weatherapp.util.WeatherDataHelper;
import com.microntek.weatherapp.util.ExecutorManager;
import com.microntek.weatherapp.util.MessageManager;
import com.microntek.weatherapp.util.TaskManager;
import com.microntek.weatherapp.util.LocationHelper;
import com.microntek.weatherapp.util.LocationHelper.LocationCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CityManagerActivity extends AppCompatActivity implements 
        BottomNavigationView.OnNavigationItemSelectedListener, 
        CityAdapter.CityClickListener,
        CityAdapter.CityDeleteListener {
    
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
    
    // 移除原有的适配器，使用新的统一适配器
    // private CityListAdapter cityListAdapter;
    // private List<City> searchResults = new ArrayList<>();
    // private SearchResultAdapter searchResultAdapter;
    private CityAdapter cityAdapter;
    
    // 添加城市操作辅助类
    private CityOperationHelper cityOperationHelper;
    
    // 添加位置工具类
    private LocationHelper locationHelper;
    
    // 搜索模式标志 - 移动到适配器中管理
    // private boolean isSearchMode = false;
    
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
        
        // 初始化城市操作辅助类
        cityOperationHelper = new CityOperationHelper(this, findViewById(android.R.id.content));
        
        // 初始化位置工具类
        locationHelper = new LocationHelper(this, new LocationCallback() {
            @Override
            public void onLocationSuccess(double latitude, double longitude) {
                // 使用城市操作辅助类处理位置结果
                showLoading();
                cityOperationHelper.processLocationResult(latitude, longitude, 
                        new CityOperationHelper.OperationCallback<City>() {
                            @Override
                            public void onSuccess(City city) {
                                hideLoading();
                                MessageManager.showSuccess(CityManagerActivity.this, 
                                        getString(R.string.location_successful));
                                addCity(city);
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                hideLoading();
                                MessageManager.showError(CityManagerActivity.this, 
                                        "获取城市信息失败: " + errorMessage);
                            }
                        });
            }

            @Override
            public void onLocationFailed(String error) {
                hideLoading();
                MessageManager.showError(CityManagerActivity.this, 
                        getString(R.string.location_failed) + ": " + error);
            }
        });
        
        // 初始化适配器
        cityAdapter = new CityAdapter(this);
        cityAdapter.setCityClickListener(this);
        cityAdapter.setCityDeleteListener(this);
        
        // 设置城市列表
        setupCityList();
        
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
        // 设置更现代美观的定位图标
        locationButton.setImageResource(R.drawable.ic_my_location);
        
        locationButton.setOnClickListener(v -> {
            // 检查并请求位置权限
            if (!locationHelper.hasLocationPermission()) {
                locationHelper.requestLocationPermission(this);
                return;
            }
            
            // 检查位置服务是否开启
            if (!locationHelper.isLocationEnabled()) {
                MessageManager.showActionMessage(findViewById(android.R.id.content), 
                        "位置服务未开启，请开启后再试", 
                        "设置", view -> locationHelper.openLocationSettings(this));
                return;
            }
            
            // 显示正在定位提示
            MessageManager.showMessage(this, getString(R.string.locating));
            showLoading();
            
            // 开始定位
            locationHelper.getCurrentLocation();
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
                    MessageManager.showMessage(this, getString(R.string.locating));
                    showLoading();
                    locationHelper.getCurrentLocation();
                } else {
                    MessageManager.showActionMessage(findViewById(android.R.id.content), 
                            "位置服务未开启，请开启后再试", 
                            "设置", view -> locationHelper.openLocationSettings(this));
                }
            } else {
                // 权限被拒绝
                MessageManager.showError(this, getString(R.string.location_permission_required));
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
        cities.addAll(savedCities);
        
        // 获取当前城市
        currentCity = cityPreferences.getCurrentCity();
        
        // 重新排序城市列表，将当前城市排在第一位
        List<City> sortedCities = WeatherDataHelper.sortCitiesList(savedCities, currentCity);
        
        // 设置适配器数据
        cityAdapter.setCities(sortedCities);
        cityAdapter.setCurrentCity(currentCity);
        cityAdapter.setSearchMode(false);
        
        // 设置RecyclerView的适配器
        recyclerView.setAdapter(cityAdapter);
        
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
        cityAdapter.setSearchMode(true);
        showLoading();

        // 使用TaskManager执行搜索任务
        final String taskId = "SEARCH_CITY_" + cityName;
        TaskManager.executeParallelTask(taskId, () -> {
            List<City> results = new ArrayList<>();
            try {
                results = WeatherApi.searchCity(cityName);
            } catch (Exception e) {
                e.printStackTrace();
                ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    MessageManager.showActionMessage(
                            findViewById(android.R.id.content),
                            "搜索城市失败: " + e.getMessage(),
                            "重试",
                            v -> searchCity(cityName));
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
                ExecutorManager.executeOnMain(() -> {
                    hideLoading();
                    // 更新搜索结果
                    cityAdapter.setSearchResults(new ArrayList<>());
                    showSearchResults();
                });
                return;
            }
            
            // 使用WeatherDataHelper加载每个搜索结果城市的天气数据
            WeatherDataHelper.loadCitiesWeather(
                    getApplicationContext(),
                    finalResults,
                    true, // 优先使用缓存
                    new WeatherDataHelper.WeatherDataCallback() {
                        @Override
                        public void onDataLoaded(List<City> updatedCities) {
                            // 更新搜索结果
                            cityAdapter.setSearchResults(updatedCities);
                            
                            // 执行UI更新
                            hideLoading();
                            showSearchResults();
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            // 即使加载天气失败，也显示搜索结果
                            cityAdapter.setSearchResults(finalResults);
                            
                            hideLoading();
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
        recyclerView.setAdapter(cityAdapter);
        
        // 根据搜索结果显示或隐藏无结果提示
        if (cityAdapter.getItemCount() == 0) {
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
        cityAdapter.setSearchMode(false);
        
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
        
        showLoading();
        
        // 使用城市操作辅助类加载城市天气数据
        cityOperationHelper.loadCitiesWeather(cities, 
                new CityOperationHelper.OperationCallback<List<City>>() {
                    @Override
                    public void onSuccess(List<City> sortedCities) {
                        cities.clear();
                        cities.addAll(sortedCities);
                        currentCity = cityPreferences.getCurrentCity();
                        
                        // 更新适配器数据
                        cityAdapter.setCities(sortedCities);
                        cityAdapter.setCurrentCity(currentCity);
                        
                        hideLoading();
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        hideLoading();
                        Log.e(TAG, "加载城市天气失败: " + errorMessage);
                        MessageManager.showActionMessage(
                                findViewById(android.R.id.content),
                                "加载城市天气失败: " + errorMessage,
                                "重试", 
                                v -> loadCitiesWeather());
                    }
                });
    }
    
    /**
     * 添加城市
     */
    private void addCity(City city) {
        showLoading();
        
        cityOperationHelper.addCity(city, 
                new CityOperationHelper.OperationCallback<List<City>>() {
                    @Override
                    public void onSuccess(List<City> sortedCities) {
                        cities.clear();
                        cities.addAll(sortedCities);
                        currentCity = cityPreferences.getCurrentCity();
                        
                        // 更新适配器数据
                        cityAdapter.setCities(sortedCities);
                        cityAdapter.setCurrentCity(currentCity);
                        
                        // 如果在搜索模式，切换回城市列表
                        if (cityAdapter.isSearchMode()) {
                            hideSearchResult();
                            // 清空搜索框
                            searchEditText.setText("");
                        }
                        
                        hideLoading();
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        hideLoading();
                        // 错误处理已在cityOperationHelper内部完成
                    }
                });
    }
    
    /**
     * 城市点击事件实现（CityAdapter.CityClickListener接口）
     */
    @Override
    public void onCityClick(City city, boolean isSavedCity) {
        if (isSavedCity) {
            // 点击已保存的城市，切换为当前城市
            showLoading();
            cityOperationHelper.switchCurrentCity(city, true, 
                    new CityOperationHelper.OperationCallback<List<City>>() {
                        @Override
                        public void onSuccess(List<City> sortedCities) {
                            hideLoading();
                            
                            // 延迟500ms返回主页
                            ExecutorManager.executeOnMainDelayed(() -> {
                                Intent intent = new Intent(CityManagerActivity.this, MainActivity.class);
                                intent.putExtra("fromOtherActivity", true);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            }, 500);
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            hideLoading();
                            // 错误处理已在cityOperationHelper内部完成
                        }
                    });
        } else {
            // 添加城市
            addCity(city);
        }
    }
    
    /**
     * 城市删除事件实现（CityAdapter.CityDeleteListener接口）
     */
    @Override
    public void onCityDelete(City city, int position) {
        showLoading();
        cityOperationHelper.deleteCity(city, 
                new CityOperationHelper.OperationCallback<List<City>>() {
                    @Override
                    public void onSuccess(List<City> sortedCities) {
                        cities.clear();
                        cities.addAll(sortedCities);
                        currentCity = cityPreferences.getCurrentCity();
                        
                        // 更新适配器数据
                        cityAdapter.setCities(sortedCities);
                        cityAdapter.setCurrentCity(currentCity);
                        
                        hideLoading();
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        hideLoading();
                        // 错误处理已在cityOperationHelper内部完成
                    }
                });
    }
    
    /**
     * 显示加载中状态
     */
    private void showLoading() {
        // 如果布局中有loading_view，则显示
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        } else {
            // 使用MessageManager作为备选方案
            MessageManager.showMessage(this, "正在搜索...");
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
        if (cityAdapter.isSearchMode()) {
            showSearchResults();
        } else {
            // 确保城市列表显示
            if (recyclerView.getAdapter() != cityAdapter) {
                recyclerView.setAdapter(cityAdapter);
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
        if (cityAdapter.isSearchMode()) {
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
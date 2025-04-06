package com.microntek.weatherapp.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 位置工具类，用于获取当前设备位置
 */
public class LocationHelper {
    private static final String TAG = "LocationHelper";
    public static final int REQUEST_LOCATION_PERMISSION = 1001;
    
    private final Context context;
    private final LocationCallback callback;
    private LocationManager locationManager;
    private LocationListener locationListener;
    
    /**
     * 位置回调接口
     */
    public interface LocationCallback {
        void onLocationSuccess(double latitude, double longitude);
        void onLocationFailed(String error);
    }
    
    public LocationHelper(Context context, LocationCallback callback) {
        this.context = context;
        this.callback = callback;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    
    /**
     * 检查是否有位置权限
     */
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == 
                PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 请求位置权限
     */
    public void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION_PERMISSION);
    }
    
    /**
     * 检查位置服务是否开启
     */
    public boolean isLocationEnabled() {
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "GPS provider check error: " + e.getMessage());
        }
        
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "Network provider check error: " + e.getMessage());
        }
        
        return gpsEnabled || networkEnabled;
    }
    
    /**
     * 打开位置设置
     */
    public void openLocationSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivity(intent);
    }
    
    /**
     * 获取当前位置
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation() {
        if (!hasLocationPermission()) {
            callback.onLocationFailed("缺少位置权限");
            return;
        }
        
        if (!isLocationEnabled()) {
            callback.onLocationFailed("位置服务未开启");
            return;
        }
        
        try {
            // 创建位置监听器
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    // 获取到位置后，停止监听并回调
                    stopLocationUpdates();
                    callback.onLocationSuccess(location.getLatitude(), location.getLongitude());
                }
                
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // 不需要实现
                }
                
                @Override
                public void onProviderEnabled(@NonNull String provider) {
                    // 不需要实现
                }
                
                @Override
                public void onProviderDisabled(@NonNull String provider) {
                    // 不需要实现
                }
            };
            
            // 先尝试获取最后已知位置
            Location lastGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            // 使用最近的位置
            Location bestLocation = null;
            if (lastGpsLocation != null && lastNetworkLocation != null) {
                bestLocation = lastGpsLocation.getTime() > lastNetworkLocation.getTime() ? lastGpsLocation : lastNetworkLocation;
            } else {
                bestLocation = lastGpsLocation != null ? lastGpsLocation : lastNetworkLocation;
            }
            
            if (bestLocation != null && System.currentTimeMillis() - bestLocation.getTime() < 5 * 60 * 1000) {
                // 如果有比较新的位置信息（5分钟内），直接使用
                stopLocationUpdates();
                callback.onLocationSuccess(bestLocation.getLatitude(), bestLocation.getLongitude());
                return;
            }
            
            // 否则请求位置更新
            boolean requestedUpdates = false;
            
            // 先尝试使用GPS获取精确位置
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000,
                        10,
                        locationListener);
                requestedUpdates = true;
            }
            
            // 同时使用网络位置作为备选
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000,
                        10,
                        locationListener);
                requestedUpdates = true;
            }
            
            if (!requestedUpdates) {
                // 如果没有可用的位置提供者
                stopLocationUpdates();
                callback.onLocationFailed("无可用的位置提供者");
            }
            
            // 设置超时处理，15秒后如果还没有获取到位置则超时
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (locationListener != null) {
                        // 位置监听器仍然存在，说明还没有获取到位置
                        stopLocationUpdates();
                        callback.onLocationFailed("位置获取超时");
                    }
                }
            }, 15000);
            
        } catch (Exception e) {
            Log.e(TAG, "获取位置出错: " + e.getMessage());
            callback.onLocationFailed("位置获取出错: " + e.getMessage());
        }
    }
    
    /**
     * 停止位置更新
     */
    public void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null;
        }
    }
    
    /**
     * 清理资源，在Activity销毁时调用
     */
    public void onDestroy() {
        stopLocationUpdates();
        locationManager = null;
    }
} 
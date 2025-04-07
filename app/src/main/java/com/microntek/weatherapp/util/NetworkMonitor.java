package com.microntek.weatherapp.util;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * 网络状态监控工具类
 * 使用现代的NetworkCallback API监控网络状态变化
 */
public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";
    
    // 网络恢复广播Action
    public static final String ACTION_NETWORK_RESTORED = "com.microntek.weatherapp.NETWORK_RESTORED";
    
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final NetworkCallback networkCallback;
    private boolean isNetworkAvailable = false;
    private NetworkMonitorCallback callback;
    
    // 单例模式
    private static NetworkMonitor instance;
    
    /**
     * 获取NetworkMonitor实例
     */
    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 网络状态回调接口
     */
    public interface NetworkMonitorCallback {
        /**
         * 网络连接恢复时调用
         */
        void onNetworkRestored();
    }
    
    // 私有构造方法，支持单例模式
    private NetworkMonitor(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.networkCallback = new NetworkCallback();
        
        // 初始化时检查一次网络状态
        isNetworkAvailable = isNetworkAvailable();
        Log.i(TAG, "初始化网络监控，当前网络状态：" + (isNetworkAvailable ? "可用" : "不可用"));
    }
    
    /**
     * 设置回调
     */
    public void setCallback(NetworkMonitorCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 开始监听网络变化
     */
    public void startMonitoring() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
                Log.i(TAG, "已启动网络状态监控");
            } else {
                // 对于旧版Android，不使用NetworkCallback
                Log.i(TAG, "当前Android版本不支持NetworkCallback，使用备用检测方法");
            }
        } catch (Exception e) {
            Log.e(TAG, "注册网络回调失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止监听网络变化
     */
    public void stopMonitoring() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.i(TAG, "已停止网络状态监控");
            }
        } catch (Exception e) {
            Log.e(TAG, "注销网络回调失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查当前网络是否可用
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(
                    connectivityManager.getActiveNetwork());
            
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // 兼容老版本
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
    
    /**
     * 网络回调内部类
     */
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            Log.i(TAG, "网络连接已恢复");
            
            // 只有从无网络状态恢复才触发回调
            if (!isNetworkAvailable) {
                isNetworkAvailable = true;
                
                // 发送网络恢复广播
                Intent intent = new Intent(ACTION_NETWORK_RESTORED);
                context.sendBroadcast(intent);
                
                // 回调通知
                ExecutorManager.executeOnMain(() -> {
                    if (callback != null) {
                        callback.onNetworkRestored();
                    }
                });
            }
        }
        
        @Override
        public void onLost(Network network) {
            Log.i(TAG, "网络连接已断开");
            isNetworkAvailable = false;
        }
    }
} 
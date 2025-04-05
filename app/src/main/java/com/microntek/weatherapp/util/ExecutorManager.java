package com.microntek.weatherapp.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 统一的线程执行器管理类
 * 用于管理应用中所有的异步任务执行
 */
public class ExecutorManager {
    private static final String TAG = "ExecutorManager";
    
    // 单线程执行器，用于顺序执行任务
    private static ExecutorService singleThreadExecutor;
    
    // 有限线程池，用于并行执行任务
    private static ExecutorService threadPoolExecutor;
    
    // 主线程Handler
    private static Handler mainHandler;
    
    /**
     * 获取单线程执行器
     * 用于需要顺序执行的任务，比如文件操作、数据库操作等
     */
    public static synchronized ExecutorService getSingleThreadExecutor() {
        if (singleThreadExecutor == null || singleThreadExecutor.isShutdown()) {
            singleThreadExecutor = Executors.newSingleThreadExecutor();
        }
        return singleThreadExecutor;
    }
    
    /**
     * 获取线程池执行器
     * 用于可以并行执行的任务，比如网络请求、图片加载等
     */
    public static synchronized ExecutorService getThreadPoolExecutor() {
        if (threadPoolExecutor == null || threadPoolExecutor.isShutdown()) {
            // 使用固定大小的线程池，避免无限增长
            threadPoolExecutor = Executors.newFixedThreadPool(3);
        }
        return threadPoolExecutor;
    }
    
    /**
     * 获取主线程Handler
     */
    public static synchronized Handler getMainHandler() {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        return mainHandler;
    }
    
    /**
     * 在单线程执行器上执行任务
     */
    public static void executeSingle(Runnable task) {
        getSingleThreadExecutor().execute(task);
    }
    
    /**
     * 在线程池上执行任务
     */
    public static void executeParallel(Runnable task) {
        getThreadPoolExecutor().execute(task);
    }
    
    /**
     * 在主线程上执行任务
     */
    public static void executeOnMain(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            getMainHandler().post(task);
        }
    }
    
    /**
     * 在主线程上延迟执行任务
     */
    public static void executeOnMainDelayed(Runnable task, long delayMillis) {
        getMainHandler().postDelayed(task, delayMillis);
    }
    
    /**
     * 关闭所有执行器
     * 应在应用退出时调用
     */
    public static void shutdownAll() {
        shutdownExecutor(singleThreadExecutor, "单线程执行器");
        shutdownExecutor(threadPoolExecutor, "线程池执行器");
    }
    
    /**
     * 关闭指定执行器
     */
    private static void shutdownExecutor(ExecutorService executor, String name) {
        if (executor != null && !executor.isShutdown()) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                Log.i(TAG, name + "已关闭");
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Log.e(TAG, "关闭" + name + "时被中断", e);
                Thread.currentThread().interrupt();
            }
        }
    }
} 
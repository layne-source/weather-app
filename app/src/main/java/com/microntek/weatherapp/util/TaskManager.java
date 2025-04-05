package com.microntek.weatherapp.util;

import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务状态管理类
 * 用于跟踪和管理异步任务的执行状态，防止重复执行
 */
public class TaskManager {
    private static final String TAG = "TaskManager";
    
    // 任务状态映射表
    private static final ConcurrentHashMap<String, AtomicBoolean> taskStates = new ConcurrentHashMap<>();
    
    /**
     * 检查是否可以执行指定任务
     * @param taskId 任务ID
     * @return 如果可以执行返回true，否则返回false
     */
    public static boolean canExecuteTask(String taskId) {
        AtomicBoolean state = taskStates.computeIfAbsent(taskId, k -> new AtomicBoolean(false));
        boolean canExecute = state.compareAndSet(false, true);
        if (!canExecute) {
            Log.d(TAG, "任务 " + taskId + " 已在执行中，忽略本次请求");
        }
        return canExecute;
    }
    
    /**
     * 标记任务完成
     * @param taskId 任务ID
     */
    public static void completeTask(String taskId) {
        AtomicBoolean state = taskStates.get(taskId);
        if (state != null) {
            state.set(false);
            Log.d(TAG, "任务 " + taskId + " 已完成");
        }
    }
    
    /**
     * 执行任务，如果任务已在执行则跳过
     * @param taskId 任务ID
     * @param task 要执行的任务
     */
    public static void executeTask(String taskId, Runnable task) {
        if (canExecuteTask(taskId)) {
            try {
                ExecutorManager.executeSingle(() -> {
                    try {
                        task.run();
                    } finally {
                        completeTask(taskId);
                    }
                });
            } catch (Exception e) {
                completeTask(taskId);
                Log.e(TAG, "执行任务 " + taskId + " 时出错", e);
            }
        }
    }
    
    /**
     * 执行并行任务，如果任务已在执行则跳过
     * @param taskId 任务ID
     * @param task 要执行的任务
     */
    public static void executeParallelTask(String taskId, Runnable task) {
        if (canExecuteTask(taskId)) {
            try {
                ExecutorManager.executeParallel(() -> {
                    try {
                        task.run();
                    } finally {
                        completeTask(taskId);
                    }
                });
            } catch (Exception e) {
                completeTask(taskId);
                Log.e(TAG, "执行并行任务 " + taskId + " 时出错", e);
            }
        }
    }
    
    /**
     * 清除所有任务状态
     * 应在应用退出时调用
     */
    public static void clearAll() {
        taskStates.clear();
    }
} 
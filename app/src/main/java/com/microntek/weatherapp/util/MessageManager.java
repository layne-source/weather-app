package com.microntek.weatherapp.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.microntek.weatherapp.R;

/**
 * 统一消息管理类
 * 用于替代传统Toast，提供更好的用户体验
 * 解决多个Toast消息队列等待的问题
 */
public class MessageManager {
    
    private static Toast currentToast;
    private static Snackbar currentSnackbar;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable pendingMessage;
    
    /**
     * 显示短消息（替代Toast.LENGTH_SHORT）
     */
    public static void showMessage(Context context, String message) {
        showToast(context, message, Toast.LENGTH_SHORT);
    }
    
    /**
     * 显示长消息（替代Toast.LENGTH_LONG）
     */
    public static void showLongMessage(Context context, String message) {
        showToast(context, message, Toast.LENGTH_LONG);
    }
    
    /**
     * 显示带有操作按钮的消息
     */
    public static void showActionMessage(View rootView, String message, 
                                        String actionText, View.OnClickListener action) {
        cancelAll();
        
        currentSnackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        currentSnackbar.setAction(actionText, action);
        currentSnackbar.show();
    }
    
    /**
     * 显示错误消息
     */
    public static void showError(Context context, String errorMessage) {
        if (context == null) return;
        
        cancelAll();
        
        // 使用自定义视图的Toast提供更醒目的错误提示
        Toast toast = new Toast(context);
        
        View view = LayoutInflater.from(context).inflate(R.layout.custom_error_toast, null);
        TextView textView = view.findViewById(R.id.toast_text);
        textView.setText(errorMessage);
        
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150);
        
        currentToast = toast;
        toast.show();
    }
    
    /**
     * 显示成功消息
     */
    public static void showSuccess(Context context, String message) {
        if (context == null) return;
        
        cancelAll();
        
        // 使用自定义视图的Toast提供更醒目的成功提示
        Toast toast = new Toast(context);
        
        View view = LayoutInflater.from(context).inflate(R.layout.custom_success_toast, null);
        TextView textView = view.findViewById(R.id.toast_text);
        textView.setText(message);
        
        toast.setView(view);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150);
        
        currentToast = toast;
        toast.show();
    }
    
    /**
     * 内部方法：显示标准Toast
     */
    private static void showToast(Context context, String message, int duration) {
        if (context == null) return;
        
        cancelAll();
        
        currentToast = Toast.makeText(context, message, duration);
        currentToast.show();
    }
    
    /**
     * 延迟显示消息，如果在延迟期间有新消息，之前的消息将被取消
     */
    public static void showMessageDelayed(final Context context, final String message, long delayMillis) {
        if (context == null) return;
        
        cancelPendingMessages();
        
        pendingMessage = () -> showMessage(context, message);
        handler.postDelayed(pendingMessage, delayMillis);
    }
    
    /**
     * 取消所有显示和待显示的消息
     */
    public static void cancelAll() {
        cancelToast();
        cancelSnackbar();
        cancelPendingMessages();
    }
    
    /**
     * 取消当前显示的Toast
     */
    public static void cancelToast() {
        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
    }
    
    /**
     * 取消当前显示的Snackbar
     */
    public static void cancelSnackbar() {
        if (currentSnackbar != null && currentSnackbar.isShown()) {
            currentSnackbar.dismiss();
            currentSnackbar = null;
        }
    }
    
    /**
     * 取消待显示的消息
     */
    public static void cancelPendingMessages() {
        if (pendingMessage != null) {
            handler.removeCallbacks(pendingMessage);
            pendingMessage = null;
        }
    }
} 
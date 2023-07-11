package net.kaaass.zerotierfix.util;

import android.util.Log;

import net.kaaass.zerotierfix.BuildConfig;

/**
 * 调试日志相关的工具函数，用于控制部分日志仅在调试模式下输出
 */
public class DebugLog {

    public static void d(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void i(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message);
        }
    }
}

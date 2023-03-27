package net.kaaass.zerotierfix.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

/**
 * 网络连接信息处理工具类
 */
public class NetworkInfoUtils {
    public enum CurrentConnection {
        CONNECTION_NONE,
        CONNECTION_MOBILE,
        CONNECTION_OTHER
    }

    public static CurrentConnection getNetworkInfoCurrentConnection(Context context) {
        var connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return CurrentConnection.CONNECTION_NONE;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            var networkCapabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (networkCapabilities == null) {
                return CurrentConnection.CONNECTION_NONE;
            }
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return CurrentConnection.CONNECTION_MOBILE;
            }
            return CurrentConnection.CONNECTION_OTHER;
        } else {
            var activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo == null) {
                return CurrentConnection.CONNECTION_NONE;
            }
            if (!activeNetworkInfo.isConnectedOrConnecting()) {
                return CurrentConnection.CONNECTION_NONE;
            }
            if (activeNetworkInfo.getType() == 0) {
                return CurrentConnection.CONNECTION_MOBILE;
            }
            return CurrentConnection.CONNECTION_OTHER;
        }
    }
}

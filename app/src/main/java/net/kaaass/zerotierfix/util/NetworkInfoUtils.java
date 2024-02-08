package net.kaaass.zerotierfix.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 网络连接信息处理工具类
 */
public class NetworkInfoUtils {
    public static final String TAG = "NetworkInfoUtils";

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

    public static List<String> listMulticastGroupOnInterface(String interfaceName, boolean isIpv6) {
        var groups = new ArrayList<String>();

        String igmpFilePath;

        if (isIpv6) {
            igmpFilePath = "/proc/net/igmp6";
        } else {
            igmpFilePath = "/proc/net/igmp";
        }

        /*
         * 从 /proc/net/igmp 或 /proc/net/igmp6 的信息格式大致为:
         *
         * Idx     Device    : Count Querier       Group    Users Timer    Reporter
         * 1       tun0      :     2      V3
         *                                 010000E0     1 0:00000000               0
         * 2       wlan0     :     1      V3
         *                                 010000E0     1 0:00000000               0
         *
         * 因此，解析时需要先找到目标 interface 的行，然后开始读取若干行的组播组信息。
         */
        try (var igmpInfo = new BufferedReader(new FileReader(igmpFilePath))) {
            boolean foundTargetInterface = false;
            String line;

            while ((line = igmpInfo.readLine()) != null) {
                var row = line.split("\\s+", -1);

                if (!foundTargetInterface) {
                    if (row.length > 1 && row[1].equals(interfaceName)) {
                        // 找到目标 interface 的行
                        foundTargetInterface = true;
                    }
                } else {
                    if (row[0].equals("")) {
                        groups.add(row[1]);
                    } else {
                        // 目标 interface 的信息结束
                        foundTargetInterface = false;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "IGMP info file not found", e);
        } catch (IOException e) {
            Log.e(TAG, "Error reading IGMP info", e);
        }
        return groups;
    }
}

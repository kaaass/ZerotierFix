package net.kaaass.zerotierfix.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import net.kaaass.zerotierfix.util.Constants;

// TODO: clear up
public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received: " + intent.getAction() + ". Starting ZeroTier One service.");
        var pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (pref.getBoolean(Constants.PREF_GENERAL_START_ZEROTIER_ON_BOOT, true)) {
            Log.i(TAG, "Preferences set to start ZeroTier on boot");
        } else {
            Log.i(TAG, "Preferences set to not start ZeroTier on boot");
        }
    }
}

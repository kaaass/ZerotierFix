package net.kaaass.zerotierfix.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

// TODO: clear up
public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received: " + intent.getAction() + ". Starting ZeroTier One service.");
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("general_start_zerotier_on_boot", true)) {
            Log.i(TAG, "Preferences set to start ZeroTier on boot");
        } else {
            Log.i(TAG, "Preferences set to not start ZeroTier on boot");
        }
    }
}

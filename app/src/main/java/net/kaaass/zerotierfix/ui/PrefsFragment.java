package net.kaaass.zerotierfix.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceFragmentCompat;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.service.ZeroTierOneService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PrefsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int OPEN_PLANET_DATA = 42;
    private static final String TAG = "PreferencesFragment";

    @Override // androidx.preference.PreferenceFragmentCompat, androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // androidx.preference.PreferenceFragmentCompat
    public void onCreatePreferences(Bundle bundle, String str) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        if (str.equals("network_use_cellular_data") && sharedPreferences.getBoolean("network_use_cellular_data", false)) {
            getActivity().startService(new Intent(getActivity(), ZeroTierOneService.class));
        }
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 42 && i2 == -1 && intent != null) {
            Uri data = intent.getData();
            if (data == null) {
                Log.e(TAG, "Invalid URI");
                return;
            }
            File file = new File(getActivity().getFilesDir(), "planet");
            try {
                InputStream openInputStream = getActivity().getContentResolver().openInputStream(data);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                byte[] bArr = new byte[1024];
                while (true) {
                    int read = openInputStream.read(bArr);
                    if (read > 0) {
                        fileOutputStream.write(bArr, 0, read);
                    } else {
                        openInputStream.close();
                        fileOutputStream.close();
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}

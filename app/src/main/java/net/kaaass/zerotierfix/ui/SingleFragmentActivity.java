package net.kaaass.zerotierfix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.kaaass.zerotierfix.R;

/**
 * 单片段 Activity
 */
public abstract class SingleFragmentActivity extends AppCompatActivity {
    private static final String TAG = "SingleFragmentActivity";

    public abstract Fragment createFragment();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_fragment);
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment findFragmentById = supportFragmentManager.findFragmentById(R.id.fragmentContainer);
        if (findFragmentById == null) {
            Fragment createFragment = createFragment();
            setArgs(createFragment);
            supportFragmentManager.beginTransaction().add(R.id.fragmentContainer, createFragment).commit();
            return;
        }
        setArgs(findFragmentById);
    }

    private void setArgs(Fragment fragment) {
        Bundle extras;
        Intent intent = getIntent();
        if (intent != null && (extras = intent.getExtras()) != null && !fragment.isAdded()) {
            try {
                fragment.setArguments(extras);
            } catch (IllegalArgumentException unused) {
                Log.e(TAG, "Exception setting arguments on fragment");
            }
        }
    }
}

package net.kaaass.zerotierfix.ui;

import androidx.fragment.app.Fragment;

public class PrefsActivity extends SingleFragmentActivity {
    @Override // com.zerotier.one.ui.SingleFragmentActivity
    public Fragment createFragment() {
        return new PrefsFragment();
    }
}

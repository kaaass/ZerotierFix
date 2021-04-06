package net.kaaass.zerotierfix.ui;

import androidx.fragment.app.Fragment;

/**
 * 入轨配置 fragment 的容器 activity
 */
public class MoonOrbitActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return new MoonOrbitFragment();
    }
}

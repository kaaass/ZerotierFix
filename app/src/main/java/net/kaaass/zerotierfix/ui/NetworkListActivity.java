package net.kaaass.zerotierfix.ui;

import androidx.fragment.app.Fragment;

/**
 * 网络列表 fragment 的容器 activity
 */
public class NetworkListActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return new NetworkListFragment();
    }
}

package net.kaaass.zerotierfix.ui.view;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import net.kaaass.zerotierfix.ui.SingleFragmentActivity;

/**
 * 网络信息 fragment 的容器 activity
 */
public class NetworkDetailActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return new NetworkDetailFragment();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // 添加返回按钮
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        // 返回上一界面
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

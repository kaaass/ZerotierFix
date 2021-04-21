package net.kaaass.zerotierfix.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import net.kaaass.zerotierfix.R;

/**
 * DNS 配置页适配器
 */
public class DNSPagerAdapter extends FragmentPagerAdapter {
    private final Context mContext;
    private final CustomDNSListener mDNSListener;

    public DNSPagerAdapter(Context context, FragmentManager fragmentManager, CustomDNSListener customDNSListener) {
        super(fragmentManager, BEHAVIOR_SET_USER_VISIBLE_HINT);
        this.mContext = context;
        this.mDNSListener = customDNSListener;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return NoDNSFragment.newInstance();
        }
        if (position == 1) {
            return NetworkDNSFragment.newInstance();
        }
        if (position != 2) {
            return null;
        }
        CustomDNSFragment newInstance = CustomDNSFragment.newInstance();
        newInstance.setDNSListener(this.mDNSListener);
        return newInstance;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            return this.mContext.getString(R.string.no_dns_tab_title);
        }
        if (position == 1) {
            return this.mContext.getString(R.string.network_dns_tab_title);
        }
        if (position != 2) {
            return null;
        }
        return this.mContext.getString(R.string.custom_dns_tab_title);
    }

    /**
     * 使用 Zerotier 网络配置 DNS
     */
    public static class NetworkDNSFragment extends Fragment {

        public static NetworkDNSFragment newInstance() {
            return new NetworkDNSFragment();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_network_d_n_s, container, false);
        }
    }

    /**
     * 不配置 DNS
     */
    public static class NoDNSFragment extends Fragment {
        public static NoDNSFragment newInstance() {
            NoDNSFragment noDNSFragment = new NoDNSFragment();
            noDNSFragment.setArguments(new Bundle());
            return noDNSFragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_no_d_n_s, container, false);
        }
    }
}

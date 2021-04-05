package net.kaaass.zerotierfix.ui;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import net.kaaass.zerotierfix.R;

public class DNSPagerAdapter extends FragmentPagerAdapter {
    private final Context mContext;
    private final CustomDNSListener mDNSListener;

    public DNSPagerAdapter(Context context, FragmentManager fragmentManager, CustomDNSListener customDNSListener) {
        super(fragmentManager);
        this.mContext = context;
        this.mDNSListener = customDNSListener;
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public int getCount() {
        return 3;
    }

    @Override // androidx.fragment.app.FragmentPagerAdapter
    public Fragment getItem(int i) {
        if (i == 0) {
            return NoDNSFragment.newInstance();
        }
        if (i == 1) {
            return NetworkDNSFragment.newInstance();
        }
        if (i != 2) {
            return null;
        }
        CustomDNSFragment newInstance = CustomDNSFragment.newInstance();
        newInstance.setDNSListener(this.mDNSListener);
        return newInstance;
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public CharSequence getPageTitle(int i) {
        if (i == 0) {
            return this.mContext.getString(R.string.no_dns_tab_title);
        }
        if (i == 1) {
            return this.mContext.getString(R.string.network_dns_tab_title);
        }
        if (i != 2) {
            return null;
        }
        return this.mContext.getString(R.string.custom_dns_tab_title);
    }
}

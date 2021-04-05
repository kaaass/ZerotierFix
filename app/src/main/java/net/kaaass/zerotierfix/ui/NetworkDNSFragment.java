package net.kaaass.zerotierfix.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import net.kaaass.zerotierfix.R;

public class NetworkDNSFragment extends Fragment {
    private String mParam1;
    private String mParam2;

    public static NetworkDNSFragment newInstance() {
        return new NetworkDNSFragment();
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.fragment_network_d_n_s, viewGroup, false);
    }
}

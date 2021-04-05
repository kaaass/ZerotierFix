package net.kaaass.zerotierfix.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

import net.kaaass.zerotierfix.R;

public class NoDNSFragment extends Fragment {
    public static NoDNSFragment newInstance() {
        NoDNSFragment noDNSFragment = new NoDNSFragment();
        noDNSFragment.setArguments(new Bundle());
        return noDNSFragment;
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getArguments();
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.fragment_no_d_n_s, viewGroup, false);
    }
}

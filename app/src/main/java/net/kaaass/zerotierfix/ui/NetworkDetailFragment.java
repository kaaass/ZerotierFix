package net.kaaass.zerotierfix.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.os.EnvironmentCompat;
import androidx.fragment.app.Fragment;

import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.events.DefaultRouteChangedEvent;
import net.kaaass.zerotierfix.model.AssignedAddress;
import net.kaaass.zerotierfix.model.DnsServer;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.model.NetworkConfig;
import net.kaaass.zerotierfix.model.NetworkDao;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.greendao.query.WhereCondition;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

// TODO: clean up
public class NetworkDetailFragment extends Fragment {
    private static final String TAG = "NetworkDetailView";
    private long mNetworkId = -1;

    public void onButtonPressed(Uri uri) {
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getArguments() != null) {
            this.mNetworkId = getArguments().getLong(NetworkListFragment.NETWORK_ID_MESSAGE);
        }
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        View inflate = layoutInflater.inflate(R.layout.fragment_network_detail, viewGroup, false);
        if (this.mNetworkId != -1) {
            List<Network> list = ((ZerotierFixApplication) getActivity().getApplication()).getDaoSession().getNetworkDao().queryBuilder().where(NetworkDao.Properties.NetworkId.eq(Long.valueOf(this.mNetworkId)), new WhereCondition[0]).build().forCurrentThread().list();
            if (list.size() > 1) {
                Log.e(TAG, "Data inconsistency error.  More than one network with a single ID!");
                return inflate;
            } else if (list.size() < 1) {
                Log.e(TAG, "Network not found!");
                return inflate;
            } else {
                Network network = list.get(0);
                if (network != null) {
                    ((TextView) inflate.findViewById(R.id.network_detail_network_id)).setText(network.getNetworkIdStr());
                    TextView textView = inflate.findViewById(R.id.network_detail_network_name);
                    if (network.getNetworkName() != null) {
                        textView.setText(network.getNetworkName());
                    } else {
                        textView.setText(EnvironmentCompat.MEDIA_UNKNOWN);
                    }
                    NetworkConfig networkConfig = network.getNetworkConfig();
                    CheckBox checkBox = inflate.findViewById(R.id.network_default_route);
                    checkBox.setChecked(network.getUseDefaultRoute());
                    if (networkConfig != null) {
                        TextView textView2 = inflate.findViewById(R.id.network_type_textview);
                        if (networkConfig.getType() != null) {
                            textView2.setText(networkConfig.getType().toStringId());
                        }
                        TextView textView3 = inflate.findViewById(R.id.network_status_textview);
                        if (networkConfig.getStatus() != null) {
                            textView3.setText(networkConfig.getStatus().toStringId());
                        }
                        TextView textView4 = inflate.findViewById(R.id.network_mac_textview);
                        if (networkConfig.getMac() != null) {
                            textView4.setText(networkConfig.getMac());
                        }
                        TextView textView5 = inflate.findViewById(R.id.network_mtu_textview);
                        if (networkConfig.getMtu() != null) {
                            textView5.setText(networkConfig.getMtu());
                        }
                        String str = getString(R.string.enabled);
                        ((TextView) inflate.findViewById(R.id.network_broadcast_textview)).setText(networkConfig.getBroadcast() ? str : getString(R.string.disabled));
                        TextView textView6 = inflate.findViewById(R.id.network_bridging_textview);
                        if (!networkConfig.getBridging()) {
                            str = getString(R.string.disabled);
                        }
                        textView6.setText(str);
                        List<AssignedAddress> assignedAddresses = networkConfig.getAssignedAddresses();
                        StringBuilder sb = new StringBuilder();
                        boolean disableIpv6 = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("network_disable_ipv6", false);
                        for (int i = 0; i < assignedAddresses.size(); i++) {
                            try {
                                InetAddress byAddress = InetAddress.getByAddress(assignedAddresses.get(i).getAddressBytes());
                                if (!disableIpv6 || !(byAddress instanceof Inet6Address)) {
                                    String inetAddress = byAddress.toString();
                                    if (inetAddress.startsWith("/")) {
                                        inetAddress = inetAddress.substring(1);
                                    }
                                    sb.append(inetAddress);
                                    sb.append('/');
                                    sb.append(assignedAddresses.get(i).getPrefix());
                                    if (i < assignedAddresses.size() - 1) {
                                        sb.append('\n');
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        ((TextView) inflate.findViewById(R.id.network_ipaddresses_textview)).setText(sb.toString());
                        StringBuilder sb2 = new StringBuilder();
                        TableRow tableRow = inflate.findViewById(R.id.custom_dns_row);
                        if (networkConfig.getDnsMode() == 2) {
                            tableRow.setVisibility(View.VISIBLE);
                            List<DnsServer> dnsServers = networkConfig.getDnsServers();
                            for (int i2 = 0; i2 < dnsServers.size(); i2++) {
                                sb2.append(dnsServers.get(i2).getNameserver());
                                if (i2 < dnsServers.size() - 1) {
                                    sb2.append('\n');
                                }
                            }
                            ((TextView) inflate.findViewById(R.id.network_dns_textview)).setText(sb2.toString());
                        } else {
                            tableRow.setVisibility(View.INVISIBLE);
                        }
                    }
                    checkBox.setOnCheckedChangeListener(new DefaultRouteChangeListener(network, networkConfig));
                }
            }
        } else {
            Log.e(TAG, "Network ID is not set");
        }
        return inflate;
    }

    @Override // androidx.fragment.app.Fragment
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override // androidx.fragment.app.Fragment
    public void onDetach() {
        super.onDetach();
    }

    private static class DefaultRouteChangeListener implements CompoundButton.OnCheckedChangeListener {
        private final NetworkConfig nc;

        /* renamed from: net  reason: collision with root package name */
        private final Network f0net;

        DefaultRouteChangeListener(Network network, NetworkConfig networkConfig) {
            this.f0net = network;
            this.nc = networkConfig;
        }

        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            this.f0net.setUseDefaultRoute(z);
            this.nc.setRouteViaZeroTier(z);
            this.f0net.update();
            this.nc.update();
            EventBus.getDefault().post(new DefaultRouteChangedEvent(this.f0net.getNetworkId(), this.f0net.getUseDefaultRoute()));
        }
    }
}

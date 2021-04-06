package net.kaaass.zerotierfix.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.os.EnvironmentCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.zerotier.sdk.NodeStatus;
import com.zerotier.sdk.VirtualNetworkConfig;
import com.zerotier.sdk.VirtualNetworkStatus;
import com.zerotier.sdk.VirtualNetworkType;

import net.kaaass.zerotierfix.AnalyticsApplication;
import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.events.AfterJoinNetworkEvent;
import net.kaaass.zerotierfix.events.IsServiceRunningEvent;
import net.kaaass.zerotierfix.events.NetworkInfoReplyEvent;
import net.kaaass.zerotierfix.events.NetworkListReplyEvent;
import net.kaaass.zerotierfix.events.NodeDestroyedEvent;
import net.kaaass.zerotierfix.events.NodeIDEvent;
import net.kaaass.zerotierfix.events.NodeStatusEvent;
import net.kaaass.zerotierfix.events.OrbitMoonEvent;
import net.kaaass.zerotierfix.events.RequestNetworkListEvent;
import net.kaaass.zerotierfix.events.RequestNodeStatusEvent;
import net.kaaass.zerotierfix.events.StopEvent;
import net.kaaass.zerotierfix.model.AppNode;
import net.kaaass.zerotierfix.model.AssignedAddress;
import net.kaaass.zerotierfix.model.AssignedAddressDao;
import net.kaaass.zerotierfix.model.DaoSession;
import net.kaaass.zerotierfix.model.MoonOrbit;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.model.NetworkConfig;
import net.kaaass.zerotierfix.model.NetworkConfigDao;
import net.kaaass.zerotierfix.model.NetworkDao;
import net.kaaass.zerotierfix.service.ZeroTierOneService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.WhereCondition;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

public class NetworkListFragment extends Fragment {
    public static final int AUTH_VPN = 3;
    public static final String NETWORK_ID_MESSAGE = "com.zerotier.one.network-id";
    public static final int START_VPN = 2;
    public static final String TAG = "NetworkListFragment";
    private final EventBus eventBus;
    boolean mIsBound = false;
    private JoinAfterAuth joinAfterAuth;
    private NetworkAdapter listAdapter;
    private ListView listView;
    private ZeroTierOneService mBoundService;
    private final ServiceConnection mConnection = new ServiceConnection() {
        /* class com.zerotier.one.ui.NetworkListFragment.AnonymousClass1 */

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            NetworkListFragment.this.mBoundService = ((ZeroTierOneService.ZeroTierBinder) iBinder).getService();
        }

        public void onServiceDisconnected(ComponentName componentName) {
            NetworkListFragment.this.mBoundService = null;
            NetworkListFragment.this.setIsBound(false);
        }
    };
    private List<Network> mNetworks;
    private VirtualNetworkConfig[] mVNC;
    private TextView nodeIdView;
    private TextView nodeStatusView;

    public NetworkListFragment() {
        Log.d(TAG, "Network List Fragment created");
        this.eventBus = EventBus.getDefault();
    }

    /* access modifiers changed from: package-private */
    public synchronized void setIsBound(boolean z) {
        this.mIsBound = z;
    }

    /* access modifiers changed from: package-private */
    public synchronized boolean isBound() {
        return this.mIsBound;
    }

    /* access modifiers changed from: package-private */
    public void doBindService() {
        if (!isBound()) {
            if (getActivity().bindService(new Intent(getActivity(), ZeroTierOneService.class), this.mConnection, Context.BIND_NOT_FOREGROUND | Context.BIND_DEBUG_UNBIND)) {
                setIsBound(true);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void doUnbindService() {
        if (isBound()) {
            try {
                getActivity().unbindService(this.mConnection);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            } catch (Throwable th) {
                setIsBound(false);
                throw th;
            }
            setIsBound(false);
        }
    }

    @Override // androidx.fragment.app.Fragment
    public void onStart() {
        super.onStart();
        this.eventBus.register(this);
        this.eventBus.post(new RequestNetworkListEvent());
        this.eventBus.post(new RequestNodeStatusEvent());
        this.eventBus.post(IsServiceRunningEvent.NewRequest());
    }

    @Override // androidx.fragment.app.Fragment
    public void onStop() {
        super.onStop();
        doUnbindService();
        this.eventBus.unregister(this);
    }

    @Override // androidx.fragment.app.Fragment
    public void onPause() {
        super.onPause();
        this.eventBus.unregister(this);
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        View view = layoutInflater.inflate(R.layout.network_list_fragment, viewGroup, false);
        ListView listView2 = view.findViewById(R.id.joined_networks_list);
        this.listView = listView2;
        listView2.setClickable(true);
        this.listView.setLongClickable(true);
        this.mNetworks = getNetworkList();
        NetworkAdapter networkAdapter = new NetworkAdapter(this, this.mNetworks);
        this.listAdapter = networkAdapter;
        this.listView.setAdapter(networkAdapter);
        this.nodeIdView = view.findViewById(R.id.node_id);
        this.nodeStatusView = view.findViewById(R.id.node_status);
        setNodeIdText();

        // 设置添加按钮
        FloatingActionButton fab = view.findViewById(R.id.fab_add_network);
        fab.setOnClickListener(parentView -> {
            Log.d(TAG, "Selected Join Network");
            startActivity(new Intent(getActivity(), JoinNetworkActivity.class));
        });

        return view;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void sendStartServiceIntent(long networkId, boolean useDefaultRoute) {
        Intent prepare = VpnService.prepare(getActivity());
        if (prepare != null) {
            this.joinAfterAuth = new JoinAfterAuth(networkId, useDefaultRoute);
            startActivityForResult(prepare, 3);
            return;
        }
        Log.d(TAG, "Intent is NULL.  Already approved.");
        startService(networkId, useDefaultRoute);
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        Log.d(TAG, "NetworkListFragment.onCreate");
        super.onCreate(bundle);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        setHasOptionsMenu(true);
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        this.nodeStatusView.setText(R.string.status_offline);
        this.eventBus.post(IsServiceRunningEvent.NewRequest());
        updateNetworkList();
        sortNetworkListAndNotify();
        this.eventBus.post(new RequestNetworkListEvent());
        this.eventBus.post(new RequestNodeStatusEvent());
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        Log.d(TAG, "NetworkListFragment.onCreateOptionsMenu");
        menuInflater.inflate(R.menu.menu_network_list, menu);
        super.onCreateOptionsMenu(menu, menuInflater);
        this.eventBus.post(new RequestNodeStatusEvent());
    }

    @Override // androidx.fragment.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int menuId = menuItem.getItemId();
        if (menuId == R.id.menu_item_settings) {
            Log.d(TAG, "Selected Settings");
            startActivity(new Intent(getActivity(), PrefsActivity.class));
            return true;
        } else if (menuId == R.id.menu_item_peers) {
            Log.d(TAG, "Selected peers");
            // TODO
            Snackbar.make(getView(), "WIP", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return true;
        } else if (menuId == R.id.menu_item_orbit) {
            Log.d(TAG, "Selected orbit");
            startActivity(new Intent(getActivity(), MoonOrbitActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        JoinAfterAuth joinAfterAuth2;
        if (i == 2) {
            long longExtra = intent.getLongExtra(ZeroTierOneService.ZT1_NETWORK_ID, 0);
            boolean booleanExtra = intent.getBooleanExtra(ZeroTierOneService.ZT1_USE_DEFAULT_ROUTE, false);
            if (longExtra != 0) {
                startService(longExtra, booleanExtra);
            } else {
                Log.e(TAG, "Network ID not provided.  Cannot start network without an ID");
            }
        } else if (i == 3) {
            Log.d(TAG, "Returned from AUTH_VPN");
            if (i2 == -1 && (joinAfterAuth2 = this.joinAfterAuth) != null) {
                startService(joinAfterAuth2.networkId, this.joinAfterAuth.useDefaultRoute);
            }
            this.joinAfterAuth = null;
        }
    }

    @Override // androidx.fragment.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private List<Network> getNetworkList() {
        DaoSession daoSession = ((AnalyticsApplication) getActivity().getApplication()).getDaoSession();
        daoSession.clear();
        return daoSession.getNetworkDao().queryBuilder().orderAsc(NetworkDao.Properties.NetworkId).build().forCurrentThread().list();
    }

    private void setNodeIdText() {
        List<AppNode> list = ((AnalyticsApplication) getActivity().getApplication()).getDaoSession().getAppNodeDao().queryBuilder().build().forCurrentThread().list();
        if (!list.isEmpty()) {
            this.nodeIdView.setText(list.get(0).getNodeIdStr());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onIsServicerunning(IsServiceRunningEvent isServiceRunningEvent) {
        if (isServiceRunningEvent.type == IsServiceRunningEvent.Type.REPLY && isServiceRunningEvent.isRunning) {
            doBindService();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkListReply(NetworkListReplyEvent networkListReplyEvent) {
        Log.d(TAG, "Got network list");
        this.mVNC = networkListReplyEvent.getNetworkList();
        updateNetworkList();
        sortNetworkListAndNotify();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkInfoReply(NetworkInfoReplyEvent networkInfoReplyEvent) {
        NetworkConfig.NetworkStatus networkStatus;
        Log.d(TAG, "Got Network Info");
        VirtualNetworkConfig networkInfo = networkInfoReplyEvent.getNetworkInfo();
        for (Network network : getNetworkList()) {
            if (network.getNetworkId() == networkInfo.networkId()) {
                network.setConnected(true);
                if (!networkInfo.name().isEmpty()) {
                    network.setNetworkName(networkInfo.name());
                }
                NetworkDao networkDao = ((AnalyticsApplication) getActivity().getApplication()).getDaoSession().getNetworkDao();
                NetworkConfigDao networkConfigDao = ((AnalyticsApplication) getActivity().getApplication()).getDaoSession().getNetworkConfigDao();
                NetworkConfig networkConfig = network.getNetworkConfig();
                if (networkConfig == null) {
                    networkConfig = new NetworkConfig();
                    networkConfig.setId(network.getNetworkId());
                    networkConfigDao.insert(networkConfig);
                }
                network.setNetworkConfig(networkConfig);
                network.setNetworkConfigId(network.getNetworkId());
                networkDao.save(network);
                networkConfig.setBridging(networkInfo.isBridgeEnabled());
                if (networkInfo.networkType() == VirtualNetworkType.NETWORK_TYPE_PRIVATE) {
                    networkConfig.setType(NetworkConfig.NetworkType.PRIVATE);
                } else if (networkInfo.networkType() == VirtualNetworkType.NETWORK_TYPE_PUBLIC) {
                    networkConfig.setType(NetworkConfig.NetworkType.PUBLIC);
                }
                switch (AnonymousClass2.$SwitchMap$com$zerotier$sdk$VirtualNetworkStatus[networkInfo.networkStatus().ordinal()]) {
                    case 1:
                        networkStatus = NetworkConfig.NetworkStatus.OK;
                        break;
                    case 2:
                        networkStatus = NetworkConfig.NetworkStatus.ACCESS_DENIED;
                        Toast.makeText(getActivity(), R.string.toast_not_authorized, Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        networkStatus = NetworkConfig.NetworkStatus.CLIENT_TOO_OLD;
                        break;
                    case 4:
                        networkStatus = NetworkConfig.NetworkStatus.NOT_FOUND;
                        break;
                    case 5:
                        networkStatus = NetworkConfig.NetworkStatus.PORT_ERROR;
                        break;
                    case 6:
                        networkStatus = NetworkConfig.NetworkStatus.REQUESTING_CONFIGURATION;
                        break;
                    default:
                        networkStatus = NetworkConfig.NetworkStatus.UNKNOWN;
                        break;
                }
                networkConfig.setStatus(networkStatus);
                String macAddress = Long.toHexString(networkInfo.macAddress());
                while (macAddress.length() < 12) {
                    macAddress = "0" + macAddress;
                }
                String sb = String.valueOf(macAddress.charAt(0)) +
                        macAddress.charAt(1) +
                        ':' +
                        macAddress.charAt(2) +
                        macAddress.charAt(3) +
                        ':' +
                        macAddress.charAt(4) +
                        macAddress.charAt(5) +
                        ':' +
                        macAddress.charAt(6) +
                        macAddress.charAt(7) +
                        ':' +
                        macAddress.charAt(8) +
                        macAddress.charAt(9) +
                        ':' +
                        macAddress.charAt(10) +
                        macAddress.charAt(11);
                networkConfig.setMac(sb);
                networkConfig.setMtu(Integer.toString(networkInfo.mtu()));
                networkConfig.setBroadcast(networkInfo.broadcastEnabled());
                network.setNetworkConfigId(networkInfo.networkId());
                network.setNetworkConfig(networkConfig);
                networkDao.save(network);
                networkConfigDao.save(networkConfig);
                ((AnalyticsApplication) getActivity().getApplication()).getDaoSession().getAssignedAddressDao().queryBuilder().where(AssignedAddressDao.Properties.NetworkId.eq(Long.valueOf(networkInfo.networkId())), new WhereCondition[0]).buildDelete().forCurrentThread().forCurrentThread().executeDeleteWithoutDetachingEntities();
                ((AnalyticsApplication) getActivity().getApplication()).getDaoSession().clear();
                AssignedAddressDao assignedAddressDao = ((AnalyticsApplication) getActivity().getApplication()).getDaoSession().getAssignedAddressDao();
                InetSocketAddress[] assignedAddresses = networkInfo.assignedAddresses();
                for (InetSocketAddress inetSocketAddress : assignedAddresses) {
                    InetAddress address = inetSocketAddress.getAddress();
                    short port = (short) inetSocketAddress.getPort();
                    AssignedAddress assignedAddress = new AssignedAddress();
                    String inetAddress = address.toString();
                    if (inetAddress.startsWith("/")) {
                        inetAddress = inetAddress.substring(1);
                    }
                    if (address instanceof Inet6Address) {
                        assignedAddress.setType(AssignedAddress.AddressType.IPV6);
                    } else if (address instanceof InetAddress) {
                        assignedAddress.setType(AssignedAddress.AddressType.IPV6);
                    }
                    assignedAddress.setAddressBytes(address.getAddress());
                    assignedAddress.setAddressString(inetAddress);
                    assignedAddress.setPrefix(port);
                    assignedAddress.setNetworkId(networkConfig.getId());
                    assignedAddressDao.save(assignedAddress);
                }
            } else {
                network.setConnected(false);
                network.update();
            }
        }
        ((AnalyticsApplication) getActivity().getApplication()).getDaoSession().clear();
        updateNetworkList();
        sortNetworkListAndNotify();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNodeID(NodeIDEvent nodeIDEvent) {
        setNodeIdText();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNodeStatus(NodeStatusEvent nodeStatusEvent) {
        NodeStatus status = nodeStatusEvent.getStatus();
        if (status.isOnline()) {
            this.nodeStatusView.setText(R.string.status_online);
            TextView textView = this.nodeIdView;
            if (textView != null) {
                textView.setText(Long.toHexString(status.getAddres()));
                return;
            }
            return;
        }
        setOfflineState();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNodeDestroyed(NodeDestroyedEvent nodeDestroyedEvent) {
        setOfflineState();
    }

    private void setOfflineState() {
        TextView textView = this.nodeStatusView;
        if (textView != null) {
            textView.setText(R.string.status_offline);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateNetworkList() {
        List<Network> networkList = getNetworkList();
        if (!networkList.isEmpty()) {
            for (Network network : this.mNetworks) {
                for (Network network2 : networkList) {
                    if (network.getNetworkId().equals(network2.getNetworkId())) {
                        network2.setConnected(network.getConnected());
                    }
                }
            }
            if (this.mVNC != null) {
                for (Network network3 : networkList) {
                    for (VirtualNetworkConfig virtualNetworkConfig : this.mVNC) {
                        network3.setConnected(network3.getNetworkId() == virtualNetworkConfig.networkId());
                    }
                }
            }
            this.mNetworks.clear();
            this.mNetworks.addAll(networkList);
        }
    }

    /* access modifiers changed from: protected */
    public void sortNetworkListAndNotify() {
        NetworkAdapter networkAdapter = this.listAdapter;
        if (networkAdapter != null) {
            networkAdapter.notifyDataSetChanged();
        }
        this.listView.invalidateViews();
        this.listAdapter.notifyDataSetChanged();
    }

    private void startService(long networkId, boolean useDefaultRoute) {
        Intent intent = new Intent(getActivity(), ZeroTierOneService.class);
        intent.putExtra(ZeroTierOneService.ZT1_NETWORK_ID, networkId);
        intent.putExtra(ZeroTierOneService.ZT1_USE_DEFAULT_ROUTE, useDefaultRoute);
        doBindService();
        getActivity().startService(intent);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void stopService() {
        ZeroTierOneService zeroTierOneService = this.mBoundService;
        if (zeroTierOneService != null) {
            zeroTierOneService.stopZeroTier();
        }
        Intent intent = new Intent(getActivity(), ZeroTierOneService.class);
        this.eventBus.post(new StopEvent());
        if (!getActivity().stopService(intent)) {
            Log.e(TAG, "stopService() returned false");
        }
        doUnbindService();
        this.mVNC = null;
    }

    /**
     * 获得 Moon 入轨配置列表
     */
    private List<MoonOrbit> getMoonOrbitList() {
        DaoSession daoSession = ((AnalyticsApplication) getActivity().getApplication()).getDaoSession();
        return daoSession.getMoonOrbitDao().loadAll();
    }

    /* renamed from: com.zerotier.one.ui.NetworkListFragment$2  reason: invalid class name */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$zerotier$sdk$VirtualNetworkStatus;

        static {
            $SwitchMap$com$zerotier$sdk$VirtualNetworkStatus = new int[VirtualNetworkStatus.values().length];
            $SwitchMap$com$zerotier$sdk$VirtualNetworkStatus[com.zerotier.sdk.VirtualNetworkStatus.NETWORK_STATUS_OK.ordinal()] = 1;
            $SwitchMap$com$zerotier$sdk$VirtualNetworkStatus[com.zerotier.sdk.VirtualNetworkStatus.NETWORK_STATUS_ACCESS_DENIED.ordinal()] = 2;
            $SwitchMap$com$zerotier$sdk$VirtualNetworkStatus[com.zerotier.sdk.VirtualNetworkStatus.NETWORK_STATUS_CLIENT_TOO_OLD.ordinal()] = 3;
            $SwitchMap$com$zerotier$sdk$VirtualNetworkStatus[com.zerotier.sdk.VirtualNetworkStatus.NETWORK_STATUS_NOT_FOUND.ordinal()] = 4;
            $SwitchMap$com$zerotier$sdk$VirtualNetworkStatus[com.zerotier.sdk.VirtualNetworkStatus.NETWORK_STATUS_PORT_ERROR.ordinal()] = 5;
            $SwitchMap$com$zerotier$sdk$VirtualNetworkStatus[com.zerotier.sdk.VirtualNetworkStatus.NETWORK_STATUS_REQUESTING_CONFIGURATION.ordinal()] = 6;
        }
    }

    /* access modifiers changed from: private */
    public static class JoinAfterAuth {
        long networkId;
        boolean useDefaultRoute;

        JoinAfterAuth(long j, boolean z) {
            this.networkId = j;
            this.useDefaultRoute = z;
        }
    }

    @Subscribe
    public void onAfterJoinNetworkEvent(AfterJoinNetworkEvent event) {
        Log.d(TAG, "Event on: AfterJoinNetworkEvent");
        // 设置网络 orbit
        List<MoonOrbit> moonOrbits = NetworkListFragment.this.getMoonOrbitList();
        this.eventBus.post(new OrbitMoonEvent(moonOrbits));
    }

    /* access modifiers changed from: private */
    public class NetworkAdapter extends ArrayAdapter<Network> {
        private final NetworkListFragment parentView;

        public NetworkAdapter(NetworkListFragment networkListFragment, List<Network> list) {
            super(NetworkListFragment.this.getActivity(), 0, list);
            this.parentView = networkListFragment;
            Log.d(NetworkListFragment.TAG, "Created network list item adapter");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!NetworkListFragment.this.listView.getItemsCanFocus()) {
                NetworkListFragment.this.listView.setItemsCanFocus(true);
            }
            if (convertView == null) {
                convertView = NetworkListFragment.this.getActivity().getLayoutInflater().inflate(R.layout.list_item_network, null);
            }
            final Network currentNetwork = getItem(position);
            convertView.setClickable(true);
            convertView.setOnClickListener(view -> {
                Log.d(NetworkListFragment.TAG, "ConvertView OnClickListener");
                Intent intent = new Intent(NetworkListFragment.this.getActivity(), NetworkDetailActivity.class);
                intent.putExtra(NetworkListFragment.NETWORK_ID_MESSAGE, currentNetwork.getNetworkId());
                NetworkListFragment.this.startActivity(intent);
            });
            convertView.setOnLongClickListener(view -> {
                Log.d(NetworkListFragment.TAG, "ConvertView OnLongClickListener");
                PopupMenu popupMenu = new PopupMenu(NetworkListFragment.this.getActivity(), view);
                popupMenu.getMenuInflater().inflate(R.menu.context_menu_network_item, popupMenu.getMenu());
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() != R.id.menu_item_delete_network) {
                        return false;
                    }
                    DaoSession daoSession = ((AnalyticsApplication) NetworkListFragment.this.getActivity().getApplication()).getDaoSession();
                    AssignedAddressDao assignedAddressDao = daoSession.getAssignedAddressDao();
                    NetworkConfigDao networkConfigDao = daoSession.getNetworkConfigDao();
                    NetworkDao networkDao = daoSession.getNetworkDao();
                    if (currentNetwork != null) {
                        if (currentNetwork.getConnected()) {
                            NetworkListFragment.this.stopService();
                        }
                        NetworkConfig networkConfig = currentNetwork.getNetworkConfig();
                        if (networkConfig != null) {
                            List<AssignedAddress> assignedAddresses = networkConfig.getAssignedAddresses();
                            if (!assignedAddresses.isEmpty()) {
                                for (AssignedAddress assignedAddress : assignedAddresses) {
                                    assignedAddressDao.delete(assignedAddress);
                                }
                            }
                            networkConfigDao.delete(networkConfig);
                        }
                        networkDao.delete(currentNetwork);
                    }
                    daoSession.clear();
                    NetworkListFragment.this.updateNetworkList();
                    NetworkAdapter.this.parentView.sortNetworkListAndNotify();
                    return true;
                });
                return true;
            });
            ((TextView) convertView.findViewById(R.id.network_list_network_id)).setText(currentNetwork.getNetworkIdStr());
            TextView textView = convertView.findViewById(R.id.network_list_network_name);
            String networkName = currentNetwork.getNetworkName();
            if (networkName != null) {
                textView.setText(networkName);
            } else {
                textView.setText(EnvironmentCompat.MEDIA_UNKNOWN);
            }
            // 网络控制开关
            final Switch networkSwitch = convertView.findViewById(R.id.network_start_network_switch);
            networkSwitch.setOnCheckedChangeListener(null);
            networkSwitch.setChecked(currentNetwork.getConnected());
            networkSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
                NetworkDao networkDao = ((AnalyticsApplication) NetworkListFragment.this.getActivity().getApplication()).getDaoSession().getNetworkDao();
                if (checked) {
                    // 启动网络
                    boolean useCellularData = PreferenceManager.getDefaultSharedPreferences(NetworkAdapter.this.getContext()).getBoolean("network_use_cellular_data", false);
                    NetworkInfo activeNetworkInfo = ((ConnectivityManager) NetworkAdapter.this.getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                    if (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting()) {
                        // 设备无网络
                        Toast.makeText(NetworkAdapter.this.getContext(), R.string.toast_no_network, Toast.LENGTH_SHORT).show();
                        networkSwitch.setChecked(false);
                    } else if (useCellularData || !(activeNetworkInfo == null || activeNetworkInfo.getType() == 0)) {
                        // 关闭所有网络连接
                        for (Network network : NetworkListFragment.this.mNetworks) {
                            if (network.getConnected()) {
                                network.setConnected(false);
                            }
                            network.setLastActivated(false);
                            network.update();
                        }
                        NetworkListFragment.this.stopService();
                        // 连接目标网络
                        if (!NetworkListFragment.this.isBound()) {
                            NetworkListFragment.this.sendStartServiceIntent(currentNetwork.getNetworkId(), currentNetwork.getUseDefaultRoute());
                        } else {
                            NetworkListFragment.this.mBoundService.joinNetwork(currentNetwork.getNetworkId(), currentNetwork.getUseDefaultRoute());
                        }
                        Log.d(NetworkListFragment.TAG, "Joining Network: " + currentNetwork.getNetworkIdStr());
                        currentNetwork.setConnected(true);
                        currentNetwork.setLastActivated(true);
                        networkDao.save(currentNetwork);
                    } else {
                        // 移动数据且未确认
                        Toast.makeText(NetworkAdapter.this.getContext(), R.string.toast_mobile_data, Toast.LENGTH_SHORT).show();
                        networkSwitch.setChecked(false);
                    }
                } else {
                    // 关闭网络
                    Log.d(NetworkListFragment.TAG, "Leaving Leaving Network: " + currentNetwork.getNetworkIdStr());
                    if (!(!NetworkListFragment.this.isBound() || NetworkListFragment.this.mBoundService == null || currentNetwork == null)) {
                        NetworkListFragment.this.mBoundService.leaveNetwork(currentNetwork.getNetworkId());
                        NetworkListFragment.this.doUnbindService();
                    }
                    NetworkListFragment.this.stopService();
                    currentNetwork.setConnected(false);
                    networkDao.save(currentNetwork);
                    NetworkListFragment.this.mVNC = null;
                }
            });
            return convertView;
        }
    }
}

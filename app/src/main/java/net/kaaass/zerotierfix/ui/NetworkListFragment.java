package net.kaaass.zerotierfix.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
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
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zerotier.sdk.NodeStatus;
import com.zerotier.sdk.Version;

import net.kaaass.zerotierfix.BuildConfig;
import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.events.AfterJoinNetworkEvent;
import net.kaaass.zerotierfix.events.IsServiceRunningReplyEvent;
import net.kaaass.zerotierfix.events.IsServiceRunningRequestEvent;
import net.kaaass.zerotierfix.events.NetworkListCheckedChangeEvent;
import net.kaaass.zerotierfix.events.NetworkListReplyEvent;
import net.kaaass.zerotierfix.events.NetworkListRequestEvent;
import net.kaaass.zerotierfix.events.NodeDestroyedEvent;
import net.kaaass.zerotierfix.events.NodeIDEvent;
import net.kaaass.zerotierfix.events.NodeStatusEvent;
import net.kaaass.zerotierfix.events.NodeStatusRequestEvent;
import net.kaaass.zerotierfix.events.OrbitMoonEvent;
import net.kaaass.zerotierfix.events.StopEvent;
import net.kaaass.zerotierfix.events.VPNErrorEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigChangedEvent;
import net.kaaass.zerotierfix.model.AppNode;
import net.kaaass.zerotierfix.model.AssignedAddress;
import net.kaaass.zerotierfix.model.AssignedAddressDao;
import net.kaaass.zerotierfix.model.DaoSession;
import net.kaaass.zerotierfix.model.MoonOrbit;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.model.NetworkConfig;
import net.kaaass.zerotierfix.model.NetworkConfigDao;
import net.kaaass.zerotierfix.model.NetworkDao;
import net.kaaass.zerotierfix.model.type.NetworkStatus;
import net.kaaass.zerotierfix.service.ZeroTierOneService;
import net.kaaass.zerotierfix.ui.view.NetworkDetailActivity;
import net.kaaass.zerotierfix.ui.viewmodel.NetworkListModel;
import net.kaaass.zerotierfix.util.Constants;
import net.kaaass.zerotierfix.util.DatabaseUtils;
import net.kaaass.zerotierfix.util.StringUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import lombok.ToString;

// TODO: clear up
public class NetworkListFragment extends Fragment {
    public static final String NETWORK_ID_MESSAGE = "com.zerotier.one.network-id";
    public static final String TAG = "NetworkListFragment";
    private final EventBus eventBus;
    private final List<Network> mNetworks = new ArrayList<>();
    boolean mIsBound = false;
    private RecyclerViewAdapter recyclerViewAdapter;
    private RecyclerView recyclerView;
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
    private TextView nodeIdView;
    private TextView nodeStatusView;
    private TextView nodeClientVersionView;

    private View emptyView = null;
    final private RecyclerView.AdapterDataObserver checkIfEmptyObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        /**
         * 检查列表是否为空
         */
        void checkIfEmpty() {
            if (emptyView != null && recyclerViewAdapter != null) {
                final boolean emptyViewVisible = recyclerViewAdapter.getItemCount() == 0;
                emptyView.setVisibility(emptyViewVisible ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(emptyViewVisible ? View.GONE : View.VISIBLE);
            }
        }
    };
    private ActivityResultLauncher<Intent> vpnAuthLauncher;
    private NetworkListModel viewModel;

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
            if (requireActivity().bindService(new Intent(getActivity(), ZeroTierOneService.class), this.mConnection, Context.BIND_NOT_FOREGROUND | Context.BIND_DEBUG_UNBIND)) {
                setIsBound(true);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void doUnbindService() {
        if (isBound()) {
            try {
                requireActivity().unbindService(this.mConnection);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            } catch (Throwable th) {
                setIsBound(false);
                throw th;
            }
            setIsBound(false);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 初始化 VPN 授权结果回调
        vpnAuthLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (activityResult) -> {
            var result = activityResult.getResultCode();
            Log.d(TAG, "Returned from AUTH_VPN");
            if (result == -1) {
                // 得到授权，连接网络
                startService(this.viewModel.getNetworkId());
            } else if (result == 0) {
                // 未授权
                updateNetworkListAndNotify();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        this.eventBus.post(new NetworkListRequestEvent());

        // 初始化节点及服务状态
        this.eventBus.post(new NodeStatusRequestEvent());
        this.eventBus.post(new IsServiceRunningRequestEvent());

        // 检查通知权限
        var notificationManager = NotificationManagerCompat.from(requireContext());
        if (!notificationManager.areNotificationsEnabled()) {
            // 无通知权限
            showNoNotificationAlertDialog();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        doUnbindService();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.eventBus.unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_network_list, container, false);

        // 空列表提示
        this.emptyView = view.findViewById(R.id.no_data);

        // 列表、适配器设置
        this.recyclerView = view.findViewById(R.id.joined_networks_list);
        this.recyclerView.setClickable(true);
        this.recyclerView.setLongClickable(true);
        this.recyclerViewAdapter = new RecyclerViewAdapter(this.mNetworks);
        this.recyclerViewAdapter.registerAdapterDataObserver(checkIfEmptyObserver);
        this.recyclerView.setAdapter(this.recyclerViewAdapter);

        // 网络状态栏设置
        this.nodeIdView = view.findViewById(R.id.node_id);
        this.nodeStatusView = view.findViewById(R.id.node_status);
        this.nodeClientVersionView = view.findViewById(R.id.client_version);
        setNodeIdText();
        TextView appVersionView = view.findViewById(R.id.app_version);
        appVersionView.setText(String.format(getString(R.string.app_version_format),
                BuildConfig.VERSION_NAME));

        // 加载网络数据
        updateNetworkListAndNotify();

        // 设置添加按钮
        FloatingActionButton fab = view.findViewById(R.id.fab_add_network);
        fab.setOnClickListener(parentView -> {
            Log.d(TAG, "Selected Join Network");
            startActivity(new Intent(getActivity(), JoinNetworkActivity.class));
        });

        // 当前连接网络变更时更新列表
        this.viewModel.getConnectNetworkId().observe(getViewLifecycleOwner(), networkId ->
                this.recyclerViewAdapter.notifyDataSetChanged());

        return view;
    }

    /**
     * 发送连接至指定网络的 Intent。将请求 VPN 权限后启动 ZT 服务
     *
     * @param networkId 网络号
     */
    private void sendStartServiceIntent(long networkId) {
        var prepare = VpnService.prepare(requireActivity());
        if (prepare != null) {
            // 等待 VPN 授权后连接网络
            this.viewModel.setNetworkId(networkId);
            vpnAuthLauncher.launch(prepare);
            return;
        }
        Log.d(TAG, "Intent is NULL.  Already approved.");
        startService(networkId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "NetworkListFragment.onCreate");
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        setHasOptionsMenu(true);

        // 获取 ViewModel
        this.viewModel = new ViewModelProvider(requireActivity()).get(NetworkListModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.nodeStatusView.setText(R.string.status_offline);
        this.eventBus.register(this);
        this.eventBus.post(new IsServiceRunningRequestEvent());
        updateNetworkListAndNotify();
        this.eventBus.post(new NetworkListRequestEvent());
        this.eventBus.post(new NodeStatusRequestEvent());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater menuInflater) {
        Log.d(TAG, "NetworkListFragment.onCreateOptionsMenu");
        menuInflater.inflate(R.menu.menu_network_list, menu);
        super.onCreateOptionsMenu(menu, menuInflater);
        this.eventBus.post(new NodeStatusRequestEvent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int menuId = menuItem.getItemId();
        if (menuId == R.id.menu_item_settings) {
            Log.d(TAG, "Selected Settings");
            startActivity(new Intent(getActivity(), PrefsActivity.class));
            return true;
        } else if (menuId == R.id.menu_item_peers) {
            Log.d(TAG, "Selected peers");
            startActivity(new Intent(getActivity(), PeerListActivity.class));
            return true;
        } else if (menuId == R.id.menu_item_orbit) {
            Log.d(TAG, "Selected orbit");
            startActivity(new Intent(getActivity(), MoonOrbitActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private List<Network> getNetworkList() {
        DaoSession daoSession = ((ZerotierFixApplication) requireActivity().getApplication()).getDaoSession();
        daoSession.clear();
        return daoSession.getNetworkDao().queryBuilder().orderAsc(NetworkDao.Properties.NetworkId).build().forCurrentThread().list();
    }

    private void setNodeIdText() {
        List<AppNode> list = ((ZerotierFixApplication) requireActivity().getApplication()).getDaoSession().getAppNodeDao().queryBuilder().build().forCurrentThread().list();
        if (!list.isEmpty()) {
            this.nodeIdView.setText(list.get(0).getNodeIdStr());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onIsServiceRunningReply(IsServiceRunningReplyEvent event) {
        if (event.isRunning()) {
            doBindService();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkListReply(NetworkListReplyEvent event) {
        Log.d(TAG, "Got connecting network list");
        // 更新当前连接的网络
        var networks = event.getNetworkList();
        for (var network : networks) {
            this.viewModel.doChangeConnectNetwork(network.getNwid());
        }
        // 更新网络列表
        updateNetworkListAndNotify();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVirtualNetworkConfigChanged(VirtualNetworkConfigChangedEvent event) {
        Log.d(TAG, "Got Network Info");
        var config = event.getVirtualNetworkConfig();

        // Toast 提示网络状态
        var status = NetworkStatus.fromVirtualNetworkStatus(config.getStatus());
        var networkId = com.zerotier.sdk.util.StringUtils.networkIdToString(config.getNwid());
        String message = null;
        switch (status) {
            case OK:
                message = getString(R.string.toast_network_status_ok, networkId);
                break;
            case ACCESS_DENIED:
                message = getString(R.string.toast_network_status_access_denied, networkId);
                break;
            case NOT_FOUND:
                message = getString(R.string.toast_network_status_not_found, networkId);
                break;
            case PORT_ERROR:
                message = getString(R.string.toast_network_status_port_error, networkId);
                break;
            case CLIENT_TOO_OLD:
                message = getString(R.string.toast_network_status_client_too_old, networkId);
                break;
            case AUTHENTICATION_REQUIRED:
                message = getString(R.string.toast_network_status_authentication_required, networkId);
                break;
            case REQUESTING_CONFIGURATION:
            default:
                break;
        }
        if (message != null) {
            Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show();
        }

        // 更新网络列表
        updateNetworkListAndNotify();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNodeID(NodeIDEvent nodeIDEvent) {
        setNodeIdText();
    }

    /**
     * 节点状态事件回调
     *
     * @param event 事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNodeStatus(NodeStatusEvent event) {
        NodeStatus status = event.getStatus();
        Version clientVersion = event.getClientVersion();
        // 更新在线状态
        if (status.isOnline()) {
            this.nodeStatusView.setText(R.string.status_online);
            if (this.nodeIdView != null) {
                this.nodeIdView.setText(Long.toHexString(status.getAddress()));
            }
        } else {
            setOfflineState();
        }
        // 更新客户端版本
        if (this.nodeClientVersionView != null) {
            this.nodeClientVersionView.setText(StringUtils.toString(clientVersion));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNodeDestroyed(NodeDestroyedEvent event) {
        setOfflineState();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVPNError(VPNErrorEvent event) {
        var errorMessage = event.getMessage();
        var message = getString(R.string.toast_vpn_error, errorMessage);
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        updateNetworkListAndNotify();
    }

    private void setOfflineState() {
        TextView textView = this.nodeStatusView;
        if (textView != null) {
            textView.setText(R.string.status_offline);
        }
    }

    /**
     * 更新网络列表
     */
    private void updateNetworkList() {
        List<Network> networkList = getNetworkList();
        if (networkList != null) {
            // 更新列表
            this.mNetworks.clear();
            this.mNetworks.addAll(networkList);
        }
    }

    /**
     * 更新网络列表与 UI
     */
    public void updateNetworkListAndNotify() {
        // 更新数据
        updateNetworkList();
        // 更新列表
        if (this.recyclerViewAdapter != null) {
            this.recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 启动 ZT 服务连接至指定网络
     *
     * @param networkId 网络号
     */
    private void startService(long networkId) {
        var intent = new Intent(requireActivity(), ZeroTierOneService.class);
        intent.putExtra(ZeroTierOneService.ZT1_NETWORK_ID, networkId);
        doBindService();
        requireActivity().startService(intent);
    }

    /**
     * 停止 ZT 服务
     */
    private void stopService() {
        if (this.mBoundService != null) {
            this.mBoundService.stopZeroTier();
        }
        var intent = new Intent(requireActivity(), ZeroTierOneService.class);
        this.eventBus.post(new StopEvent());
        if (!requireActivity().stopService(intent)) {
            Log.e(TAG, "stopService() returned false");
        }
        doUnbindService();
    }

    /**
     * 获得 Moon 入轨配置列表
     */
    private List<MoonOrbit> getMoonOrbitList() {
        DaoSession daoSession = ((ZerotierFixApplication) requireActivity().getApplication()).getDaoSession();
        return daoSession.getMoonOrbitDao().loadAll();
    }

    /**
     * 加入网络后事件回调
     *
     * @param event 事件
     */
    @Subscribe
    public void onAfterJoinNetworkEvent(AfterJoinNetworkEvent event) {
        Log.d(TAG, "Event on: AfterJoinNetworkEvent");
        // 设置网络 orbit
        List<MoonOrbit> moonOrbits = NetworkListFragment.this.getMoonOrbitList();
        this.eventBus.post(new OrbitMoonEvent(moonOrbits));
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onNetworkListCheckedChangeEvent(NetworkListCheckedChangeEvent event) {
        var switchHandle = event.getSwitchHandle();
        var checked = event.isChecked();
        var selectedNetwork = event.getSelectedNetwork();
        if (checked) {
            // 退出已连接的网络
            Long connectedNetworkId = this.viewModel.getConnectNetworkId().getValue();
            if (connectedNetworkId != null) {
                this.mBoundService.leaveNetwork(connectedNetworkId);
            }
            stopService();
            this.viewModel.doChangeConnectNetwork(null);
            // 启动网络
            var context = requireContext();
            boolean useCellularData = PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getBoolean(Constants.PREF_NETWORK_USE_CELLULAR_DATA, false);
            var activeNetworkInfo = ((ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE))
                    .getActiveNetworkInfo();
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting()) {
                // 设备无网络
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(NetworkListFragment.this.getContext(), R.string.toast_no_network, Toast.LENGTH_SHORT).show();
                    switchHandle.setChecked(false);
                });
            } else if (useCellularData || !(activeNetworkInfo.getType() == 0)) {
                // 可以连接至网络
                // 更新 DB 中的网络状态
                DatabaseUtils.writeLock.lock();
                try {
                    for (var network : this.mNetworks) {
                        network.setLastActivated(false);
                        network.update();
                    }
                    selectedNetwork.setLastActivated(true);
                    selectedNetwork.update();
                } finally {
                    DatabaseUtils.writeLock.unlock();
                }
                // 连接目标网络
                if (!this.isBound()) {
                    this.sendStartServiceIntent(selectedNetwork.getNetworkId());
                } else {
                    this.mBoundService.joinNetwork(selectedNetwork.getNetworkId());
                }
                this.viewModel.doChangeConnectNetwork(selectedNetwork.getNetworkId());
                Log.d(TAG, "Joining Network: " + selectedNetwork.getNetworkIdStr());
            } else {
                // 移动数据且未确认
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(this.getContext(), R.string.toast_mobile_data, Toast.LENGTH_SHORT).show();
                    switchHandle.setChecked(false);
                });
            }
        } else {
            // 关闭网络
            Log.d(TAG, "Leaving Leaving Network: " + selectedNetwork.getNetworkIdStr());
            if (this.isBound() && this.mBoundService != null) {
                this.mBoundService.leaveNetwork(selectedNetwork.getNetworkId());
                this.doUnbindService();
            }
            this.stopService();
            this.viewModel.doChangeConnectNetwork(null);
        }
    }

    /**
     * 显示无通知权限的提示框。若用户选择过 “不再提示”，则此方法将不进行任何操作
     */
    private void showNoNotificationAlertDialog() {
        // 检查是否选择过 “不再提示”，若是则不显示
        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        if (sharedPreferences.getBoolean(Constants.PREF_DISABLE_NO_NOTIFICATION_ALERT, false)) {
            return;
        }

        // 显示提示框
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_no_notification_alert, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(R.string.dialog_no_notification_alert_title)
                .setPositiveButton(R.string.open_notification_settings, (dialog, which) -> {
                    // 打开 APP 的通知设置
                    var intent = new Intent();

                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("app_package", requireContext().getPackageName());
                    intent.putExtra("app_uid", requireContext().getApplicationInfo().uid);
                    intent.putExtra("android.provider.extra.APP_PACKAGE", requireContext().getPackageName());
                    startActivity(intent);
                })
                .setNegativeButton(R.string.dont_show_again, (dialog, which) -> {
                    // 设置不再提示此对话框
                    sharedPreferences.edit()
                            .putBoolean(Constants.PREF_DISABLE_NO_NOTIFICATION_ALERT, true)
                            .apply();
                })
                .setCancelable(true);

        builder.create().show();
    }

    /**
     * 网络信息列表适配器
     */
    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private final List<Network> mValues;

        public RecyclerViewAdapter(List<Network> items) {
            this.mValues = items;
            Log.d(NetworkListFragment.TAG, "Created network list item adapter");
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_network, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerViewAdapter.ViewHolder holder, int position) {
            Network network = mValues.get(position);
            holder.mItem = network;
            // 设置文本信息
            holder.mNetworkId.setText(network.getNetworkIdStr());
            String networkName = network.getNetworkName();
            if (networkName != null && !networkName.isEmpty()) {
                holder.mNetworkName.setText(networkName);
            } else {
                holder.mNetworkName.setText(R.string.empty_network_name);
            }
            // 设置点击事件
            holder.mView.setOnClickListener(holder::onClick);
            // 设置长按事件
            holder.mView.setOnLongClickListener(holder::onLongClick);
            // 判断连接状态
            Long connectedNetworkId = NetworkListFragment.this.viewModel
                    .getConnectNetworkId().getValue();
            boolean connected = connectedNetworkId != null &&
                    connectedNetworkId.equals(network.getNetworkId());
            // 设置开关
            holder.mSwitch.setOnCheckedChangeListener(null);
            holder.mSwitch.setChecked(connected);
            holder.mSwitch.setOnCheckedChangeListener(holder::onSwitchCheckedChanged);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        @ToString
        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mNetworkId;
            public final TextView mNetworkName;
            public final SwitchCompat mSwitch;
            public Network mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mNetworkId = view.findViewById(R.id.network_list_network_id);
                mNetworkName = view.findViewById(R.id.network_list_network_name);
                mSwitch = view.findViewById(R.id.network_start_network_switch);
            }

            /**
             * 单击列表项打开网络详细页面
             */
            public void onClick(View view) {
                Log.d(NetworkListFragment.TAG, "ConvertView OnClickListener");
                Intent intent = new Intent(NetworkListFragment.this.getActivity(), NetworkDetailActivity.class);
                intent.putExtra(NetworkListFragment.NETWORK_ID_MESSAGE, this.mItem.getNetworkId());
                NetworkListFragment.this.startActivity(intent);
            }

            /**
             * 长按列表项创建弹出菜单
             */
            public boolean onLongClick(View view) {
                var that = NetworkListFragment.this;
                Log.d(NetworkListFragment.TAG, "ConvertView OnLongClickListener");
                PopupMenu popupMenu = new PopupMenu(that.getActivity(), view);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu_network_item, popupMenu.getMenu());
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.menu_item_delete_network) {
                        // 删除对应网络
                        DaoSession daoSession = ((ZerotierFixApplication) that.requireActivity().getApplication()).getDaoSession();
                        AssignedAddressDao assignedAddressDao = daoSession.getAssignedAddressDao();
                        NetworkConfigDao networkConfigDao = daoSession.getNetworkConfigDao();
                        NetworkDao networkDao = daoSession.getNetworkDao();
                        if (this.mItem != null) {
                            // 如果删除的是当前连接的网络，则停止服务
                            var connectedNetworkId = that.viewModel.getConnectNetworkId().getValue();
                            if (this.mItem.getNetworkId().equals(connectedNetworkId)) {
                                that.stopService();
                            }
                            // 从 DB 中删除网络
                            NetworkConfig networkConfig = this.mItem.getNetworkConfig();
                            if (networkConfig != null) {
                                List<AssignedAddress> assignedAddresses = networkConfig.getAssignedAddresses();
                                if (!assignedAddresses.isEmpty()) {
                                    for (AssignedAddress assignedAddress : assignedAddresses) {
                                        assignedAddressDao.delete(assignedAddress);
                                    }
                                }
                                networkConfigDao.delete(networkConfig);
                            }
                            networkDao.delete(this.mItem);
                        }
                        daoSession.clear();
                        // 更新数据
                        that.updateNetworkListAndNotify();
                        return true;
                    } else if (menuItem.getItemId() == R.id.menu_item_copy_network_id) {
                        // 复制网络 ID
                        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(getString(R.string.network_id), this.mItem.getNetworkIdStr());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getContext(), R.string.text_copied, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                });
                return true;
            }

            /**
             * 点击开启网络开关
             */
            public void onSwitchCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                NetworkListFragment.this.eventBus.post(new NetworkListCheckedChangeEvent(
                        this.mSwitch,
                        isChecked,
                        this.mItem
                ));
            }
        }
    }

}

package net.kaaass.zerotierfix.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.zerotier.sdk.Event;
import com.zerotier.sdk.EventListener;
import com.zerotier.sdk.Node;
import com.zerotier.sdk.NodeException;
import com.zerotier.sdk.NodeStatus;
import com.zerotier.sdk.ResultCode;
import com.zerotier.sdk.VirtualNetworkConfig;
import com.zerotier.sdk.VirtualNetworkConfigListener;
import com.zerotier.sdk.VirtualNetworkConfigOperation;
import com.zerotier.sdk.VirtualNetworkRoute;

import net.kaaass.zerotierfix.AnalyticsApplication;
import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.events.DefaultRouteChangedEvent;
import net.kaaass.zerotierfix.events.ErrorEvent;
import net.kaaass.zerotierfix.events.IsServiceRunningEvent;
import net.kaaass.zerotierfix.events.ManualDisconnectEvent;
import net.kaaass.zerotierfix.events.NetworkInfoReplyEvent;
import net.kaaass.zerotierfix.events.NetworkListReplyEvent;
import net.kaaass.zerotierfix.events.NetworkReconfigureEvent;
import net.kaaass.zerotierfix.events.NodeDestroyedEvent;
import net.kaaass.zerotierfix.events.NodeIDEvent;
import net.kaaass.zerotierfix.events.NodeStatusEvent;
import net.kaaass.zerotierfix.events.RequestNetworkInfoEvent;
import net.kaaass.zerotierfix.events.RequestNetworkListEvent;
import net.kaaass.zerotierfix.events.RequestNodeStatusEvent;
import net.kaaass.zerotierfix.events.StopEvent;
import net.kaaass.zerotierfix.model.AppNode;
import net.kaaass.zerotierfix.model.AppNodeDao;
import net.kaaass.zerotierfix.model.DaoSession;
import net.kaaass.zerotierfix.model.DnsServer;
import net.kaaass.zerotierfix.model.DnsServerDao;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.model.NetworkConfig;
import net.kaaass.zerotierfix.model.NetworkConfigDao;
import net.kaaass.zerotierfix.model.NetworkDao;
import net.kaaass.zerotierfix.ui.NetworkListActivity;
import net.kaaass.zerotierfix.util.InetAddressUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.WhereCondition;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ZeroTierOneService extends VpnService implements Runnable, EventListener, VirtualNetworkConfigListener {
    public static final int MSG_JOIN_NETWORK = 1;
    public static final int MSG_LEAVE_NETWORK = 2;
    public static final String ZT1_NETWORK_ID = "com.zerotier.one.network_id";
    public static final String ZT1_USE_DEFAULT_ROUTE = "com.zerotier.one.use_default_route";
    private static final String[] DISALLOWED_APPS = {"com.android.vending"};
    private static final String TAG = "ZT1_Service";
    private static final int ZT_NOTIFICATION_TAG = 5919812;
    private final Object configLock = new Object();
    private final IBinder mBinder = new ZeroTierBinder();
    private final DataStore dataStore = new DataStore(this);
    private final EventBus eventBus = EventBus.getDefault();
    private final long lastMulticastGroupCheck = 0;
    FileInputStream in;
    FileOutputStream out;
    DatagramSocket svrSocket;
    ParcelFileDescriptor vpnSocket;
    private int bindCount = 0;
    private boolean disableIPv6 = false;
    private int mStartID = -1;
    private VirtualNetworkConfig networkConfigs;
    private long networkId = 0;
    private long nextBackgroundTaskDeadline = 0;
    private Node node;
    private NotificationManager notificationManager;
    private TunTapAdapter tunTapAdapter;
    private UdpCom udpCom;
    private Thread udpThread;
    private boolean useDefaultRoute = false;
    private Thread v4multicastScanner = new Thread() {
        /* class com.zerotier.one.service.ZeroTierOneService.AnonymousClass1 */
        ArrayList<String> subscriptions = new ArrayList<>();

        public void run() {
            while (!isInterrupted()) {
                try {
                    ArrayList<String> arrayList = new ArrayList<>();
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/net/igmp"));
                        while (true) {
                            boolean z = false;
                            while (true) {
                                String readLine = bufferedReader.readLine();
                                if (readLine == null) {
                                    break;
                                }
                                String[] split = readLine.split("\\s+", -1);
                                if (!z && split[1].equals("tun0")) {
                                    z = true;
                                } else if (z && split[0].equals("")) {
                                    arrayList.add(split[1]);
                                }
                            }
                        }
                    } catch (FileNotFoundException unused) {
                        Log.e(ZeroTierOneService.TAG, "File Not Found: /proc/net/igmp");
                    } catch (IOException unused2) {
                        Log.e(ZeroTierOneService.TAG, "Error parsing /proc/net/igmp");
                    }
                    ArrayList<String> arrayList2 = new ArrayList<>(this.subscriptions);
                    ArrayList<String> arrayList3 = new ArrayList<>(arrayList);
                    arrayList3.removeAll(arrayList2);
                    for (String str : arrayList3) {
                        try {
                            byte[] hexStringToByteArray = ZeroTierOneService.this.hexStringToByteArray(str);
                            for (int i = 0; i < hexStringToByteArray.length / 2; i++) {
                                byte b = hexStringToByteArray[i];
                                hexStringToByteArray[i] = hexStringToByteArray[(hexStringToByteArray.length - i) - 1];
                                hexStringToByteArray[(hexStringToByteArray.length - i) - 1] = b;
                            }
                            ZeroTierOneService.this.node.multicastSubscribe(ZeroTierOneService.this.networkId, TunTapAdapter.multicastAddressToMAC(InetAddress.getByAddress(hexStringToByteArray)));
                        } catch (Exception ignored) {
                        }
                    }
                    arrayList2.removeAll(new ArrayList<>(arrayList));
                    for (String str2 : arrayList2) {
                        try {
                            byte[] hexStringToByteArray2 = ZeroTierOneService.this.hexStringToByteArray(str2);
                            for (int i2 = 0; i2 < hexStringToByteArray2.length / 2; i2++) {
                                byte b2 = hexStringToByteArray2[i2];
                                hexStringToByteArray2[i2] = hexStringToByteArray2[(hexStringToByteArray2.length - i2) - 1];
                                hexStringToByteArray2[(hexStringToByteArray2.length - i2) - 1] = b2;
                            }
                            ZeroTierOneService.this.node.multicastUnsubscribe(ZeroTierOneService.this.networkId, TunTapAdapter.multicastAddressToMAC(InetAddress.getByAddress(hexStringToByteArray2)));
                        } catch (Exception ignored) {
                        }
                    }
                    this.subscriptions = arrayList;
                    Thread.sleep(1000);
                } catch (InterruptedException unused5) {
                    Log.d(ZeroTierOneService.TAG, "V4 Multicast Scanner Thread Interrupted");
                    return;
                }
            }
        }
    };
    private Thread v6MulticastScanner = new Thread() {
        /* class com.zerotier.one.service.ZeroTierOneService.AnonymousClass2 */
        ArrayList<String> subscriptions = new ArrayList<>();

        public void run() {
            while (!isInterrupted()) {
                try {
                    ArrayList<String> arrayList = new ArrayList<>();
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/net/igmp6"));
                        while (true) {
                            String readLine = bufferedReader.readLine();
                            if (readLine == null) {
                                break;
                            }
                            String[] split = readLine.split("\\s+", -1);
                            if (split[1].equals("tun0")) {
                                arrayList.add(split[2]);
                            }
                        }
                    } catch (FileNotFoundException unused) {
                        Log.e(ZeroTierOneService.TAG, "File not found: /proc/net/igmp6");
                    } catch (IOException unused2) {
                        Log.e(ZeroTierOneService.TAG, "Error parsing /proc/net/igmp6");
                    }
                    ArrayList<String> arrayList2 = new ArrayList(this.subscriptions);
                    ArrayList<String> arrayList3 = new ArrayList(arrayList);
                    arrayList3.removeAll(arrayList2);
                    for (String str : arrayList3) {
                        try {
                            ZeroTierOneService.this.node.multicastSubscribe(ZeroTierOneService.this.networkId, TunTapAdapter.multicastAddressToMAC(InetAddress.getByAddress(ZeroTierOneService.this.hexStringToByteArray(str))));
                        } catch (Exception unused3) {
                        }
                    }
                    arrayList2.removeAll(new ArrayList(arrayList));
                    for (String str2 : arrayList2) {
                        try {
                            ZeroTierOneService.this.node.multicastUnsubscribe(ZeroTierOneService.this.networkId, TunTapAdapter.multicastAddressToMAC(InetAddress.getByAddress(ZeroTierOneService.this.hexStringToByteArray(str2))));
                        } catch (Exception unused4) {
                        }
                    }
                    this.subscriptions = arrayList;
                    Thread.sleep(1000);
                } catch (InterruptedException unused5) {
                    Log.d(ZeroTierOneService.TAG, "V6 Multicast Scanner Thread Interrupted");
                    return;
                }
            }
        }
    };
    private Thread vpnThread;

    private void logBindCount() {
        Log.i(TAG, "Bind Count: " + this.bindCount);
    }

    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Bound by: " + getPackageManager().getNameForUid(Binder.getCallingUid()));
        this.bindCount++;
        logBindCount();
        return this.mBinder;
    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbound by: " + getPackageManager().getNameForUid(Binder.getCallingUid()));
        this.bindCount--;
        logBindCount();
        return false;
    }

    /* access modifiers changed from: protected */
    public void setNextBackgroundTaskDeadline(long j) {
        synchronized (this) {
            this.nextBackgroundTaskDeadline = j;
        }
    }

    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long longValue;
        long j;
        Log.d(TAG, "onStartCommand");
        if (startId == 3) {
            Log.i(TAG, "Authorizing VPN");
            return START_NOT_STICKY;
        } else if (intent == null) {
            Log.e(TAG, "NULL intent.  Cannot start");
            return START_NOT_STICKY;
        } else {
            this.mStartID = startId;
            if (!this.eventBus.isRegistered(this)) {
                this.eventBus.register(this);
            }
            if (intent.hasExtra(ZT1_NETWORK_ID)) {
                longValue = intent.getLongExtra(ZT1_NETWORK_ID, 0);
                this.useDefaultRoute = intent.getBooleanExtra(ZT1_USE_DEFAULT_ROUTE, false);
            } else {
                DaoSession daoSession = ((AnalyticsApplication) getApplication()).getDaoSession();
                daoSession.clear();
                List<Network> list = daoSession.getNetworkDao().queryBuilder().where(NetworkDao.Properties.LastActivated.eq(true), new WhereCondition[0]).list();
                if (list == null || list.isEmpty()) {
                    Log.e(TAG, "Couldn't find last activated connection");
                    return START_NOT_STICKY;
                } else if (list.size() > 1) {
                    Log.e(TAG, "Multiple networks marked as last connected: " + list.size());
                    for (Network network : list) {
                        Log.e(TAG, "ID: " + Long.toHexString(network.getNetworkId()));
                    }
                    return START_NOT_STICKY;
                } else {
                    longValue = list.get(0).getNetworkId();
                    this.useDefaultRoute = list.get(0).getUseDefaultRoute();
                    Log.i(TAG, "Got Always On request for ZeroTier");
                }
            }
            if (longValue == 0) {
                Log.e(TAG, "Network ID not provided to service");
                stopSelf(startId);
                return START_NOT_STICKY;
            }
            this.networkId = longValue;
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean useCellularData = defaultSharedPreferences.getBoolean("network_use_cellular_data", false);
            this.disableIPv6 = defaultSharedPreferences.getBoolean("network_disable_ipv6", false);
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            int i3 = 0;
            while (activeNetworkInfo == null && i3 < 30) {
                try {
                    Log.i(TAG, "Waiting for network connectivity");
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                i3++;
            }
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting()) {
                Toast.makeText(this, R.string.toast_no_network, Toast.LENGTH_SHORT).show();
                return START_NOT_STICKY;
            } else if (useCellularData || !(activeNetworkInfo == null || activeNetworkInfo.getType() == 0)) {
                synchronized (this) {
                    try {
                        if (this.svrSocket == null) {
                            DatagramSocket datagramSocket = new DatagramSocket(null);
                            this.svrSocket = datagramSocket;
                            datagramSocket.setReuseAddress(true);
                            this.svrSocket.setSoTimeout(1000);
                            this.svrSocket.bind(new InetSocketAddress(9994));
                        }
                        if (!protect(this.svrSocket)) {
                            Log.e(TAG, "Error protecting UDP socket from feedback loop.");
                        }
                        if (this.node == null) {
                            try {
                                this.udpCom = new UdpCom(this, this.svrSocket);
                                this.tunTapAdapter = new TunTapAdapter(this, longValue);
                                long currentTimeMillis = System.currentTimeMillis();
                                DataStore dataStore2 = this.dataStore;
                                j = longValue;
                                Node node2 = new Node(currentTimeMillis, dataStore2, dataStore2, this.udpCom, this, this.tunTapAdapter, this, null);
                                this.node = node2;
                                NodeStatus status = node2.status();
                                long addres = status.getAddres();
                                AppNodeDao appNodeDao = ((AnalyticsApplication) getApplication()).getDaoSession().getAppNodeDao();
                                List<AppNode> list2 = appNodeDao.queryBuilder().build().forCurrentThread().list();
                                if (list2.isEmpty()) {
                                    AppNode appNode = new AppNode();
                                    appNode.setNodeId(addres);
                                    appNode.setNodeIdStr(String.format("%10x", addres));
                                    appNodeDao.insert(appNode);
                                } else {
                                    AppNode appNode2 = list2.get(0);
                                    appNode2.setNodeId(addres);
                                    appNode2.setNodeIdStr(String.format("%10x", addres));
                                    appNodeDao.save(appNode2);
                                }
                                this.eventBus.post(new NodeIDEvent(status.getAddres()));
                                this.udpCom.setNode(this.node);
                                this.tunTapAdapter.setNode(this.node);
                                Thread thread = new Thread(this.udpCom, "UDP Communication Thread");
                                this.udpThread = thread;
                                thread.start();
                            } catch (NodeException e) {
                                Log.e(TAG, "Error starting ZT1 Node", e);
                                return START_NOT_STICKY;
                            }
                        } else {
                            j = longValue;
                        }
                        if (this.vpnThread == null) {
                            Thread thread2 = new Thread(this, "ZeroTier Service Thread");
                            this.vpnThread = thread2;
                            thread2.start();
                        }
                        if (!this.udpThread.isAlive()) {
                            this.udpThread.start();
                        }
                    } catch (Exception e2) {
                        Log.e(TAG, e2.toString());
                        return START_NOT_STICKY;
                    }
                }
                joinNetwork(j, this.useDefaultRoute);
                return START_STICKY;
            } else {
                Toast.makeText(this, R.string.toast_mobile_data, Toast.LENGTH_SHORT).show();
                stopSelf(this.mStartID);
                Node node3 = this.node;
                if (node3 != null) {
                    node3.close();
                }
                return START_NOT_STICKY;
            }
        }
    }

    public void stopZeroTier() {
        Thread udpThread = this.udpThread;
        if (udpThread != null && udpThread.isAlive()) {
            this.udpThread.interrupt();
            this.udpThread = null;
        }
        TunTapAdapter tunTapAdapter = this.tunTapAdapter;
        if (tunTapAdapter != null && tunTapAdapter.isRunning()) {
            this.tunTapAdapter.interrupt();
            this.tunTapAdapter = null;
        }
        Thread vpnThread = this.vpnThread;
        if (vpnThread != null && vpnThread.isAlive()) {
            this.vpnThread.interrupt();
            this.vpnThread = null;
        }
        DatagramSocket svrSocket = this.svrSocket;
        if (svrSocket != null) {
            svrSocket.close();
            this.svrSocket = null;
        }
        Thread v4multicastScanner = this.v4multicastScanner;
        if (v4multicastScanner != null) {
            v4multicastScanner.interrupt();
            this.v4multicastScanner = null;
        }
        Thread v6MulticastScanner = this.v6MulticastScanner;
        if (v6MulticastScanner != null) {
            v6MulticastScanner.interrupt();
            this.v6MulticastScanner = null;
        }
        ParcelFileDescriptor vpnSocket = this.vpnSocket;
        if (vpnSocket != null) {
            try {
                vpnSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN socket", e);
            }
            this.vpnSocket = null;
        }
        if (this.node != null) {
            this.eventBus.post(new NodeDestroyedEvent());
            this.node.close();
            this.node = null;
        }
        if (this.eventBus.isRegistered(this)) {
            this.eventBus.unregister(this);
        }
        NotificationManager notificationManager = this.notificationManager;
        if (notificationManager != null) {
            notificationManager.cancel(ZT_NOTIFICATION_TAG);
        }
        if (!stopSelfResult(this.mStartID)) {
            Log.e(TAG, "stopSelfResult() failed!");
        }
    }

    public void onDestroy() {
        try {
            stopZeroTier();
            ParcelFileDescriptor parcelFileDescriptor = this.vpnSocket;
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing VPN socket", e);
                }
                this.vpnSocket = null;
            }
            stopSelf(this.mStartID);
            if (this.eventBus.isRegistered(this)) {
                this.eventBus.unregister(this);
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        } catch (Throwable th) {
            super.onDestroy();
            throw th;
        }
        super.onDestroy();
    }

    public void onRevoke() {
        stopZeroTier();
        ParcelFileDescriptor parcelFileDescriptor = this.vpnSocket;
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN socket", e);
            }
            this.vpnSocket = null;
        }
        stopSelf(this.mStartID);
        if (this.eventBus.isRegistered(this)) {
            this.eventBus.unregister(this);
        }
        super.onRevoke();
    }

    public void run() {
        Log.d(TAG, "ZeroTierOne Service Started");
        Log.d(TAG, "This Node Address: " + Long.toHexString(this.node.address()));
        while (!Thread.interrupted()) {
            try {
                long j = this.nextBackgroundTaskDeadline;
                long currentTimeMillis = System.currentTimeMillis();
                int i = (Long.compare(j, currentTimeMillis));
                if (i <= 0) {
                    long[] jArr = {0};
                    ResultCode processBackgroundTasks = this.node.processBackgroundTasks(currentTimeMillis, jArr);
                    synchronized (this) {
                        this.nextBackgroundTaskDeadline = jArr[0];
                    }
                    if (processBackgroundTasks != ResultCode.RESULT_OK) {
                        Log.e(TAG, "Error on processBackgroundTasks: " + processBackgroundTasks.toString());
                        shutdown();
                    }
                }
                Thread.sleep(i > 0 ? j - currentTimeMillis : 100);
            } catch (InterruptedException e) {
                Log.e(TAG, "ZeroTierOne Thread Interrupted", e);
                break;
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
        Log.d(TAG, "ZeroTierOne Service Ended");
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onStopEvent(StopEvent stopEvent) {
        stopZeroTier();
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onManualDisconnect(ManualDisconnectEvent manualDisconnectEvent) {
        stopZeroTier();
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onIsServiceRunning(IsServiceRunningEvent isServiceRunningEvent) {
        if (isServiceRunningEvent.type == IsServiceRunningEvent.Type.REQUEST) {
            this.eventBus.post(IsServiceRunningEvent.NewReply(true));
        }
    }

    /**
     * 加入网络
     * @param networkId
     * @param useDefaultRoute
     */
    public void joinNetwork(long networkId, boolean useDefaultRoute) {
        if (this.node == null) {
            Log.e(TAG, "Can't join network if ZeroTier isn't running");
            return;
        }
        // 如果已经加入网络，则退出
        VirtualNetworkConfig virtualNetworkConfig = this.networkConfigs;
        if (virtualNetworkConfig != null) {
            leaveNetwork(virtualNetworkConfig.networkId());
        }
        // 连接到新网络
        this.networkConfigs = null;
        this.useDefaultRoute = useDefaultRoute;
        ResultCode result = this.node.join(networkId);
        if (result != ResultCode.RESULT_OK) {
            this.eventBus.post(new ErrorEvent(result));
        }
    }

    public void leaveNetwork(long j) {
        Node node2 = this.node;
        if (node2 == null) {
            Log.e(TAG, "Can't leave network if ZeroTier isn't running");
            return;
        }
        ResultCode leave = node2.leave(j);
        if (leave != ResultCode.RESULT_OK) {
            this.eventBus.post(new ErrorEvent(leave));
            return;
        }
        VirtualNetworkConfig[] networks = this.node.networks();
        if (networks == null || (networks != null && networks.length == 0)) {
            stopZeroTier();
            ParcelFileDescriptor parcelFileDescriptor = this.vpnSocket;
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing VPN socket", e);
                }
                this.vpnSocket = null;
            }
            stopSelf(this.mStartID);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onNetworkInfoRequest(RequestNetworkInfoEvent requestNetworkInfoEvent) {
        VirtualNetworkConfig networkConfig;
        Node node2 = this.node;
        if (node2 != null && (networkConfig = node2.networkConfig(requestNetworkInfoEvent.getNetworkId())) != null) {
            this.eventBus.post(new NetworkInfoReplyEvent(networkConfig));
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onNetworkListRequest(RequestNetworkListEvent requestNetworkListEvent) {
        VirtualNetworkConfig[] networks;
        Node node2 = this.node;
        if (node2 != null && (networks = node2.networks()) != null && networks.length > 0) {
            this.eventBus.post(new NetworkListReplyEvent(networks));
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onNodeStatusRequest(RequestNodeStatusEvent requestNodeStatusEvent) {
        Node node2 = this.node;
        if (node2 != null) {
            this.eventBus.post(new NodeStatusEvent(node2.status()));
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onNetworkReconfigure(NetworkReconfigureEvent networkReconfigureEvent) {
        updateTunnelConfig();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDefaultrouteChanged(DefaultRouteChangedEvent defaultRouteChangedEvent) {
        this.useDefaultRoute = defaultRouteChangedEvent.isDefaultRoute();
        updateTunnelConfig();
    }

    @Override // com.zerotier.sdk.EventListener
    public void onEvent(Event event) {
        Log.d(TAG, "Event: " + event.toString());
        Node node2 = this.node;
        if (node2 != null) {
            this.eventBus.post(new NodeStatusEvent(node2.status()));
        }
    }

    @Override // com.zerotier.sdk.EventListener
    public void onTrace(String str) {
        Log.d(TAG, "Trace: " + str);
    }

    @Override // com.zerotier.sdk.VirtualNetworkConfigListener
    public int onNetworkConfigurationUpdated(long j, VirtualNetworkConfigOperation virtualNetworkConfigOperation, VirtualNetworkConfig virtualNetworkConfig) {
        Log.i(TAG, "Virtual Network Config Operation: " + virtualNetworkConfigOperation.toString());
        int i = AnonymousClass3.$SwitchMap$com$zerotier$sdk$VirtualNetworkConfigOperation[virtualNetworkConfigOperation.ordinal()];
        if (i == 1) {
            Log.d(TAG, "Network Type:" + virtualNetworkConfig.networkType().toString() + " Network Status: " + virtualNetworkConfig.networkStatus().toString() + " Network Name: " + virtualNetworkConfig.name() + " ");
            this.eventBus.post(new NetworkInfoReplyEvent(virtualNetworkConfig));
            TunTapAdapter tunTapAdapter2 = this.tunTapAdapter;
            if (tunTapAdapter2 == null) {
                return 0;
            }
            tunTapAdapter2.setNetworkConfig(virtualNetworkConfig);
            return 0;
        } else if (i == 2) {
            Log.i(TAG, "Network Config Update!");
            VirtualNetworkConfig virtualNetworkConfig2 = this.networkConfigs;
            if (virtualNetworkConfig2 == null) {
                Log.d(TAG, "Adding new network.");
                synchronized (this.configLock) {
                    this.networkConfigs = virtualNetworkConfig;
                }
                this.eventBus.post(new NetworkReconfigureEvent());
                this.eventBus.post(new NetworkInfoReplyEvent(virtualNetworkConfig));
                TunTapAdapter tunTapAdapter3 = this.tunTapAdapter;
                if (tunTapAdapter3 == null) {
                    return 0;
                }
                tunTapAdapter3.setNetworkConfig(virtualNetworkConfig);
                return 0;
            }
            if (!virtualNetworkConfig2.equals(virtualNetworkConfig)) {
                Log.i(TAG, "Network Config Changed.  Reconfiguring.");
                synchronized (this.configLock) {
                    this.networkConfigs = virtualNetworkConfig;
                }
                this.eventBus.post(new NetworkReconfigureEvent());
            }
            this.eventBus.post(new NetworkInfoReplyEvent(virtualNetworkConfig));
            TunTapAdapter tunTapAdapter4 = this.tunTapAdapter;
            if (tunTapAdapter4 == null) {
                return 0;
            }
            tunTapAdapter4.setNetworkConfig(virtualNetworkConfig);
            return 0;
        } else if (i == 3 || i == 4) {
            Log.d(TAG, "Network Down!");
            synchronized (this.configLock) {
                this.networkConfigs = null;
            }
            this.eventBus.post(new NetworkReconfigureEvent());
            TunTapAdapter tunTapAdapter5 = this.tunTapAdapter;
            if (tunTapAdapter5 == null) {
                return 0;
            }
            tunTapAdapter5.setNetworkConfig(null);
            return 0;
        } else {
            Log.e(TAG, "Unknown Network Config Operation!");
            return 0;
        }
    }

    /* access modifiers changed from: protected */
    public void shutdown() {
        stopZeroTier();
        ParcelFileDescriptor parcelFileDescriptor = this.vpnSocket;
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN socket", e);
            }
            this.vpnSocket = null;
        }
        stopSelf(this.mStartID);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:45:0x0175, code lost:
        if (r9 > r6) goto L_0x0179;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    // Decomp by jd-gui
    private void updateTunnelConfig() {
        try {
            synchronized (this.configLock) {
                if (this.networkConfigs == null)
                    return;
                if (this.tunTapAdapter.isRunning())
                    this.tunTapAdapter.interrupt();
                this.tunTapAdapter.clearRouteMap();
                ParcelFileDescriptor parcelFileDescriptor2 = this.vpnSocket;
                if (parcelFileDescriptor2 != null) {
                    try {
                        parcelFileDescriptor2.close();
                        this.in.close();
                        this.out.close();
                    } catch (Exception e) {
                        Log.e("ZT1_Service", "Error closing VPN socket", e);
                    }
                    this.vpnSocket = null;
                    this.in = null;
                    this.out = null;
                }
                Log.i("ZT1_Service", "Configuring VpnService.Builder");
                VpnService.Builder builder1 = new VpnService.Builder();
                long networkId = this.networkConfigs.networkId();
                InetSocketAddress[] assignedAddresses = this.networkConfigs.assignedAddresses();
                long l2 = 0L;
                int i = 0;
                int j;
                for (j = 0; j < assignedAddresses.length; j++) {
                    InetSocketAddress curAddress = assignedAddresses[j];
                    boolean bool = curAddress.getAddress() instanceof java.net.Inet6Address;
                    Log.d("ZT1_Service", "Adding VPN Address: " + curAddress.getAddress() + " Mac: " + Long.toHexString(this.networkConfigs.macAddress()));
                    byte[] rawAddress = curAddress.getAddress().getAddress();
                    long numAddress = l2;
                    try {
                        if (rawAddress.length == 4) {
                            numAddress = ByteBuffer.wrap(rawAddress).getInt();
                        }
                        int port = curAddress.getPort();
                        InetAddress curInetAddress = curAddress.getAddress();
                        if (!this.disableIPv6 || !(curInetAddress instanceof java.net.Inet6Address)) {
                            InetAddress curRouteAddress = InetAddressUtils.addressToRoute(curInetAddress, port);
                            if (curRouteAddress == null) {
                                Log.e("ZT1_Service", "NULL route calculated!");
                            } else {
                                ResultCode resultCode = null;
                                if (rawAddress.length == 4) {
                                    resultCode = this.node.multicastSubscribe(networkId, 0xffffffffffffL, numAddress);
                                } else {
                                    l2 = ByteBuffer.wrap(new byte[]{0, 0, 51, 51, -1, rawAddress[13], rawAddress[14], rawAddress[15]}).getLong();
                                    resultCode = this.node.multicastSubscribe(networkId, l2, numAddress);
                                }
                                if (resultCode != ResultCode.RESULT_OK) {
                                    Log.e("ZT1_Service", "Error joining multicast group");
                                } else {
                                    Log.d("ZT1_Service", "Joined multicast group");
                                }
                                builder1.addAddress(curInetAddress, port);
                                builder1.addRoute(curRouteAddress, port);
                                Route route = new Route(curRouteAddress, port);
                                this.tunTapAdapter.addRouteAndNetwork(route, networkId);
                                int m = this.networkConfigs.mtu();
                                port = i;
                                i = Math.max(m, port);
                            }
                        }
                        l2 = numAddress;
                    } catch (Exception e) {
                        Log.e("ZT1_Service", "Exception calculating multicast ADI", e);
                    }
                }
                InetAddress inetAddress1 = InetAddress.getByName("0.0.0.0");
                InetAddress inetAddress2 = InetAddress.getByName("::");
                if ((this.networkConfigs.routes()).length > 0) {
                    VirtualNetworkRoute[] arrayOfVirtualNetworkRoute = this.networkConfigs.routes();
                    for (j = 0; j < arrayOfVirtualNetworkRoute.length; j++) {
                        InetSocketAddress inetSocketAddress2 = (arrayOfVirtualNetworkRoute[j]).target;
                        InetSocketAddress inetSocketAddress1 = (arrayOfVirtualNetworkRoute[j]).via;
                        int k = inetSocketAddress2.getPort();
                        InetAddress inetAddress4 = inetSocketAddress2.getAddress();
                        InetAddress inetAddress3 = InetAddressUtils.addressToRoute(inetAddress4, k);
                        if ((!this.disableIPv6 || (!(inetAddress4 instanceof java.net.Inet6Address) && !(inetAddress3 instanceof java.net.Inet6Address))) && inetAddress3 != null && (this.useDefaultRoute || (!inetAddress3.equals(inetAddress1) && !inetAddress3.equals(inetAddress2)))) {
                            builder1.addRoute(inetAddress3, k);
                            Route route = new Route(inetAddress3, k);
                            if (inetSocketAddress1 != null)
                                route.setGateway(inetSocketAddress1.getAddress());
                            this.tunTapAdapter.addRouteAndNetwork(route, networkId);
                        }
                    }
                }
                builder1.addRoute(InetAddress.getByName("224.0.0.0"), 4);
                List<NetworkConfig> list = ((AnalyticsApplication) getApplication()).getDaoSession()
                        .getNetworkConfigDao().queryBuilder()
                        .where(NetworkConfigDao.Properties.Id.eq(networkId), new WhereCondition[0])
                        .build().forCurrentThread().list();
                if (list.isEmpty()) {
                    Log.e("ZT1_Service", "network config list empty?!?");
                } else if (list.size() > 1) {
                    Log.e("ZT1_Service", "network config list has more than 1?!?");
                } else {
                    j = list.get(0).getDnsMode();
                    if (j != 1) {
                        if (j == 2) {
                            for (DnsServer dnsServer : ((AnalyticsApplication) getApplication()).getDaoSession().getDnsServerDao().queryBuilder().where(DnsServerDao.Properties.NetworkId.eq(Long.valueOf(networkId)), new WhereCondition[0]).build().forCurrentThread().list()) {
                                String str = dnsServer.getNameserver();
                                try {
                                    InetAddress inetAddress = InetAddress.getByName(str);
                                    if (inetAddress instanceof java.net.Inet4Address) {
                                        builder1.addDnsServer(inetAddress);
                                        continue;
                                    }
                                    if (inetAddress instanceof java.net.Inet6Address && !this.disableIPv6)
                                        builder1.addDnsServer(inetAddress);
                                } catch (Exception e) {
                                    Log.e("ZT1_Service", "Exception parsing DNS server", e);
                                }
                            }
                        }
                    } else if (this.networkConfigs.dns() != null) {
                        builder1.addSearchDomain(this.networkConfigs.dns().getSearchDomain());
                        for (InetSocketAddress inetSocketAddress : this.networkConfigs.dns().getServers()) {
                            inetAddress1 = inetSocketAddress.getAddress();
                            if (inetAddress1 instanceof java.net.Inet4Address) {
                                builder1.addDnsServer(inetAddress1);
                                continue;
                            }
                            if (inetAddress1 instanceof java.net.Inet6Address && !this.disableIPv6)
                                builder1.addDnsServer(inetAddress1);
                        }
                    }
                }
                builder1.setMtu(i);
                builder1.setSession("ZeroTier One");
                if (Build.VERSION.SDK_INT >= 21 && !this.useDefaultRoute)
                    for (String str : DISALLOWED_APPS) {
                        try {
                            builder1.addDisallowedApplication(str);
                        } catch (Exception exception) {
                            Log.e("ZT1_Service", "Cannot disallow app", exception);
                        }
                    }
                ParcelFileDescriptor parcelFileDescriptor1 = builder1.establish();
                this.vpnSocket = parcelFileDescriptor1;
                if (parcelFileDescriptor1 == null) {
                    Log.e("ZT1_Service", "vpnSocket is NULL after builder.establish()!!!!");
                    stopZeroTier();
                    return;
                }
                this.in = new FileInputStream(this.vpnSocket.getFileDescriptor());
                this.out = new FileOutputStream(this.vpnSocket.getFileDescriptor());
                this.tunTapAdapter.setVpnSocket(this.vpnSocket);
                this.tunTapAdapter.setFileStreams(this.in, this.out);
                this.tunTapAdapter.startThreads();
                if (this.notificationManager == null)
                    this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= 26) {
                    NotificationChannel notificationChannel = new NotificationChannel("ZeroTierOne", "ZeroTier One", NotificationManager.IMPORTANCE_MIN);
                    notificationChannel.setDescription("Connected");
                    this.notificationManager.createNotificationChannel(notificationChannel);
                }
                Intent intent = new Intent(this, NetworkListActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ZeroTierOne");
                String contentText = "Connected to " + Long.toHexString(this.networkConfigs.networkId());
                Notification notification = builder.setOngoing(true)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Connected")
                        .setContentText(contentText)
                        .setSubText("")
                        .setContentIntent(pendingIntent).build();
                this.notificationManager.notify(ZT_NOTIFICATION_TAG, notification);
                Log.i("ZT1_Service", "ZeroTier One Connected");
                Thread thread = this.v4multicastScanner;
                if (!((thread != null) && !thread.isAlive()) && Build.VERSION.SDK_INT < 29)
                    this.v4multicastScanner.start();
                if (!this.disableIPv6) {
                    thread = this.v6MulticastScanner;
                    if (thread != null && !thread.isAlive() && Build.VERSION.SDK_INT < 29)
                        this.v6MulticastScanner.start();
                }
            }
        } catch (Exception exception) {
            Log.e("ZT1_Service", "Exception setting up VPN port", exception);
        }
    }

    /* access modifiers changed from: package-private */
    public byte[] hexStringToByteArray(String str) {
        int length = str.length();
        byte[] bArr = new byte[(length / 2)];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }

    /* renamed from: com.zerotier.one.service.ZeroTierOneService$3  reason: invalid class name */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$com$zerotier$sdk$VirtualNetworkConfigOperation;

        static {
            $SwitchMap$com$zerotier$sdk$VirtualNetworkConfigOperation = new int[VirtualNetworkConfigOperation.values().length];
            $SwitchMap$com$zerotier$sdk$VirtualNetworkConfigOperation[VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_UP.ordinal()] = 1;
            $SwitchMap$com$zerotier$sdk$VirtualNetworkConfigOperation[VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_CONFIG_UPDATE.ordinal()] = 2;
            $SwitchMap$com$zerotier$sdk$VirtualNetworkConfigOperation[VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_DOWN.ordinal()] = 3;
            $SwitchMap$com$zerotier$sdk$VirtualNetworkConfigOperation[VirtualNetworkConfigOperation.VIRTUAL_NETWORK_CONFIG_OPERATION_DESTROY.ordinal()] = 4;
        }
    }

    public class ZeroTierBinder extends Binder {
        public ZeroTierBinder() {
        }

        public ZeroTierOneService getService() {
            return ZeroTierOneService.this;
        }
    }
}

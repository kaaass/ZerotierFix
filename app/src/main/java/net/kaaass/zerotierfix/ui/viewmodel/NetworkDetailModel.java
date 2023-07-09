package net.kaaass.zerotierfix.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.zerotier.sdk.VirtualNetworkConfig;

import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.events.DefaultRouteChangedEvent;
import net.kaaass.zerotierfix.events.NetworkConfigChangedByUserEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigChangedEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigReplyEvent;
import net.kaaass.zerotierfix.events.VirtualNetworkConfigRequestEvent;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.model.NetworkConfig;
import net.kaaass.zerotierfix.model.NetworkDao;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 网络详情的 ViewModel
 */
public class NetworkDetailModel extends AndroidViewModel {
    private static final String TAG = "NetworkDetailViewModel";
    private final MutableLiveData<Network> network = new MutableLiveData<>();
    private final MutableLiveData<NetworkConfig> networkConfig = new MutableLiveData<>();
    private final MutableLiveData<VirtualNetworkConfig> virtualNetworkConfig = new MutableLiveData<>();
    private final EventBus eventBus = EventBus.getDefault();
    private long networkId = -1;

    public NetworkDetailModel(@NonNull Application application) {
        super(application);
        // 注册 EventBus
        this.eventBus.register(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 取消注册 EventBus
        this.eventBus.unregister(this);
    }

    /**
     * 获得指定网络的全部网络信息
     */
    public void doRetrieveDetail(long networkId) {
        this.networkId = networkId;
        doRetrieveNetworkAndConfig();
        doRetrieveVirtualNetworkConfig();
    }

    /**
     * 获得持久化的网络配置
     */
    private void doRetrieveNetworkAndConfig() {
        // 在 DB 中查询网络对象
        var networkDao = ((ZerotierFixApplication) getApplication())
                .getDaoSession().getNetworkDao();
        var queryResult = networkDao.queryBuilder()
                .where(NetworkDao.Properties.NetworkId.eq(this.networkId))
                .build().forCurrentThread().list();
        if (queryResult.size() > 1) {
            Log.e(TAG, "Data inconsistency error.  More than one network with a single ID!");
            return;
        } else if (queryResult.size() < 1) {
            Log.e(TAG, "Network not found!");
            return;
        }

        // 更新 LiveData
        Network network = queryResult.get(0);
        this.network.setValue(network);
        this.networkConfig.setValue(network.getNetworkConfig());
    }

    /**
     * 向 ZT 请求网络配置
     */
    private void doRetrieveVirtualNetworkConfig() {
        this.eventBus.post(new VirtualNetworkConfigRequestEvent(this.networkId));
    }

    /**
     * 更新是否通过 ZeroTier 进行路由的网络设置
     */
    public void doUpdateRouteViaZeroTier(boolean routeViaZeroTier) {
        var networkConfig = this.networkConfig.getValue();
        if (networkConfig == null) {
            Log.e(TAG, "Network config not found!");
            return;
        }

        // 更新数据库
        networkConfig.setRouteViaZeroTier(routeViaZeroTier);
        networkConfig.update();

        // 触发事件
        this.eventBus.post(new DefaultRouteChangedEvent(this.networkId, routeViaZeroTier));
    }

    /**
     * 处理网络配置回复事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVirtualNetworkConfigReply(VirtualNetworkConfigReplyEvent event) {
        var config = event.getVirtualNetworkConfig();
        if (config == null) {
            Log.e(TAG, "Virtual network config not found!");
            return;
        }
        if (config.getNwid() != this.networkId) {
            return;
        }
        // 更新 LiveData
        this.virtualNetworkConfig.setValue(config);
        // 因为网络名称可能会变化，但 Network DAO 获取的对象会自动更新，所以需要主动触发一次更新
        var network = this.network.getValue();
        this.network.setValue(network);
    }

    /**
     * 处理网络配置更新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVirtualNetworkConfigChanged(VirtualNetworkConfigChangedEvent event) {
        // 和 VirtualNetworkConfigReplyEvent 一样处理
        this.onVirtualNetworkConfigReply(new VirtualNetworkConfigReplyEvent(event.getVirtualNetworkConfig()));
    }

    /**
     * 处理本地网络设置更新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetworkConfigChangedByUser(NetworkConfigChangedByUserEvent event) {
        this.networkConfig.setValue(event.getNetwork().getNetworkConfig());
    }

    public LiveData<Network> getNetwork() {
        return network;
    }

    public LiveData<NetworkConfig> getNetworkConfig() {
        return networkConfig;
    }

    public LiveData<VirtualNetworkConfig> getVirtualNetworkConfig() {
        return virtualNetworkConfig;
    }
}

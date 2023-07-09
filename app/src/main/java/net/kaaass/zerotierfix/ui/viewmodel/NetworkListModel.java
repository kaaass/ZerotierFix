package net.kaaass.zerotierfix.ui.viewmodel;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.greenrobot.eventbus.EventBus;

import lombok.Getter;
import lombok.Setter;

/**
 * 网络列表的 ViewModel
 */
public class NetworkListModel extends ViewModel {
    /**
     * TODO: 将控制逻辑移动到 ViewModel，移除此字段
     */
    @Getter
    @Setter
    private long networkId;
    private MutableLiveData<Long> connectNetworkId = new MutableLiveData<>();

    /**
     * 切换连接的网络
     * TODO: 将控制逻辑移动到 ViewModel，移除此方法
     */
    @WorkerThread
    public void doChangeConnectNetwork(Long networkId) {
        this.connectNetworkId.postValue(networkId);
    }

    public LiveData<Long> getConnectNetworkId() {
        return connectNetworkId;
    }
}

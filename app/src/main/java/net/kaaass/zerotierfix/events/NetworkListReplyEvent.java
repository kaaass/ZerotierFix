package net.kaaass.zerotierfix.events;

import com.zerotier.sdk.VirtualNetworkConfig;

public class NetworkListReplyEvent {
    private final VirtualNetworkConfig[] networks;

    public NetworkListReplyEvent(VirtualNetworkConfig[] virtualNetworkConfigArr) {
        this.networks = virtualNetworkConfigArr;
    }

    public VirtualNetworkConfig[] getNetworkList() {
        return this.networks;
    }
}

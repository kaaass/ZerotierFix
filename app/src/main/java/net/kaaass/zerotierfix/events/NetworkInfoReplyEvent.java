package net.kaaass.zerotierfix.events;

import com.zerotier.sdk.VirtualNetworkConfig;

public class NetworkInfoReplyEvent {
    private final VirtualNetworkConfig vnc;

    public NetworkInfoReplyEvent(VirtualNetworkConfig virtualNetworkConfig) {
        this.vnc = virtualNetworkConfig;
    }

    public VirtualNetworkConfig getNetworkInfo() {
        return this.vnc;
    }
}

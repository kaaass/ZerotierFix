package net.kaaass.zerotierfix.events;

public class JoinNetworkEvent {
    private boolean defaultRoute;
    private long networkId;

    public JoinNetworkEvent(long j, boolean z) {
        this.networkId = j;
        this.defaultRoute = z;
    }

    public long getNetworkId() {
        return this.networkId;
    }

    public boolean isDefaultRoute() {
        return this.defaultRoute;
    }
}

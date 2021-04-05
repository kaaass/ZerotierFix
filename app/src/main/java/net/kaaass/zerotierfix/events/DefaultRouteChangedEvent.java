package net.kaaass.zerotierfix.events;

public class DefaultRouteChangedEvent {
    private final boolean isDefaultRoute;
    private final long networkId;

    public DefaultRouteChangedEvent(long j, boolean z) {
        this.networkId = j;
        this.isDefaultRoute = z;
    }

    public long getNetworkId() {
        return this.networkId;
    }

    public boolean isDefaultRoute() {
        return this.isDefaultRoute;
    }
}

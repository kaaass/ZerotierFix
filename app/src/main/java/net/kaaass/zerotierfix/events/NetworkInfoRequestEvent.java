package net.kaaass.zerotierfix.events;

@Deprecated
public class NetworkInfoRequestEvent {
    private final long networkId;

    public NetworkInfoRequestEvent(long j) {
        this.networkId = j;
    }

    public long getNetworkId() {
        return this.networkId;
    }
}

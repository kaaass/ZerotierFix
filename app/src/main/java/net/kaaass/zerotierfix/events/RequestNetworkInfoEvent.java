package net.kaaass.zerotierfix.events;

public class RequestNetworkInfoEvent {
    private final long networkId;

    public RequestNetworkInfoEvent(long j) {
        this.networkId = j;
    }

    public long getNetworkId() {
        return this.networkId;
    }
}

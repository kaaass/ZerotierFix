package net.kaaass.zerotierfix.events;

public class NodeIDEvent {
    private long nodeId;

    public NodeIDEvent(long j) {
        this.nodeId = j;
    }

    public long getNodeId() {
        return this.nodeId;
    }
}

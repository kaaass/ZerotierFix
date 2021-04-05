package net.kaaass.zerotierfix.events;

import com.zerotier.sdk.NodeStatus;

public class NodeStatusEvent {
    private NodeStatus status;

    public NodeStatusEvent(NodeStatus nodeStatus) {
        this.status = nodeStatus;
    }

    public NodeStatus getStatus() {
        return this.status;
    }
}

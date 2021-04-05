package net.kaaass.zerotierfix.model;

public class AppNode {
    private Long nodeId;
    private String nodeIdStr;

    public AppNode(Long l, String str) {
        this.nodeId = l;
        this.nodeIdStr = str;
    }

    public AppNode() {
    }

    public Long getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(Long l) {
        this.nodeId = l;
    }

    public String getNodeIdStr() {
        return this.nodeIdStr;
    }

    public void setNodeIdStr(String str) {
        this.nodeIdStr = str;
    }
}

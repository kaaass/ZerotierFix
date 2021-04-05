package net.kaaass.zerotierfix.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity
public class AppNode {

    @Id
    private Long nodeId;

    private String nodeIdStr;

    @Generated(hash = 1756503982)
    public AppNode(Long nodeId, String nodeIdStr) {
        this.nodeId = nodeId;
        this.nodeIdStr = nodeIdStr;
    }

    @Generated(hash = 714894196)
    public AppNode() {
    }

    public Long getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeIdStr() {
        return this.nodeIdStr;
    }

    public void setNodeIdStr(String nodeIdStr) {
        this.nodeIdStr = nodeIdStr;
    }
}

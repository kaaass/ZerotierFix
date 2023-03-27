package net.kaaass.zerotierfix.events;

import com.zerotier.sdk.NodeStatus;
import com.zerotier.sdk.Version;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 节点状态事件
 *
 * 在遇到 {@link NodeStatusRequestEvent} 或 Zerotier 事件时触发
 */
@Data
@AllArgsConstructor
public class NodeStatusEvent {

    /**
     * 节点自身状态
     */
    private final NodeStatus status;

    /**
     * 客户端 Zerotier 程序版本
     */
    private final Version clientVersion;
}

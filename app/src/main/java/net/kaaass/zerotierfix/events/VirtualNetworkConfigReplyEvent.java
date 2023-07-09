package net.kaaass.zerotierfix.events;

import com.zerotier.sdk.VirtualNetworkConfig;

import lombok.Data;

/**
 * 应答获取指定 ZT 网络 ID 的网络配置事件
 */
@Data
public class VirtualNetworkConfigReplyEvent {
    private final VirtualNetworkConfig virtualNetworkConfig;
}

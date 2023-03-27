package net.kaaass.zerotierfix.events;

import com.zerotier.sdk.VirtualNetworkConfig;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * ZT 网络配置成功更改事件
 */
@Data
@AllArgsConstructor
public class VirtualNetworkConfigChangedEvent {
    private final VirtualNetworkConfig virtualNetworkConfig;
}

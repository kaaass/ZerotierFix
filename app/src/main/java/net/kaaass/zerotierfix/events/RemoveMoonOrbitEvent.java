package net.kaaass.zerotierfix.events;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 删除 Moon 入轨信息事件
 *
 * @author kaaass
 */
@Data
@AllArgsConstructor
public class RemoveMoonOrbitEvent {

    /**
     * Moon 地址
     */
    private Long moonWorldId;

    /**
     * Moon 种子
     */
    private Long moonSeed;
}

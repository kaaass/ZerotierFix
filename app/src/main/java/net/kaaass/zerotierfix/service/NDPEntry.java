package net.kaaass.zerotierfix.service;

import java.net.InetAddress;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * NDP 表项。记录 MAC 与 IPv6 地址的对应关系及记录时间
 */
@Data
public class NDPEntry {
    private final long mac;
    private final InetAddress address;
    @Setter(AccessLevel.NONE)
    private long time;

    NDPEntry(long j, InetAddress inetAddress) {
        this.mac = j;
        this.address = inetAddress;
        updateTime();
    }

    public void updateTime() {
        this.time = System.currentTimeMillis();
    }
}

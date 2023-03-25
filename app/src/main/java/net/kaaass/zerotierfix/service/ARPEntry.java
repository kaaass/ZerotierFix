package net.kaaass.zerotierfix.service;

import java.net.InetAddress;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * ARP 表项。记录 MAC 与 IPv4 地址的对应关系及记录时间
 */
@Data
public class ARPEntry {
    private final long mac;
    private final InetAddress address;
    @Setter(AccessLevel.NONE)
    private long time;

    ARPEntry(long mac, InetAddress inetAddress) {
        this.mac = mac;
        this.address = inetAddress;
        updateTime();
    }

    public void updateTime() {
        this.time = System.currentTimeMillis();
    }
}

package net.kaaass.zerotierfix.service;

import java.net.InetAddress;

import lombok.Data;

/**
 * ARP 应答报文的所需数据。由于报文内容总是当前节点的 IP 与 MAC，因此仅记录应答报文目标的信息。
 */
@Data
public class ARPReplyData {
    private final long destMac;
    private final InetAddress destAddress;
}

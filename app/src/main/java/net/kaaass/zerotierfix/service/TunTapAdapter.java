package net.kaaass.zerotierfix.service;

import android.os.ParcelFileDescriptor;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.zerotier.sdk.Node;
import com.zerotier.sdk.ResultCode;
import com.zerotier.sdk.VirtualNetworkConfig;
import com.zerotier.sdk.VirtualNetworkFrameListener;
import com.zerotier.sdk.util.StringUtils;

import net.kaaass.zerotierfix.util.DebugLog;
import net.kaaass.zerotierfix.util.IPPacketUtils;
import net.kaaass.zerotierfix.util.InetAddressUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Objects;

// TODO: clear up
public class TunTapAdapter implements VirtualNetworkFrameListener {
    public static final String TAG = "TunTapAdapter";
    private static final int ARP_PACKET = 2054;
    private static final int IPV4_PACKET = 2048;
    private static final int IPV6_PACKET = 34525;
    private final HashMap<Route, Long> routeMap = new HashMap<>();
    private final long networkId;
    private final ZeroTierOneService ztService;
    private ARPTable arpTable = new ARPTable();
    private FileInputStream in;
    private NDPTable ndpTable = new NDPTable();
    private Node node;
    private FileOutputStream out;
    private Thread receiveThread;
    private ParcelFileDescriptor vpnSocket;

    public TunTapAdapter(ZeroTierOneService zeroTierOneService, long j) {
        this.ztService = zeroTierOneService;
        this.networkId = j;
    }

    public static long multicastAddressToMAC(InetAddress inetAddress) {
        if (inetAddress instanceof Inet4Address) {
            byte[] address = inetAddress.getAddress();
            return ByteBuffer.wrap(new byte[]{0, 0, 1, 0, 94, (byte) (address[1] & Byte.MAX_VALUE), address[2], address[3]}).getLong();
        } else if (!(inetAddress instanceof Inet6Address)) {
            return 0;
        } else {
            byte[] address2 = inetAddress.getAddress();
            return ByteBuffer.wrap(new byte[]{0, 0, 51, 51, address2[12], address2[13], address2[14], address2[15]}).getLong();
        }
    }

    private void addMulticastRoutes() {
    }

    public void setNode(Node node) {
        this.node = node;
        try {
            var multicastAddress = InetAddress.getByName("224.224.224.224");
            var result = node
                    .multicastSubscribe(this.networkId, multicastAddressToMAC(multicastAddress));
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error when calling multicastSubscribe: " + result);
            }
        } catch (UnknownHostException e) {
            Log.e(TAG, e.toString(), e);
        }
    }

    public void setVpnSocket(ParcelFileDescriptor vpnSocket) {
        this.vpnSocket = vpnSocket;
    }

    public void setFileStreams(FileInputStream fileInputStream, FileOutputStream fileOutputStream) {
        this.in = fileInputStream;
        this.out = fileOutputStream;
    }

    public void addRouteAndNetwork(Route route, long networkId) {
        synchronized (this.routeMap) {
            this.routeMap.put(route, networkId);
        }
    }

    public void clearRouteMap() {
        synchronized (this.routeMap) {
            this.routeMap.clear();
            addMulticastRoutes();
        }
    }

    private boolean isIPv4Multicast(InetAddress inetAddress) {
        return (inetAddress.getAddress()[0] & 0xF0) == 224;
    }

    private boolean isIPv6Multicast(InetAddress inetAddress) {
        return (inetAddress.getAddress()[0] & 0xFF) == 0xFF;
    }

    public void startThreads() {
        this.receiveThread = new Thread("Tunnel Receive Thread") {

            @Override
            public void run() {
                // 创建 ARP、NDP 表
                if (TunTapAdapter.this.ndpTable == null) {
                    TunTapAdapter.this.ndpTable = new NDPTable();
                }
                if (TunTapAdapter.this.arpTable == null) {
                    TunTapAdapter.this.arpTable = new ARPTable();
                }
                // 转发 TUN 消息至 Zerotier
                try {
                    Log.d(TunTapAdapter.TAG, "TUN Receive Thread Started");
                    var buffer = ByteBuffer.allocate(32767);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    while (!isInterrupted()) {
                        try {
                            boolean noDataBeenRead = true;
                            int readCount = TunTapAdapter.this.in.read(buffer.array());
                            if (readCount > 0) {
                                DebugLog.d(TunTapAdapter.TAG, "Sending packet to ZeroTier. " + readCount + " bytes.");
                                var readData = new byte[readCount];
                                System.arraycopy(buffer.array(), 0, readData, 0, readCount);
                                byte iPVersion = IPPacketUtils.getIPVersion(readData);
                                if (iPVersion == 4) {
                                    TunTapAdapter.this.handleIPv4Packet(readData);
                                } else if (iPVersion == 6) {
                                    TunTapAdapter.this.handleIPv6Packet(readData);
                                } else {
                                    Log.e(TunTapAdapter.TAG, "Unknown IP version");
                                }
                                buffer.clear();
                                noDataBeenRead = false;
                            }
                            if (noDataBeenRead) {
                                Thread.sleep(10);
                            }
                        } catch (IOException e) {
                            Log.e(TunTapAdapter.TAG, "Error in TUN Receive: " + e.getMessage(), e);
                        }
                    }
                } catch (InterruptedException ignored) {
                }
                Log.d(TunTapAdapter.TAG, "TUN Receive Thread ended");
                // 关闭 ARP、NDP 表
                TunTapAdapter.this.ndpTable.stop();
                TunTapAdapter.this.ndpTable = null;
                TunTapAdapter.this.arpTable.stop();
                TunTapAdapter.this.arpTable = null;
            }
        };
        this.receiveThread.start();
    }

    private void handleIPv4Packet(byte[] packetData) {
        boolean isMulticast;
        long destMac;
        var destIP = IPPacketUtils.getDestIP(packetData);
        var sourceIP = IPPacketUtils.getSourceIP(packetData);
        var virtualNetworkConfig = this.ztService.getVirtualNetworkConfig(this.networkId);

        if (virtualNetworkConfig == null) {
            Log.e(TAG, "TunTapAdapter has no network config yet");
            return;
        } else if (destIP == null) {
            Log.e(TAG, "destAddress is null");
            return;
        } else if (sourceIP == null) {
            Log.e(TAG, "sourceAddress is null");
            return;
        }
        if (isIPv4Multicast(destIP)) {
            var result = this.node.multicastSubscribe(this.networkId, multicastAddressToMAC(destIP));
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error when calling multicastSubscribe: " + result);
            }
            isMulticast = true;
        } else {
            isMulticast = false;
        }
        var route = routeForDestination(destIP);
        var gateway = route != null ? route.getGateway() : null;

        // 查找当前节点的 v4 地址
        var ztAddresses = virtualNetworkConfig.getAssignedAddresses();
        InetAddress localV4Address = null;
        int cidr = 0;

        for (var address : ztAddresses) {
            if (address.getAddress() instanceof Inet4Address) {
                localV4Address = address.getAddress();
                cidr = address.getPort();
                break;
            }
        }

        var destRoute = InetAddressUtils.addressToRouteNo0Route(destIP, cidr);
        var sourceRoute = InetAddressUtils.addressToRouteNo0Route(sourceIP, cidr);
        if (gateway != null && !Objects.equals(destRoute, sourceRoute)) {
            destIP = gateway;
        }
        if (localV4Address == null) {
            Log.e(TAG, "Couldn't determine local address");
            return;
        }

        long localMac = virtualNetworkConfig.getMac();
        long[] nextDeadline = new long[1];
        if (isMulticast || this.arpTable.hasMacForAddress(destIP)) {
            // 已确定目标 MAC，直接发送
            if (isIPv4Multicast(destIP)) {
                destMac = multicastAddressToMAC(destIP);
            } else {
                destMac = this.arpTable.getMacForAddress(destIP);
            }
            var result = this.node.processVirtualNetworkFrame(System.currentTimeMillis(), this.networkId, localMac, destMac, IPV4_PACKET, 0, packetData, nextDeadline);
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error calling processVirtualNetworkFrame: " + result.toString());
                return;
            }
            Log.d(TAG, "Packet sent to ZT");
            this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0]);
        } else {
            // 目标 MAC 未知，进行 ARP 查询
            Log.d(TAG, "Unknown dest MAC address.  Need to look it up. " + destIP);
            destMac = InetAddressUtils.BROADCAST_MAC_ADDRESS;
            packetData = this.arpTable.getRequestPacket(localMac, localV4Address, destIP);
            var result = this.node.processVirtualNetworkFrame(System.currentTimeMillis(), this.networkId, localMac, destMac, ARP_PACKET, 0, packetData, nextDeadline);
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error sending ARP packet: " + result.toString());
                return;
            }
            Log.d(TAG, "ARP Request Sent!");
            this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0]);
        }
    }

    private void handleIPv6Packet(byte[] packetData) {
        var destIP = IPPacketUtils.getDestIP(packetData);
        var sourceIP = IPPacketUtils.getSourceIP(packetData);
        var virtualNetworkConfig = this.ztService.getVirtualNetworkConfig(this.networkId);

        if (virtualNetworkConfig == null) {
            Log.e(TAG, "TunTapAdapter has no network config yet");
            return;
        } else if (destIP == null) {
            Log.e(TAG, "destAddress is null");
            return;
        } else if (sourceIP == null) {
            Log.e(TAG, "sourceAddress is null");
            return;
        }
        if (this.isIPv6Multicast(destIP)) {
            var result = this.node.multicastSubscribe(this.networkId, multicastAddressToMAC(destIP));
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error when calling multicastSubscribe: " + result);
            }
        }
        var route = routeForDestination(destIP);
        var gateway = route != null ? route.getGateway() : null;

        // 查找当前节点的 v6 地址
        var ztAddresses = virtualNetworkConfig.getAssignedAddresses();
        InetAddress localV4Address = null;
        int cidr = 0;

        for (var address : ztAddresses) {
            if (address.getAddress() instanceof Inet6Address) {
                localV4Address = address.getAddress();
                cidr = address.getPort();
                break;
            }
        }

        var destRoute = InetAddressUtils.addressToRouteNo0Route(destIP, cidr);
        var sourceRoute = InetAddressUtils.addressToRouteNo0Route(sourceIP, cidr);
        if (gateway != null && !Objects.equals(destRoute, sourceRoute)) {
            destIP = gateway;
        }
        if (localV4Address == null) {
            Log.e(TAG, "Couldn't determine local address");
            return;
        }

        long localMac = virtualNetworkConfig.getMac();
        long[] nextDeadline = new long[1];

        // 确定目标 MAC 地址
        long destMac;
        boolean sendNSPacket = false;
        if (this.isNeighborSolicitation(packetData)) {
            // 收到本地 NS 报文，根据 NDP 表记录确定是否广播查询
            if (this.ndpTable.hasMacForAddress(destIP)) {
                destMac = this.ndpTable.getMacForAddress(destIP);
            } else {
                destMac = InetAddressUtils.ipv6ToMulticastAddress(destIP);
            }
        } else if (this.isIPv6Multicast(destIP)) {
            // 多播报文
            destMac = multicastAddressToMAC(destIP);
        } else if (this.isNeighborAdvertisement(packetData)) {
            // 收到本地 NA 报文
            if (this.ndpTable.hasMacForAddress(destIP)) {
                destMac = this.ndpTable.getMacForAddress(destIP);
            } else {
                // 目标 MAC 未知，不发送数据包
                destMac = 0L;
            }
            sendNSPacket = true;
        } else {
            // 收到普通数据包，根据 NDP 表记录确定是否发送 NS 请求
            if (this.ndpTable.hasMacForAddress(destIP)) {
                // 目标地址 MAC 已知
                destMac = this.ndpTable.getMacForAddress(destIP);
            } else {
                destMac = 0L;
                sendNSPacket = true;
            }
        }
        // 发送数据包
        if (destMac != 0L) {
            var result = this.node.processVirtualNetworkFrame(System.currentTimeMillis(), this.networkId, localMac, destMac, IPV6_PACKET, 0, packetData, nextDeadline);
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error calling processVirtualNetworkFrame: " + result.toString());
            } else {
                DebugLog.d(TAG, "Packet sent to ZT");
                this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0]);
            }
        }
        // 发送 NS 请求
        if (sendNSPacket) {
            if (destMac == 0L) {
                destMac = InetAddressUtils.ipv6ToMulticastAddress(destIP);
            }
            Log.d(TAG, "Sending Neighbor Solicitation");
            packetData = this.ndpTable.getNeighborSolicitationPacket(sourceIP, destIP, localMac);
            var result = this.node.processVirtualNetworkFrame(System.currentTimeMillis(), this.networkId, localMac, destMac, IPV6_PACKET, 0, packetData, nextDeadline);
            if (result != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error calling processVirtualNetworkFrame: " + result.toString());
            } else {
                Log.d(TAG, "Neighbor Solicitation sent to ZT");
                this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0]);
            }
        }

    }

    public void interrupt() {
        if (this.receiveThread != null) {
            try {
                this.in.close();
                this.out.close();
            } catch (IOException e) {
                Log.e(TAG, "Error stopping in/out: " + e.getMessage(), e);
            }
            this.receiveThread.interrupt();
            try {
                this.receiveThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void join() throws InterruptedException {
        this.receiveThread.join();
    }

    private boolean isNeighborSolicitation(byte[] packetData) {
        return packetData[6] == 58 && packetData[40] == -121;
    }

    private boolean isNeighborAdvertisement(byte[] packetData) {
        return packetData[6] == 58 && packetData[40] == -120;
    }

    public boolean isRunning() {
        var thread = this.receiveThread;
        if (thread == null) {
            return false;
        }
        return thread.isAlive();
    }

    /**
     * 响应并处理 ZT 网络发送至本节点的以太网帧
     */
    @Override
    public void onVirtualNetworkFrame(long networkId, long srcMac, long destMac, long etherType,
                                      long vlanId, byte[] frameData) {
        DebugLog.d(TAG, "Got Virtual Network Frame. " +
                " Network ID: " + StringUtils.networkIdToString(networkId) +
                " Source MAC: " + StringUtils.macAddressToString(srcMac) +
                " Dest MAC: " + StringUtils.macAddressToString(destMac) +
                " Ether type: " + StringUtils.etherTypeToString(etherType) +
                " VLAN ID: " + vlanId + " Frame Length: " + frameData.length);
        if (this.vpnSocket == null) {
            Log.e(TAG, "vpnSocket is null!");
        } else if (this.in == null || this.out == null) {
            Log.e(TAG, "no in/out streams");
        } else if (etherType == ARP_PACKET) {
            // 收到 ARP 包。更新 ARP 表，若需要则进行应答
            Log.d(TAG, "Got ARP Packet");
            var arpReply = this.arpTable.processARPPacket(frameData);
            if (arpReply != null && arpReply.getDestMac() != 0 && arpReply.getDestAddress() != null) {
                // 获取本地 V4 地址
                var networkConfig = this.node.networkConfig(networkId);
                InetAddress localV4Address = null;
                for (var address : networkConfig.getAssignedAddresses()) {
                    if (address.getAddress() instanceof Inet4Address) {
                        localV4Address = address.getAddress();
                        break;
                    }
                }
                // 构造并返回 ARP 应答
                if (localV4Address != null) {
                    var nextDeadline = new long[1];
                    var packetData = this.arpTable.getReplyPacket(networkConfig.getMac(),
                            localV4Address, arpReply.getDestMac(), arpReply.getDestAddress());
                    var result = this.node
                            .processVirtualNetworkFrame(System.currentTimeMillis(), networkId,
                                    networkConfig.getMac(), srcMac, ARP_PACKET, 0,
                                    packetData, nextDeadline);
                    if (result != ResultCode.RESULT_OK) {
                        Log.e(TAG, "Error sending ARP packet: " + result.toString());
                        return;
                    }
                    Log.d(TAG, "ARP Reply Sent!");
                    this.ztService.setNextBackgroundTaskDeadline(nextDeadline[0]);
                }
            }
        } else if (etherType == IPV4_PACKET) {
            // 收到 IPv4 包。根据需要发送至 TUN
            DebugLog.d(TAG, "Got IPv4 packet. Length: " + frameData.length + " Bytes");
            try {
                var sourceIP = IPPacketUtils.getSourceIP(frameData);
                if (sourceIP != null) {
                    if (isIPv4Multicast(sourceIP)) {
                        var result = this.node.multicastSubscribe(this.networkId, multicastAddressToMAC(sourceIP));
                        if (result != ResultCode.RESULT_OK) {
                            Log.e(TAG, "Error when calling multicastSubscribe: " + result);
                        }
                    } else {
                        this.arpTable.setAddress(sourceIP, srcMac);
                    }
                }
                this.out.write(frameData);
            } catch (Exception e) {
                Log.e(TAG, "Error writing data to vpn socket: " + e.getMessage(), e);
            }
        } else if (etherType == IPV6_PACKET) {
            // 收到 IPv6 包。根据需要发送至 TUN，并更新 NDP 表
            DebugLog.d(TAG, "Got IPv6 packet. Length: " + frameData.length + " Bytes");
            try {
                var sourceIP = IPPacketUtils.getSourceIP(frameData);
                if (sourceIP != null) {
                    if (isIPv6Multicast(sourceIP)) {
                        var result = this.node.multicastSubscribe(this.networkId, multicastAddressToMAC(sourceIP));
                        if (result != ResultCode.RESULT_OK) {
                            Log.e(TAG, "Error when calling multicastSubscribe: " + result);
                        }
                    } else {
                        this.ndpTable.setAddress(sourceIP, srcMac);
                    }
                }
                this.out.write(frameData);
            } catch (Exception e) {
                Log.e(TAG, "Error writing data to vpn socket: " + e.getMessage(), e);
            }
        } else if (frameData.length >= 14) {
            Log.d(TAG, "Unknown Packet Type Received: 0x" + String.format("%02X%02X", frameData[12], frameData[13]));
        } else {
            Log.d(TAG, "Unknown Packet Received.  Packet Length: " + frameData.length);
        }
    }

    private Route routeForDestination(InetAddress destAddress) {
        synchronized (this.routeMap) {
            for (var route : this.routeMap.keySet()) {
                if (route.belongsToRoute(destAddress)) {
                    return route;
                }
            }
            return null;
        }
    }

    private long networkIdForDestination(InetAddress destAddress) {
        synchronized (this.routeMap) {
            for (Route route : this.routeMap.keySet()) {
                if (route.belongsToRoute(destAddress)) {
                    return this.routeMap.get(route);
                }
            }
            return 0;
        }
    }
}

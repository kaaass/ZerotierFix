package net.kaaass.zerotierfix.service;

import android.os.ParcelFileDescriptor;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.zerotier.sdk.Node;
import com.zerotier.sdk.ResultCode;
import com.zerotier.sdk.VirtualNetworkConfig;
import com.zerotier.sdk.VirtualNetworkFrameListener;

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

// TODO: clear up
public class TunTapAdapter implements VirtualNetworkFrameListener {
    public static final long BROADCAST_MAC = 281474976710655L;
    public static final String TAG = "TunTapAdapter";
    private static final int ARP_PACKET = 2054;
    private static final int IPV4_PACKET = 2048;
    private static final int IPV6_PACKET = 34525;
    private final HashMap<Route, Long> routeMap = new HashMap<>();
    private ARPTable arpTable = new ARPTable();
    private VirtualNetworkConfig cfg;
    private FileInputStream in;
    private NDPTable ndpTable = new NDPTable();
    private final long networkId;
    private Node node;
    private FileOutputStream out;
    private Thread receiveThread;
    private ParcelFileDescriptor vpnSocket;
    private final ZeroTierOneService ztService;

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

    public void setNetworkConfig(VirtualNetworkConfig virtualNetworkConfig) {
        this.cfg = virtualNetworkConfig;
    }

    public void setNode(Node node2) {
        this.node = node2;
        try {
            node2.multicastSubscribe(this.networkId, multicastAddressToMAC(InetAddress.getByName("224.224.224.224")));
        } catch (UnknownHostException e) {
            Log.e(TAG, "", e);
        }
    }

    public void setVpnSocket(ParcelFileDescriptor parcelFileDescriptor) {
        this.vpnSocket = parcelFileDescriptor;
    }

    public void setFileStreams(FileInputStream fileInputStream, FileOutputStream fileOutputStream) {
        this.in = fileInputStream;
        this.out = fileOutputStream;
    }

    public void addRouteAndNetwork(Route route, long j) {
        synchronized (this.routeMap) {
            this.routeMap.put(route, Long.valueOf(j));
        }
    }

    public void clearRouteMap() {
        synchronized (this.routeMap) {
            this.routeMap.clear();
            addMulticastRoutes();
        }
    }

    private boolean isIPv4Multicast(InetAddress inetAddress) {
        byte[] address = inetAddress.getAddress();
        return address[0] >= -32 && address[0] <= -17;
    }

    private boolean isIPv6Multicast(InetAddress inetAddress) {
        byte[] address = inetAddress.getAddress();
        return address[0] == -1 && address[1] >= 0 && address[1] <= -2;
    }

    public void startThreads() {
        this.receiveThread = new Thread("Tunnel Receive Thread") {
            /* class com.zerotier.one.service.TunTapAdapter.AnonymousClass1 */

            public void run() {
                if (TunTapAdapter.this.ndpTable == null) {
                    TunTapAdapter.this.ndpTable = new NDPTable();
                }
                if (TunTapAdapter.this.arpTable == null) {
                    TunTapAdapter.this.arpTable = new ARPTable();
                }
                try {
                    Log.d(TunTapAdapter.TAG, "TUN Receive Thread Started");
                    ByteBuffer allocate = ByteBuffer.allocate(32767);
                    allocate.order(ByteOrder.LITTLE_ENDIAN);
                    while (!isInterrupted()) {
                        try {
                            boolean z = true;
                            int read = TunTapAdapter.this.in.read(allocate.array());
                            if (read > 0) {
                                Log.d(TunTapAdapter.TAG, "Sending packet to ZeroTier. " + read + " bytes.");
                                byte[] bArr = new byte[read];
                                System.arraycopy(allocate.array(), 0, bArr, 0, read);
                                byte iPVersion = IPPacketUtils.getIPVersion(bArr);
                                if (iPVersion == 4) {
                                    TunTapAdapter.this.handleIPv4Packet(bArr);
                                } else if (iPVersion == 6) {
                                    TunTapAdapter.this.handleIPv6Packet(bArr);
                                } else {
                                    Log.e(TunTapAdapter.TAG, "Unknown IP version");
                                }
                                allocate.clear();
                                z = false;
                            }
                            if (z) {
                                Thread.sleep(10);
                            }
                        } catch (InterruptedException e) {
                            Log.e(TunTapAdapter.TAG, "Tun/Tap Interrupted", e);
                            throw e;
                        } catch (Exception e2) {
                            Log.e(TunTapAdapter.TAG, "Error in TUN Receive", e2);
                        }
                    }
                } catch (Exception e3) {
                    Log.e(TunTapAdapter.TAG, "Exception ending Tun/Tap", e3);
                }
                Log.d(TunTapAdapter.TAG, "TUN Receive Thread ended");
                TunTapAdapter.this.ndpTable.stop();
                TunTapAdapter.this.ndpTable = null;
                TunTapAdapter.this.arpTable.stop();
                TunTapAdapter.this.arpTable = null;
            }
        };
        this.receiveThread.start();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleIPv4Packet(byte[] bArr) {
        boolean z;
        InetAddress inetAddress;
        int i;
        long j;
        InetAddress destIP = IPPacketUtils.getDestIP(bArr);
        InetAddress sourceIP = IPPacketUtils.getSourceIP(bArr);
        if (this.cfg == null) {
            Log.e(TAG, "TunTapAdapter has no network config yet");
            return;
        }
        if (isIPv4Multicast(destIP)) {
            this.node.multicastSubscribe(this.networkId, multicastAddressToMAC(destIP));
            z = true;
        } else {
            z = false;
        }
        Route routeForDestination = routeForDestination(destIP);
        InetAddress gateway = routeForDestination != null ? routeForDestination.getGateway() : null;
        InetSocketAddress[] assignedAddresses = this.cfg.assignedAddresses();
        int length = assignedAddresses.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                inetAddress = null;
                i = 0;
                break;
            }
            InetSocketAddress inetSocketAddress = assignedAddresses[i2];
            if (inetSocketAddress.getAddress() instanceof Inet4Address) {
                i = inetSocketAddress.getPort();
                inetAddress = inetSocketAddress.getAddress();
                break;
            }
            i2++;
        }
        if (gateway != null && !InetAddressUtils.addressToRouteNo0Route(destIP, i).equals(InetAddressUtils.addressToRouteNo0Route(sourceIP, i))) {
            destIP = gateway;
        }
        if (inetAddress == null) {
            Log.e(TAG, "Couldn't determine local address");
            return;
        }
        long macAddress = this.cfg.macAddress();
        long[] jArr = new long[1];
        if (z || this.arpTable.hasMacForAddress(destIP)) {
            if (isIPv4Multicast(destIP)) {
                j = multicastAddressToMAC(destIP);
            } else {
                j = this.arpTable.getMacForAddress(destIP);
            }
            ResultCode processVirtualNetworkFrame = this.node.processVirtualNetworkFrame(System.currentTimeMillis(), this.networkId, macAddress, j, 2048, 0, bArr, jArr);
            if (processVirtualNetworkFrame != ResultCode.RESULT_OK) {
                Log.e(TAG, "Error calling processVirtualNetworkFrame: " + processVirtualNetworkFrame.toString());
                return;
            }
            Log.d(TAG, "Packet sent to ZT");
            this.ztService.setNextBackgroundTaskDeadline(jArr[0]);
            return;
        }
        Log.d(TAG, "Unknown dest MAC address.  Need to look it up. " + destIP);
        ResultCode processVirtualNetworkFrame2 = this.node.processVirtualNetworkFrame(System.currentTimeMillis(), this.networkId, macAddress, BROADCAST_MAC, ARP_PACKET, 0, this.arpTable.getRequestPacket(macAddress, inetAddress, destIP), jArr);
        if (processVirtualNetworkFrame2 != ResultCode.RESULT_OK) {
            Log.e(TAG, "Error sending ARP packet: " + processVirtualNetworkFrame2.toString());
            return;
        }
        Log.d(TAG, "ARP Request Sent!");
        this.ztService.setNextBackgroundTaskDeadline(jArr[0]);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00d7  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x011e  */
    /* JADX WARNING: Removed duplicated region for block: B:65:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    // Decomp by fernflower
    private void handleIPv6Packet(byte[] var1) {
        InetAddress var2 = IPPacketUtils.getDestIP(var1);
        InetAddress var3 = IPPacketUtils.getSourceIP(var1);
        if (this.cfg == null) {
            Log.e("TunTapAdapter", "TunTapAdapter has no network config yet");
        } else {
            if (this.isIPv6Multicast(var2)) {
                this.node.multicastSubscribe(this.networkId, multicastAddressToMAC(var2));
            }

            Route var4 = this.routeForDestination(var2);
            InetAddress var15;
            if (var4 != null) {
                var15 = var4.getGateway();
            } else {
                var15 = null;
            }

            InetSocketAddress[] var5 = this.cfg.assignedAddresses();
            int var6 = var5.length;
            int var7 = 0;

            InetAddress var19;
            while (true) {
                if (var7 >= var6) {
                    var19 = null;
                    var7 = 0;
                    break;
                }

                InetSocketAddress var8 = var5[var7];
                if (var8.getAddress() instanceof Inet6Address) {
                    var7 = var8.getPort();
                    var19 = var8.getAddress();
                    break;
                }

                ++var7;
            }

            InetAddress var16 = var2;
            if (var15 != null) {
                var16 = var2;
                if (!InetAddressUtils.addressToRouteNo0Route(var2, var7).equals(InetAddressUtils.addressToRouteNo0Route(var3, var7))) {
                    var16 = var15;
                }
            }

            if (var19 == null) {
                Log.e("TunTapAdapter", "Couldn't determine local address");
            } else {
                long var9;
                long var11;
                long[] var17;
                boolean var18;
                boolean var20;
                label75:
                {
                    var9 = this.cfg.macAddress();
                    var18 = true;
                    var17 = new long[1];
                    if (this.isNeighborSolicitation(var1)) {
                        if (this.ndpTable.hasMacForAddress(var16)) {
                            var11 = this.ndpTable.getMacForAddress(var16);
                        } else {
                            var11 = InetAddressUtils.ipv6ToMulticastAddress(var16);
                        }
                    } else if (this.isIPv6Multicast(var16)) {
                        var11 = multicastAddressToMAC(var16);
                    } else {
                        if (this.isNeighborAdvertisement(var1)) {
                            if (this.ndpTable.hasMacForAddress(var16)) {
                                var11 = this.ndpTable.getMacForAddress(var16);
                            } else {
                                var11 = 0L;
                            }

                            var20 = true;
                            break label75;
                        }

                        if (this.ndpTable.hasMacForAddress(var16)) {
                            var11 = this.ndpTable.getMacForAddress(var16);
                        } else {
                            var11 = 0L;
                        }
                    }

                    var20 = false;
                }

                long var21;
                int var13 = (var21 = var11) == 0L ? 0 : (var21 < 0L ? -1 : 1);
                ResultCode var14;
                if (var13 != 0) {
                    var14 = this.node.processVirtualNetworkFrame(System.currentTimeMillis(), this.networkId, var9, var11, '\u86dd', 0, var1, var17);
                    if (var14 != ResultCode.RESULT_OK) {
                        Log.e("TunTapAdapter", "Error calling processVirtualNetworkFrame: " + var14.toString());
                    } else {
                        Log.d("TunTapAdapter", "Packet sent to ZT");
                        this.ztService.setNextBackgroundTaskDeadline(var17[0]);
                    }

                    var18 = var20;
                }

                if (var18) {
                    if (var13 == 0) {
                        var11 = InetAddressUtils.ipv6ToMulticastAddress(var16);
                    }

                    Log.d("TunTapAdapter", "Sending Neighbor Solicitation");
                    var1 = this.ndpTable.getNeighborSolicitationPacket(var3, var16, var9);
                    var14 = this.node.processVirtualNetworkFrame(System.currentTimeMillis(), this.networkId, var9, var11, '\u86dd', 0, var1, var17);
                    if (var14 != ResultCode.RESULT_OK) {
                        Log.e("TunTapAdapter", "Error calling processVirtualNetworkFrame: " + var14.toString());
                    } else {
                        Log.d("TunTapAdapter", "Neighbor Solicitation sent to ZT");
                        this.ztService.setNextBackgroundTaskDeadline(var17[0]);
                    }
                }

            }
        }
    }

    public void interrupt() {
        if (this.receiveThread != null) {
            try {
                this.in.close();
                this.out.close();
            } catch (IOException e) {
                Log.e(TAG, "Error stopping in/out", e);
            }
            this.receiveThread.interrupt();
            try {
                this.receiveThread.join();
            } catch (InterruptedException unused) {
            }
        }
    }

    private boolean isNeighborSolicitation(byte[] bArr) {
        return bArr[6] == 58 && bArr[40] == -121;
    }

    private boolean isNeighborAdvertisement(byte[] bArr) {
        return bArr[6] == 58 && bArr[40] == -120;
    }

    public boolean isRunning() {
        Thread thread = this.receiveThread;
        if (thread == null) {
            return false;
        }
        return thread.isAlive();
    }

    @Override // com.zerotier.sdk.VirtualNetworkFrameListener
    public void onVirtualNetworkFrame(long j, long j2, long j3, long j4, long j5, byte[] bArr) {
        Log.d(TAG, "Got Virtual Network Frame.  Network ID: " + Long.toHexString(j) + " Source MAC: " + Long.toHexString(j2) + " Dest MAC: " + Long.toHexString(j3) + " Ether type: " + j4 + " VLAN ID: " + j5 + " Frame Length: " + bArr.length);
        if (this.vpnSocket == null) {
            Log.e(TAG, "vpnSocket is null!");
        } else if (this.in == null || this.out == null) {
            Log.e(TAG, "no in/out streams");
        } else if (j4 == 2054) {
            Log.d(TAG, "Got ARP Packet");
            ARPTable.ARPReplyData processARPPacket = this.arpTable.processARPPacket(bArr);
            if (processARPPacket != null && processARPPacket.destMac != 0 && processARPPacket.destAddress != null) {
                long[] jArr = new long[1];
                VirtualNetworkConfig networkConfig = this.node.networkConfig(j);
                InetAddress inetAddress = null;
                InetSocketAddress[] assignedAddresses = networkConfig.assignedAddresses();
                int length = assignedAddresses.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    InetSocketAddress inetSocketAddress = assignedAddresses[i];
                    if (inetSocketAddress.getAddress() instanceof Inet4Address) {
                        inetAddress = inetSocketAddress.getAddress();
                        break;
                    }
                    i++;
                }
                if (inetAddress != null) {
                    ResultCode processVirtualNetworkFrame = this.node.processVirtualNetworkFrame(System.currentTimeMillis(), j, networkConfig.macAddress(), j2, ARP_PACKET, 0, this.arpTable.getReplyPacket(networkConfig.macAddress(), inetAddress, processARPPacket.destMac, processARPPacket.destAddress), jArr);
                    if (processVirtualNetworkFrame != ResultCode.RESULT_OK) {
                        Log.e(TAG, "Error sending ARP packet: " + processVirtualNetworkFrame.toString());
                        return;
                    }
                    Log.d(TAG, "ARP Reply Sent!");
                    this.ztService.setNextBackgroundTaskDeadline(jArr[0]);
                }
            }
        } else if (j4 == PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH) {
            Log.d(TAG, "Got IPv4 packet. Length: " + bArr.length + " Bytes");
            try {
                InetAddress sourceIP = IPPacketUtils.getSourceIP(bArr);
                if (sourceIP != null) {
                    if (isIPv4Multicast(sourceIP)) {
                        this.node.multicastSubscribe(this.networkId, multicastAddressToMAC(sourceIP));
                    } else {
                        this.arpTable.setAddress(sourceIP, j2);
                    }
                }
                this.out.write(bArr);
            } catch (Exception e) {
                Log.e(TAG, "Error writing data to vpn socket", e);
            }
        } else if (j4 == 34525) {
            Log.d(TAG, "Got IPv6 packet. Length: " + bArr.length + " Bytes");
            try {
                InetAddress sourceIP2 = IPPacketUtils.getSourceIP(bArr);
                if (sourceIP2 != null) {
                    if (isIPv6Multicast(sourceIP2)) {
                        this.node.multicastSubscribe(this.networkId, multicastAddressToMAC(sourceIP2));
                    } else {
                        this.ndpTable.setAddress(sourceIP2, j2);
                    }
                }
                this.out.write(bArr);
            } catch (Exception e) {
                Log.e(TAG, "Error writing data to vpn socket", e);
            }
        } else if (bArr.length >= 14) {
            Log.d(TAG, "Unknown Packet Type Received: 0x" + String.format("%02X%02X", bArr[12], bArr[13]));
        } else {
            Log.d(TAG, "Unknown Packet Received.  Packet Length: " + bArr.length);
        }
    }

    private Route routeForDestination(InetAddress inetAddress) {
        synchronized (this.routeMap) {
            for (Route route : this.routeMap.keySet()) {
                if (route.belongsToRoute(inetAddress)) {
                    return route;
                }
            }
            return null;
        }
    }

    private long networkIdForDestination(InetAddress inetAddress) {
        synchronized (this.routeMap) {
            for (Route route : this.routeMap.keySet()) {
                if (route.belongsToRoute(inetAddress)) {
                    return this.routeMap.get(route);
                }
            }
            return 0;
        }
    }
}

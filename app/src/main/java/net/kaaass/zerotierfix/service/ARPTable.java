package net.kaaass.zerotierfix.service;

import android.util.Log;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;

// TODO: clear up
public class ARPTable {
    public static final String TAG = "ARPTable";
    private static final long ENTRY_TIMEOUT = 120000;
    private static final int REPLY = 2;
    private static final int REQUEST = 1;
    private final HashMap<Long, ARPEntry> entriesMap = new HashMap<>();
    private final HashMap<InetAddress, Long> inetAddressToMacAddress = new HashMap<>();
    private final HashMap<InetAddress, ARPEntry> ipEntriesMap = new HashMap<>();
    private final HashMap<Long, InetAddress> macAddressToInetAdddress = new HashMap<>();
    private final Thread timeoutThread = new Thread("ARP Timeout Thread") {
        /* class com.zerotier.one.service.ARPTable.AnonymousClass1 */

        public void run() {
            while (!isInterrupted()) {
                try {
                    for (ARPEntry arpEntry : new HashMap<>(ARPTable.this.entriesMap).values()) {
                        if (arpEntry.getTime() + ARPTable.ENTRY_TIMEOUT < System.currentTimeMillis()) {
                            Log.d(ARPTable.TAG, "Removing " + arpEntry.getAddress().toString() + " from ARP cache");
                            synchronized (ARPTable.this.macAddressToInetAdddress) {
                                ARPTable.this.macAddressToInetAdddress.remove(arpEntry.getMac());
                            }
                            synchronized (ARPTable.this.inetAddressToMacAddress) {
                                ARPTable.this.inetAddressToMacAddress.remove(arpEntry.getAddress());
                            }
                            synchronized (ARPTable.this.entriesMap) {
                                ARPTable.this.entriesMap.remove(arpEntry.getMac());
                            }
                            synchronized (ARPTable.this.ipEntriesMap) {
                                ARPTable.this.ipEntriesMap.remove(arpEntry.getAddress());
                            }
                        }
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(ARPTable.TAG, "Tun/Tap Interrupted", e);
                    break;
                } catch (Exception e) {
                    Log.d(ARPTable.TAG, e.toString());
                }
            }
            Log.d(ARPTable.TAG, "ARP Timeout Thread Ended.");
        }
    };

    public ARPTable() {
        this.timeoutThread.start();
    }

    public static byte[] longToBytes(long j) {
        ByteBuffer allocate = ByteBuffer.allocate(8);
        allocate.putLong(j);
        return allocate.array();
    }

    public void stop() {
        try {
            this.timeoutThread.interrupt();
            this.timeoutThread.join();
        } catch (InterruptedException ignored) {
        }
    }

    /* access modifiers changed from: package-private */
    public void setAddress(InetAddress inetAddress, long j) {
        synchronized (this.inetAddressToMacAddress) {
            this.inetAddressToMacAddress.put(inetAddress, j);
        }
        synchronized (this.macAddressToInetAdddress) {
            this.macAddressToInetAdddress.put(j, inetAddress);
        }
        ARPEntry arpEntry = new ARPEntry(j, inetAddress);
        synchronized (this.entriesMap) {
            this.entriesMap.put(j, arpEntry);
        }
        synchronized (this.ipEntriesMap) {
            this.ipEntriesMap.put(inetAddress, arpEntry);
        }
    }

    private void updateArpEntryTime(long j) {
        synchronized (this.entriesMap) {
            ARPEntry arpEntry = this.entriesMap.get(j);
            if (arpEntry != null) {
                arpEntry.updateTime();
            }
        }
    }

    private void updateArpEntryTime(InetAddress inetAddress) {
        synchronized (this.ipEntriesMap) {
            ARPEntry arpEntry = this.ipEntriesMap.get(inetAddress);
            if (arpEntry != null) {
                arpEntry.updateTime();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public long getMacForAddress(InetAddress inetAddress) {
        synchronized (this.inetAddressToMacAddress) {
            if (!this.inetAddressToMacAddress.containsKey(inetAddress)) {
                return -1;
            }
            Log.d(TAG, "Returning MAC for " + inetAddress.toString());
            var longValue = this.inetAddressToMacAddress.get(inetAddress);
            if (longValue != null) {
                updateArpEntryTime(longValue);
                return longValue;
            }
        }
        return -1;
    }

    /* access modifiers changed from: package-private */
    public InetAddress getAddressForMac(long j) {
        synchronized (this.macAddressToInetAdddress) {
            if (!this.macAddressToInetAdddress.containsKey(j)) {
                return null;
            }
            InetAddress inetAddress = this.macAddressToInetAdddress.get(j);
            updateArpEntryTime(inetAddress);
            return inetAddress;
        }
    }

    public boolean hasMacForAddress(InetAddress inetAddress) {
        boolean containsKey;
        synchronized (this.inetAddressToMacAddress) {
            containsKey = this.inetAddressToMacAddress.containsKey(inetAddress);
        }
        return containsKey;
    }

    public boolean hasAddressForMac(long j) {
        boolean containsKey;
        synchronized (this.macAddressToInetAdddress) {
            containsKey = this.macAddressToInetAdddress.containsKey(j);
        }
        return containsKey;
    }

    public byte[] getRequestPacket(long j, InetAddress inetAddress, InetAddress inetAddress2) {
        return getARPPacket(1, j, 0, inetAddress, inetAddress2);
    }

    public byte[] getReplyPacket(long j, InetAddress inetAddress, long j2, InetAddress inetAddress2) {
        return getARPPacket(2, j, j2, inetAddress, inetAddress2);
    }

    public byte[] getARPPacket(int i, long j, long j2, InetAddress inetAddress, InetAddress inetAddress2) {
        byte[] bArr = new byte[28];
        bArr[0] = 0;
        bArr[1] = 1;
        bArr[2] = 8;
        bArr[3] = 0;
        bArr[4] = 6;
        bArr[5] = 4;
        bArr[6] = 0;
        bArr[7] = (byte) i;
        System.arraycopy(longToBytes(j), 2, bArr, 8, 6);
        System.arraycopy(inetAddress.getAddress(), 0, bArr, 14, 4);
        System.arraycopy(longToBytes(j2), 2, bArr, 18, 6);
        System.arraycopy(inetAddress2.getAddress(), 0, bArr, 24, 4);
        return bArr;
    }

    public ARPReplyData processARPPacket(byte[] packetData) {
        InetAddress srcAddress;
        InetAddress dstAddress;
        Log.d(TAG, "Processing ARP packet");

        // 解析包内 IP、MAC 地址
        byte[] rawSrcMac = new byte[8];
        System.arraycopy(packetData, 8, rawSrcMac, 2, 6);
        byte[] rawSrcAddress = new byte[4];
        System.arraycopy(packetData, 14, rawSrcAddress, 0, 4);
        byte[] rawDstMac = new byte[8];
        System.arraycopy(packetData, 18, rawDstMac, 2, 6);
        byte[] rawDstAddress = new byte[4];
        System.arraycopy(packetData, 24, rawDstAddress, 0, 4);
        try {
            srcAddress = InetAddress.getByAddress(rawSrcAddress);
        } catch (Exception unused) {
            srcAddress = null;
        }
        try {
            dstAddress = InetAddress.getByAddress(rawDstAddress);
        } catch (Exception unused) {
            dstAddress = null;
        }
        long srcMac = ByteBuffer.wrap(rawSrcMac).getLong();
        long dstMac = ByteBuffer.wrap(rawDstMac).getLong();

        // 更新 ARP 表项
        if (srcMac != 0 && srcAddress != null) {
            setAddress(srcAddress, srcMac);
        }
        if (dstMac != 0 && dstAddress != null) {
            setAddress(dstAddress, dstMac);
        }

        // 处理响应行为
        var packetType = packetData[7];
        if (packetType == REQUEST) {
            // ARP 请求，返回应答数据
            Log.d(TAG, "Reply needed");
            return new ARPReplyData(srcMac, srcAddress);
        } else {
            return null;
        }
    }
}

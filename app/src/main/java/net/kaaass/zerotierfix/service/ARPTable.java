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
    private final HashMap<Long, ArpEntry> entriesMap = new HashMap<>();
    private final HashMap<InetAddress, Long> inetAddressToMacAddress = new HashMap<>();
    private final HashMap<InetAddress, ArpEntry> ipEntriesMap = new HashMap<>();
    private final HashMap<Long, InetAddress> macAddressToInetAdddress = new HashMap<>();
    private final Thread timeoutThread = new Thread("ARP Timeout Thread") {
        /* class com.zerotier.one.service.ARPTable.AnonymousClass1 */

        public void run() {
            while (!isInterrupted()) {
                try {
                    for (ArpEntry arpEntry : new HashMap<>(ARPTable.this.entriesMap).values()) {
                        if (arpEntry.time + ARPTable.ENTRY_TIMEOUT < System.currentTimeMillis()) {
                            Log.d(ARPTable.TAG, "Removing " + arpEntry.getAddress().toString() + " from ARP cache");
                            synchronized (ARPTable.this.macAddressToInetAdddress) {
                                ARPTable.this.macAddressToInetAdddress.remove(arpEntry.mac);
                            }
                            synchronized (ARPTable.this.inetAddressToMacAddress) {
                                ARPTable.this.inetAddressToMacAddress.remove(arpEntry.address);
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

    /* access modifiers changed from: protected */
    public void finalize() throws Throwable {
        stop();
        super.finalize();
    }

    /* access modifiers changed from: protected */
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
        ArpEntry arpEntry = new ArpEntry(j, inetAddress);
        synchronized (this.entriesMap) {
            this.entriesMap.put(j, arpEntry);
        }
        synchronized (this.ipEntriesMap) {
            this.ipEntriesMap.put(inetAddress, arpEntry);
        }
    }

    private void updateArpEntryTime(long j) {
        synchronized (this.entriesMap) {
            ArpEntry arpEntry = this.entriesMap.get(j);
            if (arpEntry != null) {
                arpEntry.updateTime();
            }
        }
    }

    private void updateArpEntryTime(InetAddress inetAddress) {
        synchronized (this.ipEntriesMap) {
            ArpEntry arpEntry = this.ipEntriesMap.get(inetAddress);
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
            long longValue = this.inetAddressToMacAddress.get(inetAddress);
            updateArpEntryTime(longValue);
            return longValue;
        }
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

    public ARPReplyData processARPPacket(byte[] bArr) {
        InetAddress inetAddress;
        InetAddress inetAddress2;
        Log.d(TAG, "Processing ARP packet");
        byte[] bArr2 = new byte[8];
        System.arraycopy(bArr, 8, bArr2, 2, 6);
        byte[] bArr3 = new byte[4];
        System.arraycopy(bArr, 14, bArr3, 0, 4);
        byte[] bArr4 = new byte[8];
        System.arraycopy(bArr, 18, bArr4, 2, 6);
        byte[] bArr5 = new byte[4];
        System.arraycopy(bArr, 24, bArr5, 0, 4);
        try {
            inetAddress = InetAddress.getByAddress(bArr3);
        } catch (Exception unused) {
            inetAddress = null;
        }
        try {
            inetAddress2 = InetAddress.getByAddress(bArr5);
        } catch (Exception unused2) {
            inetAddress2 = null;
        }
        long j = ByteBuffer.wrap(bArr2).getLong();
        long j2 = ByteBuffer.wrap(bArr4).getLong();
        if (!(j == 0 || inetAddress == null)) {
            setAddress(inetAddress, j);
        }
        if (!(j2 == 0 || inetAddress2 == null)) {
            setAddress(inetAddress2, j2);
        }
        if (bArr[7] != 1) {
            return null;
        }
        Log.d(TAG, "Reply needed");
        ARPReplyData aRPReplyData = new ARPReplyData();
        aRPReplyData.destMac = j;
        aRPReplyData.destAddress = inetAddress;
        return aRPReplyData;
    }

    public static class ARPReplyData {
        public InetAddress destAddress;
        public long destMac;
        public InetAddress senderAddress;
        public long senderMac;

        public ARPReplyData() {
        }
    }

    /* access modifiers changed from: private */
    public static class ArpEntry {
        private final InetAddress address;
        private final long mac;
        private long time;

        ArpEntry(long j, InetAddress inetAddress) {
            this.mac = j;
            this.address = inetAddress;
            updateTime();
        }

        public long getMac() {
            return this.mac;
        }

        public InetAddress getAddress() {
            return this.address;
        }

        public void updateTime() {
            this.time = System.currentTimeMillis();
        }

        public boolean equals(ArpEntry arpEntry) {
            return this.mac == arpEntry.mac && this.address.equals(arpEntry.address);
        }
    }
}

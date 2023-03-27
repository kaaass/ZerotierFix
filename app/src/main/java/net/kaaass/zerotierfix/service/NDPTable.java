package net.kaaass.zerotierfix.service;

import android.util.Log;

import net.kaaass.zerotierfix.util.IPPacketUtils;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;

// TODO: clear up
public class NDPTable {
    public static final String TAG = "NDPTable";
    private static final long ENTRY_TIMEOUT = 120000;
    private final HashMap<Long, NDPEntry> entriesMap = new HashMap<>();
    private final HashMap<InetAddress, Long> inetAddressToMacAddress = new HashMap<>();
    private final HashMap<InetAddress, NDPEntry> ipEntriesMap = new HashMap<>();
    private final HashMap<Long, InetAddress> macAddressToInetAddress = new HashMap<>();
    private final Thread timeoutThread = new Thread("NDP Timeout Thread") {
        /* class com.zerotier.one.service.NDPTable.AnonymousClass1 */

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    for (NDPEntry nDPEntry : new HashMap<>(NDPTable.this.entriesMap).values()) {
                        if (nDPEntry.getTime() + NDPTable.ENTRY_TIMEOUT < System.currentTimeMillis()) {
                            synchronized (NDPTable.this.macAddressToInetAddress) {
                                NDPTable.this.macAddressToInetAddress.remove(nDPEntry.getMac());
                            }
                            synchronized (NDPTable.this.inetAddressToMacAddress) {
                                NDPTable.this.inetAddressToMacAddress.remove(nDPEntry.getAddress());
                            }
                            synchronized (NDPTable.this.entriesMap) {
                                NDPTable.this.entriesMap.remove(nDPEntry.getMac());
                            }
                            synchronized (NDPTable.this.ipEntriesMap) {
                                NDPTable.this.ipEntriesMap.remove(nDPEntry.getAddress());
                            }
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.d(NDPTable.TAG, e.toString());
                    return;
                }
            }
        }
    };

    public NDPTable() {
        this.timeoutThread.start();
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
            this.inetAddressToMacAddress.put(inetAddress, Long.valueOf(j));
        }
        synchronized (this.macAddressToInetAddress) {
            this.macAddressToInetAddress.put(Long.valueOf(j), inetAddress);
        }
        NDPEntry nDPEntry = new NDPEntry(j, inetAddress);
        synchronized (this.entriesMap) {
            this.entriesMap.put(Long.valueOf(j), nDPEntry);
        }
        synchronized (this.ipEntriesMap) {
            this.ipEntriesMap.put(inetAddress, nDPEntry);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean hasMacForAddress(InetAddress inetAddress) {
        boolean containsKey;
        synchronized (this.inetAddressToMacAddress) {
            containsKey = this.inetAddressToMacAddress.containsKey(inetAddress);
        }
        return containsKey;
    }

    /* access modifiers changed from: package-private */
    public boolean hasAddressForMac(long j) {
        boolean containsKey;
        synchronized (this.macAddressToInetAddress) {
            containsKey = this.macAddressToInetAddress.containsKey(Long.valueOf(j));
        }
        return containsKey;
    }

    /* access modifiers changed from: package-private */
    public long getMacForAddress(InetAddress inetAddress) {
        synchronized (this.inetAddressToMacAddress) {
            if (!this.inetAddressToMacAddress.containsKey(inetAddress)) {
                return -1;
            }
            long longValue = this.inetAddressToMacAddress.get(inetAddress).longValue();
            updateNDPEntryTime(longValue);
            return longValue;
        }
    }

    /* access modifiers changed from: package-private */
    public InetAddress getAddressForMac(long j) {
        synchronized (this.macAddressToInetAddress) {
            if (!this.macAddressToInetAddress.containsKey(Long.valueOf(j))) {
                return null;
            }
            InetAddress inetAddress = this.macAddressToInetAddress.get(Long.valueOf(j));
            updateNDPEntryTime(inetAddress);
            return inetAddress;
        }
    }

    private void updateNDPEntryTime(InetAddress inetAddress) {
        synchronized (this.ipEntriesMap) {
            NDPEntry nDPEntry = this.ipEntriesMap.get(inetAddress);
            if (nDPEntry != null) {
                nDPEntry.updateTime();
            }
        }
    }

    private void updateNDPEntryTime(long j) {
        synchronized (this.entriesMap) {
            NDPEntry nDPEntry = this.entriesMap.get(Long.valueOf(j));
            if (nDPEntry != null) {
                nDPEntry.updateTime();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public byte[] getNeighborSolicitationPacket(InetAddress inetAddress, InetAddress inetAddress2, long j) {
        byte[] bArr = new byte[72];
        System.arraycopy(inetAddress.getAddress(), 0, bArr, 0, 16);
        System.arraycopy(inetAddress2.getAddress(), 0, bArr, 16, 16);
        System.arraycopy(ByteBuffer.allocate(4).putInt(32).array(), 0, bArr, 32, 4);
        bArr[39] = 58;
        bArr[40] = -121;
        System.arraycopy(inetAddress2.getAddress(), 0, bArr, 48, 16);
        byte[] array = ByteBuffer.allocate(8).putLong(j).array();
        bArr[64] = 1;
        bArr[65] = 1;
        System.arraycopy(array, 2, bArr, 66, 6);
        System.arraycopy(ByteBuffer.allocate(2).putShort((short) ((int) IPPacketUtils.calculateChecksum(bArr, 0, 0, 72))).array(), 0, bArr, 42, 2);
        for (int i = 0; i < 40; i++) {
            bArr[i] = 0;
        }
        bArr[0] = 96;
        System.arraycopy(ByteBuffer.allocate(2).putShort((short) 32).array(), 0, bArr, 4, 2);
        bArr[6] = 58;
        bArr[7] = -1;
        System.arraycopy(inetAddress.getAddress(), 0, bArr, 8, 16);
        System.arraycopy(inetAddress2.getAddress(), 0, bArr, 24, 16);
        return bArr;
    }

}

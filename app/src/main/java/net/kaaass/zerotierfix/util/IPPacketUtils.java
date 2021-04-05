package net.kaaass.zerotierfix.util;

import android.util.Log;

import androidx.core.view.MotionEventCompat;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPPacketUtils {
    private static final String TAG = "IPPacketUtils";

    public static InetAddress getSourceIP(byte[] bArr) {
        byte iPVersion = getIPVersion(bArr);
        if (iPVersion == 4) {
            byte[] bArr2 = new byte[4];
            System.arraycopy(bArr, 12, bArr2, 0, 4);
            try {
                return InetAddress.getByAddress(bArr2);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Error creating InetAddress", e);
                return null;
            }
        } else if (iPVersion == 6) {
            byte[] bArr3 = new byte[16];
            System.arraycopy(bArr, 8, bArr3, 0, 16);
            try {
                return InetAddress.getByAddress(bArr3);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Error creating InetAddress", e);
                return null;
            }
        } else {
            Log.e(TAG, "Unknown IP version");
            return null;
        }
    }

    public static InetAddress getDestIP(byte[] bArr) {
        byte iPVersion = getIPVersion(bArr);
        if (iPVersion == 4) {
            byte[] bArr2 = new byte[4];
            System.arraycopy(bArr, 16, bArr2, 0, 4);
            try {
                return InetAddress.getByAddress(bArr2);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Error creating InetAddress", e);
                return null;
            }
        } else if (iPVersion == 6) {
            byte[] bArr3 = new byte[16];
            System.arraycopy(bArr, 24, bArr3, 0, 16);
            try {
                return InetAddress.getByAddress(bArr3);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Error creating InetAddress", e);
                return null;
            }
        } else {
            Log.e(TAG, "Unknown IP version");
            return null;
        }
    }

    public static byte getIPVersion(byte[] bArr) {
        return (byte) (bArr[0] >> 4);
    }

    public static long calculateChecksum(byte[] bArr, long j, int i, int i2) {
        int i3 = i2 - i;
        while (i3 > 1) {
            j += (65280 & (bArr[i] << 8)) | (bArr[i + 1] & 255);
            if ((-65536 & j) > 0) {
                j = (j & 65535) + 1;
            }
            i += 2;
            i3 -= 2;
        }
        if (i3 > 0) {
            j += (bArr[i] << 8) & MotionEventCompat.ACTION_POINTER_INDEX_MASK;
            if ((j & -65536) > 0) {
                j = (j & 65535) + 1;
            }
        }
        return (~j) & 65535;
    }
}

package net.kaaass.zerotierfix.util;

import android.util.Log;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class InetAddressUtils {
    public static final String TAG = "InetAddressUtils";

    public static byte[] addressToNetmask(InetAddress inetAddress, int i) {
        int length = inetAddress.getAddress().length;
        int i2 = length * 8;
        byte[] bArr = new byte[length];
        for (int i3 = 0; i3 < length; i3++) {
            bArr[i3] = -1;
        }
        if (length == 4) {
            int i4 = i2 - i;
            return ByteBuffer.allocate(4).putInt((ByteBuffer.wrap(bArr).getInt() >> i4) << i4).array();
        }
        int i5 = i2 - i;
        byte[] byteArray = new BigInteger(bArr).shiftRight(i5).shiftLeft(i5).toByteArray();
        if (byteArray.length == length) {
            return byteArray;
        }
        byte[] bArr2 = new byte[length];
        int abs = Math.abs(length - byteArray.length);
        for (int i6 = 0; i6 < abs; i6++) {
            bArr2[i6] = byteArray[0];
        }
        System.arraycopy(byteArray, 0, bArr2, abs, byteArray.length);
        return bArr2;
    }

    public static InetAddress addressToRoute(InetAddress inetAddress, int i) {
        if (i == 0) {
            if (inetAddress instanceof Inet4Address) {
                try {
                    return Inet4Address.getByAddress(new byte[]{0, 0, 0, 0});
                } catch (UnknownHostException unused) {
                    return null;
                }
            } else if (inetAddress instanceof Inet6Address) {
                byte[] bArr = new byte[16];
                for (int i2 = 0; i2 < 16; i2++) {
                    bArr[i2] = 0;
                }
                try {
                    return Inet6Address.getByAddress(bArr);
                } catch (UnknownHostException unused2) {
                    return null;
                }
            }
        }
        return addressToRouteNo0Route(inetAddress, i);
    }

    public static InetAddress addressToRouteNo0Route(InetAddress inetAddress, int i) {
        byte[] addressToNetmask = addressToNetmask(inetAddress, i);
        byte[] bArr = new byte[addressToNetmask.length];
        for (int i2 = 0; i2 < addressToNetmask.length; i2++) {
            bArr[i2] = (byte) (inetAddress.getAddress()[i2] & addressToNetmask[i2]);
        }
        try {
            return InetAddress.getByAddress(bArr);
        } catch (UnknownHostException unused) {
            Log.e(TAG, "Uknown Host Exception calculating route");
            return null;
        }
    }

    public static long ipv6ToMulticastAddress(InetAddress inetAddress) {
        byte[] address = inetAddress.getAddress();
        if (address.length != 16) {
            return 0;
        }
        return ByteBuffer.wrap(new byte[]{0, 0, 51, 51, -1, address[13], address[14], address[15]}).getLong();
    }
}

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

    public static final long BROADCAST_MAC_ADDRESS = 0xffffffffffffL;

    /**
     * 获得地址指定 CIDR 的子网掩码
     */
    public static byte[] addressToNetmask(InetAddress address, int cidr) {
        int length = address.getAddress().length;
        int subnetLength = length * 8 - cidr;
        byte[] fullMasked = new byte[length];
        for (int i = 0; i < length; i++) {
            fullMasked[i] = -1;
        }
        if (length == 4) {
            // IPv4 地址
            return ByteBuffer.allocate(4)
                    .putInt((ByteBuffer.wrap(fullMasked).getInt() >> subnetLength) << subnetLength)
                    .array();
        } else {
            // IPv6 地址
            if (cidr == 0) {
                // 若 CIDR 为 0 则返回空子网掩码
                return new byte[length];
            }
            byte[] shiftedAddress = new BigInteger(fullMasked)
                    .shiftRight(subnetLength)
                    .shiftLeft(subnetLength)
                    .toByteArray();
            if (shiftedAddress.length == length) {
                return shiftedAddress;
            }
            // 高位为 0 时需要在前补 0
            byte[] netmask = new byte[length];
            int offset = Math.abs(length - shiftedAddress.length);
            for (int i = 0; i < offset; i++) {
                netmask[i] = shiftedAddress[0];
            }
            System.arraycopy(shiftedAddress, 0, netmask, offset, shiftedAddress.length);
            return netmask;
        }
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

    /**
     * 获得地址对应的网络前缀
     */
    public static InetAddress addressToRouteNo0Route(InetAddress address, int cidr) {
        byte[] netmask = addressToNetmask(address, cidr);
        byte[] rawAddress = new byte[netmask.length];
        for (int i = 0; i < netmask.length; i++) {
            rawAddress[i] = (byte) (address.getAddress()[i] & netmask[i]);
        }
        try {
            return InetAddress.getByAddress(rawAddress);
        } catch (UnknownHostException unused) {
            Log.e(TAG, "Unknown Host Exception calculating route");
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

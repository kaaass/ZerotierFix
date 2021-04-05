package net.kaaass.zerotierfix.util;

import java.math.BigInteger;

public class NetworkIdUtils {
    public static long hexStringToLong(String str) {
        return new BigInteger(str, 16).longValue();
    }
}

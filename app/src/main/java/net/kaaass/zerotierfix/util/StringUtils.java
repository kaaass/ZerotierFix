package net.kaaass.zerotierfix.util;

import com.zerotier.sdk.Peer;
import com.zerotier.sdk.Version;

import java.util.Locale;

/**
 * 字符串处理工具类
 *
 * @author kaaass
 */
public class StringUtils {

    private static final String VERSION_FORMAT = "%d.%d.%d";

    /**
     * 将版本号转为可读字符串
     *
     * @param version 版本号
     * @return 可读字符串
     */
    public static String toString(Version version) {
        return String.format(Locale.ROOT, VERSION_FORMAT,
                version.getMajor(), version.getMinor(), version.getRevision());
    }

    /**
     * 获得结点版本的可读字符串
     *
     * @param peer 结点
     * @return 可读字符串
     */
    public static String peerVersionString(Peer peer) {
        return String.format(Locale.ROOT, VERSION_FORMAT,
                peer.getVersionMajor(), peer.getVersionMinor(), peer.getVersionRev());
    }

    /**
     * 将 16 进制字符串转换为字符数组
     *
     * @param hex 16 进制字符串
     * @return 字符数组
     */
    public static byte[] hexStringToBytes(String hex) {
        int length = hex.length();
        if (length % 2 != 0) {
            throw new RuntimeException("String length must be even");
        }
        var result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            var highDigit = Character.digit(hex.charAt(i), 16);
            var lowDigit = Character.digit(hex.charAt(i + 1), 16);
            result[i / 2] = (byte) ((highDigit << 4) + lowDigit);
        }
        return result;
    }
}

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
                version.major, version.minor, version.revision);
    }

    /**
     * 获得结点版本的可读字符串
     *
     * @param peer 结点
     * @return 可读字符串
     */
    public static String peerVersionString(Peer peer) {
        return String.format(Locale.ROOT, VERSION_FORMAT,
                peer.versionMajor(), peer.versionMinor(), peer.versionRev());
    }
}

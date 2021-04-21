package net.kaaass.zerotierfix.ui;

/**
 * 自定义 DNS 设置监听器，当值变更时调用
 */
public interface CustomDNSListener {
    void setDNSv4_1(String str);

    void setDNSv4_2(String str);

    void setDNSv6_1(String str);

    void setDNSv6_2(String str);
}

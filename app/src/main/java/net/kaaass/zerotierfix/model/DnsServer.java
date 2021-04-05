package net.kaaass.zerotierfix.model;

public class DnsServer {
    private Long id;
    private String nameserver;
    private Long networkId;

    public DnsServer(Long l, Long l2, String str) {
        this.id = l;
        this.networkId = l2;
        this.nameserver = str;
    }

    public DnsServer() {
    }

    public Long getNetworkId() {
        return this.networkId;
    }

    public void setNetworkId(Long l) {
        this.networkId = l;
    }

    public String getNameserver() {
        return this.nameserver;
    }

    public void setNameserver(String str) {
        this.nameserver = str;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long l) {
        this.id = l;
    }
}

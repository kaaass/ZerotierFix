package net.kaaass.zerotierfix.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class DnsServer {

    @Id
    private Long id;

    private Long networkId;

    private String nameserver;

    @Generated(hash = 1177086365)
    public DnsServer(Long id, Long networkId, String nameserver) {
        this.id = id;
        this.networkId = networkId;
        this.nameserver = nameserver;
    }

    @Generated(hash = 1462226712)
    public DnsServer() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNetworkId() {
        return this.networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getNameserver() {
        return this.nameserver;
    }

    public void setNameserver(String nameserver) {
        this.nameserver = nameserver;
    }
}

package net.kaaass.zerotierfix.service;

import net.kaaass.zerotierfix.util.InetAddressUtils;

import java.net.InetAddress;

// TODO: clear up
public class Route {
    InetAddress address;
    InetAddress gateway = null;
    int prefix;

    public Route(InetAddress inetAddress, int i) {
        this.address = inetAddress;
        this.prefix = i;
    }

    /* access modifiers changed from: package-private */
    public InetAddress getGateway() {
        return this.gateway;
    }

    /* access modifiers changed from: package-private */
    public void setGateway(InetAddress inetAddress) {
        this.gateway = inetAddress;
    }

    public boolean belongsToRoute(InetAddress inetAddress) {
        return this.address.equals(InetAddressUtils.addressToRoute(inetAddress, this.prefix));
    }

    public boolean equals(Route route) {
        return this.address.equals(route.address) && this.prefix == route.prefix && this.gateway.equals(route.gateway);
    }
}

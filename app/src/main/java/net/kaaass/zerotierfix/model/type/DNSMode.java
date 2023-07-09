package net.kaaass.zerotierfix.model.type;

import net.kaaass.zerotierfix.R;

public enum DNSMode {
    NO_DNS(0),
    NETWORK_DNS(1),
    CUSTOM_DNS(2);

    private final int id;

    DNSMode(int i) {
        this.id = i;
    }

    public static DNSMode fromInt(int i) {
        if (i != 0) {
            if (i == 1) {
                return NETWORK_DNS;
            }
            if (i == 2) {
                return CUSTOM_DNS;
            }
            throw new RuntimeException("Unhandled value: " + i);
        }
        return NO_DNS;
    }

    public int toInt() {
        return this.id;
    }

    public int toStringId() {
        switch (this) {
            case NO_DNS:
                return R.string.network_dns_mode_no_dns;
            case NETWORK_DNS:
                return R.string.network_dns_mode_network_dns;
            case CUSTOM_DNS:
                return R.string.network_dns_mode_custom_dns;
            default:
                throw new RuntimeException("Unhandled value: " + this);
        }
    }
}

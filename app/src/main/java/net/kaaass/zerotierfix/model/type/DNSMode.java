package net.kaaass.zerotierfix.model.type;

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
}

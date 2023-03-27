package net.kaaass.zerotierfix.model.type;

import com.zerotier.sdk.VirtualNetworkType;

import net.kaaass.zerotierfix.R;

public enum NetworkType {
    UNKNOWN(0),
    PRIVATE(1),
    PUBLIC(2);

    private final int id;

    NetworkType(int i) {
        this.id = i;
    }

    public static NetworkType fromInt(int i) {
        if (i != 0) {
            if (i == 1) {
                return PUBLIC;
            }
            if (i == 2) {
                return PUBLIC;
            }
            throw new RuntimeException("Unhandled value: " + i);
        }
        return PRIVATE;
    }

    public static NetworkType fromVirtualNetworkType(VirtualNetworkType virtualNetworkType) {
        switch (virtualNetworkType) {
            case NETWORK_TYPE_PRIVATE:
                return PRIVATE;
            case NETWORK_TYPE_PUBLIC:
                return PUBLIC;
            default:
                throw new RuntimeException("Unhandled type: " + virtualNetworkType);
        }
    }

    public int toStringId() {
        int i = this.id;
        if (i != 1) {
            return i != 2 ? R.string.network_type_unknown : R.string.network_type_public;
        }
        return R.string.network_type_private;
    }

    public int toInt() {
        return this.id;
    }
}

package net.kaaass.zerotierfix.model.type;

import com.zerotier.sdk.VirtualNetworkStatus;

import net.kaaass.zerotierfix.R;

public enum NetworkStatus {
    REQUESTING_CONFIGURATION(0), OK(1), ACCESS_DENIED(2), NOT_FOUND(3), PORT_ERROR(4), CLIENT_TOO_OLD(5), AUTHENTICATION_REQUIRED(6);

    private final int id;

    NetworkStatus(int i) {
        this.id = i;
    }

    public static NetworkStatus fromInt(int i) {
        switch (i) {
            case 0:
                return REQUESTING_CONFIGURATION;
            case 1:
                return OK;
            case 2:
                return ACCESS_DENIED;
            case 3:
                return NOT_FOUND;
            case 4:
                return PORT_ERROR;
            case 5:
                return CLIENT_TOO_OLD;
            case 6:
                return AUTHENTICATION_REQUIRED;
            default:
                throw new RuntimeException("Unhandled value: " + i);
        }
    }

    public static NetworkStatus fromVirtualNetworkStatus(VirtualNetworkStatus virtualNetworkStatus) {
        switch (virtualNetworkStatus) {
            case NETWORK_STATUS_REQUESTING_CONFIGURATION:
                return REQUESTING_CONFIGURATION;
            case NETWORK_STATUS_OK:
                return OK;
            case NETWORK_STATUS_ACCESS_DENIED:
                return ACCESS_DENIED;
            case NETWORK_STATUS_NOT_FOUND:
                return NOT_FOUND;
            case NETWORK_STATUS_PORT_ERROR:
                return PORT_ERROR;
            case NETWORK_STATUS_CLIENT_TOO_OLD:
                return CLIENT_TOO_OLD;
            case NETWORK_STATUS_AUTHENTICATION_REQUIRED:
                return AUTHENTICATION_REQUIRED;
            default:
                throw new RuntimeException("Unhandled status: " + virtualNetworkStatus);
        }
    }

    public int toStringId() {
        switch (this) {
            case REQUESTING_CONFIGURATION:
                return R.string.network_status_requesting_configuration;
            case OK:
                return R.string.network_status_ok;
            case ACCESS_DENIED:
                return R.string.network_status_access_denied;
            case NOT_FOUND:
                return R.string.network_status_not_found;
            case PORT_ERROR:
                return R.string.network_status_port_error;
            case CLIENT_TOO_OLD:
                return R.string.network_status_client_too_old;
            case AUTHENTICATION_REQUIRED:
                return R.string.network_status_authentication_required;
            default:
                return R.string.network_status_unknown;
        }
    }

    public int toInt() {
        return this.id;
    }
}

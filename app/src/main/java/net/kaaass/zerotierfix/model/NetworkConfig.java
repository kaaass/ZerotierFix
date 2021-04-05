package net.kaaass.zerotierfix.model;

import net.kaaass.zerotierfix.R;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.converter.PropertyConverter;

import java.util.List;

public class NetworkConfig {
    private List<AssignedAddress> assignedAddresses;
    private boolean bridging;
    private boolean broadcast;
    private transient DaoSession daoSession;
    private int dnsMode;
    private List<DnsServer> dnsServers;
    private Long id;
    private String mac;
    private String mtu;
    private transient NetworkConfigDao myDao;
    private boolean routeViaZeroTier;
    private NetworkStatus status;
    private NetworkType type;
    private boolean useCustomDNS;

    public NetworkConfig(Long l, NetworkType networkType, NetworkStatus networkStatus, String str, String str2, boolean z, boolean z2, boolean z3, boolean z4, int i) {
        this.id = l;
        this.type = networkType;
        this.status = networkStatus;
        this.mac = str;
        this.mtu = str2;
        this.broadcast = z;
        this.bridging = z2;
        this.routeViaZeroTier = z3;
        this.useCustomDNS = z4;
        this.dnsMode = i;
    }

    public NetworkConfig() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long l) {
        this.id = l;
    }

    public NetworkType getType() {
        return this.type;
    }

    public void setType(NetworkType networkType) {
        this.type = networkType;
    }

    public String getMac() {
        return this.mac;
    }

    public void setMac(String str) {
        this.mac = str;
    }

    public String getMtu() {
        return this.mtu;
    }

    public void setMtu(String str) {
        this.mtu = str;
    }

    public boolean getBroadcast() {
        return this.broadcast;
    }

    public void setBroadcast(boolean z) {
        this.broadcast = z;
    }

    public boolean getBridging() {
        return this.bridging;
    }

    public void setBridging(boolean z) {
        this.bridging = z;
    }

    public boolean getRouteViaZeroTier() {
        return this.routeViaZeroTier;
    }

    public void setRouteViaZeroTier(boolean z) {
        this.routeViaZeroTier = z;
    }

    public List<AssignedAddress> getAssignedAddresses() {
        if (this.assignedAddresses == null) {
            DaoSession daoSession2 = this.daoSession;
            if (daoSession2 != null) {
                List<AssignedAddress> _queryNetworkConfig_AssignedAddresses = daoSession2.getAssignedAddressDao()._queryNetworkConfig_AssignedAddresses(this.id.longValue());
                synchronized (this) {
                    if (this.assignedAddresses == null) {
                        this.assignedAddresses = _queryNetworkConfig_AssignedAddresses;
                    }
                }
            } else {
                throw new DaoException("Entity is detached from DAO context");
            }
        }
        return this.assignedAddresses;
    }

    public synchronized void resetAssignedAddresses() {
        this.assignedAddresses = null;
    }

    public void delete() {
        NetworkConfigDao networkConfigDao = this.myDao;
        if (networkConfigDao != null) {
            networkConfigDao.delete(this);
            return;
        }
        throw new DaoException("Entity is detached from DAO context");
    }

    public void refresh() {
        NetworkConfigDao networkConfigDao = this.myDao;
        if (networkConfigDao != null) {
            networkConfigDao.refresh(this);
            return;
        }
        throw new DaoException("Entity is detached from DAO context");
    }

    public void update() {
        NetworkConfigDao networkConfigDao = this.myDao;
        if (networkConfigDao != null) {
            networkConfigDao.update(this);
            return;
        }
        throw new DaoException("Entity is detached from DAO context");
    }

    public NetworkStatus getStatus() {
        return this.status;
    }

    public void setStatus(NetworkStatus networkStatus) {
        this.status = networkStatus;
    }

    public List<DnsServer> getDnsServers() {
        if (this.dnsServers == null) {
            DaoSession daoSession2 = this.daoSession;
            if (daoSession2 != null) {
                List<DnsServer> _queryNetworkConfig_DnsServers = daoSession2.getDnsServerDao()._queryNetworkConfig_DnsServers(this.id);
                synchronized (this) {
                    if (this.dnsServers == null) {
                        this.dnsServers = _queryNetworkConfig_DnsServers;
                    }
                }
            } else {
                throw new DaoException("Entity is detached from DAO context");
            }
        }
        return this.dnsServers;
    }

    public synchronized void resetDnsServers() {
        this.dnsServers = null;
    }

    public boolean getUseCustomDNS() {
        return this.useCustomDNS;
    }

    public void setUseCustomDNS(boolean z) {
        this.useCustomDNS = z;
    }

    public int getDnsMode() {
        return this.dnsMode;
    }

    public void setDnsMode(int i) {
        this.dnsMode = i;
    }

    public void __setDaoSession(DaoSession daoSession2) {
        this.daoSession = daoSession2;
        this.myDao = daoSession2 != null ? daoSession2.getNetworkConfigDao() : null;
    }

    public enum NetworkType {
        UNKNOWN(0),
        PRIVATE(1),
        PUBLIC(2);

        final int id;

        NetworkType(int i) {
            this.id = i;
        }

        public String toString() {
            int i = this.id;
            if (i != 1) {
                return i != 2 ? "Unknown" : "Public";
            }
            return "Private";
        }

        public int toStringId() {
            int i = this.id;
            if (i != 1) {
                return i != 2 ? R.string.network_type_unknown : R.string.network_type_public;
            }
            return R.string.network_type_private;
        }
    }

    public enum DNSMode {
        NO_DNS(0),
        NETWORK_DNS(1),
        CUSTOM_DNS(2);

        final int id;

        DNSMode(int i) {
            this.id = i;
        }

        public String toString() {
            int i = this.id;
            if (i == 0) {
                return "No DNS";
            }
            if (i != 1) {
                return i != 2 ? "Unknown" : "Custom DNS";
            }
            return "Network DNS";
        }
    }

    public enum NetworkStatus {
        UNKNOWN(0),
        OK(1),
        ACCESS_DENIED(2),
        CLIENT_TOO_OLD(3),
        NOT_FOUND(4),
        PORT_ERROR(5),
        REQUESTING_CONFIGURATION(6);

        final int id;

        NetworkStatus(int i) {
            this.id = i;
        }

        public String toString() {
            switch (this.id) {
                case 1:
                    return "OK";
                case 2:
                    return "ACCESS DENIED";
                case 3:
                    return "CLIENT TOO OLD";
                case 4:
                    return "NETWORK NOT FOUND";
                case 5:
                    return "PORT ERROR";
                case 6:
                    return "REQUESTING CONFIGURATION";
                default:
                    return "UNKNOWN";
            }
        }

        public int toStringId() {
            switch (this.id) {
                case 1:
                    return R.string.network_status_ok;
                case 2:
                    return R.string.network_status_access_denied;
                case 3:
                    return R.string.network_status_client_too_old;
                case 4:
                    return R.string.network_status_not_found;
                case 5:
                    return R.string.network_status_port_error;
                case 6:
                    return R.string.network_status_requesting_configuration;
                default:
                    return R.string.network_status_unknown;
            }
        }
    }

    public static class NetworkTypeConverter implements PropertyConverter<NetworkType, Integer> {
        public NetworkType convertToEntityProperty(Integer num) {
            if (num == null) {
                return null;
            }
            NetworkType[] values = NetworkType.values();
            for (NetworkType networkType : values) {
                if (networkType.id == num.intValue()) {
                    return networkType;
                }
            }
            return NetworkType.PRIVATE;
        }

        public Integer convertToDatabaseValue(NetworkType networkType) {
            if (networkType == null) {
                return null;
            }
            return Integer.valueOf(networkType.id);
        }
    }

    public static class NetworkStatusConverter implements PropertyConverter<NetworkStatus, Integer> {
        public NetworkStatus convertToEntityProperty(Integer num) {
            if (num == null) {
                return null;
            }
            NetworkStatus[] values = NetworkStatus.values();
            for (NetworkStatus networkStatus : values) {
                if (networkStatus.id == num.intValue()) {
                    return networkStatus;
                }
            }
            return NetworkStatus.UNKNOWN;
        }

        public Integer convertToDatabaseValue(NetworkStatus networkStatus) {
            if (networkStatus == null) {
                return null;
            }
            return Integer.valueOf(networkStatus.id);
        }
    }
}

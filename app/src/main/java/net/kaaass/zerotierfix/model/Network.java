package net.kaaass.zerotierfix.model;

import org.greenrobot.greendao.DaoException;

public class Network {
    private boolean connected;
    private transient DaoSession daoSession;
    private boolean lastActivated;
    private transient NetworkDao myDao;
    private NetworkConfig networkConfig;
    private long networkConfigId;
    private transient Long networkConfig__resolvedKey;
    private Long networkId;
    private String networkIdStr;
    private String networkName;
    private boolean useDefaultRoute;

    public Network(Long l, String str, String str2, boolean z, boolean z2, long j) {
        this.networkId = l;
        this.networkIdStr = str;
        this.networkName = str2;
        this.useDefaultRoute = z;
        this.lastActivated = z2;
        this.networkConfigId = j;
    }

    public Network() {
    }

    public Long getNetworkId() {
        return this.networkId;
    }

    public void setNetworkId(Long l) {
        this.networkId = l;
    }

    public String getNetworkIdStr() {
        return this.networkIdStr;
    }

    public void setNetworkIdStr(String str) {
        this.networkIdStr = str;
    }

    public String getNetworkName() {
        return this.networkName;
    }

    public void setNetworkName(String str) {
        this.networkName = str;
    }

    public long getNetworkConfigId() {
        return this.networkConfigId;
    }

    public void setNetworkConfigId(long j) {
        this.networkConfigId = j;
    }

    public NetworkConfig getNetworkConfig() {
        long j = this.networkConfigId;
        Long l = this.networkConfig__resolvedKey;
        if (l == null || !l.equals(Long.valueOf(j))) {
            DaoSession daoSession2 = this.daoSession;
            if (daoSession2 != null) {
                NetworkConfig networkConfig2 = (NetworkConfig) daoSession2.getNetworkConfigDao().load(Long.valueOf(j));
                synchronized (this) {
                    this.networkConfig = networkConfig2;
                    this.networkConfig__resolvedKey = Long.valueOf(j);
                }
            } else {
                throw new DaoException("Entity is detached from DAO context");
            }
        }
        return this.networkConfig;
    }

    public void setNetworkConfig(NetworkConfig networkConfig2) {
        if (networkConfig2 != null) {
            synchronized (this) {
                this.networkConfig = networkConfig2;
                long longValue = networkConfig2.getId().longValue();
                this.networkConfigId = longValue;
                this.networkConfig__resolvedKey = Long.valueOf(longValue);
            }
            return;
        }
        throw new DaoException("To-one property 'networkConfigId' has not-null constraint; cannot set to-one to null");
    }

    public void delete() {
        NetworkDao networkDao = this.myDao;
        if (networkDao != null) {
            networkDao.delete(this);
            return;
        }
        throw new DaoException("Entity is detached from DAO context");
    }

    public void refresh() {
        NetworkDao networkDao = this.myDao;
        if (networkDao != null) {
            networkDao.refresh(this);
            return;
        }
        throw new DaoException("Entity is detached from DAO context");
    }

    public void update() {
        NetworkDao networkDao = this.myDao;
        if (networkDao != null) {
            networkDao.update(this);
            return;
        }
        throw new DaoException("Entity is detached from DAO context");
    }

    public boolean getUseDefaultRoute() {
        return this.useDefaultRoute;
    }

    public void setUseDefaultRoute(boolean z) {
        this.useDefaultRoute = z;
    }

    public boolean getConnected() {
        return this.connected;
    }

    public void setConnected(boolean z) {
        this.connected = z;
    }

    public boolean getLastActivated() {
        return this.lastActivated;
    }

    public void setLastActivated(boolean z) {
        this.lastActivated = z;
    }

    public void __setDaoSession(DaoSession daoSession2) {
        this.daoSession = daoSession2;
        this.myDao = daoSession2 != null ? daoSession2.getNetworkDao() : null;
    }
}

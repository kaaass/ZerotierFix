package net.kaaass.zerotierfix.model;

import androidx.annotation.Nullable;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;

@Entity
public class Network {
    @Id
    private Long networkId;

    private String networkIdStr;

    private String networkName;

    @Deprecated
    private boolean useDefaultRoute;

    private boolean lastActivated;

    private long networkConfigId;

    @Transient
    @Deprecated
    private boolean connected;

    @ToOne(joinProperty = "networkConfigId")
    private NetworkConfig networkConfig;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1869807503)
    private transient NetworkDao myDao;

    @Generated(hash = 1813013561)
    public Network(Long networkId, String networkIdStr, String networkName, boolean useDefaultRoute,
            boolean lastActivated, long networkConfigId) {
        this.networkId = networkId;
        this.networkIdStr = networkIdStr;
        this.networkName = networkName;
        this.useDefaultRoute = useDefaultRoute;
        this.lastActivated = lastActivated;
        this.networkConfigId = networkConfigId;
    }

    @Generated(hash = 1981325040)
    public Network() {
    }

    public Long getNetworkId() {
        return this.networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public String getNetworkIdStr() {
        return this.networkIdStr;
    }

    public void setNetworkIdStr(String networkIdStr) {
        this.networkIdStr = networkIdStr;
    }

    public String getNetworkName() {
        return this.networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    @Deprecated
    public boolean getUseDefaultRoute() {
        return this.useDefaultRoute;
    }

    @Deprecated
    public void setUseDefaultRoute(boolean useDefaultRoute) {
        this.useDefaultRoute = useDefaultRoute;
    }

    public boolean getLastActivated() {
        return this.lastActivated;
    }

    public void setLastActivated(boolean lastActivated) {
        this.lastActivated = lastActivated;
    }

    public long getNetworkConfigId() {
        return this.networkConfigId;
    }

    public void setNetworkConfigId(long networkConfigId) {
        this.networkConfigId = networkConfigId;
    }

    @Deprecated
    public boolean getConnected() {
        return this.connected;
    }

    @Deprecated
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Generated(hash = 1230649737)
    private transient Long networkConfig__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 507065422)
    public NetworkConfig getNetworkConfig() {
        long __key = this.networkConfigId;
        if (networkConfig__resolvedKey == null
                || !networkConfig__resolvedKey.equals(__key)) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            NetworkConfigDao targetDao = daoSession.getNetworkConfigDao();
            NetworkConfig networkConfigNew = targetDao.load(__key);
            synchronized (this) {
                networkConfig = networkConfigNew;
                networkConfig__resolvedKey = __key;
            }
        }
        return networkConfig;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 26187198)
    public void setNetworkConfig(@NotNull NetworkConfig networkConfig) {
        if (networkConfig == null) {
            throw new DaoException(
                    "To-one property 'networkConfigId' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.networkConfig = networkConfig;
            networkConfigId = networkConfig.getId();
            networkConfig__resolvedKey = networkConfigId;
        }
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 75076313)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getNetworkDao() : null;
    }
}

package net.kaaass.zerotierfix.model;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import java.util.Map;

public class DaoSession extends AbstractDaoSession {
    private final AppNodeDao appNodeDao;
    private final DaoConfig appNodeDaoConfig;
    private final AssignedAddressDao assignedAddressDao;
    private final DaoConfig assignedAddressDaoConfig;
    private final DnsServerDao dnsServerDao;
    private final DaoConfig dnsServerDaoConfig;
    private final NetworkConfigDao networkConfigDao;
    private final DaoConfig networkConfigDaoConfig;
    private final NetworkDao networkDao;
    private final DaoConfig networkDaoConfig;

    public DaoSession(Database database, IdentityScopeType identityScopeType, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig> map) {
        super(database);
        DaoConfig clone = map.get(AppNodeDao.class).clone();
        this.appNodeDaoConfig = clone;
        clone.initIdentityScope(identityScopeType);
        DaoConfig clone2 = map.get(AssignedAddressDao.class).clone();
        this.assignedAddressDaoConfig = clone2;
        clone2.initIdentityScope(identityScopeType);
        DaoConfig clone3 = map.get(DnsServerDao.class).clone();
        this.dnsServerDaoConfig = clone3;
        clone3.initIdentityScope(identityScopeType);
        DaoConfig clone4 = map.get(NetworkDao.class).clone();
        this.networkDaoConfig = clone4;
        clone4.initIdentityScope(identityScopeType);
        DaoConfig clone5 = map.get(NetworkConfigDao.class).clone();
        this.networkConfigDaoConfig = clone5;
        clone5.initIdentityScope(identityScopeType);
        AppNodeDao appNodeDao2 = new AppNodeDao(clone, this);
        this.appNodeDao = appNodeDao2;
        AssignedAddressDao assignedAddressDao2 = new AssignedAddressDao(clone2, this);
        this.assignedAddressDao = assignedAddressDao2;
        DnsServerDao dnsServerDao2 = new DnsServerDao(clone3, this);
        this.dnsServerDao = dnsServerDao2;
        NetworkDao networkDao2 = new NetworkDao(clone4, this);
        this.networkDao = networkDao2;
        NetworkConfigDao networkConfigDao2 = new NetworkConfigDao(clone5, this);
        this.networkConfigDao = networkConfigDao2;
        registerDao(AppNode.class, appNodeDao2);
        registerDao(AssignedAddress.class, assignedAddressDao2);
        registerDao(DnsServer.class, dnsServerDao2);
        registerDao(Network.class, networkDao2);
        registerDao(NetworkConfig.class, networkConfigDao2);
    }

    public void clear() {
        this.appNodeDaoConfig.clearIdentityScope();
        this.assignedAddressDaoConfig.clearIdentityScope();
        this.dnsServerDaoConfig.clearIdentityScope();
        this.networkDaoConfig.clearIdentityScope();
        this.networkConfigDaoConfig.clearIdentityScope();
    }

    public AppNodeDao getAppNodeDao() {
        return this.appNodeDao;
    }

    public AssignedAddressDao getAssignedAddressDao() {
        return this.assignedAddressDao;
    }

    public DnsServerDao getDnsServerDao() {
        return this.dnsServerDao;
    }

    public NetworkDao getNetworkDao() {
        return this.networkDao;
    }

    public NetworkConfigDao getNetworkConfigDao() {
        return this.networkConfigDao;
    }
}

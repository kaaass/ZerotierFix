package net.kaaass.zerotierfix.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import androidx.core.app.NotificationCompat;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.internal.DaoConfig;

public class NetworkConfigDao extends AbstractDao<NetworkConfig, Long> {
    public static final String TABLENAME = "NETWORK_CONFIG";
    private DaoSession daoSession;
    private final NetworkConfig.NetworkStatusConverter statusConverter = new NetworkConfig.NetworkStatusConverter();
    private final NetworkConfig.NetworkTypeConverter typeConverter = new NetworkConfig.NetworkTypeConverter();

    public static class Properties {
        public static final Property Bridging = new Property(6, Boolean.TYPE, "bridging", false, "BRIDGING");
        public static final Property Broadcast = new Property(5, Boolean.TYPE, "broadcast", false, "BROADCAST");
        public static final Property DnsMode = new Property(9, Integer.TYPE, "dnsMode", false, "DNS_MODE");
        public static final Property Id = new Property(0, Long.class, "id", true, "_id");
        public static final Property Mac = new Property(3, String.class, "mac", false, "MAC");
        public static final Property Mtu = new Property(4, String.class, "mtu", false, "MTU");
        public static final Property RouteViaZeroTier = new Property(7, Boolean.TYPE, "routeViaZeroTier", false, "ROUTE_VIA_ZERO_TIER");
        public static final Property Status = new Property(2, Integer.class, NotificationCompat.CATEGORY_STATUS, false, "STATUS");
        public static final Property Type = new Property(1, Integer.class, "type", false, "TYPE");
        public static final Property UseCustomDNS = new Property(8, Boolean.TYPE, "useCustomDNS", false, "USE_CUSTOM_DNS");
    }

    /* access modifiers changed from: protected */
    @Override // org.greenrobot.greendao.AbstractDao
    public final boolean isEntityUpdateable() {
        return true;
    }

    public NetworkConfigDao(DaoConfig daoConfig) {
        super(daoConfig);
    }

    public NetworkConfigDao(DaoConfig daoConfig, DaoSession daoSession2) {
        super(daoConfig, daoSession2);
        this.daoSession = daoSession2;
    }

    public static void createTable(Database database, boolean z) {
        database.execSQL("CREATE TABLE " + (z ? "IF NOT EXISTS " : "") + "\"NETWORK_CONFIG\" (\"_id\" INTEGER PRIMARY KEY ,\"TYPE\" INTEGER,\"STATUS\" INTEGER,\"MAC\" TEXT,\"MTU\" TEXT,\"BROADCAST\" INTEGER NOT NULL ,\"BRIDGING\" INTEGER NOT NULL ,\"ROUTE_VIA_ZERO_TIER\" INTEGER NOT NULL ,\"USE_CUSTOM_DNS\" INTEGER NOT NULL ,\"DNS_MODE\" INTEGER NOT NULL );");
    }

    public static void dropTable(Database database, boolean z) {
        database.execSQL("DROP TABLE " + (z ? "IF EXISTS " : "") + "\"NETWORK_CONFIG\"");
    }

    /* access modifiers changed from: protected */
    public final void bindValues(DatabaseStatement databaseStatement, NetworkConfig networkConfig) {
        databaseStatement.clearBindings();
        Long id = networkConfig.getId();
        if (id != null) {
            databaseStatement.bindLong(1, id.longValue());
        }
        NetworkConfig.NetworkType type = networkConfig.getType();
        if (type != null) {
            databaseStatement.bindLong(2, (long) this.typeConverter.convertToDatabaseValue(type).intValue());
        }
        NetworkConfig.NetworkStatus status = networkConfig.getStatus();
        if (status != null) {
            databaseStatement.bindLong(3, (long) this.statusConverter.convertToDatabaseValue(status).intValue());
        }
        String mac = networkConfig.getMac();
        if (mac != null) {
            databaseStatement.bindString(4, mac);
        }
        String mtu = networkConfig.getMtu();
        if (mtu != null) {
            databaseStatement.bindString(5, mtu);
        }
        long j = 1;
        databaseStatement.bindLong(6, networkConfig.getBroadcast() ? 1 : 0);
        databaseStatement.bindLong(7, networkConfig.getBridging() ? 1 : 0);
        databaseStatement.bindLong(8, networkConfig.getRouteViaZeroTier() ? 1 : 0);
        if (!networkConfig.getUseCustomDNS()) {
            j = 0;
        }
        databaseStatement.bindLong(9, j);
        databaseStatement.bindLong(10, (long) networkConfig.getDnsMode());
    }

    /* access modifiers changed from: protected */
    public final void bindValues(SQLiteStatement sQLiteStatement, NetworkConfig networkConfig) {
        sQLiteStatement.clearBindings();
        Long id = networkConfig.getId();
        if (id != null) {
            sQLiteStatement.bindLong(1, id.longValue());
        }
        NetworkConfig.NetworkType type = networkConfig.getType();
        if (type != null) {
            sQLiteStatement.bindLong(2, (long) this.typeConverter.convertToDatabaseValue(type).intValue());
        }
        NetworkConfig.NetworkStatus status = networkConfig.getStatus();
        if (status != null) {
            sQLiteStatement.bindLong(3, (long) this.statusConverter.convertToDatabaseValue(status).intValue());
        }
        String mac = networkConfig.getMac();
        if (mac != null) {
            sQLiteStatement.bindString(4, mac);
        }
        String mtu = networkConfig.getMtu();
        if (mtu != null) {
            sQLiteStatement.bindString(5, mtu);
        }
        long j = 1;
        sQLiteStatement.bindLong(6, networkConfig.getBroadcast() ? 1 : 0);
        sQLiteStatement.bindLong(7, networkConfig.getBridging() ? 1 : 0);
        sQLiteStatement.bindLong(8, networkConfig.getRouteViaZeroTier() ? 1 : 0);
        if (!networkConfig.getUseCustomDNS()) {
            j = 0;
        }
        sQLiteStatement.bindLong(9, j);
        sQLiteStatement.bindLong(10, (long) networkConfig.getDnsMode());
    }

    /* access modifiers changed from: protected */
    public final void attachEntity(NetworkConfig networkConfig) {
        super.attachEntity(networkConfig);
        networkConfig.__setDaoSession(this.daoSession);
    }

    @Override // org.greenrobot.greendao.AbstractDao
    public Long readKey(Cursor cursor, int i) {
        int i2 = i + 0;
        if (cursor.isNull(i2)) {
            return null;
        }
        return Long.valueOf(cursor.getLong(i2));
    }

    @Override // org.greenrobot.greendao.AbstractDao
    public NetworkConfig readEntity(Cursor cursor, int i) {
        int i2 = i + 0;
        Long valueOf = cursor.isNull(i2) ? null : Long.valueOf(cursor.getLong(i2));
        int i3 = i + 1;
        NetworkConfig.NetworkType convertToEntityProperty = cursor.isNull(i3) ? null : this.typeConverter.convertToEntityProperty(Integer.valueOf(cursor.getInt(i3)));
        int i4 = i + 2;
        NetworkConfig.NetworkStatus convertToEntityProperty2 = cursor.isNull(i4) ? null : this.statusConverter.convertToEntityProperty(Integer.valueOf(cursor.getInt(i4)));
        int i5 = i + 3;
        String string = cursor.isNull(i5) ? null : cursor.getString(i5);
        int i6 = i + 4;
        return new NetworkConfig(valueOf, convertToEntityProperty, convertToEntityProperty2, string, cursor.isNull(i6) ? null : cursor.getString(i6), cursor.getShort(i + 5) != 0, cursor.getShort(i + 6) != 0, cursor.getShort(i + 7) != 0, cursor.getShort(i + 8) != 0, cursor.getInt(i + 9));
    }

    public void readEntity(Cursor cursor, NetworkConfig networkConfig, int i) {
        int i2 = i + 0;
        String str = null;
        networkConfig.setId(cursor.isNull(i2) ? null : Long.valueOf(cursor.getLong(i2)));
        int i3 = i + 1;
        networkConfig.setType(cursor.isNull(i3) ? null : this.typeConverter.convertToEntityProperty(Integer.valueOf(cursor.getInt(i3))));
        int i4 = i + 2;
        networkConfig.setStatus(cursor.isNull(i4) ? null : this.statusConverter.convertToEntityProperty(Integer.valueOf(cursor.getInt(i4))));
        int i5 = i + 3;
        networkConfig.setMac(cursor.isNull(i5) ? null : cursor.getString(i5));
        int i6 = i + 4;
        if (!cursor.isNull(i6)) {
            str = cursor.getString(i6);
        }
        networkConfig.setMtu(str);
        boolean z = true;
        networkConfig.setBroadcast(cursor.getShort(i + 5) != 0);
        networkConfig.setBridging(cursor.getShort(i + 6) != 0);
        networkConfig.setRouteViaZeroTier(cursor.getShort(i + 7) != 0);
        if (cursor.getShort(i + 8) == 0) {
            z = false;
        }
        networkConfig.setUseCustomDNS(z);
        networkConfig.setDnsMode(cursor.getInt(i + 9));
    }

    /* access modifiers changed from: protected */
    public final Long updateKeyAfterInsert(NetworkConfig networkConfig, long j) {
        networkConfig.setId(Long.valueOf(j));
        return Long.valueOf(j);
    }

    public Long getKey(NetworkConfig networkConfig) {
        if (networkConfig != null) {
            return networkConfig.getId();
        }
        return null;
    }

    public boolean hasKey(NetworkConfig networkConfig) {
        return networkConfig.getId() != null;
    }
}

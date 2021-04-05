package net.kaaass.zerotierfix.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.internal.SqlUtils;

import java.util.ArrayList;
import java.util.List;

public class NetworkDao extends AbstractDao<Network, Long> {
    public static final String TABLENAME = "NETWORK";
    private DaoSession daoSession;
    private String selectDeep;

    public NetworkDao(DaoConfig daoConfig) {
        super(daoConfig);
    }

    public NetworkDao(DaoConfig daoConfig, DaoSession daoSession2) {
        super(daoConfig, daoSession2);
        this.daoSession = daoSession2;
    }

    public static void createTable(Database database, boolean z) {
        database.execSQL("CREATE TABLE " + (z ? "IF NOT EXISTS " : "") + "\"NETWORK\" (\"_id\" INTEGER PRIMARY KEY ,\"NETWORK_ID_STR\" TEXT,\"NETWORK_NAME\" TEXT,\"USE_DEFAULT_ROUTE\" INTEGER NOT NULL ,\"LAST_ACTIVATED\" INTEGER NOT NULL ,\"NETWORK_CONFIG_ID\" INTEGER NOT NULL );");
    }

    public static void dropTable(Database database, boolean z) {
        database.execSQL("DROP TABLE " + (z ? "IF EXISTS " : "") + "\"NETWORK\"");
    }

    /* access modifiers changed from: protected */
    @Override // org.greenrobot.greendao.AbstractDao
    public final boolean isEntityUpdateable() {
        return true;
    }

    /* access modifiers changed from: protected */
    public final void bindValues(DatabaseStatement databaseStatement, Network network) {
        databaseStatement.clearBindings();
        Long networkId = network.getNetworkId();
        if (networkId != null) {
            databaseStatement.bindLong(1, networkId.longValue());
        }
        String networkIdStr = network.getNetworkIdStr();
        if (networkIdStr != null) {
            databaseStatement.bindString(2, networkIdStr);
        }
        String networkName = network.getNetworkName();
        if (networkName != null) {
            databaseStatement.bindString(3, networkName);
        }
        long j = 1;
        databaseStatement.bindLong(4, network.getUseDefaultRoute() ? 1 : 0);
        if (!network.getLastActivated()) {
            j = 0;
        }
        databaseStatement.bindLong(5, j);
        databaseStatement.bindLong(6, network.getNetworkConfigId());
    }

    /* access modifiers changed from: protected */
    public final void bindValues(SQLiteStatement sQLiteStatement, Network network) {
        sQLiteStatement.clearBindings();
        Long networkId = network.getNetworkId();
        if (networkId != null) {
            sQLiteStatement.bindLong(1, networkId.longValue());
        }
        String networkIdStr = network.getNetworkIdStr();
        if (networkIdStr != null) {
            sQLiteStatement.bindString(2, networkIdStr);
        }
        String networkName = network.getNetworkName();
        if (networkName != null) {
            sQLiteStatement.bindString(3, networkName);
        }
        long j = 1;
        sQLiteStatement.bindLong(4, network.getUseDefaultRoute() ? 1 : 0);
        if (!network.getLastActivated()) {
            j = 0;
        }
        sQLiteStatement.bindLong(5, j);
        sQLiteStatement.bindLong(6, network.getNetworkConfigId());
    }

    /* access modifiers changed from: protected */
    public final void attachEntity(Network network) {
        super.attachEntity(network);
        network.__setDaoSession(this.daoSession);
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
    public Network readEntity(Cursor cursor, int i) {
        int i2 = i + 0;
        Long valueOf = cursor.isNull(i2) ? null : Long.valueOf(cursor.getLong(i2));
        int i3 = i + 1;
        String string = cursor.isNull(i3) ? null : cursor.getString(i3);
        int i4 = i + 2;
        String string2 = cursor.isNull(i4) ? null : cursor.getString(i4);
        boolean z = false;
        boolean z2 = cursor.getShort(i + 3) != 0;
        if (cursor.getShort(i + 4) != 0) {
            z = true;
        }
        return new Network(valueOf, string, string2, z2, z, cursor.getLong(i + 5));
    }

    public void readEntity(Cursor cursor, Network network, int i) {
        int i2 = i + 0;
        String str = null;
        network.setNetworkId(cursor.isNull(i2) ? null : Long.valueOf(cursor.getLong(i2)));
        int i3 = i + 1;
        network.setNetworkIdStr(cursor.isNull(i3) ? null : cursor.getString(i3));
        int i4 = i + 2;
        if (!cursor.isNull(i4)) {
            str = cursor.getString(i4);
        }
        network.setNetworkName(str);
        boolean z = true;
        network.setUseDefaultRoute(cursor.getShort(i + 3) != 0);
        if (cursor.getShort(i + 4) == 0) {
            z = false;
        }
        network.setLastActivated(z);
        network.setNetworkConfigId(cursor.getLong(i + 5));
    }

    /* access modifiers changed from: protected */
    public final Long updateKeyAfterInsert(Network network, long j) {
        network.setNetworkId(Long.valueOf(j));
        return Long.valueOf(j);
    }

    public Long getKey(Network network) {
        if (network != null) {
            return network.getNetworkId();
        }
        return null;
    }

    public boolean hasKey(Network network) {
        return network.getNetworkId() != null;
    }

    /* access modifiers changed from: protected */
    public String getSelectDeep() {
        if (this.selectDeep == null) {
            StringBuilder sb = new StringBuilder("SELECT ");
            SqlUtils.appendColumns(sb, "T", getAllColumns());
            sb.append(',');
            SqlUtils.appendColumns(sb, "T0", this.daoSession.getNetworkConfigDao().getAllColumns());
            sb.append(" FROM NETWORK T");
            sb.append(" LEFT JOIN NETWORK_CONFIG T0 ON T.\"NETWORK_CONFIG_ID\"=T0.\"_id\"");
            sb.append(' ');
            this.selectDeep = sb.toString();
        }
        return this.selectDeep;
    }

    /* access modifiers changed from: protected */
    public Network loadCurrentDeep(Cursor cursor, boolean z) {
        Network network = loadCurrent(cursor, 0, z);
        NetworkConfig networkConfig = loadCurrentOther(this.daoSession.getNetworkConfigDao(), cursor, getAllColumns().length);
        if (networkConfig != null) {
            network.setNetworkConfig(networkConfig);
        }
        return network;
    }

    public Network loadDeep(Long l) {
        assertSinglePk();
        if (l == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(getSelectDeep());
        sb.append("WHERE ");
        SqlUtils.appendColumnsEqValue(sb, "T", getPkColumns());
        Cursor rawQuery = this.db.rawQuery(sb.toString(), new String[]{l.toString()});
        try {
            if (!rawQuery.moveToFirst()) {
                return null;
            }
            if (rawQuery.isLast()) {
                Network loadCurrentDeep = loadCurrentDeep(rawQuery, true);
                rawQuery.close();
                return loadCurrentDeep;
            }
            throw new IllegalStateException("Expected unique result, but count was " + rawQuery.getCount());
        } finally {
            rawQuery.close();
        }
    }

    public List<Network> loadAllDeepFromCursor(Cursor cursor) {
        int count = cursor.getCount();
        ArrayList arrayList = new ArrayList(count);
        if (cursor.moveToFirst()) {
            if (this.identityScope != null) {
                this.identityScope.lock();
                this.identityScope.reserveRoom(count);
            }
            do {
                try {
                    arrayList.add(loadCurrentDeep(cursor, false));
                } finally {
                    if (this.identityScope != null) {
                        this.identityScope.unlock();
                    }
                }
            } while (cursor.moveToNext());
        }
        return arrayList;
    }

    /* access modifiers changed from: protected */
    public List<Network> loadDeepAllAndCloseCursor(Cursor cursor) {
        try {
            return loadAllDeepFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }

    public List<Network> queryDeep(String str, String... strArr) {
        return loadDeepAllAndCloseCursor(this.db.rawQuery(getSelectDeep() + str, strArr));
    }

    public static class Properties {
        public static final Property LastActivated = new Property(4, Boolean.TYPE, "lastActivated", false, "LAST_ACTIVATED");
        public static final Property NetworkConfigId = new Property(5, Long.TYPE, "networkConfigId", false, "NETWORK_CONFIG_ID");
        public static final Property NetworkId = new Property(0, Long.class, "networkId", true, "_id");
        public static final Property NetworkIdStr = new Property(1, String.class, "networkIdStr", false, "NETWORK_ID_STR");
        public static final Property NetworkName = new Property(2, String.class, "networkName", false, "NETWORK_NAME");
        public static final Property UseDefaultRoute = new Property(3, Boolean.TYPE, "useDefaultRoute", false, "USE_DEFAULT_ROUTE");
    }
}

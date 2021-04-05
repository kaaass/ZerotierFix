package net.kaaass.zerotierfix.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import java.util.List;
import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

public class DnsServerDao extends AbstractDao<DnsServer, Long> {
    public static final String TABLENAME = "DNS_SERVER";
    private Query<DnsServer> networkConfig_DnsServersQuery;

    public static class Properties {
        public static final Property Id = new Property(0, Long.class, "id", true, "_id");
        public static final Property Nameserver = new Property(2, String.class, "nameserver", false, "NAMESERVER");
        public static final Property NetworkId = new Property(1, Long.class, "networkId", false, "NETWORK_ID");
    }

    /* access modifiers changed from: protected */
    @Override // org.greenrobot.greendao.AbstractDao
    public final boolean isEntityUpdateable() {
        return true;
    }

    public DnsServerDao(DaoConfig daoConfig) {
        super(daoConfig);
    }

    public DnsServerDao(DaoConfig daoConfig, DaoSession daoSession) {
        super(daoConfig, daoSession);
    }

    public static void createTable(Database database, boolean z) {
        String str = z ? "IF NOT EXISTS " : "";
        database.execSQL("CREATE TABLE " + str + "\"DNS_SERVER\" (\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ,\"NETWORK_ID\" INTEGER,\"NAMESERVER\" TEXT);");
        database.execSQL("CREATE INDEX " + str + "IDX_DNS_SERVER_NETWORK_ID ON \"DNS_SERVER\" (\"NETWORK_ID\" ASC);");
    }

    public static void dropTable(Database database, boolean z) {
        database.execSQL("DROP TABLE " + (z ? "IF EXISTS " : "") + "\"DNS_SERVER\"");
    }

    /* access modifiers changed from: protected */
    public final void bindValues(DatabaseStatement databaseStatement, DnsServer dnsServer) {
        databaseStatement.clearBindings();
        Long id = dnsServer.getId();
        if (id != null) {
            databaseStatement.bindLong(1, id.longValue());
        }
        Long networkId = dnsServer.getNetworkId();
        if (networkId != null) {
            databaseStatement.bindLong(2, networkId.longValue());
        }
        String nameserver = dnsServer.getNameserver();
        if (nameserver != null) {
            databaseStatement.bindString(3, nameserver);
        }
    }

    /* access modifiers changed from: protected */
    public final void bindValues(SQLiteStatement sQLiteStatement, DnsServer dnsServer) {
        sQLiteStatement.clearBindings();
        Long id = dnsServer.getId();
        if (id != null) {
            sQLiteStatement.bindLong(1, id.longValue());
        }
        Long networkId = dnsServer.getNetworkId();
        if (networkId != null) {
            sQLiteStatement.bindLong(2, networkId.longValue());
        }
        String nameserver = dnsServer.getNameserver();
        if (nameserver != null) {
            sQLiteStatement.bindString(3, nameserver);
        }
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
    public DnsServer readEntity(Cursor cursor, int i) {
        int i2 = i + 0;
        String str = null;
        Long valueOf = cursor.isNull(i2) ? null : Long.valueOf(cursor.getLong(i2));
        int i3 = i + 1;
        Long valueOf2 = cursor.isNull(i3) ? null : Long.valueOf(cursor.getLong(i3));
        int i4 = i + 2;
        if (!cursor.isNull(i4)) {
            str = cursor.getString(i4);
        }
        return new DnsServer(valueOf, valueOf2, str);
    }

    public void readEntity(Cursor cursor, DnsServer dnsServer, int i) {
        int i2 = i + 0;
        String str = null;
        dnsServer.setId(cursor.isNull(i2) ? null : Long.valueOf(cursor.getLong(i2)));
        int i3 = i + 1;
        dnsServer.setNetworkId(cursor.isNull(i3) ? null : Long.valueOf(cursor.getLong(i3)));
        int i4 = i + 2;
        if (!cursor.isNull(i4)) {
            str = cursor.getString(i4);
        }
        dnsServer.setNameserver(str);
    }

    /* access modifiers changed from: protected */
    public final Long updateKeyAfterInsert(DnsServer dnsServer, long j) {
        dnsServer.setId(Long.valueOf(j));
        return Long.valueOf(j);
    }

    public Long getKey(DnsServer dnsServer) {
        if (dnsServer != null) {
            return dnsServer.getId();
        }
        return null;
    }

    public boolean hasKey(DnsServer dnsServer) {
        return dnsServer.getId() != null;
    }

    public List<DnsServer> _queryNetworkConfig_DnsServers(Long l) {
        synchronized (this) {
            if (this.networkConfig_DnsServersQuery == null) {
                QueryBuilder queryBuilder = queryBuilder();
                queryBuilder.where(Properties.NetworkId.eq(null), new WhereCondition[0]);
                this.networkConfig_DnsServersQuery = queryBuilder.build();
            }
        }
        Query<DnsServer> forCurrentThread = this.networkConfig_DnsServersQuery.forCurrentThread();
        forCurrentThread.setParameter(0, (Object) l);
        return forCurrentThread.list();
    }
}

package net.kaaass.zerotierfix.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.List;

public class AssignedAddressDao extends AbstractDao<AssignedAddress, Long> {
    public static final String TABLENAME = "ASSIGNED_ADDRESS";
    private final AssignedAddress.AddressTypeConverter typeConverter = new AssignedAddress.AddressTypeConverter();
    private Query<AssignedAddress> networkConfig_AssignedAddressesQuery;

    public AssignedAddressDao(DaoConfig daoConfig) {
        super(daoConfig);
    }

    public AssignedAddressDao(DaoConfig daoConfig, DaoSession daoSession) {
        super(daoConfig, daoSession);
    }

    public static void createTable(Database database, boolean z) {
        database.execSQL("CREATE TABLE " + (z ? "IF NOT EXISTS " : "") + "\"ASSIGNED_ADDRESS\" (\"_id\" INTEGER PRIMARY KEY ,\"NETWORK_ID\" INTEGER NOT NULL ,\"TYPE\" INTEGER,\"ADDRESS_BYTES\" BLOB,\"ADDRESS_STRING\" TEXT,\"PREFIX\" INTEGER NOT NULL );");
    }

    public static void dropTable(Database database, boolean z) {
        database.execSQL("DROP TABLE " + (z ? "IF EXISTS " : "") + "\"ASSIGNED_ADDRESS\"");
    }

    /* access modifiers changed from: protected */
    @Override // org.greenrobot.greendao.AbstractDao
    public final boolean isEntityUpdateable() {
        return true;
    }

    /* access modifiers changed from: protected */
    public final void bindValues(DatabaseStatement databaseStatement, AssignedAddress assignedAddress) {
        databaseStatement.clearBindings();
        Long id = assignedAddress.getId();
        if (id != null) {
            databaseStatement.bindLong(1, id.longValue());
        }
        databaseStatement.bindLong(2, assignedAddress.getNetworkId());
        AssignedAddress.AddressType type = assignedAddress.getType();
        if (type != null) {
            databaseStatement.bindLong(3, this.typeConverter.convertToDatabaseValue(type).intValue());
        }
        byte[] addressBytes = assignedAddress.getAddressBytes();
        if (addressBytes != null) {
            databaseStatement.bindBlob(4, addressBytes);
        }
        String addressString = assignedAddress.getAddressString();
        if (addressString != null) {
            databaseStatement.bindString(5, addressString);
        }
        databaseStatement.bindLong(6, assignedAddress.getPrefix());
    }

    /* access modifiers changed from: protected */
    public final void bindValues(SQLiteStatement sQLiteStatement, AssignedAddress assignedAddress) {
        sQLiteStatement.clearBindings();
        Long id = assignedAddress.getId();
        if (id != null) {
            sQLiteStatement.bindLong(1, id.longValue());
        }
        sQLiteStatement.bindLong(2, assignedAddress.getNetworkId());
        AssignedAddress.AddressType type = assignedAddress.getType();
        if (type != null) {
            sQLiteStatement.bindLong(3, this.typeConverter.convertToDatabaseValue(type).intValue());
        }
        byte[] addressBytes = assignedAddress.getAddressBytes();
        if (addressBytes != null) {
            sQLiteStatement.bindBlob(4, addressBytes);
        }
        String addressString = assignedAddress.getAddressString();
        if (addressString != null) {
            sQLiteStatement.bindString(5, addressString);
        }
        sQLiteStatement.bindLong(6, assignedAddress.getPrefix());
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
    public AssignedAddress readEntity(Cursor cursor, int i) {
        int i2 = i + 0;
        Long valueOf = cursor.isNull(i2) ? null : Long.valueOf(cursor.getLong(i2));
        long j = cursor.getLong(i + 1);
        int i3 = i + 2;
        AssignedAddress.AddressType convertToEntityProperty = cursor.isNull(i3) ? null : this.typeConverter.convertToEntityProperty(Integer.valueOf(cursor.getInt(i3)));
        int i4 = i + 3;
        byte[] blob = cursor.isNull(i4) ? null : cursor.getBlob(i4);
        int i5 = i + 4;
        return new AssignedAddress(valueOf, j, convertToEntityProperty, blob, cursor.isNull(i5) ? null : cursor.getString(i5), cursor.getShort(i + 5));
    }

    public void readEntity(Cursor cursor, AssignedAddress assignedAddress, int i) {
        int i2 = i + 0;
        String str = null;
        assignedAddress.setId(cursor.isNull(i2) ? null : Long.valueOf(cursor.getLong(i2)));
        assignedAddress.setNetworkId(cursor.getLong(i + 1));
        int i3 = i + 2;
        assignedAddress.setType(cursor.isNull(i3) ? null : this.typeConverter.convertToEntityProperty(Integer.valueOf(cursor.getInt(i3))));
        int i4 = i + 3;
        assignedAddress.setAddressBytes(cursor.isNull(i4) ? null : cursor.getBlob(i4));
        int i5 = i + 4;
        if (!cursor.isNull(i5)) {
            str = cursor.getString(i5);
        }
        assignedAddress.setAddressString(str);
        assignedAddress.setPrefix(cursor.getShort(i + 5));
    }

    /* access modifiers changed from: protected */
    public final Long updateKeyAfterInsert(AssignedAddress assignedAddress, long j) {
        assignedAddress.setId(Long.valueOf(j));
        return Long.valueOf(j);
    }

    public Long getKey(AssignedAddress assignedAddress) {
        if (assignedAddress != null) {
            return assignedAddress.getId();
        }
        return null;
    }

    public boolean hasKey(AssignedAddress assignedAddress) {
        return assignedAddress.getId() != null;
    }

    public List<AssignedAddress> _queryNetworkConfig_AssignedAddresses(long j) {
        synchronized (this) {
            if (this.networkConfig_AssignedAddressesQuery == null) {
                QueryBuilder queryBuilder = queryBuilder();
                queryBuilder.where(Properties.NetworkId.eq(null));
                this.networkConfig_AssignedAddressesQuery = queryBuilder.build();
            }
        }
        Query<AssignedAddress> forCurrentThread = this.networkConfig_AssignedAddressesQuery.forCurrentThread();
        forCurrentThread.setParameter(0, Long.valueOf(j));
        return forCurrentThread.list();
    }

    public static class Properties {
        public static final Property AddressBytes = new Property(3, byte[].class, "addressBytes", false, "ADDRESS_BYTES");
        public static final Property AddressString = new Property(4, String.class, "addressString", false, "ADDRESS_STRING");
        public static final Property Id = new Property(0, Long.class, "id", true, "_id");
        public static final Property NetworkId = new Property(1, Long.TYPE, "networkId", false, "NETWORK_ID");
        public static final Property Prefix = new Property(5, Short.TYPE, "prefix", false, "PREFIX");
        public static final Property Type = new Property(2, Integer.class, "type", false, "TYPE");
    }
}

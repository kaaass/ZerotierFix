package net.kaaass.zerotierfix.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.internal.DaoConfig;

public class AppNodeDao extends AbstractDao<AppNode, Long> {
    public static final String TABLENAME = "APP_NODE";

    public AppNodeDao(DaoConfig daoConfig) {
        super(daoConfig);
    }

    public AppNodeDao(DaoConfig daoConfig, DaoSession daoSession) {
        super(daoConfig, daoSession);
    }

    public static void createTable(Database database, boolean z) {
        database.execSQL("CREATE TABLE " + (z ? "IF NOT EXISTS " : "") + "\"APP_NODE\" (\"_id\" INTEGER PRIMARY KEY ,\"NODE_ID_STR\" TEXT);");
    }

    public static void dropTable(Database database, boolean z) {
        database.execSQL("DROP TABLE " + (z ? "IF EXISTS " : "") + "\"APP_NODE\"");
    }

    /* access modifiers changed from: protected */
    @Override // org.greenrobot.greendao.AbstractDao
    public final boolean isEntityUpdateable() {
        return true;
    }

    /* access modifiers changed from: protected */
    public final void bindValues(DatabaseStatement databaseStatement, AppNode appNode) {
        databaseStatement.clearBindings();
        Long nodeId = appNode.getNodeId();
        if (nodeId != null) {
            databaseStatement.bindLong(1, nodeId.longValue());
        }
        String nodeIdStr = appNode.getNodeIdStr();
        if (nodeIdStr != null) {
            databaseStatement.bindString(2, nodeIdStr);
        }
    }

    /* access modifiers changed from: protected */
    public final void bindValues(SQLiteStatement sQLiteStatement, AppNode appNode) {
        sQLiteStatement.clearBindings();
        Long nodeId = appNode.getNodeId();
        if (nodeId != null) {
            sQLiteStatement.bindLong(1, nodeId);
        }
        String nodeIdStr = appNode.getNodeIdStr();
        if (nodeIdStr != null) {
            sQLiteStatement.bindString(2, nodeIdStr);
        }
    }

    @Override // org.greenrobot.greendao.AbstractDao
    public Long readKey(Cursor cursor, int i) {
        if (cursor.isNull(i)) {
            return null;
        }
        return cursor.getLong(i);
    }

    @Override // org.greenrobot.greendao.AbstractDao
    public AppNode readEntity(Cursor cursor, int i) {
        String str = null;
        Long valueOf = cursor.isNull(i) ? null : cursor.getLong(i);
        int i3 = i + 1;
        if (!cursor.isNull(i3)) {
            str = cursor.getString(i3);
        }
        return new AppNode(valueOf, str);
    }

    public void readEntity(Cursor cursor, AppNode appNode, int i) {
        String str = null;
        appNode.setNodeId(cursor.isNull(i) ? null : cursor.getLong(i));
        int i3 = i + 1;
        if (!cursor.isNull(i3)) {
            str = cursor.getString(i3);
        }
        appNode.setNodeIdStr(str);
    }

    /* access modifiers changed from: protected */
    public final Long updateKeyAfterInsert(AppNode appNode, long j) {
        appNode.setNodeId(j);
        return j;
    }

    public Long getKey(AppNode appNode) {
        if (appNode != null) {
            return appNode.getNodeId();
        }
        return null;
    }

    public boolean hasKey(AppNode appNode) {
        return appNode.getNodeId() != null;
    }

    public static class Properties {
        public static final Property NodeId = new Property(0, Long.class, "nodeId", true, "_id");
        public static final Property NodeIdStr = new Property(1, String.class, "nodeIdStr", false, "NODE_ID_STR");
    }
}

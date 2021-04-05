package net.kaaass.zerotierfix.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.greenrobot.greendao.database.Database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ZTOpenHelper extends DaoMaster.OpenHelper {
    static final String TAG = "ZTOpenHelper";

    public ZTOpenHelper(Context context, String str) {
        super(context, str);
    }

    public ZTOpenHelper(Context context, String str, SQLiteDatabase.CursorFactory cursorFactory) {
        super(context, str, cursorFactory);
    }

    @Override // org.greenrobot.greendao.database.DatabaseOpenHelper
    public void onUpgrade(Database database, int i, int i2) {
        Log.i(TAG, "Upgrading schema from version " + i + " to " + i2);
        for (Migration migration : getMigrations()) {
            if (i < migration.getVersion()) {
                migration.runMigration(database);
            }
        }
    }

    private List<Migration> getMigrations() {
        ArrayList<Migration> arrayList = new ArrayList<Migration>();
        arrayList.add(new MigrationV18());
        arrayList.add(new MigrationV19());
        arrayList.add(new MigrationV20());
        Collections.sort(arrayList, new Comparator<Migration>() {
            /* class com.zerotier.one.model.ZTOpenHelper.AnonymousClass1 */

            public int compare(Migration migration, Migration migration2) {
                return migration.getVersion().compareTo(migration2.getVersion());
            }
        });
        return arrayList;
    }

    /* access modifiers changed from: private */
    public interface Migration {
        Integer getVersion();

        void runMigration(Database database);
    }

    /* access modifiers changed from: private */
    public static class MigrationV18 implements Migration {
        private MigrationV18() {
        }

        @Override // com.zerotier.one.model.ZTOpenHelper.Migration
        public Integer getVersion() {
            return 18;
        }

        @Override // com.zerotier.one.model.ZTOpenHelper.Migration
        public void runMigration(Database database) {
            database.execSQL("ALTER TABLE NETWORK ADD COLUMN " + NetworkDao.Properties.LastActivated.columnName + " BOOLEAN NOT NULL DEFAULT FALSE ");
        }
    }

    /* access modifiers changed from: private */
    public static class MigrationV19 implements Migration {
        private MigrationV19() {
        }

        @Override // com.zerotier.one.model.ZTOpenHelper.Migration
        public Integer getVersion() {
            return 19;
        }

        @Override // com.zerotier.one.model.ZTOpenHelper.Migration
        public void runMigration(Database database) {
            database.execSQL("ALTER TABLE NETWORK_CONFIG ADD COLUMN " + NetworkConfigDao.Properties.DnsMode.columnName + " INTEGER NOT NULL DEFAULT 0 ");
        }
    }

    /* access modifiers changed from: private */
    public static class MigrationV20 implements Migration {
        private MigrationV20() {
        }

        @Override // com.zerotier.one.model.ZTOpenHelper.Migration
        public Integer getVersion() {
            return 20;
        }

        @Override // com.zerotier.one.model.ZTOpenHelper.Migration
        public void runMigration(Database database) {
            database.execSQL("UPDATE NETWORK_CONFIG SET " + NetworkConfigDao.Properties.DnsMode.columnName + " = 2 WHERE " + NetworkConfigDao.Properties.UseCustomDNS.columnName + " = 1 ");
        }
    }
}

package net.kaaass.zerotierfix.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.greenrobot.greendao.database.Database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZTOpenHelper extends DaoMaster.OpenHelper {
    static final String TAG = "ZTOpenHelper";

    public ZTOpenHelper(Context context, String str) {
        super(context, str);
    }

    public ZTOpenHelper(Context context, String str, SQLiteDatabase.CursorFactory cursorFactory) {
        super(context, str, cursorFactory);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading schema from version " + oldVersion + " to " + newVersion);
        for (Migration migration : getMigrations()) {
            if (oldVersion < migration.getVersion()) {
                migration.runMigration(db);
            }
        }
    }

    private List<Migration> getMigrations() {
        ArrayList<Migration> arrayList = new ArrayList<Migration>();
        arrayList.add(new MigrationV18());
        arrayList.add(new MigrationV19());
        arrayList.add(new MigrationV20());
        Collections.sort(arrayList, (migration, migration2) -> migration.getVersion().compareTo(migration2.getVersion()));
        return arrayList;
    }

    private interface Migration {
        Integer getVersion();

        void runMigration(Database database);
    }

    private static class MigrationV18 implements Migration {
        private MigrationV18() {
        }

        @Override
        public Integer getVersion() {
            return 18;
        }

        @Override
        public void runMigration(Database database) {
            database.execSQL("ALTER TABLE NETWORK ADD COLUMN " + NetworkDao.Properties.LastActivated.columnName + " BOOLEAN NOT NULL DEFAULT FALSE ");
        }
    }

    private static class MigrationV19 implements Migration {
        private MigrationV19() {
        }

        @Override
        public Integer getVersion() {
            return 19;
        }

        @Override
        public void runMigration(Database database) {
            database.execSQL("ALTER TABLE NETWORK_CONFIG ADD COLUMN " + NetworkConfigDao.Properties.DnsMode.columnName + " INTEGER NOT NULL DEFAULT 0 ");
        }
    }

    private static class MigrationV20 implements Migration {
        private MigrationV20() {
        }

        @Override
        public Integer getVersion() {
            return 20;
        }

        @Override
        public void runMigration(Database database) {
            database.execSQL("UPDATE NETWORK_CONFIG SET " + NetworkConfigDao.Properties.DnsMode.columnName + " = 2 WHERE " + NetworkConfigDao.Properties.UseCustomDNS.columnName + " = 1 ");
        }
    }
}

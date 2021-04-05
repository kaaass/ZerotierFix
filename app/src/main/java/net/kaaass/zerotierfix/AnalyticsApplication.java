package net.kaaass.zerotierfix;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import net.kaaass.zerotierfix.model.DaoMaster;
import net.kaaass.zerotierfix.model.DaoSession;
import net.kaaass.zerotierfix.model.Network;
import net.kaaass.zerotierfix.model.NetworkConfig;
import net.kaaass.zerotierfix.model.NetworkConfigDao;
import net.kaaass.zerotierfix.model.NetworkDao;
import net.kaaass.zerotierfix.model.ZTOpenHelper;
import net.kaaass.zerotierfix.util.NetworkIdUtils;

import org.greenrobot.greendao.query.WhereCondition;
import org.json.JSONArray;

import java.util.ArrayList;

public class AnalyticsApplication extends Application {
    private DaoSession mDaoSession;

    public void onCreate() {
        super.onCreate();
        Log.i("Application", "Starting Application");
        this.mDaoSession = new DaoMaster(new ZTOpenHelper(this, "ztdb", null).getWritableDatabase()).newSession();
        transferKnownNetworks();
    }

    private void transferKnownNetworks() {
        SharedPreferences sharedPreferences = getSharedPreferences("recent_networks", 0);
        if (!sharedPreferences.getBoolean("transfer_to_sqlitedb", false)) {
            try {
                JSONArray jSONArray = new JSONArray(sharedPreferences.getString("recent_networks", new JSONArray().toString()));
                if (jSONArray.length() > 0) {
                    NetworkDao networkDao = this.mDaoSession.getNetworkDao();
                    NetworkConfigDao networkConfigDao = this.mDaoSession.getNetworkConfigDao();
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (int i = 0; i < jSONArray.length(); i++) {
                        arrayList.add(jSONArray.getString(i));
                    }
                    for (String str : arrayList) {
                        long hexStringToLong = NetworkIdUtils.hexStringToLong(str);
                        if (networkDao.queryBuilder().where(NetworkDao.Properties.NetworkId.eq(hexStringToLong), new WhereCondition[0]).list().isEmpty()) {
                            Network network = new Network();
                            network.setNetworkId(hexStringToLong);
                            network.setNetworkIdStr(str);
                            NetworkConfig networkConfig = new NetworkConfig();
                            networkConfig.setId(hexStringToLong);
                            network.setNetworkConfigId(hexStringToLong);
                            networkConfigDao.insert(networkConfig);
                            networkDao.insert(network);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            this.mDaoSession.clear();
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putBoolean("transfer_to_sqlitedb", true);
            edit.apply();
        }
    }

    public DaoSession getDaoSession() {
        return this.mDaoSession;
    }
}

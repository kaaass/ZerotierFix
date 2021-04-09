package net.kaaass.zerotierfix.service;

import android.content.Context;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.zerotier.sdk.DataStoreGetListener;
import com.zerotier.sdk.DataStorePutListener;

import net.kaaass.zerotierfix.util.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Zerotier 文件数据源
 */
public class DataStore implements DataStoreGetListener, DataStorePutListener {

    private static final String TAG = "DataStore";

    private final Context context;

    public DataStore(Context context) {
        this.context = context;
    }

    @Override
    public int onDataStorePut(String name, byte[] buffer, boolean secure) {
        Log.d(TAG, "Writing File: " + name + ", to: " + this.context.getFilesDir());
        // 保护自定义 Planet 文件
        if (hookPlanetFile(name)) {
            return 0;
        }
        try {
            if (name.contains("/")) {
                File file = new File(this.context.getFilesDir(), name.substring(0, name.lastIndexOf('/')));
                if (!file.exists()) {
                    file.mkdirs();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(new File(file, name.substring(name.lastIndexOf('/') + 1)));
                fileOutputStream.write(buffer);
                fileOutputStream.flush();
                fileOutputStream.close();
                return 0;
            }
            FileOutputStream openFileOutput = this.context.openFileOutput(name, 0);
            openFileOutput.write(buffer);
            openFileOutput.flush();
            openFileOutput.close();
            return 0;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        } catch (IOException e2) {
            StringWriter stringWriter = new StringWriter();
            e2.printStackTrace(new PrintWriter(stringWriter));
            Log.e(TAG, stringWriter.toString());
            return -2;
        } catch (IllegalArgumentException e3) {
            StringWriter stringWriter2 = new StringWriter();
            e3.printStackTrace(new PrintWriter(stringWriter2));
            Log.e(TAG, stringWriter2.toString());
            return -3;
        }
    }

    @Override
    public int onDelete(String name) {
        boolean deleted;
        Log.d(TAG, "Deleting File: " + name);
        // 保护自定义 Planet 文件
        if (hookPlanetFile(name)) {
            return 0;
        }
        if (name.contains("/")) {
            File file = new File(this.context.getFilesDir(), name);
            if (!file.exists()) {
                deleted = true;
            } else {
                deleted = file.delete();
            }
        } else {
            deleted = this.context.deleteFile(name);
        }
        return !deleted ? 1 : 0;
    }

    @Override
    public long onDataStoreGet(String name, byte[] out_buffer) {
        Log.d(TAG, "Reading File: " + name);
        if (hookPlanetFile(name)) {
            name = Constants.FILE_CUSTOM_PLANET;
        }
        // 读入文件
        try {
            if (name.contains("/")) {
                File file = new File(this.context.getFilesDir(), name.substring(0, name.lastIndexOf('/')));
                if (!file.exists()) {
                    file.mkdirs();
                }
                File file2 = new File(file, name.substring(name.lastIndexOf('/') + 1));
                if (!file2.exists()) {
                    return 0;
                }
                FileInputStream fileInputStream = new FileInputStream(file2);
                int read = fileInputStream.read(out_buffer);
                fileInputStream.close();
                return read;
            }
            FileInputStream openFileInput = this.context.openFileInput(name);
            int read2 = openFileInput.read(out_buffer);
            openFileInput.close();
            return read2;
        } catch (FileNotFoundException unused) {
            return -1;
        } catch (IOException e) {
            Log.e(TAG, "", e);
            return -2;
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return -3;
        }
    }

    /**
     * 判断自定义 Planet 文件
     */
    boolean hookPlanetFile(String name) {
        if (Constants.FILE_PLANET.equals(name)) {
            return PreferenceManager
                    .getDefaultSharedPreferences(this.context)
                    .getBoolean(Constants.PREF_PLANET_USE_CUSTOM, false);
        }
        return false;
    }
}

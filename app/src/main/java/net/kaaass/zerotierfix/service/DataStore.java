package net.kaaass.zerotierfix.service;

import android.content.Context;
import android.util.Log;

import com.zerotier.sdk.DataStoreGetListener;
import com.zerotier.sdk.DataStorePutListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DataStore implements DataStoreGetListener, DataStorePutListener {
    private static final String TAG = "DataStore";
    private final Context _ctx;

    public DataStore(Context context) {
        this._ctx = context;
    }

    @Override // com.zerotier.sdk.DataStorePutListener
    public int onDataStorePut(String str, byte[] bArr, boolean z) {
        Log.d(TAG, "Writing File: " + str + ", to: " + this._ctx.getFilesDir());
        try {
            if (str.contains("/")) {
                File file = new File(this._ctx.getFilesDir(), str.substring(0, str.lastIndexOf(47)));
                if (!file.exists()) {
                    file.mkdirs();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(new File(file, str.substring(str.lastIndexOf("/") + 1)));
                fileOutputStream.write(bArr);
                fileOutputStream.flush();
                fileOutputStream.close();
                return 0;
            }
            FileOutputStream openFileOutput = this._ctx.openFileOutput(str, 0);
            openFileOutput.write(bArr);
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

    @Override // com.zerotier.sdk.DataStorePutListener
    public int onDelete(String str) {
        boolean z;
        Log.d(TAG, "Deleting File: " + str);
        if (str.contains("/")) {
            File file = new File(this._ctx.getFilesDir(), str);
            if (!file.exists()) {
                z = true;
            } else {
                z = file.delete();
            }
        } else {
            z = this._ctx.deleteFile(str);
        }
        return !z ? 1 : 0;
    }

    @Override // com.zerotier.sdk.DataStoreGetListener
    public long onDataStoreGet(String str, byte[] bArr) {
        Log.d(TAG, "Reading File: " + str);
        try {
            if (str.contains("/")) {
                File file = new File(this._ctx.getFilesDir(), str.substring(0, str.lastIndexOf(47)));
                if (!file.exists()) {
                    file.mkdirs();
                }
                File file2 = new File(file, str.substring(str.lastIndexOf(47) + 1));
                if (!file2.exists()) {
                    return 0;
                }
                FileInputStream fileInputStream = new FileInputStream(file2);
                int read = fileInputStream.read(bArr);
                fileInputStream.close();
                return read;
            }
            FileInputStream openFileInput = this._ctx.openFileInput(str);
            int read2 = openFileInput.read(bArr);
            openFileInput.close();
            return read2;
        } catch (FileNotFoundException unused) {
            return -1;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return -2;
        } catch (Exception e2) {
            Log.e(TAG, e2.toString());
            e2.printStackTrace();
            return -3;
        }
    }
}

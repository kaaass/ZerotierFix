package net.kaaass.zerotierfix.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.service.ZeroTierOneService;
import net.kaaass.zerotierfix.util.Constants;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * 设置页面 fragment
 */
public class PrefsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_PLANET_FILE = 42;

    private static final String TAG = "PreferencesFragment";

    private SwitchPreference prefPlanetUseCustom;

    private Preference prefSetPlanetFile;

    private Dialog planetDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        this.prefPlanetUseCustom = findPreference(Constants.PREF_PLANET_USE_CUSTOM);
        this.prefSetPlanetFile = findPreference(Constants.PREF_SET_PLANET_FILE);

        // Plane 配置开关
        Objects.requireNonNull(this.prefPlanetUseCustom).setOnPreferenceClickListener(preference -> {
            // 判断状态
            updatePlanetSetting();
            if (!preference.getSharedPreferences().getBoolean(Constants.PREF_PLANET_USE_CUSTOM, false)) {
                // 设置为假时直接退出
                return true;
            }
            // 设置为真时，如果还没有设置文件则提示设置文件
            if (customPlanetFileNotExit()) {
                showPlanetFileDialog();
            }
            return true;
        });

        // 打开 Plant 文件设置
        Objects.requireNonNull(this.prefSetPlanetFile).setOnPreferenceClickListener(preference -> {
            showPlanetFileDialog();
            return true;
        });
        updatePlanetSetting();
    }

    /**
     * 显示选择 Planet 文件来源对话框
     */
    private void showPlanetFileDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_custom_planet_select, null);

        // 选择文件
        View viewFile = view.findViewById(R.id.from_file);
        viewFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_PLANET_FILE);
        });

        // 选择 URL
        View viewUrl = view.findViewById(R.id.from_url);
        viewUrl.setOnClickListener(v -> this.showPlanetUrlDialog());

        // 窗口
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(R.string.load_planet_file)
                .setOnCancelListener(dialog -> {
                    closePlanetDialog();
                });

        this.planetDialog = builder.create();
        this.planetDialog.show();
    }

    /**
     * 显示输入 Planet 文件 URL 对话框
     */
    private void showPlanetUrlDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_planet_url, null);

        final EditText editText = view.findViewById(R.id.planet_url);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(R.string.import_via_url)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String url = editText.getText().toString();

                    // TODO 下载 Planet 文件
                    Toast.makeText(getContext(), url, Toast.LENGTH_SHORT).show();

                    // 关闭对话框
                    closePlanetDialog();
                });

        builder.create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_NETWORK_USE_CELLULAR_DATA)) {
            // 移动网络数据配置
            if (sharedPreferences.getBoolean(Constants.PREF_NETWORK_USE_CELLULAR_DATA, false)) {
                requireActivity().startService(new Intent(getActivity(), ZeroTierOneService.class));
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PLANET_FILE && resultCode == -1 && data != null) {
            // Planet 自定义文件设置
            Uri uriData = data.getData();
            if (uriData == null) {
                Log.e(TAG, "Invalid planet URI");
                return;
            }
            // 复制文件
            Context context = requireContext();
            try (InputStream in = context.getContentResolver().openInputStream(uriData)) {
                File destFile = new File(requireActivity().getFilesDir(), Constants.FILE_CUSTOM_PLANET);
                FileUtils.copyInputStreamToFile(in, destFile);
            } catch (IOException e) {
                Log.e(TAG, "Cannot copy planet file", e);
                // 设置失败
                Toast.makeText(getContext(), R.string.cannot_copy_planet, Toast.LENGTH_LONG).show();
                this.prefPlanetUseCustom.setChecked(false);
                this.closePlanetDialog();
                return;
            }
            Log.i(TAG, "Copy planet file successfully");
            // 关闭对话框
            if (this.planetDialog != null) {
                this.planetDialog.dismiss();
                this.planetDialog = null;
            }
        }
    }

    /**
     * 检查是否存在 Planet 文件
     */
    private boolean customPlanetFileNotExit() {
        File file = new File(requireActivity().getFilesDir(), Constants.FILE_CUSTOM_PLANET);
        return !file.exists();
    }

    /**
     * 关闭 Planet 对话框
     */
    private void closePlanetDialog() {
        if (this.planetDialog != null) {
            // 如果没有设置文件，就取消选项
            if (customPlanetFileNotExit()) {
                this.prefPlanetUseCustom.setChecked(false);
            }
            // 关闭对话框
            this.planetDialog.dismiss();
            this.planetDialog = null;
        }
    }

    /**
     * 更新 Planet 设置选项
     */
    private void updatePlanetSetting() {
        boolean useCustom = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(Constants.PREF_PLANET_USE_CUSTOM, false);
        this.prefSetPlanetFile.setEnabled(useCustom);
    }
}

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.service.ZeroTierOneService;
import net.kaaass.zerotierfix.util.Constants;
import net.kaaass.zerotierfix.util.FileUtil;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

/**
 * 设置页面 fragment
 */
public class PrefsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int PLANET_DOWNLOAD_CONN_TIMEOUT = 5 * 1000;
    public static final int PLANET_DOWNLOAD_TIMEOUT = 10 * 1000;
    private static final String TAG = "PreferencesFragment";
    /**
     * Plant 文件固定头
     */
    private static final byte[] PLANET_FILE_HEADER = new byte[]{
            0x01,  // World type, 0x01 = planet
    };
    private SwitchPreference prefPlanetUseCustom;
    private Preference prefSetPlanetFile;
    private Dialog planetDialog = null;
    private Dialog loadingDialog = null;
    private ActivityResultLauncher<Intent> planetFileSelectLauncher = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 初始化 Planet 文件选择结果回调
        this.planetFileSelectLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (activityResult) -> {
            var result = activityResult.getResultCode();
            var data = activityResult.getData();
            if (result == -1 && data != null) {
                // Planet 自定义文件设置
                Uri uriData = data.getData();
                if (uriData == null) {
                    Log.e(TAG, "Invalid planet URI");
                    return;
                }
                // 复制文件
                try (InputStream in = requireContext().getContentResolver().openInputStream(uriData)) {
                    FileUtils.copyInputStreamToFile(in, FileUtil.tempFile(requireContext()));
                    boolean success = dealTempPlanetFile();
                    if (!success) {
                        // 校验失败
                        Toast.makeText(getContext(), R.string.planet_wrong_format, Toast.LENGTH_LONG).show();
                        closePlanetDialog();
                        return;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cannot copy planet file", e);
                    // 设置失败
                    Toast.makeText(getContext(), R.string.cannot_open_planet, Toast.LENGTH_LONG).show();
                    closePlanetDialog();
                    return;
                } finally {
                    FileUtil.clearTempFile(requireContext());
                }
                Log.i(TAG, "Copy planet file successfully");
                // 关闭对话框
                Snackbar.make(requireView(), R.string.set_planet_succ, BaseTransientBottomBar.LENGTH_LONG).show();
                closePlanetDialog();
            } else {
                Toast.makeText(getContext(), R.string.cannot_open_planet, Toast.LENGTH_LONG).show();
            }
        });
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
            var pref = preference.getSharedPreferences();
            if (pref == null || !pref.getBoolean(Constants.PREF_PLANET_USE_CUSTOM, false)) {
                // 设置为假时直接退出
                return true;
            }
            // 设置为真时，如果还没有设置文件则提示设置文件
            if (customPlanetFileNotExit()) {
                showPlanetDialog();
            }
            return true;
        });

        // 打开 Plant 文件设置
        Objects.requireNonNull(this.prefSetPlanetFile).setOnPreferenceClickListener(preference -> {
            showPlanetDialog();
            return true;
        });
        updatePlanetSetting();
    }

    /**
     * 显示选择 Planet 文件来源对话框
     */
    private void showPlanetDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_custom_planet_select, null);

        // 选择文件
        View viewFile = view.findViewById(R.id.from_file);
        viewFile.setOnClickListener(v -> this.showPlanetFromFileDialog());

        // 选择 URL
        View viewUrl = view.findViewById(R.id.from_url);
        viewUrl.setOnClickListener(v -> this.showPlanetFromUrlDialog());

        // 窗口
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(R.string.load_planet_file)
                .setOnCancelListener(dialog -> closePlanetDialog());

        this.planetDialog = builder.create();
        this.planetDialog.show();
    }

    /**
     * 显示选择 Planet 文件的文件选择器
     */
    private void showPlanetFromFileDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        this.planetFileSelectLauncher.launch(intent);
    }

    /**
     * 显示输入 Planet 文件 URL 对话框
     */
    private void showPlanetFromUrlDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_planet_url, null);

        final EditText editText = view.findViewById(R.id.planet_url);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(R.string.import_via_url)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String url = editText.getText().toString();
                    URL fileUrl;
                    try {
                        fileUrl = new URL(url);
                    } catch (MalformedURLException e) {
                        Toast.makeText(getContext(), R.string.wrong_url_format, Toast.LENGTH_LONG).show();
                        // 不必关闭对话框
                        return;
                    }

                    // 关闭对话框
                    if (this.planetDialog != null) {
                        this.planetDialog.dismiss();
                        this.planetDialog = null;
                    }
                    // 显示加载动画
                    showLoadingDialog(R.string.downloading);
                    // 下载 Planet 文件
                    new Thread(() -> {
                        try {
                            FileUtils.copyURLToFile(fileUrl,
                                    FileUtil.tempFile(requireContext()),
                                    PLANET_DOWNLOAD_CONN_TIMEOUT, PLANET_DOWNLOAD_TIMEOUT);
                            boolean success = dealTempPlanetFile();
                            if (!success) {
                                // 校验失败
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), R.string.planet_wrong_format, Toast.LENGTH_LONG).show();
                                    closePlanetDialog();
                                    closeLoadingDialog();
                                });
                                return;
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Cannot download planet file", e);
                            // 设置失败
                            requireActivity().runOnUiThread(() -> {
                                int message = R.string.cannot_download_planet_file;
                                if (e instanceof SocketTimeoutException) {
                                    message = R.string.planet_download_timeout;
                                }
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                closePlanetDialog();
                                closeLoadingDialog();
                            });
                            return;
                        } finally {
                            FileUtil.clearTempFile(requireContext());
                        }

                        // 关闭对话框
                        requireActivity().runOnUiThread(() -> {
                            Snackbar.make(requireView(), R.string.set_planet_succ, BaseTransientBottomBar.LENGTH_LONG).show();
                            closePlanetDialog();
                            closeLoadingDialog();
                        });
                    }).start();
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
        // 关闭对话框
        if (this.planetDialog != null) {
            this.planetDialog.dismiss();
            this.planetDialog = null;
        }
        // 如果没有设置文件，就取消选项
        if (customPlanetFileNotExit()) {
            this.prefPlanetUseCustom.setChecked(false);
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

    /**
     * 将临时文件设置为 Planet 文件
     */
    private boolean dealTempPlanetFile() {
        // Plant 文件校验
        File temp = FileUtil.tempFile(requireContext());
        byte[] buf = new byte[PLANET_FILE_HEADER.length];
        try (FileInputStream in = new FileInputStream(temp)) {
            // 读入文件头
            if (in.read(buf) != PLANET_FILE_HEADER.length) {
                return false;
            }
            // 校验
            if (!Arrays.equals(buf, PLANET_FILE_HEADER)) {
                Log.i(TAG, "Planet file has a wrong header");
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        // 移动临时文件
        File dest = new File(requireActivity().getFilesDir(), Constants.FILE_CUSTOM_PLANET);
        return temp.renameTo(dest);
    }

    /**
     * 显示加载框
     */
    private void showLoadingDialog(int prompt) {
        closeLoadingDialog();
        // 创建
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_loading, null);

        TextView textPrompt = view.findViewById(R.id.prompt);
        textPrompt.setText(prompt);

        this.loadingDialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .setCancelable(false)
                .create();
        this.loadingDialog.show();
    }

    /**
     * 关闭加载框
     */
    private void closeLoadingDialog() {
        if (this.loadingDialog != null) {
            this.loadingDialog.dismiss();
        }
    }
}

package net.kaaass.zerotierfix.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.events.AddMoonOrbitEvent;
import net.kaaass.zerotierfix.events.OrbitMoonEvent;
import net.kaaass.zerotierfix.events.RemoveMoonOrbitEvent;
import net.kaaass.zerotierfix.model.DaoSession;
import net.kaaass.zerotierfix.model.MoonOrbit;
import net.kaaass.zerotierfix.model.MoonOrbitDao;
import net.kaaass.zerotierfix.util.FileUtil;

import org.apache.commons.io.FileUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import lombok.ToString;

/**
 * 入轨配置片段
 *
 * @author kaaass
 */
public class MoonOrbitFragment extends Fragment {

    public static final String TAG = "MoonOrbitFragment";
    private static final byte[] MOON_FILE_HEADER = new byte[]{0x7f};
    private final List<MoonOrbit> moonOrbitList = new ArrayList<>();
    private final EventBus eventBus;
    private RecyclerViewAdapter recyclerViewAdapter = null;
    private RecyclerView recyclerView = null;
    private View emptyView = null;
    final private RecyclerView.AdapterDataObserver checkIfEmptyObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        /**
         * 检查列表是否为空
         */
        void checkIfEmpty() {
            if (emptyView != null && recyclerViewAdapter != null) {
                final boolean emptyViewVisible = recyclerViewAdapter.getItemCount() == 0;
                emptyView.setVisibility(emptyViewVisible ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(emptyViewVisible ? View.GONE : View.VISIBLE);
            }
        }
    };
    private AlertDialog providerDialog = null;
    private ActivityResultLauncher<Intent> moonFileSelectLauncher = null;

    public MoonOrbitFragment() {
        this.eventBus = EventBus.getDefault();
        this.eventBus.register(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moon_orbit_list, container, false);

        // 空列表提示
        this.emptyView = view.findViewById(R.id.no_data);

        // 设置适配器
        this.recyclerView = view.findViewById(R.id.list_moon_orbit);
        Context context = recyclerView.getContext();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        this.recyclerViewAdapter = new RecyclerViewAdapter(this.moonOrbitList);
        this.recyclerViewAdapter.registerAdapterDataObserver(checkIfEmptyObserver);
        recyclerView.setAdapter(this.recyclerViewAdapter);

        // 设置添加按钮
        FloatingActionButton fab = view.findViewById(R.id.fab_moon_orbit);
        fab.setOnClickListener(parentView -> showMoonProviderDialog());

        // 更新入轨数据
        updateOrbitList();

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 初始化 Moon 文件选择结果回调
        this.moonFileSelectLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (activityResult) -> {
            var result = activityResult.getResultCode();
            var data = activityResult.getData();
            if (result == -1 && data != null) {
                long moonWorldId;
                // Moon 文件设置
                Uri uriData = data.getData();
                if (uriData == null) {
                    Log.e(TAG, "Invalid moon URI");
                    return;
                }
                // 复制文件到临时文件
                try (InputStream in = requireContext().getContentResolver().openInputStream(uriData)) {
                    FileUtils.copyInputStreamToFile(in, FileUtil.tempFile(requireContext()));
                    // 校验
                    moonWorldId = checkTempMoonFile();
                    if (moonWorldId < 0) {
                        Toast.makeText(getContext(), R.string.moon_wrong_file_format, Toast.LENGTH_LONG).show();
                        FileUtil.clearTempFile(requireContext());
                        return;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cannot copy planet file", e);
                    // 设置失败
                    Toast.makeText(getContext(), R.string.cannot_open_moon, Toast.LENGTH_LONG).show();
                    FileUtil.clearTempFile(requireContext());
                    return;
                }
                Log.i(TAG, "Copy planet file successfully");
                // 加入 Moon
                onAddMoonOrbitEvent(new AddMoonOrbitEvent(moonWorldId, moonWorldId, true));
            } else {
                Toast.makeText(getContext(), R.string.cannot_open_moon, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 更新数据列表
     */
    private void updateOrbitList() {
        this.moonOrbitList.clear();
        this.moonOrbitList.addAll(getMoonOrbitList());
        this.recyclerViewAdapter.notifyDataSetChanged();
    }

    /**
     * 获得 Moon 入轨配置列表
     */
    private List<MoonOrbit> getMoonOrbitList() {
        DaoSession daoSession = ((ZerotierFixApplication) requireActivity().getApplication()).getDaoSession();
        return daoSession.getMoonOrbitDao().loadAll();
    }

    /**
     * 显示选择 Moon 来源对话框
     */
    private void showMoonProviderDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_moon_provider, null);

        // 选择文件
        View viewFile = view.findViewById(R.id.from_file);
        viewFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            this.moonFileSelectLauncher.launch(intent);
        });

        // 选择入轨
        View viewOrbit = view.findViewById(R.id.from_orbit);
        viewOrbit.setOnClickListener(v -> this.showMoonOrbitDialog());

        // 窗口
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(R.string.import_moon_node);

        this.providerDialog = builder.create();
        this.providerDialog.show();
    }

    /**
     * 显示 Moon 入轨对话框
     */
    private void showMoonOrbitDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_moon_orbit, null);
        final EditText textMoonWorldId = view.findViewById(R.id.text_moon_world_id);
        final EditText textMoonSeed = view.findViewById(R.id.text_moon_seed);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(view)
                .setTitle(R.string.moon_orbit_info)
                .setPositiveButton(getString(R.string.add), (dialog, which) -> {
                    long moonWorldId;
                    long moonSeed;
                    // 解析 Moon 地址
                    try {
                        moonWorldId = Long.parseLong(textMoonWorldId.getText().toString(), 16);
                        if (!(0 <= moonWorldId && moonWorldId <= 0xffffffffffL))
                            throw new NumberFormatException();
                    } catch (NumberFormatException ignored) {
                        Snackbar.make(requireView(), R.string.moon_world_id_wrong_format, BaseTransientBottomBar.LENGTH_SHORT).show();
                        return;
                    }
                    // 解析 Seed
                    try {
                        moonSeed = Long.parseLong(textMoonSeed.getText().toString(), 16);
                        if (!(0 <= moonSeed && moonSeed <= 0xffffffffffL))
                            throw new NumberFormatException();
                    } catch (NumberFormatException ignored) {
                        Snackbar.make(requireView(), R.string.moon_seed_wrong_format, BaseTransientBottomBar.LENGTH_SHORT).show();
                        return;
                    }
                    // 触发事件
                    this.eventBus.post(new AddMoonOrbitEvent(moonWorldId, moonSeed, false));
                })
                .setOnDismissListener(dialog -> {
                    // 关闭外层对话框
                    if (this.providerDialog != null) {
                        this.providerDialog.dismiss();
                        this.providerDialog = null;
                    }
                });

        builder.create().show();
    }

    /**
     * 校验临时 Moon 文件
     *
     * @return Moon 地址
     */
    private long checkTempMoonFile() {
        File temp = FileUtil.tempFile(requireContext());
        int dataLen = MOON_FILE_HEADER.length + 8;
        byte[] buf = new byte[dataLen];
        try (FileInputStream in = new FileInputStream(temp)) {
            // 读入文件头
            if (in.read(buf) != dataLen) {
                return -1;
            }
            // 校验
            if (buf[0] != MOON_FILE_HEADER[0]) {
                Log.i(TAG, "Moon file has a wrong header");
                return -1;
            }
            // 取得 Moon 地址
            long moonWorldId = 0;
            for (int i = MOON_FILE_HEADER.length; i < dataLen; i++) {
                moonWorldId = (moonWorldId << 8) + (buf[i] & 0xff);
            }
            return moonWorldId;
        } catch (IOException ignored) {
        }
        return -1;
    }

    /**
     * 增加 Moon 入轨信息事件回调
     *
     * @param event 事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddMoonOrbitEvent(AddMoonOrbitEvent event) {
        long moonWorldId = event.getMoonWorldId();
        long moonSeed = event.getMoonSeed();
        boolean fromFile = event.isFromFile();
        Log.i(TAG, "add orbit info " + Long.toHexString(moonWorldId));
        // 数据库修改
        DaoSession daoSession = ((ZerotierFixApplication) requireActivity().getApplication()).getDaoSession();
        long existCount = daoSession.getMoonOrbitDao().queryBuilder()
                .where(MoonOrbitDao.Properties.MoonWorldId.eq(moonWorldId))
                .buildCount()
                .count();
        if (existCount > 0) {
            Toast.makeText(getContext(), R.string.moon_orbit_exist, Toast.LENGTH_SHORT).show();
            FileUtil.clearTempFile(requireContext());
            return;
        }
        // 移动临时文件
        if (fromFile) {
            File dest = new File(requireActivity().getFilesDir(),
                    String.format(MoonOrbit.MOON_FILE_PATH, moonWorldId));
            if (!FileUtil.tempFile(requireContext()).renameTo(dest)) {
                Toast.makeText(getContext(), R.string.cannot_open_moon, Toast.LENGTH_LONG).show();
            }
            FileUtil.clearTempFile(requireContext());
        }
        MoonOrbit moonOrbit = new MoonOrbit(moonWorldId, moonSeed, fromFile);
        daoSession.getMoonOrbitDao().insert(moonOrbit);
        // 关闭窗口并提示
        if (this.providerDialog != null) {
            this.providerDialog.dismiss();
            this.providerDialog = null;
        }
        Snackbar.make(requireView(), R.string.moon_orbit_add_success, BaseTransientBottomBar.LENGTH_SHORT).show();
        // 触发事件入轨
        this.eventBus.post(new OrbitMoonEvent(new ArrayList<>() {{
            add(moonOrbit);
        }}));
        // 更新界面
        updateOrbitList();
    }

    /**
     * 删除 Moon 入轨信息事件回调
     *
     * @param event 事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoveMoonOrbitEvent(RemoveMoonOrbitEvent event) {
        long moonWorldId = event.getMoonWorldId();
        long moonSeed = event.getMoonSeed();
        Log.i(TAG, "remove orbit info " + Long.toHexString(moonWorldId));
        // 查询待删除项目
        DaoSession daoSession = ((ZerotierFixApplication) requireActivity().getApplication()).getDaoSession();
        MoonOrbit moonOrbit = daoSession.getMoonOrbitDao().queryBuilder()
                .where(MoonOrbitDao.Properties.MoonWorldId.eq(moonWorldId),
                        MoonOrbitDao.Properties.MoonSeed.eq(moonSeed))
                .build()
                .unique();
        // 删除缓存文件
        moonOrbit.deleteCacheFile(requireContext());
        // 删除记录
        daoSession.delete(moonOrbit);
        Snackbar.make(requireView(), R.string.moon_orbit_delete_success, BaseTransientBottomBar.LENGTH_SHORT).show();
        // 更新界面
        updateOrbitList();
    }

    /**
     * Moon 入轨信息列表适配器
     */
    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private final List<MoonOrbit> mValues;

        public RecyclerViewAdapter(List<MoonOrbit> items) {
            mValues = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_moon_orbit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            MoonOrbit moonOrbit = mValues.get(position);
            moonOrbit.checkCacheFile(requireContext());
            holder.mItem = moonOrbit;
            // 设置文本
            holder.mMoonWorldId.setText(Long.toHexString(moonOrbit.getMoonWorldId()));
            if (moonOrbit.getFromFile()) {
                holder.mMoonConfig.setText(R.string.external_file);
            } else {
                holder.mMoonSeed.setText(Long.toHexString(moonOrbit.getMoonSeed()));
                if (moonOrbit.isCacheFile()) {
                    holder.mMoonConfig.setText(R.string.cached);
                } else {
                    holder.mMoonConfig.setText(R.string.wait_to_fetch);
                }
            }
            // 长按菜单
            holder.mView.setOnLongClickListener(holder::onLongClick);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        @ToString
        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mMoonWorldId;
            public final TextView mMoonSeed;
            public final TextView mMoonConfig;
            public MoonOrbit mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mMoonWorldId = view.findViewById(R.id.moon_world_id);
                mMoonSeed = view.findViewById(R.id.moon_seed);
                mMoonConfig = view.findViewById(R.id.moon_config);
            }

            /**
             * 为给定 view 创建弹出菜单
             */
            public boolean onLongClick(View view) {
                Log.d(TAG, "Long click " + this);
                PopupMenu popupMenu = new PopupMenu(MoonOrbitFragment.this.getActivity(), view);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu_moon_orbit, popupMenu.getMenu());
                // 非文件添加且已经有缓存文件时，允许删除文件
                if (!(mItem.isCacheFile() && !mItem.getFromFile())) {
                    popupMenu.getMenu().removeItem(R.id.menu_item_delete_moon_orbit_cache);
                }
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.menu_item_delete_moon_orbit) {
                        // 触发事件
                        MoonOrbitFragment.this.eventBus.post(new RemoveMoonOrbitEvent(mItem.getMoonWorldId(), mItem.getMoonSeed()));
                        return true;
                    } else if (menuItem.getItemId() == R.id.menu_item_copy_moon_world_id) {
                        // 复制 Moon 地址
                        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(getString(R.string.network_id), Long.toHexString(this.mItem.getMoonWorldId()));
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getContext(), R.string.text_copied, Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (menuItem.getItemId() == R.id.menu_item_delete_moon_orbit_cache) {
                        // 删除缓存文件
                        this.mItem.deleteCacheFile(requireContext());
                        this.mMoonConfig.setText(R.string.wait_to_fetch);
                        Snackbar.make(requireView(), R.string.cached_moon_file_delete, BaseTransientBottomBar.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                });
                return true;
            }
        }
    }
}
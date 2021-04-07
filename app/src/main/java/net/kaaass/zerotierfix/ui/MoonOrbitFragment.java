package net.kaaass.zerotierfix.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.kaaass.zerotierfix.AnalyticsApplication;
import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.events.AddMoonOrbitEvent;
import net.kaaass.zerotierfix.events.OrbitMoonEvent;
import net.kaaass.zerotierfix.events.RemoveMoonOrbitEvent;
import net.kaaass.zerotierfix.model.DaoSession;
import net.kaaass.zerotierfix.model.MoonOrbit;
import net.kaaass.zerotierfix.model.MoonOrbitDao;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * 入轨配置片段
 */
public class MoonOrbitFragment extends Fragment {

    public static final String TAG = "MoonOrbitFragment";

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
        fab.setOnClickListener(parentView -> showMoonOrbitDialog());

        // 更新入轨数据
        updateOrbitList();

        return view;
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
        DaoSession daoSession = ((AnalyticsApplication) getActivity().getApplication()).getDaoSession();
        return daoSession.getMoonOrbitDao().loadAll();
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
                    // 解析数字
                    try {
                        moonWorldId = Long.parseLong(textMoonWorldId.getText().toString(), 16);
                        if (!(0 <= moonWorldId && moonWorldId <= 0xffffffffffL))
                            throw new NumberFormatException();
                    } catch (NumberFormatException ignored) {
                        Snackbar.make(getView(), R.string.moon_world_id_wrong_format, BaseTransientBottomBar.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        moonSeed = Long.parseLong(textMoonSeed.getText().toString(), 16);
                        if (!(0 <= moonSeed && moonSeed <= 0xffffffffffL))
                            throw new NumberFormatException();
                    } catch (NumberFormatException ignored) {
                        Snackbar.make(getView(), R.string.moon_seed_wrong_format, BaseTransientBottomBar.LENGTH_SHORT).show();
                        return;
                    }
                    // 触发事件
                    this.eventBus.post(new AddMoonOrbitEvent(moonWorldId, moonSeed));
                });

        builder.create().show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddMoonOrbitEvent(AddMoonOrbitEvent event) {
        long moonWorldId = event.getMoonWorldId();
        long moonSeed = event.getMoonSeed();
        Log.i(TAG, "add orbit info " + Long.toHexString(moonWorldId));
        // 数据库修改
        DaoSession daoSession = ((AnalyticsApplication) getActivity().getApplication()).getDaoSession();
        long existCount = daoSession.getMoonOrbitDao().queryBuilder()
                .where(MoonOrbitDao.Properties.MoonWorldId.eq(moonWorldId),
                        MoonOrbitDao.Properties.MoonSeed.eq(moonSeed))
                .buildCount()
                .count();
        if (existCount > 0) {
            Snackbar.make(getView(), R.string.moon_orbit_exist, BaseTransientBottomBar.LENGTH_SHORT).show();
            return;
        }
        MoonOrbit moonOrbit = new MoonOrbit(moonWorldId, moonSeed);
        daoSession.getMoonOrbitDao().insert(moonOrbit);
        Snackbar.make(getView(), R.string.moon_orbit_add_success, BaseTransientBottomBar.LENGTH_SHORT).show();
        // 触发事件入轨
        this.eventBus.post(new OrbitMoonEvent(new ArrayList<MoonOrbit>() {{
            add(moonOrbit);
        }}));
        // 更新界面
        updateOrbitList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoveMoonOrbitEvent(RemoveMoonOrbitEvent event) {
        long moonWorldId = event.getMoonWorldId();
        long moonSeed = event.getMoonSeed();
        Log.i(TAG, "remove orbit info " + Long.toHexString(moonWorldId));
        // 删除数据
        DaoSession daoSession = ((AnalyticsApplication) getActivity().getApplication()).getDaoSession();
        daoSession.getMoonOrbitDao().queryBuilder()
                .where(MoonOrbitDao.Properties.MoonWorldId.eq(moonWorldId),
                        MoonOrbitDao.Properties.MoonSeed.eq(moonSeed))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
        Snackbar.make(getView(), R.string.moon_orbit_delete_success, BaseTransientBottomBar.LENGTH_SHORT).show();
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
            holder.mItem = mValues.get(position);
            holder.mMoonWorldId.setText(Long.toHexString(mValues.get(position).getMoonWorldId()));
            holder.mMoonSeed.setText(Long.toHexString(mValues.get(position).getMoonSeed()));
            // 长按菜单
            holder.mView.setOnLongClickListener(holder::onLongClick);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mMoonWorldId;
            public final TextView mMoonSeed;
            public MoonOrbit mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mMoonWorldId = (TextView) view.findViewById(R.id.moon_world_id);
                mMoonSeed = (TextView) view.findViewById(R.id.moon_seed);
            }

            /**
             * 为给定 view 创建弹出菜单
             */
            public boolean onLongClick(View view) {
                Log.d(TAG, "Long click " + this);
                PopupMenu popupMenu = new PopupMenu(MoonOrbitFragment.this.getActivity(), view);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu_moon_orbit, popupMenu.getMenu());
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.menu_item_delete_moon_orbit) {
                        // 触发事件
                        MoonOrbitFragment.this.eventBus.post(new RemoveMoonOrbitEvent(mItem.getMoonWorldId(), mItem.getMoonSeed()));
                        return true;
                    } else if (menuItem.getItemId() == R.id.menu_item_copy_moon_world_id) {
                        // 复制 Moon 地址
                        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(getString(R.string.network_id), Long.toHexString(this.mItem.getMoonWorldId()));
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getContext(), R.string.text_copied, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                });
                return true;
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mMoonWorldId.getText() + "', '" + mMoonSeed.getText() + "'";
            }
        }
    }
}
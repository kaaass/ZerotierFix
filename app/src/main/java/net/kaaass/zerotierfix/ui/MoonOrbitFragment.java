package net.kaaass.zerotierfix.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.model.MoonOrbit;

import java.util.ArrayList;
import java.util.List;

/**
 * 入轨配置片段
 */
public class MoonOrbitFragment extends Fragment {

    public static final String TAG = "MoonOrbitFragment";
    private static final String DIALOG_TAG = "dialog-moon-orbit";

    public MoonOrbitFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moon_orbit_list, container, false);

        // 设置适配器
        RecyclerView recyclerView = view.findViewById(R.id.list_moon_orbit);
        Context context = recyclerView.getContext();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new RecyclerViewAdapter(new ArrayList<MoonOrbit>() {{
            add(new MoonOrbit(0x123L, 0x456L));
            add(new MoonOrbit(0x123L, 0x456L));
            add(new MoonOrbit(0x123L, 0x456L));
        }}));

        // 设置添加按钮
        FloatingActionButton fab = view.findViewById(R.id.fab_moon_orbit);
        fab.setOnClickListener(parentView -> showMoonOrbitDialog());

        return view;
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
                    Toast.makeText(getContext(), "textMoonWorldId = " + textMoonWorldId.getText() + ", textMoonSeed = " + textMoonSeed.getText(), Toast.LENGTH_SHORT).show();
                });

        builder.create().show();
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
                    .inflate(R.layout.fragment_moon_orbit, parent, false);
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
                popupMenu.getMenuInflater().inflate(R.menu.context_menu_network_item, popupMenu.getMenu());
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    Log.d(TAG, "Click popup delete " + this);
                    return true;
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
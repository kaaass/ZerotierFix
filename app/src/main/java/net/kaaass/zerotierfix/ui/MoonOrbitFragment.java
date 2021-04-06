package net.kaaass.zerotierfix.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        fab.setOnClickListener(parentView -> {
            Snackbar.make(parentView, "Here's a Snackbar", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });

        return view;
    }

    public static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private final List<MoonOrbit> mValues;

        public RecyclerViewAdapter(List<MoonOrbit> items) {
            mValues = items;
        }

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
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
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

            @Override
            public String toString() {
                return super.toString() + " '" + mMoonWorldId.getText() + "', '" + mMoonSeed.getText() + "'";
            }
        }
    }
}
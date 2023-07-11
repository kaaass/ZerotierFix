package net.kaaass.zerotierfix.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.zerotier.sdk.Peer;
import com.zerotier.sdk.PeerPhysicalPath;
import com.zerotier.sdk.PeerRole;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.events.PeerInfoReplyEvent;
import net.kaaass.zerotierfix.events.PeerInfoRequestEvent;
import net.kaaass.zerotierfix.util.StringUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.ToString;

/**
 * Peer 信息展示 fragment
 *
 * @author kaaass
 */
public class PeerListFragment extends Fragment {

    public static final String TAG = "PeerListFragment";

    private final List<Peer> peerList = new ArrayList<>();

    private final EventBus eventBus;

    private RecyclerViewAdapter recyclerViewAdapter = null;

    private RecyclerView recyclerView = null;

    private SwipeRefreshLayout swipeRefreshLayout = null;

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

    public PeerListFragment() {
        this.eventBus = EventBus.getDefault();
        this.eventBus.register(this);
    }

    /**
     * Peer 类型转为文本
     */
    public static int peerRoleToString(PeerRole peerRole) {
        switch (peerRole) {
            case PEER_ROLE_PLANET:
                return R.string.peer_role_planet;
            case PEER_ROLE_LEAF:
                return R.string.peer_role_leaf;
            case PEER_ROLE_MOON:
                return R.string.peer_role_moon;
        }
        return 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_peer_list, container, false);

        // 空列表提示
        this.emptyView = view.findViewById(R.id.no_data);

        // 设置适配器
        this.recyclerView = view.findViewById(R.id.list_peer);
        Context context = this.recyclerView.getContext();
        this.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        this.recyclerViewAdapter = new RecyclerViewAdapter(this.peerList);
        this.recyclerViewAdapter.registerAdapterDataObserver(checkIfEmptyObserver);
        this.recyclerView.setAdapter(this.recyclerViewAdapter);

        // 设置下拉刷新
        this.swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_peer);
        this.swipeRefreshLayout.setOnRefreshListener(this::onRefresh);

        // 更新入轨数据
        this.eventBus.post(new PeerInfoRequestEvent());

        return view;
    }

    /**
     * 刷新列表
     */
    public void onRefresh() {
        this.eventBus.post(new PeerInfoRequestEvent());
        // 超时自动重置刷新状态
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> this.swipeRefreshLayout.setRefreshing(false));
            }
        }).start();
    }

    /**
     * 收到 peer 信息
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPeerInfoReplyEvent(PeerInfoReplyEvent event) {
        Peer[] peers = event.getPeers();
        if (peers == null) {
            Snackbar.make(requireView(), R.string.fail_retrieve_peer_list, BaseTransientBottomBar.LENGTH_LONG).show();
            return;
        }
        // 更新数据列表
        this.peerList.clear();
        Collections.addAll(this.peerList, peers);
        this.recyclerViewAdapter.notifyDataSetChanged();
        this.swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Peer 列表适配器
     */
    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        private final List<Peer> mValues;

        public RecyclerViewAdapter(List<Peer> items) {
            mValues = items;
        }

        @NonNull
        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_peer, parent, false);
            return new RecyclerViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerViewAdapter.ViewHolder holder, int position) {
            Peer peer = mValues.get(position);
            holder.mItem = peer;
            holder.mAddress.setText(Long.toHexString(peer.getAddress()));
            holder.mRole.setText(peerRoleToString(peer.getRole()));
            // 客户端版本
            String clientVersion = getString(R.string.unknown_version);
            if (peer.getVersionMajor() > 0) {
                clientVersion = StringUtils.peerVersionString(peer);
            }
            holder.mVersion.setText(clientVersion);
            // 延迟
            holder.mLatency.setText(String.format(getString(R.string.peer_lat), peer.getLatency()));
            // 当前路径
            PeerPhysicalPath preferred = null;
            if (peer.getPaths() != null) {
                for (PeerPhysicalPath path : peer.getPaths()) {
                    if (path.isPreferred()) {
                        preferred = path;
                        break;
                    }
                }
            }
            String strPreferred = getString(R.string.peer_relay);
            if (preferred != null) {
                strPreferred = StringUtils.toString(preferred.getAddress());
            }
            holder.mPath.setText(strPreferred);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        @ToString
        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mAddress;
            public final TextView mRole;
            public final TextView mVersion;
            public final TextView mLatency;
            public final TextView mPath;
            public Peer mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mAddress = view.findViewById(R.id.list_peer_addr);
                mRole = view.findViewById(R.id.list_peer_role);
                mVersion = view.findViewById(R.id.list_peer_ver);
                mLatency = view.findViewById(R.id.list_peer_lat);
                mPath = view.findViewById(R.id.list_peer_path);
            }
        }
    }
}

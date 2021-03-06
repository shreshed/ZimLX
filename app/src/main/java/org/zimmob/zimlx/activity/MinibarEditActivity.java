package org.zimmob.zimlx.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter_extensions.drag.ItemTouchCallback;
import com.mikepenz.fastadapter_extensions.drag.SimpleDragCallback;

import org.zimmob.zimlx.R;
import org.zimmob.zimlx.util.AppSettings;
import org.zimmob.zimlx.widget.Minibar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by saul on 04-25-18.
 * Project ZimLX
 * henriquez.saul@gmail.com
 */

public class MinibarEditActivity extends ThemeActivity implements ItemTouchCallback {
    @BindView(R.id.toolbar)
    public Toolbar _toolbar;

    @BindView(R.id.enableSwitch)
    public SwitchCompat _enableSwitch;

    @BindView(R.id.recyclerView)
    public RecyclerView _recyclerView;
    private FastItemAdapter<Item> _adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_minibar_edit);
        ButterKnife.bind(this);
        setSupportActionBar(_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(R.string.minibar);

        _adapter = new FastItemAdapter<>();

        SimpleDragCallback touchCallback = new SimpleDragCallback(this);
        ItemTouchHelper touchHelper = new ItemTouchHelper(touchCallback);
        touchHelper.attachToRecyclerView(_recyclerView);

        _recyclerView.setLayoutManager(new LinearLayoutManager(this));
        _recyclerView.setAdapter(_adapter);


        final ArrayList<String> minibarArrangement = AppSettings.get().getMinibarArrangement();
        for (Minibar.ActionDisplayItem item : Minibar.actionDisplayItems) {
            _adapter.add(new Item(item.id, item, minibarArrangement.contains(Integer.toString(item.id))));
        }

        boolean minBarEnable = AppSettings.get().getMinibarEnable();
        _enableSwitch.setChecked(minBarEnable);
        _enableSwitch.setText(minBarEnable ? R.string.on : R.string.off);
        _enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonView.setText(isChecked ? R.string.on : R.string.off);
            AppSettings.get().setMinibarEnable(isChecked);
            if (HomeActivity.Companion.getLauncher() != null) {
                HomeActivity.Companion.getLauncher().closeAppDrawer();
                HomeActivity.Companion.getLauncher().getDrawerLayout().setDrawerLockMode(isChecked ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        });

        setResult(RESULT_OK);
    }

    @Override
    protected void onPause() {
        ArrayList<String> minibarArrangement = new ArrayList<>();
        for (Item item : _adapter.getAdapterItems()) {
            if (item.enable)
                minibarArrangement.add(Long.toString(item.id));
        }
        AppSettings.get().setMinibarArrangement(minibarArrangement);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (HomeActivity.Companion.getLauncher() != null) {
            HomeActivity.Companion.getLauncher().initMinibar();
        }
        super.onStop();
    }

    @Override
    public boolean itemTouchOnMove(int oldPosition, int newPosition) {
        Collections.swap(_adapter.getAdapterItems(), oldPosition, newPosition);
        _adapter.notifyAdapterDataSetChanged();
        return false;
    }

    @Override
    public void itemTouchDropped(int i, int i1) {
    }

    public static class Item extends AbstractItem<Item, Item.ViewHolder> {
        final long id;
        final Minibar.ActionDisplayItem item;
        boolean enable;
        boolean edited;

        Item(long id, Minibar.ActionDisplayItem item, boolean enable) {
            this.id = id;
            this.item = item;
            this.enable = enable;
        }

        @Override
        public int getType() {
            return 0;
        }

        @Override
        public int getLayoutRes() {
            return R.layout.item_minibar_edit;
        }

        @Override
        public ViewHolder getViewHolder(@NonNull View v) {
            return new ViewHolder(v);
        }


        public void bindView(@NonNull ViewHolder holder, List payloads) {
            holder._tv.setText(item.label);
            holder._tv2.setText(item.description);
            holder._iv.setImageResource(item.icon);
            holder._cb.setChecked(enable);
            holder._cb.setOnCheckedChangeListener((compoundButton, b) -> {
                edited = true;
                enable = b;
            });
            super.bindView(holder, payloads);
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView _tv;
            TextView _tv2;
            ImageView _iv;
            CheckBox _cb;

            ViewHolder(View itemView) {
                super(itemView);
                _tv = itemView.findViewById(R.id.tv);
                _tv2 = itemView.findViewById(R.id.tv2);
                _iv = itemView.findViewById(R.id.iv);
                _cb = itemView.findViewById(R.id.cb);
            }
        }
    }
}

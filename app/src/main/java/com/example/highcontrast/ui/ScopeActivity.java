package com.example.highcontrast.ui;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.highcontrast.R;
import com.example.highcontrast.core.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScopeActivity extends AppCompatActivity {

    private ListView listView;
    private TextView emptyView, modeLabel;
    private Spinner modeSpinner;

    private List<AppEntry> apps = new ArrayList<>();
    private ScopeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scope);

        listView = findViewById(R.id.list);
        emptyView = findViewById(R.id.empty);
        modeLabel = findViewById(R.id.mode_label);
        modeSpinner = findViewById(R.id.mode_spinner);

        setupModeSpinner();
        adapter = new ScopeAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((p, v, pos, id) -> {
            AppEntry app = apps.get(pos);
            Intent intent = new Intent(this, ConfigActivity.class);
            intent.putExtra("pkg", app.pkg);
            intent.putExtra("name", app.name);
            startActivity(intent);
        });

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void setupModeSpinner() {
        String[] modes = {"全局生效", "白名单（仅选中）", "黑名单（排除选中）"};
        modeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modes));

        int current = ConfigManager.getScopeMode();
        modeSpinner.setSelection(Math.min(current, 2));

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                ConfigManager.setScopeMode(ScopeActivity.this, pos);
                updateModeLabel(pos);
                adapter.notifyDataSetChanged();
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        updateModeLabel(current);
    }

    private void updateModeLabel(int mode) {
        String scoped = ConfigManager.getScopedPackages();
        int count = scoped.isEmpty() ? 0 : scoped.split(",").length;
        switch (mode) {
            case ConfigManager.MODE_ALL:
                modeLabel.setText("所有应用均生效");
                break;
            case ConfigManager.MODE_WHITELIST:
                modeLabel.setText("仅对 " + count + " 个选中应用生效，点击应用切换选中状态");
                break;
            case ConfigManager.MODE_BLACKLIST:
                modeLabel.setText("排除 " + count + " 个应用，对其余全部生效");
                break;
        }
    }

    private void loadApps() {
        new AsyncTask<Void, Void, List<AppEntry>>() {
            @Override
            protected List<AppEntry> doInBackground(Void... v) {
                List<AppEntry> list = new ArrayList<>();
                PackageManager pm = getPackageManager();
                for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
                    if (ai.packageName.equals(getPackageName())) continue;
                    AppEntry e = new AppEntry();
                    e.pkg = ai.packageName;
                    e.name = ai.loadLabel(pm).toString();
                    e.icon = ai.loadIcon(pm);
                    e.isSystem = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    list.add(e);
                }
                Collections.sort(list, (a, b) -> a.name.compareToIgnoreCase(b.name));
                return list;
            }
            @Override
            protected void onPostExecute(List<AppEntry> result) {
                apps = result;
                adapter.notifyDataSetChanged();
                emptyView.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }.execute();
    }

    // ─── 适配器 ───
    class ScopeAdapter extends android.widget.BaseAdapter {
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int i) { return apps.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View v, android.view.ViewGroup p) {
            if (v == null) v = getLayoutInflater().inflate(R.layout.item_app, p, false);
            AppEntry app = apps.get(i);

            ((android.widget.ImageView) v.findViewById(R.id.icon)).setImageDrawable(app.icon);
            ((TextView) v.findViewById(R.id.name)).setText(app.name);
            ((TextView) v.findViewById(R.id.pkg)).setText(app.pkg);

            TextView scopeTag = v.findViewById(R.id.scope_tag);
            String scoped = ConfigManager.getScopedPackages();
            boolean inScope = scoped.contains(app.pkg);
            boolean enabled = ConfigManager.isPkgEnabled(app.pkg);

            int mode = modeSpinner.getSelectedItemPosition();
            if (mode == ConfigManager.MODE_ALL) {
                scopeTag.setText(enabled ? "生效" : "关闭");
                scopeTag.setTextColor(enabled ? 0xFF4CAF50 : 0xFFF44336);
            } else if (mode == ConfigManager.MODE_WHITELIST) {
                scopeTag.setText(inScope ? "已选" : "未选");
                scopeTag.setTextColor(inScope ? 0xFF4CAF50 : 0xFF9E9E9E);
            } else {
                scopeTag.setText(inScope ? "排除" : "生效");
                scopeTag.setTextColor(inScope ? 0xFFF44336 : 0xFF4CAF50);
            }

            // 点击切换作用域
            int mode2 = modeSpinner.getSelectedItemPosition();
            v.setOnLongClickListener(vv -> {
                if (mode2 == ConfigManager.MODE_ALL) {
                    ConfigManager.setPkgEnabled(ScopeActivity.this, app.pkg, !enabled);
                    Toast.makeText(ScopeActivity.this, app.name + (enabled ? " 已关闭" : " 已打开"), Toast.LENGTH_SHORT).show();
                } else {
                    String current = ConfigManager.getScopedPackages();
                    if (inScope) {
                        current = current.replace("," + app.pkg + ",", ",").replace(app.pkg + ",", "").replace("," + app.pkg, "").replace(app.pkg, "");
                        ConfigManager.setScopedPackages(ScopeActivity.this, current);
                    } else {
                        String added = current.isEmpty() ? app.pkg : current + "," + app.pkg;
                        ConfigManager.setScopedPackages(ScopeActivity.this, added);
                    }
                    adapter.notifyDataSetChanged();
                    updateModeLabel(mode2);
                }
                return true;
            });

            return v;
        }
    }

    static class AppEntry {
        String pkg, name;
        android.graphics.drawable.Drawable icon;
        boolean isSystem;
    }
}

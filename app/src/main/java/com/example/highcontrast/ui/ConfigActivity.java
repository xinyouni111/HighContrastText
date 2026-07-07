package com.example.highcontrast.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.highcontrast.R;
import com.example.highcontrast.core.ConfigManager;

public class ConfigActivity extends AppCompatActivity {

    private String pkg, name;
    private View previewBox, tcPreview, bgPreview;
    private TextView previewText, previewHint;
    private EditText etTextColor, etBgColor, etHintColor, etLinkColor, etSize;
    private Button btnSave, btnReset;

    private int tc = ConfigManager.AppConfig.DEF_TEXT;
    private int bc = ConfigManager.AppConfig.DEF_BG;
    private int hc = ConfigManager.AppConfig.DEF_HINT;
    private int lc = ConfigManager.AppConfig.DEF_LINK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        pkg = getIntent().getStringExtra("pkg");
        name = getIntent().getStringExtra("name");
        getSupportActionBar().setTitle(name != null ? name : pkg);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        previewBox = findViewById(R.id.preview);
        tcPreview = findViewById(R.id.tc_preview);
        bgPreview = findViewById(R.id.bg_preview);
        previewText = findViewById(R.id.preview_text);
        previewHint = findViewById(R.id.preview_hint);
        etTextColor = findViewById(R.id.et_text);
        etBgColor = findViewById(R.id.et_bg);
        etHintColor = findViewById(R.id.et_hint);
        etLinkColor = findViewById(R.id.et_link);
        etSize = findViewById(R.id.et_size);
        btnSave = findViewById(R.id.btn_save);
        btnReset = findViewById(R.id.btn_reset);

        load();
        updateUI();

        tcPreview.setOnClickListener(v -> showColorPick(true));
        bgPreview.setOnClickListener(v -> showColorPick(false));
        btnSave.setOnClickListener(v -> save());
        btnReset.setOnClickListener(v -> { load(); updateUI(); Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show(); });
    }

    private void load() {
        ConfigManager.AppConfig cfg = ConfigManager.getAppConfig(pkg);
        tc = cfg.textColor; bc = cfg.bgColor; hc = cfg.hintColor; lc = cfg.linkColor;
        etTextColor.setText(fmt(tc)); etBgColor.setText(fmt(bc));
        etHintColor.setText(fmt(hc)); etLinkColor.setText(fmt(lc));
        etSize.setText(String.valueOf((int) cfg.textSize));
    }

    private void save() {
        try {
            tc = parseColor(etTextColor.getText().toString());
            bc = parseColor(etBgColor.getText().toString());
            hc = parseColor(etHintColor.getText().toString());
            lc = parseColor(etLinkColor.getText().toString());
            float sz = Float.parseFloat(etSize.getText().toString());
            ConfigManager.setAppConfig(this, pkg,
                new ConfigManager.AppConfig(tc, bc, hc, lc, sz));
            updateUI();
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "颜色格式错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        tcPreview.setBackgroundColor(tc); bgPreview.setBackgroundColor(bc);
        previewBox.setBackgroundColor(bc);
        previewText.setTextColor(tc);
        previewHint.setTextColor(hc);
        etTextColor.setText(fmt(tc)); etBgColor.setText(fmt(bc));
        etHintColor.setText(fmt(hc)); etLinkColor.setText(fmt(lc));
    }

    private void showColorPick(final boolean isText) {
        final int[] presets = {
            Color.BLACK, Color.WHITE, 0xFFF44336, 0xFF4CAF50, 0xFF2196F3,
            0xFFFF9800, 0xFFFFEB3B, 0xFF9C27B0, 0xFF00BCD4, 0xFFFFD700,
            Color.GRAY, Color.DKGRAY, Color.LTGRAY, Color.CYAN, Color.MAGENTA
        };
        final String[] names = {
            "黑","白","红","绿","蓝","橙","黄","紫","青","金","灰","深灰","浅灰","青蓝","品红"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择颜色")
            .setItems(names, (d, which) -> {
                int c = presets[which];
                if (isText) {
                    tc = c; tcPreview.setBackgroundColor(tc);
                    previewText.setTextColor(tc); etTextColor.setText(fmt(tc));
                } else {
                    bc = c; bgPreview.setBackgroundColor(bc);
                    previewBox.setBackgroundColor(bc); etBgColor.setText(fmt(bc));
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private String fmt(int c) { return String.format("#%06X", 0xFFFFFF & c); }
    private int parseColor(String s) throws Exception {
        s = s.trim();
        if (!s.startsWith("#")) s = "#" + s;
        return Color.parseColor(s);
    }
}

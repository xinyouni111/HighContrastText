package com.example.highcontrast;

import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

import com.example.highcontrast.core.ConfigManager;

/**
 * 高对比度文字应用器 v2
 * 根据每应用的独立配置精确设置文字样式
 */
public class TextHelper {

    public static void apply(TextView tv, ConfigManager.AppConfig config) {
        if (tv == null || config == null) return;
        try {
            tv.setTextColor(config.textColor);
            tv.setHintTextColor(config.hintColor);
            tv.setLinkTextColor(config.linkColor);

            tv.setBackgroundColor(config.bgColor);

            if (config.textSize > 0) {
                float cur = tv.getTextSize();
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, cur + config.textSize);
            }

            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } catch (Exception ignored) {}
    }
}

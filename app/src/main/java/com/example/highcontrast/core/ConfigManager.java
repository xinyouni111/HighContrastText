package com.example.highcontrast.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * 配置管理器
 * UI 端用 SharedPreferences 读写，Hook 端用 XSharedPreferences（反射）跨进程读
 */
public class ConfigManager {

    private static final String PREFS = "hc_module";
    private static final String PKG = "com.example.highcontrast";

    public static final String KEY_SCOPE_MODE = "scope_mode";
    public static final String KEY_SCOPED_APPS = "scoped_apps";
    public static final String KEY_PKG_PREFIX = "cfg_";

    public static final int MODE_ALL = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_BLACKLIST = 2;

    private static Boolean sXposedOk;
    private static Object sXPrefs;

    private ConfigManager() {}

    private static boolean xposedOk() {
        if (sXposedOk == null) {
            try { Class.forName("de.robv.android.xposed.XSharedPreferences"); sXposedOk = true; }
            catch (Exception e) { sXposedOk = false; }
        }
        return sXposedOk;
    }

    // ─── XSharedPreferences 反射 ───
    private static Object getXPrefs() {
        if (!xposedOk()) return null;
        try {
            if (sXPrefs == null) {
                sXPrefs = Class.forName("de.robv.android.xposed.XSharedPreferences")
                    .getConstructor(String.class, String.class).newInstance(PKG, PREFS);
                sXPrefs.getClass().getMethod("makeWorldReadable").invoke(sXPrefs);
            } else {
                sXPrefs.getClass().getMethod("reload").invoke(sXPrefs);
            }
            return sXPrefs;
        } catch (Exception e) { return null; }
    }

    private static String xGetString(String key, String def) {
        try { return (String) getXPrefs().getClass().getMethod("getString", String.class, String.class).invoke(getXPrefs(), key, def); }
        catch (Exception e) { return def; }
    }
    private static int xGetInt(String key, int def) {
        try { return (int) getXPrefs().getClass().getMethod("getInt", String.class, int.class).invoke(getXPrefs(), key, def); }
        catch (Exception e) { return def; }
    }
    private static boolean xGetBool(String key, boolean def) {
        try { return (boolean) getXPrefs().getClass().getMethod("getBoolean", String.class, boolean.class).invoke(getXPrefs(), key, def); }
        catch (Exception e) { return def; }
    }

    // ─── SharedPreferences（UI 端） ───
    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ─── 作用域模式 ───
    public static int getScopeMode() {
        try { return Integer.parseInt(xGetString(KEY_SCOPE_MODE, "0")); }
        catch (Exception e) { return MODE_ALL; }
    }
    public static void setScopeMode(Context ctx, int mode) {
        prefs(ctx).edit().putString(KEY_SCOPE_MODE, String.valueOf(mode)).apply();
    }

    // ─── 作用域包列表 ───
    public static String getScopedPackages() {
        return xGetString(KEY_SCOPED_APPS, "");
    }
    public static void setScopedPackages(Context ctx, String packages) {
        prefs(ctx).edit().putString(KEY_SCOPED_APPS, packages).apply();
    }

    // ─── 作用域判断（Hook 端调用） ───
    public static boolean isPackageInScope(String pkg) {
        int mode = getScopeMode();
        if (mode == MODE_ALL) return true;
        String scoped = getScopedPackages();
        boolean inList = scoped.contains(pkg);
        return (mode == MODE_WHITELIST) ? inList : !inList;
    }

    // ─── 包级别开关 ───
    public static boolean isPkgEnabled(String pkg) {
        return xGetBool("en_" + pkg, true);
    }
    public static void setPkgEnabled(Context ctx, String pkg, boolean on) {
        prefs(ctx).edit().putBoolean("en_" + pkg, on).apply();
    }

    // ─── 每应用配置 ───
    public static AppConfig getAppConfig(String pkg) {
        String prefix = KEY_PKG_PREFIX + pkg + "_";
        return new AppConfig(
            xGetInt(prefix + "textColor", AppConfig.DEF_TEXT),
            xGetInt(prefix + "bgColor", AppConfig.DEF_BG),
            xGetInt(prefix + "hintColor", AppConfig.DEF_HINT),
            xGetInt(prefix + "linkColor", AppConfig.DEF_LINK),
            Float.parseFloat(xGetString(prefix + "size", "0"))
        );
    }

    public static void setAppConfig(Context ctx, String pkg, AppConfig cfg) {
        SharedPreferences p = prefs(ctx);
        String prefix = KEY_PKG_PREFIX + pkg + "_";
        p.edit()
            .putInt(prefix + "textColor", cfg.textColor)
            .putInt(prefix + "bgColor", cfg.bgColor)
            .putInt(prefix + "hintColor", cfg.hintColor)
            .putInt(prefix + "linkColor", cfg.linkColor)
            .putString(prefix + "size", String.valueOf(cfg.textSize))
            .apply();
    }

    // ─── 配置实体 ───
    public static class AppConfig {
        public static final int DEF_TEXT = Color.WHITE;
        public static final int DEF_BG = Color.BLACK;
        public static final int DEF_HINT = Color.argb(255, 180, 180, 180);
        public static final int DEF_LINK = Color.CYAN;

        public final int textColor, bgColor, hintColor, linkColor;
        public final float textSize;

        public AppConfig(int tc, int bc, int hc, int lc, float ts) {
            this.textColor = tc; this.bgColor = bc;
            this.hintColor = hc; this.linkColor = lc; this.textSize = ts;
        }
        public static AppConfig defaults() {
            return new AppConfig(DEF_TEXT, DEF_BG, DEF_HINT, DEF_LINK, 0);
        }
    }
}

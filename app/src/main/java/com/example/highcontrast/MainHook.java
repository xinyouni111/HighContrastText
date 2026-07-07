package com.example.highcontrast;

import android.content.ContentResolver;
import android.net.Uri;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook {

    public static void hook(final LoadPackageParam lpparam) throws Throwable {
        ClassLoader cl = lpparam.classLoader;

        // 1. Hook Settings.System 读取高对比文字配置
        hookSettingsRead(cl, "android.provider.Settings$System");

        // 2. Hook Settings.Secure (Android 13+ 迁移至此)
        hookSettingsRead(cl, "android.provider.Settings$Secure");

        // 3. Hook Settings.Global
        hookSettingsRead(cl, "android.provider.Settings$Global");

        // 4. ContentResolver 写入模拟 (可选, 确保完全模拟开启状态)
        XposedHelpers.findAndHookMethod(
                ContentResolver.class,
                "getString",
                Uri.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (isHighContrastSetting(param.args)) {
                            param.setResult("1");
                        }
                    }
                });
    }

    private static void hookSettingsRead(ClassLoader cl, String settingsClassName) {
        try {
            Class<?> settingsClass = cl.loadClass(settingsClassName);

            // Hook getString(ContentResolver, String)
            XposedHelpers.findAndHookMethod(settingsClass, "getString",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (isHighContrastSetting(param.args)) {
                                param.setResult("1");
                            }
                        }
                    });

            // Hook getInt(ContentResolver, String)
            XposedHelpers.findAndHookMethod(settingsClass, "getInt",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (isHighContrastSetting(param.args)) {
                                param.setResult(1);
                            }
                        }
                    });

            // Hook getInt(ContentResolver, String, int) 带默认值
            XposedHelpers.findAndHookMethod(settingsClass, "getInt",
                    ContentResolver.class, String.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (isHighContrastSetting(param.args)) {
                                param.setResult(1);
                            }
                        }
                    });

            // Hook getString(ContentResolver, String, String) 带默认值
            XposedHelpers.findAndHookMethod(settingsClass, "getString",
                    ContentResolver.class, String.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (isHighContrastSetting(param.args)) {
                                param.setResult("1");
                            }
                        }
                    });

        } catch (Exception e) {
            // 某些类或方法不存在则跳过
        }
    }

    private static boolean isHighContrastSetting(Object[] args) {
        if (args == null || args.length < 2) return false;
        if (args[1] instanceof String) {
            String name = (String) args[1];
            return "high_text_contrast_enabled".equals(name)
                    || "accessibility_high_text_contrast_enabled".equals(name);
        }
        return false;
    }
}

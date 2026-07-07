package com.example.highcontrast;

import android.widget.TextView;

import com.example.highcontrast.core.ConfigManager;

import java.lang.reflect.Constructor;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed 主入口 v2
 * 根据 ConfigManager 储存的作用域和每应用配置，精准应用高对比度
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String MY_PKG = "com.example.highcontrast";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (MY_PKG.equals(lpparam.packageName)) return;

        // 作用域过滤：仅处理已选中的应用
        if (!ConfigManager.isPackageInScope(lpparam.packageName)) return;

        // 包开关过滤
        if (!ConfigManager.isPkgEnabled(lpparam.packageName)) return;

        // 加载此应用的独立配置
        final ConfigManager.AppConfig config = ConfigManager.getAppConfig(lpparam.packageName);

        // Hook 所有 TextView 构造函数
        hookTextView(XposedHelpers.findClass("android.widget.TextView", lpparam.classLoader), config);
    }

    private void hookTextView(Class<?> textViewClass, final ConfigManager.AppConfig config) {
        XposedBridgeHack.hookAllConstructors(textViewClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                TextView tv = (TextView) param.thisObject;
                TextHelper.apply(tv, config);
            }
        });
    }

    // 兼容不同 Xposed 版本
    private static class XposedBridgeHack {
        static void hookAllConstructors(Class<?> clazz, XC_MethodHook hook) {
            try {
                de.robv.android.xposed.XposedBridge.hookAllConstructors(clazz, hook);
            } catch (NoSuchMethodError e) {
                for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                    XposedHelpers.findMethodBestMatch(clazz, "<init>", ctor.getParameterTypes());
                    try {
                        de.robv.android.xposed.XposedBridge.hookMethod(ctor, hook);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}

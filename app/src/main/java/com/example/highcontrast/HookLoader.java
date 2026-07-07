package com.example.highcontrast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookLoader implements IXposedHookLoadPackage {
    // 美团众包常见包名
    private static final String MEITUAN_CATERING = "com.meituan.catering.delivery";
    private static final String MEITUAN_BANMA = "com.meituan.banma";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(MEITUAN_CATERING)
            && !lpparam.packageName.equals(MEITUAN_BANMA)) {
            return;
        }
        MainHook.hook(lpparam);
    }
}

package com.android.dexunshell;

public class NativeTool {
    static{
        System.loadLibrary("dynamicplugin");
    }
    static native int loadDex(byte[] dex,long dexlen);
}

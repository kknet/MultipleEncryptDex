package com.android.dexunshell;

public class NativeTool {
    static{
        try {
            System.loadLibrary("dynamicplugin");
        }catch (Throwable e){

        }
    }
    static native int loadDex(byte[] dex,long dexlen);
}

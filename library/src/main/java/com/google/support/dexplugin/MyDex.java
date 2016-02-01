package com.google.support.dexplugin;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MyDex {
    static final String DEX_DIR = "plugin_cache";
    static final boolean LOG = BuildConfig.DEBUG;
    static String DEX_PREFIX = "assets/classes";
    static String ZIP_PREFIX = "plugin";
    static final String DEX_SUFFIX = ".dat";
    static final int MAX_NUM = 5;
    static final String TAG = "MyDex";

    public static boolean install(Context context, String prefix, String seed) {
        ApplicationInfo applicationInfo = getApplicationInfo(context);
        if (applicationInfo == null) {
            return false;
        }
        if (!TextUtils.isEmpty(prefix)) {
            DEX_PREFIX = prefix;
        }
        ClassLoader loader = null;
        try {
            loader = context.getClassLoader();
        } catch (RuntimeException e) {
            if (LOG)
                Log.w(TAG, "Failure while trying to obtain Context class loader. " +
                        "Must be running in test mode. Skip patching.", e);
        }
        if (loader == null) {
            Log.e(TAG,
                    "Context class loader is null. Must be running in test mode. "
                            + "Skip patching.");
            return false;
        }
        File dexDir = new File(applicationInfo.dataDir, DEX_DIR);
        if (!dexDir.exists()) {
            dexDir.mkdirs();
        }
        boolean ok = true;
        try {
            List<File> files = getPlugins(context, applicationInfo.sourceDir, dexDir, seed);
            if (Build.VERSION.SDK_INT >= 19) {
                if (LOG)
                    Log.d(TAG, "load form 19 size:" + files.size());
                V19.install(loader, files, dexDir);
            } else if (Build.VERSION.SDK_INT >= 14) {
                if (LOG)
                    Log.d(TAG, "load form 14 size:" + files.size());
                V14.install(loader, files, dexDir);
            } else {
                throw new RuntimeException("Multi dex installation failed. SDK min is 14.");
            }
        } catch (IOException e) {
            if (LOG)
                Log.e("System.err", "" + e);
            ok = false;
        } catch (Exception e) {
            if (LOG)
                Log.e("System.err", "" + e);
            ok = false;
        }
        return ok;
    }

    private static List<File> getPlugins(Context context, String sourceApk, File dexDir, String seed) throws IOException {
        ZipEntry e = null;
        final ZipFile apk = new ZipFile(sourceApk);
        int index = 2;
        File tmp1 = new File(dexDir, "1.tmp");
        File tmp2 = new File(dexDir, "2.tmp");
        File toZip;
        InputStream in;
        OutputStream out;
        List<File> files = new ArrayList<>();
        while (index < MAX_NUM && (e = apk.getEntry(DEX_PREFIX + index + DEX_SUFFIX)) != null) {
            //解压
            int i = index;
            index++;
            toZip = new File(dexDir, ZIP_PREFIX + i + ".zip");
            if (!toZip.exists()) {
                toZip.createNewFile();
            } else {
                if (toZip.lastModified() == e.getTime()) {
                    files.add(toZip);
                    if (LOG)
                        Log.d(TAG, "use old:" + e.getName() + "," + e.getTime());
                    continue;
                }
            }
            //清空tmp1
            tmp1.delete();
            in = apk.getInputStream(e);
            out = new FileOutputStream(tmp1);
            //复制assets文件
            write(in, out);
            if (isZip(tmp1)) {
                if (LOG)
                    Log.d(TAG, i + " is zip.");
                tmp1.renameTo(toZip);
                files.add(toZip);
                continue;
            }
            if (!isDex(tmp1)) {
                //解密
                AESUtils.decryptFile(seed, tmp1.getAbsolutePath(), tmp2.getAbsolutePath());
                if (isZip(tmp2)) {
                    if (LOG)
                        Log.d(TAG, i + " is zip.");
                    tmp2.renameTo(toZip);
                    files.add(toZip);
                    continue;
                }
            } else {
                if (LOG)
                    Log.d(TAG, i + " is dex.");
            }
            //添加zip
            ZipUtil.addToZip(toZip.getAbsolutePath(), "classes.dex", tmp2.getAbsolutePath(), e.getTime());
            toZip.setLastModified(e.getTime());
            files.add(toZip);
        }
        apk.close();
        tmp1.delete();
        tmp2.delete();
        return files;
    }

    private static void write(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int length = 0;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
    }

    final static byte[] HEAD_DEX = new byte[]{0x64, 0x65, 0x78, 0x0a};
    final static byte[] HEAD_ZIP = new byte[]{0x50, 0x4b, 0x3, 0x4};

    private static boolean isZip(File file) {
        byte[] head = readHead(file, HEAD_ZIP.length);
        for (int i = 0; i < HEAD_ZIP.length; i++) {
            if (head[i] != HEAD_ZIP[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDex(File file) {
        byte[] head = readHead(file, HEAD_DEX.length);
        for (int i = 0; i < HEAD_DEX.length; i++) {
            if (head[i] != HEAD_DEX[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] readHead(File file, int len) {
        FileInputStream in = null;
        byte[] head = new byte[len];
        try {
            in = new FileInputStream(file);
            in.read(head);
        } catch (Exception e) {

        }
        return head;
    }

    private static ApplicationInfo getApplicationInfo(Context context) {
        PackageManager pm;
        String packageName;
        try {
            pm = context.getPackageManager();
            packageName = context.getPackageName();
        } catch (RuntimeException e) {
            if (LOG)
                Log.w(TAG, "Failure while trying to obtain ApplicationInfo from Context. " +
                        "Must be running in test mode. Skip patching.", e);
            return null;
        }
        if (pm == null || packageName == null) {
            // This is most likely a mock context, so just return without patching.
            return null;
        }
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo =
                    pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {

        }
        return applicationInfo;
    }
}

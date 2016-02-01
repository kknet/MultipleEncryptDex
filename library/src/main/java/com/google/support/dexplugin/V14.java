package com.google.support.dexplugin;

import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/12/22.
 */
 /* package */  class V14 {
    /* package */
    static void install(ClassLoader loader, List<File> additionalClassPathEntries,
                        File optimizedDirectory)
            throws IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.BaseDexClassLoader. We modify its
             * dalvik.system.DexPathList pathList field to append additional DEX
             * file entries.
             */
        Field pathListField = Reflect.findField(loader, "pathList");
        Object dexPathList = pathListField.get(loader);
        if (MyDex.LOG)
            Log.d(MyDex.TAG, "dexPathList exist=" + (dexPathList != null));
        Reflect.expandFieldArray(dexPathList, "dexElements", makeDexElements(dexPathList,
                new ArrayList<File>(additionalClassPathEntries), optimizedDirectory));
    }

    /**
     * A wrapper around
     * {@code private static final dalvik.system.DexPathList#makeDexElements}.
     */
     /* package */
    static Object[] makeDexElements(
            Object dexPathList, ArrayList<File> files, File optimizedDirectory)
            throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        Method makeDexElements =
                Reflect.findMethod(dexPathList, "makeDexElements", ArrayList.class, File.class);
        if (MyDex.LOG)
            Log.d(MyDex.TAG, "makeDexElements exist=" + (makeDexElements != null));
        return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory);
    }
}

#include "com_android_dexunshell_NativeTool.h"
#include <stdlib.h>
#include <dlfcn.h>
#include <stdio.h>
#include <jni.h>
// 引入log头文件
#include  <android/log.h>
// log标签
#define  TAG    "ddex"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
// 定义debug信息
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
// 定义error信息
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
#define DEBUG 1
JNINativeMethod *dvm_dalvik_system_DexFile;
void (*openDexFile)(const u4* args,union  JValue* pResult);

int lookup(JNINativeMethod *table, const char *name, const char *sig,
   void (**fnPtrout)(u4 const *, union JValue *))
{
    int i = 0;
    while (table[i].name != NULL)
    {
        if(DEBUG)
        LOGI("lookup %d %s" ,i,table[i].name);
        if ((strcmp(name, table[i].name) == 0)
            && (strcmp(sig, table[i].signature) == 0))
        {
            *fnPtrout = table[i].fnPtr;
            return 1;
        }
        i++;
    }
    return 0;
}

/* This function will be call when the library first be load.
 * You can do some init in the libray. return which version jni it support.
 */
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    void *ldvm = (void*) dlopen("libdvm.so", RTLD_LAZY);
    dvm_dalvik_system_DexFile = (JNINativeMethod*) dlsym(ldvm,
        "dvm_dalvik_system_DexFile");
    if(0 == lookup(dvm_dalvik_system_DexFile, "openDexFile", "([B)I",
        &openDexFile))
    {
        openDexFile = NULL;
    }else
    {
        if(DEBUG)
        LOGI("method found ! HAVE_BIG_ENDIAN");
    }
    void *venv;
    if(DEBUG)
    LOGI("dufresne----->JNI_OnLoad!");
    if ((*vm)->GetEnv(vm, (void**) &venv, JNI_VERSION_1_4) != JNI_OK)
    {
        if(DEBUG)
            LOGE("dufresne--->ERROR: GetEnv failed");
        return -1;
    }
    return JNI_VERSION_1_4;
}

JNIEXPORT jint JNICALL Java_com_android_dexunshell_NativeTool_loadDex(
   JNIEnv * env, jclass jv, jbyteArray dexArray, jlong dexLen)
{
    // header+dex content
    u1 * olddata = (u1*)(*env)-> GetByteArrayElements(env,dexArray,   NULL);
    char* arr;
    arr=(char*)malloc(16+dexLen);
    ArrayObject *ao=(ArrayObject*)arr;
    ao->length=dexLen;
    memcpy(arr+16,olddata,dexLen);
    u4 args[] = { (u4) ao };
    union JValue pResult;
    jint result;
    if(DEBUG)
     LOGI("call openDexFile 33..." );
    if(openDexFile != NULL)
    {
        openDexFile(args,&pResult);
    }
    else
    {
        result = -1;
    }
    result = (jint) pResult.l;
    if(DEBUG)
        LOGI("Java_com_android_dexunshell_NativeTool_loadDex %d" , result);
    return result;
}
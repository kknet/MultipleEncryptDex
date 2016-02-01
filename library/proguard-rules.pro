# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript internal
# class:
#-keepclassmembers class fqcn.of.javascript.internal.for.webview {
#   public *;
#}
-keepparameternames
-keep class com.google.support.dexplugin.MyDex {
     public static *;
 }
-keep class com.google.support.dexplugin.AESUtils {
      public static *;
  }
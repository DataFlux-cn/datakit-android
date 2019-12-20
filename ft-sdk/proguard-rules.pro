# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#保护代码中的Annotation不被混淆
-keepattributes *Annotation

##避免混淆泛型
-keepattributes Signature

#抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable

-keep class com.ft.sdk.FTSdk{
   public *;
}
-keep class com.ft.sdk.FTSDKConfig{
   public *;
}
-keep class com.ft.sdk.FTTrack{
   public *;
}

-keep class com.ft.sdk.FTAutoTrack{
     *;
}

-keep enum com.ft.sdk.FTAutoTrackType{
     *;
}

-keep class com.ft.sdk.garble.http.ResponseData{
     *;
}

-keep class * extends com.ft.sdk.garble.http.ResponseData{
     *;
}



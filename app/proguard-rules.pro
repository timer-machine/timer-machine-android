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

-repackageclasses
-allowaccessmodification

# Datas
-keep class xyz.aprildown.timer.data.datas.SchedulerData { *; }
-keep class xyz.aprildown.timer.data.datas.TimerData { *; }
-keep class xyz.aprildown.timer.data.datas.TimerMoreData { *; }
-keep class xyz.aprildown.timer.data.datas.BehaviourData { *; }
-keep class xyz.aprildown.timer.data.datas.StepData { *; }
-keep class xyz.aprildown.timer.data.datas.StepData$Step { *; }
-keep class xyz.aprildown.timer.data.datas.StepData$Group { *; }
-keep class xyz.aprildown.timer.data.datas.AppDataData { *; }
-keep class xyz.aprildown.timer.data.datas.TimerStampData { *; }
-keep class xyz.aprildown.timer.data.datas.TimerInfoData { *; }

-keep class xyz.aprildown.timer.domain.entities.SchedulerRepeatMode { *; }
-keep class xyz.aprildown.timer.domain.entities.BehaviourType { *; }
-keep class xyz.aprildown.timer.domain.entities.StepType { *; }

# Moshi https://github.com/square/moshi#r8--proguard
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}

-keep @com.squareup.moshi.JsonQualifier interface *

# Enum field names are used by the integrated EnumJsonAdapter.
# values() is synthesized by the Kotlin compiler and is used by EnumJsonAdapter indirectly
# Annotate enums with @JsonClass(generateAdapter = false) to use them with Moshi.
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
    **[] values();
}

# Okio https://github.com/square/okio#r8--proguard
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# MPAndroidChart https://github.com/PhilJay/MPAndroidChart/issues/348
-keep public class com.github.mikephil.charting.animation.* {
    public protected *;
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkParameterIsNotNull(...);
    public static void throwUninitializedPropertyAccessException(...);
}

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
-dontobfuscate

# --- Android Auto OEM projection SDK (vendored prebuilt .aar) ---
# These classes are instantiated reflectively by Android Auto / the SDK, so R8
# must not strip them. The SDK is also compiled against the legacy
# android.support.** APIs, which are not on our classpath; silence those warnings.
-keep class com.google.android.apps.auto.sdk.** { *; }
-keep class com.google.android.gms.car.** { *; }
-keep class android.support.car.** { *; }

# Our own projection entry points, referenced only from the manifest.
-keep class com.kododake.aabrowser.car.** { *; }

-dontwarn com.google.android.apps.auto.sdk.**
-dontwarn com.google.android.gms.car.**
-dontwarn android.support.**
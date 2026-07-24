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

# ---------------------------------------------------------------------------
# Safety-net rules for release minification (isMinifyEnabled is currently
# false, but these keep the app from breaking the moment it is turned on).
# ---------------------------------------------------------------------------

# Room: generated implementations and entities/DAOs use reflection-adjacent
# annotation processing; keep entities and Dao interfaces intact.
-keep class com.example.models.** { *; }
-keep interface com.example.services.*Dao { *; }
-keep class com.example.services.AppDatabase_Impl { *; }
-dontwarn androidx.room.**

# Retrofit / OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Moshi: keep generated JsonAdapters and @JsonClass models
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class **JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-dontwarn com.squareup.moshi.**

# Coil
-dontwarn coil.**

# CameraX
-dontwarn androidx.camera.**

# Keep AI provider implementations (loaded by name/reflection-free factory,
# but their public API surface is used from Compose lambdas which R8 can
# otherwise strip if it misjudges reachability).
-keep class com.example.services.ai.** { *; }

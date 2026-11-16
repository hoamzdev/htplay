# Media3 Transformer / ExoPlayer rules
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Media3 Transformer specific
-keep class androidx.media3.transformer.** { *; }
-keep interface androidx.media3.transformer.** { *; }

# For reflection on MediaItem and related classes
-keep class androidx.media3.common.MediaItem { *; }
-keep class androidx.media3.common.MediaMetadata { *; }
-keep class androidx.media3.common.MimeTypes { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Room entities
-keep class * extends androidx.room.Entity { *; }

# Keep Hilt generated classes
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Keep Koin modules
-keep class * extends org.koin.core.module.Module { *; }

# Prevent obfuscation of generated Hilt components
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# OKHttp logging interceptor
-dontwarn okhttp3.logging.**

# Retrofit/Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }

# Media3 HLS
-keep class androidx.media3.exoplayer.hls.** { *; }

# Default Android ProGuard rules
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends androidx.fragment.app.Fragment

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable creator
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep safe args
-keepnames class * implements androidx.navigation.NavArgs { *; }
-keepnames class * implements androidx.navigation.NavDirections { *; }
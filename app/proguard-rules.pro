# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep line numbers and source attributes for debugging
-keepattributes SourceFile,LineNumberTable,*Annotation*

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep interface androidx.media3.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }

# OkHttp & Networking
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin Coroutines & Reflection
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { *; }

# Data Models & JSON Serialization
-keepclassmembers class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.remote.** { *; }

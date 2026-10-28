# Mankeu - R8 / ProGuard Configuration

# Room Database and SQLite
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.mankeu.data.** { *; }

# Domain entities and calculations
-keep class com.mankeu.domain.** { *; }

# Authentication, Google Play Services
-keep class com.google.android.gms.auth.api.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.mankeu.auth.** { *; }

# Cloud Backup & Encryption models
-keep class com.mankeu.backup.** { *; }

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX Lifecycle
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}


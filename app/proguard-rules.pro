# Proguard rules for JARVIS Android Application

# Room Database and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Biometrics
-keep class androidx.biometric.** { *; }

# WorkManager Workers
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Lifecycle & ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Security & EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

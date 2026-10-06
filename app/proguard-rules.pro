# Salim ProGuard Rules

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Room database
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Play Billing Library
-keep class com.android.billingclient.api.** { *; }

# Models and Room entities
-keep class com.example.data.model.** { *; }
-keep class com.example.data.db.** { *; }

# Coil
-keep class coil.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

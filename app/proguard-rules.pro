-keepattributes SourceFile,LineNumberTable

# Room — entities must survive minification (field names map to DB column names)
-keep class com.mapgie.goflo.data.database.entities.** { *; }

# Room — DAO interfaces are referenced by generated code; keep all members
-keep interface com.mapgie.goflo.data.database.dao.** { *; }
-keep class com.mapgie.goflo.data.database.dao.**_Impl { *; }

# Room — RoomDatabase subclass referenced by reflection at runtime
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# DataStore — Kotlin serialization internals used by Preferences DataStore
-keepclassmembers class * {
    @androidx.datastore.preferences.protobuf.ProtoField *;
}

# Biometric — internal classes referenced by the BiometricManager/Prompt APIs
-keep class androidx.biometric.** { *; }

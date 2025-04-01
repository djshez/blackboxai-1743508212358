# Add project specific ProGuard rules here.

# Keep the Application class
-keep class com.example.ht2000obd.OBDApplication { *; }

# Keep all Fragment classes
-keep class com.example.ht2000obd.fragments.** { *; }

# Keep all ViewModel classes
-keep class com.example.ht2000obd.viewmodel.** { *; }

# Keep all Model classes
-keep class com.example.ht2000obd.model.** { *; }

# Keep all Database related classes
-keep class com.example.ht2000obd.data.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX Navigation
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.fragment.app.Fragment{}

# Bluetooth related classes
-keep class android.bluetooth.** { *; }

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}

# General Android
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove debug logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# Remove unused code
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
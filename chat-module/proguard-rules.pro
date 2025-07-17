# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /android-sdk/tools/proguard/proguard-android.txt

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Socket.IO classes
-keep class io.socket.** { *; }
-keep class io.socket.emitter.** { *; }

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep chat module model classes
-keep class com.example.chatmodule.model.** { *; }
-keep class com.example.chatmodule.data.** { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
} 
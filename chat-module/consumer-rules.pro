# Consumer proguard rules for chat module

# Keep all public APIs
-keep public class com.example.chatmodule.ChatManager { *; }
-keep public class com.example.chatmodule.ChatConfiguration { *; }
-keep public class com.example.chatmodule.model.** { *; }

# Keep Socket.IO classes for consumers
-keep class io.socket.** { *; }
-keep class io.socket.emitter.** { *; }

# Keep JSON parsing classes
-keep class com.google.gson.** { *; } 
# Chat Module SDK

A comprehensive, flexible chat module for Android that supports both Jetpack Compose and XML layouts. Simply add the module to your app, configure your socket URL, and you're ready to implement chat functionality with minimal effort.

## ✨ Features

- 🔄 **Easy Integration** - Just add the module and configure socket URL
- 🎨 **Dual UI Support** - Both Jetpack Compose and XML/Fragment implementations
- 🌐 **Dynamic Event Handling** - Customize event names and add custom event listeners
- 📱 **Rich Message Types** - Text, images, audio, and file messages
- 🌙 **Theme Adaptability** - Automatic dark/light theme support with custom colors
- 📴 **Offline Messaging** - Queue messages when offline and send when reconnected
- 🔧 **Highly Configurable** - Enable/disable features as needed
- 📊 **Message Status** - Sending, sent, delivered, read, and failed states
- 🎯 **Custom Message Models** - Extend and customize message structure
- 📎 **File Attachments** - Support for images, audio, and files with compression

## 🚀 Quick Start

### 1. Add the Module

Include the chat module in your `settings.gradle.kts`:

```kotlin
include(":chat-module")
```

Add dependency in your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":chat-module"))
}
```

### 2. Initialize ChatManager

```kotlin
// Create current user
val currentUser = User(
    id = "user123",
    name = "John Doe",
    email = "john.doe@example.com"
)

// Configure and initialize chat manager
val chatManager = ChatManager.Builder()
    .socketUrl("wss://your-socket-server.com") // Your socket server URL
    .currentUser(currentUser)
    .build(context)

// Initialize and connect
chatManager.initialize()
chatManager.connect()
```

### 3. Use in Compose

```kotlin
@Composable
fun MyChatScreen() {
    ChatModuleTheme {
        ChatScreen(
            chatManager = chatManager,
            chatRoomId = "room_123",
            uiConfig = UIConfiguration(),
            onBackClick = { /* Handle back */ },
            onAttachmentClick = { /* Handle attachments */ }
        )
    }
}
```

### 4. Use with XML/Fragments

```kotlin
val chatFragment = ChatFragment.newInstance(
    chatManager = chatManager,
    chatRoomId = "room_123",
    uiConfig = UIConfiguration()
)

supportFragmentManager.beginTransaction()
    .replace(R.id.fragment_container, chatFragment)
    .commit()
```

## 🔧 Advanced Configuration

### Custom Event Names

```kotlin
val chatManager = ChatManager.Builder()
    .socketUrl("wss://your-server.com")
    .currentUser(user)
    .eventNames {
        copy(
            sendMessage = "custom_send",
            receiveMessage = "custom_receive",
            messageDelivered = "msg_delivered",
            messageRead = "msg_read"
        )
    }
    .build(context)
```

### Feature Configuration

```kotlin
val chatManager = ChatManager.Builder()
    .socketUrl("wss://your-server.com")
    .currentUser(user)
    .features {
        copy(
            enableOfflineMessaging = true,
            enableFileSharing = true,
            enableImageSharing = true,
            enableAudioMessages = false, // Disable audio messages
            enableTypingIndicators = true,
            enableReadReceipts = true
        )
    }
    .build(context)
```

### UI Customization

```kotlin
val chatManager = ChatManager.Builder()
    .socketUrl("wss://your-server.com")
    .currentUser(user)
    .uiConfig {
        copy(
            theme = ChatTheme.DARK, // Force dark theme
            showTimestamps = true,
            showUserAvatars = true,
            enableMessageBubbles = true,
            customColors = ChatColors(
                primaryColor = 0xFF2196F3,
                sendBubbleColor = 0xFF4CAF50,
                receiveBubbleColor = 0xFFE0E0E0
            )
        )
    }
    .build(context)
```

### File Upload Configuration

```kotlin
val chatManager = ChatManager.Builder()
    .socketUrl("wss://your-server.com")
    .currentUser(user)
    .fileConfig {
        copy(
            maxFileSize = 25 * 1024 * 1024, // 25MB
            enableImageCompression = true,
            maxImageDimension = 2048,
            imageQuality = 85
        )
    }
    .build(context)
```

### Custom Event Handlers

```kotlin
val chatManager = ChatManager.Builder()
    .socketUrl("wss://your-server.com")
    .currentUser(user)
    .addCustomEventHandler("user_typing") { data ->
        // Handle typing indicator
        val typingData = JSONObject(data.toString())
        val userId = typingData.getString("userId")
        showTypingIndicator(userId)
    }
    .addCustomEventHandler("custom_notification") { data ->
        // Handle custom notifications
        handleCustomNotification(data)
    }
    .build(context)
```

## 📨 Sending Messages

### Text Messages

```kotlin
chatManager.sendMessage("Hello, World!", "room_123")
```

### Image Messages

```kotlin
chatManager.sendImageMessage(
    imageUrl = "https://example.com/image.jpg",
    chatRoomId = "room_123",
    thumbnailUrl = "https://example.com/thumb.jpg"
)
```

### File Messages

```kotlin
chatManager.sendFileMessage(
    fileUrl = "https://example.com/document.pdf",
    fileName = "document.pdf",
    fileSize = 1024000,
    mimeType = "application/pdf",
    chatRoomId = "room_123"
)
```

### Audio Messages

```kotlin
chatManager.sendAudioMessage(
    audioUrl = "https://example.com/audio.mp3",
    duration = 30000, // 30 seconds in milliseconds
    chatRoomId = "room_123"
)
```

### Custom Messages

```kotlin
val customMessage = Message(
    senderId = currentUser.id,
    senderName = currentUser.name,
    content = "Custom content",
    type = MessageType.TEXT,
    chatRoomId = "room_123",
    metadata = mapOf("customField" to "customValue")
)

chatManager.sendMessage(customMessage)
```

## 🎨 Theming

### Using Built-in Themes

```kotlin
ChatModuleTheme(
    chatTheme = ChatTheme.DARK, // LIGHT, DARK, or AUTO
    content = {
        ChatScreen(...)
    }
)
```

### Custom Colors

```kotlin
ChatModuleTheme(
    customColors = ChatColors(
        primaryColor = 0xFF6200EA,
        secondaryColor = 0xFF03DAC6,
        backgroundColor = 0xFFFFFFFF,
        sendBubbleColor = 0xFF6200EA,
        receiveBubbleColor = 0xFFF5F5F5
    ),
    content = {
        ChatScreen(...)
    }
)
```

## 🔌 Socket Events

The module automatically handles standard chat events, but you can also listen to custom events:

### Standard Events (Configurable)
- `send_message` - Send a message
- `receive_message` - Receive a message
- `message_delivered` - Message delivery confirmation
- `message_read` - Message read confirmation
- `user_typing` - User typing indicator
- `user_joined` - User joined room
- `user_left` - User left room

### Adding Custom Event Listeners

```kotlin
// Add listener
chatManager.addEventListener("custom_event") { data ->
    // Handle custom event
}

// Remove listener
chatManager.removeEventListener("custom_event", listener)

// Emit custom event
chatManager.emit("custom_event", customData)
```

## 💾 Offline Support

Messages are automatically queued when offline and sent when connection is restored:

```kotlin
// Enable offline messaging (enabled by default)
.features {
    copy(enableOfflineMessaging = true)
}
```

Offline messages are stored locally using Room database and automatically sent when connection is reestablished.

## 📱 Message Types

### Text Messages
Standard text messages with support for:
- Multi-line text
- Emoji support
- Text selection
- Rich text formatting

### Image Messages
- Automatic image compression
- Thumbnail generation
- Progressive loading
- Zoom capabilities

### File Messages
- Support for any file type
- File size display
- Download progress
- MIME type detection

### Audio Messages
- Duration display
- Playback controls
- Waveform visualization (optional)
- Audio compression

## 🎯 Message Status

Messages show real-time status:
- ⏳ **Sending** - Message is being sent
- ✓ **Sent** - Message sent to server
- ✓✓ **Delivered** - Message delivered to recipient
- ✓✓ **Read** - Message read by recipient (blue)
- ❌ **Failed** - Message failed to send

## 🔧 Configuration Options

### ChatFeatures
```kotlin
data class ChatFeatures(
    val enableOfflineMessaging: Boolean = true,
    val enableFileSharing: Boolean = true,
    val enableImageSharing: Boolean = true,
    val enableAudioMessages: Boolean = true,
    val enableTypingIndicators: Boolean = true,
    val enableReadReceipts: Boolean = true,
    val enableMessageReplies: Boolean = true,
    val enableMessageReactions: Boolean = false,
    val enableUserPresence: Boolean = true
)
```

### UIConfiguration
```kotlin
data class UIConfiguration(
    val theme: ChatTheme = ChatTheme.AUTO,
    val showTimestamps: Boolean = true,
    val showUserAvatars: Boolean = true,
    val showDeliveryStatus: Boolean = true,
    val enableMessageBubbles: Boolean = true,
    val messageMaxLines: Int = 5,
    val customColors: ChatColors? = null
)
```

### FileConfiguration
```kotlin
data class FileConfiguration(
    val maxFileSize: Long = 10 * 1024 * 1024, // 10MB
    val enableImageCompression: Boolean = true,
    val maxImageDimension: Int = 1920,
    val imageQuality: Int = 80,
    val enableAudioCompression: Boolean = true
)
```

## 📋 Requirements

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35
- **Kotlin**: 2.0.21+
- **Compose BOM**: 2024.12.01+

## 📦 Dependencies

The module includes:
- Socket.IO Client for real-time communication
- Room Database for offline storage
- Jetpack Compose for modern UI
- Coil for image loading
- Material Design 3 components

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the IROID License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue on GitHub
- Check the documentation
- Review the demo app for implementation examples


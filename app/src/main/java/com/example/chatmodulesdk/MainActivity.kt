package com.example.chatmodulesdk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chatmodule.ChatManager
import com.example.chatmodule.config.ChatConfiguration
import com.example.chatmodule.ui.compose.ChatScreen
import com.example.chatmodule.ui.theme.ChatModuleTheme
import com.example.chatmodulesdk.ui.theme.ChatModuleSDKTheme

class MainActivity : ComponentActivity() {

    private lateinit var chatManager: ChatManager
    private var currentRoomId = ""

    // File picker launcher for all types of files
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    // Image picker launcher specifically for images
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedImage(it) }
    }

    // Audio picker launcher
    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedAudio(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ChatManager first
        initializeChatManager()

        // Make sure chatManager is initialized before setting content
        if (::chatManager.isInitialized) {
            enableEdgeToEdge()
            setContent {
                ChatModuleSDKTheme {
                    ChatModuleTheme {
                        MainScreen()
                    }
                }
            }
        } else {
            Log.e("MainActivity", "ChatManager not initialized properly")
            finish() // Close the activity if initialization failed
        }
    }

    /**
     * Initializes the ChatManager with configuration
     * ✅ UPDATED: Now uses configurable parameter names!
     * Shows how to customize field names to match YOUR server API
     */
    private fun initializeChatManager() {
        if (ChatManagerProvider.isInitialized()) {
            chatManager = ChatManagerProvider.getChatManager()
            Log.d("MainActivity", "✅ ChatManager retrieved from provider")
            return
        }

        Log.d("MainActivity", "🚀 Initializing ChatManager with custom parameter configuration...")

        try {
            val configuration = ChatConfiguration.Builder(
                serverUrl = "https://demo.iroidsolutions.com:7050",
                authToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwOlwvXC9kZW1vLmlyb2lkc29sdXRpb25zLmNvbTo3MDUwXC9hcGlcL2xvZ2luIiwiaWF0IjoxNzMxNjY2ODA3LCJleHAiOjE3MzI4NzY0MDcsIm5iZiI6MTczMTY2NjgwNywianRpIjoicTlYdENPRlpkVjR6MTJOeSIsInN1YiI6IjE2MyIsInBydiI6IjIzYmQ1Yzg5NDlmNjAwYWRiMzllNzAxYzQwMDg3MmRiN2E1OTc2ZjcifQ.FKr8K5heCd5eoJLJgO8I1gWl5L_dqTyHKSBpopZmHQQ"
            )
                .setCurrentUser("163", "Kartik")
                .setReceivedUser("120")
                .setConnectionSettings(timeout = 10000, reconnectionAttempts = 3)
                .setFileSharing(enabled = true, maxSize = 5 * 1024 * 1024)
                .setMediaMessages(enableAudio = true, enableImages = true)
                .setParameterConfig(
                    com.example.chatmodule.config.ParameterConfiguration(
                        parameterNames = mapOf(
                            "message" to "message",
                            "type" to "type",
                            "senderId" to "senderId",
                            "receiverId" to "receiverId",
                            "roomId" to "roomId",
                            "fileName" to "file_name",
                            "name" to "name",
                            "chatId" to "chatId",
                            "token" to "token"
                        ),
                        eventNames = mapOf(
                            "sendMessage" to "sendMessage",
                            "receiveMessage" to "newMessage",
                            "messageDelivered" to "messageDelivered",
                            "messageRead" to "messageRead",
                            "createRoom" to "createRoom",
                            "roomConnected" to "roomConnected",
                            "joinRoom" to "joinRoom",
                            "leaveRoom" to "leaveRoom"
                        )
                    )
                )
                .build()

            chatManager = ChatManager.Builder(this, configuration).build()
            chatManager.initialize()
            chatManager.connect()

            // Store in provider
            ChatManagerProvider.setChatManager(chatManager)

            Log.d("MainActivity", "✅ ChatManager initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error initializing ChatManager: ${e.message}", e)
            throw e // Rethrow to handle in onCreate
        }
    }

    @Composable
    private fun MainScreen() {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Chat Module SDK Demo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose Implementation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { openComposeChat() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Compose Chat")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { openXmlChat() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open XML Chat")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Features Enabled:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val features = listOf(
                            "✅ Offline messaging",
                            "✅ File sharing",
                            "✅ Image sharing",
                            "✅ Audio messages",
                            "✅ Typing indicators",
                            "✅ Read receipts",
                            "✅ Dark/Light theme",
                            "✅ Custom colors",
                            "✅ Message status",
                            "✅ Custom events"
                        )

                        features.forEach { feature ->
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openComposeChat() {
        // Verify chatManager is initialized before proceeding
        if (!::chatManager.isInitialized) {
            Log.e("MainActivity", "Cannot open chat - ChatManager not initialized")
            return
        }

        setContent {
            ChatModuleSDKTheme {
                ChatModuleTheme {
                    var actualRoomId by remember { mutableStateOf<String?>(null) }
                    var isRoomReady by remember { mutableStateOf(false) }
                    val receiverId = 120

                    LaunchedEffect(Unit) {
                        try {
                            Log.d("MainActivity", "🏠 Creating room for Compose chat with receiver: $receiverId")

                            // Setup room creation response listener FIRST
                            chatManager.addEventListener(
                                chatManager.getConfiguration().parameterConfig.getEventName("roomConnected")
                            ) { data ->
                                Log.d("MainActivity", "📥 Compose chat room creation response: $data")

                                try {
                                    val roomId = com.example.chatmodule.model.ChatRoom.extractRoomIdFromResponse(
                                        data,
                                        chatManager.getConfiguration().parameterConfig
                                    )

                                    if (roomId != null) {
                                        Log.d("MainActivity", "✅ Compose chat room created: $roomId")
                                        actualRoomId = roomId
                                        currentRoomId = roomId
                                        isRoomReady = true

                                        chatManager.joinRoom(roomId)
                                        Log.d("MainActivity", "🚪 Joined room: $roomId")
                                    } else {
                                        Log.e("MainActivity", "❌ No room ID in response, using fallback")
                                        actualRoomId = "demo_room_123"
                                        isRoomReady = true
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "❌ Error processing room response", e)
                                    actualRoomId = "demo_room_123"
                                    isRoomReady = true
                                }
                            }

                            chatManager.createRoom(
                                senderID = chatManager.currentUser.value.id.toInt(),
                                receiverID = receiverId
                            )

                            Log.d("MainActivity", "📤 Room creation request sent, waiting for response...")
                        } catch (e: Exception) {
                            Log.e("MainActivity", "❌ Error in room creation process", e)
                            // Handle error appropriately
                        }
                    }

                    if (isRoomReady && actualRoomId != null) {
                        ChatScreen(
                            chatManager = chatManager,
                            chatRoomId = actualRoomId!!,
                            uiConfig = chatManager.getConfiguration().uiConfig,
                            onBackClick = { recreate() },
                            onAttachmentClick = { showAttachmentOptions() }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Creating chat room...")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showAttachmentOptions() {
        // Show a dialog or bottom sheet with attachment options
        filePickerLauncher.launch("*/*")  // Launch with all file types
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            // Get file metadata
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(uri)
            val fileSize = getFileSize(uri)

            // Upload file and get URL (implement your upload logic)
            val fileUrl = uploadFile(uri)

            // Send message based on file type
            when {
                mimeType.startsWith("image/") -> {
                    chatManager.sendImageMessage(
                        imageUrl = fileUrl,
                        chatRoomId = currentRoomId
                    )
                }
                mimeType.startsWith("audio/") -> {
                    // Get audio duration
                    val duration = getAudioDuration(uri)
                    chatManager.sendAudioMessage(
                        audioUrl = fileUrl,
                        duration = duration,
                        chatRoomId = currentRoomId
                    )
                }
                else -> {
                    chatManager.sendFileMessage(
                        fileUrl = fileUrl,
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        chatRoomId = currentRoomId
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling file: ${e.message}", e)
            // Show error to user
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val fileUrl = uploadFile(uri)
            chatManager.sendImageMessage(
                imageUrl = fileUrl,
                chatRoomId = currentRoomId
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling image: ${e.message}", e)
        }
    }

    private fun handleSelectedAudio(uri: Uri) {
        try {
            val fileUrl = uploadFile(uri)
            val duration = getAudioDuration(uri)
            chatManager.sendAudioMessage(
                audioUrl = fileUrl,
                duration = duration,
                chatRoomId = currentRoomId
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling audio: ${e.message}", e)
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "file"
    }

    private fun getFileSize(uri: Uri): Long {
        return contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
    }

    private fun getAudioDuration(uri: Uri): Long {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            return durationStr?.toLongOrNull() ?: 0
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting audio duration: ${e.message}", e)
            return 0
        } finally {
            retriever.release()
        }
    }

    private fun uploadFile(uri: Uri): String {
        // TODO: Implement your file upload logic here
        // This should:
        // 1. Upload the file to your server
        // 2. Return the URL where the file can be accessed
        // For now, returning a dummy URL
        return "https://your-server.com/uploads/${getFileName(uri)}"
    }

    private fun openXmlChat() {
        val intent = Intent(this, XmlChatActivity::class.java).apply {
            // Don't pass EXTRA_CHAT_ROOM_ID to trigger room creation flow
            // putExtra(XmlChatActivity.EXTRA_CHAT_ROOM_ID, "demo_room_123") // Commented out
            putExtra(XmlChatActivity.EXTRA_RECEIVER_ID, 120) // Target user for conversation
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::chatManager.isInitialized) {
            chatManager.disconnect()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if chatManager is initialized and socket is connected
        if (::chatManager.isInitialized) {
            if (chatManager.connectionState.value != com.example.chatmodule.network.ConnectionState.CONNECTED) {
                chatManager.connect()
                // If you want to rejoin a room, you need to know the current room ID:
                chatManager.joinRoom(currentRoomId)
            }
        }
    }


} 
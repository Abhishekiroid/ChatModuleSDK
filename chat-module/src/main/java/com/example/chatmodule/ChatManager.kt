package com.example.chatmodule

import android.content.Context
import android.util.Log
import com.example.chatmodule.config.ChatConfiguration
import com.example.chatmodule.data.ChatRepository
import com.example.chatmodule.model.Message
import com.example.chatmodule.model.MessageType
import com.example.chatmodule.network.ConnectionState
import com.example.chatmodule.network.EventManager
import com.example.chatmodule.network.SocketManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * Main chat manager class - the primary interface for integrating chat functionality
 * 
 * Usage:
 * ```
 * val chatManager = ChatManager.Builder()
 *     .socketUrl("https://your-socket-server.com")
 *     .currentUser(user)
 *     .build(context)
 * 
 * chatManager.initialize()
 * chatManager.connect()
 * ```
 */
class ChatManager private constructor(
    private val context: Context,
    private val configuration: ChatConfiguration
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()
    
    private var socketManager: SocketManager? = null
    private var eventManager: EventManager? = null
    private var chatRepository: ChatRepository? = null
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _currentUser = MutableStateFlow(
        com.example.chatmodule.model.User(
            id = configuration.currentUserId,
            name = configuration.currentUserName
        )
    )
    val currentUser: StateFlow<com.example.chatmodule.model.User> = _currentUser.asStateFlow()
    
    /**
     * Initializes the chat manager and sets up all necessary components
     */
    fun initialize() {
        Log.d("ChatManager", "🚀 Initializing ChatManager...")
        Log.d("ChatManager", "🔧 Server URL: ${configuration.serverUrl}")
        Log.d("ChatManager", "🔧 Current User: ${configuration.currentUserId} (${configuration.currentUserName})")
        Log.d("ChatManager", "🔧 Auth Token: ${if (configuration.authToken.isNotEmpty()) "PROVIDED" else "NOT PROVIDED"}")
        Log.d("ChatManager", "🔧 Parameter Config: Room ID field = ${configuration.parameterConfig.parameterNames["roomId"]}")
        Log.d("ChatManager", "🔧 Parameter Config: Room created event = ${configuration.parameterConfig.eventNames["roomConnected"]}")
        
        socketManager = SocketManager(context, configuration)
        eventManager = socketManager!!.initialize()
        chatRepository = ChatRepository(context, configuration)
        
        setupEventListeners()
        observeConnectionState()
        
        Log.d("ChatManager", "✅ ChatManager initialized successfully")
    }
    
    /**
     * Connects to the chat server
     */
    fun connect() {
        Log.d("ChatManager", "🔌 Connecting to chat server...")
        socketManager?.connect()
    }
    
    /**
     * Disconnects from the chat server
     */
    fun disconnect() {
        Log.d("ChatManager", "🔌 Disconnecting from chat server...")
        socketManager?.disconnect()
    }
    
    /**
     * Sends a text message
     */
    fun sendMessage(content: String, chatRoomId: String) {
        Log.d("ChatManager", "💬 Sending text message to room: $chatRoomId")
        Log.d("ChatManager", "💬 Message content: $content")
        
        val message = Message(
            senderId = configuration.currentUserId,
            senderName = configuration.currentUserName,
            content = content,
            receiverId = configuration.receiverId,
            type = MessageType.TEXT,
            chatRoomId = chatRoomId
        )
        
        sendMessage(message)
    }
    
    /**
     * Sends a message with custom type and data
     */
    fun sendMessage(message: Message) {
        Log.d("ChatManager", "📤 Sending message: ${message.id}")
        Log.d("ChatManager", "📤 Message type: ${message.type}")
        Log.d("ChatManager", "📤 Message to room: ${message.chatRoomId}")
        
        // Add to local list immediately for better UX
        updateMessagesList(message)
        
        // Store in local database
        scope.launch {
            chatRepository?.insertMessage(message)
            Log.d("ChatManager", "💾 Message stored in local database: ${message.id}")
        }
        
        // Send to server using configurable parameters
        val params = configuration.parameterConfig.createMessageParameters(
            roomId = message.chatRoomId,
            senderId = message.senderId,
            senderName = message.senderName,
            receiverId = message.receiverId,
            content = message.content,
            type = message.type.value,
            token = configuration.authToken
        )
        
        Log.d("ChatManager", "📤 Sending to server via event: ${configuration.parameterConfig.getEventName("sendMessage")}")
        Log.d("ChatManager", "📤 Message parameters: $params")
        socketManager?.emit(configuration.parameterConfig.getEventName("sendMessage"), JSONObject(params))
    }
    
    /**
     * Sends an image message
     */
    fun sendImageMessage(imageUrl: String, chatRoomId: String, thumbnailUrl: String? = null) {
        Log.d("ChatManager", "🖼️ Sending image message to room: $chatRoomId")
        Log.d("ChatManager", "🖼️ Image URL: $imageUrl")
        
        if (!configuration.enableImageMessages) {
            Log.e("ChatManager", "❌ Image messages are disabled in configuration")
            throw IllegalStateException("Image messages are disabled in configuration")
        }
        
        val message = Message(
            senderId = configuration.currentUserId,
            senderName = configuration.currentUserName,
            content = "Image",
            receiverId = configuration.receiverId,
            type = MessageType.IMAGE,
            chatRoomId = chatRoomId,
            fileUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            mimeType = "image/*"
        )
        
        sendMessage(message)
    }
    
    /**
     * Sends a file message
     */
    fun sendFileMessage(
        fileUrl: String,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        chatRoomId: String
    ) {
        Log.d("ChatManager", "📎 Sending file message to room: $chatRoomId")
        Log.d("ChatManager", "📎 File: $fileName ($fileSize bytes)")
        
        if (!configuration.enableFileSharing) {
            Log.e("ChatManager", "❌ File sharing is disabled in configuration")
            throw IllegalStateException("File sharing is disabled in configuration")
        }
        
        val message = Message(
            senderId = configuration.currentUserId,
            senderName = configuration.currentUserName,
            content = fileName,
            receiverId = configuration.receiverId,
            type = MessageType.FILE,
            chatRoomId = chatRoomId,
            fileName = fileName,
            fileSize = fileSize,
            fileUrl = fileUrl,
            mimeType = mimeType
        )
        
        sendMessage(message)
    }
    
    /**
     * Sends an audio message
     */
    fun sendAudioMessage(
        audioUrl: String,
        duration: Long,
        chatRoomId: String
    ) {
        Log.d("ChatManager", "🎵 Sending audio message to room: $chatRoomId")
        Log.d("ChatManager", "🎵 Duration: ${duration}ms")
        
        if (!configuration.enableAudioMessages) {
            Log.e("ChatManager", "❌ Audio messages are disabled in configuration")
            throw IllegalStateException("Audio messages are disabled in configuration")
        }
        
        val message = Message(
            senderId = configuration.currentUserId,
            senderName = configuration.currentUserName,
            receiverId = configuration.receiverId,
            content = "Audio message",
            type = MessageType.AUDIO,
            chatRoomId = chatRoomId,
            fileUrl = audioUrl,
            duration = duration,
            mimeType = "audio/*"
        )
        
        sendMessage(message)
    }
    
    /**
     * Creates a new chat room using configurable parameter names
     */
    fun createRoom(
        senderID: Int,
        receiverID: Int,
    ) {
        Log.d("ChatManager", "🏠 Creating room with senderID: $senderID and receiverID: $receiverID")
        
        // Use configurable parameter names
        val params = mapOf(
            configuration.parameterConfig.parameterNames["senderId"]!! to senderID,
            configuration.parameterConfig.parameterNames["receiverId"]!! to receiverID,
            configuration.parameterConfig.parameterNames["token"]!! to configuration.authToken
        )
        
        Log.d("ChatManager", "📤 Creating room with parameters: $params")
        Log.d("ChatManager", "📤 Using event name: ${configuration.parameterConfig.getEventName("createRoom")}")
        
        socketManager?.emit(configuration.parameterConfig.getEventName("createRoom"), JSONObject(params))
    }
    
    /**
     * Joins a chat room using configurable parameter names
     */
    fun joinRoom(roomId: String) {
        Log.d("ChatManager", "🚪 Joining room: $roomId")
        
        val params = mapOf(
            configuration.parameterConfig.parameterNames["token"]!! to configuration.authToken,
            configuration.parameterConfig.parameterNames["roomId"]!! to roomId
        )
        
        Log.d("ChatManager", "📤 Joining room with parameters: $params")
        Log.d("ChatManager", "📤 Using event name: ${configuration.parameterConfig.getEventName("joinRoom")}")
        
        socketManager?.emit(configuration.parameterConfig.getEventName("joinRoom"), JSONObject(params))
    }
    
    /**
     * Leaves a chat room using configurable parameter names
     */
    fun leaveRoom(roomId: String) {
        Log.d("ChatManager", "🚪 Leaving room: $roomId")
        
        val params = mapOf(
            configuration.parameterConfig.parameterNames["token"]!! to configuration.authToken,
            configuration.parameterConfig.parameterNames["roomId"]!! to roomId,
            configuration.parameterConfig.parameterNames["senderId"]!! to configuration.currentUserId
        )
        
        Log.d("ChatManager", "📤 Leaving room with parameters: $params")
        Log.d("ChatManager", "📤 Using event name: ${configuration.parameterConfig.getEventName("leaveRoom")}")
        
        socketManager?.emit(configuration.parameterConfig.getEventName("leaveRoom"), JSONObject(params))
    }
    
    /**
     * Adds a custom event listener
     */
    fun addEventListener(eventName: String, listener: (data: Any) -> Unit) {
        Log.d("ChatManager", "🎧 Adding custom event listener: $eventName")
        eventManager?.addEventListener(eventName, listener)
    }
    
    /**
     * Removes an event listener
     */
    fun removeEventListener(eventName: String, listener: (data: Any) -> Unit) {
        Log.d("ChatManager", "🗑️ Removing event listener: $eventName")
        eventManager?.removeEventListener(eventName, listener)
    }
    
    /**
     * Emits a custom event
     */
    fun emit(eventName: String, data: Any? = null) {
        Log.d("ChatManager", "📤 Emitting custom event: $eventName")
        Log.d("ChatManager", "📤 Custom event data: $data")
        socketManager?.emit(eventName, data)
    }
    
    /**
     * Gets the current event manager for advanced usage
     */
    fun getEventManager(): EventManager? = eventManager
    
    /**
     * Gets the current socket manager for advanced usage
     */
    fun getSocketManager(): SocketManager? = socketManager
    
    /**
     * Gets messages for a specific chat room
     */
    suspend fun getMessagesForRoom(roomId: String): List<Message> {
        return chatRepository?.getMessagesForRoom(roomId) ?: emptyList()
    }
    
    /**
     * Gets the current configuration
     */
    fun getConfiguration(): ChatConfiguration = configuration
    
    /**
     * Sets up default event listeners
     */
    private fun setupEventListeners() {
        Log.d("ChatManager", "🎧 Setting up default event listeners...")
        
        eventManager?.apply {
            // Message received
            addEventListener(configuration.parameterConfig.getEventName("receiveMessage")) { data ->
                Log.d("ChatManager", "📥 RECEIVED MESSAGE EVENT: ${configuration.parameterConfig.getEventName("receiveMessage")}")
                handleReceivedMessage(data)
            }
            
            // Message delivered
            addEventListener(configuration.parameterConfig.getEventName("messageDelivered")) { data ->
                Log.d("ChatManager", "📥 RECEIVED MESSAGE DELIVERED EVENT")
                handleMessageDelivered(data)
            }
            
            // Message read
            addEventListener(configuration.parameterConfig.getEventName("messageRead")) { data ->
                Log.d("ChatManager", "📥 RECEIVED MESSAGE READ EVENT")
                handleMessageRead(data)
            }
            
            // Room created
            addEventListener(configuration.parameterConfig.getEventName("roomConnected")) { data ->
                Log.d("ChatManager", "📥 RECEIVED ROOM CREATED EVENT")
                handleRoomCreated(data)
            }
        }
        
        Log.d("ChatManager", "✅ Event listeners setup completed")
    }
    
    /**
     * Observes connection state changes
     */
    private fun observeConnectionState() {
        Log.d("ChatManager", "👀 Starting to observe connection state...")
        
        scope.launch {
            socketManager?.connectionState?.collect { state ->
                Log.d("ChatManager", "🔄 Connection state changed: $state")
                _connectionState.value = state
            }
        }
    }
    
    /**
     * Handles received messages
     */
    private fun handleReceivedMessage(data: Any) {
        Log.d("ChatManager", "📥 Handling received message...")
        Log.d("ChatManager", "📥 Raw message data: $data")
        
        try {
            val jsonData = if (data is JSONObject) data else JSONObject(data.toString())
            
            // Create message from received data using dynamic parameter names
            val message = Message(
                id = UUID.randomUUID().toString(),
                senderId = jsonData.getString(configuration.parameterConfig.parameterNames["senderId"]),
                senderName = jsonData.getString(configuration.parameterConfig.parameterNames["name"]),
                receiverId = jsonData.getString(configuration.parameterConfig.parameterNames["receiverId"]),
                content = jsonData.getString(configuration.parameterConfig.parameterNames["message"]),
                type = when {
                    jsonData.get(configuration.parameterConfig.parameterNames["type"]) is String -> 
                        MessageType.fromInt(jsonData.getString(configuration.parameterConfig.parameterNames["type"]).toInt())
                    else -> 
                        MessageType.fromInt(jsonData.getInt(configuration.parameterConfig.parameterNames["type"]))
                },
                chatRoomId = jsonData.getString(configuration.parameterConfig.parameterNames["roomId"]),
                fileName = if (jsonData.has(configuration.parameterConfig.parameterNames["fileName"])) 
                    jsonData.getString(configuration.parameterConfig.parameterNames["fileName"]) else null
            )
            
            Log.d("ChatManager", "📥 Parsed message: ID=${message.id}, From=${message.senderName}, Type=${message.type}")
            
            updateMessagesList(message)
            
            // Store in local database
            scope.launch {
                chatRepository?.insertMessage(message)
                Log.d("ChatManager", "💾 Received message stored in database: ${message.id}")
            }
        } catch (e: Exception) {
            Log.e("ChatManager", "❌ Error handling received message", e)
        }
    }
    
    /**
     * Handles message delivered events
     */
    private fun handleMessageDelivered(data: Any) {
        Log.d("ChatManager", "📥 Handling message delivered event...")
        Log.d("ChatManager", "📥 Delivered data: $data")
        
        try {
            val jsonData = if (data is JSONObject) data else JSONObject(data.toString())
            val messageId = jsonData.getString("messageId")
            
            Log.d("ChatManager", "✅ Message delivered: $messageId")
            
            // Update message status in local list and database
            updateMessageStatus(messageId) { it.copy(status = com.example.chatmodule.model.MessageStatus.DELIVERED) }
        } catch (e: Exception) {
            Log.e("ChatManager", "❌ Error handling message delivered event", e)
        }
    }
    
    /**
     * Handles message read events
     */
    private fun handleMessageRead(data: Any) {
        Log.d("ChatManager", "📥 Handling message read event...")
        Log.d("ChatManager", "📥 Read data: $data")
        
        try {
            val jsonData = if (data is JSONObject) data else JSONObject(data.toString())
            val messageId = jsonData.getString("messageId")
            
            Log.d("ChatManager", "👁️ Message read: $messageId")
            
            // Update message status in local list and database
            updateMessageStatus(messageId) { it.copy(status = com.example.chatmodule.model.MessageStatus.READ) }
        } catch (e: Exception) {
            Log.e("ChatManager", "❌ Error handling message read event", e)
        }
    }
    
    /**
     * Handles room creation events
     */
    private fun handleRoomCreated(data: Any) {
        Log.d("ChatManager", "📥 Handling room created event...")
        Log.d("ChatManager", "📥 Room created data: $data")

        try {
            // Use robust extraction
            val roomId = com.example.chatmodule.model.ChatRoom.extractRoomIdFromResponse(
                data,
                configuration.parameterConfig
            )
            if (roomId != null) {
                Log.d("ChatManager", "🏠 Room created successfully: $roomId")
                // Optionally: notify listeners, update state, etc.
            } else {
                Log.w("ChatManager", "⚠️ No room ID found in room created event data")
            }
        } catch (e: Exception) {
            Log.e("ChatManager", "❌ Error handling room created event", e)
        }
    }
    
    /**
     * Updates the messages list
     */
    private fun updateMessagesList(message: Message) {
        val currentMessages = _messages.value.toMutableList()
        val existingIndex = currentMessages.indexOfFirst { it.id == message.id }
        
        if (existingIndex >= 0) {
            currentMessages[existingIndex] = message
        } else {
            currentMessages.add(message)
        }
        
        _messages.value = currentMessages.sortedBy { it.timestamp }
    }
    
    /**
     * Updates message status
     */
    private fun updateMessageStatus(messageId: String, update: (Message) -> Message) {
        val currentMessages = _messages.value.toMutableList()
        val messageIndex = currentMessages.indexOfFirst { it.id == messageId }
        
        if (messageIndex >= 0) {
            currentMessages[messageIndex] = update(currentMessages[messageIndex])
            _messages.value = currentMessages
            
            // Update in database
            scope.launch {
                chatRepository?.updateMessage(currentMessages[messageIndex])
            }
        }
    }
    
    /**
     * Builder class for ChatManager using new configuration system
     */
    class Builder(
        private val context: Context,
        private val configuration: ChatConfiguration
    ) {
        /**
         * Builds the ChatManager instance with the provided configuration
         */
        fun build(): ChatManager {
            return ChatManager(context, configuration)
        }
    }
    
    companion object {
        /**
         * Creates a new ChatManager builder with context and configuration
         */
        fun Builder(context: Context, configuration: ChatConfiguration): Builder = Builder(context, configuration)
        
        /**
         * Creates a ChatManager with existing configuration
         */
        fun create(context: Context, configuration: ChatConfiguration): ChatManager {
            return ChatManager(context, configuration)
        }
    }
} 
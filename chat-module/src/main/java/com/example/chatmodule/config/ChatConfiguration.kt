package com.example.chatmodule.config

import com.example.chatmodule.ui.theme.UiConfig

/**
 * Configuration for customizable parameter names
 * Allows different projects to use their own API conventions
 */
data class ParameterConfiguration(
    // Dynamic parameter names with defaults
    val parameterNames: Map<String, String> = mapOf(
        "message" to "message",
        "type" to "type",
        "senderId" to "senderId",
        "receiverId" to "receiverId",
        "roomId" to "roomId",
        "fileName" to "file_name",
        "name" to "name",
        "chatId" to "chatId",
        "token" to "token",
        "messageId" to "messageId",
        "timestamp" to "timestamp",
        "fileSize" to "fileSize",
        "fileUrl" to "fileUrl",
        "mimeType" to "mimeType"
    ),

    // Event names with defaults
    val eventNames: Map<String, String> = mapOf(
        "sendMessage" to "sendMessage",
        "receiveMessage" to "newMessage",
        "messageDelivered" to "messageDelivered",
        "messageRead" to "messageRead",
        "createRoom" to "createRoom",
        "roomConnected" to "roomConnected",
        "joinRoom" to "joinRoom",
        "leaveRoom" to "leaveRoom"
    ),

    // Type format configuration
    val useStringForMessageType: Boolean = false
) {
    
    /**
     * Creates a parameter map for message sending
     */
    fun createMessageParameters(
        roomId: String, 
        senderId: String, 
        senderName: String,
        receiverId: String,
        content: String, 
        type: Int = 0,
        token: String
    ): Map<String, Any> {
        val messageType = if (useStringForMessageType) type.toString() else type
        
        return mapOf(
            parameterNames["message"]!! to content,
            parameterNames["type"]!! to messageType,
            parameterNames["senderId"]!! to senderId,
            parameterNames["receiverId"]!! to receiverId,
            parameterNames["roomId"]!! to roomId,
            parameterNames["fileName"]!! to "",
            parameterNames["name"]!! to senderName
        )
    }

    /**
     * Updates parameter names dynamically
     */
    fun withParameterNames(newNames: Map<String, String>): ParameterConfiguration {
        return copy(parameterNames = parameterNames + newNames)
    }

    /**
     * Updates event names dynamically
     */
    fun withEventNames(newNames: Map<String, String>): ParameterConfiguration {
        return copy(eventNames = eventNames + newNames)
    }

    /**
     * Gets the event name for a specific event
     */
    fun getEventName(key: String): String {
        return eventNames[key] ?: key
    }
}

/**
 * Main configuration class for the chat module
 */
data class ChatConfiguration(
    val serverUrl: String,
    val authToken: String,
    val currentUserId: String,
    val currentUserName: String,
    val receiverId: String = "", // Default receiver ID, can be overridden
    val enableOfflineMessages: Boolean = true,
    val enableFileSharing: Boolean = true,
    val enableAudioMessages: Boolean = true,
    val enableImageMessages: Boolean = true,
    val enableVideoMessages: Boolean = true,
    val maxFileSize: Long = 10 * 1024 * 1024, // 10MB
    val maxVideoSize: Long = 50 * 1024 * 1024, // 50MB default for videos
    val connectionTimeout: Long = 10000,
    val reconnectionAttempts: Int = 5,
    val uiConfig: UiConfig = UiConfig(),
    val parameterConfig: ParameterConfiguration = ParameterConfiguration() // 🆕 Configurable parameters
) {
    
    /**
     * Builder pattern for easier configuration
     */
    class Builder(private val serverUrl: String, private val authToken: String) {
        private var currentUserId: String = "1"
        private var receiverId: String = "1"
        private var currentUserName: String = "User"
        private var enableOfflineMessages: Boolean = true
        private var enableFileSharing: Boolean = true
        private var enableAudioMessages: Boolean = true
        private var enableImageMessages: Boolean = true
        private var enableVideoMessages: Boolean = true
        private var maxFileSize: Long = 10 * 1024 * 1024
        private var maxVideoSize: Long = 50 * 1024 * 1024
        private var connectionTimeout: Long = 10000
        private var reconnectionAttempts: Int = 5
        private var uiConfig: UiConfig = UiConfig()
        private var parameterConfig: ParameterConfiguration = ParameterConfiguration()
        
        fun setCurrentUser(userId: String, userName: String) = apply {
            this.currentUserId = userId
            this.currentUserName = userName
        }
        fun setReceivedUser(userId: String) = apply {
            this.receiverId = userId
        }
        
        fun setOfflineMessages(enabled: Boolean) = apply {
            this.enableOfflineMessages = enabled
        }
        
        fun setFileSharing(enabled: Boolean, maxSize: Long = 10 * 1024 * 1024) = apply {
            this.enableFileSharing = enabled
            this.maxFileSize = maxSize
        }
        
        fun setMediaMessages(
            enableAudio: Boolean = true,
            enableImages: Boolean = true,
            enableVideos: Boolean = true
        ) = apply {
            this.enableAudioMessages = enableAudio
            this.enableImageMessages = enableImages
            this.enableVideoMessages = enableVideos
        }

        fun setVideoConfig(
            enabled: Boolean = true,
            maxSize: Long = 50 * 1024 * 1024
        ) = apply {
            this.enableVideoMessages = enabled
            this.maxVideoSize = maxSize
        }
        
        fun setConnectionSettings(timeout: Long = 10000, reconnectionAttempts: Int = 5) = apply {
            this.connectionTimeout = timeout
            this.reconnectionAttempts = reconnectionAttempts
        }
        
        fun setUiConfig(config: UiConfig) = apply {
            this.uiConfig = config
        }
        
        /**
         * 🆕 Configure parameter names to match your API
         */
        fun setParameterConfig(config: ParameterConfiguration) = apply {
            this.parameterConfig = config
        }
        
        /**
         * Quick method to set common parameter overrides
         */
        fun setParameterNames(
            roomIdField: String? = null,
            roomIdResponseField: String? = null,
            senderIdField: String? = null,
            receiverIdField: String? = null,
            sendMessageEvent: String? = null,
            receiveMessageEvent: String? = null,
            createRoomEvent: String? = null,
            roomCreatedEvent: String? = null
        ) = apply {
            val newParameterNames = mutableMapOf<String, String>()
            val newEventNames = mutableMapOf<String, String>()

            // Update parameter names if provided
            if (roomIdField != null) newParameterNames["roomId"] = roomIdField
            if (senderIdField != null) newParameterNames["senderId"] = senderIdField
            if (receiverIdField != null) newParameterNames["receiverId"] = receiverIdField

            // Update event names if provided
            if (sendMessageEvent != null) newEventNames["sendMessage"] = sendMessageEvent
            if (receiveMessageEvent != null) newEventNames["receiveMessage"] = receiveMessageEvent
            if (createRoomEvent != null) newEventNames["createRoom"] = createRoomEvent
            if (roomCreatedEvent != null) newEventNames["roomConnected"] = roomCreatedEvent

            this.parameterConfig = this.parameterConfig
                .withParameterNames(newParameterNames)
                .withEventNames(newEventNames)
        }
        
        fun build(): ChatConfiguration {
            return ChatConfiguration(
                serverUrl = serverUrl,
                authToken = authToken,
                currentUserId = currentUserId,
                receiverId = receiverId,
                currentUserName = currentUserName,
                enableOfflineMessages = enableOfflineMessages,
                enableFileSharing = enableFileSharing,
                enableAudioMessages = enableAudioMessages,
                enableImageMessages = enableImageMessages,
                enableVideoMessages = enableVideoMessages,
                maxFileSize = maxFileSize,
                maxVideoSize = maxVideoSize,
                connectionTimeout = connectionTimeout,
                reconnectionAttempts = reconnectionAttempts,
                uiConfig = uiConfig,
                parameterConfig = parameterConfig
            )
        }
    }
} 
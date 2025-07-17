package com.example.chatmodule.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Represents different types of messages supported by the chat module
 */
enum class MessageType(val value: Int) {
    TEXT(0),
    IMAGE(1),
    AUDIO(2),
    FILE(3),
    SYSTEM(4);

    companion object {
        fun fromInt(value: Int): MessageType = values().find { it.value == value } ?: TEXT
    }
}

/**
 * Represents the status of a message
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/**
 * Represents a chat message with support for different content types
 */
@Parcelize
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val receiverId: String, // For one-to-one chats
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val chatRoomId: String,
    
    // File-related properties
    val fileName: String? = null,
    val fileSize: Long? = null,
    val fileUrl: String? = null,
    val mimeType: String? = null,
    
    // Media properties
    val thumbnailUrl: String? = null,
    val duration: Long? = null, // For audio/video in milliseconds
    
    // Reply/thread properties
    val replyToMessageId: String? = null,
    val replyToMessage: Message? = null,
    
    // Metadata
    val metadata: Map<String, String> = emptyMap(),
    
    // Local properties (not synced)
    val isLocal: Boolean = false,
    val localFilePath: String? = null
) : Parcelable {
    
    /**
     * Checks if this message contains media content
     */
    fun isMediaMessage(): Boolean {
        return type in listOf(MessageType.IMAGE, MessageType.AUDIO, MessageType.FILE)
    }
    
    /**
     * Gets the display text for the message
     */
    fun getDisplayText(): String {
        return when (type) {
            MessageType.TEXT -> content
            MessageType.IMAGE -> "📷 Image"
            MessageType.AUDIO -> "🎵 Audio message"
            MessageType.FILE -> "📎 ${fileName ?: "File"}"
            MessageType.SYSTEM -> content
        }
    }
    
    /**
     * Creates a copy of the message with updated status
     */
    fun withStatus(newStatus: MessageStatus): Message {
        return copy(status = newStatus)
    }
}

/**
 * Represents a chat room/conversation
 */
@Parcelize
data class ChatRoom(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val participants: List<String> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) : Parcelable {
    
    companion object {
        /**
         * Extracts room ID from server response
         * Handles different possible response formats:
         * 1. Plain string: "B12VU94G91"
         * 2. JSON object: {"roomId": "B12VU94G91"}
         * 3. Nested JSON: {"data": {"roomId": "B12VU94G91"}}
         */
        fun extractRoomIdFromResponse(
            data: Any, 
            parameterConfig: com.example.chatmodule.config.ParameterConfiguration? = null
        ): String? {
            return try {
                android.util.Log.d("ChatRoom", "🔄 Extracting room ID from data: $data (type: ${data::class.java.simpleName})")
                
                // Case 1: Plain string room ID
                if (data is String && data.isNotBlank()) {
                    android.util.Log.d("ChatRoom", "✅ Found plain string room ID: $data")
                    return data.trim()
                }
                
                // Case 2: Already a JSONObject
                val jsonData = if (data is org.json.JSONObject) {
                    data
                } else {
                    // Try to parse as JSON, but handle the case where it's just a string
                    val dataString = data.toString().trim()
                    
                    // If it doesn't start with { or [, it's probably a plain string
                    if (!dataString.startsWith("{") && !dataString.startsWith("[")) {
                        android.util.Log.d("ChatRoom", "✅ Found plain string room ID: $dataString")
                        return dataString
                    }
                    
                    // Try to parse as JSON
                    try {
                        org.json.JSONObject(dataString)
                    } catch (e: org.json.JSONException) {
                        android.util.Log.w("ChatRoom", "⚠️ Failed to parse as JSON, treating as plain string: $dataString")
                        return dataString
                    }
                }
                
                // Case 3: JSON object with room ID fields
                android.util.Log.d("ChatRoom", "🔍 Searching JSON object for room ID fields...")
                
                // Use configurable parameter names
                val config = parameterConfig ?: com.example.chatmodule.config.ParameterConfiguration()
                val roomIdField = config.parameterNames["roomId"] ?: "roomId"
                
                // Try direct field access first
                if (jsonData.has(roomIdField)) {
                    val roomId = jsonData.getString(roomIdField)
                    android.util.Log.d("ChatRoom", "✅ Found room ID using field: $roomIdField")
                    return roomId
                }
                
                // Try nested fields like "data", "result", etc.
                val nestedFields = listOf("data", "result", "response")
                for (nestedField in nestedFields) {
                    if (jsonData.has(nestedField)) {
                        val nestedObj = jsonData.getJSONObject(nestedField)
                        if (nestedObj.has(roomIdField)) {
                            val roomId = nestedObj.getString(roomIdField)
                            android.util.Log.d("ChatRoom", "✅ Found room ID in nested field: $nestedField.$roomIdField")
                            return roomId
                        }
                    }
                }
                
                android.util.Log.w("ChatRoom", "⚠️ No room ID found in JSON object")
                null
                
            } catch (e: Exception) {
                android.util.Log.e("ChatRoom", "❌ Error extracting room ID from response: ${e.message}", e)
                
                // Last resort: try to use the data as a string
                val fallbackString = data.toString().trim()
                if (fallbackString.isNotBlank() && !fallbackString.startsWith("{")) {
                    android.util.Log.d("ChatRoom", "🔄 Using fallback string as room ID: $fallbackString")
                    return fallbackString
                }
                
                null
            }
        }
        
        /**
         * Creates a ChatRoom from server response data
         */
        fun fromServerResponse(data: Any): ChatRoom? {
            return try {
                val roomId = extractRoomIdFromResponse(data)
                if (roomId != null) {
                    ChatRoom(
                        id = roomId,
                        name = "Chat Room",
                        description = "Created from server response",
                        createdAt = System.currentTimeMillis()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatRoom", "Error creating ChatRoom from server response", e)
                null
            }
        }
    }
}

/**
 * Represents a chat participant/user
 */
@Parcelize
data class User(
    val id: String,
    val name: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val metadata: Map<String, String> = emptyMap()
) : Parcelable 
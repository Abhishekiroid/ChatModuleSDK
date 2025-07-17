package com.example.chatmodule.data

import android.content.Context
// Room imports temporarily disabled due to KAPT compatibility issues
// import androidx.room.ColumnInfo
// import androidx.room.Dao
// import androidx.room.Database
// import androidx.room.Entity
// import androidx.room.Insert
// import androidx.room.OnConflictStrategy
// import androidx.room.PrimaryKey
// import androidx.room.Query
// import androidx.room.Room
// import androidx.room.RoomDatabase
// import androidx.room.TypeConverter
// import androidx.room.TypeConverters
// import androidx.room.Update
import com.example.chatmodule.config.ChatConfiguration
import com.example.chatmodule.model.Message
import com.example.chatmodule.model.MessageStatus
import com.example.chatmodule.model.MessageType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repository class for handling local data storage and offline message queuing
 * Note: Room database temporarily disabled due to KAPT compatibility issues
 */
class ChatRepository(
    context: Context,
    private val configuration: ChatConfiguration
) {
    
    // In-memory storage as temporary replacement for Room
    private val messages = mutableMapOf<String, MutableList<Message>>()
    private val offlineMessages = mutableListOf<OfflineMessageEntity>()
    
    /**
     * Inserts a message into the local storage
     */
    suspend fun insertMessage(message: Message) {
        val roomMessages = messages.getOrPut(message.chatRoomId) { mutableListOf() }
        roomMessages.removeAll { it.id == message.id } // Remove existing if any
        roomMessages.add(message)
        roomMessages.sortBy { it.timestamp } // Keep sorted by timestamp
    }
    
    /**
     * Updates a message in the local storage
     */
    suspend fun updateMessage(message: Message) {
        val roomMessages = messages[message.chatRoomId]
        roomMessages?.let { list ->
            val index = list.indexOfFirst { it.id == message.id }
            if (index != -1) {
                list[index] = message
            }
        }
    }
    
    /**
     * Gets all messages for a specific room
     */
    suspend fun getMessagesForRoom(roomId: String): List<Message> {
        return messages[roomId]?.toList() ?: emptyList()
    }
    
    /**
     * Gets a specific message by ID
     */
    suspend fun getMessage(messageId: String): Message? {
        return messages.values.flatten().find { it.id == messageId }
    }
    
    /**
     * Deletes a message
     */
    suspend fun deleteMessage(messageId: String) {
        messages.values.forEach { list ->
            list.removeAll { it.id == messageId }
        }
    }
    
    /**
     * Queues a message for offline sending
     */
    suspend fun queueOfflineMessage(message: Message, eventName: String) {
        if (!configuration.enableOfflineMessages) return
        
        val offlineMessage = OfflineMessageEntity(
            id = message.id,
            eventName = eventName,
            messageJson = Gson().toJson(message),
            timestamp = System.currentTimeMillis(),
            retryCount = 0
        )
        
        offlineMessages.removeAll { it.id == message.id } // Remove existing if any
        offlineMessages.add(offlineMessage)
    }
    
    /**
     * Gets all pending offline messages
     */
    suspend fun getPendingOfflineMessages(): List<OfflineMessageEntity> {
        return offlineMessages.toList()
    }
    
    /**
     * Removes an offline message after successful sending
     */
    suspend fun removeOfflineMessage(messageId: String) {
        offlineMessages.removeAll { it.id == messageId }
    }
    
    /**
     * Updates retry count for a failed offline message
     */
    suspend fun incrementRetryCount(messageId: String) {
        val message = offlineMessages.find { it.id == messageId }
        message?.let {
            val index = offlineMessages.indexOf(it)
            offlineMessages[index] = it.copy(retryCount = it.retryCount + 1)
        }
    }
    
    /**
     * Removes old offline messages that have exceeded retry limit
     */
    suspend fun cleanupFailedMessages(maxRetries: Int = 3) {
        offlineMessages.removeAll { it.retryCount >= maxRetries }
    }
}

/**
 * Entity for storing messages locally (Room temporarily disabled)
 */
// @Entity(tableName = "messages")
data class MessageEntity(
    // @PrimaryKey 
    val id: String,
    // @ColumnInfo(name = "sender_id") 
    val senderId: String,
    val receiverId: String,
    // @ColumnInfo(name = "sender_name")
    val senderName: String,
    // @ColumnInfo(name = "content") 
    val content: String,
    // @ColumnInfo(name = "type") 
    val type: Int,
    // @ColumnInfo(name = "timestamp") 
    val timestamp: Long,
    // @ColumnInfo(name = "status") 
    val status: String,
    // @ColumnInfo(name = "chat_room_id") 
    val chatRoomId: String,
    // @ColumnInfo(name = "file_name") 
    val fileName: String? = null,
    // @ColumnInfo(name = "file_size") 
    val fileSize: Long? = null,
    // @ColumnInfo(name = "file_url") 
    val fileUrl: String? = null,
    // @ColumnInfo(name = "mime_type") 
    val mimeType: String? = null,
    // @ColumnInfo(name = "thumbnail_url") 
    val thumbnailUrl: String? = null,
    // @ColumnInfo(name = "duration") 
    val duration: Long? = null,
    // @ColumnInfo(name = "reply_to_message_id") 
    val replyToMessageId: String? = null,
    // @ColumnInfo(name = "metadata") 
    val metadata: String? = null,
    // @ColumnInfo(name = "is_local") 
    val isLocal: Boolean = false,
    // @ColumnInfo(name = "local_file_path") 
    val localFilePath: String? = null
) {
    fun toMessage(): Message {
        val metadataMap: Map<String, String> = if (metadata != null) {
            Gson().fromJson(metadata, object : TypeToken<Map<String, String>>() {}.type)
        } else {
            emptyMap<String, String>()
        }
        
        return Message(
            id = id,
            senderId = senderId,
            senderName = senderName,
            receiverId = receiverId,
            content = content,
            type = MessageType.fromInt(type),
            timestamp = timestamp,
            status = MessageStatus.valueOf(status),
            chatRoomId = chatRoomId,
            fileName = fileName,
            fileSize = fileSize,
            fileUrl = fileUrl,
            mimeType = mimeType,
            thumbnailUrl = thumbnailUrl,
            duration = duration,
            replyToMessageId = replyToMessageId,
            metadata = metadataMap,
            isLocal = isLocal,
            localFilePath = localFilePath
        )
    }
}

/**
 * Entity for storing offline messages (Room temporarily disabled)
 */
// @Entity(tableName = "offline_messages")
data class OfflineMessageEntity(
    // @PrimaryKey 
    val id: String,
    // @ColumnInfo(name = "event_name") 
    val eventName: String,
    // @ColumnInfo(name = "message_json") 
    val messageJson: String,
    // @ColumnInfo(name = "timestamp") 
    val timestamp: Long,
    // @ColumnInfo(name = "retry_count") 
    val retryCount: Int = 0
)

/**
 * Extension function to convert Message to MessageEntity
 */
fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        senderName = senderName,
        content = content,
        type = type.value,
        timestamp = timestamp,
        status = status.name,
        chatRoomId = chatRoomId,
        fileName = fileName,
        fileSize = fileSize,
        fileUrl = fileUrl,
        mimeType = mimeType,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        replyToMessageId = replyToMessageId,
        metadata = if (metadata.isNotEmpty()) Gson().toJson(metadata) else null,
        isLocal = isLocal,
        localFilePath = localFilePath
    )
}

fun MessageEntity.toMessage(): Message {
    val metadataMap: Map<String, String> = if (metadata != null) {
        Gson().fromJson(metadata, object : TypeToken<Map<String, String>>() {}.type)
    } else {
        emptyMap<String, String>()
    }
    
    return Message(
        id = id,
        senderId = senderId,
        senderName = senderName,
        receiverId = receiverId,
        content = content,
        type = MessageType.fromInt(type),
        timestamp = timestamp,
        status = MessageStatus.valueOf(status),
        chatRoomId = chatRoomId,
        fileName = fileName,
        fileSize = fileSize,
        fileUrl = fileUrl,
        mimeType = mimeType,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        replyToMessageId = replyToMessageId,
        metadata = metadataMap,
        isLocal = isLocal,
        localFilePath = localFilePath
    )
}

/*
// Room DAO interfaces and Database class temporarily disabled due to KAPT compatibility issues

/**
 * DAO for message operations
 */
@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Query("SELECT * FROM messages WHERE chat_room_id = :roomId ORDER BY timestamp ASC")
    suspend fun getMessagesForRoom(roomId: String): List<MessageEntity>
    
    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessage(messageId: String): MessageEntity?
    
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    @Query("DELETE FROM messages WHERE chat_room_id = :roomId")
    suspend fun deleteMessagesForRoom(roomId: String)
}

/**
 * DAO for offline message operations
 */
@Dao
interface OfflineMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineMessage(message: OfflineMessageEntity)
    
    @Query("SELECT * FROM offline_messages ORDER BY timestamp ASC")
    suspend fun getPendingMessages(): List<OfflineMessageEntity>
    
    @Query("DELETE FROM offline_messages WHERE id = :messageId")
    suspend fun deleteOfflineMessage(messageId: String)
    
    @Query("UPDATE offline_messages SET retry_count = retry_count + 1 WHERE id = :messageId")
    suspend fun incrementRetryCount(messageId: String)
    
    @Query("DELETE FROM offline_messages WHERE retry_count >= :maxRetries")
    suspend fun deleteFailedMessages(maxRetries: Int)
}

/**
 * Type converters for Room database
 */
class Converters {
    @TypeConverter
    fun fromMap(value: Map<String, String>?): String? {
        return if (value == null) null else Gson().toJson(value)
    }
    
    @TypeConverter
    fun toMap(value: String?): Map<String, String>? {
        return if (value == null) null else {
            Gson().fromJson(value, object : TypeToken<Map<String, String>>() {}.type)
        }
    }
}

/**
 * Room database class
 */
@Database(
    entities = [MessageEntity::class, OfflineMessageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun offlineMessageDao(): OfflineMessageDao
    
    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null
        
        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
*/ 
package com.example.chatmodulesdk

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.chatmodule.ChatManager
import com.example.chatmodule.ui.xml.ChatFragment

class XmlChatActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "XmlChatActivity"
        const val EXTRA_CHAT_MANAGER = "extra_chat_manager"
        const val EXTRA_CHAT_ROOM_ID = "extra_chat_room_id"
        const val EXTRA_RECEIVER_ID = "extra_receiver_id" // For creating new conversations
    }
    
    private lateinit var chatManager: ChatManager
    private var actualRoomId: String? = null // 🏠 Store the real room ID from server
    private var chatFragment: ChatFragment? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xml_chat)
        
        // 🔧 FIX: Hide the default action bar to prevent double app bar
        // The fragment has its own toolbar
        supportActionBar?.hide()
        
        // Get ChatManager from provider
        chatManager = ChatManagerProvider.getChatManager()
        
        // Get parameters from intent
        val predefinedRoomId = intent.getStringExtra(EXTRA_CHAT_ROOM_ID)
        val receiverId = intent.getIntExtra(EXTRA_RECEIVER_ID, 120)
        
        Log.d(TAG, "🚀 Starting chat - Predefined Room: $predefinedRoomId, Receiver: $receiverId")
        
        if (predefinedRoomId != null) {
            // Use existing room ID
            Log.d(TAG, "📋 Using existing room ID: $predefinedRoomId")
            actualRoomId = predefinedRoomId
            setupChatFragment(actualRoomId!!)
        } else {
            // 🏠 Create new room and wait for server response
            createRoomAndWaitForResponse(receiverId)
        }
    }
    
    /**
     * 🏠 Creates a room and waits for server response with actual room ID
     * This is the CORRECT flow for room creation
     */
    private fun createRoomAndWaitForResponse(receiverId: Int) {
        Log.d(TAG, "🏠 Creating NEW room for conversation with receiver: $receiverId")
        
        // Setup room creation response listener FIRST
        chatManager.addEventListener("roomConnected") { data ->
            Log.d(TAG, "📥 Room creation response received: $data")
            handleRoomCreationResponse(data)
        }
        
        // Create room with proper user context
        chatManager.createRoom(
            senderID = chatManager.currentUser.value.id.toInt(), // Current user
            receiverID = receiverId // Target user for conversation
        )
        
        Log.d(TAG, "📤 Room creation request sent, waiting for server response...")
    }
    
    /**
     * 🏠 Handles the room creation response from server
     * Extracts the actual room ID and sets up the chat
     */
    private fun handleRoomCreationResponse(data: Any) {
        try {
            Log.d(TAG, "🔄 Processing room creation response...")
            
            // Use helper method to extract room ID with parameter configuration
            actualRoomId = com.example.chatmodule.model.ChatRoom.extractRoomIdFromResponse(
                data, 
                chatManager.getConfiguration().parameterConfig
            )
            
            if (actualRoomId != null) {
                Log.d(TAG, "✅ Room created successfully! Room ID: $actualRoomId")
                
                // Now setup the chat fragment with the REAL room ID from server
                runOnUiThread {
                    setupChatFragment(actualRoomId!!)
                }
                
                // Join the room to start receiving messages
                chatManager.joinRoom(actualRoomId!!)
                Log.d(TAG, "🚪 Joined room: $actualRoomId")
                
            } else {
                Log.e(TAG, "❌ No room ID found in server response")
                Log.e(TAG, "❌ Response data: $data")
                
                // Fallback to demo room ID
                actualRoomId = "demo_room_123"
                runOnUiThread {
                    setupChatFragment(actualRoomId!!)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing room creation response", e)
            
            // Fallback to demo room ID
            actualRoomId = "demo_room_123"
            runOnUiThread {
                setupChatFragment(actualRoomId!!)
            }
        }
    }
    
    private fun setupChatFragment(roomId: String) {
        Log.d(TAG, "📱 Setting up chat fragment with room ID: $roomId")
        
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            chatFragment = ChatFragment.newInstance(
                chatManager = chatManager,
                chatRoomId = roomId, // Use the ACTUAL room ID from server
                uiConfig = chatManager.getConfiguration().uiConfig
            )
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, chatFragment!!)
                .commit()
                
            Log.d(TAG, "✅ Chat fragment setup completed with room: $roomId")
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 
package com.example.chatmodulesdk

import com.example.chatmodule.ChatManager

/**
 * Simple provider to share ChatManager instance across activities
 * In a production app, consider using dependency injection (Dagger/Hilt) instead
 */
object ChatManagerProvider {
    private var chatManager: ChatManager? = null
    
    fun setChatManager(manager: ChatManager) {
        chatManager = manager
    }
    
    fun getChatManager(): ChatManager {
        return chatManager ?: throw IllegalStateException("ChatManager not initialized. Call setChatManager() first.")
    }
    
    fun isInitialized(): Boolean {
        return chatManager != null
    }
} 
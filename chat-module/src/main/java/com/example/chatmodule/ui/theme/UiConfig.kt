package com.example.chatmodule.ui.theme

/**
 * Simple UI configuration for the chat module
 * This is a simplified version to fix compilation issues
 */
data class UiConfig(
    val showTimestamps: Boolean = true,
    val showUserAvatars: Boolean = true,
    val showDeliveryStatus: Boolean = true,
    val enableMessageBubbles: Boolean = true,
    val messageMaxLines: Int = 5,
    val messageItemSpacing: Int = 8, // in dp
    val messagePadding: Int = 12 // in dp
)

/**
 * Legacy UIConfiguration alias for backward compatibility
 */
typealias UIConfiguration = UiConfig 
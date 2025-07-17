package com.example.chatmodule.config

/**
 * Data class for chat color configuration.
 * All colors are ARGB Ints (e.g., 0xFF2196F3)
 */
data class ChatColors(
    val primaryColor: Int = 0xFF2196F3.toInt(),
    val secondaryColor: Int = 0xFF03DAC5.toInt(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val surfaceColor: Int = 0xFFF5F5F5.toInt(),
    val textColor: Int = 0xFF1C1B1F.toInt(),
    val sendBubbleColor: Int = 0xFF4CAF50.toInt(),
    val receiveBubbleColor: Int = 0xFFE0E0E0.toInt()
) 
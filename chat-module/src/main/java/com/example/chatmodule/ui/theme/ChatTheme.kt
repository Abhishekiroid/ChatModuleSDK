package com.example.chatmodule.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.chatmodule.config.ChatColors
import com.example.chatmodule.config.ChatTheme as ChatThemeConfig

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2196F3),
    secondary = Color(0xFF03DAC5),
    tertiary = Color(0xFF4CAF50),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    secondary = Color(0xFF03DAC5),
    tertiary = Color(0xFF4CAF50),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

/**
 * Creates a color scheme based on custom chat colors
 */
@Composable
private fun createCustomColorScheme(
    chatColors: ChatColors,
    darkTheme: Boolean
): androidx.compose.material3.ColorScheme {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    return baseScheme.copy(
        primary = Color(chatColors.primaryColor),
        secondary = Color(chatColors.secondaryColor),
        background = Color(chatColors.backgroundColor),
        surface = Color(chatColors.surfaceColor),
        onBackground = Color(chatColors.textColor),
        onSurface = Color(chatColors.textColor)
    )
}

/**
 * Chat module theme composable
 */
@Composable
fun ChatModuleTheme(
    chatTheme: ChatThemeConfig = ChatThemeConfig.AUTO,
    customColors: ChatColors? = null,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (chatTheme) {
        ChatThemeConfig.LIGHT -> false
        ChatThemeConfig.DARK -> true
        ChatThemeConfig.AUTO -> isSystemInDarkTheme()
    }
    
    val colorScheme = when {
        customColors != null -> createCustomColorScheme(customColors, darkTheme)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChatTypography,
        content = content
    )
}

/**
 * Extension to get chat-specific colors from the current theme
 */
@Composable
fun getChatColors(): ChatColorPalette {
    val colorScheme = MaterialTheme.colorScheme
    
    return ChatColorPalette(
        sendBubbleColor = colorScheme.primary,
        receiveBubbleColor = colorScheme.surfaceVariant,
        sendTextColor = colorScheme.onPrimary,
        receiveTextColor = colorScheme.onSurfaceVariant,
        timestampColor = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        dividerColor = colorScheme.outline.copy(alpha = 0.3f),
        statusIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        linkColor = colorScheme.primary,
        errorColor = colorScheme.error,
        successColor = Color(0xFF4CAF50)
    )
}

/**
 * Chat-specific color palette
 */
data class ChatColorPalette(
    val sendBubbleColor: Color,
    val receiveBubbleColor: Color,
    val sendTextColor: Color,
    val receiveTextColor: Color,
    val timestampColor: Color,
    val dividerColor: Color,
    val statusIconColor: Color,
    val linkColor: Color,
    val errorColor: Color,
    val successColor: Color
) 
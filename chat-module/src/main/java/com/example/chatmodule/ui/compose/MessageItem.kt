package com.example.chatmodule.ui.compose

import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chatmodule.ui.theme.UIConfiguration
import com.example.chatmodule.model.Message
import com.example.chatmodule.model.MessageStatus
import com.example.chatmodule.model.MessageType
import com.example.chatmodule.ui.theme.ChatTextStyles
import com.example.chatmodule.ui.theme.getChatColors
import com.example.chatmodule.ui.theme.ChatColorPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Composable for displaying a single chat message
 */
@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean,
    showSenderInfo: Boolean = true,
    uiConfig: UIConfiguration,
    onMessageClick: ((Message) -> Unit)? = null,
    onMessageLongClick: ((Message) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val chatColors = getChatColors()
    val configuration = LocalConfiguration.current
    val maxBubbleWidth = (configuration.screenWidthDp.dp * 0.75f)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = uiConfig.messageItemSpacing.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser && uiConfig.showUserAvatars) {
            // Sender avatar
            AsyncImage(
                model = null, // You can implement avatar loading here
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = maxBubbleWidth)
        ) {
            // Sender name (only for received messages if enabled)
            if (!isCurrentUser && showSenderInfo) {
                Text(
                    text = message.senderName,
                    style = ChatTextStyles.senderName,
                    color = chatColors.receiveTextColor,
                    modifier = Modifier.padding(
                        start = if (uiConfig.enableMessageBubbles) 12.dp else 0.dp,
                        bottom = 2.dp
                    )
                )
            }
            
            // Message bubble
            MessageBubble(
                message = message,
                isCurrentUser = isCurrentUser,
                uiConfig = uiConfig,
                chatColors = chatColors,
                onMessageClick = onMessageClick,
                onMessageLongClick = onMessageLongClick
            )
            
            // Timestamp and status
            if (uiConfig.showTimestamps || uiConfig.showDeliveryStatus) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
                    modifier = Modifier.padding(
                        start = if (uiConfig.enableMessageBubbles) 12.dp else 0.dp,
                        top = 2.dp
                    )
                ) {
                    if (uiConfig.showTimestamps) {
                        Text(
                            text = formatMessageTime(message.timestamp),
                            style = ChatTextStyles.messageTimestamp,
                            color = chatColors.timestampColor
                        )
                    }
                    
                    if (isCurrentUser && uiConfig.showDeliveryStatus) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(
                            status = message.status,
                            color = chatColors.statusIconColor
                        )
                    }
                }
            }
        }
        
        if (isCurrentUser && uiConfig.showUserAvatars) {
            Spacer(modifier = Modifier.width(8.dp))
            
            // Current user avatar
            AsyncImage(
                model = null, // You can implement avatar loading here
                contentDescription = "Your avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Message bubble component
 */
@Composable
private fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    uiConfig: UIConfiguration,
    chatColors: ChatColorPalette,
    onMessageClick: ((Message) -> Unit)?,
    onMessageLongClick: ((Message) -> Unit)?
) {
    val bubbleColor = if (isCurrentUser) {
        chatColors.sendBubbleColor
    } else {
        chatColors.receiveBubbleColor
    }
    
    val textColor = if (isCurrentUser) {
        chatColors.sendTextColor
    } else {
        chatColors.receiveTextColor
    }
    
    val bubbleShape = if (uiConfig.enableMessageBubbles) {
        if (isCurrentUser) {
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 4.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        }
    } else {
        RoundedCornerShape(8.dp)
    }
    
    Box(
        modifier = Modifier
            .background(
                color = bubbleColor,
                shape = bubbleShape
            )
            .padding(uiConfig.messagePadding.dp)
    ) {
        when (message.type) {
            MessageType.TEXT -> {
                Text(
                    text = message.content,
                    style = ChatTextStyles.messageText,
                    color = textColor,
                    maxLines = uiConfig.messageMaxLines
                )
            }
            
            MessageType.IMAGE -> {
                ImageMessageContent(
                    message = message,
                    textColor = textColor
                )
            }
            
            MessageType.FILE -> {
                FileMessageContent(
                    message = message,
                    textColor = textColor
                )
            }
            
            MessageType.AUDIO -> {
                AudioMessageContent(
                    message = message,
                    textColor = textColor
                )
            }
            
            MessageType.SYSTEM -> {
                Text(
                    text = message.content,
                    style = ChatTextStyles.messageText.copy(fontWeight = FontWeight.Medium),
                    color = textColor
                )
            }

            MessageType.VIDEO -> {
                VideoMessageContent(
                    message = message,
                    textColor = textColor
                )
            }
        }
    }
}

/**
 * Image message content
 */
@Composable
private fun ImageMessageContent(
    message: Message,
    textColor: Color
) {
    Column {
        AsyncImage(
            model = message.thumbnailUrl ?: message.fileUrl,
            contentDescription = "Image message",
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        if (message.content.isNotEmpty() && message.content != "Image") {
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = message.content,
                style = ChatTextStyles.messageText,
                color = textColor
            )
        }
    }
}

/**
 * File message content
 */
@Composable
private fun FileMessageContent(
    message: Message,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Done, // Use appropriate file icon
            contentDescription = "File",
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = message.fileName ?: "File",
                style = ChatTextStyles.messageText,
                color = textColor
            )
            
            message.fileSize?.let { size ->
                Text(
                    text = formatFileSize(size),
                    style = ChatTextStyles.fileInfo,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Audio message content
 */
@Composable
private fun AudioMessageContent(
    message: Message,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Done, // Use appropriate audio icon
            contentDescription = "Audio",
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = "Audio message",
                style = ChatTextStyles.messageText,
                color = textColor
            )
            
            message.duration?.let { duration ->
                Text(
                    text = formatDuration(duration),
                    style = ChatTextStyles.fileInfo,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Video message content component
 */
@Composable
private fun VideoMessageContent(
    message: Message,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 240.dp)
    ) {
        // Video thumbnail with play button overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Thumbnail
            AsyncImage(
                model = message.thumbnailUrl ?: message.fileUrl,
                contentDescription = "Video thumbnail",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop
            )
            
            // Play button overlay
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Video duration and size info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Duration
            message.duration?.let { duration ->
                Text(
                    text = formatDuration(duration),
                    style = ChatTextStyles.fileInfo,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            
            // File size
            message.fileSize?.let { size ->
                Text(
                    text = formatFileSize(size),
                    style = ChatTextStyles.fileInfo,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Message status icon
 */
@Composable
private fun MessageStatusIcon(
    status: MessageStatus,
    color: Color
) {
    val icon = when (status) {
        MessageStatus.SENDING -> Icons.Default.Refresh
        MessageStatus.SENT -> Icons.Default.Check
        MessageStatus.DELIVERED -> Icons.Default.Done
        MessageStatus.READ -> Icons.Default.Done
        MessageStatus.FAILED -> Icons.Default.Warning
    }
    
    val iconColor = when (status) {
        MessageStatus.SENDING -> color
        MessageStatus.SENT -> color
        MessageStatus.DELIVERED -> color
        MessageStatus.READ -> MaterialTheme.colorScheme.primary
        MessageStatus.FAILED -> MaterialTheme.colorScheme.error
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = iconColor,
        modifier = Modifier.size(12.dp)
    )
}

/**
 * Utility functions
 */
private fun formatMessageTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = durationMs / (1000 * 60 * 60)
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
} 
package com.example.chatmodule.ui.compose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chatmodule.ChatManager
import com.example.chatmodule.ui.theme.UIConfiguration
import com.example.chatmodule.model.Message
import com.example.chatmodule.network.ConnectionState
import com.example.chatmodule.ui.theme.ChatTextStyles
import com.example.chatmodule.ui.theme.getChatColors

/**
 * Main chat screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatManager: ChatManager,
    chatRoomId: String,
    uiConfig: UIConfiguration,
    onBackClick: (() -> Unit)? = null,
    onAttachmentClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val messages by chatManager.messages.collectAsState()
    val currentUser by chatManager.currentUser.collectAsState()
    val connectionState by chatManager.connectionState.collectAsState()
    
    val chatColors = getChatColors()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            // This block runs every time a new message is added
            val latestMessage = messages.last()
            // Do something, e.g.:
            Log.d("MainActivity", "New message received: ${latestMessage.content}")
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ChatTopBar(
                title = "Chat", // You can pass room name here
                subtitle = getConnectionStatusText(connectionState),
                onBackClick = onBackClick,
                chatColors = chatColors
            )
        },
        bottomBar = {
            MessageInputBar(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = { text ->
                    if (text.isNotBlank()) {
                        chatManager.sendMessage(text, chatRoomId)
                        messageText = TextFieldValue("")
                        keyboardController?.hide()
                    }
                },
                onAttachmentClick = onAttachmentClick,
                uiConfig = uiConfig,
                chatColors = chatColors
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (messages.isEmpty()) {
                EmptyStateMessage()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = false
                ) {
                    items(
                        items = messages.filter { it.chatRoomId == chatRoomId },
                        key = { it.id }
                    ) { message ->
                        MessageItem(
                            message = message,
                            isCurrentUser = message.senderId == currentUser.id,
                            showSenderInfo = true, // You can implement logic to group consecutive messages
                            uiConfig = uiConfig,
                            onMessageClick = { /* Handle message click */ },
                            onMessageLongClick = { /* Handle message long click */ }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chat top app bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    chatColors: com.example.chatmodule.ui.theme.ChatColorPalette
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = ChatTextStyles.chatTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = ChatTextStyles.statusText,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            onBackClick?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            // You can add more actions here (video call, voice call, etc.)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Message input bar at the bottom
 */
@Composable
private fun MessageInputBar(
    messageText: TextFieldValue,
    onMessageTextChange: (TextFieldValue) -> Unit,
    onSendMessage: (String) -> Unit,
    onAttachmentClick: (() -> Unit)?,
    uiConfig: UIConfiguration,
    chatColors: com.example.chatmodule.ui.theme.ChatColorPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .imePadding(),
        verticalAlignment = Alignment.Bottom
    ) {
        // Attachment button
        onAttachmentClick?.let {
            IconButton(
                onClick = it,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach file",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Text input field
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            placeholder = {
                Text(
                    text = "Type a message...",
                    style = ChatTextStyles.inputHint,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            textStyle = ChatTextStyles.inputText,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { 
                    onSendMessage(messageText.text)
                }
            ),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            shape = RoundedCornerShape(24.dp),
            maxLines = if (uiConfig.messageMaxLines > 0) uiConfig.messageMaxLines else Int.MAX_VALUE
        )
        
        // Send button
        IconButton(
            onClick = { onSendMessage(messageText.text) },
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (messageText.text.isNotBlank()) chatColors.sendBubbleColor else Color.Gray,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send message",
                tint = Color.White
            )
        }
    }
}

/**
 * Empty state when no messages
 */
@Composable
private fun EmptyStateMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No messages yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Start a conversation by sending a message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Get connection status text
 */
private fun getConnectionStatusText(connectionState: ConnectionState): String {
    return when (connectionState) {
        ConnectionState.CONNECTED -> "Online"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.RECONNECTING -> "Reconnecting..."
        ConnectionState.DISCONNECTED -> "Offline"
        ConnectionState.NO_NETWORK -> "No internet"
        ConnectionState.ERROR -> "Connection error"
        ConnectionState.FAILED -> "Connection failed"
        ConnectionState.DISCONNECTING -> "Disconnecting..."
    }
} 
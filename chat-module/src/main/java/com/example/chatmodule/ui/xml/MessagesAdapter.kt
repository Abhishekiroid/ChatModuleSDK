package com.example.chatmodule.ui.xml

import android.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.chatmodule.ui.theme.UIConfiguration
import com.example.chatmodule.databinding.ItemMessageBinding
import com.example.chatmodule.model.Message
import com.example.chatmodule.model.MessageStatus
import com.example.chatmodule.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying chat messages
 */
class MessagesAdapter(
    private val currentUserId: String,
    private val uiConfig: UIConfiguration,
    private val onMessageClick: (Message) -> Unit,
    private val onMessageLongClick: (Message) -> Unit
) : ListAdapter<Message, MessagesAdapter.MessageViewHolder>(MessageDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: Message) {
            val isCurrentUser = message.senderId == currentUserId
            
            // Set click listeners
            binding.root.setOnClickListener { onMessageClick(message) }
            binding.root.setOnLongClickListener { 
                onMessageLongClick(message)
                true
            }
            
            // Configure layout for sent/received messages
            configureMessageLayout(message, isCurrentUser)
            
            // Set message content based on type
            setMessageContent(message)
            
            // Set timestamp
            if (uiConfig.showTimestamps) {
                binding.textViewTimestamp.text = formatMessageTime(message.timestamp)
                binding.textViewTimestamp.visibility = View.VISIBLE
            } else {
                binding.textViewTimestamp.visibility = View.GONE
            }
            
            // Set message status for sent messages
            if (isCurrentUser && uiConfig.showDeliveryStatus) {
                setMessageStatus(message.status)
                binding.imageViewMessageStatus.visibility = View.VISIBLE
            } else {
                binding.imageViewMessageStatus.visibility = View.GONE
            }
        }
        
        private fun configureMessageLayout(message: Message, isCurrentUser: Boolean) {
            val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            
            if (isCurrentUser) {
                // Sent message - align to right
                binding.imageViewSenderAvatar.visibility = View.GONE
                binding.imageViewCurrentUserAvatar.visibility = if (uiConfig.showUserAvatars) View.VISIBLE else View.GONE
                binding.textViewSenderName.visibility = View.GONE
                
                // Set bubble alignment to end
                val bubbleLayoutParams = binding.cardViewMessageBubble.layoutParams as ViewGroup.LayoutParams
                binding.cardViewMessageBubble.layoutParams = bubbleLayoutParams
                
                // Set timestamp alignment
                val timestampLayoutParams = binding.textViewTimestamp.layoutParams as ViewGroup.MarginLayoutParams
                timestampLayoutParams.marginStart = 0
                timestampLayoutParams.marginEnd = 12
                
                // Set bubble color for sent messages
                binding.cardViewMessageBubble.setCardBackgroundColor(
                    binding.root.context.getColor(android.R.color.holo_blue_light)
                )
                
                layoutParams.marginStart = 64
                layoutParams.marginEnd = 16
            } else {
                // Received message - align to left
                binding.imageViewSenderAvatar.visibility = if (uiConfig.showUserAvatars) View.VISIBLE else View.GONE
                binding.imageViewCurrentUserAvatar.visibility = View.GONE
                binding.textViewSenderName.visibility = View.VISIBLE
                binding.textViewSenderName.text = message.senderName
                
                // Set bubble color for received messages
                binding.cardViewMessageBubble.setCardBackgroundColor(
                    binding.root.context.getColor(android.R.color.darker_gray)
                )
                
                layoutParams.marginStart = 16
                layoutParams.marginEnd = 64
            }
            
            binding.root.layoutParams = layoutParams
        }
        
        private fun setMessageContent(message: Message) {
            // Hide all content views first
            binding.textViewMessageContent.visibility = View.GONE
            binding.imageViewMessageImage.visibility = View.GONE
            binding.layoutFileMessage.visibility = View.GONE
            binding.layoutAudioMessage.visibility = View.GONE
            
            when (message.type) {
                MessageType.TEXT -> {
                    binding.textViewMessageContent.visibility = View.VISIBLE
                    binding.textViewMessageContent.text = message.content
                }
                
                MessageType.IMAGE -> {
                    binding.imageViewMessageImage.visibility = View.VISIBLE
                    binding.imageViewMessageImage.load(message.thumbnailUrl ?: message.fileUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_menu_gallery)
                        error(R.drawable.ic_menu_close_clear_cancel)
                    }
                    
                    if (message.content.isNotEmpty() && message.content != "Image") {
                        binding.textViewMessageContent.visibility = View.VISIBLE
                        binding.textViewMessageContent.text = message.content
                    }
                }
                
                MessageType.FILE -> {
                    binding.layoutFileMessage.visibility = View.VISIBLE
                    binding.textViewFileName.text = message.fileName ?: "File"
                    
                    message.fileSize?.let { size ->
                        binding.textViewFileSize.text = formatFileSize(size)
                    }
                }
                
                MessageType.AUDIO -> {
                    binding.layoutAudioMessage.visibility = View.VISIBLE
                    
                    message.duration?.let { duration ->
                        binding.textViewAudioDuration.text = formatDuration(duration)
                    }
                }
                
                MessageType.SYSTEM -> {
                    binding.textViewMessageContent.visibility = View.VISIBLE
                    binding.textViewMessageContent.text = message.content
                }

                MessageType.VIDEO -> {
                    binding.imageViewMessageImage.visibility = View.VISIBLE
                    binding.imageViewMessageImage.load(message.thumbnailUrl ?: message.fileUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_menu_gallery)
                        error(R.drawable.ic_menu_close_clear_cancel)
                    }

                    if (message.content.isNotEmpty() && message.content != "Video") {
                        binding.textViewMessageContent.visibility = View.VISIBLE
                        binding.textViewMessageContent.text = message.content
                    }
                }
            }
        }
        
        private fun setMessageStatus(status: MessageStatus) {
            val statusIcon = when (status) {
                MessageStatus.SENDING -> android.R.drawable.ic_media_play
                MessageStatus.SENT -> android.R.drawable.checkbox_on_background
                MessageStatus.DELIVERED -> android.R.drawable.checkbox_on_background
                MessageStatus.READ -> android.R.drawable.checkbox_on_background
                MessageStatus.FAILED -> android.R.drawable.ic_delete
            }
            
            binding.imageViewMessageStatus.setImageResource(statusIcon)
            
            // Set tint based on status
            val tint = when (status) {
                MessageStatus.READ -> binding.root.context.getColor(android.R.color.holo_blue_light)
                MessageStatus.FAILED -> binding.root.context.getColor(android.R.color.holo_red_dark)
                else -> binding.root.context.getColor(android.R.color.darker_gray)
            }
            
            binding.imageViewMessageStatus.setColorFilter(tint)
        }
    }
    
    private fun formatMessageTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
    
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.1f %s".format(size, units[unitIndex])
    }
    
    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }
}

/**
 * DiffUtil callback for efficient list updates
 */
class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
} 
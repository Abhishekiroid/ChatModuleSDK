package com.example.chatmodule.ui.xml

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatmodule.ChatManager
import com.example.chatmodule.R
import com.example.chatmodule.ui.theme.UIConfiguration
import com.example.chatmodule.databinding.FragmentChatBinding
import com.example.chatmodule.model.Message
import kotlinx.coroutines.launch

/**
 * Fragment for displaying chat interface using XML layouts
 */
class ChatFragment : Fragment() {
    
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var chatManager: ChatManager
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var chatRoomId: String
    private lateinit var uiConfig: UIConfiguration
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupInputHandlers()
        observeData()
    }
    
    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(
            currentUserId = chatManager.currentUser.value.id,
            uiConfig = uiConfig,
            onMessageClick = { message ->
                // Handle message click
            },
            onMessageLongClick = { message ->
                // Handle message long click
            }
        )
        
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = messagesAdapter
        }
    }
    
    private fun setupInputHandlers() {
        binding.editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
        
        binding.fabSend.setOnClickListener {
            sendMessage()
        }
        
        binding.buttonAttachment.setOnClickListener {
            // Handle attachment click
            // You can open file picker, camera, etc.
        }
        
        binding.toolbar.setNavigationOnClickListener {
            // Handle back navigation using modern OnBackPressedDispatcher
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            chatManager.messages.collect { messages ->
                val filteredMessages = messages.filter { it.chatRoomId == chatRoomId }
                messagesAdapter.submitList(filteredMessages)
                
                // Update empty state visibility
                if (filteredMessages.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewMessages.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.recyclerViewMessages.visibility = View.VISIBLE
                    
                    // Scroll to bottom
                    binding.recyclerViewMessages.scrollToPosition(filteredMessages.size - 1)
                }
                // This block runs every time the message list changes
                val latestMessage = messages.lastOrNull()
                if (latestMessage != null) {
                    Log.d("MainActivity", "New message received: ${latestMessage.content}")
                    // Do something
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            chatManager.connectionState.collect { connectionState ->
                // Update connection status in toolbar subtitle
                binding.toolbar.subtitle = when (connectionState) {
                    com.example.chatmodule.network.ConnectionState.CONNECTED -> "Online"
                    com.example.chatmodule.network.ConnectionState.CONNECTING -> "Connecting..."
                    com.example.chatmodule.network.ConnectionState.RECONNECTING -> "Reconnecting..."
                    com.example.chatmodule.network.ConnectionState.DISCONNECTED -> "Offline"
                    com.example.chatmodule.network.ConnectionState.NO_NETWORK -> "No internet"
                    com.example.chatmodule.network.ConnectionState.ERROR -> "Connection error"
                    com.example.chatmodule.network.ConnectionState.FAILED -> "Connection failed"
                    com.example.chatmodule.network.ConnectionState.DISCONNECTING -> "Disconnecting..."
                }
            }
        }
    }
    
    private fun sendMessage() {
        val messageText = binding.editTextMessage.text?.toString()?.trim()
        if (!messageText.isNullOrEmpty()) {
            chatManager.sendMessage(messageText, chatRoomId)
            binding.editTextMessage.text?.clear()
        }
    }
    
    fun setChatManager(chatManager: ChatManager) {
        this.chatManager = chatManager
    }
    
    fun setChatRoomId(roomId: String) {
        this.chatRoomId = roomId
    }
    
    fun setUIConfiguration(config: UIConfiguration) {
        this.uiConfig = config
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(
            chatManager: ChatManager,
            chatRoomId: String,
            uiConfig: UIConfiguration
        ): ChatFragment {
            return ChatFragment().apply {
                setChatManager(chatManager)
                setChatRoomId(chatRoomId)
                setUIConfiguration(uiConfig)
            }
        }
    }
} 
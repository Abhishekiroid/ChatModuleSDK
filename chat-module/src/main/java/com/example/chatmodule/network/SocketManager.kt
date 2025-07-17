package com.example.chatmodule.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.chatmodule.config.ChatConfiguration
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.Polling
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.URISyntaxException
import com.google.gson.Gson

/**
 * Manages socket connection and handles reconnection logic
 */
class SocketManager(
    private val context: Context,
    private val configuration: ChatConfiguration
) {
    
    companion object {
        private const val TAG = "SocketManager"
    }
    
    private var socket: Socket? = null
    private var eventManager: EventManager? = null
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    /**
     * Initializes the socket connection
     */
    fun initialize(): EventManager {
        Log.d(TAG, "🔧 Initializing socket connection to: ${configuration.serverUrl}")
        
        if (socket != null) {
            Log.d(TAG, "♻️ Disconnecting existing socket before reinitializing")
            disconnect()
        }
        
        try {
            val options = IO.Options().apply {
                transports = arrayOf(WebSocket.NAME, Polling.NAME)
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 10000
                
                // Add authentication token if provided
                configuration.authToken?.let { token ->
                    auth = mapOf("token" to token)
                    Log.d(TAG, "🔐 Authentication token added to socket options")
                } ?: Log.d(TAG, "⚠️ No authentication token provided")
                
                // Apply socket options from configuration
                timeout = configuration.connectionTimeout
                reconnectionAttempts = configuration.reconnectionAttempts
                Log.d(TAG, "⚙️ Applied socket timeout: ${configuration.connectionTimeout}")
                Log.d(TAG, "⚙️ Applied reconnection attempts: ${configuration.reconnectionAttempts}")
            }
            
            socket = IO.socket(configuration.serverUrl, options)
            eventManager = EventManager(socket!!)
            
            setupDefaultListeners()
            
            Log.d(TAG, "✅ Socket initialized successfully")
            return eventManager!!
            
        } catch (e: URISyntaxException) {
            Log.e(TAG, "❌ Invalid socket URL: ${configuration.serverUrl}", e)
            throw IllegalArgumentException("Invalid socket URL: ${configuration.serverUrl}", e)
        }
    }
    
    /**
     * Connects to the socket server
     */
    fun connect() {
        Log.d(TAG, "🔌 Attempting to connect to socket server")
        
        if (!isNetworkAvailable()) {
            Log.w(TAG, "📵 No network available, cannot connect")
            _connectionState.value = ConnectionState.NO_NETWORK
            return
        }
        
        socket?.let { socket ->
            if (!socket.connected()) {
                Log.d(TAG, "🚀 Initiating socket connection...")
                _connectionState.value = ConnectionState.CONNECTING
                socket.connect()
            } else {
                Log.d(TAG, "✅ Socket already connected")
            }
        } ?: run {
            Log.e(TAG, "❌ Socket not initialized. Call initialize() first.")
            throw IllegalStateException("Socket not initialized. Call initialize() first.")
        }
    }
    
    /**
     * Disconnects from the socket server
     */
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting from socket server")
        socket?.let { socket ->
            _connectionState.value = ConnectionState.DISCONNECTING
            socket.disconnect()
            socket.off()
            Log.d(TAG, "✅ Socket disconnected and listeners removed")
        }
        eventManager?.removeAllListeners()
    }
    
    /**
     * Gets the current EventManager instance
     */
    fun getEventManager(): EventManager? {
        return eventManager
    }
    
    /**
     * Checks if socket is currently connected
     */
    fun isConnected(): Boolean {
        val connected = socket?.connected() == true
        Log.d(TAG, "🔍 Socket connection status: $connected")
        return connected
    }
    
    /**
     * Emits data to the server
     */
    fun emit(eventName: String, data: Any? = null) {
        Log.d(TAG, "📤 EMIT: $eventName")
        Log.d(TAG, "📤 EMIT Data: $data")
        
        socket?.let { socket ->
            if (socket.connected()) {
                val authenticatedData = createAuthenticatedPayload(data)
                Log.d(TAG, "📤 EMIT Authenticated Data: $authenticatedData")
                eventManager?.emit(eventName, authenticatedData)
                Log.d(TAG, "✅ Event '$eventName' emitted successfully")
            } else {
                Log.w(TAG, "⚠️ Socket not connected, cannot emit '$eventName'")
                // Queue message if offline messaging is enabled
                if (configuration.enableOfflineMessages) {
                    Log.d(TAG, "📥 Queuing offline message: $eventName")
                    queueOfflineMessage(eventName, data)
                }
            }
        } ?: Log.e(TAG, "❌ Socket is null, cannot emit '$eventName'")
    }
    
    /**
     * Creates an authenticated payload with token if available
     */
    private fun createAuthenticatedPayload(data: Any?): Any {
        configuration.authToken?.let { token ->
            Log.d(TAG, "🔐 Adding authentication token to payload")
            return when (data) {
                is JSONObject -> {
                    data.put("token", token)
                    Log.d(TAG, "🔐 Token added to JSONObject")
                    data
                }
                null -> JSONObject().apply { 
                    put("token", token)
                    Log.d(TAG, "🔐 Created new JSONObject with token")
                }
                else -> {
                    // Try to convert data to JSONObject and add token
                    try {
                        val jsonString = if (data is String) data else Gson().toJson(data)
                        val jsonObject = JSONObject(jsonString)
                        jsonObject.put("token", token)
                        Log.d(TAG, "🔐 Token added to converted JSONObject")
                        jsonObject
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to convert data to JSON, creating wrapper object", e)
                        // If conversion fails, create wrapper object
                        JSONObject().apply {
                            put("token", token)
                            put("data", data)
                        }
                    }
                }
            }
        } ?: run {
            Log.d(TAG, "ℹ️ No authentication token available")
            return data ?: JSONObject()
        }
    }
    
    /**
     * Updates network connectivity status
     */
    fun updateNetworkStatus() {
        val wasOnline = _isOnline.value
        val nowOnline = isNetworkAvailable()
        _isOnline.value = nowOnline
        
        Log.d(TAG, "📶 Network status: was $wasOnline, now $nowOnline")
        
        if (!wasOnline && nowOnline) {
            Log.d(TAG, "📶 Network restored, attempting to reconnect")
            // Network came back, try to reconnect
            if (_connectionState.value == ConnectionState.NO_NETWORK) {
                connect()
            }
        } else if (wasOnline && !nowOnline) {
            Log.w(TAG, "📵 Network lost")
            // Network lost
            _connectionState.value = ConnectionState.NO_NETWORK
        }
    }
    
    /**
     * Sets up default socket event listeners
     */
    private fun setupDefaultListeners() {
        Log.d(TAG, "🎧 Setting up default socket event listeners")
        
        socket?.let { socket ->
            socket.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "🎉 CONNECTED: Socket connected successfully")
                _connectionState.value = ConnectionState.CONNECTED
                sendPendingOfflineMessages()
            }
            
            socket.on(Socket.EVENT_DISCONNECT) { args ->
                val reason = if (args.isNotEmpty()) args[0].toString() else "unknown"
                Log.d(TAG, "🔌 DISCONNECTED: Reason = $reason")
                _connectionState.value = if (reason == "io server disconnect") {
                    ConnectionState.DISCONNECTED
                } else {
                    ConnectionState.RECONNECTING
                }
            }
            
            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = if (args.isNotEmpty()) args[0] else "Connection error"
                Log.e(TAG, "❌ CONNECTION ERROR: $error")
                _connectionState.value = ConnectionState.ERROR
            }
            
            socket.on("reconnect") {
                Log.d(TAG, "🔄 RECONNECTED: Socket reconnected successfully")
                _connectionState.value = ConnectionState.CONNECTED
                sendPendingOfflineMessages()
            }
            
            socket.on("reconnecting") {
                Log.d(TAG, "🔄 RECONNECTING: Attempting to reconnect...")
                _connectionState.value = ConnectionState.RECONNECTING
            }
            
            socket.on("reconnect_error") {
                Log.e(TAG, "❌ RECONNECT ERROR: Failed to reconnect")
                _connectionState.value = ConnectionState.ERROR
            }
            
            socket.on("reconnect_failed") {
                Log.e(TAG, "💀 RECONNECT FAILED: All reconnection attempts failed")
                _connectionState.value = ConnectionState.FAILED
            }
        }
        
        Log.d(TAG, "✅ Default socket listeners configured")
    }
    
    /**
     * Checks if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Queues a message for offline sending
     */
    private fun queueOfflineMessage(eventName: String, data: Any?) {
        // This would be implemented with the offline message queue system
        // For now, it's a placeholder
    }
    
    /**
     * Sends any pending offline messages
     */
    private fun sendPendingOfflineMessages() {
        // This would be implemented with the offline message queue system
        // For now, it's a placeholder
    }
}

/**
 * Represents the current connection state
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    RECONNECTING,
    ERROR,
    FAILED,
    NO_NETWORK
} 
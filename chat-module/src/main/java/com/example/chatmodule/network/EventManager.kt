package com.example.chatmodule.network

import android.util.Log
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

/**
 * Manages socket events and provides a flexible way to handle custom events
 */
class EventManager(private val socket: Socket) {
    
    companion object {
        private const val TAG = "EventManager"
    }
    
    private val _events = MutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()
    
    private val eventListeners = mutableMapOf<String, MutableList<(data: Any) -> Unit>>()
    private val registeredEvents = mutableSetOf<String>()
    
    /**
     * Registers a new event listener for the specified event name
     */
    fun addEventListener(eventName: String, listener: (data: Any) -> Unit) {
        Log.d(TAG, "🎧 REGISTER LISTENER: '$eventName'")
        
        if (!eventListeners.containsKey(eventName)) {
            eventListeners[eventName] = mutableListOf()
        }
        eventListeners[eventName]?.add(listener)
        
        // Register socket listener if not already registered
        if (!registeredEvents.contains(eventName)) {
            Log.d(TAG, "🎧 SOCKET LISTENER REGISTERED: '$eventName'")
            socket.on(eventName) { args ->
                val data = if (args.isNotEmpty()) args[0] else null
                Log.d(TAG, "📥 RECEIVED: '$eventName' with data: $data")
                handleEvent(eventName, data)
            }
            registeredEvents.add(eventName)
        }
        
        Log.d(TAG, "✅ Event listener added for '$eventName'. Total listeners: ${eventListeners[eventName]?.size}")
    }
    
    /**
     * Removes an event listener for the specified event name
     */
    fun removeEventListener(eventName: String, listener: (data: Any) -> Unit) {
        Log.d(TAG, "🗑️ REMOVE LISTENER: '$eventName'")
        
        eventListeners[eventName]?.remove(listener)
        
        // If no more listeners for this event, remove socket listener
        if (eventListeners[eventName]?.isEmpty() == true) {
            Log.d(TAG, "🗑️ SOCKET LISTENER REMOVED: '$eventName'")
            socket.off(eventName)
            registeredEvents.remove(eventName)
            eventListeners.remove(eventName)
        }
        
        Log.d(TAG, "✅ Event listener removed for '$eventName'. Remaining listeners: ${eventListeners[eventName]?.size ?: 0}")
    }
    
    /**
     * Removes all listeners for a specific event
     */
    fun removeEventListeners(eventName: String) {
        Log.d(TAG, "🗑️ REMOVE ALL LISTENERS: '$eventName'")
        
        socket.off(eventName)
        registeredEvents.remove(eventName)
        eventListeners.remove(eventName)
        
        Log.d(TAG, "✅ All listeners removed for '$eventName'")
    }
    
    /**
     * Removes all event listeners
     */
    fun removeAllListeners() {
        Log.d(TAG, "🗑️ REMOVE ALL LISTENERS")
        
        registeredEvents.forEach { eventName ->
            Log.d(TAG, "🗑️ Removing socket listener: '$eventName'")
            socket.off(eventName)
        }
        registeredEvents.clear()
        eventListeners.clear()
        
        Log.d(TAG, "✅ All event listeners removed")
    }
    
    /**
     * Emits an event to the server
     */
    fun emit(eventName: String, data: Any? = null) {
        Log.d(TAG, "📤 EMIT: '$eventName'")
        Log.d(TAG, "📤 EMIT Data: $data")
        
        if (data != null) {
            socket.emit(eventName, data)
            Log.d(TAG, "✅ Event '$eventName' emitted with data")
        } else {
            socket.emit(eventName)
            Log.d(TAG, "✅ Event '$eventName' emitted without data")
        }
    }
    
    /**
     * Emits an event with acknowledgment callback
     */
    fun emitWithAck(eventName: String, data: Any?, callback: (args: Array<Any>) -> Unit) {
        Log.d(TAG, "📤 EMIT WITH ACK: '$eventName'")
        Log.d(TAG, "📤 EMIT WITH ACK Data: $data")
        
        if (data != null) {
            socket.emit(eventName, data, callback)
            Log.d(TAG, "✅ Event '$eventName' emitted with data and acknowledgment callback")
        } else {
            socket.emit(eventName, callback)
            Log.d(TAG, "✅ Event '$eventName' emitted without data but with acknowledgment callback")
        }
    }
    
    /**
     * Registers a one-time event listener
     */
    fun once(eventName: String, listener: (data: Any) -> Unit) {
        Log.d(TAG, "🎧 REGISTER ONCE LISTENER: '$eventName'")
        
        val onceListener: (data: Any) -> Unit = { data ->
            Log.d(TAG, "🎧 ONCE LISTENER TRIGGERED: '$eventName'")
            listener(data)
            removeEventListener(eventName, listener)
        }
        addEventListener(eventName, onceListener)
        
        Log.d(TAG, "✅ One-time listener registered for '$eventName'")
    }
    
    /**
     * Gets all registered event names
     */
    fun getRegisteredEvents(): Set<String> {
        val events = registeredEvents.toSet()
        Log.d(TAG, "📋 REGISTERED EVENTS: $events")
        return events
    }
    
    /**
     * Checks if an event is currently registered
     */
    fun isEventRegistered(eventName: String): Boolean {
        val isRegistered = registeredEvents.contains(eventName)
        Log.d(TAG, "🔍 CHECK EVENT REGISTERED: '$eventName' = $isRegistered")
        return isRegistered
    }
    
    /**
     * Handles incoming events and notifies listeners
     */
    private fun handleEvent(eventName: String, data: Any?) {
        Log.d(TAG, "🔄 HANDLE EVENT: '$eventName'")
        Log.d(TAG, "🔄 HANDLE EVENT Data: $data")
        
        // Emit to Flow for reactive handling
        _events.tryEmit(ChatEvent(eventName, data))
        Log.d(TAG, "🔄 Event emitted to Flow")
        
        // Notify registered listeners
        val listeners = eventListeners[eventName]
        Log.d(TAG, "🔄 Notifying ${listeners?.size ?: 0} listeners for '$eventName'")
        
        listeners?.forEach { listener ->
            try {
                listener(data ?: JSONObject())
                Log.d(TAG, "✅ Listener notified successfully for '$eventName'")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in listener for '$eventName'", e)
            }
        }
        
        Log.d(TAG, "✅ Event handling completed for '$eventName'")
    }
}

/**
 * Represents a chat event with its name and data
 */
data class ChatEvent(
    val eventName: String,
    val data: Any?
) 
# 🔧 Parameter Configuration Examples

This document shows how to customize the Chat Module SDK parameter names to match different server APIs and projects.

## 📋 Default Configuration

By default, the chat module uses these parameter names:

```kotlin
ParameterConfiguration(
    // Room-related parameters
    roomIdField = "chatRoomId",           // Field name for room ID in messages
    roomIdResponseField = "roomId",        // Field name for room ID in server responses
    senderIdField = "senderId",           // Field name for sender ID
    receiverIdField = "receiverId",       // Field name for receiver ID
    
    // Event names
    sendMessageEvent = "sendMessage",     // Event name for sending messages
    receiveMessageEvent = "newmessage",   // Event name for receiving messages
    createRoomEvent = "createRoom",       // Event name for creating rooms
    roomCreatedEvent = "roomConnected",   // Event name for room creation response
    
    // Authentication
    tokenField = "token",                 // Field name for authentication token
    
    // Message parameters
    messageContentField = "content",      // Field name for message content
    messageTypeField = "type",            // Field name for message type
    timestampField = "timestamp",         // Field name for timestamp
    senderNameField = "senderName"        // Field name for sender name
)
```

## 🎯 Example 1: Using "roomId" instead of "chatRoomId"

**Problem**: Your server expects `roomId` field instead of `chatRoomId`.

**Solution**:
```kotlin
val configuration = ChatConfiguration.Builder(
    serverUrl = "https://your-server.com",
    authToken = "your-jwt-token"
)
    .setCurrentUser("123", "John Doe")
    .setParameterNames(
        roomIdField = "roomId",              // ✅ Changed from "chatRoomId" to "roomId"
        roomIdResponseField = "roomId"       // ✅ Server response also uses "roomId"
    )
    .build()
```

**Result**: Messages will be sent as:
```json
{
  "token": "your-jwt-token",
  "roomId": "room_abc123",        // ✅ Uses "roomId" not "chatRoomId"
  "senderId": "123",
  "content": "Hello world",
  "type": "text",
  "timestamp": 1640995200000
}
```

## 🎯 Example 2: Different Event Names

**Problem**: Your server uses different event names.

**Solution**:
```kotlin
val configuration = ChatConfiguration.Builder(
    serverUrl = "https://your-server.com",
    authToken = "your-jwt-token"
)
    .setCurrentUser("123", "John Doe")
    .setParameterNames(
        sendMessageEvent = "message:send",        // ✅ Custom event name
        receiveMessageEvent = "message:receive",  // ✅ Custom event name
        createRoomEvent = "room:create",          // ✅ Custom event name
        roomCreatedEvent = "room:created"         // ✅ Custom event name
    )
    .build()
```

**Result**: The SDK will emit and listen to your custom event names.

## 🎯 Example 3: Different Field Names for Users

**Problem**: Your server uses `userId` and `userName` instead of `senderId` and `senderName`.

**Solution**:
```kotlin
val configuration = ChatConfiguration.Builder(
    serverUrl = "https://your-server.com",
    authToken = "your-jwt-token"
)
    .setCurrentUser("123", "John Doe")
    .setParameterConfig(
        ParameterConfiguration(
            senderIdField = "userId",         // ✅ Changed from "senderId"
            senderNameField = "userName",     // ✅ Changed from "senderName"
            receiverIdField = "targetUserId", // ✅ Custom receiver field
            roomIdField = "conversationId"    // ✅ Custom room field
        )
    )
    .build()
```

**Result**: Room creation will send:
```json
{
  "token": "your-jwt-token",
  "userId": 123,                    // ✅ Uses "userId" not "senderId"
  "targetUserId": 456,              // ✅ Uses "targetUserId" not "receiverId"
}
```

## 🎯 Example 4: Nested Response Parsing

**Problem**: Your server returns room ID in nested data structure.

**Your Server Response**:
```json
{
  "success": true,
  "data": {
    "roomId": "room_abc123",
    "participants": [123, 456]
  }
}
```

**Solution**:
```kotlin
val configuration = ChatConfiguration.Builder(
    serverUrl = "https://your-server.com", 
    authToken = "your-jwt-token"
)
    .setCurrentUser("123", "John Doe")
    .setParameterConfig(
        ParameterConfiguration(
            roomIdResponseField = "roomId",
            nestedDataFields = listOf("data", "result", "response"), // ✅ Will check nested fields
            alternativeRoomIdFields = listOf("roomId", "id", "room_id") // ✅ Multiple field names to try
        )
    )
    .build()
```

**Result**: The SDK will automatically extract `roomId` from `data.roomId`.

## 🎯 Example 5: Complete Custom Configuration

**Problem**: Your project has completely different naming conventions.

**Solution**:
```kotlin
val customConfig = ParameterConfiguration(
    // Room parameters
    roomIdField = "conversationId",
    roomIdResponseField = "conversation_id", 
    senderIdField = "authorId",
    receiverIdField = "recipientId",
    
    // Message parameters
    messageContentField = "text",
    messageTypeField = "messageType",
    timestampField = "createdAt",
    senderNameField = "authorName",
    
    // Event names
    sendMessageEvent = "chat:message:send",
    receiveMessageEvent = "chat:message:incoming",
    createRoomEvent = "chat:room:create",
    roomCreatedEvent = "chat:room:established",
    joinRoomEvent = "chat:room:join",
    leaveRoomEvent = "chat:room:leave",
    
    // Authentication
    tokenField = "authToken",
    
    // File parameters
    fileNameField = "filename",
    fileSizeField = "size",
    fileUrlField = "downloadUrl",
    
    // Response parsing
    alternativeRoomIdFields = listOf("conversation_id", "room_id", "id"),
    nestedDataFields = listOf("data", "payload", "result")
)

val configuration = ChatConfiguration.Builder(
    serverUrl = "https://your-server.com",
    authToken = "your-jwt-token"
)
    .setCurrentUser("123", "John Doe")
    .setParameterConfig(customConfig)  // ✅ Use complete custom configuration
    .build()
```

## 🎯 Example 6: Multiple Projects Configuration

**Scenario**: You have different projects with different APIs.

**Project A** (uses default fields):
```kotlin
// Project A - Gaming Chat App
val gamingConfig = ChatConfiguration.Builder(
    serverUrl = "https://gaming-api.com",
    authToken = gamingToken
)
    .setCurrentUser(playerId, playerName)
    // Uses default parameter names
    .build()
```

**Project B** (uses custom fields):
```kotlin
// Project B - Business Communication App  
val businessConfig = ChatConfiguration.Builder(
    serverUrl = "https://business-api.com",
    authToken = businessToken
)
    .setCurrentUser(employeeId, employeeName)
    .setParameterNames(
        roomIdField = "threadId",
        senderIdField = "employeeId",
        receiverIdField = "recipientId",
        sendMessageEvent = "thread:message",
        receiveMessageEvent = "thread:incoming"
    )
    .build()
```

**Project C** (uses completely different API):
```kotlin
// Project C - Support Ticket System
val supportConfig = ChatConfiguration.Builder(
    serverUrl = "https://support-api.com",
    authToken = supportToken
)
    .setCurrentUser(userId, userName)
    .setParameterNames(
        roomIdField = "ticketId",
        senderIdField = "agentId", 
        receiverIdField = "customerId",
        sendMessageEvent = "ticket:reply",
        receiveMessageEvent = "ticket:update",
        createRoomEvent = "ticket:create",
        roomCreatedEvent = "ticket:opened"
    )
    .build()
```

## 📝 Configuration Checklist

When integrating with a new server API, check these items:

### ✅ Required Fields
- [ ] `roomIdField` - What field name does your server expect for room/conversation ID?
- [ ] `senderIdField` - What field name for the message sender?
- [ ] `receiverIdField` - What field name for message recipient?
- [ ] `tokenField` - What field name for authentication token?

### ✅ Event Names  
- [ ] `sendMessageEvent` - What event name to emit when sending messages?
- [ ] `receiveMessageEvent` - What event name to listen for incoming messages?
- [ ] `createRoomEvent` - What event name to emit when creating rooms?
- [ ] `roomCreatedEvent` - What event name to listen for room creation response?

### ✅ Response Parsing
- [ ] `roomIdResponseField` - What field contains the room ID in server responses?
- [ ] `nestedDataFields` - Does your server wrap data in objects like `{data: {...}}`?
- [ ] `alternativeRoomIdFields` - What other field names might contain room IDs?

### ✅ Optional Fields
- [ ] `messageContentField` - Custom field name for message text content
- [ ] `messageTypeField` - Custom field name for message type
- [ ] `timestampField` - Custom field name for timestamps
- [ ] `fileNameField` - Custom field name for file names
- [ ] `fileUrlField` - Custom field name for file URLs

## 🚀 Quick Start Template

Copy and customize this template for your project:

```kotlin
val configuration = ChatConfiguration.Builder(
    serverUrl = "YOUR_SERVER_URL",
    authToken = "YOUR_JWT_TOKEN"
)
    .setCurrentUser("YOUR_USER_ID", "YOUR_USER_NAME")
    .setParameterNames(
        roomIdField = "YOUR_ROOM_FIELD",              // e.g., "roomId", "conversationId", "threadId"
        roomIdResponseField = "YOUR_RESPONSE_FIELD",   // e.g., "roomId", "id", "conversation_id"  
        senderIdField = "YOUR_SENDER_FIELD",          // e.g., "senderId", "userId", "authorId"
        receiverIdField = "YOUR_RECEIVER_FIELD",      // e.g., "receiverId", "targetId", "recipientId"
        sendMessageEvent = "YOUR_SEND_EVENT",         // e.g., "sendMessage", "message:send"
        receiveMessageEvent = "YOUR_RECEIVE_EVENT",   // e.g., "newMessage", "message:incoming"
        createRoomEvent = "YOUR_CREATE_EVENT",        // e.g., "createRoom", "room:create"
        roomCreatedEvent = "YOUR_CREATED_EVENT"       // e.g., "roomCreated", "room:established"
    )
    .build()

val chatManager = ChatManager.Builder(this, configuration).build()
chatManager.initialize()
chatManager.connect()
```

## 🔍 Debugging Configuration

Add these logs to verify your configuration:

```kotlin
Log.d("ChatConfig", "Room ID field: ${configuration.parameterConfig.roomIdField}")
Log.d("ChatConfig", "Send event: ${configuration.parameterConfig.sendMessageEvent}")
Log.d("ChatConfig", "Receive event: ${configuration.parameterConfig.receiveMessageEvent}")
Log.d("ChatConfig", "Create room event: ${configuration.parameterConfig.createRoomEvent}")
```

## 📞 Need Help?

If your server API doesn't fit these examples:

1. **Check your server documentation** for exact field names and event names
2. **Test with API tools** like Postman to see exact request/response format
3. **Use debugging logs** to see what data the SDK is sending/receiving
4. **Create a custom ParameterConfiguration** with your exact specifications

The Chat Module SDK is designed to be flexible and work with any WebSocket-based chat API! 🎯 
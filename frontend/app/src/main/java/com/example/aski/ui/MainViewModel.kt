package com.example.aski.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.aski.model.*

class MainViewModel : ViewModel() {
    var currentUser = mutableStateOf<User?>(null)
    
    // In-memory storage for users and their passwords
    private val registeredUsers = mutableListOf<Pair<User, String>>()
    
    private val _items = mutableStateListOf<Item>()
    val items: List<Item> get() = _items.filter { it.status == ItemStatus.AVAILABLE }

    // Chat management
    private val _chats = mutableStateListOf<Chat>()
    private val _messages = mutableStateListOf<Message>()

    init {
        // App starts empty, ready for real user data
    }

    fun signup(email: String, name: String, password: String): Boolean {
        if (registeredUsers.any { it.first.email == email }) return false
        
        val newUser = User(email = email, displayName = name)
        registeredUsers.add(newUser to password)
        currentUser.value = newUser
        return true
    }

    fun login(email: String, password: String): Boolean {
        val userPair = registeredUsers.find { it.first.email == email && it.second == password }
        return if (userPair != null) {
            currentUser.value = userPair.first
            true
        } else {
            false
        }
    }

    fun logout() {
        currentUser.value = null
    }

    fun addItem(title: String, description: String, categoryId: Int, condition: ItemCondition, imageUrl: String) {
        currentUser.value?.let { user ->
            val newItem = Item(
                ownerId = user.id,
                categoryId = categoryId,
                title = title,
                description = description,
                condition = condition,
                primaryImageUrl = imageUrl
            )
            _items.add(0, newItem) // Add to top of the feed
        }
    }
    
    fun updateItem(
        itemId: String,
        title: String,
        description: String,
        condition: ItemCondition,
        status: ItemStatus
    ) {
        val index = _items.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val updatedItem = _items[index].copy(
                title = title,
                description = description,
                condition = condition,
                status = status
            )
            _items[index] = updatedItem
        }
    }
    
    fun getItemById(itemId: String): Item? {
        return _items.find { it.id == itemId }
    }

    fun getItemsForCurrentUser(): List<Item> {
        val userId = currentUser.value?.id ?: return emptyList()
        return _items.filter { it.ownerId == userId }
    }

    // Chat Functions
    fun getOrCreateChat(itemId: String, ownerId: String): Chat? {
        val requesterId = currentUser.value?.id ?: return null
        if (requesterId == ownerId) return null // Can't chat with yourself

        var chat = _chats.find { it.itemId == itemId && it.requesterId == requesterId }
        if (chat == null) {
            chat = Chat(itemId = itemId, requesterId = requesterId, ownerId = ownerId)
            _chats.add(chat)
            
            // Send initial message
            val item = getItemById(itemId)
            sendMessage(chat.id, "I'm interested in '${item?.title}'")
        }
        return chat
    }

    fun sendMessage(chatId: String, content: String) {
        val senderId = currentUser.value?.id ?: return
        val message = Message(chatId = chatId, senderId = senderId, content = content)
        _messages.add(message)
        
        // Update last message timestamp in chat
        _chats.find { it.id == chatId }?.let { chat ->
            val updatedChat = chat.copy(lastMessageAt = System.currentTimeMillis())
            val index = _chats.indexOf(chat)
            _chats[index] = updatedChat
        }
    }

    fun getMessagesForChat(chatId: String): List<Message> {
        return _messages.filter { it.chatId == chatId }.sortedBy { it.createdAt }
    }

    fun getChatsForCurrentUser(): List<Chat> {
        val userId = currentUser.value?.id ?: return emptyList()
        return _chats.filter { it.requesterId == userId || it.ownerId == userId }
            .sortedByDescending { it.lastMessageAt }
    }
    
    fun getChatById(chatId: String): Chat? {
        return _chats.find { it.id == chatId }
    }

    fun getUserById(userId: String): User? {
        return registeredUsers.find { it.first.id == userId }?.first
    }
}

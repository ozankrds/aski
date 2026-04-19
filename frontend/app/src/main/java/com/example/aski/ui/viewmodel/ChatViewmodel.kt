package com.example.aski.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aski.model.Chat
import com.example.aski.model.Message
import com.example.aski.model.User
import com.example.aski.repository.AuthRepository
import com.example.aski.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repo: ChatRepository = ChatRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _otherUser = MutableStateFlow<User?>(null)
    val otherUser: StateFlow<User?> = _otherUser

    private val _userNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val userNames: StateFlow<Map<String, String>> = _userNames

    private var currentUserId: String = ""

    val totalUnread: StateFlow<Int> = _chats.map { chats ->
        chats.sumOf { it.unreadCounts[currentUserId] ?: 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun observeChats(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            repo.observeUserChats(userId).collect { _chats.value = it }
        }
    }

    fun observeMessages(chatId: String) {
        viewModelScope.launch {
            repo.observeMessages(chatId).collect { _messages.value = it }
        }
    }

    fun markAsRead(chatId: String, userId: String) {
        viewModelScope.launch { repo.markAsRead(chatId, userId) }
    }

    fun fetchOtherUser(userId: String) {
        viewModelScope.launch {
            _otherUser.value = authRepo.getUserById(userId)
        }
    }

    fun fetchUserName(userId: String) {
        if (_userNames.value.containsKey(userId)) return
        viewModelScope.launch {
            authRepo.getUserById(userId)?.name?.let { name ->
                _userNames.value = _userNames.value + (userId to name)
            }
        }
    }

    suspend fun getOrCreateChat(itemId: String, requesterId: String, ownerId: String, itemImageUrl: String = ""): Chat? =
        repo.getOrCreateChat(itemId, requesterId, ownerId, itemImageUrl).getOrNull()

    fun sendMessage(chatId: String, senderId: String, content: String) {
        viewModelScope.launch {
            val participants = _chats.value.find { it.id == chatId }?.participants ?: listOf(senderId)
            repo.sendMessage(chatId, senderId, content, participants)
        }
    }

    fun sendImage(chatId: String, senderId: String, imageUri: Uri) {
        viewModelScope.launch {
            val url = repo.uploadImage(imageUri).getOrNull() ?: return@launch
            val participants = _chats.value.find { it.id == chatId }?.participants ?: listOf(senderId)
            repo.sendMessage(chatId, senderId, "", participants, imageUrl = url)
        }
    }

    suspend fun getChat(chatId: String) = repo.getChat(chatId)
}

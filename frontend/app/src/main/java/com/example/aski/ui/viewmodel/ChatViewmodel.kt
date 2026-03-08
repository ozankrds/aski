package com.example.aski.ui.viewmodel

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

    fun observeChats(userId: String) {
        viewModelScope.launch {
            repo.observeUserChats(userId).collect { _chats.value = it }
        }
    }

    fun observeMessages(chatId: String) {
        viewModelScope.launch {
            repo.observeMessages(chatId).collect { _messages.value = it }
        }
    }

    fun fetchOtherUser(userId: String) {
        viewModelScope.launch {
            val user = authRepo.getUserById(userId)
            _otherUser.value = user
        }
    }

    fun fetchUserName(userId: String) {
        if (_userNames.value.containsKey(userId)) return
        viewModelScope.launch {
            val user = authRepo.getUserById(userId)
            user?.name?.let { name ->
                _userNames.value = _userNames.value + (userId to name)
            }
        }
    }

    suspend fun getOrCreateChat(itemId: String, requesterId: String, ownerId: String): Chat? =
        repo.getOrCreateChat(itemId, requesterId, ownerId).getOrNull()

    fun sendMessage(chatId: String, senderId: String, content: String) {
        viewModelScope.launch { repo.sendMessage(chatId, senderId, content) }
    }

    suspend fun getChat(chatId: String) = repo.getChat(chatId)
}
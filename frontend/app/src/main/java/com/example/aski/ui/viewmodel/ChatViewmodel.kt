package com.example.aski.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aski.model.Chat
import com.example.aski.model.Message
import com.example.aski.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

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

    suspend fun getOrCreateChat(itemId: String, requesterId: String, ownerId: String): Chat? =
        repo.getOrCreateChat(itemId, requesterId, ownerId).getOrNull()

    fun sendMessage(chatId: String, senderId: String, content: String) {
        viewModelScope.launch { repo.sendMessage(chatId, senderId, content) }
    }

    suspend fun getChat(chatId: String) = repo.getChat(chatId)
}
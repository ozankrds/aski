package com.example.aski.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val senderId: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

package com.example.aski.model

import java.util.UUID

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val requesterId: String,
    val ownerId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis()
)

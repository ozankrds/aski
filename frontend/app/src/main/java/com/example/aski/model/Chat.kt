package com.example.aski.model

data class Chat(
    val id: String = "",
    val itemId: String = "",
    val participants: List<String> = emptyList(), // [requesterId, ownerId]
    val requesterId: String = "",
    val lastMessage: String = "",               // preview, avoid extra query
    val lastMessageAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")
}
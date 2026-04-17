package com.example.aski.model

data class Chat(
    val id: String = "",
    val itemId: String = "",
    val participants: List<String> = emptyList(),
    val requesterId: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Long = System.currentTimeMillis(),
    val unreadCounts: Map<String, Int> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")
}
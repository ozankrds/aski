package com.example.aski.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")
}
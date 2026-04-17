package com.example.aski.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val favoriteIds: List<String> = emptyList(),
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")
}
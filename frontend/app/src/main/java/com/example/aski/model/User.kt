package com.example.aski.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val favoriteIds: List<String> = emptyList(),
    val fcmToken: String = "",
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val givenCount: Int = 0,
    val karmaPoints: Int = 0,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")
}
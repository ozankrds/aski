package com.example.aski.model

data class Rating(
    val itemId: String = "",
    val raterId: String = "",
    val targetUserId: String = "",
    val score: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this(itemId = "")
}
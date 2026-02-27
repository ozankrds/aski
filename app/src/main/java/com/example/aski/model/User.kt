package com.example.aski.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis()
)

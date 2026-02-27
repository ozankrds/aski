package com.example.aski.model

import java.util.UUID

enum class ItemCondition {
    NEW, USED_GOOD, USED_FAIR
}

enum class ItemStatus {
    AVAILABLE, RESERVED, GIVEN
}

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val categoryId: Int,
    val title: String,
    val description: String,
    val condition: ItemCondition,
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val primaryImageUrl: String,
    val createdAt: Long = System.currentTimeMillis()
)

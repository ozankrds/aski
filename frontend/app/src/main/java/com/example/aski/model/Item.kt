package com.example.aski.model

enum class ItemCondition { NEW, USED_GOOD, USED_FAIR }
enum class ItemStatus { AVAILABLE, RESERVED, GIVEN }

data class Item(
    val id: String = "",
    val ownerId: String = "",
    val categoryId: Int = 0,
    val title: String = "",
    val description: String = "",
    val condition: ItemCondition = ItemCondition.NEW,
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    // Firestore requires no-arg constructor
    constructor() : this(id = "")
}
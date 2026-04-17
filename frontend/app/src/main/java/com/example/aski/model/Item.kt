package com.example.aski.model

enum class ItemCondition { NEW, LIKE_NEW, USED_GOOD, USED_FAIR, FOR_PARTS }
enum class ItemStatus { AVAILABLE, RESERVED, GIVEN }

data class Item(
    val id: String = "",
    val ownerId: String = "",
    val categoryId: Int = 0,
    val title: String = "",
    val description: String = "",
    val condition: ItemCondition = ItemCondition.NEW,
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrls: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")
}
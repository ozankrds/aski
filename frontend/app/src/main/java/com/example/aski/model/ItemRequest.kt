package com.example.aski.model

enum class RequestStatus { PENDING, ACCEPTED, REJECTED, COMPLETED }

data class ItemRequest(
    val id: String = "",
    val itemId: String = "",
    val itemTitle: String = "",
    val itemImageUrl: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")
}

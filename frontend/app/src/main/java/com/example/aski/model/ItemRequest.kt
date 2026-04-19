package com.example.aski.model

enum class RequestStatus { PENDING, ACCEPTED, REJECTED, COMPLETED }
enum class DeliveryMethod { HAND_TO_HAND, CARGO }
enum class DeliveryStatus { NONE, PREPARING, SHIPPED, DELIVERED }

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
    val deliveryMethod: DeliveryMethod? = null,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.NONE,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")
}

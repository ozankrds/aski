package com.example.aski.repository

import android.net.Uri
import com.example.aski.firebase.FirebaseManager
import com.example.aski.model.DeliveryMethod
import com.example.aski.model.DeliveryStatus
import com.example.aski.model.Item
import com.example.aski.model.ItemStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ItemRepository(
    private val db: FirebaseFirestore = FirebaseManager.firestore
) {
    private val col = db.collection("items")

    fun observeFeedItems(): Flow<List<Item>> = callbackFlow {
        val listener = col
            .whereEqualTo("status", ItemStatus.AVAILABLE.name)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val sorted = snap?.toObjects(Item::class.java)
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    fun observeUserItems(userId: String): Flow<List<Item>> = callbackFlow {
        val listener = col
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val sorted = snap?.toObjects(Item::class.java)
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addItem(item: Item): Result<Item> = runCatching {
        if (FirebaseManager.auth.currentUser == null) {
            throw IllegalStateException("User must be authenticated to upload images.")
        }
        val ref = col.document()
        val withId = item.copy(id = ref.id)
        ref.set(withId).await()
        withId
    }

    suspend fun updateItem(item: Item): Result<Unit> = runCatching {
        col.document(item.id).set(item).await()
    }

    suspend fun deleteItem(itemId: String): Result<Unit> = runCatching {
        col.document(itemId).delete().await()
    }

    suspend fun getItem(itemId: String): Item? =
        col.document(itemId).get().await().toObject(Item::class.java)

    suspend fun reportItem(itemId: String, reporterId: String, reason: String): Result<Unit> = runCatching {
        val report = mapOf(
            "itemId" to itemId,
            "reporterId" to reporterId,
            "reason" to reason,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("reports").add(report).await()
    }

    suspend fun uploadImage(uri: Uri): Result<String> = runCatching {
        if (FirebaseManager.auth.currentUser == null) {
            throw IllegalStateException("User must be authenticated to upload images.")
        }
        val storageRef = FirebaseManager.storage.reference
        val imageRef = storageRef.child("items/${UUID.randomUUID()}.jpg")
        val snapshot = imageRef.putFile(uri).await()
        if (snapshot.bytesTransferred == 0L) {
            throw Exception("Upload failed: File stream was empty.")
        }
        imageRef.downloadUrl.await().toString()
    }

    suspend fun getUserItems(userId: String): List<Item> =
        col.whereEqualTo("ownerId", userId).get().await().toObjects(Item::class.java)
            .sortedByDescending { it.createdAt }

    // Request Management
    fun observeIncomingRequests(ownerId: String): Flow<List<com.example.aski.model.ItemRequest>> = callbackFlow {
        val listener = db.collection("requests")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val sorted = snap?.toObjects(com.example.aski.model.ItemRequest::class.java)
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    fun observeOutgoingRequests(requesterId: String): Flow<List<com.example.aski.model.ItemRequest>> = callbackFlow {
        val listener = db.collection("requests")
            .whereEqualTo("requesterId", requesterId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val sorted = snap?.toObjects(com.example.aski.model.ItemRequest::class.java)
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    fun observeItemRequests(itemId: String): Flow<List<com.example.aski.model.ItemRequest>> = callbackFlow {
        val listener = db.collection("requests")
            .whereEqualTo("itemId", itemId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(com.example.aski.model.ItemRequest::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun createRequest(request: com.example.aski.model.ItemRequest): Result<Unit> = runCatching {
        val ref = db.collection("requests").document()
        ref.set(request.copy(id = ref.id)).await()
    }

    suspend fun updateRequest(request: com.example.aski.model.ItemRequest): Result<Unit> = runCatching {
        db.collection("requests").document(request.id).set(request).await()
    }

    suspend fun updateRequestStatus(requestId: String, status: com.example.aski.model.RequestStatus): Result<Unit> = runCatching {
        db.collection("requests").document(requestId).update("status", status.name).await()
    }

    suspend fun cancelRequest(requestId: String): Result<Unit> = runCatching {
        db.collection("requests").document(requestId).delete().await()
    }

    suspend fun acceptRequestWithDelivery(requestId: String, method: DeliveryMethod, itemId: String): Result<Unit> = runCatching {
        val batch = db.batch()
        batch.update(db.collection("requests").document(requestId),
            mapOf(
                "status" to com.example.aski.model.RequestStatus.ACCEPTED.name,
                "deliveryMethod" to method.name,
                "deliveryStatus" to DeliveryStatus.PREPARING.name
            )
        )
        if (itemId.isNotBlank()) {
            batch.update(col.document(itemId), "status", ItemStatus.RESERVED.name)
        }
        batch.commit().await()
    }

    suspend fun rejectRequest(requestId: String, itemId: String, restoreAvailable: Boolean): Result<Unit> = runCatching {
        if (restoreAvailable && itemId.isNotBlank()) {
            val batch = db.batch()
            batch.update(db.collection("requests").document(requestId), "status", com.example.aski.model.RequestStatus.REJECTED.name)
            batch.update(col.document(itemId), "status", ItemStatus.AVAILABLE.name)
            batch.commit().await()
        } else {
            db.collection("requests").document(requestId).update("status", com.example.aski.model.RequestStatus.REJECTED.name).await()
        }
    }

    suspend fun completeRequest(requestId: String, itemId: String): Result<Unit> = runCatching {
        val batch = db.batch()
        batch.update(db.collection("requests").document(requestId), "status", com.example.aski.model.RequestStatus.COMPLETED.name)
        if (itemId.isNotBlank()) {
            batch.update(col.document(itemId), "status", ItemStatus.GIVEN.name)
        }
        batch.commit().await()
    }

    suspend fun updateDeliveryStatus(requestId: String, deliveryStatus: DeliveryStatus): Result<Unit> = runCatching {
        db.collection("requests").document(requestId).update("deliveryStatus", deliveryStatus.name).await()
    }

    // Owner confirms handover (hand-to-hand). Returns true when both parties confirmed → delivery complete.
    suspend fun ownerConfirmHandover(requestId: String, itemId: String): Result<Boolean> = runCatching {
        val reqRef = db.collection("requests").document(requestId)
        db.runTransaction { tx ->
            val snap = tx.get(reqRef)
            val requesterAlreadyConfirmed = snap.getBoolean("requesterConfirmed") ?: false
            if (requesterAlreadyConfirmed) {
                tx.update(reqRef, mapOf(
                    "ownerConfirmed" to true,
                    "deliveryStatus" to DeliveryStatus.DELIVERED.name,
                    "status" to com.example.aski.model.RequestStatus.COMPLETED.name
                ))
                if (itemId.isNotBlank()) tx.update(col.document(itemId), "status", ItemStatus.GIVEN.name)
                true
            } else {
                tx.update(reqRef, "ownerConfirmed", true)
                false
            }
        }.await()
    }

    // Requester confirms receipt. Cargo: always completes. Hand-to-hand: completes if owner already confirmed.
    suspend fun requesterConfirmDelivery(requestId: String, itemId: String, method: DeliveryMethod): Result<Boolean> = runCatching {
        val reqRef = db.collection("requests").document(requestId)
        if (method == DeliveryMethod.CARGO) {
            val batch = db.batch()
            batch.update(reqRef, mapOf(
                "requesterConfirmed" to true,
                "deliveryStatus" to DeliveryStatus.DELIVERED.name,
                "status" to com.example.aski.model.RequestStatus.COMPLETED.name
            ))
            if (itemId.isNotBlank()) batch.update(col.document(itemId), "status", ItemStatus.GIVEN.name)
            batch.commit().await()
            true
        } else {
            db.runTransaction { tx ->
                val snap = tx.get(reqRef)
                val ownerAlreadyConfirmed = snap.getBoolean("ownerConfirmed") ?: false
                if (ownerAlreadyConfirmed) {
                    tx.update(reqRef, mapOf(
                        "requesterConfirmed" to true,
                        "deliveryStatus" to DeliveryStatus.DELIVERED.name,
                        "status" to com.example.aski.model.RequestStatus.COMPLETED.name
                    ))
                    if (itemId.isNotBlank()) tx.update(col.document(itemId), "status", ItemStatus.GIVEN.name)
                    true
                } else {
                    tx.update(reqRef, "requesterConfirmed", true)
                    false
                }
            }.await()
        }
    }
}

package com.example.aski.repository

import android.net.Uri
import com.example.aski.firebase.FirebaseManager
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
            .whereIn("status", listOf(ItemStatus.AVAILABLE.name, ItemStatus.RESERVED.name))
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
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
                if (err != null) { close(err); return@addSnapshotListener }
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
}

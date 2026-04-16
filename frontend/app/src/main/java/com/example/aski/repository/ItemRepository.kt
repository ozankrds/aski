package com.example.aski.repository

import android.net.Uri
import com.example.aski.firebase.FirebaseManager
import com.example.aski.model.Item
import com.example.aski.model.ItemStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ItemRepository(
    private val db: FirebaseFirestore = FirebaseManager.firestore
) {
    private val col = db.collection("items")

    // Real-time feed of available items
    fun observeAvailableItems(): Flow<List<Item>> = callbackFlow {
        val listener = col
            .whereEqualTo("status", ItemStatus.AVAILABLE.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(Item::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // Real-time items for a specific user
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

    suspend fun getItem(itemId: String): Item? =
        col.document(itemId).get().await().toObject(Item::class.java)

    suspend fun uploadImage(uri: Uri): Result<String> = runCatching {
        if (FirebaseManager.auth.currentUser == null) {
            throw IllegalStateException("User must be authenticated to upload images.")
        }
        val storageRef = FirebaseManager.storage.reference
        val imageRef = storageRef.child("items/${UUID.randomUUID()}.jpg")

        // Await the upload and capture the snapshot
        val snapshot = imageRef.putFile(uri).await()

        // Ensure the upload actually transferred bytes
        if (snapshot.bytesTransferred == 0L) {
            throw Exception("Upload failed: File stream was empty.")
        }

        // Only request the URL if the upload is confirmed
        imageRef.downloadUrl.await().toString()
    }
}

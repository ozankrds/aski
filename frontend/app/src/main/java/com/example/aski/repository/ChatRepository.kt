package com.example.aski.repository

import android.net.Uri
import com.example.aski.firebase.FirebaseManager
import com.example.aski.model.Chat
import com.example.aski.model.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val chatsCol = db.collection("chats")

    fun observeUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = chatsCol
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val sorted = snap?.toObjects(Chat::class.java)
                    ?.sortedByDescending { it.lastMessageAt }
                    ?: emptyList()
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = chatsCol.document(chatId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(Message::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun getOrCreateChat(itemId: String, requesterId: String, ownerId: String, itemImageUrl: String = ""): Result<Chat> = runCatching {
        val existing = chatsCol
            .whereEqualTo("itemId", itemId)
            .whereArrayContains("participants", requesterId)
            .get().await()
            .toObjects(Chat::class.java)
            .firstOrNull { it.participants.contains(ownerId) }

        if (existing != null) return@runCatching existing

        val ref = chatsCol.document()
        val chat = Chat(
            id = ref.id,
            itemId = itemId,
            itemImageUrl = itemImageUrl,
            participants = listOf(requesterId, ownerId),
            requesterId = requesterId
        )
        ref.set(chat).await()
        chat
    }

    suspend fun sendMessage(chatId: String, senderId: String, content: String, participants: List<String>, imageUrl: String = ""): Result<Unit> = runCatching {
        val msgRef = chatsCol.document(chatId).collection("messages").document()
        val message = Message(id = msgRef.id, chatId = chatId, senderId = senderId, content = content, imageUrl = imageUrl)
        msgRef.set(message).await()

        val preview = if (imageUrl.isNotBlank()) "📷 Photo" else content
        val updates = mutableMapOf<String, Any>(
            "lastMessage" to preview,
            "lastMessageAt" to System.currentTimeMillis()
        )
        participants.filter { it != senderId }.forEach { participantId ->
            updates["unreadCounts.$participantId"] = FieldValue.increment(1)
        }
        chatsCol.document(chatId).update(updates).await()
    }

    suspend fun uploadImage(uri: Uri): Result<String> = runCatching {
        val ref = FirebaseManager.storage.reference.child("chat_images/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    suspend fun markAsRead(chatId: String, userId: String): Result<Unit> = runCatching {
        chatsCol.document(chatId).update("unreadCounts.$userId", 0).await()
    }

    suspend fun getChat(chatId: String): Chat? =
        chatsCol.document(chatId).get().await().toObject(Chat::class.java)
}

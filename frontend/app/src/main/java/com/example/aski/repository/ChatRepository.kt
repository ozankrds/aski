package com.example.aski.repository

import com.example.aski.model.Chat
import com.example.aski.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val chatsCol = db.collection("chats")

    fun observeUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = chatsCol
            .whereArrayContains("participants", userId)  // orderBy kaldır
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val sorted = snap?.toObjects(Chat::class.java)
                    ?.sortedByDescending { it.lastMessageAt }  // client-side
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

    suspend fun getOrCreateChat(itemId: String, requesterId: String, ownerId: String): Result<Chat> = runCatching {
        // Check if chat already exists
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
            participants = listOf(requesterId, ownerId),
            requesterId = requesterId
        )
        ref.set(chat).await()
        chat
    }

    suspend fun sendMessage(chatId: String, senderId: String, content: String): Result<Unit> = runCatching {
        val msgRef = chatsCol.document(chatId).collection("messages").document()
        val message = Message(id = msgRef.id, chatId = chatId, senderId = senderId, content = content)
        msgRef.set(message).await()

        // Update chat preview
        chatsCol.document(chatId).update(
            mapOf(
                "lastMessage" to content,
                "lastMessageAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun getChat(chatId: String): Chat? =
        chatsCol.document(chatId).get().await().toObject(Chat::class.java)
}
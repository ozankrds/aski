package com.example.aski.repository

import com.example.aski.model.Rating
import com.example.aski.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RatingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getRatingForItem(itemId: String): Rating? = runCatching {
        db.collection("ratings").document(itemId).get().await().toObject(Rating::class.java)
    }.getOrNull()

    suspend fun submitOrUpdateRating(
        itemId: String,
        raterId: String,
        targetUserId: String,
        newScore: Int
    ): Result<Unit> = runCatching {
        db.runTransaction { transaction ->
            val ratingRef = db.collection("ratings").document(itemId)
            val userRef = db.collection("users").document(targetUserId)

            val ratingDoc = transaction.get(ratingRef)
            val userDoc = transaction.get(userRef)
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")

            if (!ratingDoc.exists()) {
                // New Rating
                val newRatingCount = user.ratingCount + 1
                val newAverage = ((user.rating * user.ratingCount) + newScore) / newRatingCount
                
                transaction.update(userRef, mapOf(
                    "rating" to newAverage,
                    "ratingCount" to newRatingCount
                ))
                
                val rating = Rating(
                    itemId = itemId,
                    raterId = raterId,
                    targetUserId = targetUserId,
                    score = newScore,
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(ratingRef, rating)
            } else {
                // Update Rating
                val existingRating = ratingDoc.toObject(Rating::class.java)!!
                val oldScore = existingRating.score
                
                val newAverage = ((user.rating * user.ratingCount) - oldScore + newScore) / user.ratingCount
                
                transaction.update(userRef, "rating", newAverage)
                transaction.update(ratingRef, "score", newScore, "timestamp", System.currentTimeMillis())
            }
        }.await()
    }
}

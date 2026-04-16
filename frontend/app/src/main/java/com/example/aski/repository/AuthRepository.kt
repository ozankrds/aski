package com.example.aski.repository

import com.example.aski.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentFirebaseUser get() = auth.currentUser

    suspend fun signup(email: String, password: String, name: String): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        val user = User(id = uid, name = name, email = email)
        db.collection("users").document(uid).set(user).await()
        user
    }

    suspend fun login(email: String, password: String): Result<User> = runCatching {
        val firebaseUser = auth.signInWithEmailAndPassword(email, password).await().user!!
        // Fetch full user profile from Firestore after successful auth login
        getUserById(firebaseUser.uid) ?: User(
            id = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: ""
        )
    }

    fun logout() = auth.signOut()

    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return getUserById(uid)
    }

    suspend fun getUserById(userId: String): User? =
        db.collection("users").document(userId).get().await().toObject(User::class.java)

    suspend fun toggleFavorite(userId: String, itemId: String, isFavorite: Boolean): Result<Unit> = runCatching {
        val ref = db.collection("users").document(userId)
        if (isFavorite) {
            ref.update("favoriteIds", FieldValue.arrayUnion(itemId)).await()
        } else {
            ref.update("favoriteIds", FieldValue.arrayRemove(itemId)).await()
        }
    }
}

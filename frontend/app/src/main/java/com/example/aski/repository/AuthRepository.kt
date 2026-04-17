package com.example.aski.repository

import android.net.Uri
import com.example.aski.firebase.FirebaseManager
import com.example.aski.model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

    suspend fun uploadProfilePhoto(userId: String, uri: Uri): Result<String> = runCatching {
        val ref = FirebaseManager.storage.reference.child("avatars/$userId.jpg")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    suspend fun updateProfile(userId: String, name: String, newPassword: String?, currentPassword: String?, photoUri: Uri?): Result<User> = runCatching {
        val firebaseUser = auth.currentUser ?: throw IllegalStateException("Not authenticated")

        if (!newPassword.isNullOrBlank() && !currentPassword.isNullOrBlank()) {
            reauthenticate(firebaseUser.email ?: "", currentPassword).getOrThrow()
            firebaseUser.updatePassword(newPassword).await()
        }

        val photoUrl = if (photoUri != null) uploadProfilePhoto(userId, photoUri).getOrThrow() else null

        val profileUpdates = userProfileChangeRequest {
            displayName = name
            if (photoUrl != null) this.photoUri = android.net.Uri.parse(photoUrl)
        }
        firebaseUser.updateProfile(profileUpdates).await()

        val updates = mutableMapOf<String, Any>("name" to name)
        if (photoUrl != null) updates["photoUrl"] = photoUrl
        FirebaseManager.firestore.collection("users").document(userId).update(updates).await()

        getUserById(userId) ?: throw IllegalStateException("User not found after update")
    }

    suspend fun reauthenticate(email: String, currentPassword: String): Result<Unit> = runCatching {
        val firebaseUser = auth.currentUser ?: throw IllegalStateException("Not authenticated")
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        firebaseUser.reauthenticate(credential).await()
    }

    suspend fun saveFcmToken(userId: String, token: String): Result<Unit> = runCatching {
        db.collection("users").document(userId).update("fcmToken", token).await()
    }

    suspend fun toggleFavorite(userId: String, itemId: String, isFavorite: Boolean): Result<Unit> = runCatching {
        val ref = db.collection("users").document(userId)
        if (isFavorite) {
            ref.update("favoriteIds", FieldValue.arrayUnion(itemId)).await()
        } else {
            ref.update("favoriteIds", FieldValue.arrayRemove(itemId)).await()
        }
    }
}

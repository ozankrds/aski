package com.example.aski.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aski.model.User
import com.example.aski.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError

    init {
        viewModelScope.launch {
            val user = repo.getCurrentUser()
            _authState.value = if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated
        }
    }

    fun saveFcmToken(token: String) {
        val uid = (authState.value as? AuthState.Authenticated)?.user?.id ?: return
        viewModelScope.launch { repo.saveFcmToken(uid, token) }
    }

    fun signup(email: String, password: String, name: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repo.signup(email, password, name)
                .onSuccess { _authState.value = AuthState.Authenticated(it) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Signup failed") }
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            repo.login(email, password)
                .onSuccess { _authState.value = AuthState.Authenticated(it) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Login failed") }
        }
    }

    fun logout() {
        repo.logout()
        _authState.value = AuthState.Unauthenticated
    }

    fun updateProfile(name: String, newPassword: String?, currentPassword: String?, photoUri: Uri?) {
        val currentUser = (authState.value as? AuthState.Authenticated)?.user ?: return
        _authState.value = AuthState.Loading
        _profileError.value = null
        viewModelScope.launch {
            repo.updateProfile(currentUser.id, name, newPassword, currentPassword, photoUri)
                .onSuccess { _authState.value = AuthState.Authenticated(it) }
                .onFailure {
                    _authState.value = AuthState.Authenticated(currentUser)
                    _profileError.value = it.message
                }
        }
    }

    fun clearProfileError() { _profileError.value = null }

    fun toggleFavorite(itemId: String) {
        val currentUser = (authState.value as? AuthState.Authenticated)?.user ?: return
        val isFavorite = currentUser.favoriteIds.contains(itemId)
        viewModelScope.launch {
            repo.toggleFavorite(currentUser.id, itemId, !isFavorite)
                .onSuccess {
                    val updated = if (isFavorite) {
                        currentUser.favoriteIds - itemId
                    } else {
                        currentUser.favoriteIds + itemId
                    }
                    _authState.value = AuthState.Authenticated(currentUser.copy(favoriteIds = updated))
                }
        }
    }

    suspend fun getUserName(userId: String): String? = repo.getUserById(userId)?.name

    suspend fun getUserById(userId: String): User? = repo.getUserById(userId)
}

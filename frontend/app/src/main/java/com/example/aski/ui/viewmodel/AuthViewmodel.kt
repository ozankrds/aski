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
    data class Authenticated(val user: User, val isUpdating: Boolean = false) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

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
        _authState.value = AuthState.Authenticated(currentUser, isUpdating = true)
        _profileError.value = null
        viewModelScope.launch {
            repo.updateProfile(currentUser.id, name, newPassword, currentPassword, photoUri)
                .onSuccess { _authState.value = AuthState.Authenticated(it, isUpdating = false) }
                .onFailure {
                    _authState.value = AuthState.Authenticated(currentUser, isUpdating = false)
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
                        val list = currentUser.favoriteIds.toMutableList()
                        list.add(itemId)
                        list
                    }
                    _authState.value = AuthState.Authenticated(currentUser.copy(favoriteIds = updated))
                }
        }
    }

    suspend fun getUserName(userId: String): String? = repo.getUserById(userId)?.name

    suspend fun getUserById(userId: String): User? = repo.getUserById(userId)

    fun rateUser(userId: String, rating: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repo.rateUser(userId, rating)
            onComplete()
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            _searchResults.value = repo.searchUsers(query)
        }
    }

    fun incrementKarmaAndGiven(userId: String) {
        viewModelScope.launch { repo.incrementKarmaAndGiven(userId) }
    }
}

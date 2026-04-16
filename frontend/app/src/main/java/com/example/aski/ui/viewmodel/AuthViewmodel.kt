package com.example.aski.ui.viewmodel

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

    init {
        viewModelScope.launch {
            val user = repo.getCurrentUser()
            _authState.value = if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated
        }
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

    fun toggleFavorite(itemId: String) {
        val currentUser = (authState.value as? AuthState.Authenticated)?.user ?: return
        val isFavorite = currentUser.favoriteIds.contains(itemId)
        
        viewModelScope.launch {
            repo.toggleFavorite(currentUser.id, itemId, !isFavorite)
                .onSuccess {
                    val updatedFavorites = if (isFavorite) {
                        currentUser.favoriteIds - itemId
                    } else {
                        currentUser.favoriteIds + itemId
                    }
                    _authState.value = AuthState.Authenticated(currentUser.copy(favoriteIds = updatedFavorites))
                }
        }
    }
}

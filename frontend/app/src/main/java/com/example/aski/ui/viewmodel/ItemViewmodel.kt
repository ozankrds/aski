package com.example.aski.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aski.model.Item
import com.example.aski.model.ItemCondition
import com.example.aski.repository.ItemRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ItemViewModel(
    private val repo: ItemRepository = ItemRepository()
) : ViewModel() {

    private val _feedItems = MutableStateFlow<List<Item>>(emptyList())
    val feedItems: StateFlow<List<Item>> = _feedItems

    private val _userItems = MutableStateFlow<List<Item>>(emptyList())
    val userItems: StateFlow<List<Item>> = _userItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _incomingRequests = MutableStateFlow<List<com.example.aski.model.ItemRequest>>(emptyList())
    val incomingRequests: StateFlow<List<com.example.aski.model.ItemRequest>> = _incomingRequests

    private val _outgoingRequests = MutableStateFlow<List<com.example.aski.model.ItemRequest>>(emptyList())
    val outgoingRequests: StateFlow<List<com.example.aski.model.ItemRequest>> = _outgoingRequests

    private val _itemRequests = MutableStateFlow<Map<String, List<com.example.aski.model.ItemRequest>>>(emptyMap())
    val itemRequests: StateFlow<Map<String, List<com.example.aski.model.ItemRequest>>> = _itemRequests

    private val _selectedCategoryId = MutableStateFlow(0)
    val selectedCategoryId: StateFlow<Int> = _selectedCategoryId

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCondition = MutableStateFlow<ItemCondition?>(null)
    val selectedCondition: StateFlow<ItemCondition?> = _selectedCondition

    val filteredItems: StateFlow<List<Item>> = combine(
        _feedItems, _selectedCategoryId, _selectedCondition, _searchQuery
    ) { items, cat, cond, query ->
        items.filter { item ->
            val matchesCategory = cat == 0 || item.categoryId == cat
            val matchesCondition = cond == null || item.condition == cond
            val matchesSearch = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.location.contains(query, ignoreCase = true)
            
            matchesCategory && matchesCondition && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCondition(condition: ItemCondition?) { _selectedCondition.value = condition }

    init {
        observeFeed()
    }

    private fun observeFeed() {
        _isLoading.value = true
        viewModelScope.launch {
            repo.observeFeedItems().collect { 
                _feedItems.value = it
                _isLoading.value = false
            }
        }
    }

    fun observeUserItems(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            repo.observeUserItems(userId).collect { 
                _userItems.value = it
                _isLoading.value = false
            }
        }
    }

    fun observeItemRequests(itemId: String) {
        viewModelScope.launch {
            repo.observeItemRequests(itemId).collect { requests ->
                _itemRequests.value = _itemRequests.value + (itemId to requests)
            }
        }
    }

    fun selectCategory(id: Int) { _selectedCategoryId.value = id }

    fun addItem(
        ownerId: String,
        title: String,
        description: String,
        categoryId: Int,
        condition: ItemCondition,
        location: String,
        imageUris: List<Uri>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val imageUrls = imageUris.map { uri -> repo.uploadImage(uri).getOrThrow() }
                val item = Item(
                    ownerId = ownerId,
                    title = title,
                    description = description,
                    categoryId = categoryId,
                    condition = condition,
                    location = location,
                    imageUrls = imageUrls
                )
                repo.addItem(item).getOrThrow()
                onSuccess()
            } catch (e: IllegalStateException) {
                onError(e.message ?: "User must be authenticated to upload images.")
            } catch (e: Exception) {
                onError(e.message ?: "Failed to post item")
            }
        }
    }

    fun updateItem(item: Item) = viewModelScope.launch { repo.updateItem(item) }

    fun deleteItem(itemId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.deleteItem(itemId).onSuccess { onSuccess() }
        }
    }

    fun reportItem(itemId: String, reporterId: String, reason: String) {
        viewModelScope.launch { repo.reportItem(itemId, reporterId, reason) }
    }

    suspend fun getItem(itemId: String) = repo.getItem(itemId)

    suspend fun getUserItems(userId: String) = repo.getUserItems(userId)

    // Request Flow
    fun observeIncomingRequests(ownerId: String) {
        viewModelScope.launch {
            repo.observeIncomingRequests(ownerId).collect { _incomingRequests.value = it }
        }
    }

    fun observeOutgoingRequests(requesterId: String) {
        viewModelScope.launch {
            repo.observeOutgoingRequests(requesterId).collect { _outgoingRequests.value = it }
        }
    }

    fun createRequest(
        item: Item,
        requesterId: String,
        requesterName: String,
        ownerName: String,
        onSuccess: () -> Unit = {}
    ) {
        // Find if there's an existing request that's not rejected
        val existingRequest = _outgoingRequests.value.find { 
            it.itemId == item.id && it.requesterId == requesterId 
        }

        if (existingRequest != null) {
            if (existingRequest.status == com.example.aski.model.RequestStatus.REJECTED) {
                // If previously rejected, reuse the document and set back to PENDING
                viewModelScope.launch {
                    val refreshedRequest = existingRequest.copy(
                        status = com.example.aski.model.RequestStatus.PENDING,
                        createdAt = System.currentTimeMillis()
                    )
                    repo.updateRequest(refreshedRequest).onSuccess { onSuccess() }
                }
                return
            } else {
                // If Pending or Accepted, do nothing (don't double-send)
                return
            }
        }

        // No previous request found, create a brand new one
        val request = com.example.aski.model.ItemRequest(
            itemId = item.id,
            itemTitle = item.title,
            itemImageUrl = item.imageUrls.firstOrNull() ?: "",
            requesterId = requesterId,
            requesterName = requesterName,
            ownerId = item.ownerId,
            ownerName = ownerName,
            status = com.example.aski.model.RequestStatus.PENDING
        )
        viewModelScope.launch {
            repo.createRequest(request).onSuccess { onSuccess() }
        }
    }

    fun cancelRequest(itemId: String, requesterId: String) {
        val request = _outgoingRequests.value.find { it.itemId == itemId && it.requesterId == requesterId }
        request?.let {
            viewModelScope.launch {
                repo.cancelRequest(it.id)
            }
        }
    }

    fun updateRequestStatus(requestId: String, status: com.example.aski.model.RequestStatus) {
        viewModelScope.launch {
            repo.updateRequestStatus(requestId, status)
        }
    }
}

package com.example.aski.ui.viewmodel

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

    private val _selectedCategoryId = MutableStateFlow(0)
    val selectedCategoryId: StateFlow<Int> = _selectedCategoryId

    val filteredItems: StateFlow<List<Item>> = combine(_feedItems, _selectedCategoryId) { items, cat ->
        if (cat == 0) items else items.filter { it.categoryId == cat }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeFeed()
    }

    private fun observeFeed() {
        viewModelScope.launch {
            repo.observeAvailableItems().collect { _feedItems.value = it }
        }
    }

    fun observeUserItems(userId: String) {
        viewModelScope.launch {
            repo.observeUserItems(userId).collect { _userItems.value = it }
        }
    }

    fun selectCategory(id: Int) { _selectedCategoryId.value = id }

    fun addItem(
        ownerId: String,
        title: String,
        description: String,
        categoryId: Int,
        condition: ItemCondition,
        imageUrl: String
    ) = viewModelScope.launch {
        val item = Item(
            ownerId = ownerId,
            title = title,
            description = description,
            categoryId = categoryId,
            condition = condition,
            imageUrl = imageUrl
        )
        repo.addItem(item)
    }

    fun updateItem(item: Item) = viewModelScope.launch { repo.updateItem(item) }

    suspend fun getItem(itemId: String) = repo.getItem(itemId)
}
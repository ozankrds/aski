package com.example.aski.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aski.model.Item
import com.example.aski.model.ItemStatus
import com.example.aski.model.mockCategories

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    items: List<Item>,
    onItemClick: (String) -> Unit,
    onCreateListingClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(mockCategories.first()) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aski - Give & Take") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateListingClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Listing")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Search items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            LazyRow(modifier = Modifier.padding(horizontal = 8.dp)) {
                items(mockCategories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            LazyColumn {
                val filteredItems = items.filter { 
                    (selectedCategory.id == 0 || it.categoryId == selectedCategory.id) &&
                    it.title.contains(searchQuery, ignoreCase = true)
                }
                items(filteredItems) { item ->
                    ItemCard(item = item, onClick = { onItemClick(item.id) })
                }
            }
        }
    }
}

@Composable
fun ItemCard(item: Item, onClick: () -> Unit) {
    val categoryName = mockCategories.find { it.id == item.categoryId }?.name ?: "Unknown"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = item.primaryImageUrl,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = item.title, style = MaterialTheme.typography.titleLarge)
                Text(text = categoryName, style = MaterialTheme.typography.bodyMedium)
                Text(text = item.condition.name, style = MaterialTheme.typography.bodySmall)
                Badge(
                    containerColor = when(item.status) {
                        ItemStatus.AVAILABLE -> MaterialTheme.colorScheme.primaryContainer
                        ItemStatus.RESERVED -> MaterialTheme.colorScheme.secondaryContainer
                        ItemStatus.GIVEN -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(item.status.name)
                }
            }
        }
    }
}

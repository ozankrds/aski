package com.example.aski.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aski.model.Item
import com.example.aski.model.ItemStatus
import com.example.aski.model.ItemCondition
import com.example.aski.model.mockCategories

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: Item,
    isOwner: Boolean,
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit,
    onUpdateItem: (String, String, String, ItemCondition, ItemStatus) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(item.title) }
    var editDescription by remember { mutableStateOf(item.description) }
    var editCondition by remember { mutableStateOf(item.condition) }
    var editStatus by remember { mutableStateOf(item.status) }

    val categoryName = mockCategories.find { it.id == item.categoryId }?.name ?: "Unknown"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Item" else "Item Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isOwner) {
                        if (isEditing) {
                            IconButton(onClick = {
                                onUpdateItem(item.id, editTitle, editDescription, editCondition, editStatus)
                                isEditing = false
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isOwner && item.status == ItemStatus.AVAILABLE) {
                Button(
                    onClick = { onChatClick(item.ownerId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Chat with Owner")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = item.primaryImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Condition", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ItemCondition.entries.forEach { condition ->
                            FilterChip(
                                selected = editCondition == condition,
                                onClick = { editCondition = condition },
                                label = { Text(condition.name) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Status", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ItemStatus.entries.forEach { status ->
                            FilterChip(
                                selected = editStatus == status,
                                onClick = { editStatus = status },
                                label = { Text(status.name) }
                            )
                        }
                    }
                } else {
                    Text(text = item.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SuggestionChip(onClick = {}, label = { Text(categoryName) })
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(onClick = {}, label = { Text(item.condition.name) })
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Description", style = MaterialTheme.typography.titleMedium)
                    Text(text = item.description, style = MaterialTheme.typography.bodyLarge)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Status: ${item.status}", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

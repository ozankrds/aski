package com.example.aski.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aski.model.Chat
import com.example.aski.model.User
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chats: List<Chat>,
    currentUserId: String,
    getUserById: (String) -> User?,
    onChatClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No messages yet")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(chats) { chat ->
                    val otherUserId = if (chat.requesterId == currentUserId) chat.ownerId else chat.requesterId
                    val otherUser = getUserById(otherUserId)
                    
                    ListItem(
                        headlineContent = { Text(otherUser?.displayName ?: "Unknown User") },
                        supportingContent = { 
                            val date = Date(chat.lastMessageAt)
                            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                            Text("Last message at ${format.format(date)}")
                        },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = otherUser?.displayName?.take(1) ?: "?",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onChatClick(chat.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

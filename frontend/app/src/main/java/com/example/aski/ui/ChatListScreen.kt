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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chats: List<Chat>,
    currentUserId: String,
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
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(chats, key = { it.id }) { chat ->
                    val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: "Unknown"
                    val initial = otherUserId.take(1).uppercase()
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.lastMessageAt))

                    ListItem(
                        headlineContent = { Text(otherUserId) }, // isim göstermek istersen Chat'e otherUserName ekle
                        supportingContent = {
                            Text(
                                chat.lastMessage.ifBlank { timeStr },
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(initial, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        },
                        trailingContent = {
                            Text(timeStr, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier.clickable { onChatClick(chat.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
package com.example.aski.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aski.model.Chat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chats: List<Chat>,
    currentUserId: String,
    userNames: Map<String, String>,
    onFetchName: (String) -> Unit,
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
                    val displayName = userNames[otherUserId] ?: otherUserId
                    val initial = displayName.take(1).uppercase()
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.lastMessageAt))
                    val unread = chat.unreadCounts[currentUserId] ?: 0

                    LaunchedEffect(otherUserId) {
                        if (!userNames.containsKey(otherUserId)) onFetchName(otherUserId)
                    }

                    ListItem(
                        headlineContent = {
                            Text(displayName, fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal)
                        },
                        supportingContent = {
                            Text(
                                chat.lastMessage.ifBlank { "No messages" },
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingContent = {
                            Box(modifier = Modifier.size(48.dp)) {
                                if (chat.itemImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = chat.itemImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(initial, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                                if (unread > 0) {
                                    Badge(
                                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                                    ) { Text("$unread") }
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

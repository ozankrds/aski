package com.example.aski.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aski.model.ItemRequest
import com.example.aski.model.RequestStatus
import com.example.aski.ui.theme.AskiOnBgVariant
import com.example.aski.ui.theme.AskiSuccess
import com.example.aski.ui.theme.AskiWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    incomingRequests: List<ItemRequest>,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Received Requests") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (incomingRequests.isEmpty()) {
            Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No requests found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(incomingRequests, key = { it.id }) { request ->
                    // Find if there's any accepted request for this item
                    val hasAnyAccepted = incomingRequests.any { 
                        it.itemId == request.itemId && it.status == RequestStatus.ACCEPTED 
                    }

                    RequestItemCard(
                        request = request,
                        canAccept = !hasAnyAccepted,
                        onAccept = { onAcceptRequest(request.id) },
                        onReject = { onRejectRequest(request.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestItemCard(
    request: ItemRequest,
    canAccept: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val statusColor = when (request.status) {
        RequestStatus.PENDING -> AskiWarning
        RequestStatus.ACCEPTED -> AskiSuccess
        RequestStatus.REJECTED -> MaterialTheme.colorScheme.error
        RequestStatus.COMPLETED -> AskiOnBgVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = request.itemImageUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(request.itemTitle, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    "from ${request.requesterName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        request.status.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (request.status == RequestStatus.PENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = onReject,
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Reject",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Tick button only appears if canAccept is true
                    if (canAccept) {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier
                                .size(40.dp)
                                .background(AskiSuccess.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Accept",
                                tint = AskiSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

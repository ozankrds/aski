package com.example.aski.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    outgoingRequests: List<ItemRequest>,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onCompleteRequest: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Received", "Sent")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Requests") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            val displayed = if (selectedTab == 0) incomingRequests else outgoingRequests

            if (displayed.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No requests found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayed, key = { it.id }) { request ->
                        RequestItemCard(
                            request = request,
                            isIncoming = selectedTab == 0,
                            onAccept = { onAcceptRequest(request.id) },
                            onReject = { onRejectRequest(request.id) },
                            onComplete = { onCompleteRequest(request.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestItemCard(
    request: ItemRequest,
    isIncoming: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onComplete: () -> Unit
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
                    if (isIncoming) "from ${request.requesterName}" else "to ${request.ownerName}",
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

            if (isIncoming && request.status == RequestStatus.PENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onReject, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)) {
                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onAccept, modifier = Modifier.size(32.dp).background(AskiSuccess.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.Default.Check, contentDescription = "Accept", tint = AskiSuccess, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (!isIncoming && request.status == RequestStatus.ACCEPTED) {
                Button(
                    onClick = onComplete,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AskiSuccess)
                ) {
                    Text("Received", fontSize = 12.sp)
                }
            }
        }
    }
}

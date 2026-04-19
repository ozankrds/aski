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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.aski.model.DeliveryMethod
import com.example.aski.model.DeliveryStatus
import com.example.aski.model.ItemRequest
import com.example.aski.model.Rating
import com.example.aski.model.RequestStatus
import com.example.aski.ui.theme.AskiOnBgVariant
import com.example.aski.ui.theme.AskiSuccess
import com.example.aski.ui.theme.AskiWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemRequestsScreen(
    itemId: String,
    allRequests: List<ItemRequest>,
    ratings: Map<String, Rating> = emptyMap(),
    onAcceptWithDelivery: (requestId: String, method: DeliveryMethod) -> Unit,
    onRejectRequest: (String) -> Unit,
    onOwnerMarkShipped: (String) -> Unit,
    onOwnerConfirmHandover: (String) -> Unit,
    onRatingClick: (ItemRequest) -> Unit,
    onBackClick: () -> Unit
) {
    val filteredRequests = remember(allRequests, itemId) {
        allRequests.filter { it.itemId == itemId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Requests") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (filteredRequests.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No requests for this item yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRequests, key = { it.id }) { request ->
                    val hasAnyAccepted = filteredRequests.any { it.status == RequestStatus.ACCEPTED }
                    
                    IncomingRequestCard(
                        request = request,
                        rating = ratings[request.itemId],
                        canAccept = !hasAnyAccepted || request.status == RequestStatus.ACCEPTED,
                        onAcceptWithDelivery = { method -> onAcceptWithDelivery(request.id, method) },
                        onReject = { onRejectRequest(request.id) },
                        onOwnerMarkShipped = { onOwnerMarkShipped(request.id) },
                        onOwnerConfirmHandover = { onOwnerConfirmHandover(request.id) },
                        onRatingClick = { onRatingClick(request) }
                    )
                }
            }
        }
    }
}

// Re-using the card logic from RequestsScreen but keeping it internal or reachable
@Composable
private fun IncomingRequestCard(
    request: ItemRequest,
    rating: Rating?,
    canAccept: Boolean,
    onAcceptWithDelivery: (DeliveryMethod) -> Unit,
    onReject: () -> Unit,
    onOwnerMarkShipped: () -> Unit,
    onOwnerConfirmHandover: () -> Unit,
    onRatingClick: () -> Unit
) {
    var showDeliveryDialog by remember { mutableStateOf(false) }

    if (showDeliveryDialog) {
        DeliveryMethodDialog(
            onSelect = { method -> onAcceptWithDelivery(method); showDeliveryDialog = false },
            onDismiss = { showDeliveryDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (request.status == RequestStatus.COMPLETED) Modifier.clickable { onRatingClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(request.status)
                        if (request.status == RequestStatus.COMPLETED && rating != null) {
                            RatingBadge(rating.score)
                        }
                    }
                }
                if (request.status == RequestStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, "Reject", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                        }
                        if (canAccept) {
                            IconButton(
                                onClick = { showDeliveryDialog = true },
                                modifier = Modifier.size(40.dp).background(AskiSuccess.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(Icons.Default.Check, "Accept", tint = AskiSuccess, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            if (request.status == RequestStatus.ACCEPTED) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                // Note: Delivery tracking logic is simpler here as it's always from owner's perspective
                OwnerDeliveryTracker(
                    request = request,
                    onMarkShipped = onOwnerMarkShipped,
                    onConfirmHandover = onOwnerConfirmHandover
                )
            }
        }
    }
}

@Composable
private fun RatingBadge(score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(2.dp))
        Text(score.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusBadge(status: RequestStatus) {
    val (color, label) = when (status) {
        RequestStatus.PENDING -> AskiWarning to "Pending"
        RequestStatus.ACCEPTED -> AskiSuccess to "Accepted"
        RequestStatus.REJECTED -> MaterialTheme.colorScheme.error to "Rejected"
        RequestStatus.COMPLETED -> AskiOnBgVariant to "Completed"
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OwnerDeliveryTracker(
    request: ItemRequest,
    onMarkShipped: () -> Unit,
    onConfirmHandover: () -> Unit
) {
    val isCargo = request.deliveryMethod == DeliveryMethod.CARGO
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                if (isCargo) Icons.Default.LocalShipping else Icons.Default.Handshake,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (isCargo) "Cargo Delivery" else "Hand-to-Hand",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            request.deliveryStatus == DeliveryStatus.PREPARING -> {
                Button(
                    onClick = onMarkShipped,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AskiSuccess)
                ) { Text(if (isCargo) "Mark as Shipped" else "Mark as Ready for Pickup", fontSize = 13.sp) }
            }
            request.deliveryStatus == DeliveryStatus.SHIPPED && !isCargo -> {
                if (request.ownerConfirmed) {
                    Text(
                        "You confirmed handover. Waiting for ${request.requesterName}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Button(
                        onClick = onConfirmHandover,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AskiSuccess)
                    ) { Text("I've Handed It Over", fontSize = 13.sp) }
                }
            }
            request.deliveryStatus == DeliveryStatus.SHIPPED && isCargo -> {
                Text(
                    "Waiting for ${request.requesterName} to confirm delivery...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeliveryMethodDialog(onSelect: (DeliveryMethod) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("How will you deliver?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Choose the delivery method for this item.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onSelect(DeliveryMethod.HAND_TO_HAND) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                            Icon(Icons.Default.Handshake, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Hand-to-Hand", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedButton(
                        onClick = { onSelect(DeliveryMethod.CARGO) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                            Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Cargo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}

package com.example.aski.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.aski.model.DeliveryMethod
import com.example.aski.model.DeliveryStatus
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
    filterItemId: String? = null,
    onAcceptWithDelivery: (requestId: String, method: DeliveryMethod) -> Unit,
    onRejectRequest: (String) -> Unit,
    onOwnerMarkShipped: (String) -> Unit,
    onOwnerConfirmHandover: (String) -> Unit,
    onRequesterConfirm: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val filteredIncoming = remember(incomingRequests, filterItemId) {
        if (filterItemId != null) incomingRequests.filter { it.itemId == filterItemId }
        else incomingRequests
    }

    val filteredOutgoing = remember(outgoingRequests, filterItemId) {
        if (filterItemId != null) outgoingRequests.filter { it.itemId == filterItemId }
        else outgoingRequests
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (filterItemId != null) "Item Requests" else "Requests") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Incoming")
                            val pending = filteredIncoming.count { it.status == RequestStatus.PENDING }
                            if (pending > 0) Badge { Text("$pending") }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Outgoing") }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            when (selectedTab) {
                0 -> IncomingRequestsList(
                    requests = filteredIncoming,
                    onAcceptWithDelivery = onAcceptWithDelivery,
                    onReject = onRejectRequest,
                    onOwnerMarkShipped = onOwnerMarkShipped,
                    onOwnerConfirmHandover = onOwnerConfirmHandover
                )
                1 -> OutgoingRequestsList(
                    requests = filteredOutgoing,
                    onRequesterConfirm = onRequesterConfirm
                )
            }
        }
    }
}

@Composable
private fun IncomingRequestsList(
    requests: List<ItemRequest>,
    onAcceptWithDelivery: (requestId: String, method: DeliveryMethod) -> Unit,
    onReject: (String) -> Unit,
    onOwnerMarkShipped: (String) -> Unit,
    onOwnerConfirmHandover: (String) -> Unit
) {
    if (requests.isEmpty()) { EmptyState("No incoming requests"); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(requests, key = { it.id }) { request ->
            val hasAnyAccepted = requests.any { it.itemId == request.itemId && it.status == RequestStatus.ACCEPTED }
            IncomingRequestCard(
                request = request,
                canAccept = !hasAnyAccepted,
                onAcceptWithDelivery = { method -> onAcceptWithDelivery(request.id, method) },
                onReject = { onReject(request.id) },
                onOwnerMarkShipped = { onOwnerMarkShipped(request.id) },
                onOwnerConfirmHandover = { onOwnerConfirmHandover(request.id) }
            )
        }
    }
}

@Composable
private fun OutgoingRequestsList(
    requests: List<ItemRequest>,
    onRequesterConfirm: (String) -> Unit
) {
    if (requests.isEmpty()) { EmptyState("No outgoing requests"); return }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(requests, key = { it.id }) { request ->
            OutgoingRequestCard(request = request, onRequesterConfirm = { onRequesterConfirm(request.id) })
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: ItemRequest,
    canAccept: Boolean,
    onAcceptWithDelivery: (DeliveryMethod) -> Unit,
    onReject: () -> Unit,
    onOwnerMarkShipped: () -> Unit,
    onOwnerConfirmHandover: () -> Unit
) {
    var showDeliveryDialog by remember { mutableStateOf(false) }

    if (showDeliveryDialog) {
        DeliveryMethodDialog(
            onSelect = { method -> onAcceptWithDelivery(method); showDeliveryDialog = false },
            onDismiss = { showDeliveryDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    StatusBadge(request.status)
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
private fun OutgoingRequestCard(
    request: ItemRequest,
    onRequesterConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        "to ${request.ownerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    StatusBadge(request.status)
                }
            }

            if (request.status == RequestStatus.ACCEPTED) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                RequesterDeliveryTracker(request = request, onConfirm = onRequesterConfirm)
            }
        }
    }
}

@Composable
private fun OwnerDeliveryTracker(
    request: ItemRequest,
    onMarkShipped: () -> Unit,
    onConfirmHandover: () -> Unit
) {
    val isCargo = request.deliveryMethod == DeliveryMethod.CARGO
    val steps = deliverySteps(isCargo)
    val currentIdx = steps.indexOfFirst { it.first == request.deliveryStatus }.coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DeliveryMethodLabel(isCargo)
        DeliveryStepRow(steps, currentIdx)

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
private fun RequesterDeliveryTracker(
    request: ItemRequest,
    onConfirm: () -> Unit
) {
    val isCargo = request.deliveryMethod == DeliveryMethod.CARGO
    val steps = deliverySteps(isCargo)
    val currentIdx = steps.indexOfFirst { it.first == request.deliveryStatus }.coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DeliveryMethodLabel(isCargo)
        DeliveryStepRow(steps, currentIdx)

        if (request.deliveryStatus == DeliveryStatus.SHIPPED) {
            if (!isCargo && request.requesterConfirmed) {
                Text(
                    "You confirmed receipt. Waiting for ${request.ownerName}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val label = if (isCargo) "Confirm Delivery" else "I've Received It"
                val note = if (isCargo) "You have 48 hours to confirm after delivery." else null
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (note != null) {
                        Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text(label, fontSize = 13.sp) }
                }
            }
        } else if (request.deliveryStatus == DeliveryStatus.PREPARING) {
            Text(
                "Waiting for ${request.ownerName} to prepare...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeliveryMethodLabel(isCargo: Boolean) {
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
}

@Composable
private fun DeliveryStepRow(steps: List<Triple<DeliveryStatus, ImageVector, String>>, currentIdx: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { idx, (_, icon, label) ->
            val done = idx <= currentIdx
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(32.dp).background(
                        if (done) AskiSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, modifier = Modifier.size(16.dp),
                        tint = if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (done) AskiSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (idx == currentIdx) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (idx < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.weight(0.5f),
                    color = if (idx < currentIdx) AskiSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 2.dp
                )
            }
        }
    }
}

private fun deliverySteps(isCargo: Boolean): List<Triple<DeliveryStatus, ImageVector, String>> =
    if (isCargo) listOf(
        Triple(DeliveryStatus.PREPARING, Icons.Default.Inventory2, "Preparing"),
        Triple(DeliveryStatus.SHIPPED, Icons.Default.LocalShipping, "Shipped"),
        Triple(DeliveryStatus.DELIVERED, Icons.Default.CheckCircle, "Delivered")
    ) else listOf(
        Triple(DeliveryStatus.PREPARING, Icons.Default.Inventory2, "Preparing"),
        Triple(DeliveryStatus.SHIPPED, Icons.Default.Handshake, "Ready"),
        Triple(DeliveryStatus.DELIVERED, Icons.Default.CheckCircle, "Handed Over")
    )

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
                    DeliveryOptionButton(Icons.Default.Handshake, "Hand-to-Hand", Modifier.weight(1f)) { onSelect(DeliveryMethod.HAND_TO_HAND) }
                    DeliveryOptionButton(Icons.Default.LocalShipping, "Cargo", Modifier.weight(1f)) { onSelect(DeliveryMethod.CARGO) }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun DeliveryOptionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
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
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

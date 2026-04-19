package com.example.aski.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.aski.model.Item
import com.example.aski.model.ItemCondition
import com.example.aski.model.ItemRequest
import com.example.aski.model.ItemStatus
import com.example.aski.model.RequestStatus
import com.example.aski.model.categories
import com.example.aski.ui.theme.AskiDarkBg
import com.example.aski.ui.theme.AskiError
import com.example.aski.ui.theme.AskiOnBgVariant
import com.example.aski.ui.theme.AskiSuccess
import com.example.aski.ui.theme.AskiWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: Item,
    isOwner: Boolean,
    isFavorite: Boolean,
    ownerName: String?,
    allRequests: List<ItemRequest> = emptyList(),
    hasRequested: Boolean = false,
    isRequestAccepted: Boolean = false,
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit,
    onOwnerClick: (() -> Unit)?,
    onRequestClick: (() -> Unit)? = null,
    onCancelRequestClick: (() -> Unit)? = null,
    onViewRequestsClick: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    onUpdateItem: (Item) -> Unit,
    onDeleteItem: (() -> Unit)?,
    onReportItem: ((String) -> Unit)?
) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(item.title) }
    var editDescription by remember { mutableStateOf(item.description) }
    var editCondition by remember { mutableStateOf(item.condition) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showCancelAcceptedDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val categoryName = categories.find { it.id == item.categoryId }?.name ?: "Other"
    val statusColor = when (item.status) {
        ItemStatus.AVAILABLE -> AskiSuccess
        ItemStatus.RESERVED -> AskiWarning
        ItemStatus.GIVEN -> AskiOnBgVariant
    }

    val activeRequests = remember(allRequests, item.id) {
        allRequests.filter { it.itemId == item.id && it.status != RequestStatus.REJECTED }
    }
    val activeRequestCount = activeRequests.size

    if (showCancelAcceptedDialog) {
        AlertDialog(
            onDismissRequest = { showCancelAcceptedDialog = false },
            title = { Text("Cancel Request") },
            text = { Text("Are you sure you want to cancel request?") },
            confirmButton = {
                TextButton(onClick = {
                    onCancelRequestClick?.invoke()
                    showCancelAcceptedDialog = false
                }) { Text("Yes", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelAcceptedDialog = false }) { Text("No") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete listing?") },
            text = { Text("This will permanently remove your listing.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteItem?.invoke()
                    showDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReportDialog) {
        ReportDialog(
            onReport = { reason ->
                onReportItem?.invoke(reason)
                showReportDialog = false
            },
            onDismiss = { showReportDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            // Image Carousel
            Box(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                if (item.imageUrls.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { item.imageUrls.size })
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        AsyncImage(
                            model = item.imageUrls[page],
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (item.imageUrls.size > 1) {
                        Row(
                            Modifier.wrapContentHeight().fillMaxWidth()
                                .align(Alignment.BottomCenter).padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(item.imageUrls.size) { i ->
                                val color = if (pagerState.currentPage == i) Color.White else Color.White.copy(alpha = 0.5f)
                                Box(Modifier.padding(2.dp).clip(CircleShape).background(color).size(8.dp))
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                )
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, AskiDarkBg)))
                )

                // Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.statusBarsPadding().padding(8.dp).size(40.dp)
                        .clip(CircleShape).background(Color(0x80000000))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // Top-right action buttons
                Row(
                    modifier = Modifier.statusBarsPadding().padding(12.dp).align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Share
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out \"${item.title}\" on aski!\naski://item/${item.id}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share item"))
                        },
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x80000000))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }

                    if (isOwner && item.status != ItemStatus.GIVEN) {
                        // Edit
                        IconButton(
                            onClick = {
                                if (isEditing) {
                                    onUpdateItem(item.copy(title = editTitle, description = editDescription, condition = editCondition))
                                }
                                isEditing = !isEditing
                            },
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x80000000))
                        ) {
                            Icon(
                                if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = null, tint = Color.White
                            )
                        }
                        // Delete
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x80000000))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AskiError)
                        }
                    }

                    if (!isOwner) {
                        // Report
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x80000000))
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = "Report", tint = Color.White)
                        }
                    }
                }

                // Status badge
                if (!isEditing) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                            .clip(RoundedCornerShape(20.dp)).background(statusColor.copy(alpha = 0.15f)),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                            Text(
                                item.status.name,
                                color = statusColor,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Content
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editTitle, onValueChange = { editTitle = it },
                        label = { Text("Title") }, singleLine = true,
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                        colors = authFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editDescription, onValueChange = { editDescription = it },
                        label = { Text("Description") }, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(), minLines = 3,
                        colors = authFieldColors()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Condition", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ItemCondition.entries.forEach { condition ->
                            FilterChip(
                                selected = editCondition == condition,
                                onClick = { editCondition = condition },
                                label = { Text(condition.name.replace("_", " "), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                border = null
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Yellow else Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(label = categoryName)
                        Chip(label = item.condition.name.replace("_", " "))
                        if (item.location.isNotBlank()) {
                            Chip(label = item.location)
                        }
                    }

                    if (!ownerName.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (onOwnerClick != null) Modifier.clickable { onOwnerClick() } else Modifier
                        ) {
                            Text("by ", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ownerName, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary)
                            if (onOwnerClick != null) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Description", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(item.description, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground, lineHeight = 26.sp)
                }

                Spacer(Modifier.height(100.dp))
            }
        }

        // Bottom CTA
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, AskiDarkBg.copy(alpha = 0.95f))))
                .padding(horizontal = 20.dp, vertical = 20.dp).navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (activeRequestCount > 0) {
                    val requestText = if (activeRequestCount == 1) "1 user is requesting this item" 
                                    else "$activeRequestCount users are requesting this item"
                    Text(
                        text = requestText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp).align(Alignment.Start)
                    )
                }
                
                if (!isOwner && item.status == ItemStatus.AVAILABLE) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { onChatClick(item.ownerId) },
                            modifier = Modifier.weight(1f).height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Chat")
                        }
                        if (isRequestAccepted) {
                            Button(
                                onClick = { showCancelAcceptedDialog = true },
                                modifier = Modifier.weight(1.5f).height(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AskiSuccess),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Text("Request Accepted", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else if (hasRequested) {
                            Button(
                                onClick = { onCancelRequestClick?.invoke() },
                                modifier = Modifier.weight(1.5f).height(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AskiError),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Text("Cancel Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    onRequestClick?.invoke()
                                    scope.launch { snackbarHostState.showSnackbar("Request sent!") }
                                },
                                modifier = Modifier.weight(1.5f).height(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Text("Request Item", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (isOwner && item.status != ItemStatus.GIVEN) {
                    Button(
                        onClick = { onViewRequestsClick?.invoke() },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("View Requests", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}

@Composable
fun ReportDialog(onReport: (String) -> Unit, onDismiss: () -> Unit) {
    val reasons = listOf("Inappropriate content", "Spam", "Already given away", "Fake listing", "Other")
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp, modifier = Modifier.width(280.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Report listing", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                reasons.forEach { reason ->
                    TextButton(
                        onClick = { onReport(reason) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(reason, modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun Chip(label: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

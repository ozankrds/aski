package com.example.aski.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aski.model.Item
import com.example.aski.model.ItemCondition
import com.example.aski.model.ItemStatus
import com.example.aski.model.categories

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: Item,
    isOwner: Boolean,
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit,
    onUpdateItem: (Item) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(item.title) }
    var editDescription by remember { mutableStateOf(item.description) }
    var editCondition by remember { mutableStateOf(item.condition) }
    var editStatus by remember { mutableStateOf(item.status) }

    val categoryName = categories.find { it.id == item.categoryId }?.name ?: "Other"
    val statusColor = when (item.status) {
        ItemStatus.AVAILABLE -> Color(0xFF2ECC71)
        ItemStatus.RESERVED -> Color(0xFFF39C12)
        ItemStatus.GIVEN -> Color(0xFF666666)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            // Hero image
            Box(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                }

                // Top gradient for back button visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent))
                        )
                )

                // Bottom gradient for content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0A0A0A)))
                        )
                )

                // Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // Edit button
                if (isOwner) {
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                onUpdateItem(item.copy(
                                    title = editTitle,
                                    description = editDescription,
                                    condition = editCondition,
                                    status = editStatus
                                ))
                            }
                            isEditing = !isEditing
                        },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x80000000))
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                // Status badge overlay
                if (!isEditing) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                            Text(item.status.name, color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Content
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = authFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Description") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
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
                    Spacer(Modifier.height(16.dp))
                    Text("Status", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ItemStatus.entries.forEach { status ->
                            FilterChip(
                                selected = editStatus == status,
                                onClick = { editStatus = status },
                                label = { Text(status.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                border = null
                            )
                        }
                    }
                } else {
                    Text(item.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(label = categoryName)
                        Chip(label = item.condition.name.replace("_", " "))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Description",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 26.sp
                    )
                }

                // Bottom spacing for button
                Spacer(Modifier.height(100.dp))
            }
        }

        // Bottom CTA
        if (!isOwner && item.status == ItemStatus.AVAILABLE) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xF00A0A0A)))
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = { onChatClick(item.ownerId) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Request Item", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Chip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
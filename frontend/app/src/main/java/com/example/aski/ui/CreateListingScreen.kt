package com.example.aski.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
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
import com.example.aski.model.ItemCondition
import com.example.aski.model.categories

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onPostItem: (String, String, Int, ItemCondition, String, List<Uri>) -> Unit,
    onBackClick: () -> Unit,
    isUploading: Boolean = false,
    errorMessage: String? = null
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.first { it.id != 0 }.id) }
    var selectedCondition by remember { mutableStateOf(ItemCondition.USED_GOOD) }
    val imageUris = remember { mutableStateListOf<Uri>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null && imageUris.size < 5) imageUris.add(uri) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Listing") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Photos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${imageUris.size}/5", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(imageUris) { index, uri ->
                    Box(modifier = Modifier.size(96.dp)) {
                        AsyncImage(
                            model = uri, contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { imageUris.removeAt(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (imageUris.size < 5) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp))
                                Text("Add photo", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            val fieldColors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            )

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") }, singleLine = true,
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description") }, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(), minLines = 3, colors = fieldColors
            )

            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Location") }, singleLine = true,
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            // Category dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: ""
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                OutlinedTextField(
                    value = currentCategoryName, onValueChange = {}, readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = fieldColors
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.filter { it.id != 0 }.forEach { category ->
                        DropdownMenuItem(text = { Text(category.name) },
                            onClick = { selectedCategoryId = category.id; categoryExpanded = false })
                    }
                }
            }

            // Condition chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Condition", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemCondition.entries.forEach { condition ->
                        val label = condition.name.replace("_", " ")
                            .lowercase().replaceFirstChar { it.uppercase() }
                        FilterChip(
                            selected = selectedCondition == condition,
                            onClick = { selectedCondition = condition },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = selectedCondition == condition,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank()) return@Button
                    onPostItem(title, description, selectedCategoryId, selectedCondition, location.trim(), imageUris.toList())
                },
                enabled = title.isNotBlank() && description.isNotBlank() && !isUploading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Post Item", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

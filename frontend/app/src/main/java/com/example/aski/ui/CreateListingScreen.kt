package com.example.aski.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aski.model.ItemCondition
import com.example.aski.model.categories
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.tasks.await
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onPostItem: (String, String, Int, ItemCondition, String) -> Unit,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.first { it.id != 0 }.id) }
    var selectedCondition by remember { mutableStateOf(ItemCondition.USED_GOOD) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Photo", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Add photo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Category Dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: ""
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = currentCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.filter { it.id != 0 }.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = { selectedCategoryId = category.id; categoryExpanded = false }
                        )
                    }
                }
            }

            // Condition Dropdown
            var conditionExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = conditionExpanded,
                onExpandedChange = { conditionExpanded = !conditionExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCondition.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Condition") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = conditionExpanded, onDismissRequest = { conditionExpanded = false }) {
                    ItemCondition.entries.forEach { condition ->
                        DropdownMenuItem(
                            text = { Text(condition.name) },
                            onClick = { selectedCondition = condition; conditionExpanded = false }
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
                    scope.launch {
                        isUploading = true
                        errorMessage = null
                        try {
                            val imageUrl = if (imageUri != null) {
                                uploadImageToStorage(imageUri!!)
                            } else ""
                            onPostItem(title, description, selectedCategoryId, selectedCondition, imageUrl)
                        } catch (e: Exception) {
                            errorMessage = "Failed to upload image: ${e.message}"
                        } finally {
                            isUploading = false
                        }
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank() && !isUploading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Post Item")
                }
            }
        }
    }
}

private suspend fun uploadImageToStorage(uri: Uri): String {
    val ref = Firebase.storage.reference.child("items/${UUID.randomUUID()}.jpg")
    ref.putFile(uri).await()  // bu await() kotlinx-coroutines-play-services'ten geliyor
    return ref.downloadUrl.await().toString()
}
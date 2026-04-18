package com.example.aski.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aski.model.Item
import com.example.aski.model.ItemRequest
import com.example.aski.model.ItemStatus
import com.example.aski.model.RequestStatus
import com.example.aski.model.User
import com.example.aski.ui.theme.AskiOnBgVariant
import com.example.aski.ui.theme.AskiSuccess
import com.example.aski.ui.theme.AskiWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    userItems: List<Item>,
    favoriteItems: List<Item>,
    sentRequests: List<ItemRequest>,
    isLoading: Boolean,
    profileError: String?,
    onItemClick: (String) -> Unit,
    onMessagesClick: () -> Unit,
    onRequestsClick: () -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onUpdateProfile: (name: String, newPassword: String?, currentPassword: String?, photoUri: Uri?) -> Unit,
    onClearError: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember(user) { mutableStateOf(user?.name ?: "") }
    var editPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) photoUri = uri }
    )

    val rawItems = if (selectedTab == 0) userItems else favoriteItems
    // Sort: Available & Reserved first, Given last. Within those, newest first.
    val sortedItems = remember(rawItems) {
        rawItems.sortedWith(
            compareBy<Item> { it.status == ItemStatus.GIVEN }
                .thenByDescending { it.createdAt }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                onUpdateProfile(editName, editPassword.ifBlank { null }, currentPassword.ifBlank { null }, photoUri)
                                editPassword = ""
                                currentPassword = ""
                                photoUri = null
                                isEditing = false
                            } else {
                                isEditing = true
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Save" else "Edit profile"
                            )
                        }
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Profile header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .clickable(enabled = isEditing) {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val photoSource: Any? = photoUri ?: user?.photoUrl?.ifBlank { null }
                        if (photoSource != null) {
                            AsyncImage(
                                model = photoSource,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val initial = user?.name?.trim()?.takeIf { it.isNotEmpty() }?.take(1)?.uppercase() ?: "?"
                            Text(
                                initial,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (isEditing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = authFieldColors()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editPassword,
                            onValueChange = { editPassword = it },
                            label = { Text("New Password (leave blank to keep)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = authFieldColors()
                        )
                        if (editPassword.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = { currentPassword = it },
                                label = { Text("Current Password (required to change)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                colors = authFieldColors()
                            )
                        }
                        if (!profileError.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(profileError, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.clickable { onClearError() })
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                user?.name?.ifBlank { "Unknown" } ?: "Unknown",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (user?.isVerified == true) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = AskiSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            user?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onMessagesClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Messages", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onRequestsClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Received", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(8.dp))
            }

            // Stats row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "Listed", value = userItems.size.toString())
                    StatItem(label = "Karma", value = user?.karmaPoints?.toString() ?: "0")
                    StatItem(
                        label = "Rating",
                        value = if (user?.ratingCount == 0) "-" else "%.1f".format(user?.rating ?: 0.0)
                    )
                    StatItem(
                        label = "Given",
                        value = user?.givenCount?.toString() ?: userItems.count { it.status == ItemStatus.GIVEN }.toString()
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            // Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Listings") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Favorites") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Sent") }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            when (selectedTab) {
                0, 1 -> {
                    val emptyMessage = if (selectedTab == 0) "No listings yet" else "No favorites yet"
                    if (sortedItems.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(sortedItems, key = { it.id }) { item ->
                            ProfileItemRow(item = item, onClick = { onItemClick(item.id) })
                        }
                    }
                }
                2 -> {
                    if (sentRequests.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text("No requests sent", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(sentRequests, key = { it.id }) { request ->
                            SentRequestRow(request = request, onClick = { onItemClick(request.itemId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SentRequestRow(request: ItemRequest, onClick: () -> Unit) {
    val statusColor = when (request.status) {
        RequestStatus.PENDING -> AskiWarning
        RequestStatus.ACCEPTED -> AskiSuccess
        RequestStatus.REJECTED -> Color(0xFFE74C3C)
        RequestStatus.COMPLETED -> AskiOnBgVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (request.itemImageUrl.isNotBlank()) {
                AsyncImage(
                    model = request.itemImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(request.itemTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Text("to ${request.ownerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(request.status.name, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProfileItemRow(item: Item, onClick: () -> Unit) {
    val statusColor = when (item.status) {
        ItemStatus.AVAILABLE -> AskiSuccess
        ItemStatus.RESERVED -> AskiWarning
        ItemStatus.GIVEN -> AskiOnBgVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val thumbnail = item.imageUrls.firstOrNull()
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Text(item.condition.name.replace("_", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(item.status.name, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outline
    )
}

package com.example.aski.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aski.model.Item
import com.example.aski.model.ItemStatus
import com.example.aski.model.User
import com.example.aski.ui.theme.AskiOnBgVariant
import com.example.aski.ui.theme.AskiSuccess
import com.example.aski.ui.theme.AskiWarning
import com.example.aski.ui.theme.ColorPreset
import com.example.aski.ui.theme.LocalThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    userItems: List<Item>,
    favoriteItems: List<Item>,
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
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout",
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            Text(initial, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        if (isEditing) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = editName, onValueChange = { editName = it },
                            label = { Text("Name") }, singleLine = true,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                            colors = authFieldColors()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editPassword, onValueChange = { editPassword = it },
                            label = { Text("New password (blank = keep)") }, singleLine = true,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = authFieldColors()
                        )
                        if (editPassword.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = currentPassword, onValueChange = { currentPassword = it },
                                label = { Text("Current password") }, singleLine = true,
                                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (user?.isVerified == true) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Verified",
                                    tint = AskiSuccess, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Action buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onMessagesClick,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Messages", fontWeight = FontWeight.Medium, maxLines = 1,
                                overflow = TextOverflow.Clip, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onRequestsClick,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("My Requests", fontWeight = FontWeight.Medium, maxLines = 1,
                                overflow = TextOverflow.Clip, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            // Stats row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "Listed", value = userItems.size.toString())
                    VerticalDivider(modifier = Modifier.height(36.dp), color = MaterialTheme.colorScheme.outline)
                    StatItem(label = "Given", value = user?.givenCount?.toString()
                        ?: userItems.count { it.status == ItemStatus.GIVEN }.toString())
                    VerticalDivider(modifier = Modifier.height(36.dp), color = MaterialTheme.colorScheme.outline)
                    StatItem(label = "Karma", value = user?.karmaPoints?.toString() ?: "0")
                    VerticalDivider(modifier = Modifier.height(36.dp), color = MaterialTheme.colorScheme.outline)
                    StatItem(
                        label = "Rating",
                        value = if (user?.ratingCount == 0) "—" else "%.1f".format(user?.rating ?: 0.0)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            // Theme & appearance settings
            item {
                val themeConfig = LocalThemeConfig.current
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text("Appearance", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (themeConfig.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Dark mode", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ColorPreset.entries.forEach { preset ->
                                val selected = themeConfig.preset == preset
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(preset.swatch)
                                        .then(
                                            if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { themeConfig.onSetPreset(preset) }
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = themeConfig.isDark,
                                onCheckedChange = { themeConfig.onToggleDark() },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
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
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("My Listings") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("Favorites") })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(4.dp))
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
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, style = MaterialTheme.typography.labelSmall,
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
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
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
            Text(item.title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(item.condition.name.replace("_", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(statusColor.copy(alpha = 0.12f))
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

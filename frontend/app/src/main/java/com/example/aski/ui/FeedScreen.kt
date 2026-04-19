package com.example.aski.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.example.aski.model.User
import com.example.aski.model.categories
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedScreen(
    items: List<Item>,
    isLoading: Boolean,
    favoriteIds: List<String>,
    totalUnread: Int,
    onItemClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCreateListingClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRefresh: () -> Unit,
    onSearchUsers: (String) -> Unit,
    searchUsersResults: List<User>,
    onUserClick: (String) -> Unit,
    currentUser: User? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var userSearchQuery by remember { mutableStateOf("") }
    
    // Effective filters (applied to the list)
    var appliedCategoryIds by remember { mutableStateOf(setOf(0)) }
    var appliedConditions by remember { mutableStateOf(setOf<ItemCondition>()) }
    
    // Temporary filters (shown in the bottom sheet before clicking Apply)
    var tempCategoryIds by remember { mutableStateOf(setOf(0)) }
    var tempConditions by remember { mutableStateOf(setOf<ItemCondition>()) }
    
    var isUserSearchActive by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val displayed = remember(items, searchQuery, appliedCategoryIds, appliedConditions) {
        items.filter {
            val matchesCategory = if (appliedCategoryIds.contains(0)) true 
                                 else appliedCategoryIds.contains(it.categoryId)
            val matchesCondition = if (appliedConditions.isEmpty()) true 
                                  else appliedConditions.contains(it.condition)
            
            matchesCategory && matchesCondition && it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "aski",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                searchQuery = ""
                                appliedCategoryIds = setOf(0)
                                appliedConditions = emptySet()
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { 
                                isUserSearchActive = !isUserSearchActive 
                                if (!isUserSearchActive) userSearchQuery = ""
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isUserSearchActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.PersonSearch,
                                contentDescription = "Search Users",
                                tint = if (isUserSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        BadgedBox(
                            badge = { if (totalUnread > 0) Badge { Text(totalUnread.toString()) } }
                        ) {
                            IconButton(onClick = onProfileClick) {
                                if (currentUser?.photoUrl?.isNotBlank() == true) {
                                    AsyncImage(
                                        model = currentUser.photoUrl,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(30.dp))
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )

                if (isUserSearchActive) {
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = {
                            userSearchQuery = it
                            onSearchUsers(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search users...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (userSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    userSearchQuery = ""
                                    onSearchUsers("")
                                }) { Icon(Icons.Default.Close, contentDescription = "Clear search") }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateListingClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "Give") }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
        // Main Feed Content
        LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search items...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp)
                        )
                        
                        val isFilterActive = showFilterSheet || !appliedCategoryIds.contains(0) || appliedConditions.isNotEmpty()
                        Surface(
                            onClick = { 
                                tempCategoryIds = appliedCategoryIds
                                tempConditions = appliedConditions
                                showFilterSheet = true 
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isFilterActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Filter",
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = appliedCategoryIds.contains(category.id),
                                onClick = { 
                                    appliedCategoryIds = if (category.id == 0) {
                                        setOf(0)
                                    } else {
                                        val newSet = appliedCategoryIds - 0
                                        if (newSet.contains(category.id)) {
                                            val filtered = newSet - category.id
                                            if (filtered.isEmpty()) setOf(0) else filtered
                                        } else {
                                            newSet + category.id
                                        }
                                    }
                                },
                                label = { Text(category.name) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (isLoading) {
                    item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                } else if (displayed.isEmpty()) {
                    item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("Nothing found") } }
                } else {
                    items(displayed, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            isFavorite = favoriteIds.contains(item.id),
                            onFavoriteClick = { onToggleFavorite(item.id) },
                            onClick = { onItemClick(item.id) }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            Text("That's all!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // User Search Results Overlay
            if (isUserSearchActive && userSearchQuery.isNotBlank()) {
                // Translucent overlay to dim the background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { /* Could close search here if desired */ }
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .heightIn(max = 400.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    if (searchUsersResults.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn {
                            items(searchUsersResults) { user ->
                                ListItem(
                                    headlineContent = { Text(user.name) },
                                    supportingContent = { Text(user.email, fontSize = 12.sp) },
                                    leadingContent = {
                                        if (user.photoUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = user.photoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(40.dp))
                                        }
                                    },
                                    modifier = Modifier.clickable { 
                                        onUserClick(user.id)
                                        isUserSearchActive = false
                                        userSearchQuery = ""
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF1A1A1A).copy(alpha = 0.92f), // Darker translucent background
            scrimColor = Color.Black.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    "Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = tempCategoryIds.contains(category.id),
                            onClick = { 
                                tempCategoryIds = if (category.id == 0) {
                                    setOf(0)
                                } else {
                                    val newSet = tempCategoryIds - 0
                                    if (newSet.contains(category.id)) {
                                        val filtered = newSet - category.id
                                        if (filtered.isEmpty()) setOf(0) else filtered
                                    } else {
                                        newSet + category.id
                                    }
                                }
                            },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White.copy(alpha = 0.7f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Condition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = tempConditions.isEmpty(),
                        onClick = { tempConditions = emptySet() },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White.copy(alpha = 0.7f),
                            selectedLabelColor = Color.White
                        )
                    )
                    ItemCondition.values().forEach { condition ->
                        FilterChip(
                            selected = tempConditions.contains(condition),
                            onClick = { 
                                tempConditions = if (tempConditions.contains(condition)) {
                                    tempConditions - condition
                                } else {
                                    tempConditions + condition
                                }
                            },
                            label = { Text(condition.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White.copy(alpha = 0.7f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = { 
                        appliedCategoryIds = tempCategoryIds
                        appliedConditions = tempConditions
                        showFilterSheet = false 
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Apply Filters", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                TextButton(
                    onClick = {
                        tempCategoryIds = setOf(0)
                        tempConditions = emptySet()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                ) {
                    Text("Reset All", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ItemCard(item: Item, isFavorite: Boolean, onFavoriteClick: () -> Unit, onClick: () -> Unit) {
    val categoryName = categories.find { it.id == item.categoryId }?.name ?: "Other"
    val statusColor = when (item.status) {
        ItemStatus.AVAILABLE -> Color(0xFF2ECC71)
        ItemStatus.RESERVED -> Color(0xFFF39C12)
        ItemStatus.GIVEN -> Color(0xFF666666)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(260.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
            val thumbnail = item.imageUrls.firstOrNull()
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))))

            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(5.dp))
                    Text(item.status.name, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = { onFavoriteClick() },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Yellow else Color.White
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1)
                Text("$categoryName · ${item.condition.name.replace("_", " ")}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

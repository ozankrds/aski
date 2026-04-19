package com.example.aski.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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

private enum class SearchMode { ITEMS, PEOPLE }

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
    var searchMode by remember { mutableStateOf(SearchMode.ITEMS) }

    // Effective filters (applied to the list)
    var appliedCategoryIds by remember { mutableStateOf(setOf(0)) }
    var appliedConditions by remember { mutableStateOf(setOf<ItemCondition>()) }

    // Temporary filters (shown in the bottom sheet before clicking Apply)
    var tempCategoryIds by remember { mutableStateOf(setOf(0)) }
    var tempConditions by remember { mutableStateOf(setOf<ItemCondition>()) }

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
            TopAppBar(
                title = {
                    Text(
                        "aski",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            searchQuery = ""
                            searchMode = SearchMode.ITEMS
                            appliedCategoryIds = setOf(0)
                            appliedConditions = emptySet()
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                    )
                },
                actions = {
                    BadgedBox(
                        badge = { if (totalUnread > 0) Badge { Text(totalUnread.toString()) } }
                    ) {
                        IconButton(onClick = onProfileClick) {
                            if (currentUser?.photoUrl?.isNotBlank() == true) {
                                AsyncImage(
                                    model = currentUser.photoUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(30.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Profile",
                                    modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
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
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    if (searchMode == SearchMode.PEOPLE) onSearchUsers(it)
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(if (searchMode == SearchMode.PEOPLE) "Search people..." else "Search items...")
                                },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            if (searchMode == SearchMode.PEOPLE) onSearchUsers("")
                                        }) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp)
                            )

                            if (searchMode == SearchMode.ITEMS) {
                                val isFilterActive = !appliedCategoryIds.contains(0) || appliedConditions.isNotEmpty()
                                Surface(
                                    onClick = {
                                        tempCategoryIds = appliedCategoryIds
                                        tempConditions = appliedConditions
                                        showFilterSheet = true
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isFilterActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Tune, contentDescription = "Filter",
                                            tint = if (isFilterActive) MaterialTheme.colorScheme.primary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Mode toggle
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = searchMode == SearchMode.ITEMS,
                                onClick = { searchMode = SearchMode.ITEMS },
                                label = { Text("Items") },
                                leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = searchMode == SearchMode.PEOPLE,
                                onClick = {
                                    searchMode = SearchMode.PEOPLE
                                    if (searchQuery.isNotBlank()) onSearchUsers(searchQuery)
                                },
                                label = { Text("People") },
                                leadingIcon = { Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }

                if (searchMode == SearchMode.ITEMS) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
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
                    }
                }

                if (searchMode == SearchMode.PEOPLE) {
                    if (searchQuery.isBlank()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.PersonSearch, contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Search for people by name", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else if (searchUsersResults.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(searchUsersResults, key = { it.id }) { user ->
                            ListItem(
                                headlineContent = { Text(user.name, fontWeight = FontWeight.Medium) },
                                supportingContent = { Text(user.email, fontSize = 12.sp) },
                                leadingContent = {
                                    if (user.photoUrl.isNotBlank()) {
                                        AsyncImage(model = user.photoUrl, contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop)
                                    } else {
                                        Box(Modifier.size(40.dp).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(user.name.take(1).uppercase(), fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                modifier = Modifier.clickable { onUserClick(user.id) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                } else {
                    val isFiltered = !appliedCategoryIds.contains(0) || appliedConditions.isNotEmpty() || searchQuery.isNotBlank()
                    if (isLoading) {
                        items(4) { ItemCardSkeleton() }
                    } else if (displayed.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        if (isFiltered) "No items match your filters" else "No items yet",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isFiltered) {
                                        OutlinedButton(onClick = {
                                            appliedCategoryIds = setOf(0)
                                            appliedConditions = emptySet()
                                            tempCategoryIds = setOf(0)
                                            tempConditions = emptySet()
                                            searchQuery = ""
                                        }) { Text("Clear filters") }
                                    }
                                }
                            }
                        }
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
                                Text("That's all!", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    "Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Condition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                            colors = FilterChipDefaults.filterChipColors()
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

            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                        Spacer(Modifier.width(5.dp))
                        Text(item.status.name, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val isNew = System.currentTimeMillis() - item.createdAt < 24 * 60 * 60 * 1000L
                if (isNew) {
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), shape = RoundedCornerShape(20.dp)) {
                        Text("New", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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

@Composable
fun ItemCardSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -600f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF2A2A2A), Color(0xFF3D3D3D), Color(0xFF2A2A2A)),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 600f, 0f)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .height(260.dp)
            .background(brush)
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth(0.6f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.15f)))
            Box(Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.1f)))
        }
    }
    Spacer(Modifier.height(16.dp))
}

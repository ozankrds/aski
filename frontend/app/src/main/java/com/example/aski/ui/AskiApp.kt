package com.example.aski.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aski.model.DeliveryMethod
import com.example.aski.model.ItemRequest
import com.example.aski.ui.viewmodel.AuthState
import com.example.aski.ui.viewmodel.AuthViewModel
import com.example.aski.ui.viewmodel.ChatViewModel
import com.example.aski.ui.viewmodel.ItemViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AskiApp(
    deepLinkItemId: String? = null,
    deepLinkChatId: String? = null
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val itemViewModel: ItemViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val authState by authViewModel.authState.collectAsState()
    val profileError by authViewModel.profileError.collectAsState()
    val currentUser = (authState as? AuthState.Authenticated)?.user
    val isProfileLoading = authState is AuthState.Loading

    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let {
            itemViewModel.observeUserItems(it)
            itemViewModel.observeIncomingRequests(it)
            itemViewModel.observeOutgoingRequests(it)
            chatViewModel.observeChats(it)
            // Save FCM token whenever user logs in
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                authViewModel.saveFcmToken(token)
            }
        }
    }

    // Handle deep links after nav is ready
    LaunchedEffect(deepLinkItemId) {
        if (deepLinkItemId != null) {
            navController.navigate(Screen.ItemDetail.createRoute(deepLinkItemId))
        }
    }
    LaunchedEffect(deepLinkChatId) {
        if (deepLinkChatId != null) {
            navController.navigate(Screen.Chat.createRoute(deepLinkChatId))
        }
    }

    val totalUnread by chatViewModel.totalUnread.collectAsState()
    var ratingTargetRequest by remember { mutableStateOf<ItemRequest?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            LaunchedEffect(authState) {
                when (authState) {
                    is AuthState.Loading -> {}
                    is AuthState.Authenticated -> navController.navigate(Screen.Feed.route) {
                        popUpTo("splash") { inclusive = true }
                    }
                    else -> navController.navigate(Screen.Login.route) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
            )
        }
        composable(Screen.Signup.route) {
            SignupScreen(
                viewModel = authViewModel,
                onSignupSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.Feed.route) {
            val items by itemViewModel.filteredItems.collectAsState()
            val isItemsLoading by itemViewModel.isLoading.collectAsState()
            val searchUsersResults by authViewModel.searchResults.collectAsState()

            FeedScreen(
                items = items,
                isLoading = isItemsLoading,
                favoriteIds = currentUser?.favoriteIds ?: emptyList(),
                totalUnread = totalUnread,
                onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) },
                onToggleFavorite = {
                    if (currentUser == null) navController.navigate(Screen.Login.route)
                    else authViewModel.toggleFavorite(it)
                },
                onCreateListingClick = {
                    if (currentUser == null) navController.navigate(Screen.Login.route)
                    else navController.navigate(Screen.CreateListing.route)
                },
                onProfileClick = {
                    if (currentUser == null) navController.navigate(Screen.Login.route)
                    else navController.navigate(Screen.Profile.route)
                },
                onRefresh = { itemViewModel.refresh() },
                onSearchUsers = { authViewModel.searchUsers(it) },
                searchUsersResults = searchUsersResults,
                onUserClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                currentUser = currentUser
            )
        }
        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
            val feedItems by itemViewModel.filteredItems.collectAsState()
            val userItems by itemViewModel.userItems.collectAsState()
            val outgoingRequests by itemViewModel.outgoingRequests.collectAsState()
            val itemRequestsMap by itemViewModel.itemRequests.collectAsState()

            val item = remember(itemId, feedItems, userItems) {
                feedItems.find { it.id == itemId } ?: userItems.find { it.id == itemId }
            }
            var localItem by remember { mutableStateOf<com.example.aski.model.Item?>(null) }
            var ownerName by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(itemId) {
                if (item == null) localItem = itemViewModel.getItem(itemId)
                itemViewModel.observeItemRequests(itemId)
            }

            val displayItem = item ?: localItem

            LaunchedEffect(displayItem?.ownerId) {
                displayItem?.ownerId?.let { ownerId ->
                    ownerName = authViewModel.getUserName(ownerId)
                }
            }

            displayItem?.let { itm ->
                val isOwner = itm.ownerId == currentUser?.id
                val userRequest = outgoingRequests.find { it.itemId == itm.id && it.requesterId == currentUser?.id }
                val hasRequested = userRequest != null && userRequest.status != com.example.aski.model.RequestStatus.REJECTED
                val isRequestAccepted = userRequest?.status == com.example.aski.model.RequestStatus.ACCEPTED
                val itemRequests = itemRequestsMap[itm.id] ?: emptyList()

                ItemDetailScreen(
                    item = itm,
                    isOwner = isOwner,
                    isFavorite = currentUser?.favoriteIds?.contains(itm.id) == true,
                    ownerName = if (isOwner) null else ownerName,
                    allRequests = itemRequests,
                    hasRequested = hasRequested,
                    isRequestAccepted = isRequestAccepted,
                    onBackClick = { navController.popBackStack() },
                    onOwnerClick = if (!isOwner) {
                        { navController.navigate(Screen.UserProfile.createRoute(itm.ownerId)) }
                    } else null,
                    onChatClick = { ownerId ->
                        if (currentUser == null) {
                            navController.navigate(Screen.Login.route)
                        } else {
                            scope.launch {
                                val chat = chatViewModel.getOrCreateChat(itm.id, currentUser.id, ownerId, itm.imageUrls.firstOrNull() ?: "")
                                if (chat != null) {
                                    chatViewModel.sendMessage(chat.id, currentUser.id, "I'm interested in ${itm.title}")
                                    navController.navigate(Screen.Chat.createRoute(chat.id))
                                }
                            }
                        }
                    },
                    onRequestClick = {
                        if (currentUser == null) navController.navigate(Screen.Login.route)
                        else itemViewModel.createRequest(itm, currentUser.id, currentUser.name, ownerName ?: "User")
                    },
                    onCancelRequestClick = {
                        currentUser?.let { user ->
                            itemViewModel.cancelRequest(itm.id, user.id)
                        }
                    },
                    onViewRequestsClick = { navController.navigate(Screen.ItemRequests.createRoute(itm.id)) },
                    onToggleFavorite = { authViewModel.toggleFavorite(itm.id) },
                    onUpdateItem = { updatedItem ->
                        itemViewModel.updateItem(updatedItem)
                        if (item == null) localItem = updatedItem
                    },
                    onDeleteItem = if (isOwner) {
                        {
                            itemViewModel.deleteItem(itm.id) {
                                navController.popBackStack()
                            }
                        }
                    } else null,
                    onReportItem = if (!isOwner && currentUser != null) {
                        { reason -> itemViewModel.reportItem(itm.id, currentUser.id, reason) }
                    } else null
                )
            }
        }
        composable(Screen.CreateListing.route) {
            var isUploading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            CreateListingScreen(
                onPostItem = { title, desc, catId, cond, location, uris ->
                    currentUser?.id?.let { uid ->
                        isUploading = true
                        errorMessage = null
                        itemViewModel.addItem(
                            ownerId = uid,
                            title = title,
                            description = desc,
                            categoryId = catId,
                            condition = cond,
                            location = location,
                            imageUris = uris,
                            onSuccess = {
                                isUploading = false
                                navController.popBackStack()
                            },
                            onError = { err ->
                                isUploading = false
                                errorMessage = err
                            }
                        )
                    }
                },
                onBackClick = { navController.popBackStack() },
                isUploading = isUploading,
                errorMessage = errorMessage
            )
        }
        composable(Screen.Profile.route) {
            val userItems by itemViewModel.userItems.collectAsState()
            val allItems by itemViewModel.feedItems.collectAsState()

            ProfileScreen(
                user = currentUser,
                userItems = userItems,
                favoriteItems = allItems.filter { currentUser?.favoriteIds?.contains(it.id) == true },
                isLoading = isProfileLoading,
                profileError = profileError,
                onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) },
                onMessagesClick = { navController.navigate(Screen.ChatList.route) },
                onRequestsClick = { navController.navigate(Screen.Requests.route) },
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onUpdateProfile = { name, newPassword, currentPassword, photoUri ->
                    authViewModel.updateProfile(name, newPassword, currentPassword, photoUri)
                },
                onClearError = { authViewModel.clearProfileError() }
            )
        }
        composable(Screen.ChatList.route) {
            val chats by chatViewModel.chats.collectAsState()
            val userNames by chatViewModel.userNames.collectAsState()
            currentUser?.let { user ->
                ChatListScreen(
                    chats = chats,
                    currentUserId = user.id,
                    userNames = userNames,
                    onFetchName = { chatViewModel.fetchUserName(it) },
                    onChatClick = { chatId -> navController.navigate(Screen.Chat.createRoute(chatId)) },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val messages by chatViewModel.messages.collectAsState()
            val otherUser by chatViewModel.otherUser.collectAsState()

            LaunchedEffect(chatId) {
                chatViewModel.observeMessages(chatId)
                currentUser?.id?.let { chatViewModel.markAsRead(chatId, it) }
            }

            currentUser?.let { user ->
                val chat = chatViewModel.chats.value.find { it.id == chatId }
                val otherUserId = chat?.participants?.firstOrNull { it != user.id }

                LaunchedEffect(otherUserId) {
                    otherUserId?.let { chatViewModel.fetchOtherUser(it) }
                }

                ChatScreen(
                    chatId = chatId,
                    messages = messages,
                    currentUserId = user.id,
                    otherUserName = otherUser?.name,
                    onSendMessage = { content -> chatViewModel.sendMessage(chatId, user.id, content) },
                    onSendImage = { uri -> chatViewModel.sendImage(chatId, user.id, uri) },
                    onRateUser = { rating -> otherUserId?.let { authViewModel.rateUser(it, rating) } },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            var targetUser by remember { mutableStateOf<com.example.aski.model.User?>(null) }
            var targetItems by remember { mutableStateOf<List<com.example.aski.model.Item>>(emptyList()) }

            LaunchedEffect(userId) {
                targetUser = authViewModel.getUserById(userId)
                targetItems = itemViewModel.getUserItems(userId)
            }

            UserProfileScreen(
                user = targetUser,
                items = targetItems,
                onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) },
                onChatClick = { ownerId ->
                    if (currentUser == null) {
                        navController.navigate(Screen.Login.route)
                    } else {
                        scope.launch {
                            val itemId = targetItems.firstOrNull()?.id ?: ""
                            val chat = chatViewModel.getOrCreateChat(itemId, currentUser.id, ownerId)
                            if (chat != null) {
                                navController.navigate(Screen.Chat.createRoute(chat.id))
                            }
                        }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Requests.route) {
            val incomingRequests by itemViewModel.incomingRequests.collectAsState()
            val outgoingRequests by itemViewModel.outgoingRequests.collectAsState()
            val ratings by itemViewModel.ratings.collectAsState()
            RequestsScreen(
                incomingRequests = incomingRequests,
                outgoingRequests = outgoingRequests,
                ratings = ratings,
                onAcceptWithDelivery = { requestId, method ->
                    itemViewModel.acceptRequestWithDelivery(requestId, method)
                },
                onRejectRequest = { requestId ->
                    itemViewModel.rejectRequest(requestId)
                },
                onOwnerMarkShipped = { requestId ->
                    itemViewModel.ownerMarkShipped(requestId)
                },
                onOwnerConfirmHandover = { requestId ->
                    itemViewModel.ownerConfirmHandover(requestId) {}
                },
                onRequesterConfirm = { requestId ->
                    val request = outgoingRequests.find { it.id == requestId }
                    itemViewModel.requesterConfirmDelivery(requestId) {
                        request?.let {
                            authViewModel.incrementKarmaAndGiven(it.ownerId)
                            ratingTargetRequest = it
                            showRatingDialog = true
                        }
                    }
                },
                onRatingClick = { request ->
                    ratingTargetRequest = request
                    showRatingDialog = true
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ItemRequests.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val incomingRequests by itemViewModel.incomingRequests.collectAsState()
            val ratings by itemViewModel.ratings.collectAsState()
            ItemRequestsScreen(
                itemId = itemId,
                allRequests = incomingRequests,
                ratings = ratings,
                onAcceptWithDelivery = { requestId, method ->
                    itemViewModel.acceptRequestWithDelivery(requestId, method)
                },
                onRejectRequest = { requestId ->
                    itemViewModel.rejectRequest(requestId)
                },
                onOwnerMarkShipped = { requestId ->
                    itemViewModel.ownerMarkShipped(requestId)
                },
                onOwnerConfirmHandover = { requestId ->
                    itemViewModel.ownerConfirmHandover(requestId) {}
                },
                onRatingClick = { request ->
                    ratingTargetRequest = request
                    showRatingDialog = true
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }

    if (showRatingDialog && ratingTargetRequest != null && currentUser != null) {
        val ratings by itemViewModel.ratings.collectAsState()
        val existingRating = ratings[ratingTargetRequest!!.itemId]
        val isRequester = ratingTargetRequest!!.requesterId == currentUser.id
        val targetUserId = if (isRequester) ratingTargetRequest!!.ownerId else ratingTargetRequest!!.requesterId
        val targetUserName = if (isRequester) ratingTargetRequest!!.ownerName else ratingTargetRequest!!.requesterName

        com.example.aski.ui.RatingDialog(
            targetUserName = targetUserName,
            existingScore = existingRating?.score,
            onRatingSelected = { r ->
                itemViewModel.submitOrUpdateRating(
                    itemId = ratingTargetRequest!!.itemId,
                    raterId = currentUser.id,
                    targetUserId = targetUserId,
                    newScore = r
                )
                showRatingDialog = false
                ratingTargetRequest = null
            },
            onDismiss = {
                showRatingDialog = false
                ratingTargetRequest = null
            }
        )
    }
}

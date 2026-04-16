package com.example.aski.ui

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aski.ui.viewmodel.AuthState
import com.example.aski.ui.viewmodel.AuthViewModel
import com.example.aski.ui.viewmodel.ChatViewModel
import com.example.aski.ui.viewmodel.ItemViewModel
import kotlinx.coroutines.launch

@Composable
fun AskiApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val itemViewModel: ItemViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()

    val authState by authViewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    // Kullanıcı giriş yapınca item/chat observer'larını başlat
    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let {
            itemViewModel.observeUserItems(it)
            chatViewModel.observeChats(it)
        }
    }

    val startDestination = Screen.Feed.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
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
            FeedScreen(
                items = items,
                favoriteIds = currentUser?.favoriteIds ?: emptyList(),
                onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) },
                onToggleFavorite = { authViewModel.toggleFavorite(it) },
                onCreateListingClick = {
                    if (currentUser == null) navController.navigate(Screen.Login.route)
                    else navController.navigate(Screen.CreateListing.route)
                },
                onProfileClick = {
                    if (currentUser == null) navController.navigate(Screen.Login.route)
                    else navController.navigate(Screen.Profile.route)
                }
            )
        }
        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
            val scope = rememberCoroutineScope()
            val feedItems by itemViewModel.feedItems.collectAsState()
            val userItems by itemViewModel.userItems.collectAsState()
            
            // Observe item changes in real-time by finding it in the observed flows
            val item = remember(itemId, feedItems, userItems) {
                feedItems.find { it.id == itemId } ?: userItems.find { it.id == itemId }
            }
            
            var localItem by remember { mutableStateOf<com.example.aski.model.Item?>(null) }

            LaunchedEffect(itemId) {
                if (item == null) {
                    localItem = itemViewModel.getItem(itemId)
                }
            }

            val displayItem = item ?: localItem

            displayItem?.let { itm ->
                ItemDetailScreen(
                    item = itm,
                    isOwner = itm.ownerId == currentUser?.id,
                    isFavorite = currentUser?.favoriteIds?.contains(itm.id) == true,
                    onBackClick = { navController.popBackStack() },
                    onChatClick = { ownerId ->
                        if (currentUser == null) {
                            navController.navigate(Screen.Login.route)
                        } else {
                            scope.launch {
                                val chat = chatViewModel.getOrCreateChat(itm.id, currentUser.id, ownerId)
                                if (chat != null) {
                                    // Send the interest message
                                    chatViewModel.sendMessage(chat.id, currentUser.id, "I'm interested in ${itm.title}")
                                    navController.navigate(Screen.Chat.createRoute(chat.id))
                                }
                            }
                        }
                    },
                    onToggleFavorite = { authViewModel.toggleFavorite(itm.id) },
                    onUpdateItem = { updatedItem ->
                        itemViewModel.updateItem(updatedItem)
                        // If it's a local fetch, update it too
                        if (item == null) localItem = updatedItem
                    }
                )
            }
        }
        composable(Screen.CreateListing.route) {
            var isUploading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            
            CreateListingScreen(
                onPostItem = { title, desc, catId, cond, uris ->
                    currentUser?.id?.let { uid ->
                        isUploading = true
                        errorMessage = null
                        itemViewModel.addItem(
                            ownerId = uid,
                            title = title,
                            description = desc,
                            categoryId = catId,
                            condition = cond,
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
                onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) },
                onMessagesClick = { navController.navigate(Screen.ChatList.route) },
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

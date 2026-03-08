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
                onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) },
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
            var item by remember { mutableStateOf(itemViewModel.feedItems.value.find { it.id == itemId }) }

            LaunchedEffect(itemId) {
                if (item == null) item = itemViewModel.getItem(itemId)
            }

            item?.let { itm ->
                ItemDetailScreen(
                    item = itm,
                    isOwner = itm.ownerId == currentUser?.id,
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
                    onUpdateItem = { updatedItem ->
                        itemViewModel.updateItem(updatedItem)
                    }
                )
            }
        }
        composable(Screen.CreateListing.route) {
            CreateListingScreen(
                onPostItem = { title, desc, catId, cond, url ->
                    currentUser?.id?.let { uid ->
                        itemViewModel.addItem(uid, title, desc, catId, cond, url)
                    }
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            val userItems by itemViewModel.userItems.collectAsState()
            ProfileScreen(
                user = currentUser,
                userItems = userItems,
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

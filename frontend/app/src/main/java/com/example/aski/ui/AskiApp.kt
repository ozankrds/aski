package com.example.aski.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AskiApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.Feed.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginClick = { email, password ->
                    viewModel.login(email, password)
                },
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
                onSignupClick = { email, name, password ->
                    viewModel.signup(email, name, password)
                },
                onSignupSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.Feed.route) {
            FeedScreen(
                items = viewModel.items,
                onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) },
                onCreateListingClick = {
                    if (viewModel.currentUser.value == null) {
                        navController.navigate(Screen.Login.route)
                    } else {
                        navController.navigate(Screen.CreateListing.route)
                    }
                },
                onProfileClick = {
                    if (viewModel.currentUser.value == null) {
                        navController.navigate(Screen.Login.route)
                    } else {
                        navController.navigate(Screen.Profile.route)
                    }
                }
            )
        }
        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            val item = itemId?.let { viewModel.getItemById(it) }
            val currentUserId = viewModel.currentUser.value?.id
            
            if (item != null) {
                ItemDetailScreen(
                    item = item,
                    isOwner = item.ownerId == currentUserId,
                    onBackClick = { navController.popBackStack() },
                    onChatClick = { ownerId ->
                        if (currentUserId == null) {
                            navController.navigate(Screen.Login.route)
                        } else {
                            val chat = viewModel.getOrCreateChat(item.id, ownerId)
                            if (chat != null) {
                                navController.navigate(Screen.Chat.createRoute(chat.id))
                            }
                        }
                    },
                    onUpdateItem = { id, title, desc, cond, status ->
                        viewModel.updateItem(id, title, desc, cond, status)
                    }
                )
            }
        }
        composable(Screen.CreateListing.route) {
            CreateListingScreen(
                onPostItem = { title, desc, catId, cond, url ->
                    viewModel.addItem(title, desc, catId, cond, url)
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                user = viewModel.currentUser.value,
                userItems = viewModel.getItemsForCurrentUser(),
                onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) },
                onMessagesClick = { navController.navigate(Screen.ChatList.route) },
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    viewModel.logout()
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Feed.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ChatList.route) {
            val currentUserId = viewModel.currentUser.value?.id
            if (currentUserId != null) {
                ChatListScreen(
                    chats = viewModel.getChatsForCurrentUser(),
                    currentUserId = currentUserId,
                    getUserById = { id -> viewModel.getUserById(id) },
                    onChatClick = { chatId -> navController.navigate(Screen.Chat.createRoute(chatId)) },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            val currentUserId = viewModel.currentUser.value?.id
            if (chatId != null && currentUserId != null) {
                val chat = viewModel.getChatById(chatId)
                val otherUserId = if (chat?.requesterId == currentUserId) chat.ownerId else chat?.requesterId
                val otherUser = otherUserId?.let { viewModel.getUserById(it) }
                
                ChatScreen(
                    chatId = chatId,
                    messages = viewModel.getMessagesForChat(chatId),
                    currentUserId = currentUserId,
                    otherUser = otherUser,
                    onSendMessage = { content -> viewModel.sendMessage(chatId, content) },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

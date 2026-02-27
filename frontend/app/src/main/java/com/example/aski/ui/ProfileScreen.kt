package com.example.aski.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aski.model.Item
import com.example.aski.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    userItems: List<Item>,
    onItemClick: (String) -> Unit,
    onMessagesClick: () -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var showMyItems by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (user != null) {
                Text(text = user.displayName, style = MaterialTheme.typography.headlineMedium)
                Text(text = user.email, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { showMyItems = !showMyItems },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showMyItems) "Hide My Listings" else "Show My Listings")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onMessagesClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("My Messages")
                }

                if (showMyItems) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (userItems.isEmpty()) {
                        Text("You haven't listed anything yet.")
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(userItems) { item ->
                                ItemCard(item = item, onClick = { onItemClick(item.id) })
                            }
                        }
                    }
                }
            } else {
                Text(text = "No user logged in.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

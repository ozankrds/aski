package com.example.aski

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aski.ui.AskiApp
import com.example.aski.ui.theme.AskiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkItemId = intent?.data?.let { uri ->
            if (uri.scheme == "aski" && uri.host == "item") uri.pathSegments.firstOrNull()
            else null
        }
        val chatIdFromNotification = intent?.getStringExtra("chatId")

        setContent {
            AskiTheme {
                AskiApp(
                    deepLinkItemId = deepLinkItemId,
                    deepLinkChatId = chatIdFromNotification
                )
            }
        }
    }
}

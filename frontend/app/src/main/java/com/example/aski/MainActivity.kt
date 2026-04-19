package com.example.aski

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.aski.ui.AskiApp
import com.example.aski.ui.theme.AskiTheme
import com.example.aski.ui.theme.ColorPreset
import com.example.aski.ui.theme.LocalThemeConfig
import com.example.aski.ui.theme.ThemeConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("aski_theme", MODE_PRIVATE)

        val deepLinkItemId = intent?.data?.let { uri ->
            if (uri.scheme == "aski" && uri.host == "item") uri.pathSegments.firstOrNull()
            else null
        }
        val chatIdFromNotification = intent?.getStringExtra("chatId")

        setContent {
            var isDark by rememberSaveable { mutableStateOf(prefs.getBoolean("dark", true)) }
            var preset by rememberSaveable { mutableStateOf(
                runCatching { ColorPreset.valueOf(prefs.getString("preset", "FOREST")!!) }
                    .getOrDefault(ColorPreset.FOREST)
            )}

            val themeConfig = ThemeConfig(
                isDark = isDark,
                preset = preset,
                onToggleDark = {
                    isDark = !isDark
                    prefs.edit().putBoolean("dark", isDark).apply()
                },
                onSetPreset = { p ->
                    preset = p
                    prefs.edit().putString("preset", p.name).apply()
                }
            )

            CompositionLocalProvider(LocalThemeConfig provides themeConfig) {
                AskiTheme(isDark = isDark, preset = preset) {
                    AskiApp(
                        deepLinkItemId = deepLinkItemId,
                        deepLinkChatId = chatIdFromNotification
                    )
                }
            }
        }
    }
}

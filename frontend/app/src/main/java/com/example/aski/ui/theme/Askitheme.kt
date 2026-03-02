package com.example.aski.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Black900 = Color(0xFF0A0A0A)
val Black800 = Color(0xFF141414)
val Black700 = Color(0xFF1E1E1E)
val Black600 = Color(0xFF2A2A2A)
val Black500 = Color(0xFF3A3A3A)

val Accent = Color(0xFF6C63FF)
val AccentLight = Color(0xFF9D97FF)
val AccentContainer = Color(0xFF1A1830)

val White = Color(0xFFFFFFFF)
val White70 = Color(0xFFB3B3B3)

val Success = Color(0xFF2ECC71)
val Danger = Color(0xFFE74C3C)

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = White,
    primaryContainer = AccentContainer,
    onPrimaryContainer = AccentLight,

    secondary = Color(0xFF00D4AA),
    onSecondary = Black900,
    secondaryContainer = Color(0xFF003D30),
    onSecondaryContainer = Color(0xFF00D4AA),

    tertiary = Color(0xFFFF6B6B),
    onTertiary = White,
    tertiaryContainer = Color(0xFF3D1010),
    onTertiaryContainer = Color(0xFFFF6B6B),

    background = Black900,
    onBackground = White,
    surface = Black800,
    onSurface = White,
    surfaceVariant = Black700,
    onSurfaceVariant = White70,
    outline = Black500,
    outlineVariant = Black600,
    error = Danger,
    onError = White
)

@Composable
fun AskiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AskiTypography,
        shapes = AskiShapes,
        content = content
    )
}
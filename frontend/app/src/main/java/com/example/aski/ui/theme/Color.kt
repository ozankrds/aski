package com.example.aski.ui.theme

import androidx.compose.ui.graphics.Color

// Semantic colors (used throughout app, theme-independent)
val AskiSuccess = Color(0xFF22C55E)
val AskiWarning = Color(0xFFF59E0B)
val AskiError   = Color(0xFFEF4444)
val AskiOnBgVariant = Color(0xFF8A8A9A)

// Legacy aliases kept for backward compat
val AskiDarkBg  = Color(0xFF0D1117)
val AskiCardBg  = Color(0xFF161B22)
val AskiSurface = Color(0xFF1E2330)
val AskiOnBg    = Color(0xFFF0F2F5)

// ── Color Presets ────────────────────────────────────────────────────────────
enum class ColorPreset(
    // Dark scheme
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondary: Color,
    // Light scheme
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightSecondary: Color,
    // UI
    val label: String,
    val swatch: Color
) {
    FOREST(
        darkPrimary             = Color(0xFF00C896),
        darkOnPrimary           = Color(0xFF003D2A),
        darkPrimaryContainer    = Color(0xFF00381F),
        darkOnPrimaryContainer  = Color(0xFF7FFFD4),
        darkSecondary           = Color(0xFF00A878),
        lightPrimary            = Color(0xFF007A55),
        lightOnPrimary          = Color(0xFFFFFFFF),
        lightPrimaryContainer   = Color(0xFFB7F5E0),
        lightOnPrimaryContainer = Color(0xFF002117),
        lightSecondary          = Color(0xFF00896A),
        label = "Forest",
        swatch = Color(0xFF00C896)
    ),
    OCEAN(
        darkPrimary             = Color(0xFF60A5FA),
        darkOnPrimary           = Color(0xFF0A2040),
        darkPrimaryContainer    = Color(0xFF0A2040),
        darkOnPrimaryContainer  = Color(0xFFBADAFF),
        darkSecondary           = Color(0xFF93C5FD),
        lightPrimary            = Color(0xFF2563EB),
        lightOnPrimary          = Color(0xFFFFFFFF),
        lightPrimaryContainer   = Color(0xFFDBEAFE),
        lightOnPrimaryContainer = Color(0xFF0A1F4A),
        lightSecondary          = Color(0xFF3B82F6),
        label = "Ocean",
        swatch = Color(0xFF3B82F6)
    ),
    SUNSET(
        darkPrimary             = Color(0xFFFBBF24),
        darkOnPrimary           = Color(0xFF3D2800),
        darkPrimaryContainer    = Color(0xFF3D2800),
        darkOnPrimaryContainer  = Color(0xFFFFE0A0),
        darkSecondary           = Color(0xFFF59E0B),
        lightPrimary            = Color(0xFFB45309),
        lightOnPrimary          = Color(0xFFFFFFFF),
        lightPrimaryContainer   = Color(0xFFFEF3C7),
        lightOnPrimaryContainer = Color(0xFF3D1A00),
        lightSecondary          = Color(0xFFD97706),
        label = "Sunset",
        swatch = Color(0xFFF59E0B)
    )
}

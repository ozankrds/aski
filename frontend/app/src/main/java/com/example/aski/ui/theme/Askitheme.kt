package com.example.aski.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Theme config exposed via CompositionLocal ────────────────────────────────
data class ThemeConfig(
    val isDark: Boolean = true,
    val preset: ColorPreset = ColorPreset.FOREST,
    val onToggleDark: () -> Unit = {},
    val onSetPreset: (ColorPreset) -> Unit = {}
)

val LocalThemeConfig = compositionLocalOf { ThemeConfig() }

// ── Color scheme factories ───────────────────────────────────────────────────
private fun buildDarkScheme(p: ColorPreset) = darkColorScheme(
    primary             = p.darkPrimary,
    onPrimary           = p.darkOnPrimary,
    primaryContainer    = p.darkPrimaryContainer,
    onPrimaryContainer  = p.darkOnPrimaryContainer,
    secondary           = p.darkSecondary,
    onSecondary         = Color(0xFF0D1117),
    secondaryContainer  = Color(0xFF1A2A20),
    onSecondaryContainer= p.darkSecondary,
    background          = Color(0xFF0D1117),
    onBackground        = Color(0xFFF0F2F5),
    surface             = Color(0xFF161B22),
    onSurface           = Color(0xFFECEFF4),
    surfaceVariant      = Color(0xFF1E2330),
    onSurfaceVariant    = Color(0xFF8A8AA0),
    outline             = Color(0xFF2E3340),
    outlineVariant      = Color(0xFF252B38),
    error               = AskiError,
    onError             = Color(0xFFFFFFFF),
    errorContainer      = Color(0xFF3D0F0F),
    onErrorContainer    = Color(0xFFFFB4AB)
)

private fun buildLightScheme(p: ColorPreset) = lightColorScheme(
    primary             = p.lightPrimary,
    onPrimary           = p.lightOnPrimary,
    primaryContainer    = p.lightPrimaryContainer,
    onPrimaryContainer  = p.lightOnPrimaryContainer,
    secondary           = p.lightSecondary,
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFE8F5EE),
    onSecondaryContainer= p.lightOnPrimaryContainer,
    background          = Color(0xFFF6F7F9),
    onBackground        = Color(0xFF1A1A2E),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF1A1A2E),
    surfaceVariant      = Color(0xFFEFF1F5),
    onSurfaceVariant    = Color(0xFF5A5A7A),
    outline             = Color(0xFFC5C6D0),
    outlineVariant      = Color(0xFFDDDEE8),
    error               = Color(0xFFBA1A1A),
    onError             = Color(0xFFFFFFFF),
    errorContainer      = Color(0xFFFFDAD6),
    onErrorContainer    = Color(0xFF410002)
)

// ── Theme composable ─────────────────────────────────────────────────────────
@Composable
fun AskiTheme(
    isDark: Boolean = true,
    preset: ColorPreset = ColorPreset.FOREST,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDark) buildDarkScheme(preset) else buildLightScheme(preset)
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AskiTypography,
        shapes      = AskiShapes,
        content     = content
    )
}

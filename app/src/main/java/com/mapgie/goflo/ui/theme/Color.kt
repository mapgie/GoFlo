package com.mapgie.goflo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppTheme { CORAL, TURQUOISE, GREEN }

// ── Coral / Peach ────────────────────────────────────────────────────────────
private val CoralLight = lightColorScheme(
    primary = Color(0xFFD9604A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF3E0400),
    secondary = Color(0xFFB0544A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF3E000A),
    tertiary = Color(0xFFB85C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF3C1A00),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DEDA),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857370)
)

// ── Baby Turquoise ────────────────────────────────────────────────────────────
private val TurquoiseLight = lightColorScheme(
    primary = Color(0xFF00696F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF0F5),
    onPrimaryContainer = Color(0xFF001F22),
    secondary = Color(0xFF4A6364),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E9),
    onSecondaryContainer = Color(0xFF051F20),
    tertiary = Color(0xFF4E6078),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E4FF),
    onTertiaryContainer = Color(0xFF091C31),
    background = Color(0xFFF4FBFB),
    onBackground = Color(0xFF161D1E),
    surface = Color(0xFFF4FBFB),
    onSurface = Color(0xFF161D1E),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4949),
    outline = Color(0xFF6F7979)
)

// ── Happy Green ───────────────────────────────────────────────────────────────
private val GreenLight = lightColorScheme(
    primary = Color(0xFF386A20),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F397),
    onPrimaryContainer = Color(0xFF072100),
    secondary = Color(0xFF55624C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E8CB),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiary = Color(0xFF386669),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBBEBEE),
    onTertiaryContainer = Color(0xFF002022),
    background = Color(0xFFF7FBF1),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFF7FBF1),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFDEE4D8),
    onSurfaceVariant = Color(0xFF42493E),
    outline = Color(0xFF72796D)
)

fun colorSchemeFor(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.CORAL -> CoralLight
    AppTheme.TURQUOISE -> TurquoiseLight
    AppTheme.GREEN -> GreenLight
}

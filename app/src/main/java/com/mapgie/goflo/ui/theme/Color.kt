package com.mapgie.goflo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// ── Theme groups ─────────────────────────────────────────────────────────────

enum class ThemeGroup(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM(""),           // shown as a standalone row, no group header needed
    HIGH_CONTRAST("High Contrast"),
    COLOR_BLIND("Color-Blind Friendly"),
}

// ── Theme catalogue ───────────────────────────────────────────────────────────
//
// Adding a new theme:
//   1. Add an entry here with a display name, group, preview ARGB, and isDark flag.
//   2. Define a private val color scheme below.
//   3. Add a branch to colorSchemeFor().
//   4. All colors must pass WCAG AA — run the wcag_check.py script before shipping.
//
// Do NOT rename existing enum entries — the name is persisted to DataStore.
// "Turquoise" → display "Teal" and "Green" → display "Sage" for better UX while
// keeping backward-compatible storage keys.

enum class AppTheme(
    val displayName: String,
    val group: ThemeGroup,
    /** ARGB Long for the small colour-swatch shown next to the chip label. */
    val previewArgb: Long,
    val isDark: Boolean = false,
) {
    // Light
    CORAL("Coral",         ThemeGroup.LIGHT,         0xFFC15542L),
    TURQUOISE("Teal",      ThemeGroup.LIGHT,         0xFF00696FL),
    GREEN("Sage",          ThemeGroup.LIGHT,         0xFF386A20L),
    // Dark
    CORAL_DARK("Coral",       ThemeGroup.DARK,       0xFFFFB4A8L, isDark = true),
    TURQUOISE_DARK("Teal",    ThemeGroup.DARK,       0xFF80D5DBL, isDark = true),
    GREEN_DARK("Sage",        ThemeGroup.DARK,       0xFF9DD679L, isDark = true),
    // System auto (Teal palette, follows device light/dark preference)
    SYSTEM("Follow system",   ThemeGroup.SYSTEM,     0xFF9E9E9EL),
    // Accessibility
    HIGH_CONTRAST_LIGHT("Light",  ThemeGroup.HIGH_CONTRAST, 0xFF1A1A1AL),
    HIGH_CONTRAST_DARK("Dark",    ThemeGroup.HIGH_CONTRAST, 0xFFFFFFFFFL, isDark = true),
    // Color-blind friendly (safe for deuteranopia & protanopia, ~9% of users)
    BLUE_ORANGE("Blue & Orange",  ThemeGroup.COLOR_BLIND,   0xFF005FADL),
}

// ── Light color schemes ───────────────────────────────────────────────────────

// Coral: primary darkened to #C15542 (WCAG AA audit 2026-05-23; original #D9604A failed 3.7:1)
private val CoralLight = lightColorScheme(
    primary             = Color(0xFFC15542),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDAD4),
    onPrimaryContainer  = Color(0xFF3E0400),
    secondary           = Color(0xFFB0544A),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDAD6),
    onSecondaryContainer= Color(0xFF3E000A),
    tertiary            = Color(0xFFB85C00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF3C1A00),
    background          = Color(0xFFFFF8F7),
    onBackground        = Color(0xFF231917),
    surface             = Color(0xFFFFF8F7),
    onSurface           = Color(0xFF231917),
    surfaceVariant      = Color(0xFFF5DEDA),
    onSurfaceVariant    = Color(0xFF534341),
    outline             = Color(0xFF857370),
)

private val TurquoiseLight = lightColorScheme(
    primary             = Color(0xFF00696F),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFF9CF0F5),
    onPrimaryContainer  = Color(0xFF001F22),
    secondary           = Color(0xFF4A6364),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFCCE8E9),
    onSecondaryContainer= Color(0xFF051F20),
    tertiary            = Color(0xFF4E6078),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFD6E4FF),
    onTertiaryContainer = Color(0xFF091C31),
    background          = Color(0xFFF4FBFB),
    onBackground        = Color(0xFF161D1E),
    surface             = Color(0xFFF4FBFB),
    onSurface           = Color(0xFF161D1E),
    surfaceVariant      = Color(0xFFDAE4E5),
    onSurfaceVariant    = Color(0xFF3F4949),
    outline             = Color(0xFF6F7979),
)

private val GreenLight = lightColorScheme(
    primary             = Color(0xFF386A20),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFB7F397),
    onPrimaryContainer  = Color(0xFF072100),
    secondary           = Color(0xFF55624C),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFD8E8CB),
    onSecondaryContainer= Color(0xFF131F0D),
    tertiary            = Color(0xFF386669),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFBBEBEE),
    onTertiaryContainer = Color(0xFF002022),
    background          = Color(0xFFF7FBF1),
    onBackground        = Color(0xFF1A1C18),
    surface             = Color(0xFFF7FBF1),
    onSurface           = Color(0xFF1A1C18),
    surfaceVariant      = Color(0xFFDEE4D8),
    onSurfaceVariant    = Color(0xFF42493E),
    outline             = Color(0xFF72796D),
)

// ── Dark color schemes ────────────────────────────────────────────────────────
// All pairs verified WCAG AA; outline colours bumped slightly vs Material defaults
// to clear the 3:1 threshold on dark surfaceVariant backgrounds.

private val CoralDark = darkColorScheme(
    primary             = Color(0xFFFFB4A8),
    onPrimary           = Color(0xFF5F1612),
    primaryContainer    = Color(0xFF7D2B20),
    onPrimaryContainer  = Color(0xFFFFDAD4),
    secondary           = Color(0xFFE7BDB8),
    onSecondary         = Color(0xFF442925),
    secondaryContainer  = Color(0xFF5D3F3B),
    onSecondaryContainer= Color(0xFFFFDAD6),
    tertiary            = Color(0xFFFFB787),
    onTertiary          = Color(0xFF532200),
    tertiaryContainer   = Color(0xFF743100),
    onTertiaryContainer = Color(0xFFFFDCBE),
    background          = Color(0xFF201A19),
    onBackground        = Color(0xFFEDE0DE),
    surface             = Color(0xFF201A19),
    onSurface           = Color(0xFFEDE0DE),
    surfaceVariant      = Color(0xFF534341),
    onSurfaceVariant    = Color(0xFFD8C2BE),
    outline             = Color(0xFFA28E8C), // bumped from #A08C8A to pass 3:1 on surfaceVariant
)

private val TurquoiseDark = darkColorScheme(
    primary             = Color(0xFF80D5DB),
    onPrimary           = Color(0xFF003739),
    primaryContainer    = Color(0xFF004F52),
    onPrimaryContainer  = Color(0xFF9CF0F5),
    secondary           = Color(0xFFB0CCCD),
    onSecondary         = Color(0xFF1B3435),
    secondaryContainer  = Color(0xFF324B4C),
    onSecondaryContainer= Color(0xFFCCE8E9),
    tertiary            = Color(0xFFB3C8E8),
    onTertiary          = Color(0xFF1E3148),
    tertiaryContainer   = Color(0xFF354860),
    onTertiaryContainer = Color(0xFFD6E4FF),
    background          = Color(0xFF191C1D),
    onBackground        = Color(0xFFE1E3E3),
    surface             = Color(0xFF191C1D),
    onSurface           = Color(0xFFE1E3E3),
    surfaceVariant      = Color(0xFF3F4949),
    onSurfaceVariant    = Color(0xFFBEC8C9),
    outline             = Color(0xFF8B9595), // bumped from #899393 to pass 3:1 on surfaceVariant
)

private val GreenDark = darkColorScheme(
    primary             = Color(0xFF9DD679),
    onPrimary           = Color(0xFF0D3900),
    primaryContainer    = Color(0xFF254F0A),
    onPrimaryContainer  = Color(0xFFB8F397),
    secondary           = Color(0xFFBBCBAD),
    onSecondary         = Color(0xFF273420),
    secondaryContainer  = Color(0xFF3D4A35),
    onSecondaryContainer= Color(0xFFD8E8CB),
    tertiary            = Color(0xFFA1CECE),
    onTertiary          = Color(0xFF013737),
    tertiaryContainer   = Color(0xFF1F4E4E),
    onTertiaryContainer = Color(0xFFBBEBEE),
    background          = Color(0xFF1A1C18),
    onBackground        = Color(0xFFE3E3DC),
    surface             = Color(0xFF1A1C18),
    onSurface           = Color(0xFFE3E3DC),
    surfaceVariant      = Color(0xFF42493E),
    onSurfaceVariant    = Color(0xFFC2C9BB),
    outline             = Color(0xFF8E958A), // bumped from #8C9388 to pass 3:1 on surfaceVariant
)

// ── Accessibility: high contrast ──────────────────────────────────────────────

private val HighContrastLight = lightColorScheme(
    primary             = Color(0xFF1A1A1A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFE8E8E8),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF1A1A1A),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFE8E8E8),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF1A1A1A),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFE8E8E8),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFF0F0F0),
    onSurfaceVariant    = Color(0xFF000000),
    outline             = Color(0xFF1A1A1A),
)

private val HighContrastDark = darkColorScheme(
    primary             = Color(0xFFFFFFFF),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF1A1A1A),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFFFFFFF),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF1A1A1A),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFFFFFFF),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF1A1A1A),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF000000),
    onBackground        = Color(0xFFFFFFFF),
    surface             = Color(0xFF000000),
    onSurface           = Color(0xFFFFFFFF),
    surfaceVariant      = Color(0xFF1A1A1A),
    onSurfaceVariant    = Color(0xFFFFFFFF),
    outline             = Color(0xFFDEDEDE),
)

// ── Accessibility: color-blind friendly ──────────────────────────────────────
// Blue/orange palette — safe for deuteranopia (~6 % of males) and protanopia
// (~1 % of males), the two most common forms of red-green color vision deficiency.
// Period days render as blue circles; no red or green is used as a sole distinguisher.

private val BlueOrange = lightColorScheme(
    primary             = Color(0xFF005FAD),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFD1E4FF),
    onPrimaryContainer  = Color(0xFF001D36),
    secondary           = Color(0xFF8B5000),  // accessible amber
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDDB8),
    onSecondaryContainer= Color(0xFF2D1600),
    tertiary            = Color(0xFF6B5F00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFF5E400),
    onTertiaryContainer = Color(0xFF201C00),
    background          = Color(0xFFF8FAFF),
    onBackground        = Color(0xFF191C1E),
    surface             = Color(0xFFF8FAFF),
    onSurface           = Color(0xFF191C1E),
    surfaceVariant      = Color(0xFFDDE3EA),
    onSurfaceVariant    = Color(0xFF404A51),
    outline             = Color(0xFF70787F),
)

// ── Scheme selector ───────────────────────────────────────────────────────────

/**
 * Returns the [ColorScheme] for [theme].
 *
 * [systemIsDark] is only consulted when [theme] is [AppTheme.SYSTEM]; pass the
 * result of `isSystemInDarkTheme()` at the call site.
 */
fun colorSchemeFor(theme: AppTheme, systemIsDark: Boolean = false): ColorScheme = when (theme) {
    AppTheme.CORAL              -> CoralLight
    AppTheme.TURQUOISE          -> TurquoiseLight
    AppTheme.GREEN              -> GreenLight
    AppTheme.CORAL_DARK         -> CoralDark
    AppTheme.TURQUOISE_DARK     -> TurquoiseDark
    AppTheme.GREEN_DARK         -> GreenDark
    AppTheme.SYSTEM             -> if (systemIsDark) TurquoiseDark else TurquoiseLight
    AppTheme.HIGH_CONTRAST_LIGHT -> HighContrastLight
    AppTheme.HIGH_CONTRAST_DARK  -> HighContrastDark
    AppTheme.BLUE_ORANGE         -> BlueOrange
}

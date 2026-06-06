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
    // ── Classic palettes ──────────────────────────────────────────────────────
    // Light
    CORAL("Coral",         ThemeGroup.LIGHT,         0xFFC35040L),
    TURQUOISE("Teal",      ThemeGroup.LIGHT,         0xFF00747CL),
    GREEN("Sage",          ThemeGroup.LIGHT,         0xFF4F7D2BL),
    // Dark
    CORAL_DARK("Coral",       ThemeGroup.DARK,       0xFFFFB4A4L, isDark = true),
    TURQUOISE_DARK("Teal",    ThemeGroup.DARK,       0xFF80D5DBL, isDark = true),
    GREEN_DARK("Sage",        ThemeGroup.DARK,       0xFFACD888L, isDark = true),

    // ── Fun palettes ──────────────────────────────────────────────────────────
    // Summer Candy — bubblegum raspberry, mint aqua, lemon gold
    SUMMER_CANDY("Summer Candy",            ThemeGroup.LIGHT, 0xFFD81B60L),
    SUMMER_CANDY_DARK("Summer Candy",       ThemeGroup.DARK,  0xFFFFB1CAL, isDark = true),
    // Beach Vibes — clear sea blue, sun-gold sand, sea-foam green
    BEACH_VIBES("Beach Vibes",              ThemeGroup.LIGHT, 0xFF1265AFL),
    BEACH_VIBES_DARK("Beach Vibes",         ThemeGroup.DARK,  0xFFA8C8FFL, isDark = true),
    // Peach Melba — apricot peach, raspberry, vanilla-caramel cream
    PEACH_MELBA("Peach Melba",              ThemeGroup.LIGHT, 0xFFBC5A38L),
    PEACH_MELBA_DARK("Peach Melba",         ThemeGroup.DARK,  0xFFFFB694L, isDark = true),
    // All-Night Disco Party — hot magenta, electric violet, glitter gold
    DISCO("All-Night Disco Party",          ThemeGroup.LIGHT, 0xFFC1127AL),
    DISCO_DARK("All-Night Disco Party",     ThemeGroup.DARK,  0xFFFF66B8L, isDark = true),
    // Metal Chic — gunmetal, crimson-lipstick, brushed chrome
    METAL_CHICK("Metal Chic",              ThemeGroup.LIGHT, 0xFF2E2E3AL),
    METAL_CHICK_DARK("Metal Chic",         ThemeGroup.DARK,  0xFFC7C5D6L, isDark = true),
    // Whimsy Whispers — lavender, blush rose, spearmint
    WHIMSY("Whimsy Whispers",              ThemeGroup.LIGHT, 0xFF6E5DC4L),
    WHIMSY_DARK("Whimsy Whispers",         ThemeGroup.DARK,  0xFFC7BEFFL, isDark = true),
    // Colour Me Happy — tomato red, sky blue, grass green
    COLOUR_HAPPY("Colour Me Happy",         ThemeGroup.LIGHT, 0xFFD63A26L),
    COLOUR_HAPPY_DARK("Colour Me Happy",    ThemeGroup.DARK,  0xFFFFB4A4L, isDark = true),
    // Dragon Fire — ember red, molten orange, furnace gold
    DRAGON_FIRE("Dragon Fire",                 ThemeGroup.LIGHT, 0xFFB0181FL),
    DRAGON_FIRE_DARK("Dragon Fire",            ThemeGroup.DARK,  0xFFFF8A82L, isDark = true),
    // Midnight Neon — neon magenta, electric cyan, acid lime
    MIDNIGHT_NEON("Midnight Neon",             ThemeGroup.LIGHT, 0xFFC5128AL),
    MIDNIGHT_NEON_DARK("Midnight Neon",        ThemeGroup.DARK,  0xFFFF55C8L, isDark = true),

    // ── System auto (palette-matched, follows device light/dark preference) ───
    SYSTEM("Follow system",                  ThemeGroup.SYSTEM, 0xFF80D5DBL),
    CORAL_SYSTEM("Follow system",            ThemeGroup.SYSTEM, 0xFFFFB4A4L),
    GREEN_SYSTEM("Follow system",            ThemeGroup.SYSTEM, 0xFFACD888L),
    SUMMER_CANDY_SYSTEM("Follow system",     ThemeGroup.SYSTEM, 0xFFFFB1CAL),
    BEACH_VIBES_SYSTEM("Follow system",      ThemeGroup.SYSTEM, 0xFFA8C8FFL),
    PEACH_MELBA_SYSTEM("Follow system",      ThemeGroup.SYSTEM, 0xFFFFB694L),
    DISCO_SYSTEM("Follow system",            ThemeGroup.SYSTEM, 0xFFFF66B8L),
    METAL_CHICK_SYSTEM("Follow system",      ThemeGroup.SYSTEM, 0xFFC7C5D6L),
    WHIMSY_SYSTEM("Follow system",           ThemeGroup.SYSTEM, 0xFFC7BEFFL),
    COLOUR_HAPPY_SYSTEM("Follow system",     ThemeGroup.SYSTEM, 0xFFFFB4A4L),
    DRAGON_FIRE_SYSTEM("Follow system",        ThemeGroup.SYSTEM, 0xFFFF8A82L),
    MIDNIGHT_NEON_SYSTEM("Follow system",      ThemeGroup.SYSTEM, 0xFFFF55C8L),

    // ── Accessibility ─────────────────────────────────────────────────────────
    HIGH_CONTRAST_LIGHT("Max Contrast",  ThemeGroup.HIGH_CONTRAST, 0xFF1A1A1AL),
    HIGH_CONTRAST_DARK("Max Contrast",   ThemeGroup.HIGH_CONTRAST, 0xFFFFFFFFFL, isDark = true),
    // Color-blind friendly (safe for deuteranopia & protanopia, ~9% of users)
    BLUE_ORANGE("Blue & Orange",  ThemeGroup.COLOR_BLIND,   0xFF005FADL),
    // User-defined custom palette
    CUSTOM("Custom", ThemeGroup.LIGHT, 0xFF808080L),
}

// ── Classic light color schemes ───────────────────────────────────────────────
// Redesigned 2026-05 — see GoFlo Theme Redesign.md

// Coral: vivid coral reef — coral red · lagoon teal · vivid rose-magenta
private val CoralLight = lightColorScheme(
    primary             = Color(0xFFC35040),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDAD2),
    onPrimaryContainer  = Color(0xFF3F0900),
    secondary           = Color(0xFF00817D),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFB4ECE7),
    onSecondaryContainer= Color(0xFF002523),
    tertiary            = Color(0xFFB5307A),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFD6EA),
    onTertiaryContainer = Color(0xFF3C001E),
    background          = Color(0xFFFFF6F2),
    onBackground        = Color(0xFF221915),
    surface             = Color(0xFFFFF6F2),
    onSurface           = Color(0xFF221915),
    surfaceVariant      = Color(0xFFF4DDD5),
    onSurfaceVariant    = Color(0xFF524340),
    outline             = Color(0xFF856E68),
)

// Teal: clear lagoon water — deep teal · terra-cotta · indigo
private val TurquoiseLight = lightColorScheme(
    primary             = Color(0xFF00747C),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFF9DEFF6),
    onPrimaryContainer  = Color(0xFF002023),
    secondary           = Color(0xFFB7592B),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBC8),
    onSecondaryContainer= Color(0xFF381300),
    tertiary            = Color(0xFF4B5BAC),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFDEE1FF),
    onTertiaryContainer = Color(0xFF00115A),
    background          = Color(0xFFF0FBFC),
    onBackground        = Color(0xFF161C1D),
    surface             = Color(0xFFF0FBFC),
    onSurface           = Color(0xFF161C1D),
    surfaceVariant      = Color(0xFFDAE4E5),
    onSurfaceVariant    = Color(0xFF3F4949),
    outline             = Color(0xFF6F7979),
)

// Sage: herb garden at dawn — sage green · terra-cotta clay · periwinkle
private val GreenLight = lightColorScheme(
    primary             = Color(0xFF4F7D2B),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFC8F09F),
    onPrimaryContainer  = Color(0xFF112100),
    secondary           = Color(0xFFB5532A),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBC8),
    onSecondaryContainer= Color(0xFF3A1100),
    tertiary            = Color(0xFF6B5BAE),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFE5DFFF),
    onTertiaryContainer = Color(0xFF1A0067),
    background          = Color(0xFFF7FBEE),
    onBackground        = Color(0xFF1A1C16),
    surface             = Color(0xFFF7FBEE),
    onSurface           = Color(0xFF1A1C16),
    surfaceVariant      = Color(0xFFDEE5D2),
    onSurfaceVariant    = Color(0xFF424A3B),
    outline             = Color(0xFF72796A),
)

// ── Classic dark color schemes ────────────────────────────────────────────────
// Redesigned 2026-05 — see GoFlo Theme Redesign.md

private val CoralDark = darkColorScheme(
    primary             = Color(0xFFFFB4A4),
    onPrimary           = Color(0xFF5C1600),
    primaryContainer    = Color(0xFF7C2A18),
    onPrimaryContainer  = Color(0xFFFFDAD0),
    secondary           = Color(0xFF5BD8D0),
    onSecondary         = Color(0xFF003734),
    secondaryContainer  = Color(0xFF00504C),
    onSecondaryContainer= Color(0xFFB4EEE9),
    tertiary            = Color(0xFFFFB1C8),
    onTertiary          = Color(0xFF65003A),
    tertiaryContainer   = Color(0xFF8E1F50),
    onTertiaryContainer = Color(0xFFFFD9E4),
    background          = Color(0xFF1C110E),
    onBackground        = Color(0xFFF1DED8),
    surface             = Color(0xFF1C110E),
    onSurface           = Color(0xFFF1DED8),
    surfaceVariant      = Color(0xFF523F3A),
    onSurfaceVariant    = Color(0xFFD7C2BC),
    outline             = Color(0xFFA28B86),
)

private val TurquoiseDark = darkColorScheme(
    primary             = Color(0xFF80D5DB),
    onPrimary           = Color(0xFF003739),
    primaryContainer    = Color(0xFF004F52),
    onPrimaryContainer  = Color(0xFF9DEFF6),
    secondary           = Color(0xFFFFB28F),
    onSecondary         = Color(0xFF5A1B00),
    secondaryContainer  = Color(0xFF8C3B16),
    onSecondaryContainer= Color(0xFFFFDBC8),
    tertiary            = Color(0xFFBCC2FF),
    onTertiary          = Color(0xFF1C257B),
    tertiaryContainer   = Color(0xFF353F93),
    onTertiaryContainer = Color(0xFFDEE1FF),
    background          = Color(0xFF0E1818),
    onBackground        = Color(0xFFE0E3E3),
    surface             = Color(0xFF0E1818),
    onSurface           = Color(0xFFE0E3E3),
    surfaceVariant      = Color(0xFF3F4949),
    onSurfaceVariant    = Color(0xFFBEC8C9),
    outline             = Color(0xFF8B9595),
)

private val GreenDark = darkColorScheme(
    primary             = Color(0xFFACD888),
    onPrimary           = Color(0xFF1B3900),
    primaryContainer    = Color(0xFF2D530B),
    onPrimaryContainer  = Color(0xFFC8F09F),
    secondary           = Color(0xFFFFB28F),
    onSecondary         = Color(0xFF5A1B00),
    secondaryContainer  = Color(0xFF8B3914),
    onSecondaryContainer= Color(0xFFFFDBC8),
    tertiary            = Color(0xFFC7BEFF),
    onTertiary          = Color(0xFF260092),
    tertiaryContainer   = Color(0xFF3D2EAE),
    onTertiaryContainer = Color(0xFFE5DFFF),
    background          = Color(0xFF14170F),
    onBackground        = Color(0xFFE2E4D7),
    surface             = Color(0xFF14170F),
    onSurface           = Color(0xFFE2E4D7),
    surfaceVariant      = Color(0xFF424A3B),
    onSurfaceVariant    = Color(0xFFC2C9B6),
    outline             = Color(0xFF919989),
)

// ── Fun light color schemes ───────────────────────────────────────────────────
// Redesigned 2026-05 — see GoFlo Theme Redesign.md

// Summer Candy — bubblegum raspberry · mint aqua · electric violet
private val SummerCandyLight = lightColorScheme(
    primary             = Color(0xFFD81B60),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFD8E5),
    onPrimaryContainer  = Color(0xFF40001B),
    secondary           = Color(0xFF008179),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFF6FF5E5),
    onSecondaryContainer= Color(0xFF00201D),
    tertiary            = Color(0xFF9B27AF),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFF2D7FF),
    onTertiaryContainer = Color(0xFF35005A),
    background          = Color(0xFFFFF7FA),
    onBackground        = Color(0xFF1E1316),
    surface             = Color(0xFFFFF7FA),
    onSurface           = Color(0xFF1E1316),
    surfaceVariant      = Color(0xFFF4DDE5),
    onSurfaceVariant    = Color(0xFF534249),
    outline             = Color(0xFF856D74),
)

// Beach Vibes — clear sea blue · vivid coral-orange · sea-foam green
private val BeachVibesLight = lightColorScheme(
    primary             = Color(0xFF1265AF),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFD5E3FF),
    onPrimaryContainer  = Color(0xFF001C3D),
    secondary           = Color(0xFFB55300),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBB2),
    onSecondaryContainer= Color(0xFF3B1100),
    tertiary            = Color(0xFF2A8470),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFA8F0DC),
    onTertiaryContainer = Color(0xFF002019),
    background          = Color(0xFFF8FAFE),
    onBackground        = Color(0xFF181C22),
    surface             = Color(0xFFF8FAFE),
    onSurface           = Color(0xFF181C22),
    surfaceVariant      = Color(0xFFE1E3EE),
    onSurfaceVariant    = Color(0xFF444751),
    outline             = Color(0xFF757782),
)

// Peach Melba — apricot peach · raspberry · dusty lilac
private val PeachMelbaLight = lightColorScheme(
    primary             = Color(0xFFBC5A38),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDBC9),
    onPrimaryContainer  = Color(0xFF3A1300),
    secondary           = Color(0xFFB53369),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD9E3),
    onSecondaryContainer= Color(0xFF3F0024),
    tertiary            = Color(0xFF884E92),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFF6D8FF),
    onTertiaryContainer = Color(0xFF2F003E),
    background          = Color(0xFFFFF6F0),
    onBackground        = Color(0xFF201813),
    surface             = Color(0xFFFFF6F0),
    onSurface           = Color(0xFF201813),
    surfaceVariant      = Color(0xFFF4DED1),
    onSurfaceVariant    = Color(0xFF534439),
    outline             = Color(0xFF867262),
)

// All-Night Disco Party — hot magenta · electric violet · acid lime
private val DiscoLight = lightColorScheme(
    primary             = Color(0xFFC1127A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFD6EA),
    onPrimaryContainer  = Color(0xFF3D003C),
    secondary           = Color(0xFF6E1FB5),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFECDCFF),
    onSecondaryContainer= Color(0xFF270060),
    tertiary            = Color(0xFF486E00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFD6F0A0),
    onTertiaryContainer = Color(0xFF132200),
    background          = Color(0xFFFFF7FB),
    onBackground        = Color(0xFF1E1020),
    surface             = Color(0xFFFFF7FB),
    onSurface           = Color(0xFF1E1020),
    surfaceVariant      = Color(0xFFF0DEEC),
    onSurfaceVariant    = Color(0xFF4F4452),
    outline             = Color(0xFF807385),
)

// Metal Chic — gunmetal · crimson-lipstick · brushed chrome
private val MetalChickLight = lightColorScheme(
    primary             = Color(0xFF2E2E3A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFDCDBE9),
    onPrimaryContainer  = Color(0xFF0E0E1C),
    secondary           = Color(0xFFC8235A),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD8E0),
    onSecondaryContainer= Color(0xFF3F001A),
    tertiary            = Color(0xFF6A6A78),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFE3E2F0),
    onTertiaryContainer = Color(0xFF1C1C2A),
    background          = Color(0xFFF4F3F8),
    onBackground        = Color(0xFF1B1B22),
    surface             = Color(0xFFF4F3F8),
    onSurface           = Color(0xFF1B1B22),
    surfaceVariant      = Color(0xFFE2E0EC),
    onSurfaceVariant    = Color(0xFF45434E),
    outline             = Color(0xFF76737E),
)

// Whimsy Whispers — lavender · blush rose · spearmint
private val WhimsyLight = lightColorScheme(
    primary             = Color(0xFF6E5DC4),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFE5DFFF),
    onPrimaryContainer  = Color(0xFF1A0067),
    secondary           = Color(0xFFBD4878),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD9E4),
    onSecondaryContainer= Color(0xFF3D0024),
    tertiary            = Color(0xFF2C846C),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFA6F0D6),
    onTertiaryContainer = Color(0xFF002016),
    background          = Color(0xFFFBF8FF),
    onBackground        = Color(0xFF1C1A24),
    surface             = Color(0xFFFBF8FF),
    onSurface           = Color(0xFF1C1A24),
    surfaceVariant      = Color(0xFFE5DFF0),
    onSurfaceVariant    = Color(0xFF49454F),
    outline             = Color(0xFF7A7580),
)

// Colour Me Happy — vivid strawberry · electric cerulean · neon lime
private val ColourHappyLight = lightColorScheme(
    primary             = Color(0xFFD63A26),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDAD2),
    onPrimaryContainer  = Color(0xFF410000),
    secondary           = Color(0xFF006BA8),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFCCE5FF),
    onSecondaryContainer= Color(0xFF00203A),
    tertiary            = Color(0xFF486E00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFD6F0A0),
    onTertiaryContainer = Color(0xFF132200),
    background          = Color(0xFFFFFCF5),
    onBackground        = Color(0xFF1F1A14),
    surface             = Color(0xFFFFFCF5),
    onSurface           = Color(0xFF1F1A14),
    surfaceVariant      = Color(0xFFEFE3D5),
    onSurfaceVariant    = Color(0xFF4E443A),
    outline             = Color(0xFF80766A),
)

// ── Fun dark color schemes ────────────────────────────────────────────────────
// Redesigned 2026-05 — see GoFlo Theme Redesign.md

private val SummerCandyDark = darkColorScheme(
    primary             = Color(0xFFFFB1CA),
    onPrimary           = Color(0xFF650033),
    primaryContainer    = Color(0xFF8E0049),
    onPrimaryContainer  = Color(0xFFFFD8E5),
    secondary           = Color(0xFF6DDFD0),
    onSecondary         = Color(0xFF003A35),
    secondaryContainer  = Color(0xFF00514B),
    onSecondaryContainer= Color(0xFF6FF5E5),
    tertiary            = Color(0xFFE5AEFF),
    onTertiary          = Color(0xFF540080),
    tertiaryContainer   = Color(0xFF6E00A6),
    onTertiaryContainer = Color(0xFFF2D7FF),
    background          = Color(0xFF1D1014),
    onBackground        = Color(0xFFECDFE3),
    surface             = Color(0xFF1D1014),
    onSurface           = Color(0xFFECDFE3),
    surfaceVariant      = Color(0xFF534249),
    onSurfaceVariant    = Color(0xFFD6C1C8),
    outline             = Color(0xFFA39096),
)

private val BeachVibesDark = darkColorScheme(
    primary             = Color(0xFFA8C8FF),
    onPrimary           = Color(0xFF002E66),
    primaryContainer    = Color(0xFF00478A),
    onPrimaryContainer  = Color(0xFFD5E3FF),
    secondary           = Color(0xFFFFB089),
    onSecondary         = Color(0xFF5A1D00),
    secondaryContainer  = Color(0xFF8A2D00),
    onSecondaryContainer= Color(0xFFFFDBB2),
    tertiary            = Color(0xFF84D7BC),
    onTertiary          = Color(0xFF003828),
    tertiaryContainer   = Color(0xFF00513B),
    onTertiaryContainer = Color(0xFFA8F0DC),
    background          = Color(0xFF101820),
    onBackground        = Color(0xFFDDE3EB),
    surface             = Color(0xFF101820),
    onSurface           = Color(0xFFDDE3EB),
    surfaceVariant      = Color(0xFF444751),
    onSurfaceVariant    = Color(0xFFC5C7D2),
    outline             = Color(0xFF9395A0),
)

private val PeachMelbaDark = darkColorScheme(
    primary             = Color(0xFFFFB694),
    onPrimary           = Color(0xFF571F00),
    primaryContainer    = Color(0xFF7A3015),
    onPrimaryContainer  = Color(0xFFFFDBC9),
    secondary           = Color(0xFFFFB1C9),
    onSecondary         = Color(0xFF65003A),
    secondaryContainer  = Color(0xFF8C1F4F),
    onSecondaryContainer= Color(0xFFFFD9E3),
    tertiary            = Color(0xFFE9B4F4),
    onTertiary          = Color(0xFF46005A),
    tertiaryContainer   = Color(0xFF620078),
    onTertiaryContainer = Color(0xFFF6D8FF),
    background          = Color(0xFF1E1410),
    onBackground        = Color(0xFFEDE0D8),
    surface             = Color(0xFF1E1410),
    onSurface           = Color(0xFFEDE0D8),
    surfaceVariant      = Color(0xFF534439),
    onSurfaceVariant    = Color(0xFFD7C3B5),
    outline             = Color(0xFFA59286),
)

private val DiscoDark = darkColorScheme(
    primary             = Color(0xFFFF66B8),
    onPrimary           = Color(0xFF5C0040),
    primaryContainer    = Color(0xFF890062),
    onPrimaryContainer  = Color(0xFFFFD6EA),
    secondary           = Color(0xFFD5B2FF),
    onSecondary         = Color(0xFF3F0090),
    secondaryContainer  = Color(0xFF5800B0),
    onSecondaryContainer= Color(0xFFECDCFF),
    tertiary            = Color(0xFFC4F076),
    onTertiary          = Color(0xFF1A3300),
    tertiaryContainer   = Color(0xFF2D4E00),
    onTertiaryContainer = Color(0xFFD6F0A0),
    background          = Color(0xFF170820),
    onBackground        = Color(0xFFECDAEC),
    surface             = Color(0xFF170820),
    onSurface           = Color(0xFFECDAEC),
    surfaceVariant      = Color(0xFF4F4452),
    onSurfaceVariant    = Color(0xFFD2C2D2),
    outline             = Color(0xFFA193A3),
)

private val MetalChickDark = darkColorScheme(
    primary             = Color(0xFFC7C5D6),
    onPrimary           = Color(0xFF2F2D3D),
    primaryContainer    = Color(0xFF454354),
    onPrimaryContainer  = Color(0xFFDCDBE9),
    secondary           = Color(0xFFFF8FB0),
    onSecondary         = Color(0xFF5C0028),
    secondaryContainer  = Color(0xFF88133E),
    onSecondaryContainer= Color(0xFFFFD8E0),
    tertiary            = Color(0xFF9D9DAC),
    onTertiary          = Color(0xFF2F2F3E),
    tertiaryContainer   = Color(0xFF4A4A58),
    onTertiaryContainer = Color(0xFFE3E2F0),
    background          = Color(0xFF0E0E13),
    onBackground        = Color(0xFFE5E3ED),
    surface             = Color(0xFF0E0E13),
    onSurface           = Color(0xFFE5E3ED),
    surfaceVariant      = Color(0xFF45434E),
    onSurfaceVariant    = Color(0xFFC8C5D0),
    outline             = Color(0xFF918E99),
)

private val WhimsyDark = darkColorScheme(
    primary             = Color(0xFFC7BEFF),
    onPrimary           = Color(0xFF260092),
    primaryContainer    = Color(0xFF3D2EAE),
    onPrimaryContainer  = Color(0xFFE5DFFF),
    secondary           = Color(0xFFFFB1C6),
    onSecondary         = Color(0xFF650033),
    secondaryContainer  = Color(0xFF8E1F50),
    onSecondaryContainer= Color(0xFFFFD9E4),
    tertiary            = Color(0xFF80DAB8),
    onTertiary          = Color(0xFF003828),
    tertiaryContainer   = Color(0xFF00513B),
    onTertiaryContainer = Color(0xFFA6F0D6),
    background          = Color(0xFF16131F),
    onBackground        = Color(0xFFE6E1F0),
    surface             = Color(0xFF16131F),
    onSurface           = Color(0xFFE6E1F0),
    surfaceVariant      = Color(0xFF49454F),
    onSurfaceVariant    = Color(0xFFCBC5D0),
    outline             = Color(0xFF96909F),
)

private val ColourHappyDark = darkColorScheme(
    primary             = Color(0xFFFFB4A4),
    onPrimary           = Color(0xFF5C1500),
    primaryContainer    = Color(0xFF882000),
    onPrimaryContainer  = Color(0xFFFFDAD2),
    secondary           = Color(0xFF93CFFF),
    onSecondary         = Color(0xFF003558),
    secondaryContainer  = Color(0xFF004E80),
    onSecondaryContainer= Color(0xFFCCE5FF),
    tertiary            = Color(0xFFB8E860),
    onTertiary          = Color(0xFF1A3300),
    tertiaryContainer   = Color(0xFF2D4900),
    onTertiaryContainer = Color(0xFFD6F0A0),
    background          = Color(0xFF1A1612),
    onBackground        = Color(0xFFEDE0D2),
    surface             = Color(0xFF1A1612),
    onSurface           = Color(0xFFEDE0D2),
    surfaceVariant      = Color(0xFF4E443A),
    onSurfaceVariant    = Color(0xFFD2C4B5),
    outline             = Color(0xFFA09386),
)

// ── Bold palettes ─────────────────────────────────────────────────────────────
// Redesigned 2026-05 — see GoFlo Theme Redesign.md

// Dragon Fire — ember red · molten orange · lava-orange
private val DragonFireLight = lightColorScheme(
    primary             = Color(0xFFB0181F),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFCFCC),
    onPrimaryContainer  = Color(0xFF3F0001),
    secondary           = Color(0xFFC04A0E),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBC9),
    onSecondaryContainer= Color(0xFF3B1100),
    tertiary            = Color(0xFFE07800),
    onTertiary          = Color(0xFF1F0A00),
    tertiaryContainer   = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF2D1000),
    background          = Color(0xFFFFF8F4),
    onBackground        = Color(0xFF1E140F),
    surface             = Color(0xFFFFF8F4),
    onSurface           = Color(0xFF1E140F),
    surfaceVariant      = Color(0xFFF3DED5),
    onSurfaceVariant    = Color(0xFF524338),
    outline             = Color(0xFF87715F),
)

// Dragon Fire dark — ember coral · molten amber · furnace gold
private val DragonFireDark = darkColorScheme(
    primary             = Color(0xFFFF8A82),
    onPrimary           = Color(0xFF680000),
    primaryContainer    = Color(0xFF960000),
    onPrimaryContainer  = Color(0xFFFFDAD4),
    secondary           = Color(0xFFFFB28F),
    onSecondary         = Color(0xFF561A00),
    secondaryContainer  = Color(0xFF8E2D00),
    onSecondaryContainer= Color(0xFFFFDBC9),
    tertiary            = Color(0xFFFFB870),
    onTertiary          = Color(0xFF351A00),
    tertiaryContainer   = Color(0xFF7A3D00),
    onTertiaryContainer = Color(0xFFFFDDB0),
    background          = Color(0xFF1C0907),
    onBackground        = Color(0xFFF1DDD7),
    surface             = Color(0xFF1C0907),
    onSurface           = Color(0xFFF1DDD7),
    surfaceVariant      = Color(0xFF523F38),
    onSurfaceVariant    = Color(0xFFD7C2B8),
    outline             = Color(0xFFA28D81),
)

// Midnight Neon light — neon magenta · electric cyan · acid lime (neon sign at dusk)
private val MidnightNeonLight = lightColorScheme(
    primary             = Color(0xFFC5128A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFD6E8),
    onPrimaryContainer  = Color(0xFF3D003C),
    secondary           = Color(0xFF006D90),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFB8E9FF),
    onSecondaryContainer= Color(0xFF001E2F),
    tertiary            = Color(0xFF486E00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFD6F0A0),
    onTertiaryContainer = Color(0xFF132200),
    background          = Color(0xFFFBF7FF),
    onBackground        = Color(0xFF161020),
    surface             = Color(0xFFFBF7FF),
    onSurface           = Color(0xFF161020),
    surfaceVariant      = Color(0xFFEDDEEC),
    onSurfaceVariant    = Color(0xFF4D4350),
    outline             = Color(0xFF7F7484),
)

// Midnight Neon dark — full cyberpunk glow (Akira after midnight)
private val MidnightNeonDark = darkColorScheme(
    primary             = Color(0xFFFF55C8),
    onPrimary           = Color(0xFF5C0048),
    primaryContainer    = Color(0xFF870068),
    onPrimaryContainer  = Color(0xFFFFD6EA),
    secondary           = Color(0xFF5EE0FF),
    onSecondary         = Color(0xFF00374A),
    secondaryContainer  = Color(0xFF00516C),
    onSecondaryContainer= Color(0xFFB8E9FF),
    tertiary            = Color(0xFFC2F052),
    onTertiary          = Color(0xFF1D3300),
    tertiaryContainer   = Color(0xFF2D4900),
    onTertiaryContainer = Color(0xFFC2F088),
    background          = Color(0xFF0A0A18),
    onBackground        = Color(0xFFEDE5FA),
    surface             = Color(0xFF0A0A18),
    onSurface           = Color(0xFFEDE5FA),
    surfaceVariant      = Color(0xFF4D4350),
    onSurfaceVariant    = Color(0xFFD0C3D0),
    outline             = Color(0xFF9E90A4),
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

// ── WCAG AAA accessible variants (one light + one dark per standard palette) ───
//
// Design rules:
//   Light: primary darkened to L≤0.10 so white onPrimary text passes 7:1.
//          Background = #FFFFFF, onBackground = #000000.
//   Dark:  primary lightened to L≥0.32 so dark onPrimary text passes 7:1 on
//          near-black (#0C0C0C) background.  Background = #0C0C0C.
//   Secondaries / tertiaries follow the same darkening / lightening approach.
//   MAX_CONTRAST and BLUE_ORANGE return themselves unchanged (already max-contrast).

// ── Coral WCAG ────────────────────────────────────────────────────────────────
private val CoralLightWcag = lightColorScheme(
    primary             = Color(0xFF8A1A0A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDAD2),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF00504C),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFB4ECE7),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF6D1547),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFD6EA),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFF0EDEC),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val CoralDarkWcag = darkColorScheme(
    primary             = Color(0xFFFFCCBE),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF7C2A18),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFF80EDE8),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF00504C),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFFFCCDD),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF8E1F50),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Teal WCAG ─────────────────────────────────────────────────────────────────
private val TurquoiseLightWcag = lightColorScheme(
    primary             = Color(0xFF00474F),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFF9DEFF6),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF6E2C08),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBC8),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF1E307A),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFDEE1FF),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFECF3F4),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val TurquoiseDarkWcag = darkColorScheme(
    primary             = Color(0xFFA0E8EE),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF004F52),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFFFCAAF),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF8C3B16),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFD4D8FF),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF353F93),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Sage WCAG ─────────────────────────────────────────────────────────────────
private val GreenLightWcag = lightColorScheme(
    primary             = Color(0xFF2E500F),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFC8F09F),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF6E2500),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBC8),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF3D2E80),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFE5DFFF),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFEEF2E8),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val GreenDarkWcag = darkColorScheme(
    primary             = Color(0xFFC4EDAA),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF2D530B),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFFFCCB0),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF8B3914),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFDDD8FF),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF3D2EAE),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Summer Candy WCAG ─────────────────────────────────────────────────────────
private val SummerCandyLightWcag = lightColorScheme(
    primary             = Color(0xFF8B0043),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFD8E5),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF005047),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFF6FF5E5),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF5E0080),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFF2D7FF),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFF2EDEF),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val SummerCandyDarkWcag = darkColorScheme(
    primary             = Color(0xFFFFCCDA),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF8E0049),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFF90F5E8),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF00514B),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFF0CCFF),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF6E00A6),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Beach Vibes WCAG ──────────────────────────────────────────────────────────
private val BeachVibesLightWcag = lightColorScheme(
    primary             = Color(0xFF00397A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFD5E3FF),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF6A2800),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBB2),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF005240),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFA8F0DC),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFEDF0F8),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val BeachVibesDarkWcag = darkColorScheme(
    primary             = Color(0xFFBDD8FF),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF00478A),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFFFCCA8),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF8A2D00),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFA0EDD4),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF00513B),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Peach Melba WCAG ──────────────────────────────────────────────────────────
private val PeachMelbaLightWcag = lightColorScheme(
    primary             = Color(0xFF7A2800),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDBC9),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF6B0035),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD9E3),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF540063),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFF6D8FF),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFF2EEEC),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val PeachMelbaDarkWcag = darkColorScheme(
    primary             = Color(0xFFFFCCB4),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF7A3015),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFFFCCDB),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF8C1F4F),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFF5CCFF),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF620078),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Disco WCAG ────────────────────────────────────────────────────────────────
private val DiscoLightWcag = lightColorScheme(
    primary             = Color(0xFF8B005A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFD6EA),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF3D0080),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFECDCFF),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF2A4200),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFD6F0A0),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFF2EEF4),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val DiscoDarkWcag = darkColorScheme(
    primary             = Color(0xFFFF9ECC),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF890062),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFE0CCFF),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF5800B0),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFDAF29A),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF2D4E00),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Metal Chic WCAG ───────────────────────────────────────────────────────────
private val MetalChickLightWcag = lightColorScheme(
    primary             = Color(0xFF111118),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFDCDBE9),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF770020),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD8E0),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF2D2C3A),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFE3E2F0),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFEEEDF6),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val MetalChickDarkWcag = darkColorScheme(
    primary             = Color(0xFFDCDBED),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF454354),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFFFB2C8),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF88133E),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFD0CEE0),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF4A4A58),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Whimsy WCAG ───────────────────────────────────────────────────────────────
private val WhimsyLightWcag = lightColorScheme(
    primary             = Color(0xFF3D2EAE),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFE5DFFF),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF6B0035),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD9E4),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF005240),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFA6F0D6),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFF0EFF8),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val WhimsyDarkWcag = darkColorScheme(
    primary             = Color(0xFFDDD8FF),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF3D2EAE),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFFFCCDA),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF8E1F50),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFA0F0D8),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF00513B),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Colour Me Happy WCAG ──────────────────────────────────────────────────────
private val ColourHappyLightWcag = lightColorScheme(
    primary             = Color(0xFF921600),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDAD2),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF003E68),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFCCE5FF),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF2A4200),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFD6F0A0),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFF2EDEA),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val ColourHappyDarkWcag = darkColorScheme(
    primary             = Color(0xFFFFCCBE),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF882000),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFB8DFFF),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF004E80),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFD4F5A0),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF2D4900),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Dragon Fire WCAG ──────────────────────────────────────────────────────────
private val DragonFireLightWcag = lightColorScheme(
    primary             = Color(0xFF7A0000),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFCFCC),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF6A1C00),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBC9),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF7A3A00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFF2EDEA),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val DragonFireDarkWcag = darkColorScheme(
    primary             = Color(0xFFFFB0AB),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF960000),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFFFFCCB0),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF8E2D00),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFFFD4A0),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF7A3D00),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Midnight Neon WCAG ────────────────────────────────────────────────────────
private val MidnightNeonLightWcag = lightColorScheme(
    primary             = Color(0xFF8A0063),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFD6E8),
    onPrimaryContainer  = Color(0xFF000000),
    secondary           = Color(0xFF003E56),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFB8E9FF),
    onSecondaryContainer= Color(0xFF000000),
    tertiary            = Color(0xFF2A4200),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFD6F0A0),
    onTertiaryContainer = Color(0xFF000000),
    background          = Color(0xFFFFFFFF),
    onBackground        = Color(0xFF000000),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF000000),
    surfaceVariant      = Color(0xFFF0EEF5),
    onSurfaceVariant    = Color(0xFF121212),
    outline             = Color(0xFF545454),
)
private val MidnightNeonDarkWcag = darkColorScheme(
    primary             = Color(0xFFFFA0E8),
    onPrimary           = Color(0xFF000000),
    primaryContainer    = Color(0xFF870068),
    onPrimaryContainer  = Color(0xFFFFFFFF),
    secondary           = Color(0xFF90EDFF),
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF00516C),
    onSecondaryContainer= Color(0xFFFFFFFF),
    tertiary            = Color(0xFFDAF29A),
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF2D4900),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background          = Color(0xFF0C0C0C),
    onBackground        = Color(0xFFF0F0F0),
    surface             = Color(0xFF0C0C0C),
    onSurface           = Color(0xFFF0F0F0),
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFE0E0E0),
    outline             = Color(0xFFB8B8B8),
)

// ── Scheme selector ───────────────────────────────────────────────────────────

/**
 * Returns the [ColorScheme] for [theme].
 *
 * [systemIsDark] is only consulted when [theme] is [AppTheme.SYSTEM]; pass the
 * result of `isSystemInDarkTheme()` at the call site.
 *
 * [wcag] — when true the WCAG AAA accessible variant is returned for standard
 * palettes. HIGH_CONTRAST and BLUE_ORANGE are unaffected (already maximum-contrast).
 */
fun colorSchemeFor(theme: AppTheme, systemIsDark: Boolean = false, wcag: Boolean = false): ColorScheme {
    // HIGH_CONTRAST and BLUE_ORANGE are already maximum-contrast; wcag flag ignored.
    if (!wcag || theme == AppTheme.HIGH_CONTRAST_LIGHT || theme == AppTheme.HIGH_CONTRAST_DARK
        || theme == AppTheme.BLUE_ORANGE) {
        return standardColorSchemeFor(theme, systemIsDark)
    }
    // WCAG AAA variants for standard palettes
    return when (theme) {
        AppTheme.CORAL               -> CoralLightWcag
        AppTheme.CORAL_DARK          -> CoralDarkWcag
        AppTheme.CORAL_SYSTEM        -> if (systemIsDark) CoralDarkWcag        else CoralLightWcag
        AppTheme.TURQUOISE           -> TurquoiseLightWcag
        AppTheme.TURQUOISE_DARK      -> TurquoiseDarkWcag
        AppTheme.SYSTEM              -> if (systemIsDark) TurquoiseDarkWcag    else TurquoiseLightWcag
        AppTheme.GREEN               -> GreenLightWcag
        AppTheme.GREEN_DARK          -> GreenDarkWcag
        AppTheme.GREEN_SYSTEM        -> if (systemIsDark) GreenDarkWcag        else GreenLightWcag
        AppTheme.SUMMER_CANDY        -> SummerCandyLightWcag
        AppTheme.SUMMER_CANDY_DARK   -> SummerCandyDarkWcag
        AppTheme.SUMMER_CANDY_SYSTEM -> if (systemIsDark) SummerCandyDarkWcag  else SummerCandyLightWcag
        AppTheme.BEACH_VIBES         -> BeachVibesLightWcag
        AppTheme.BEACH_VIBES_DARK    -> BeachVibesDarkWcag
        AppTheme.BEACH_VIBES_SYSTEM  -> if (systemIsDark) BeachVibesDarkWcag   else BeachVibesLightWcag
        AppTheme.PEACH_MELBA         -> PeachMelbaLightWcag
        AppTheme.PEACH_MELBA_DARK    -> PeachMelbaDarkWcag
        AppTheme.PEACH_MELBA_SYSTEM  -> if (systemIsDark) PeachMelbaDarkWcag   else PeachMelbaLightWcag
        AppTheme.DISCO               -> DiscoLightWcag
        AppTheme.DISCO_DARK          -> DiscoDarkWcag
        AppTheme.DISCO_SYSTEM        -> if (systemIsDark) DiscoDarkWcag        else DiscoLightWcag
        AppTheme.METAL_CHICK         -> MetalChickLightWcag
        AppTheme.METAL_CHICK_DARK    -> MetalChickDarkWcag
        AppTheme.METAL_CHICK_SYSTEM  -> if (systemIsDark) MetalChickDarkWcag   else MetalChickLightWcag
        AppTheme.WHIMSY              -> WhimsyLightWcag
        AppTheme.WHIMSY_DARK         -> WhimsyDarkWcag
        AppTheme.WHIMSY_SYSTEM       -> if (systemIsDark) WhimsyDarkWcag       else WhimsyLightWcag
        AppTheme.COLOUR_HAPPY        -> ColourHappyLightWcag
        AppTheme.COLOUR_HAPPY_DARK   -> ColourHappyDarkWcag
        AppTheme.COLOUR_HAPPY_SYSTEM -> if (systemIsDark) ColourHappyDarkWcag  else ColourHappyLightWcag
        AppTheme.DRAGON_FIRE         -> DragonFireLightWcag
        AppTheme.DRAGON_FIRE_DARK    -> DragonFireDarkWcag
        AppTheme.DRAGON_FIRE_SYSTEM  -> if (systemIsDark) DragonFireDarkWcag   else DragonFireLightWcag
        AppTheme.MIDNIGHT_NEON       -> MidnightNeonLightWcag
        AppTheme.MIDNIGHT_NEON_DARK  -> MidnightNeonDarkWcag
        AppTheme.MIDNIGHT_NEON_SYSTEM-> if (systemIsDark) MidnightNeonDarkWcag else MidnightNeonLightWcag
        else -> standardColorSchemeFor(theme, systemIsDark)
    }
}

private fun standardColorSchemeFor(theme: AppTheme, systemIsDark: Boolean): ColorScheme = when (theme) {
    // Classic
    AppTheme.CORAL              -> CoralLight
    AppTheme.TURQUOISE          -> TurquoiseLight
    AppTheme.GREEN              -> GreenLight
    AppTheme.CORAL_DARK         -> CoralDark
    AppTheme.TURQUOISE_DARK     -> TurquoiseDark
    AppTheme.GREEN_DARK         -> GreenDark
    // Fun
    AppTheme.SUMMER_CANDY       -> SummerCandyLight
    AppTheme.SUMMER_CANDY_DARK  -> SummerCandyDark
    AppTheme.BEACH_VIBES        -> BeachVibesLight
    AppTheme.BEACH_VIBES_DARK   -> BeachVibesDark
    AppTheme.PEACH_MELBA        -> PeachMelbaLight
    AppTheme.PEACH_MELBA_DARK   -> PeachMelbaDark
    AppTheme.DISCO              -> DiscoLight
    AppTheme.DISCO_DARK         -> DiscoDark
    AppTheme.METAL_CHICK        -> MetalChickLight
    AppTheme.METAL_CHICK_DARK   -> MetalChickDark
    AppTheme.WHIMSY             -> WhimsyLight
    AppTheme.WHIMSY_DARK        -> WhimsyDark
    AppTheme.COLOUR_HAPPY       -> ColourHappyLight
    AppTheme.COLOUR_HAPPY_DARK  -> ColourHappyDark
    // System (each follows light/dark preference using its palette's own schemes)
    AppTheme.SYSTEM                -> if (systemIsDark) TurquoiseDark    else TurquoiseLight
    AppTheme.CORAL_SYSTEM          -> if (systemIsDark) CoralDark        else CoralLight
    AppTheme.GREEN_SYSTEM          -> if (systemIsDark) GreenDark        else GreenLight
    AppTheme.SUMMER_CANDY_SYSTEM   -> if (systemIsDark) SummerCandyDark  else SummerCandyLight
    AppTheme.BEACH_VIBES_SYSTEM    -> if (systemIsDark) BeachVibesDark   else BeachVibesLight
    AppTheme.PEACH_MELBA_SYSTEM    -> if (systemIsDark) PeachMelbaDark   else PeachMelbaLight
    AppTheme.DISCO_SYSTEM          -> if (systemIsDark) DiscoDark        else DiscoLight
    AppTheme.METAL_CHICK_SYSTEM    -> if (systemIsDark) MetalChickDark   else MetalChickLight
    AppTheme.WHIMSY_SYSTEM         -> if (systemIsDark) WhimsyDark       else WhimsyLight
    AppTheme.COLOUR_HAPPY_SYSTEM   -> if (systemIsDark) ColourHappyDark  else ColourHappyLight
    // Bold
    AppTheme.DRAGON_FIRE              -> DragonFireLight
    AppTheme.DRAGON_FIRE_DARK         -> DragonFireDark
    AppTheme.MIDNIGHT_NEON            -> MidnightNeonLight
    AppTheme.MIDNIGHT_NEON_DARK       -> MidnightNeonDark
    AppTheme.DRAGON_FIRE_SYSTEM       -> if (systemIsDark) DragonFireDark   else DragonFireLight
    AppTheme.MIDNIGHT_NEON_SYSTEM     -> if (systemIsDark) MidnightNeonDark else MidnightNeonLight
    AppTheme.HIGH_CONTRAST_LIGHT -> HighContrastLight
    AppTheme.HIGH_CONTRAST_DARK  -> HighContrastDark
    AppTheme.BLUE_ORANGE         -> BlueOrange
    AppTheme.CUSTOM              -> CoralLight  // Fallback; GoFloTheme builds the real scheme dynamically
}

/**
 * Builds a full Material 3 colour scheme from three HSL hue values (0–360°).
 *
 * When [primaryArgb]/[secondaryArgb]/[tertiaryArgb] are non-zero AND [isDark] matches
 * [pickedForDark], the actual picked ARGB is used directly as the semantic role colour so
 * the theme reflects exactly what the user chose. The complementary mode (light↔dark) is
 * always hue-derived so it remains readable without any user effort.
 */
fun buildCustomColorScheme(
    primaryHue:    Float,
    secondaryHue:  Float,
    tertiaryHue:   Float,
    primaryArgb:   Int = 0,
    secondaryArgb: Int = 0,
    tertiaryArgb:  Int = 0,
    isDark:        Boolean,
    pickedForDark: Boolean = false,
): ColorScheme {
    // Only use the actual picked ARGB when building the mode the user designed for.
    val useActual = isDark == pickedForDark

    fun resolveColor(argb: Int, hue: Float, darkS: Float, darkL: Float, lightS: Float, lightL: Float): Color {
        if (useActual && argb != 0) return Color(argb)
        return if (isDark) Color.hsl(hue, darkS, darkL) else Color.hsl(hue, lightS, lightL)
    }

    // WCAG-safe on-color for an actual picked ARGB: near-black on bright, white on dark.
    fun onArgb(argb: Int): Color {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8)  and 0xFF) / 255.0
        val b = ( argb         and 0xFF) / 255.0
        fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        val lum = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
        return if (lum > 0.35) Color(0xFF1C1B1F) else Color.White
    }

    val primary   = resolveColor(primaryArgb,   primaryHue,   0.75f, 0.75f, 0.60f, 0.35f)
    val secondary = resolveColor(secondaryArgb, secondaryHue, 0.55f, 0.70f, 0.45f, 0.35f)
    val tertiary  = resolveColor(tertiaryArgb,  tertiaryHue,  0.55f, 0.70f, 0.45f, 0.35f)

    val onPrimary   = when {
        useActual && primaryArgb   != 0 -> onArgb(primaryArgb)
        isDark                          -> Color.hsl(primaryHue,   0.75f, 0.10f)
        else                            -> Color.White
    }
    val onSecondary = when {
        useActual && secondaryArgb != 0 -> onArgb(secondaryArgb)
        isDark                          -> Color.hsl(secondaryHue, 0.55f, 0.10f)
        else                            -> Color.White
    }
    val onTertiary  = when {
        useActual && tertiaryArgb  != 0 -> onArgb(tertiaryArgb)
        isDark                          -> Color.hsl(tertiaryHue,  0.55f, 0.10f)
        else                            -> Color.White
    }

    return if (isDark) {
        darkColorScheme(
            primary             = primary,
            onPrimary           = onPrimary,
            primaryContainer    = Color.hsl(primaryHue,   0.55f, 0.25f),
            onPrimaryContainer  = Color.hsl(primaryHue,   0.55f, 0.90f),
            secondary           = secondary,
            onSecondary         = onSecondary,
            secondaryContainer  = Color.hsl(secondaryHue, 0.40f, 0.25f),
            onSecondaryContainer= Color.hsl(secondaryHue, 0.40f, 0.90f),
            tertiary            = tertiary,
            onTertiary          = onTertiary,
            tertiaryContainer   = Color.hsl(tertiaryHue,  0.40f, 0.25f),
            onTertiaryContainer = Color.hsl(tertiaryHue,  0.40f, 0.90f),
            background          = Color.hsl(primaryHue, 0.05f, 0.10f),
            onBackground        = Color.hsl(primaryHue, 0.10f, 0.90f),
            surface             = Color.hsl(primaryHue, 0.05f, 0.12f),
            onSurface           = Color.hsl(primaryHue, 0.10f, 0.90f),
            surfaceVariant      = Color.hsl(primaryHue, 0.15f, 0.20f),
            onSurfaceVariant    = Color.hsl(primaryHue, 0.10f, 0.75f),
            outline             = Color.hsl(primaryHue, 0.10f, 0.55f),
            outlineVariant      = Color.hsl(primaryHue, 0.08f, 0.30f),
            error               = Color(0xFFFFB4AB),
            onError             = Color(0xFF690005),
            errorContainer      = Color(0xFF93000A),
            onErrorContainer    = Color(0xFFFFDAD6),
            inverseSurface      = Color.hsl(primaryHue, 0.10f, 0.90f),
            inverseOnSurface    = Color.hsl(primaryHue, 0.05f, 0.15f),
            inversePrimary      = Color.hsl(primaryHue, 0.60f, 0.35f),
            scrim               = Color.Black,
            surfaceTint         = Color.hsl(primaryHue, 0.75f, 0.75f),
        )
    } else {
        lightColorScheme(
            primary             = primary,
            onPrimary           = onPrimary,
            primaryContainer    = Color.hsl(primaryHue,   0.55f, 0.90f),
            onPrimaryContainer  = Color.hsl(primaryHue,   0.55f, 0.10f),
            secondary           = secondary,
            onSecondary         = onSecondary,
            secondaryContainer  = Color.hsl(secondaryHue, 0.40f, 0.88f),
            onSecondaryContainer= Color.hsl(secondaryHue, 0.40f, 0.10f),
            tertiary            = tertiary,
            onTertiary          = onTertiary,
            tertiaryContainer   = Color.hsl(tertiaryHue,  0.40f, 0.88f),
            onTertiaryContainer = Color.hsl(tertiaryHue,  0.40f, 0.10f),
            background          = Color.hsl(primaryHue, 0.08f, 0.98f),
            onBackground        = Color.hsl(primaryHue, 0.25f, 0.10f),
            surface             = Color.hsl(primaryHue, 0.05f, 0.98f),
            onSurface           = Color.hsl(primaryHue, 0.25f, 0.10f),
            surfaceVariant      = Color.hsl(primaryHue, 0.20f, 0.90f),
            onSurfaceVariant    = Color.hsl(primaryHue, 0.15f, 0.30f),
            outline             = Color.hsl(primaryHue, 0.10f, 0.55f),
            outlineVariant      = Color.hsl(primaryHue, 0.08f, 0.80f),
            error               = Color(0xFFBA1A1A),
            onError             = Color.White,
            errorContainer      = Color(0xFFFFDAD6),
            onErrorContainer    = Color(0xFF410002),
            inverseSurface      = Color.hsl(primaryHue, 0.20f, 0.20f),
            inverseOnSurface    = Color.hsl(primaryHue, 0.08f, 0.95f),
            inversePrimary      = Color.hsl(primaryHue, 0.60f, 0.75f),
            scrim               = Color.Black,
            surfaceTint         = Color.hsl(primaryHue, 0.60f, 0.35f),
        )
    }
}

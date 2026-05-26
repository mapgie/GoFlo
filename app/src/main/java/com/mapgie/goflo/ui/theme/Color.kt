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
    // Metal Chick — gunmetal, crimson-lipstick, brushed chrome
    METAL_CHICK("Metal Chick",              ThemeGroup.LIGHT, 0xFF2E2E3AL),
    METAL_CHICK_DARK("Metal Chick",         ThemeGroup.DARK,  0xFFC7C5D6L, isDark = true),
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
    HIGH_CONTRAST_LIGHT("Light",  ThemeGroup.HIGH_CONTRAST, 0xFF1A1A1AL),
    HIGH_CONTRAST_DARK("Dark",    ThemeGroup.HIGH_CONTRAST, 0xFFFFFFFFFL, isDark = true),
    // Color-blind friendly (safe for deuteranopia & protanopia, ~9% of users)
    BLUE_ORANGE("Blue & Orange",  ThemeGroup.COLOR_BLIND,   0xFF005FADL),
}

// ── Classic light color schemes ───────────────────────────────────────────────
// Redesigned 2026-05 — see GoFlo Theme Redesign.md

// Coral: vivid coral reef — coral red · lagoon teal · sun-bleached gold
private val CoralLight = lightColorScheme(
    primary             = Color(0xFFC35040),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDAD2),
    onPrimaryContainer  = Color(0xFF3F0900),
    secondary           = Color(0xFF00817D),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFB4ECE7),
    onSecondaryContainer= Color(0xFF002523),
    tertiary            = Color(0xFF996A00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFE08D),
    onTertiaryContainer = Color(0xFF251A00),
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

// Sage: herb garden at dawn — sage green · terra-cotta clay · honey gold
private val GreenLight = lightColorScheme(
    primary             = Color(0xFF4F7D2B),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFC8F09F),
    onPrimaryContainer  = Color(0xFF112100),
    secondary           = Color(0xFFB5532A),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBC8),
    onSecondaryContainer= Color(0xFF3A1100),
    tertiary            = Color(0xFF8C6212),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFDF9C),
    onTertiaryContainer = Color(0xFF2A1D00),
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
    tertiary            = Color(0xFFFFD787),
    onTertiary          = Color(0xFF3D2D00),
    tertiaryContainer   = Color(0xFF574200),
    onTertiaryContainer = Color(0xFFFFE08D),
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
    tertiary            = Color(0xFFF4C16D),
    onTertiary          = Color(0xFF422C00),
    tertiaryContainer   = Color(0xFF5E4300),
    onTertiaryContainer = Color(0xFFFFDF9C),
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

// Summer Candy — bubblegum raspberry · mint aqua · lemon gold
private val SummerCandyLight = lightColorScheme(
    primary             = Color(0xFFD81B60),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFD8E5),
    onPrimaryContainer  = Color(0xFF40001B),
    secondary           = Color(0xFF008179),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFF6FF5E5),
    onSecondaryContainer= Color(0xFF00201D),
    tertiary            = Color(0xFF9E6A00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFE08D),
    onTertiaryContainer = Color(0xFF271B00),
    background          = Color(0xFFFFF7FA),
    onBackground        = Color(0xFF1E1316),
    surface             = Color(0xFFFFF7FA),
    onSurface           = Color(0xFF1E1316),
    surfaceVariant      = Color(0xFFF4DDE5),
    onSurfaceVariant    = Color(0xFF534249),
    outline             = Color(0xFF856D74),
)

// Beach Vibes — clear sea blue · sun-gold sand · sea-foam green
private val BeachVibesLight = lightColorScheme(
    primary             = Color(0xFF1265AF),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFD5E3FF),
    onPrimaryContainer  = Color(0xFF001C3D),
    secondary           = Color(0xFF9F6B00),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFE08D),
    onSecondaryContainer= Color(0xFF251A00),
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

// Peach Melba — apricot peach · raspberry · vanilla-caramel cream
private val PeachMelbaLight = lightColorScheme(
    primary             = Color(0xFFBC5A38),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDBC9),
    onPrimaryContainer  = Color(0xFF3A1300),
    secondary           = Color(0xFFB53369),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD9E3),
    onSecondaryContainer= Color(0xFF3F0024),
    tertiary            = Color(0xFF8A6926),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFE2A0),
    onTertiaryContainer = Color(0xFF2A1F00),
    background          = Color(0xFFFFF6F0),
    onBackground        = Color(0xFF201813),
    surface             = Color(0xFFFFF6F0),
    onSurface           = Color(0xFF201813),
    surfaceVariant      = Color(0xFFF4DED1),
    onSurfaceVariant    = Color(0xFF534439),
    outline             = Color(0xFF867262),
)

// All-Night Disco Party — hot magenta · electric violet · glitter gold
private val DiscoLight = lightColorScheme(
    primary             = Color(0xFFC1127A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFD6EA),
    onPrimaryContainer  = Color(0xFF3D003C),
    secondary           = Color(0xFF6E1FB5),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFECDCFF),
    onSecondaryContainer= Color(0xFF270060),
    tertiary            = Color(0xFF9A6B00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFE08C),
    onTertiaryContainer = Color(0xFF2A1D00),
    background          = Color(0xFFFFF7FB),
    onBackground        = Color(0xFF1E1020),
    surface             = Color(0xFFFFF7FB),
    onSurface           = Color(0xFF1E1020),
    surfaceVariant      = Color(0xFFF0DEEC),
    onSurfaceVariant    = Color(0xFF4F4452),
    outline             = Color(0xFF807385),
)

// Metal Chick — gunmetal · crimson-lipstick · brushed chrome
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

// Colour Me Happy — tomato red · sky blue · grass green
private val ColourHappyLight = lightColorScheme(
    primary             = Color(0xFFD63A26),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDAD2),
    onPrimaryContainer  = Color(0xFF410000),
    secondary           = Color(0xFF1872BD),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFD1E4FF),
    onSecondaryContainer= Color(0xFF001D36),
    tertiary            = Color(0xFF43852A),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFC6F0A1),
    onTertiaryContainer = Color(0xFF0F2200),
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
    tertiary            = Color(0xFFFFCD66),
    onTertiary          = Color(0xFF3F2D00),
    tertiaryContainer   = Color(0xFF5A4200),
    onTertiaryContainer = Color(0xFFFFE08D),
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
    secondary           = Color(0xFFF4C16D),
    onSecondary         = Color(0xFF422C00),
    secondaryContainer  = Color(0xFF5E4300),
    onSecondaryContainer= Color(0xFFFFE08D),
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
    tertiary            = Color(0xFFF4C16D),
    onTertiary          = Color(0xFF432D00),
    tertiaryContainer   = Color(0xFF5F4300),
    onTertiaryContainer = Color(0xFFFFE2A0),
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
    tertiary            = Color(0xFFFFD350),
    onTertiary          = Color(0xFF3A2A00),
    tertiaryContainer   = Color(0xFF523D00),
    onTertiaryContainer = Color(0xFFFFE08C),
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
    secondary           = Color(0xFFA0CAFF),
    onSecondary         = Color(0xFF002F66),
    secondaryContainer  = Color(0xFF00497D),
    onSecondaryContainer= Color(0xFFD1E4FF),
    tertiary            = Color(0xFFACD688),
    onTertiary          = Color(0xFF1A3900),
    tertiaryContainer   = Color(0xFF2D530B),
    onTertiaryContainer = Color(0xFFC6F0A1),
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

// Dragon Fire — ember red · molten orange · furnace gold
private val DragonFireLight = lightColorScheme(
    primary             = Color(0xFFB0181F),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFCFCC),
    onPrimaryContainer  = Color(0xFF3F0001),
    secondary           = Color(0xFFC04A0E),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDBC9),
    onSecondaryContainer= Color(0xFF3B1100),
    tertiary            = Color(0xFF9C6A00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFDE8E),
    onTertiaryContainer = Color(0xFF2D1F00),
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
    tertiary            = Color(0xFFFFCD66),
    onTertiary          = Color(0xFF422C00),
    tertiaryContainer   = Color(0xFF5E4300),
    onTertiaryContainer = Color(0xFFFFDE8E),
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
    tertiary            = Color(0xFF4C7A0E),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFC2F088),
    onTertiaryContainer = Color(0xFF0F2300),
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

// ── Scheme selector ───────────────────────────────────────────────────────────

/**
 * Returns the [ColorScheme] for [theme].
 *
 * [systemIsDark] is only consulted when [theme] is [AppTheme.SYSTEM]; pass the
 * result of `isSystemInDarkTheme()` at the call site.
 */
fun colorSchemeFor(theme: AppTheme, systemIsDark: Boolean = false): ColorScheme = when (theme) {
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
}

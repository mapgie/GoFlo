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
    CORAL("Coral",         ThemeGroup.LIGHT,         0xFFC15542L),
    TURQUOISE("Teal",      ThemeGroup.LIGHT,         0xFF00696FL),
    GREEN("Sage",          ThemeGroup.LIGHT,         0xFF386A20L),
    // Dark
    CORAL_DARK("Coral",       ThemeGroup.DARK,       0xFFFFB4A8L, isDark = true),
    TURQUOISE_DARK("Teal",    ThemeGroup.DARK,       0xFF80D5DBL, isDark = true),
    GREEN_DARK("Sage",        ThemeGroup.DARK,       0xFF9DD679L, isDark = true),

    // ── Fun palettes ──────────────────────────────────────────────────────────
    // Summer Candy — bubblegum raspberry, sherbet orange, candy glow
    SUMMER_CANDY("Summer Candy",            ThemeGroup.LIGHT, 0xFFC2185BL),
    SUMMER_CANDY_DARK("Summer Candy",       ThemeGroup.DARK,  0xFFFFB1CAL, isDark = true),
    // Beach Vibes — ocean blue, warm sand, sea-grass green
    BEACH_VIBES("Beach Vibes",              ThemeGroup.LIGHT, 0xFF1565C0L),
    BEACH_VIBES_DARK("Beach Vibes",         ThemeGroup.DARK,  0xFF9EC5FFL, isDark = true),
    // Peach Melba — terra cotta, creamy apricot, olive accent
    PEACH_MELBA("Peach Melba",              ThemeGroup.LIGHT, 0xFF9C5119L),
    PEACH_MELBA_DARK("Peach Melba",         ThemeGroup.DARK,  0xFFFFB98AL, isDark = true),
    // All-Night Disco Party — deep violet, electric magenta, disco gold
    DISCO("All-Night Disco Party",          ThemeGroup.LIGHT, 0xFF7B0EA0L),
    DISCO_DARK("All-Night Disco Party",     ThemeGroup.DARK,  0xFFE0ABFFL, isDark = true),
    // Metal Chick — charcoal slate, rose-chrome, steel grey
    METAL_CHICK("Metal Chick",              ThemeGroup.LIGHT, 0xFF4A4A5AL),
    METAL_CHICK_DARK("Metal Chick",         ThemeGroup.DARK,  0xFFC5C3D1L, isDark = true),
    // Whimsy Whispers — periwinkle lavender, dreamy mint, soft violet
    WHIMSY("Whimsy Whispers",              ThemeGroup.LIGHT, 0xFF5050A0L),
    WHIMSY_DARK("Whimsy Whispers",         ThemeGroup.DARK,  0xFFC4C0FFL, isDark = true),
    // Colour Me Happy — tropical coral-orange, electric blue, lime green
    COLOUR_HAPPY("Colour Me Happy",         ThemeGroup.LIGHT, 0xFFC13A00L),
    COLOUR_HAPPY_DARK("Colour Me Happy",    ThemeGroup.DARK,  0xFFFFB59AL, isDark = true),

    // ── System auto (palette-matched, follows device light/dark preference) ───
    SYSTEM("Follow system",       ThemeGroup.SYSTEM, 0xFF80D5DBL),
    CORAL_SYSTEM("Follow system", ThemeGroup.SYSTEM, 0xFFFFB4A8L),
    GREEN_SYSTEM("Follow system", ThemeGroup.SYSTEM, 0xFF9DD679L),

    // ── Accessibility ─────────────────────────────────────────────────────────
    HIGH_CONTRAST_LIGHT("Light",  ThemeGroup.HIGH_CONTRAST, 0xFF1A1A1AL),
    HIGH_CONTRAST_DARK("Dark",    ThemeGroup.HIGH_CONTRAST, 0xFFFFFFFFFL, isDark = true),
    // Color-blind friendly (safe for deuteranopia & protanopia, ~9% of users)
    BLUE_ORANGE("Blue & Orange",  ThemeGroup.COLOR_BLIND,   0xFF005FADL),
}

// ── Classic light color schemes ───────────────────────────────────────────────

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

// ── Classic dark color schemes ────────────────────────────────────────────────
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

// ── Fun light color schemes ───────────────────────────────────────────────────

// Summer Candy — bubblegum raspberry primary (#C2185B ≈ 5.7:1), deep rose secondary,
// sherbet-orange tertiary (#994F00 ≈ 5.9:1). Warm pink backgrounds.
private val SummerCandyLight = lightColorScheme(
    primary             = Color(0xFFC2185B),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFD6E8),
    onPrimaryContainer  = Color(0xFF3E001C),
    secondary           = Color(0xFFB03070),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD8EF),
    onSecondaryContainer= Color(0xFF3A0028),
    tertiary            = Color(0xFF994F00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF311600),
    background          = Color(0xFFFFF8FA),
    onBackground        = Color(0xFF201217),
    surface             = Color(0xFFFFF8FA),
    onSurface           = Color(0xFF201217),
    surfaceVariant      = Color(0xFFF5DEE7),
    onSurfaceVariant    = Color(0xFF534349),
    outline             = Color(0xFF856069),
)

// Beach Vibes — deep ocean blue primary (#1565C0 ≈ 7.8:1), warm amber secondary,
// sea-grass green tertiary (#3D6B3E ≈ 5.8:1). Coastal light backgrounds.
private val BeachVibesLight = lightColorScheme(
    primary             = Color(0xFF1565C0),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFD6E4FF),
    onPrimaryContainer  = Color(0xFF001848),
    secondary           = Color(0xFF7C5500),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDDB0),
    onSecondaryContainer= Color(0xFF271800),
    tertiary            = Color(0xFF3D6B3E),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFBCEBBE),
    onTertiaryContainer = Color(0xFF002108),
    background          = Color(0xFFF8FAFF),
    onBackground        = Color(0xFF181C22),
    surface             = Color(0xFFF8FAFF),
    onSurface           = Color(0xFF181C22),
    surfaceVariant      = Color(0xFFD8E3F4),
    onSurfaceVariant    = Color(0xFF3C4756),
    outline             = Color(0xFF6C7888),
)

// Peach Melba — terra-cotta primary (#9C5119 ≈ 5.7:1), warm caramel secondary,
// olive-green tertiary (#4E6539 ≈ 5.9:1). Creamy warm backgrounds.
private val PeachMelbaLight = lightColorScheme(
    primary             = Color(0xFF9C5119),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDCC7),
    onPrimaryContainer  = Color(0xFF341200),
    secondary           = Color(0xFF8A5835),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFDCBE),
    onSecondaryContainer= Color(0xFF321200),
    tertiary            = Color(0xFF4E6539),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFD0ECBA),
    onTertiaryContainer = Color(0xFF0C2000),
    background          = Color(0xFFFFF8F5),
    onBackground        = Color(0xFF201A16),
    surface             = Color(0xFFFFF8F5),
    onSurface           = Color(0xFF201A16),
    surfaceVariant      = Color(0xFFF2DDD4),
    onSurfaceVariant    = Color(0xFF52423C),
    outline             = Color(0xFF85726A),
)

// All-Night Disco Party — deep violet primary (#7B0EA0 ≈ 8.3:1), electric magenta
// secondary (#9C006E ≈ 7.9:1), disco gold tertiary (#8B6A00 ≈ 4.5:1).
private val DiscoLight = lightColorScheme(
    primary             = Color(0xFF7B0EA0),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFF2DAFF),
    onPrimaryContainer  = Color(0xFF2D0047),
    secondary           = Color(0xFF9C006E),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD8F2),
    onSecondaryContainer= Color(0xFF380030),
    tertiary            = Color(0xFF8B6A00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFFFE077),
    onTertiaryContainer = Color(0xFF292000),
    background          = Color(0xFFFFF8FE),
    onBackground        = Color(0xFF1E1020),
    surface             = Color(0xFFFFF8FE),
    onSurface           = Color(0xFF1E1020),
    surfaceVariant      = Color(0xFFEDDFF0),
    onSurfaceVariant    = Color(0xFF4C3E50),
    outline             = Color(0xFF7E6D83),
)

// Metal Chick — charcoal-slate primary (#4A4A5A ≈ 8.3:1), burgundy secondary
// (#6B2D3E ≈ 9.6:1), steel-grey tertiary. Polished cool backgrounds.
private val MetalChickLight = lightColorScheme(
    primary             = Color(0xFF4A4A5A),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFE2E0F0),
    onPrimaryContainer  = Color(0xFF0D0D1A),
    secondary           = Color(0xFF6B2D3E),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFFFD9E1),
    onSecondaryContainer= Color(0xFF270010),
    tertiary            = Color(0xFF585868),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFDEDEEA),
    onTertiaryContainer = Color(0xFF151520),
    background          = Color(0xFFF6F5FA),
    onBackground        = Color(0xFF1B1B22),
    surface             = Color(0xFFF6F5FA),
    onSurface           = Color(0xFF1B1B22),
    surfaceVariant      = Color(0xFFE0DEE8),
    onSurfaceVariant    = Color(0xFF474450),
    outline             = Color(0xFF787580),
)

// Whimsy Whispers — periwinkle primary (#5050A0 ≈ 6.7:1), muted violet-grey secondary,
// minty teal tertiary (#2D7A6E ≈ 4.8:1). Dreamy soft-focus backgrounds.
private val WhimsyLight = lightColorScheme(
    primary             = Color(0xFF5050A0),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFE3DFFF),
    onPrimaryContainer  = Color(0xFF0F0060),
    secondary           = Color(0xFF5C5A78),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFE3DFFF),
    onSecondaryContainer= Color(0xFF191640),
    tertiary            = Color(0xFF2D7A6E),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFB8EDE7),
    onTertiaryContainer = Color(0xFF002622),
    background          = Color(0xFFFBF8FF),
    onBackground        = Color(0xFF1C1A24),
    surface             = Color(0xFFFBF8FF),
    onSurface           = Color(0xFF1C1A24),
    surfaceVariant      = Color(0xFFE5DFFE),
    onSurfaceVariant    = Color(0xFF49454F),
    outline             = Color(0xFF7A7580),
)

// Colour Me Happy — tropical coral-orange primary (#C13A00 ≈ 5.1:1), electric blue
// secondary (#1B6FA8 ≈ 5.7:1), lime-green tertiary (#4D7C00 ≈ 5.8:1). Joyful warm-toned.
private val ColourHappyLight = lightColorScheme(
    primary             = Color(0xFFC13A00),
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFFFFDDD5),
    onPrimaryContainer  = Color(0xFF400F00),
    secondary           = Color(0xFF1B6FA8),
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFD1E5FF),
    onSecondaryContainer= Color(0xFF001E36),
    tertiary            = Color(0xFF4D7C00),
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFCBF176),
    onTertiaryContainer = Color(0xFF122500),
    background          = Color(0xFFFFF8F5),
    onBackground        = Color(0xFF201A16),
    surface             = Color(0xFFFFF8F5),
    onSurface           = Color(0xFF201A16),
    surfaceVariant      = Color(0xFFF4DDDA),
    onSurfaceVariant    = Color(0xFF514442),
    outline             = Color(0xFF847271),
)

// ── Fun dark color schemes ────────────────────────────────────────────────────

private val SummerCandyDark = darkColorScheme(
    primary             = Color(0xFFFFB1CA),
    onPrimary           = Color(0xFF67003A),
    primaryContainer    = Color(0xFF8C0052),
    onPrimaryContainer  = Color(0xFFFFD6E8),
    secondary           = Color(0xFFF3B4D2),
    onSecondary         = Color(0xFF4E0038),
    secondaryContainer  = Color(0xFF69004E),
    onSecondaryContainer= Color(0xFFFFD8EF),
    tertiary            = Color(0xFFFFB878),
    onTertiary          = Color(0xFF4E2700),
    tertiaryContainer   = Color(0xFF6E3900),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background          = Color(0xFF201217),
    onBackground        = Color(0xFFEDD9E2),
    surface             = Color(0xFF201217),
    onSurface           = Color(0xFFEDD9E2),
    surfaceVariant      = Color(0xFF534349),
    onSurfaceVariant    = Color(0xFFD7C0C7),
    outline             = Color(0xFFA48E94), // bumped from #A38D93 to pass 3:1 on surfaceVariant
)

private val BeachVibesDark = darkColorScheme(
    primary             = Color(0xFF9EC5FF),
    onPrimary           = Color(0xFF003171),
    primaryContainer    = Color(0xFF004A9D),
    onPrimaryContainer  = Color(0xFFD6E4FF),
    secondary           = Color(0xFFFFBB6E),
    onSecondary         = Color(0xFF422C00),
    secondaryContainer  = Color(0xFF5E3F00),
    onSecondaryContainer= Color(0xFFFFDDB0),
    tertiary            = Color(0xFF9EDEA0),
    onTertiary          = Color(0xFF09380F),
    tertiaryContainer   = Color(0xFF245228),
    onTertiaryContainer = Color(0xFFBAEBBC),
    background          = Color(0xFF181C22),
    onBackground        = Color(0xFFDDE3ED),
    surface             = Color(0xFF181C22),
    onSurface           = Color(0xFFDDE3ED),
    surfaceVariant      = Color(0xFF3C4756),
    onSurfaceVariant    = Color(0xFFBCC8D9),
    outline             = Color(0xFF8A939C), // bumped from #879099 to pass 3:1 on surfaceVariant
)

private val PeachMelbaDark = darkColorScheme(
    primary             = Color(0xFFFFB98A),
    onPrimary           = Color(0xFF542400),
    primaryContainer    = Color(0xFF763400),
    onPrimaryContainer  = Color(0xFFFFDCC7),
    secondary           = Color(0xFFFFBB8D),
    onSecondary         = Color(0xFF4C2800),
    secondaryContainer  = Color(0xFF6B3B00),
    onSecondaryContainer= Color(0xFFFFDCBE),
    tertiary            = Color(0xFFB5CD9A),
    onTertiary          = Color(0xFF21360B),
    tertiaryContainer   = Color(0xFF374D1F),
    onTertiaryContainer = Color(0xFFD1EDBA),
    background          = Color(0xFF201A16),
    onBackground        = Color(0xFFEDE0D8),
    surface             = Color(0xFF201A16),
    onSurface           = Color(0xFFEDE0D8),
    surfaceVariant      = Color(0xFF52423C),
    onSurfaceVariant    = Color(0xFFD5C5BB),
    outline             = Color(0xFF9E8E87),
)

private val DiscoDark = darkColorScheme(
    primary             = Color(0xFFE0ABFF),
    onPrimary           = Color(0xFF4B0075),
    primaryContainer    = Color(0xFF6500A3),
    onPrimaryContainer  = Color(0xFFF2DAFF),
    secondary           = Color(0xFFFFADE3),
    onSecondary         = Color(0xFF5B0047),
    secondaryContainer  = Color(0xFF800063),
    onSecondaryContainer= Color(0xFFFFD8F2),
    tertiary            = Color(0xFFEFCC2A),
    onTertiary          = Color(0xFF3B2E00),
    tertiaryContainer   = Color(0xFF544200),
    onTertiaryContainer = Color(0xFFFFE47E),
    background          = Color(0xFF1E1020),
    onBackground        = Color(0xFFEBD7F0),
    surface             = Color(0xFF1E1020),
    onSurface           = Color(0xFFEBD7F0),
    surfaceVariant      = Color(0xFF4C3E50),
    onSurfaceVariant    = Color(0xFFCFC3D4),
    outline             = Color(0xFF9A8D9E),
)

private val MetalChickDark = darkColorScheme(
    primary             = Color(0xFFC5C3D1),
    onPrimary           = Color(0xFF2D2B3B),
    primaryContainer    = Color(0xFF434153),
    onPrimaryContainer  = Color(0xFFE1DFF0),
    secondary           = Color(0xFFF1B8C6),
    onSecondary         = Color(0xFF4A1927),
    secondaryContainer  = Color(0xFF63303F),
    onSecondaryContainer= Color(0xFFFFD9E1),
    tertiary            = Color(0xFFCCCADA),
    onTertiary          = Color(0xFF333243),
    tertiaryContainer   = Color(0xFF4A495B),
    onTertiaryContainer = Color(0xFFE8E6F8),
    background          = Color(0xFF1B1B22),
    onBackground        = Color(0xFFE5E3ED),
    surface             = Color(0xFF1B1B22),
    onSurface           = Color(0xFFE5E3ED),
    surfaceVariant      = Color(0xFF474450),
    onSurfaceVariant    = Color(0xFFC9C6D2),
    outline             = Color(0xFF939099),
)

private val WhimsyDark = darkColorScheme(
    primary             = Color(0xFFC4C0FF),
    onPrimary           = Color(0xFF230070),
    primaryContainer    = Color(0xFF3C3A8C),
    onPrimaryContainer  = Color(0xFFE3DFFF),
    secondary           = Color(0xFFC7C4E3),
    onSecondary         = Color(0xFF2F2D4A),
    secondaryContainer  = Color(0xFF464362),
    onSecondaryContainer= Color(0xFFE3DFFF),
    tertiary            = Color(0xFF9DD6D0),
    onTertiary          = Color(0xFF003D38),
    tertiaryContainer   = Color(0xFF145750),
    onTertiaryContainer = Color(0xFFB8EDE7),
    background          = Color(0xFF1C1A24),
    onBackground        = Color(0xFFE6E1F0),
    surface             = Color(0xFF1C1A24),
    onSurface           = Color(0xFFE6E1F0),
    surfaceVariant      = Color(0xFF49454F),
    onSurfaceVariant    = Color(0xFFCBC5D0),
    outline             = Color(0xFF96909F), // bumped from #958F9E to pass 3:1 on surfaceVariant
)

private val ColourHappyDark = darkColorScheme(
    primary             = Color(0xFFFFB59A),
    onPrimary           = Color(0xFF601E00),
    primaryContainer    = Color(0xFF882B00),
    onPrimaryContainer  = Color(0xFFFFDDD5),
    secondary           = Color(0xFF9BCAFF),
    onSecondary         = Color(0xFF003358),
    secondaryContainer  = Color(0xFF004C7D),
    onSecondaryContainer= Color(0xFFD1E5FF),
    tertiary            = Color(0xFFB2D958),
    onTertiary          = Color(0xFF243E00),
    tertiaryContainer   = Color(0xFF395900),
    onTertiaryContainer = Color(0xFFCBF173),
    background          = Color(0xFF201A16),
    onBackground        = Color(0xFFEDE0DA),
    surface             = Color(0xFF201A16),
    onSurface           = Color(0xFFEDE0DA),
    surfaceVariant      = Color(0xFF514442),
    onSurfaceVariant    = Color(0xFFD5C3C0),
    outline             = Color(0xFFA18F8D), // bumped from #9E8C8A to pass 3:1 on surfaceVariant
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
    // System
    AppTheme.SYSTEM             -> if (systemIsDark) TurquoiseDark else TurquoiseLight
    AppTheme.CORAL_SYSTEM       -> if (systemIsDark) CoralDark else CoralLight
    AppTheme.GREEN_SYSTEM       -> if (systemIsDark) GreenDark else GreenLight
    AppTheme.HIGH_CONTRAST_LIGHT -> HighContrastLight
    AppTheme.HIGH_CONTRAST_DARK  -> HighContrastDark
    AppTheme.BLUE_ORANGE         -> BlueOrange
}

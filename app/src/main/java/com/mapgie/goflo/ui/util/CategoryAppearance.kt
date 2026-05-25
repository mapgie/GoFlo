package com.mapgie.goflo.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector

// ── Icon catalogue ────────────────────────────────────────────────────────────

/**
 * Curated set of Material icons users can assign to a custom tracking category.
 *
 * [key] is the string persisted in the database — it is stable and must NOT be
 * renamed.  [vector] is the rendered icon; [displayName] appears as a tooltip
 * in the icon picker.
 */
enum class CategoryIcon(
    val key: String,
    val displayName: String,
    val vector: ImageVector,
) {
    CATEGORY    ("category",    "General",      Icons.Outlined.Category),
    MOOD        ("mood",        "Mood",         Icons.Outlined.EmojiEmotions),
    HEART       ("heart",       "Heart",        Icons.Outlined.FavoriteBorder),
    FITNESS     ("fitness",     "Fitness",      Icons.Outlined.FitnessCenter),
    FOOD        ("food",        "Food",         Icons.Outlined.LocalDining),
    WATER       ("water",       "Water",        Icons.Outlined.WaterDrop),
    SLEEP       ("sleep",       "Sleep",        Icons.Outlined.Hotel),
    BEDTIME     ("bedtime",     "Rest",         Icons.Outlined.Bedtime),
    MEDITATION  ("meditation",  "Mindfulness",  Icons.Outlined.SelfImprovement),
    TEMPERATURE ("temperature", "Temperature",  Icons.Outlined.Thermostat),
    MEDICATION  ("medication",  "Medication",   Icons.Outlined.Medication),
    RUNNING     ("running",     "Running",      Icons.Outlined.DirectionsRun),
    SPA         ("spa",         "Wellness",     Icons.Outlined.Spa),
    PSYCHOLOGY  ("psychology",  "Mental",       Icons.Outlined.Psychology),
    BOLT        ("bolt",        "Energy",       Icons.Outlined.Bolt),
    CLOUD       ("cloud",       "Discharge",    Icons.Outlined.Cloud),
    BOOK        ("book",        "Journal",      Icons.Outlined.Book),
    CAFE        ("cafe",        "Caffeine",     Icons.Outlined.LocalCafe),
    HEALING     ("healing",     "Health",       Icons.Outlined.Healing),
    MONITOR     ("monitor",     "Vitals",       Icons.Outlined.MonitorHeart),
}

/**
 * Returns the [CategoryIcon] whose [CategoryIcon.key] matches [this], or
 * [CategoryIcon.CATEGORY] as a safe fallback for unrecognised/legacy keys.
 */
fun String.toCategoryIcon(): CategoryIcon =
    CategoryIcon.entries.firstOrNull { it.key == this } ?: CategoryIcon.CATEGORY

// ── Semantic colour tokens ────────────────────────────────────────────────────

/**
 * Theme-relative colour choices for category bubbles.
 *
 * [key] is persisted in the database.  Tokens resolve from
 * [androidx.compose.material3.ColorScheme] at render time, so bubbles
 * automatically follow the user's palette and light/dark mode.
 *
 * Custom colours are stored as 8-char uppercase hex strings (AARRGGBB) via
 * [Int.toHexColorKey].  [toCategoryColor] and [toCategoryOnColor] transparently
 * handle both token keys and hex strings, so the rest of the UI is unaware of
 * the distinction.
 */
enum class CategoryColor(
    val key: String,
    val displayName: String,
) {
    PRIMARY   ("primary",   "Primary"),
    SECONDARY ("secondary", "Secondary"),
    TERTIARY  ("tertiary",  "Accent"),
    ERROR     ("error",     "Error"),
}

/**
 * Extended colour palette offered in the "More colours" section of the picker.
 * Values are fully-opaque ARGB ints; convert to a storage key via [toHexColorKey].
 */
val CATEGORY_COLOR_OPTIONS: List<Int> = listOf(
    (0xFFE53935L).toInt(),  // Red 600
    (0xFFD81B60L).toInt(),  // Pink 600
    (0xFF8E24AAL).toInt(),  // Purple 700
    (0xFF3949ABL).toInt(),  // Indigo 600
    (0xFF1E88E5L).toInt(),  // Blue 600
    (0xFF00ACC1L).toInt(),  // Cyan 600
    (0xFF00897BL).toInt(),  // Teal 600
    (0xFF43A047L).toInt(),  // Green 600
    (0xFFF4511EL).toInt(),  // Deep Orange 600
    (0xFFE65100L).toInt(),  // Orange 900
    (0xFF8D6E63L).toInt(),  // Brown 400
    (0xFF546E7AL).toInt(),  // Blue Grey 600
)

/**
 * Converts an ARGB [Int] to the 8-char uppercase hex key used for storage.
 *
 * Example: `(0xFFE53935L).toInt().toHexColorKey()` → `"FFE53935"`
 */
fun Int.toHexColorKey(): String =
    (toLong() and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')

/**
 * Resolves [this] stored value to the bubble background [Color].
 *
 * Handles both semantic token keys ("primary", "secondary", …) and 8-char hex
 * strings produced by [Int.toHexColorKey].  Falls back to
 * [androidx.compose.material3.ColorScheme.secondary] for unrecognised values.
 */
@Composable
fun String.toCategoryColor(): Color {
    val s = MaterialTheme.colorScheme
    return when (this) {
        "primary"   -> s.primary
        "secondary" -> s.secondary
        "tertiary"  -> s.tertiary
        "error"     -> s.error
        else        -> runCatching { Color(toLong(16)) }.getOrDefault(s.secondary)
    }
}

/**
 * Resolves [this] stored value to the icon tint that passes contrast on
 * [toCategoryColor].
 *
 * For semantic tokens, the on* counterpart from [MaterialTheme.colorScheme] is
 * used (WCAG AA guaranteed by the Material spec in both light and dark).
 * For custom hex colours, luminance is checked: light backgrounds get a
 * near-black tint; dark backgrounds get white.
 */
@Composable
fun String.toCategoryOnColor(): Color {
    val s = MaterialTheme.colorScheme
    return when (this) {
        "primary"   -> s.onPrimary
        "secondary" -> s.onSecondary
        "tertiary"  -> s.onTertiary
        "error"     -> s.onError
        else        -> {
            val bg = runCatching { Color(toLong(16)) }.getOrDefault(s.secondary)
            // WCAG: contrast ≥ 3:1 for icons. luminance > 0.35 means the background
            // is light enough that white would fail — use near-black instead.
            if (bg.luminance() > 0.35f) Color(0xFF1C1B1F) else Color.White
        }
    }
}

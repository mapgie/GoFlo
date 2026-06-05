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
import androidx.compose.ui.graphics.lerp
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

// ── Category type ─────────────────────────────────────────────────────────────

/**
 * The input model for a tracking category.
 *
 * [key] is persisted in the database and must NOT be renamed.
 */
enum class CategoryType(val key: String, val displayName: String) {
    DEFAULT       ("default",        "Default"),
    NUMERIC_SLIDER("numeric_slider", "Slider scale"),
    NUMERIC_FREE  ("numeric_free",   "Numeric (Input)"),
    INCREMENT     ("increment",      "Plus One"),
}

fun String.toCategoryType(): CategoryType =
    CategoryType.entries.firstOrNull { it.key == this } ?: CategoryType.DEFAULT

// ── Slider scale labels ───────────────────────────────────────────────────────

/**
 * Encodes a sparse map of whole-number slider steps to optional text labels into
 * the storage string used by [TrackingCategory.scaleLabels].
 *
 * Format: newline-separated `value=label` pairs, sorted by value. Blank labels are
 * dropped. Labels are single-line, so `=` only splits on its first occurrence and
 * may safely appear inside a label.
 */
fun Map<Int, String>.encodeScaleLabels(): String =
    entries
        .filter { it.value.isNotBlank() }
        .sortedBy { it.key }
        .joinToString("\n") { "${it.key}=${it.value.trim()}" }

/** Decodes the [TrackingCategory.scaleLabels] storage string back into a step→label map. */
fun String.decodeScaleLabels(): Map<Int, String> {
    if (isBlank()) return emptyMap()
    return split('\n').mapNotNull { line ->
        val sep = line.indexOf('=')
        if (sep <= 0) return@mapNotNull null
        val key = line.substring(0, sep).toIntOrNull() ?: return@mapNotNull null
        val label = line.substring(sep + 1)
        if (label.isBlank()) null else key to label
    }.toMap()
}

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
    (0xFFFFCB0FL).toInt(),  // Amber
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
/**
 * Returns a shade of [base] for the [index]-th value in an ordinal sequence of
 * [total] values, blending toward [surface] for lower-order (lighter) values.
 *
 * index 0 → lightest (30% base, 70% surface); index total-1 → full [base].
 * With a single value, [base] is returned unchanged.
 */
fun ordinalShade(base: Color, surface: Color, index: Int, total: Int): Color {
    if (total <= 1) return base
    val fraction = index.toFloat() / (total - 1).toFloat()
    val t = 0.30f + 0.70f * fraction
    return lerp(surface, base, t)
}

/**
 * Continuous analogue of [ordinalShade] for heatmap cells: blends [surface] toward
 * [base] by a normalised [fraction] in 0..1 (lower = lighter, higher = more saturated).
 *
 * A small floor (0.15) keeps the lowest non-empty cell faintly tinted so it stays
 * visible against the surface. A single-hue lightness ramp like this is colour-blind
 * safe; callers still pair it with a label/legend so meaning is never colour-only.
 */
fun continuousShade(base: Color, surface: Color, fraction: Float): Color {
    val t = 0.15f + 0.85f * fraction.coerceIn(0f, 1f)
    return lerp(surface, base, t)
}

@Composable
fun String.toCategoryOnColor(): Color {
    val s = MaterialTheme.colorScheme
    return when (this) {
        "primary"   -> s.onPrimary
        "secondary" -> s.onSecondary
        "tertiary"  -> s.onTertiary
        else        -> {
            val bg = runCatching { Color(toLong(16)) }.getOrDefault(s.secondary)
            // WCAG: contrast ≥ 3:1 for icons. luminance > 0.35 means the background
            // is light enough that white would fail — use near-black instead.
            if (bg.luminance() > 0.35f) Color(0xFF1C1B1F) else Color.White
        }
    }
}

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
 * Tokens map to [androidx.compose.material3.ColorScheme] slots at render time,
 * so the bubble automatically updates whenever the user switches palette or
 * light/dark mode.  The [key] string is persisted in the database.
 *
 * Bubble background  → [resolve]
 * Icon tint          → [resolveOn]
 * Both pairs are guaranteed WCAG AA contrast by the Material colour system.
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

/** Resolves [this] token to the corresponding bubble background [Color]. */
@Composable
fun String.toCategoryColor(): Color {
    val s = MaterialTheme.colorScheme
    return when (this) {
        "primary"   -> s.primary
        "secondary" -> s.secondary
        "tertiary"  -> s.tertiary
        "error"     -> s.error
        else        -> s.secondary   // safe fallback
    }
}

/** Resolves [this] token to the icon tint that passes contrast on [toCategoryColor]. */
@Composable
fun String.toCategoryOnColor(): Color {
    val s = MaterialTheme.colorScheme
    return when (this) {
        "primary"   -> s.onPrimary
        "secondary" -> s.onSecondary
        "tertiary"  -> s.onTertiary
        "error"     -> s.onError
        else        -> s.onSecondary
    }
}

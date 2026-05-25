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
import androidx.compose.ui.graphics.vector.ImageVector

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

/**
 * Curated colour palette for category personalisation.
 * Values are fully-opaque ARGB [Int]s (Android @ColorInt convention — signed).
 * Material Design 600/700-range colours chosen for legibility on white icon tints.
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

/** Default category colour used when no explicit choice has been made. */
val DEFAULT_CATEGORY_COLOR: Int = (0xFF1976D2L).toInt()  // Blue 700

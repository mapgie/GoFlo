package com.mapgie.goflo.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined tracking category (e.g. Mood, Discharge, Ovulation Results).
 *
 * Two system categories — Flow and Symptoms — are seeded on first install with
 * [isSystem] = true. System categories cannot be deleted (enforced in the UI
 * layer), but their names, icons, and colours can be edited freely.
 *
 * [iconName] maps to a [com.mapgie.goflo.ui.util.CategoryIcon] key string.
 *
 * [colorToken] maps to a [com.mapgie.goflo.ui.util.CategoryColor] key string
 * ("primary", "secondary", "tertiary", "error").  The token is resolved to an
 * actual [androidx.compose.ui.graphics.Color] at render time via
 * [com.mapgie.goflo.ui.util.toCategoryColor], so the bubble automatically
 * follows the user's chosen palette and light/dark mode.
 */
@Entity(tableName = "tracking_categories")
data class TrackingCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** True for the built-in Flow and Symptoms categories seeded on install. */
    val isSystem: Boolean = false,
    val displayOrder: Int = 0,
    /** Key matching a [com.mapgie.goflo.ui.util.CategoryIcon] entry. */
    val iconName: String = "category",
    /** Theme-relative colour token — resolves live from MaterialTheme.colorScheme. */
    val colorToken: String = "secondary",
    /**
     * When true the category records a single numeric value (stored as its string
     * representation in [TrackingLogValue.valueLabel]) instead of a set of text labels.
     * The log screen shows a slider between [numericMin] and [numericMax].
     */
    val isNumeric: Boolean = false,
    /** Inclusive lower bound for numeric input. Ignored when [isNumeric] is false. */
    val numericMin: Float = 0f,
    /** Inclusive upper bound for numeric input. Ignored when [isNumeric] is false. */
    val numericMax: Float = 10f,
    /**
     * When true the slider snaps to one decimal place; when false it snaps to integers.
     * Ignored when [isNumeric] is false.
     */
    val allowDecimals: Boolean = false,
)


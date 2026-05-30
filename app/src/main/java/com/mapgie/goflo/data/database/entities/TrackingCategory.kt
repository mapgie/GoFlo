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
 * [systemKey] is a stable machine-readable identifier for system categories
 * ("flow", "symptoms", empty for non-system). Used for lookups that must survive
 * a user renaming the category.
 *
 * [iconName] maps to a [com.mapgie.goflo.ui.util.CategoryIcon] key string.
 *
 * [colorToken] maps to a [com.mapgie.goflo.ui.util.CategoryColor] key string
 * ("primary", "secondary", "tertiary").  The token is resolved to an actual
 * [androidx.compose.ui.graphics.Color] at render time via
 * [com.mapgie.goflo.ui.util.toCategoryColor], so the bubble automatically
 * follows the user's chosen palette and light/dark mode.
 *
 * [categoryType] is one of "default" | "numeric_slider" | "numeric_free" | "increment"
 * (see [com.mapgie.goflo.ui.util.CategoryType]).  It is immutable after creation.
 *
 * [numericUnit] is an optional suffix shown alongside numeric values (e.g. "°C").
 *
 * [scaleLabels] optionally maps individual whole-number slider steps to text
 * labels (e.g. 1→"Good", 3→"Neutral", 5→"Bad").  Encoded as newline-separated
 * "value=label" pairs; see [com.mapgie.goflo.ui.util.decodeScaleLabels].  Only
 * meaningful for the "numeric_slider" type.
 *
 * [isArchived] hides the category from the logging UI while preserving all data.
 */
@Entity(tableName = "tracking_categories")
data class TrackingCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isSystem: Boolean = false,
    val systemKey: String = "",
    val displayOrder: Int = 0,
    val iconName: String = "category",
    val colorToken: String = "secondary",
    val categoryType: String = "default",
    val numericMin: Float = 0f,
    val numericMax: Float = 10f,
    val allowDecimals: Boolean = false,
    val numericUnit: String = "",
    val scaleLabels: String = "",
    val isArchived: Boolean = false,
    val allowMultiple: Boolean = false,
    val showInLogPeriod: Boolean = false,
    val trackAgainstTime: Boolean = false,
) {
    val isNumeric: Boolean get() = categoryType != "default"
}

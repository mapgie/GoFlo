package com.mapgie.goflo.data.database.entities

import androidx.room.ColumnInfo
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
 * ("primary", "secondary", "tertiary", ...).  The token is resolved to an actual
 * [androidx.compose.ui.graphics.Color] at render time via
 * [com.mapgie.goflo.ui.util.toCategoryColor], so the bubble automatically
 * follows the user's chosen palette and light/dark mode.  The sentinel value
 * "inherit" defers to the owning group's [Group.colorRole] (neutral when the
 * category has no group); resolve it via
 * [com.mapgie.goflo.ui.util.effectiveColorToken] before rendering.
 *
 * [groupId] optionally files this category under a [Group].  Nullable, no
 * foreign key: deleting a group unfiles its members rather than cascading.
 *
 * [categoryType] is one of "default" | "numeric_slider" | "numeric_free" |
 * "increment" | "yes_no" | "time" (see [com.mapgie.goflo.ui.util.CategoryType]).
 * It is immutable after creation.  The "yes_no" and "time" types store their
 * readings as plain value-label strings ("Yes"/"No" and 24-hour "HH:mm"), so
 * they need no schema support beyond [TrackingLogValue].
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
    /** Stable key linking this category to a tracking mode preset (e.g. "bbt_temperature").
     *  Empty string for system categories and manually created categories.
     *  Used to deduplicate mode suggestions across modes. */
    @ColumnInfo(defaultValue = "") val modeKey: String = "",
    val groupId: Long? = null,
) {
    /**
     * Whether this category's stored value labels parse as numbers (drives the
     * numeric chart types in Stats).  Enumerated explicitly rather than
     * "anything but default" because "yes_no" and "time" store non-numeric
     * labels ("Yes"/"No", "HH:mm") and must chart as label categories.
     */
    val isNumeric: Boolean
        get() = categoryType == "numeric_slider" ||
            categoryType == "numeric_free" ||
            categoryType == "increment"
}

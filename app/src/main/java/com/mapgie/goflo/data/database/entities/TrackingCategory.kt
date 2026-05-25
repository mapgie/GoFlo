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
 * [colorArgb] is an Android @ColorInt (fully-opaque ARGB) used for the
 * coloured bubble displayed in the log menu and category list.
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
    /** Fully-opaque ARGB colour int for the category bubble. */
    val colorArgb: Int = (0xFF1976D2L).toInt(),   // Material Blue 700
)


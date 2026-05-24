package com.mapgie.goflo.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined tracking category (e.g. Mood, Discharge, Ovulation Results).
 *
 * Two system categories — Flow and Symptoms — are seeded on first install with
 * [isSystem] = true. System categories cannot be deleted (enforced in the UI
 * layer), but their names and values can be edited freely.
 */
@Entity(tableName = "tracking_categories")
data class TrackingCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** True for the built-in Flow and Symptoms categories seeded on install. */
    val isSystem: Boolean = false,
    val displayOrder: Int = 0
)

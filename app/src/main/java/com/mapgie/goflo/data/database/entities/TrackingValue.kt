package com.mapgie.goflo.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A selectable value/option within a [TrackingCategory] (e.g. "Happy" in Mood,
 * "Heavy" in Flow). Deleting a category cascades and removes its values.
 */
@Entity(
    tableName = "tracking_values",
    foreignKeys = [
        ForeignKey(
            entity = TrackingCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class TrackingValue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val label: String,
    val displayOrder: Int = 0,
    /** True for values that ship with a system category (Flow, Symptoms). Cannot be deleted. */
    @ColumnInfo(defaultValue = "0") val isSeeded: Boolean = false,
)

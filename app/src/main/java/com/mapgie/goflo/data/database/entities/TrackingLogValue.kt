package com.mapgie.goflo.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A selected value recorded in a [TrackingLog] entry.
 *
 * The value is stored as a **string snapshot** of the label at the time of
 * logging — not as a foreign key to [TrackingValue]. This ensures that
 * renaming or deleting a value from the catalog does not silently corrupt
 * historical records; past entries always show what was recorded.
 *
 * Deleting the parent log cascades and removes all its value rows.
 */
@Entity(
    tableName = "tracking_log_values",
    foreignKeys = [
        ForeignKey(
            entity = TrackingLog::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("logId")]
)
data class TrackingLogValue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId: Long,
    /** Snapshot of the value label at the time it was logged. */
    val valueLabel: String
)

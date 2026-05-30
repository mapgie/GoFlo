package com.mapgie.goflo.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A per-day log entry for a [TrackingCategory]. One record per (date, category)
 * pair — logging Flow on 2025-05-10 produces one row; logging Mood on the same
 * day produces a separate row.
 *
 * Deleting the parent category cascades and removes all its log entries.
 */
@Entity(
    tableName = "tracking_logs",
    foreignKeys = [
        ForeignKey(
            entity = TrackingCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("date")]
)
data class TrackingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO 8601 date string, e.g. "2025-05-10". */
    val date: String,
    val categoryId: Long,
    val notes: String = "",
    /** HH:mm time of logging, e.g. "13:37". Empty when not tracked against time. */
    val loggedAt: String = "",
)

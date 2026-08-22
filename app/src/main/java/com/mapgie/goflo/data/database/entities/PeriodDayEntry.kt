package com.mapgie.goflo.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged day of menstruation.
 *
 * This is the per-day source of truth for period tracking: each day the user
 * marks as a period day gets exactly one row (dates are unique). Period
 * *episodes* ([PeriodEntry] rows) are derived from these days by grouping
 * dates that are consecutive or within the user's gap tolerance of each
 * other — see PeriodRepository.rebuildEpisodes.
 *
 * Day-specific data (flow, symptoms, any other category logged for that day)
 * is not stored here; it lives in the tracking_logs table keyed by the same
 * ISO-8601 date string.
 */
@Entity(
    tableName = "period_days",
    indices = [Index(value = ["date"], unique = true)]
)
data class PeriodDayEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO-8601 date, e.g. "2026-08-22". */
    val date: String,
)

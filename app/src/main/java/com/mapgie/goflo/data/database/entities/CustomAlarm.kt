package com.mapgie.goflo.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_alarms")
data class CustomAlarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    /** "NOTIFICATION" | "ALARM" | "SILENT" */
    val alarmType: String = "NOTIFICATION",
    val overrideDnd: Boolean = false,
    val isRecurring: Boolean = true,
    /**
     * "DAILY"              — fires every day at the given time
     * "DURING_PERIOD"      — fires only on days the user has an active period
     * "NOT_DURING_PERIOD"  — fires only when no active period
     * "DAYS_BEFORE_PERIOD" — fires [daysOffset] days before the predicted next period start
     * "DAYS_AFTER_PERIOD"  — fires [daysOffset] days after the current period start
     * "DAY_OF_PERIOD"      — fires on day [dayOfPeriod] (1-based) of the current period
     */
    val scheduleType: String = "DAILY",
    val daysOffset: Int = 1,
    val dayOfPeriod: Int = 1,
    val snoozeDurationMinutes: Int = 10,
    val isEnabled: Boolean = true,
)

package com.mapgie.goflo.ui.navigation

import java.time.LocalDate

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Dashboard : Screen("dashboard")
    data object Stats : Screen("stats")
    data object StatsGrid : Screen("stats_grid")
    data object Settings : Screen("settings")
    data object LogPeriod : Screen("log_period?periodId={periodId}&startDate={startDate}") {
        fun withId(periodId: Long, targetDate: LocalDate? = null) =
            if (targetDate != null) "log_period?periodId=$periodId&startDate=$targetDate" else "log_period?periodId=$periodId"
        val newEntry = "log_period?periodId=-1"
        fun newEntryForDate(date: LocalDate) = "log_period?periodId=-1&startDate=$date"
    }
    data object PinSetup : Screen("pin_setup?changing={changing}") {
        val newPin = "pin_setup?changing=false"
        val changePin = "pin_setup?changing=true"
    }
    data object Licenses : Screen("licenses")
    data object Privacy  : Screen("privacy")

    // ── Tracking categories management ─────────────────────────────────────────────

    data object Manage : Screen("manage")
    data object TrackingModes : Screen("tracking_modes")
    data object NotificationsHub : Screen("notifications_hub")
    data object Reminders : Screen("reminders")

    data object ManageCategories : Screen("manage_categories")

    data object ManageCycle : Screen("manage_cycle")

    data object ManageQuickLog : Screen("manage_quick_log")

    data object ManageCategoryValues : Screen("manage_category_values/{categoryId}") {
        fun forCategory(id: Long) = "manage_category_values/$id"
    }

    // ── Custom alarms ──────────────────────────────────────────────────────────

    data object CustomAlarms : Screen("custom_alarms")

    data object EditAlarm : Screen("edit_alarm?alarmId={alarmId}&categoryId={categoryId}") {
        val newAlarm = "edit_alarm?alarmId=-1&categoryId=-1"
        fun forAlarm(alarmId: Long) = "edit_alarm?alarmId=$alarmId&categoryId=-1"
        fun newForCategory(categoryId: Long) = "edit_alarm?alarmId=-1&categoryId=$categoryId"
    }

    // ── Per-day category logging ────────────────────────────────────────────────

    /**
     * Route for logging or editing a tracking category entry.
     * - [categoryId] — the TrackingCategory.id to log
     * - [date] — ISO 8601 date string; omit to default to today
     * - [logId] — the existing TrackingLog.id when editing; omit for a new entry
     */
    data object LogCategory : Screen(
        "log_category/{categoryId}?date={date}&logId={logId}"
    ) {
        fun newEntry(categoryId: Long, date: LocalDate) =
            "log_category/$categoryId?date=$date"

        fun editEntry(categoryId: Long, logId: Long) =
            "log_category/$categoryId?logId=$logId"
    }
}

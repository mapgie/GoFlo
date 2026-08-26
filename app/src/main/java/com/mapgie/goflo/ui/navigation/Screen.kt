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

    /**
     * The 2-step category create/edit flow (logging redesign Phase 7).
     * - [categoryId] `> 0` edits an existing category; `-1` creates a new one.
     * - [groupId] `> 0` (create only) files the new category into that group on
     *   save and pre-selects the group's default input type.
     */
    data object CategoryEdit : Screen("category_edit?categoryId={categoryId}&groupId={groupId}") {
        val newCategory = "category_edit?categoryId=-1&groupId=-1"
        fun newInGroup(groupId: Long) = "category_edit?categoryId=-1&groupId=$groupId"
        fun forCategory(categoryId: Long) = "category_edit?categoryId=$categoryId&groupId=-1"
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

    // ── Unified day logging (logging redesign Phase 5) ─────────────────────────

    /**
     * Route for the unified day screen, where a running period is a state of
     * the day rather than a separate destination.
     *
     * Additive: [LogPeriod] and [LogCategory] stay registered and reachable
     * until the parity sign-off (removal is Phase 8 of the logging redesign).
     * - [date] — ISO 8601 date string; omit to default to today
     */
    data object LogDay : Screen("log_day?date={date}") {
        fun forDate(date: LocalDate) = "log_day?date=$date"
    }
}

package com.mapgie.goflo.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "goflo_prefs")

data class ReminderSettings(
    val preperiodEnabled: Boolean = false,
    val preperiodDaysBefore: Int = 2,
    val ovulationEnabled: Boolean = false,
    val dailyDuringPeriodEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    /** "NOTIFICATION" (inexact, no special permission) or "ALARM" (exact, requires SCHEDULE_EXACT_ALARM). */
    val deliveryMode: String = "NOTIFICATION",
    /** Custom label shown on the alarm screen when delivery mode is ALARM. */
    val alarmLabel: String = "",
)

data class AppPreferences(
    val theme: String = "CORAL",
    /**
     * The name of the [com.mapgie.goflo.AppIconChoice] enum entry the user has
     * selected.  Defaults to "DEFAULT" which matches the manifest's initial enabled alias.
     */
    val iconChoice: String = "DEFAULT",
    val reminder: ReminderSettings = ReminderSettings(),
    /**
     * User-preferred cycle length in days (21–45).
     * 0 means "auto" — the app calculates the average from logged history.
     */
    val preferredCycleLength: Int = 0,
    /**
     * The tracking category ID to open when the user taps the FAB (Quick Log).
     * -1L means "Log Period" (the default). Any other value is a TrackingCategory.id
     * and opens LogCategoryScreen for that category.
     */
    val quickLogCategoryId: Long = -1L,
    /** Whether to show predicted future period days on the calendar. */
    val showPeriodPrediction: Boolean = true,
    /** Whether to show ovulation day and fertility-window markers on the calendar. */
    val showOvulationMarkers: Boolean = true,
    /**
     * When true the colour picker switches every standard palette to its WCAG AAA
     * accessible variant (deeper/lighter primary colours, higher-contrast outlines).
     * MAX_CONTRAST and BLUE_ORANGE are unaffected — they are already maximum-contrast.
     */
    val wcagMode: Boolean = false,
    /** When true, the archive-category warning dialog is skipped permanently. */
    val archiveWarningDisabled: Boolean = false,
    val bannerStyle: String = "PLAIN",
    /**
     * True once the one-time migration of period flow data into TrackingLog has
     * been completed. Prevents the migration running on every app start.
     */
    val flowBackfillDone: Boolean = false,
    /**
     * When true, the GoFlo Status home-screen widget shows live cycle data even
     * if a PIN is set.  Users who trust their home screen can opt in; the default
     * (false) keeps the privacy placeholder when PIN lock is active.
     */
    val widgetDataVisible: Boolean = false,
    /** Whether the dashboard tab is enabled. */
    val dashboardEnabled: Boolean = false,
    /** JSON-encoded list of pinned stat combos. */
    val pinnedStats: String = "",
    /**
     * Comma-separated TrackingCategory IDs to show in the Quick Log (4×2) widget.
     * Empty string means "auto" — the first four active categories by displayOrder.
     */
    val widgetCategoryIds: String = "",
    /** Last-selected time range on the Stats screen. Encoded as "ALL_TIME", "YTD", "YEAR:2025", or "MONTH:2025-01". */
    val statsTimeRange: String = "YTD",
    /** Last-selected primary category ID on the Stats screen. -1 means none. */
    val statsCategory1Id: Long = -1L,
    /** Last-selected secondary category ID on the Stats screen. -1 means none. */
    val statsCategory2Id: Long = -1L,
    /** Last-selected chart type on the Stats screen. Empty means use auto default. */
    val statsChartType: String = "",
    /** Zoom level for the month bar chart. 0=compact, 1=normal, 2=wide. */
    val statsZoomLevel: Int = 1,
    /** Comma-separated TrackingCategory IDs selected as rows in the Grid (heatmap) view. */
    val heatmapCategoryIds: String = "",
    /** Day window for the Grid view columns: 30, 60, 90, 180, or 365. */
    val heatmapWindowDays: Int = 30,
    /** Per-cell aggregation for the Grid view: "SUM" or "AVERAGE". */
    val heatmapAggregation: String = "AVERAGE",
    /** Cell size for the Grid view. 0=compact, 1=normal, 2=wide. */
    val heatmapZoomLevel: Int = 1,
    /** True once the new-user onboarding banner has been dismissed. */
    val onboardingBannerDismissed: Boolean = false,
    /** Hue (0–360°) for the primary colour in the custom theme. */
    val customPrimaryHue: Float = 0f,
    /** Hue (0–360°) for the secondary colour in the custom theme. */
    val customSecondaryHue: Float = 200f,
    /** Hue (0–360°) for the tertiary colour in the custom theme. */
    val customTertiaryHue: Float = 330f,
    /** Comma-separated list of active tracking mode IDs (e.g. "FERTILITY,PREGNANCY"). */
    val activeModes: String = "",
    /** ISO-8601 date string for the pregnancy anchor date. Interpretation depends on [pregnancyStartType]. */
    val pregnancyDateStr: String = "",
    /** "EDD" if [pregnancyDateStr] is the expected due date; "LMP" if it is the last menstrual period. */
    val pregnancyStartType: String = "EDD",
    /** True when BBT temperature should be displayed in Celsius; false for Fahrenheit. */
    val temperatureUnitCelsius: Boolean = true,
)

class AppPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ICON_CHOICE = stringPreferencesKey("icon_choice")
        val PREPERIOD_ENABLED = booleanPreferencesKey("preperiod_enabled")
        val PREPERIOD_DAYS = intPreferencesKey("preperiod_days")
        val OVULATION_ENABLED = booleanPreferencesKey("ovulation_enabled")
        val DAILY_ENABLED = booleanPreferencesKey("daily_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_DELIVERY_MODE = stringPreferencesKey("reminder_delivery_mode")
        val ALARM_LABEL = stringPreferencesKey("alarm_label")
        val PREFERRED_CYCLE_LENGTH = intPreferencesKey("preferred_cycle_length")
        val QUICK_LOG_CATEGORY_ID = longPreferencesKey("quick_log_category_id")
        val SHOW_PERIOD_PREDICTION = booleanPreferencesKey("show_period_prediction")
        val SHOW_OVULATION_MARKERS = booleanPreferencesKey("show_ovulation_markers")
        val WCAG_MODE = booleanPreferencesKey("wcag_mode")
        val ARCHIVE_WARNING_DISABLED = booleanPreferencesKey("archive_warning_disabled")
        val BANNER_STYLE = stringPreferencesKey("banner_style")
        val FLOW_BACKFILL_DONE = booleanPreferencesKey("flow_backfill_done")
        val WIDGET_DATA_VISIBLE = booleanPreferencesKey("widget_data_visible")
        val DASHBOARD_ENABLED = booleanPreferencesKey("dashboard_enabled")
        val PINNED_STATS = stringPreferencesKey("pinned_stats")
        val WIDGET_CATEGORY_IDS = stringPreferencesKey("widget_category_ids")
        val STATS_TIME_RANGE = stringPreferencesKey("stats_time_range")
        val STATS_CATEGORY1_ID = longPreferencesKey("stats_category1_id")
        val STATS_CATEGORY2_ID = longPreferencesKey("stats_category2_id")
        val STATS_CHART_TYPE = stringPreferencesKey("stats_chart_type")
        val STATS_ZOOM_LEVEL = intPreferencesKey("stats_zoom_level")
        val HEATMAP_CATEGORY_IDS = stringPreferencesKey("heatmap_category_ids")
        val HEATMAP_WINDOW_DAYS = intPreferencesKey("heatmap_window_days")
        val HEATMAP_AGGREGATION = stringPreferencesKey("heatmap_aggregation")
        val HEATMAP_ZOOM_LEVEL = intPreferencesKey("heatmap_zoom_level")
        val ONBOARDING_BANNER_DISMISSED = booleanPreferencesKey("onboarding_banner_dismissed")
        val CUSTOM_PRIMARY_HUE   = floatPreferencesKey("custom_primary_hue")
        val CUSTOM_SECONDARY_HUE = floatPreferencesKey("custom_secondary_hue")
        val CUSTOM_TERTIARY_HUE  = floatPreferencesKey("custom_tertiary_hue")
        val ACTIVE_MODES              = stringPreferencesKey("active_modes")
        val PREGNANCY_DATE_STR        = stringPreferencesKey("pregnancy_date_str")
        val PREGNANCY_START_TYPE      = stringPreferencesKey("pregnancy_start_type")
        val TEMPERATURE_UNIT_CELSIUS  = booleanPreferencesKey("temperature_unit_celsius")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            theme = prefs[Keys.THEME] ?: "CORAL",
            iconChoice = prefs[Keys.ICON_CHOICE] ?: "DEFAULT",
            preferredCycleLength = prefs[Keys.PREFERRED_CYCLE_LENGTH] ?: 0,
            quickLogCategoryId = prefs[Keys.QUICK_LOG_CATEGORY_ID] ?: -1L,
            showPeriodPrediction = prefs[Keys.SHOW_PERIOD_PREDICTION] ?: true,
            showOvulationMarkers = prefs[Keys.SHOW_OVULATION_MARKERS] ?: true,
            wcagMode = prefs[Keys.WCAG_MODE] ?: false,
            archiveWarningDisabled = prefs[Keys.ARCHIVE_WARNING_DISABLED] ?: false,
            bannerStyle = prefs[Keys.BANNER_STYLE] ?: "PLAIN",
            flowBackfillDone = prefs[Keys.FLOW_BACKFILL_DONE] ?: false,
            widgetDataVisible = prefs[Keys.WIDGET_DATA_VISIBLE] ?: false,
            dashboardEnabled = prefs[Keys.DASHBOARD_ENABLED] ?: false,
            pinnedStats = prefs[Keys.PINNED_STATS] ?: "",
            widgetCategoryIds = prefs[Keys.WIDGET_CATEGORY_IDS] ?: "",
            statsTimeRange = prefs[Keys.STATS_TIME_RANGE] ?: "YTD",
            statsCategory1Id = prefs[Keys.STATS_CATEGORY1_ID] ?: -1L,
            statsCategory2Id = prefs[Keys.STATS_CATEGORY2_ID] ?: -1L,
            statsChartType = prefs[Keys.STATS_CHART_TYPE] ?: "",
            statsZoomLevel = prefs[Keys.STATS_ZOOM_LEVEL] ?: 1,
            heatmapCategoryIds = prefs[Keys.HEATMAP_CATEGORY_IDS] ?: "",
            heatmapWindowDays = prefs[Keys.HEATMAP_WINDOW_DAYS] ?: 30,
            heatmapAggregation = prefs[Keys.HEATMAP_AGGREGATION] ?: "AVERAGE",
            heatmapZoomLevel = prefs[Keys.HEATMAP_ZOOM_LEVEL] ?: 1,
            onboardingBannerDismissed = prefs[Keys.ONBOARDING_BANNER_DISMISSED] ?: false,
            customPrimaryHue       = prefs[Keys.CUSTOM_PRIMARY_HUE]          ?: 0f,
            customSecondaryHue     = prefs[Keys.CUSTOM_SECONDARY_HUE]        ?: 200f,
            customTertiaryHue      = prefs[Keys.CUSTOM_TERTIARY_HUE]         ?: 330f,
            activeModes            = prefs[Keys.ACTIVE_MODES]                ?: "",
            pregnancyDateStr       = prefs[Keys.PREGNANCY_DATE_STR]          ?: "",
            pregnancyStartType     = prefs[Keys.PREGNANCY_START_TYPE]        ?: "EDD",
            temperatureUnitCelsius = prefs[Keys.TEMPERATURE_UNIT_CELSIUS]    ?: true,
            reminder = ReminderSettings(
                preperiodEnabled = prefs[Keys.PREPERIOD_ENABLED] ?: false,
                preperiodDaysBefore = prefs[Keys.PREPERIOD_DAYS] ?: 2,
                ovulationEnabled = prefs[Keys.OVULATION_ENABLED] ?: false,
                dailyDuringPeriodEnabled = prefs[Keys.DAILY_ENABLED] ?: false,
                reminderHour = prefs[Keys.REMINDER_HOUR] ?: 8,
                reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
                deliveryMode = prefs[Keys.REMINDER_DELIVERY_MODE] ?: "NOTIFICATION",
                alarmLabel = prefs[Keys.ALARM_LABEL] ?: "",
            )
        )
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun setIconChoice(choice: String) {
        context.dataStore.edit { it[Keys.ICON_CHOICE] = choice }
    }

    suspend fun setPreperiodEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PREPERIOD_ENABLED] = enabled }
    }

    suspend fun setPreperiodDaysBefore(days: Int) {
        context.dataStore.edit { it[Keys.PREPERIOD_DAYS] = days }
    }

    suspend fun setOvulationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OVULATION_ENABLED] = enabled }
    }

    suspend fun setDailyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DAILY_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setReminderDeliveryMode(mode: String) {
        context.dataStore.edit { it[Keys.REMINDER_DELIVERY_MODE] = mode }
    }

    suspend fun setAlarmLabel(label: String) {
        context.dataStore.edit { it[Keys.ALARM_LABEL] = label }
    }

    /**
     * Persists the user's preferred cycle length.
     *
     * @param days 0 to clear the override (auto-calculated from history), or a value
     *             in the range 21–45 for a fixed length. Values outside these bounds
     *             are rejected with [IllegalArgumentException] to prevent silent
     *             corruption of cycle predictions.
     */
    suspend fun setShowPeriodPrediction(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_PERIOD_PREDICTION] = show }
    }

    suspend fun setShowOvulationMarkers(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_OVULATION_MARKERS] = show }
    }

    suspend fun setPreferredCycleLength(days: Int) {
        require(days == 0 || days in 21..90) {
            "preferredCycleLength must be 0 (auto) or in 21..90, got $days"
        }
        context.dataStore.edit { it[Keys.PREFERRED_CYCLE_LENGTH] = days }
    }

    /**
     * Sets the default Quick Log target.
     * @param categoryId -1L for "Log Period"; any other value is a TrackingCategory.id.
     */
    suspend fun setQuickLogCategoryId(categoryId: Long) {
        context.dataStore.edit { it[Keys.QUICK_LOG_CATEGORY_ID] = categoryId }
    }

    suspend fun setWcagMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WCAG_MODE] = enabled }
    }

    suspend fun setArchiveWarningDisabled(disabled: Boolean) {
        context.dataStore.edit { it[Keys.ARCHIVE_WARNING_DISABLED] = disabled }
    }

    suspend fun setBannerStyle(style: String) {
        context.dataStore.edit { it[Keys.BANNER_STYLE] = style }
    }

    suspend fun setFlowBackfillDone(done: Boolean) {
        context.dataStore.edit { it[Keys.FLOW_BACKFILL_DONE] = done }
    }

    suspend fun setWidgetDataVisible(visible: Boolean) {
        context.dataStore.edit { it[Keys.WIDGET_DATA_VISIBLE] = visible }
    }

    suspend fun setDashboardEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DASHBOARD_ENABLED] = enabled }
    }

    suspend fun setPinnedStats(json: String) {
        context.dataStore.edit { it[Keys.PINNED_STATS] = json }
    }

    suspend fun setWidgetCategoryIds(ids: String) {
        context.dataStore.edit { it[Keys.WIDGET_CATEGORY_IDS] = ids }
    }

    suspend fun setStatsTimeRange(encoded: String) {
        context.dataStore.edit { it[Keys.STATS_TIME_RANGE] = encoded }
    }

    suspend fun setStatsCategory1Id(id: Long) {
        context.dataStore.edit { it[Keys.STATS_CATEGORY1_ID] = id }
    }

    suspend fun setStatsCategory2Id(id: Long) {
        context.dataStore.edit { it[Keys.STATS_CATEGORY2_ID] = id }
    }

    suspend fun setStatsChartType(type: String) {
        context.dataStore.edit { it[Keys.STATS_CHART_TYPE] = type }
    }

    suspend fun setStatsZoomLevel(level: Int) {
        context.dataStore.edit { it[Keys.STATS_ZOOM_LEVEL] = level }
    }

    suspend fun setHeatmapCategoryIds(ids: String) {
        context.dataStore.edit { it[Keys.HEATMAP_CATEGORY_IDS] = ids }
    }

    suspend fun setHeatmapWindowDays(days: Int) {
        context.dataStore.edit { it[Keys.HEATMAP_WINDOW_DAYS] = days }
    }

    suspend fun setHeatmapAggregation(mode: String) {
        context.dataStore.edit { it[Keys.HEATMAP_AGGREGATION] = mode }
    }

    suspend fun setHeatmapZoomLevel(level: Int) {
        context.dataStore.edit { it[Keys.HEATMAP_ZOOM_LEVEL] = level }
    }

    suspend fun setOnboardingBannerDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_BANNER_DISMISSED] = dismissed }
    }

    suspend fun setCustomPrimaryHue(hue: Float) {
        context.dataStore.edit { it[Keys.CUSTOM_PRIMARY_HUE] = hue }
    }

    suspend fun setCustomSecondaryHue(hue: Float) {
        context.dataStore.edit { it[Keys.CUSTOM_SECONDARY_HUE] = hue }
    }

    suspend fun setCustomTertiaryHue(hue: Float) {
        context.dataStore.edit { it[Keys.CUSTOM_TERTIARY_HUE] = hue }
    }

    suspend fun setActiveModes(modes: String) {
        context.dataStore.edit { it[Keys.ACTIVE_MODES] = modes }
    }

    suspend fun setPregnancyDate(dateStr: String, startType: String) {
        context.dataStore.edit {
            it[Keys.PREGNANCY_DATE_STR]   = dateStr
            it[Keys.PREGNANCY_START_TYPE] = startType
        }
    }

    suspend fun setTemperatureUnitCelsius(celsius: Boolean) {
        context.dataStore.edit { it[Keys.TEMPERATURE_UNIT_CELSIUS] = celsius }
    }
}

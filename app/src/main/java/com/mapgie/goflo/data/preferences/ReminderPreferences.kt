package com.mapgie.goflo.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    val reminderMinute: Int = 0
)

data class AppPreferences(
    val theme: String = "CORAL",
    val reminder: ReminderSettings = ReminderSettings()
)

class AppPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val PREPERIOD_ENABLED = booleanPreferencesKey("preperiod_enabled")
        val PREPERIOD_DAYS = intPreferencesKey("preperiod_days")
        val OVULATION_ENABLED = booleanPreferencesKey("ovulation_enabled")
        val DAILY_ENABLED = booleanPreferencesKey("daily_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            theme = prefs[Keys.THEME] ?: "CORAL",
            reminder = ReminderSettings(
                preperiodEnabled = prefs[Keys.PREPERIOD_ENABLED] ?: false,
                preperiodDaysBefore = prefs[Keys.PREPERIOD_DAYS] ?: 2,
                ovulationEnabled = prefs[Keys.OVULATION_ENABLED] ?: false,
                dailyDuringPeriodEnabled = prefs[Keys.DAILY_ENABLED] ?: false,
                reminderHour = prefs[Keys.REMINDER_HOUR] ?: 8,
                reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0
            )
        )
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[Keys.THEME] = theme }
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
}

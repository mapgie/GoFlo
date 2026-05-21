package com.mapgie.goflo.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.preferences.AppPreferences
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: AppPreferencesStore,
    private val repository: PeriodRepository,
    private val context: Context
) : ViewModel() {

    val prefs: StateFlow<AppPreferences> = store.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    fun setTheme(theme: String) = viewModelScope.launch { store.setTheme(theme) }

    fun setPreperiodEnabled(enabled: Boolean) = viewModelScope.launch {
        store.setPreperiodEnabled(enabled)
        reschedule()
    }

    fun setPreperiodDays(days: Int) = viewModelScope.launch {
        store.setPreperiodDaysBefore(days)
        reschedule()
    }

    fun setOvulationEnabled(enabled: Boolean) = viewModelScope.launch {
        store.setOvulationEnabled(enabled)
        reschedule()
    }

    fun setDailyEnabled(enabled: Boolean) = viewModelScope.launch {
        store.setDailyEnabled(enabled)
        reschedule()
    }

    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        store.setReminderTime(hour, minute)
        reschedule()
    }

    private suspend fun reschedule() {
        val periods = repository.getAllPeriods().first()
        val settings = store.preferences.first().reminder
        ReminderScheduler.rescheduleAll(context, periods, settings)
    }

    class Factory(
        private val store: AppPreferencesStore,
        private val repository: PeriodRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(store, repository, context) as T
        }
    }
}

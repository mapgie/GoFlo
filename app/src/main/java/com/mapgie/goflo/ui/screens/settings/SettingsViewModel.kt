package com.mapgie.goflo.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.export.DataExporter
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.ImportResult
import com.mapgie.goflo.data.preferences.AppPreferences
import com.mapgie.goflo.data.preferences.SecurityPreferences
import com.mapgie.goflo.data.preferences.SecuritySettings
import com.mapgie.goflo.data.preferences.hasPinSet
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.security.PinManager
import com.mapgie.goflo.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: AppPreferencesStore,
    private val securityPreferences: SecurityPreferences,
    private val repository: PeriodRepository,
    private val context: Context
) : ViewModel() {

    val prefs: StateFlow<AppPreferences> = store.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    val securitySettings: StateFlow<SecuritySettings> = securityPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SecuritySettings())

    val isBiometricAvailable: Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

    // ── Theme ──────────────────────────────────────────────────────────────────

    fun setTheme(theme: String) = viewModelScope.launch { store.setTheme(theme) }

    // ── Reminders ─────────────────────────────────────────────────────────────

    fun setPreperiodEnabled(enabled: Boolean) = viewModelScope.launch {
        store.setPreperiodEnabled(enabled); reschedule()
    }

    fun setPreperiodDays(days: Int) = viewModelScope.launch {
        store.setPreperiodDaysBefore(days); reschedule()
    }

    fun setOvulationEnabled(enabled: Boolean) = viewModelScope.launch {
        store.setOvulationEnabled(enabled); reschedule()
    }

    fun setDailyEnabled(enabled: Boolean) = viewModelScope.launch {
        store.setDailyEnabled(enabled); reschedule()
    }

    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        store.setReminderTime(hour, minute); reschedule()
    }

    // ── Cycle ─────────────────────────────────────────────────────────────────

    /** 0 clears the override (reverts to auto-calculated average from history). */
    fun setPreferredCycleLength(days: Int) = viewModelScope.launch {
        store.setPreferredCycleLength(days)
    }

    // ── Security ──────────────────────────────────────────────────────────────

    // Requires the user to have entered the correct current PIN to call this.
    fun removePin(currentPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val settings = securityPreferences.settings.first()
            val valid = settings.hasPinSet &&
                    PinManager.verifyPin(currentPin, settings.pinHash, settings.pinSalt)
            if (valid) securityPreferences.clearPin()
            onResult(valid)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) = viewModelScope.launch {
        // Guard: biometric requires PIN; if PIN is gone, silently ignore.
        val settings = securityPreferences.settings.first()
        if (settings.hasPinSet) securityPreferences.setBiometricEnabled(enabled)
    }

    // ── Data lifecycle ────────────────────────────────────────────────────────

    /**
     * Serialises all periods and symptoms to JSON and delivers a share-sheet
     * Intent back to the caller. The Intent is ready for startActivity().
     */
    fun exportData(onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportData()
            val intent = DataExporter.buildShareIntent(context, json)
            onReady(intent)
        }
    }

    /**
     * Reads the JSON file at [uri] (from a share-sheet / Files app pick) and
     * imports the periods it contains. Runs on the ViewModel coroutine scope so
     * the UI stays responsive; calls [onResult] back on the main thread.
     */
    fun importData(uri: Uri, replace: Boolean, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val json = runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.readText()
            }.getOrNull()

            if (json == null) {
                onResult(ImportResult.Failure("Could not read the selected file."))
                return@launch
            }

            val result = repository.importData(json, replace)
            if (result is ImportResult.Success) reschedule()
            onResult(result)
        }
    }

    /**
     * Permanently deletes all stored periods and symptoms, then reschedules
     * reminders (which will cancel predictive alarms now that data is gone).
     */
    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllData()
            reschedule()
            onComplete()
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private suspend fun reschedule() {
        val periods = repository.getAllPeriods().first()
        val settings = store.preferences.first().reminder
        ReminderScheduler.rescheduleAll(context, periods, settings)
    }

    class Factory(
        private val store: AppPreferencesStore,
        private val securityPreferences: SecurityPreferences,
        private val repository: PeriodRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(store, securityPreferences, repository, context) as T
        }
    }
}

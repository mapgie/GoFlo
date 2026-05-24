package com.mapgie.goflo.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.mapgie.goflo.AppIconChoice
import com.mapgie.goflo.AppIconManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.export.DataExporter
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.data.repository.ImportResult
import com.mapgie.goflo.data.preferences.AppPreferences
import com.mapgie.goflo.data.preferences.SecurityPreferences
import com.mapgie.goflo.data.preferences.SecuritySettings
import com.mapgie.goflo.data.preferences.hasPinSet
import com.mapgie.goflo.data.repository.PeriodRepository
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.data.security.PinManager
import com.mapgie.goflo.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: AppPreferencesStore,
    private val securityPreferences: SecurityPreferences,
    private val repository: PeriodRepository,
    private val trackingRepository: TrackingRepository,
    private val context: Context
) : ViewModel() {

    val prefs: StateFlow<AppPreferences> = store.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    val securitySettings: StateFlow<SecuritySettings> = securityPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SecuritySettings())

    val trackingCategories: StateFlow<List<TrackingCategory>> =
        trackingRepository.getAllCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isBiometricAvailable: Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

    // ── Tracking ──────────────────────────────────────────────────────────────

    fun setQuickLogCategory(categoryId: Long) =
        viewModelScope.launch { store.setQuickLogCategoryId(categoryId) }

    // ── Theme ──────────────────────────────────────────────────────────────────

    fun setTheme(theme: String) = viewModelScope.launch { store.setTheme(theme) }

    // ── App icon ───────────────────────────────────────────────────────────────

    /**
     * Persists [choice] and immediately applies it as the active launcher alias.
     * Unlike the old behaviour, this is triggered only by an explicit user action —
     * never automatically on theme change — which eliminates the crash caused by
     * rapid successive calls to [PackageManager.setComponentEnabledSetting].
     */
    fun setIconChoice(choice: AppIconChoice) = viewModelScope.launch {
        store.setIconChoice(choice.name)
        AppIconManager.applyIcon(context, choice)
    }

    /**
     * Loads the image at [uri], scales it to a square, and asks the launcher to
     * create a pinned home-screen shortcut with that image as its icon.
     *
     * The shortcut launches the app normally.  Users can then hide the regular
     * app icon in their launcher's app-drawer settings to keep GoFlo discreet.
     *
     * [onError] is invoked on the main thread if anything goes wrong.
     */
    fun createCustomIconShortcut(uri: Uri, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = loadSquareBitmap(uri)
                val icon   = IconCompat.createWithBitmap(bitmap)
                val intent = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?: throw Exception("Could not resolve launch intent")

                val shortcut = ShortcutInfoCompat.Builder(
                    context,
                    "goflo_custom_${System.currentTimeMillis()}"
                )
                    .setShortLabel("GoFlo")
                    .setLongLabel("GoFlo")
                    .setIcon(icon)
                    .setIntent(intent)
                    .build()

                withContext(Dispatchers.Main) {
                    if (!ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)) {
                        onError("Your launcher doesn't support home-screen shortcuts.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Could not load the image. Please try a PNG or JPEG file.")
                }
            }
        }
    }

    /** Crops to a centred square then scales to 512 × 512 px. */
    private fun loadSquareBitmap(uri: Uri): Bitmap {
        val stream   = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image")
        val original = BitmapFactory.decodeStream(stream).also { stream.close() }
            ?: throw Exception("Cannot decode image")

        val size    = minOf(original.width, original.height)
        val cropped = Bitmap.createBitmap(
            original,
            (original.width - size) / 2,
            (original.height - size) / 2,
            size, size
        )
        return if (size <= 512) cropped
        else Bitmap.createScaledBitmap(cropped, 512, 512, true)
    }

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

    fun setShowPeriodPrediction(show: Boolean) = viewModelScope.launch {
        store.setShowPeriodPrediction(show)
    }

    fun setShowOvulationMarkers(show: Boolean) = viewModelScope.launch {
        store.setShowOvulationMarkers(show)
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
     * Serialises all periods to CSV (RFC 4180) and delivers a share-sheet
     * Intent back to the caller. The Intent is ready for startActivity().
     */
    fun exportCsv(onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val csv    = repository.exportAsCsv()
            val intent = DataExporter.buildCsvShareIntent(context, csv)
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
        private val trackingRepository: TrackingRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(store, securityPreferences, repository, trackingRepository, context) as T
        }
    }
}

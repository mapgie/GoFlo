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
import com.mapgie.goflo.data.export.ExportConfig
import com.mapgie.goflo.data.export.ExportFormat
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
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
        trackingRepository.getActiveCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All categories (active + archived) for display in the export dialog. */
    val allCategoriesForExport: StateFlow<List<TrackingCategory>> =
        trackingRepository.getAllCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isBiometricAvailable: Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

    // ── Tracking ──────────────────────────────────────────────────────────────

    fun setQuickLogCategory(categoryId: Long) =
        viewModelScope.launch { store.setQuickLogCategoryId(categoryId) }

    // ── Widget ────────────────────────────────────────────────────────────────

    fun setWidgetDataVisible(visible: Boolean) =
        viewModelScope.launch { store.setWidgetDataVisible(visible) }

    // ── Theme ──────────────────────────────────────────────────────────────────

    fun setTheme(theme: String) = viewModelScope.launch { store.setTheme(theme) }

    fun setWcagMode(enabled: Boolean) = viewModelScope.launch { store.setWcagMode(enabled) }

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
     * Exports data according to [config] — selected categories, date range, and format —
     * then delivers a share-sheet Intent to [onReady].
     */
    fun exportWithOptions(config: ExportConfig, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val (startDate, endDate) = config.effectiveDateRange
            val content = when (config.format) {
                ExportFormat.JSON -> buildJsonExport(config, startDate, endDate)
                ExportFormat.CSV  -> buildCsvExport(config, startDate, endDate)
            }
            val intent = when (config.format) {
                ExportFormat.JSON -> DataExporter.buildShareIntent(context, content)
                ExportFormat.CSV  -> DataExporter.buildCsvShareIntent(context, content)
            }
            onReady(intent)
        }
    }

    private suspend fun buildJsonExport(
        config: ExportConfig,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): String {
        // Resolve actual data bounds for metadata when no explicit range is given.
        val metaFrom = startDate ?: trackingRepository.getEarliestLogDate()
        val metaTo   = endDate   ?: LocalDate.now()

        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", LocalDate.now().toString())
        val rangeObj = JSONObject()
        rangeObj.put("from", metaFrom?.toString() ?: JSONObject.NULL)
        rangeObj.put("to", metaTo.toString())
        root.put("dateRange", rangeObj)

        if (config.includePeriods) {
            val allPeriods = repository.getAllPeriods().first().sortedBy { it.startDate }
            val periods = if (startDate != null && endDate != null) {
                allPeriods.filter {
                    val s = LocalDate.parse(it.startDate)
                    val e = it.endDate?.let { d -> LocalDate.parse(d) } ?: s
                    s <= endDate && e >= startDate
                }
            } else allPeriods
            val allSymptoms = repository.getAllSymptomsOnce()
            val symptomsByPeriod = allSymptoms.groupBy { it.periodId }
            val periodsArray = JSONArray()
            periods.forEach { period ->
                val obj = JSONObject().apply {
                    put("id", period.id)
                    put("startDate", period.startDate)
                    put("endDate", if (period.endDate != null) period.endDate else JSONObject.NULL)
                    put("flowLevel", period.flowLevel)
                    put("notes", period.notes)
                    val symptomsArray = JSONArray()
                    symptomsByPeriod[period.id]?.forEach { symptomsArray.put(it.symptomType) }
                    put("symptoms", symptomsArray)
                }
                periodsArray.put(obj)
            }
            root.put("periods", periodsArray)
        }

        if (config.selectedCategoryIds.isNotEmpty()) {
            val logs = trackingRepository.exportTrackingLogs(
                config.selectedCategoryIds.toList(), startDate, endDate
            )
            val byCategory = logs.groupBy { it.log.categoryId }
            val allCats = trackingRepository.getAllCategoriesOnce()
                .filter { it.id in config.selectedCategoryIds }
            val trackingArray = JSONArray()
            allCats.forEach { cat ->
                val catObj = JSONObject()
                catObj.put("id", cat.id)
                catObj.put("name", cat.name)
                catObj.put("archived", cat.isArchived)
                catObj.put("type", cat.categoryType)
                val logsArray = JSONArray()
                byCategory[cat.id]?.forEach { entry ->
                    val logObj = JSONObject()
                    logObj.put("date", entry.log.date)
                    val valArray = JSONArray()
                    entry.values.forEach { valArray.put(it) }
                    logObj.put("values", valArray)
                    logObj.put("notes", entry.log.notes)
                    logsArray.put(logObj)
                }
                catObj.put("logs", logsArray)
                trackingArray.put(catObj)
            }
            root.put("tracking", trackingArray)
        }

        return root.toString(2)
    }

    private suspend fun buildCsvExport(
        config: ExportConfig,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): String {
        val sb = StringBuilder()

        if (config.includePeriods) {
            val allPeriods = repository.getAllPeriods().first().sortedBy { it.startDate }
            val periods = if (startDate != null && endDate != null) {
                allPeriods.filter {
                    val s = LocalDate.parse(it.startDate)
                    val e = it.endDate?.let { d -> LocalDate.parse(d) } ?: s
                    s <= endDate && e >= startDate
                }
            } else allPeriods
            val allSymptoms = repository.getAllSymptomsOnce()
            val symptomsByPeriod = allSymptoms.groupBy { it.periodId }
            sb.appendLine("# Periods")
            sb.appendLine("start_date,end_date,duration_days,flow_level,symptoms,notes")
            periods.forEach { period ->
                val start    = LocalDate.parse(period.startDate)
                val end      = period.endDate?.let { LocalDate.parse(it) }
                val duration = if (end != null) (ChronoUnit.DAYS.between(start, end) + 1).toString() else ""
                val symptoms = sanitizeCsvField(
                    symptomsByPeriod[period.id]
                        ?.joinToString(";") { it.symptomType }?.replace("\"", "\"\"") ?: ""
                )
                val notes = sanitizeCsvField(period.notes.replace("\"", "\"\""))
                sb.appendLine("${period.startDate},${period.endDate ?: ""},${duration},${period.flowLevel},\"${symptoms}\",\"${notes}\"")
            }
        }

        if (config.selectedCategoryIds.isNotEmpty()) {
            val logs = trackingRepository.exportTrackingLogs(
                config.selectedCategoryIds.toList(), startDate, endDate
            )
            if (config.includePeriods && logs.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("# Tracking")
            }
            if (logs.isNotEmpty() || config.selectedCategoryIds.isNotEmpty()) {
                sb.appendLine("date,category,values,notes")
            }
            logs.forEach { entry ->
                val catName  = sanitizeCsvField((entry.category?.name ?: "").replace("\"", "\"\""))
                val values   = sanitizeCsvField(entry.values.joinToString(";").replace("\"", "\"\""))
                val notes    = sanitizeCsvField(entry.log.notes.replace("\"", "\"\""))
                sb.appendLine("${entry.log.date},\"${catName}\",\"${values}\",\"${notes}\"")
            }
        }

        return sb.toString()
    }

    private fun sanitizeCsvField(value: String): String {
        val dangerChars = setOf('=', '+', '-', '@', '\t', '\r')
        return if (value.isNotEmpty() && value[0] in dangerChars) "\t$value" else value
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

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

    fun setWidgetCategoryIds(ids: String) =
        viewModelScope.launch { store.setWidgetCategoryIds(ids) }

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

    fun setReminderDeliveryMode(mode: String) = viewModelScope.launch {
        store.setReminderDeliveryMode(mode); reschedule()
    }

    fun setAlarmLabel(label: String) = viewModelScope.launch {
        store.setAlarmLabel(label); reschedule()
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
     * Generates a plain-text cycle summary covering the last 12 months and
     * delivers a share-sheet Intent suitable for emailing to a healthcare provider.
     */
    fun exportDoctorVisit(onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val text   = buildDoctorVisitReport()
            val intent = DataExporter.buildTextShareIntent(context, text)
            onReady(intent)
        }
    }

    private suspend fun buildDoctorVisitReport(): String {
        val periods = repository.getAllPeriods().first().sortedBy { it.startDate }
        val allSymptoms = repository.getAllSymptomsOnce()
        val symptomsByPeriod = allSymptoms.groupBy { it.periodId }
        val avgCycle = PeriodRepository.calculateAvgCycleLength(periods)
        val cutoff = LocalDate.now().minusMonths(12)
        val today = LocalDate.now()

        val sb = StringBuilder()
        sb.appendLine("GoFlo Cycle Summary")
        sb.appendLine("Generated: $today")
        sb.appendLine("=".repeat(40))
        sb.appendLine()
        sb.appendLine("Average cycle length: $avgCycle days")
        sb.appendLine("Total cycles logged: ${maxOf(0, periods.size - 1)}")
        sb.appendLine()

        sb.appendLine("Recent Periods (last 12 months)")
        sb.appendLine("-".repeat(40))
        val recent = periods.filter { LocalDate.parse(it.startDate) >= cutoff }
        if (recent.isEmpty()) {
            sb.appendLine("No periods logged in this period.")
        } else {
            recent.forEach { period ->
                val start = LocalDate.parse(period.startDate)
                val end   = period.endDate?.let { LocalDate.parse(it) }
                val dur   = if (end != null) "${ChronoUnit.DAYS.between(start, end) + 1} days" else "Ongoing"
                sb.appendLine("${period.startDate}  Flow: ${period.flowLevel}  Duration: $dur")
                val syms = symptomsByPeriod[period.id]?.map { it.symptomType } ?: emptyList()
                if (syms.isNotEmpty()) sb.appendLine("  Symptoms: ${syms.joinToString(", ")}")
                if (period.notes.isNotBlank()) sb.appendLine("  Notes: ${period.notes}")
            }
        }

        sb.appendLine()
        sb.appendLine("Symptom Frequency (last 12 months)")
        sb.appendLine("-".repeat(40))
        val allCategories = trackingRepository.getAllCategoriesOnce()
        val sympCat = allCategories.firstOrNull { it.systemKey == "symptoms" }
        if (sympCat != null) {
            val counts = trackingRepository.getValueCountsForCategory(sympCat.id, cutoff, today)
            if (counts.isNotEmpty()) {
                val total = counts.sumOf { it.count }
                sb.appendLine("${total} total logged:")
                counts.sortedByDescending { it.count }.forEach { vc ->
                    sb.appendLine("  ${vc.valueLabel}: ${vc.count} times")
                }
            } else {
                sb.appendLine("No symptoms logged in this period.")
            }
        }

        val userCats = allCategories.filter { !it.isArchived && !it.isSystem }
        if (userCats.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Other Tracked Categories (last 12 months)")
            sb.appendLine("-".repeat(40))
            for (cat in userCats) {
                val counts = trackingRepository.getValueCountsForCategory(cat.id, cutoff, today)
                if (counts.isEmpty()) continue
                val total = counts.sumOf { it.count }
                sb.appendLine("${cat.name} ($total entries):")
                counts.sortedByDescending { it.count }.take(5).forEach { vc ->
                    val pct = vc.count * 100 / total.coerceAtLeast(1)
                    sb.appendLine("  ${vc.valueLabel}: ${vc.count}x ($pct%)")
                }
            }
        }

        sb.appendLine()
        sb.appendLine("=".repeat(40))
        sb.appendLine("Exported from GoFlo")
        return sb.toString()
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
        root.put("version", if (config.fullBackup) 3 else 2)
        root.put("exportedAt", LocalDate.now().toString())
        if (config.fullBackup) root.put("fullBackup", true)
        val rangeObj = JSONObject()
        rangeObj.put("from", metaFrom?.toString() ?: JSONObject.NULL)
        rangeObj.put("to", metaTo.toString())
        root.put("dateRange", rangeObj)

        // ── Full backup: category configuration ──────────────────────────────
        if (config.fullBackup) {
            val allCats = trackingRepository.getAllCategoriesOnce()
            val catsArray = JSONArray()
            allCats.forEach { cat ->
                val catObj = JSONObject()
                catObj.put("id", cat.id)
                catObj.put("name", cat.name)
                catObj.put("isSystem", cat.isSystem)
                catObj.put("systemKey", cat.systemKey)
                catObj.put("displayOrder", cat.displayOrder)
                catObj.put("iconName", cat.iconName)
                catObj.put("colorToken", cat.colorToken)
                catObj.put("categoryType", cat.categoryType)
                catObj.put("numericMin", cat.numericMin)
                catObj.put("numericMax", cat.numericMax)
                catObj.put("allowDecimals", cat.allowDecimals)
                catObj.put("numericUnit", cat.numericUnit)
                catObj.put("scaleLabels", cat.scaleLabels)
                catObj.put("isArchived", cat.isArchived)
                catObj.put("allowMultiple", cat.allowMultiple)
                catObj.put("showInLogPeriod", cat.showInLogPeriod)
                catObj.put("trackAgainstTime", cat.trackAgainstTime)
                val valArr = JSONArray()
                trackingRepository.getValuesForCategoryOnce(cat.id)
                    .sortedBy { it.displayOrder }
                    .forEach { tv -> valArr.put(tv.label) }
                catObj.put("values", valArr)
                catsArray.put(catObj)
            }
            root.put("categories", catsArray)

            val pinnedStats = store.preferences.first().pinnedStats
            if (pinnedStats.isNotBlank()) root.put("pinnedStats", pinnedStats)
        }

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
     * Reads the JSON file at [uri] and imports all data it contains:
     *  - Period logs and symptoms (v1 bare-array and v2 wrapper formats)
     *  - Tracking logs per category (v2 "tracking" key only)
     *
     * Runs on the ViewModel coroutine scope so the UI stays responsive;
     * calls [onResult] back on the main thread.
     *
     * NOTE: if new export sections are added to exportWithOptions / buildJsonExport,
     * this method must be updated to import them as well.
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

            // In replace mode, wipe tracking logs before importing the new set.
            // Period deletion is handled inside PeriodRepository.importData.
            if (replace) {
                trackingRepository.deleteAllLogs()
            }

            val result = repository.importData(json, replace)
            if (result is ImportResult.Success) {
                runCatching {
                    if (!json.trimStart().startsWith('[')) {
                        val root = JSONObject(json)
                        // Full backup: restore category configuration first so log import can match names.
                        val categoriesArray = root.optJSONArray("categories")
                        if (categoriesArray != null) {
                            importCategoryConfig(categoriesArray, replace)
                        }
                        val trackingArray = root.optJSONArray("tracking")
                        if (trackingArray != null) {
                            importTrackingLogs(trackingArray, replace)
                        }
                        // Restore pinned stats if present.
                        val pinnedStats = root.optString("pinnedStats", "")
                        if (pinnedStats.isNotBlank()) {
                            store.setPinnedStats(pinnedStats)
                        }
                    }
                } // import errors are non-fatal; period result still reported
                reschedule()
            }
            onResult(result)
        }
    }

    private suspend fun importTrackingLogs(trackingArray: JSONArray, replace: Boolean) {
        for (i in 0 until trackingArray.length()) {
            val catObj = trackingArray.getJSONObject(i)
            val catName = catObj.optString("name").takeIf { it.isNotBlank() } ?: continue
            val catType = catObj.optString("type", "default")
            val isArchived = catObj.optBoolean("archived", false)

            var category = trackingRepository.getCategoryByName(catName)
            if (category == null) {
                val newId = trackingRepository.addCategory(name = catName, categoryType = catType)
                if (isArchived) trackingRepository.archiveCategory(newId)
                category = trackingRepository.getCategoryByIdOnce(newId)
            }
            val categoryId = category?.id ?: continue

            val logsArray = catObj.optJSONArray("logs") ?: continue
            for (j in 0 until logsArray.length()) {
                val logObj = logsArray.getJSONObject(j)
                val dateStr = logObj.optString("date").takeIf { it.isNotBlank() } ?: continue
                val date = runCatching { java.time.LocalDate.parse(dateStr) }.getOrNull() ?: continue

                if (!replace && trackingRepository.getExistingLog(date, categoryId) != null) continue

                val valuesArray = logObj.optJSONArray("values")
                val values = buildSet<String> {
                    if (valuesArray != null) {
                        for (k in 0 until valuesArray.length()) add(valuesArray.getString(k))
                    }
                }
                trackingRepository.saveLog(date, categoryId, values, logObj.optString("notes", ""))
            }
        }
    }

    private suspend fun importCategoryConfig(categoriesArray: JSONArray, replace: Boolean) {
        for (i in 0 until categoriesArray.length()) {
            val catObj = categoriesArray.getJSONObject(i)
            val catName = catObj.optString("name").takeIf { it.isNotBlank() } ?: continue
            val isSystem = catObj.optBoolean("isSystem", false)
            val systemKey = catObj.optString("systemKey", "")

            // Match existing category by systemKey (for system cats) or name.
            var category = if (systemKey.isNotBlank())
                trackingRepository.getSystemCategoryByKey(systemKey)
            else null
            if (category == null) category = trackingRepository.getCategoryByName(catName)

            val categoryId: Long
            if (category == null) {
                categoryId = trackingRepository.addCategory(
                    name             = catName,
                    iconName         = catObj.optString("iconName", "category"),
                    colorToken       = catObj.optString("colorToken", "secondary"),
                    categoryType     = catObj.optString("categoryType", "default"),
                    numericMin       = catObj.optDouble("numericMin", 0.0).toFloat(),
                    numericMax       = catObj.optDouble("numericMax", 10.0).toFloat(),
                    allowDecimals    = catObj.optBoolean("allowDecimals", false),
                    numericUnit      = catObj.optString("numericUnit", ""),
                    allowMultiple    = catObj.optBoolean("allowMultiple", false),
                    showInLogPeriod  = catObj.optBoolean("showInLogPeriod", false),
                    trackAgainstTime = catObj.optBoolean("trackAgainstTime", false),
                )
                if (catObj.optBoolean("isArchived", false)) trackingRepository.archiveCategory(categoryId)
            } else {
                categoryId = category.id
                // Update appearance and settings on existing category.
                trackingRepository.updateCategoryFullSettings(
                    id               = categoryId,
                    name             = catName,
                    iconName         = catObj.optString("iconName", category.iconName),
                    colorToken       = catObj.optString("colorToken", category.colorToken),
                    categoryType     = catObj.optString("categoryType", category.categoryType),
                    numericMin       = catObj.optDouble("numericMin", category.numericMin.toDouble()).toFloat(),
                    numericMax       = catObj.optDouble("numericMax", category.numericMax.toDouble()).toFloat(),
                    allowDecimals    = catObj.optBoolean("allowDecimals", category.allowDecimals),
                    numericUnit      = catObj.optString("numericUnit", category.numericUnit),
                    allowMultiple    = catObj.optBoolean("allowMultiple", category.allowMultiple),
                    showInLogPeriod  = catObj.optBoolean("showInLogPeriod", category.showInLogPeriod),
                    trackAgainstTime = catObj.optBoolean("trackAgainstTime", category.trackAgainstTime),
                )
                if (catObj.optBoolean("isArchived", false) && !category.isArchived)
                    trackingRepository.archiveCategory(categoryId)
                else if (!catObj.optBoolean("isArchived", false) && category.isArchived)
                    trackingRepository.unarchiveCategory(categoryId)
            }

            // Restore values (labels/options) for this category.
            val valuesArray = catObj.optJSONArray("values") ?: continue
            val existingValues = trackingRepository.getValuesForCategoryOnce(categoryId)
            val existingLabels = existingValues.map { it.label }.toSet()
            for (j in 0 until valuesArray.length()) {
                val label = valuesArray.optString(j, "").takeIf { it.isNotBlank() } ?: continue
                if (label !in existingLabels) {
                    trackingRepository.addValueToCategory(categoryId, label)
                }
            }
        }
    }

    /**
     * Permanently deletes all stored data (periods, symptoms, and tracking logs),
     * then reschedules reminders (which will cancel predictive alarms now that
     * data is gone). Categories and their value definitions are preserved.
     *
     * NOTE: whenever new data tables are added, this method must be updated
     * to include them — and the same applies to importData and exportWithOptions.
     */
    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllData()
            trackingRepository.deleteAllLogs()
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

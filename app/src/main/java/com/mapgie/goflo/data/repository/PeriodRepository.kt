package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.dao.CustomSymptomDao
import com.mapgie.goflo.data.database.dao.PeriodDao
import com.mapgie.goflo.data.database.dao.SymptomDao
import com.mapgie.goflo.data.database.entities.CustomSymptomEntry
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
import com.mapgie.goflo.data.model.SymptomType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Result of a data import operation. */
sealed class ImportResult {
    data class Success(val imported: Int, val skipped: Int) : ImportResult()
    data class Failure(val message: String) : ImportResult()
}

class PeriodRepository(
    private val periodDao: PeriodDao,
    private val symptomDao: SymptomDao,
    private val customSymptomDao: CustomSymptomDao
) {
    fun getAllPeriods(): Flow<List<PeriodEntry>> = periodDao.getAllPeriods()

    /** One-shot read of all periods — used for the Flow data backfill migration. */
    suspend fun getAllPeriodsOnce(): List<PeriodEntry> = periodDao.getAllPeriodsOnce()

    fun getPeriodById(id: Long): Flow<PeriodEntry?> = periodDao.getPeriodById(id)

    fun getSymptomsForPeriod(periodId: Long): Flow<List<SymptomEntry>> =
        symptomDao.getSymptomsForPeriod(periodId)

    /** Reactive stream of every symptom row — used for symptom-trend analytics. */
    fun getAllSymptomsFlow(): Flow<List<SymptomEntry>> = symptomDao.getAllSymptomsFlow()

    /** One-shot read of all symptoms — used for export. */
    suspend fun getAllSymptomsOnce(): List<SymptomEntry> = symptomDao.getAllSymptoms()

    // ── Period write operations ───────────────────────────────────────────────

    suspend fun insertPeriod(
        entry: PeriodEntry,
        symptoms: List<SymptomType>,
        customSymptoms: List<String> = emptyList()
    ): Long {
        val id = periodDao.insertPeriod(entry)
        symptoms.forEach { symptomDao.insertSymptom(SymptomEntry(periodId = id, symptomType = it.name)) }
        customSymptoms.forEach { name ->
            symptomDao.insertSymptom(SymptomEntry(periodId = id, symptomType = name.lowercase()))
        }
        return id
    }

    suspend fun updatePeriod(
        entry: PeriodEntry,
        symptoms: List<SymptomType>,
        customSymptoms: List<String> = emptyList()
    ) {
        periodDao.updatePeriod(entry)
        symptomDao.deleteSymptomsByPeriodId(entry.id)
        symptoms.forEach { symptomDao.insertSymptom(SymptomEntry(periodId = entry.id, symptomType = it.name)) }
        customSymptoms.forEach { name ->
            symptomDao.insertSymptom(SymptomEntry(periodId = entry.id, symptomType = name.lowercase()))
        }
    }

    suspend fun deletePeriod(entry: PeriodEntry) {
        periodDao.deletePeriod(entry)
    }

    // ── Symptom read operations ───────────────────────────────────────────────

    /**
     * Returns symptoms for a period split into built-in ([SymptomType]) and custom (lowercase
     * [String]) sets.  Built-in symptoms are identified by a successful [SymptomType.valueOf]
     * lookup; anything else is treated as a custom symptom.
     */
    suspend fun getSymptomsParsed(periodId: Long): Pair<Set<SymptomType>, Set<String>> {
        val entries = symptomDao.getSymptomsForPeriodOnce(periodId)
        val builtIn = mutableSetOf<SymptomType>()
        val custom = mutableSetOf<String>()
        for (entry in entries) {
            val type = runCatching { SymptomType.valueOf(entry.symptomType) }.getOrNull()
            if (type != null) builtIn.add(type)
            else if (entry.symptomType.isNotBlank()) custom.add(entry.symptomType)
        }
        return builtIn to custom
    }

    // ── Custom symptom library operations ────────────────────────────────────

    /** Observe the full user-defined symptom library, ordered alphabetically. */
    fun getAllCustomSymptoms(): Flow<List<CustomSymptomEntry>> =
        customSymptomDao.getAllCustomSymptoms()

    /** Persist a new custom symptom (name is normalised to lowercase). */
    suspend fun addCustomSymptom(name: String) {
        customSymptomDao.insertCustomSymptom(CustomSymptomEntry(name = name.lowercase()))
    }

    // ── Cycle math ───────────────────────────────────────────────────────────

    /**
     * Permanently deletes all period and symptom records.
     * Symptoms are deleted first to satisfy the foreign-key relationship,
     * even though the schema uses CASCADE — belt-and-suspenders for clarity.
     */
    suspend fun deleteAllData() {
        symptomDao.deleteAllSymptoms()
        periodDao.deleteAllPeriods()
    }

    /**
     * Serialises all periods and their associated symptoms to a JSON string.
     *
     * Format:
     * [
     *   {
     *     "id": 1,
     *     "startDate": "2024-01-15",
     *     "endDate": "2024-01-19",        // null if ongoing
     *     "flowLevel": "MEDIUM",
     *     "notes": "...",
     *     "symptoms": ["CRAMPS", "FATIGUE"]
     *   },
     *   ...
     * ]
     */
    suspend fun exportData(): String {
        val periods = periodDao.getAllPeriods().first()
        val allSymptoms = symptomDao.getAllSymptoms()
        val symptomsByPeriod = allSymptoms.groupBy { it.periodId }

        val root = JSONArray()
        periods.sortedBy { it.startDate }.forEach { period ->
            val obj = JSONObject().apply {
                put("id", period.id)
                put("startDate", period.startDate)
                put("endDate", if (period.endDate != null) period.endDate else JSONObject.NULL)
                put("flowLevel", period.flowLevel)
                put("notes", period.notes)
                val symptoms = JSONArray()
                symptomsByPeriod[period.id]?.forEach { symptom ->
                    symptoms.put(symptom.symptomType)
                }
                put("symptoms", symptoms)
            }
            root.put(obj)
        }
        return root.toString(2) // pretty-printed with 2-space indent
    }

    /**
     * Exports all periods as a CSV string (RFC 4180).
     *
     * Columns: start_date, end_date, duration_days, flow_level, symptoms, notes
     * Symptoms are joined with ";" inside a quoted field.
     * Any double-quotes inside notes/symptoms are escaped as "".
     */
    suspend fun exportAsCsv(): String {
        val periods = periodDao.getAllPeriods().first().sortedBy { it.startDate }
        val allSymptoms = symptomDao.getAllSymptoms()
        val symptomsByPeriod = allSymptoms.groupBy { it.periodId }

        val sb = StringBuilder()
        sb.appendLine("start_date,end_date,duration_days,flow_level,symptoms,notes")
        periods.forEach { period ->
            val start = LocalDate.parse(period.startDate)
            val end   = period.endDate?.let { LocalDate.parse(it) }
            val duration = if (end != null) (ChronoUnit.DAYS.between(start, end) + 1).toString() else ""
            // Double-quote escaping (RFC 4180) then formula-injection sanitisation.
            val symptoms = sanitizeCsvField(
                symptomsByPeriod[period.id]
                    ?.joinToString(";") { it.symptomType }?.replace("\"", "\"\"")
                    ?: ""
            )
            val notes = sanitizeCsvField(period.notes.replace("\"", "\"\""))
            sb.appendLine("${period.startDate},${period.endDate ?: ""},${duration},${period.flowLevel},\"${symptoms}\",\"${notes}\"")
        }
        return sb.toString()
    }

    /**
     * Defends against CSV formula injection (a.k.a. DDE injection).
     *
     * Spreadsheet apps (Excel, LibreOffice, Google Sheets) interpret cells whose
     * first character is `=`, `+`, `-`, `@`, `\t`, or `\r` as formulas. A malicious
     * (or simply unusual) period note could otherwise trigger arbitrary formula
     * execution when a user opens the CSV export.
     *
     * Fix: prefix any such value with a tab character so the cell is treated as
     * plain text. The tab is invisible in most apps but prevents formula parsing.
     */
    private fun sanitizeCsvField(value: String): String {
        val dangerChars = setOf('=', '+', '-', '@', '\t', '\r')
        return if (value.isNotEmpty() && value[0] in dangerChars) "\t$value" else value
    }

    /**
     * Imports periods from a JSON string produced by [exportData].
     *
     * @param json   The raw JSON text from the export file.
     * @param replace If true, all existing data is deleted before importing.
     *                If false, periods whose [PeriodEntry.startDate] already exists
     *                in the database are skipped (safe to run on a non-empty device).
     *
     * Unknown symptom type strings are silently dropped so that old exports are
     * forward-compatible even if the symptom list grows or shrinks.
     */
    suspend fun importData(json: String, replace: Boolean): ImportResult {
        return try {
            // Support both v1 (bare array) and v2 (wrapper object with "periods" key).
            val array = when {
                json.trimStart().startsWith('[') -> JSONArray(json)
                else -> JSONObject(json).optJSONArray("periods") ?: JSONArray()
            }

            if (replace) deleteAllData()

            val existingStartDates: Set<String> = if (replace) {
                emptySet()
            } else {
                periodDao.getAllPeriods().first().map { it.startDate }.toSet()
            }

            var imported = 0
            var skipped = 0

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val startDate = obj.getString("startDate")

                if (startDate in existingStartDates) {
                    skipped++
                    continue
                }

                val entry = PeriodEntry(
                    // id intentionally omitted — let Room auto-generate a fresh one
                    startDate = startDate,
                    endDate = if (obj.isNull("endDate")) null else obj.optString("endDate"),
                    flowLevel = obj.optString("flowLevel", "MEDIUM"),
                    notes = obj.optString("notes", "")
                )
                val newId = periodDao.insertPeriod(entry)

                val symptomsArray = obj.optJSONArray("symptoms")
                if (symptomsArray != null) {
                    for (j in 0 until symptomsArray.length()) {
                        val typeName = symptomsArray.getString(j)
                        // Ignore unrecognised symptom strings — keeps old exports importable
                        runCatching { SymptomType.valueOf(typeName) }.onSuccess {
                            symptomDao.insertSymptom(SymptomEntry(periodId = newId, symptomType = typeName))
                        }
                    }
                }

                imported++
            }

            ImportResult.Success(imported, skipped)
        } catch (e: Exception) {
            ImportResult.Failure(e.message ?: "Could not parse import file")
        }
    }

    companion object {
        fun calculateAvgCycleLength(periods: List<PeriodEntry>): Int {
            if (periods.size < 2) return 28
            val sorted = periods.sortedBy { it.startDate }
            val gaps = sorted.zipWithNext { a, b ->
                ChronoUnit.DAYS.between(
                    LocalDate.parse(a.startDate),
                    LocalDate.parse(b.startDate)
                ).toInt()
            }.filter { it in 15..60 }
            return if (gaps.isEmpty()) 28 else (gaps.sum() / gaps.size).coerceIn(21, 35)
        }

        fun predictNextStart(periods: List<PeriodEntry>, avgCycle: Int): LocalDate? {
            val last = periods.maxByOrNull { it.startDate } ?: return null
            return LocalDate.parse(last.startDate).plusDays(avgCycle.toLong())
        }

        fun ovulationDate(periods: List<PeriodEntry>, avgCycle: Int): LocalDate? {
            val last = periods.maxByOrNull { it.startDate } ?: return null
            return LocalDate.parse(last.startDate).plusDays((avgCycle / 2).toLong())
        }

        fun activePeriod(periods: List<PeriodEntry>): PeriodEntry? =
            periods.firstOrNull { it.endDate == null }

        fun cycleDay(periods: List<PeriodEntry>): Int? {
            val last = periods.maxByOrNull { it.startDate } ?: return null
            val start = LocalDate.parse(last.startDate)
            val today = LocalDate.now()
            val day = ChronoUnit.DAYS.between(start, today).toInt() + 1
            return if (day >= 1) day else null
        }
    }
}

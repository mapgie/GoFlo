package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.dao.PeriodDao
import com.mapgie.goflo.data.database.dao.SymptomDao
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
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
        symptoms: List<String>,
    ): Long {
        val id = periodDao.insertPeriod(entry)
        symptoms.filter { it.isNotBlank() }.forEach { label ->
            symptomDao.insertSymptom(SymptomEntry(periodId = id, symptomType = label))
        }
        return id
    }

    suspend fun updatePeriod(
        entry: PeriodEntry,
        symptoms: List<String>,
    ) {
        periodDao.updatePeriod(entry)
        symptomDao.deleteSymptomsByPeriodId(entry.id)
        symptoms.filter { it.isNotBlank() }.forEach { label ->
            symptomDao.insertSymptom(SymptomEntry(periodId = entry.id, symptomType = label))
        }
    }

    suspend fun deletePeriod(entry: PeriodEntry) {
        periodDao.deletePeriod(entry)
    }

    /** Updates only the flowLevel field of a period without touching its symptoms. */
    suspend fun updateFlowLevel(period: PeriodEntry, flowLevel: String) {
        periodDao.updatePeriod(period.copy(flowLevel = flowLevel))
    }

    /**
     * One-time data fixup: merges period entries whose date ranges overlap into a
     * single entry. An ongoing period (endDate == null) is treated as extending
     * through today when checking for overlap.
     *
     * Periods are processed in start-date order. When a period's start date falls
     * within the running merged range, it is folded into that range (notes are
     * concatenated, symptoms moved over, and the duplicate entry deleted).
     *
     * @return the number of duplicate entries removed.
     */
    suspend fun mergeOverlappingPeriods(): Int {
        val periods = periodDao.getAllPeriodsOnce()
        if (periods.size < 2) return 0

        var merged = 0
        var current = periods.first()
        var currentEnd = current.endDate?.let { LocalDate.parse(it) }

        for (next in periods.drop(1)) {
            val currentEffectiveEnd = currentEnd ?: LocalDate.now()
            val nextStart = LocalDate.parse(next.startDate)

            if (!nextStart.isAfter(currentEffectiveEnd)) {
                val nextEnd = next.endDate?.let { LocalDate.parse(it) }
                currentEnd = when {
                    currentEnd == null || nextEnd == null -> null
                    nextEnd.isAfter(currentEnd) -> nextEnd
                    else -> currentEnd
                }

                val currentSymptoms = symptomDao.getSymptomsForPeriodOnce(current.id).map { it.symptomType }.toSet()
                symptomDao.getSymptomsForPeriodOnce(next.id)
                    .map { it.symptomType }
                    .filter { it.isNotBlank() && it !in currentSymptoms }
                    .forEach { symptomDao.insertSymptom(SymptomEntry(periodId = current.id, symptomType = it)) }

                current = current.copy(endDate = currentEnd?.toString(), notes = mergeNotes(current.notes, next.notes))
                periodDao.updatePeriod(current)
                periodDao.deletePeriod(next)
                merged++
            } else {
                current = next
                currentEnd = next.endDate?.let { LocalDate.parse(it) }
            }
        }

        return merged
    }

    /**
     * Merges [a] and [b] into a single period entry spanning both, combining their
     * notes and symptoms. Used for the manual "Merge with…" action in History, so
     * a user can join two entries that were split apart (e.g. by a logging bug, or
     * by an accidental extra tap) back into one continuous period.
     *
     * The earlier period (by start date) is kept and updated in place; the later
     * period is deleted. The merged result is ongoing only if the later period was
     * ongoing — an ongoing period is always the most recent one, so an earlier
     * period can never legitimately outlast it.
     *
     * @return the surviving, merged period entry.
     */
    suspend fun mergePeriods(a: PeriodEntry, b: PeriodEntry): PeriodEntry {
        val (earlier, later) = if (a.startDate <= b.startDate) a to b else b to a

        val laterEnd = later.endDate?.let { LocalDate.parse(it) }
        val earlierEnd = earlier.endDate?.let { LocalDate.parse(it) }
        val mergedEnd = when {
            later.endDate == null -> null
            earlierEnd != null && earlierEnd.isAfter(laterEnd) -> earlierEnd
            else -> laterEnd
        }

        val earlierSymptoms = symptomDao.getSymptomsForPeriodOnce(earlier.id).map { it.symptomType }.toSet()
        symptomDao.getSymptomsForPeriodOnce(later.id)
            .map { it.symptomType }
            .filter { it.isNotBlank() && it !in earlierSymptoms }
            .forEach { symptomDao.insertSymptom(SymptomEntry(periodId = earlier.id, symptomType = it)) }

        val merged = earlier.copy(endDate = mergedEnd?.toString(), notes = mergeNotes(earlier.notes, later.notes))
        periodDao.updatePeriod(merged)
        periodDao.deletePeriod(later)
        return merged
    }

    private fun mergeNotes(a: String, b: String): String = when {
        b.isBlank() -> a
        a.isBlank() -> b
        a.contains(b) -> a
        else -> "$a\n$b"
    }

    // ── Symptom read operations ───────────────────────────────────────────────

    /** Returns all symptom labels for a period as a flat set of strings. */
    suspend fun getSymptomsParsed(periodId: Long): Set<String> {
        return symptomDao.getSymptomsForPeriodOnce(periodId)
            .map { it.symptomType }
            .filter { it.isNotBlank() }
            .toSet()
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
     *     "flowLevel": "Medium",
     *     "notes": "...",
     *     "symptoms": ["Cramps", "Fatigue"]
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
     * All non-blank symptom strings are imported as-is, supporting both old enum-name
     * exports ("CRAMPS") and current label exports ("Cramps").
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

                val rawFlow = obj.optString("flowLevel", "Medium")
                // Support old enum-name exports: map "MEDIUM" → "Medium" etc.
                val flowLevel = FLOW_ENUM_TO_LABEL[rawFlow] ?: rawFlow

                val entry = PeriodEntry(
                    // id intentionally omitted — let Room auto-generate a fresh one
                    startDate = startDate,
                    endDate = if (obj.isNull("endDate")) null else obj.optString("endDate"),
                    flowLevel = flowLevel,
                    notes = obj.optString("notes", "")
                )
                val newId = periodDao.insertPeriod(entry)

                val symptomsArray = obj.optJSONArray("symptoms")
                if (symptomsArray != null) {
                    for (j in 0 until symptomsArray.length()) {
                        val label = symptomsArray.getString(j).trim()
                        if (label.isBlank()) continue
                        // Support old enum-name exports: map "CRAMPS" → "Cramps" etc.
                        val normalized = SYMPTOM_ENUM_TO_LABEL[label] ?: label
                        symptomDao.insertSymptom(SymptomEntry(periodId = newId, symptomType = normalized))
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
        /** Maps legacy enum-name flow levels (pre-v0.23) to their display labels. */
        internal val FLOW_ENUM_TO_LABEL = mapOf(
            "SPOTTING" to "Spotting",
            "LIGHT"    to "Light",
            "MEDIUM"   to "Medium",
            "HEAVY"    to "Heavy",
        )

        /** Maps legacy enum-name symptom types (pre-v0.23) to their display labels. */
        private val SYMPTOM_ENUM_TO_LABEL = mapOf(
            "CRAMPS"      to "Cramps",
            "HEADACHE"    to "Headache",
            "BLOATING"    to "Bloating",
            "FATIGUE"     to "Fatigue",
            "BACK_PAIN"   to "Back Pain",
            "MOOD_SWINGS" to "Mood Swings",
        )

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

        /**
         * Returns the period that logging [date] should be treated as part of, if any.
         *
         * An ongoing period (endDate == null) extends indefinitely into the future —
         * any date on or after its start is covered, not just "up to today". Using
         * today's wall-clock date as the cutoff meant an ongoing period silently
         * stopped covering new days once the app was reopened on a later date,
         * causing the next tap to create a duplicate period instead of continuing it.
         *
         * A period with an explicit end date also covers the day immediately after
         * that end date, so logging the very next day continues the period instead
         * of starting a new, disconnected one (the single most common way period
         * entries get fragmented: closing a period each day with an explicit end
         * date rather than leaving it ongoing).
         */
        fun periodForDate(periods: List<PeriodEntry>, date: LocalDate): PeriodEntry? =
            periods.firstOrNull { entry ->
                val start = LocalDate.parse(entry.startDate)
                if (date.isBefore(start)) return@firstOrNull false
                val end = entry.endDate?.let { LocalDate.parse(it) } ?: return@firstOrNull true
                !date.isAfter(end) || date == end.plusDays(1)
            }

        fun cycleDay(periods: List<PeriodEntry>): Int? {
            val last = periods.maxByOrNull { it.startDate } ?: return null
            val start = LocalDate.parse(last.startDate)
            val today = LocalDate.now()
            val day = ChronoUnit.DAYS.between(start, today).toInt() + 1
            return if (day >= 1) day else null
        }

        fun cyclePhaseLabel(cycleDay: Int, avgCycleLength: Int): String {
            val ovulationDay = avgCycleLength / 2
            return when {
                cycleDay <= 5               -> "Menstrual"
                cycleDay < ovulationDay - 2 -> "Follicular"
                cycleDay <= ovulationDay + 2 -> "Ovulatory"
                else                        -> "Luteal"
            }
        }
    }
}

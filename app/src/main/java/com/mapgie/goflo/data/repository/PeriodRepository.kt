package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.dao.PeriodDao
import com.mapgie.goflo.data.database.dao.PeriodDayDao
import com.mapgie.goflo.data.database.dao.SymptomDao
import com.mapgie.goflo.data.database.entities.PeriodDayEntry
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

/**
 * Repository for period data, built around per-day logging.
 *
 * The source of truth is the `period_days` table: one row per day the user
 * marked as a period day. Period *episodes* (rows in the `periods` table,
 * exposed as [PeriodEntry]) are derived from those days by [rebuildEpisodes]:
 * days that are consecutive, or separated by no more than the user's gap
 * tolerance, belong to the same episode. Day-specific data (flow, symptoms,
 * other categories) lives in the tracking-log system keyed by date, not here.
 *
 * Episode `endDate` semantics:
 * - `null` — ongoing: the last logged day is still within the tolerance
 *   window of today, so the next logged day would continue this episode.
 * - a date — ended: either set explicitly by the user ("until" date, which
 *   may extend past the last individually logged day), or stamped
 *   automatically by [reconcile] once the tolerance window has passed
 *   without further logging.
 *
 * The derived `periods` table is kept persistent (rather than computed on
 * read) so every existing consumer — stats, widgets, reminders, export —
 * keeps reading episodes exactly as before.
 */
class PeriodRepository(
    private val periodDao: PeriodDao,
    private val symptomDao: SymptomDao,
    private val periodDayDao: PeriodDayDao,
) {
    fun getAllPeriods(): Flow<List<PeriodEntry>> = periodDao.getAllPeriods()

    /** One-shot read of all periods — used for the Flow data backfill migration. */
    suspend fun getAllPeriodsOnce(): List<PeriodEntry> = periodDao.getAllPeriodsOnce()

    fun getPeriodById(id: Long): Flow<PeriodEntry?> = periodDao.getPeriodById(id)

    /** Reactive stream of every logged period day. */
    fun getAllPeriodDays(): Flow<List<PeriodDayEntry>> = periodDayDao.getAllDays()

    /** One-shot read of all logged period days, ascending by date. */
    suspend fun getAllPeriodDaysOnce(): List<PeriodDayEntry> = periodDayDao.getAllDaysOnce()

    /** True if [date] is an individually logged period day. */
    suspend fun isPeriodDay(date: LocalDate): Boolean = periodDayDao.getDay(date.toString()) != null

    /** Reactive stream of every symptom row — used for symptom-trend analytics. */
    fun getAllSymptomsFlow(): Flow<List<SymptomEntry>> = symptomDao.getAllSymptomsFlow()

    /** One-shot read of all symptoms — used for export. */
    suspend fun getAllSymptomsOnce(): List<SymptomEntry> = symptomDao.getAllSymptoms()

    // ── Period day write operations ───────────────────────────────────────────

    /**
     * Marks [date] as a period day and re-derives episodes.
     *
     * Starting a period, continuing one, retro-logging a missed day, and
     * bridging the gap between two fragmented episodes are all this one
     * operation — the episode grouping absorbs the day into whatever shape
     * the data now has.
     *
     * @return the episode that now contains [date].
     */
    suspend fun logPeriodDay(
        date: LocalDate,
        toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
        today: LocalDate = LocalDate.now(),
    ): PeriodEntry? {
        periodDayDao.insertDay(PeriodDayEntry(date = date.toString()))
        rebuildEpisodes(toleranceDays, today)
        return episodeContaining(date, toleranceDays)
    }

    /**
     * Marks every day in [start]..[end] as period days in one step — the
     * retrospective "I had my period from X to Y" entry. When [end] is null
     * the period is ongoing: days are materialised from [start] through
     * [today]. When [end] is given it is also recorded as the episode's
     * explicit end date.
     */
    suspend fun logPeriodRange(
        start: LocalDate,
        end: LocalDate?,
        toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
        today: LocalDate = LocalDate.now(),
    ): PeriodEntry? {
        val effectiveEnd = end ?: maxOf(start, minOf(today, start.plusDays(MAX_MATERIALIZED_ONGOING_DAYS)))
        var day = start
        while (!day.isAfter(effectiveEnd)) {
            periodDayDao.insertDay(PeriodDayEntry(date = day.toString()))
            day = day.plusDays(1)
        }
        rebuildEpisodes(toleranceDays, today)
        val episode = episodeContaining(start, toleranceDays) ?: return null
        // An explicit end is authoritative even while the tolerance window is
        // still open (rebuild alone would leave a just-finished period "ongoing").
        return if (end != null && episode.endDate != end.toString()) {
            val closed = episode.copy(endDate = end.toString())
            periodDao.updatePeriod(closed)
            closed
        } else {
            episode
        }
    }

    /**
     * Removes [date] from the logged period days and re-derives episodes.
     * Depending on where the day sat this shrinks, splits, or deletes an
     * episode.
     */
    suspend fun unlogPeriodDay(
        date: LocalDate,
        toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
        today: LocalDate = LocalDate.now(),
    ) {
        periodDayDao.deleteDay(date.toString())
        rebuildEpisodes(toleranceDays, today)
    }

    /**
     * Applies an episode-boundary edit from the log screen: a new start date,
     * a new explicit end date (or null to leave the period open), and new
     * notes for the episode identified by [id].
     *
     * Moving the start later or the end earlier removes the day rows that
     * fall outside the new range; moving the start earlier materialises the
     * newly claimed days. An explicit [end] may extend past the last
     * individually logged day ("it lasted until the 9th, I just stopped
     * logging") — no day rows are fabricated for that tail.
     *
     * @return the updated episode, or null if it no longer exists.
     */
    suspend fun updateEpisode(
        id: Long,
        start: LocalDate,
        end: LocalDate?,
        notes: String,
        toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
        today: LocalDate = LocalDate.now(),
    ): PeriodEntry? {
        // A day logged just before this call can have bridged two episodes and
        // absorbed [id]'s row — fall back to whichever episode now covers the
        // requested start so the boundary/notes edit still lands.
        val existing = periodDao.getAllPeriodsOnce().firstOrNull { it.id == id }
            ?: episodeContaining(start, toleranceDays)
            ?: return null
        val oldStart = LocalDate.parse(existing.startDate)
        val lastDay = lastDayOfEpisodeAt(oldStart, toleranceDays) ?: oldStart

        // Trim days outside the new range.
        if (start.isAfter(oldStart)) {
            periodDayDao.deleteDaysInRange(oldStart.toString(), start.minusDays(1).toString())
        }
        if (end != null && end.isBefore(lastDay)) {
            periodDayDao.deleteDaysInRange(end.plusDays(1).toString(), lastDay.toString())
        }
        // Materialise days the new, earlier start claims.
        if (start.isBefore(oldStart)) {
            var day = start
            while (day.isBefore(oldStart)) {
                periodDayDao.insertDay(PeriodDayEntry(date = day.toString()))
                day = day.plusDays(1)
            }
        }
        // Make sure the start day itself is logged.
        periodDayDao.insertDay(PeriodDayEntry(date = start.toString()))

        periodDao.updatePeriod(existing.copy(notes = notes))
        rebuildEpisodes(toleranceDays, today)

        val episode = episodeContaining(start, toleranceDays) ?: return null
        return if (end != null && episode.endDate != end.toString()) {
            val closed = episode.copy(endDate = end.toString())
            periodDao.updatePeriod(closed)
            closed
        } else {
            episode
        }
    }

    /**
     * The ISO dates of the logged days belonging to [entry]'s episode: from
     * its start to its explicit end, or to the end of its day chain when the
     * episode is still open.
     */
    suspend fun getDaysForEpisode(
        entry: PeriodEntry,
        toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
    ): List<String> {
        val start = LocalDate.parse(entry.startDate)
        val last = entry.endDate?.let { LocalDate.parse(it) }
            ?: lastDayOfEpisodeAt(start, toleranceDays)
            ?: start
        return periodDayDao.getDaysInRange(start.toString(), last.toString()).map { it.date }
    }

    /**
     * Deletes an entire episode: its row, its day rows, and its legacy
     * symptom rows.
     *
     * @return the ISO dates of the day rows that were removed, so callers can
     * clean up per-day tracking logs and stash them for undo.
     */
    suspend fun deletePeriod(
        entry: PeriodEntry,
        toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
    ): List<String> {
        val removed = getDaysForEpisode(entry, toleranceDays)
        removed.forEach { periodDayDao.deleteDay(it) }
        symptomDao.deleteSymptomsByPeriodId(entry.id)
        periodDao.deletePeriod(entry)
        return removed
    }

    /**
     * Restores a previously deleted episode (the History undo path): the
     * episode row, its logged days, and its legacy symptom rows.
     */
    suspend fun restorePeriod(
        entry: PeriodEntry,
        symptoms: List<String>,
        days: List<String>,
        toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
        today: LocalDate = LocalDate.now(),
    ) {
        val id = periodDao.insertPeriod(entry)
        symptoms.filter { it.isNotBlank() }.forEach { label ->
            symptomDao.insertSymptom(SymptomEntry(periodId = id, symptomType = label))
        }
        days.forEach { periodDayDao.insertDay(PeriodDayEntry(date = it)) }
        rebuildEpisodes(toleranceDays, today)
    }

    /**
     * Merges [a] and [b] into one continuous episode by marking the gap days
     * between them as period days. This is the manual "Merge with…" action in
     * History; it is explicit user intent that the whole span was one period,
     * so materialising the unlogged days in between is the honest
     * representation — anything less would immediately re-split on the next
     * rebuild.
     *
     * @return the surviving, merged episode.
     */
    suspend fun mergePeriods(
        a: PeriodEntry,
        b: PeriodEntry,
        toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
        today: LocalDate = LocalDate.now(),
    ): PeriodEntry? {
        val (earlier, later) = if (a.startDate <= b.startDate) a to b else b to a
        val fillFrom = LocalDate.parse(earlier.startDate)
        val fillTo = LocalDate.parse(later.startDate)
        var day = fillFrom
        while (!day.isAfter(fillTo)) {
            // Only the true gap needs new rows; INSERT OR IGNORE makes
            // re-inserting already-logged days a no-op.
            periodDayDao.insertDay(PeriodDayEntry(date = day.toString()))
            day = day.plusDays(1)
        }
        rebuildEpisodes(toleranceDays, today)
        return episodeContaining(fillFrom, toleranceDays)
    }

    /** Updates only the flowLevel field of an episode (legacy display mirror). */
    suspend fun updateFlowLevel(period: PeriodEntry, flowLevel: String) {
        periodDao.updatePeriod(period.copy(flowLevel = flowLevel))
    }

    /**
     * Updates an episode's display fields — notes and the flowLevel legacy
     * mirror — against its freshest row, so a rebuild that just ran cannot be
     * clobbered by a stale copy.
     */
    suspend fun updateEpisodeMeta(id: Long, notes: String, flowLevel: String) {
        val row = periodDao.getAllPeriodsOnce().firstOrNull { it.id == id } ?: return
        val newFlow = if (flowLevel.isBlank()) row.flowLevel else flowLevel
        periodDao.updatePeriod(row.copy(notes = notes, flowLevel = newFlow))
    }

    // ── Episode derivation ────────────────────────────────────────────────────

    /**
     * Re-derives the `periods` table from the logged period days, then closes
     * any episode whose tolerance window has lapsed. Called on app start and
     * by the daily check worker, and after every day mutation.
     *
     * Auto-end rule: an episode stays ongoing (endDate = null) while
     * `today - lastLoggedDay <= toleranceDays + 1`, i.e. while a day logged
     * today would still connect to it. One day beyond that, the episode is
     * closed at its last logged day.
     */
    suspend fun reconcile(
        toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
        today: LocalDate = LocalDate.now(),
    ) {
        rebuildEpisodes(toleranceDays, today)
    }

    /**
     * Groups all period days into episodes (max allowed gap between
     * neighbouring days = toleranceDays + 1) and syncs the `periods` table to
     * match: episode ids and notes are preserved through overlap matching,
     * episodes bridged by new days are merged, episodes whose days were
     * removed are split or deleted, and valid explicit end dates are kept.
     */
    private suspend fun rebuildEpisodes(toleranceDays: Int, today: LocalDate) {
        val maxGap = toleranceDays + 1L
        val days = periodDayDao.getAllDaysOnce().map { LocalDate.parse(it.date) }
        val existing = periodDao.getAllPeriodsOnce()

        val groups = mutableListOf<MutableList<LocalDate>>()
        for (day in days) {
            val current = groups.lastOrNull()
            if (current != null && ChronoUnit.DAYS.between(current.last(), day) <= maxGap) {
                current.add(day)
            } else {
                groups.add(mutableListOf(day))
            }
        }

        val consumed = mutableSetOf<Long>()
        groups.forEachIndexed { index, group ->
            val first = group.first()
            val last = group.last()
            val nextGroupFirst = groups.getOrNull(index + 1)?.first()

            val overlapping = existing.filter { row ->
                if (row.id in consumed) return@filter false
                val rowStart = LocalDate.parse(row.startDate)
                val rowEnd = row.endDate?.let { LocalDate.parse(it) } ?: rowStart
                !rowStart.isAfter(last) && !maxOf(rowStart, rowEnd).isBefore(first)
            }
            consumed += overlapping.map { it.id }

            // An explicit "until" end survives only if it still makes sense:
            // it must not fall before the group's last logged day, and it must
            // not reach into the next episode.
            val explicitEnd = overlapping
                .mapNotNull { it.endDate?.let { e -> LocalDate.parse(e) } }
                .filter { !it.isBefore(last) && (nextGroupFirst == null || it.isBefore(nextGroupFirst)) }
                .maxOrNull()

            val resolvedEnd: LocalDate? = when {
                explicitEnd != null -> explicitEnd
                ChronoUnit.DAYS.between(last, today) > maxGap -> last
                else -> null
            }

            val survivor = overlapping.firstOrNull()
            if (survivor != null) {
                val mergedNotes = overlapping.map { it.notes }.reduce(::mergeNotes)
                periodDao.updatePeriod(
                    survivor.copy(
                        startDate = first.toString(),
                        endDate = resolvedEnd?.toString(),
                        notes = mergedNotes,
                    )
                )
                overlapping.drop(1).forEach { absorbed ->
                    // Re-key the absorbed episode's legacy symptom rows so they
                    // are not orphaned, then drop the row itself.
                    val survivorSymptoms =
                        symptomDao.getSymptomsForPeriodOnce(survivor.id).map { it.symptomType }.toSet()
                    symptomDao.getSymptomsForPeriodOnce(absorbed.id)
                        .map { it.symptomType }
                        .filter { it.isNotBlank() && it !in survivorSymptoms }
                        .forEach {
                            symptomDao.insertSymptom(SymptomEntry(periodId = survivor.id, symptomType = it))
                        }
                    symptomDao.deleteSymptomsByPeriodId(absorbed.id)
                    periodDao.deletePeriod(absorbed)
                }
            } else {
                periodDao.insertPeriod(
                    PeriodEntry(
                        startDate = first.toString(),
                        endDate = resolvedEnd?.toString(),
                        flowLevel = "",
                    )
                )
            }
        }

        // Episodes left with no days at all no longer exist.
        existing.filter { it.id !in consumed }.forEach { orphan ->
            symptomDao.deleteSymptomsByPeriodId(orphan.id)
            periodDao.deletePeriod(orphan)
        }
    }

    /** Returns the episode whose span (with tolerance) covers [date], if any. */
    private suspend fun episodeContaining(date: LocalDate, toleranceDays: Int): PeriodEntry? =
        periodForDate(periodDao.getAllPeriodsOnce(), date, toleranceDays)

    /**
     * Returns the last day of the chain of logged period days that starts at
     * (or contains) [from], following the gap-tolerance rule, or null when
     * [from] is not near any logged day.
     */
    private suspend fun lastDayOfEpisodeAt(from: LocalDate, toleranceDays: Int): LocalDate? {
        val maxGap = toleranceDays + 1L
        val days = periodDayDao.getAllDaysOnce().map { LocalDate.parse(it.date) }
        var last: LocalDate? = null
        for (day in days) {
            if (day.isBefore(from)) continue
            val previous = last
            if (previous == null) {
                if (ChronoUnit.DAYS.between(from, day) > maxGap) break
            } else {
                if (ChronoUnit.DAYS.between(previous, day) > maxGap) break
            }
            last = day
        }
        return last
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

    // ── Bulk operations ──────────────────────────────────────────────────────

    /**
     * Permanently deletes all period, period-day, and symptom records.
     * Symptoms are deleted first to satisfy the foreign-key relationship,
     * even though the schema uses CASCADE — belt-and-suspenders for clarity.
     */
    suspend fun deleteAllData() {
        symptomDao.deleteAllSymptoms()
        periodDao.deleteAllPeriods()
        periodDayDao.deleteAllDays()
    }

    /**
     * Serialises all periods and their associated data to a JSON string.
     *
     * Format (v3 — adds "days"):
     * [
     *   {
     *     "id": 1,
     *     "startDate": "2024-01-15",
     *     "endDate": "2024-01-19",        // null if ongoing
     *     "flowLevel": "Medium",          // legacy episode-level mirror
     *     "notes": "...",
     *     "symptoms": ["Cramps", "Fatigue"],
     *     "days": ["2024-01-15", "2024-01-16", ...]   // individually logged days
     *   },
     *   ...
     * ]
     */
    suspend fun exportData(): String {
        val periods = periodDao.getAllPeriods().first()
        val allSymptoms = symptomDao.getAllSymptoms()
        val symptomsByPeriod = allSymptoms.groupBy { it.periodId }
        val allDays = periodDayDao.getAllDaysOnce().map { it.date }

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
                val days = JSONArray()
                daysForPeriod(period, allDays).forEach { days.put(it) }
                put("days", days)
            }
            root.put(obj)
        }
        return root.toString(2) // pretty-printed with 2-space indent
    }

    /**
     * Exports all periods as a CSV string (RFC 4180).
     *
     * Columns: start_date, end_date, duration_days, flow_level, symptoms, notes, logged_days
     * Symptoms and logged days are joined with ";" inside a quoted field.
     * Any double-quotes inside notes/symptoms are escaped as "".
     */
    suspend fun exportAsCsv(): String {
        val periods = periodDao.getAllPeriods().first().sortedBy { it.startDate }
        val allSymptoms = symptomDao.getAllSymptoms()
        val symptomsByPeriod = allSymptoms.groupBy { it.periodId }
        val allDays = periodDayDao.getAllDaysOnce().map { it.date }

        val sb = StringBuilder()
        sb.appendLine("start_date,end_date,duration_days,flow_level,symptoms,notes,logged_days")
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
            val loggedDays = daysForPeriod(period, allDays).joinToString(";")
            sb.appendLine("${period.startDate},${period.endDate ?: ""},${duration},${period.flowLevel},\"${symptoms}\",\"${notes}\",\"${loggedDays}\"")
        }
        return sb.toString()
    }

    /** The logged days that fall inside [period]'s span, ascending. */
    private fun daysForPeriod(period: PeriodEntry, allDaysSorted: List<String>): List<String> {
        val end = period.endDate
        return allDaysSorted.filter { day ->
            day >= period.startDate && (end == null || day <= end)
        }
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
     * Exports that carry a "days" array (v3+) restore the individually logged
     * days exactly; older exports have their episode range expanded into day
     * rows so the data participates in per-day episode derivation.
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

                val daysArray = obj.optJSONArray("days")
                if (daysArray != null && daysArray.length() > 0) {
                    for (j in 0 until daysArray.length()) {
                        periodDayDao.insertDay(PeriodDayEntry(date = daysArray.getString(j)))
                    }
                } else {
                    materializeDaysForImportedRange(entry)
                }

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

    /**
     * Expands a legacy imported episode (no per-day data) into day rows —
     * the same rule the v23 database migration applies: full range for ended
     * periods, capped at [MAX_MATERIALIZED_ONGOING_DAYS] for ongoing ones.
     */
    private suspend fun materializeDaysForImportedRange(entry: PeriodEntry) {
        val start = LocalDate.parse(entry.startDate)
        val end = entry.endDate?.let { LocalDate.parse(it) }
            ?: maxOf(start, minOf(LocalDate.now(), start.plusDays(MAX_MATERIALIZED_ONGOING_DAYS)))
        var day = start
        while (!day.isAfter(end)) {
            periodDayDao.insertDay(PeriodDayEntry(date = day.toString()))
            day = day.plusDays(1)
        }
    }

    companion object {
        /**
         * Default number of unlogged days allowed between two period days that
         * still count as the same period. Also drives auto-end: one day beyond
         * this window without logging closes the episode.
         */
        const val DEFAULT_GAP_TOLERANCE_DAYS = 1

        /**
         * When an ongoing episode must be expanded into day rows without
         * per-day information (legacy import), cap the expansion so a period
         * accidentally left open long ago does not fabricate months of days.
         * 9 extra days = a 10-day period, the upper bound of typical duration.
         */
        const val MAX_MATERIALIZED_ONGOING_DAYS = 9L

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
         * A date within [toleranceDays] + 1 of an episode's boundary (on
         * either side) still belongs to that episode: logging it continues
         * the period rather than starting a new, disconnected one. An ongoing
         * period (endDate == null) additionally covers every future date —
         * under per-day derivation ongoing episodes are always recent (they
         * auto-close once the tolerance window lapses), so the next date the
         * user logs continues them.
         */
        fun periodForDate(
            periods: List<PeriodEntry>,
            date: LocalDate,
            toleranceDays: Int = DEFAULT_GAP_TOLERANCE_DAYS,
        ): PeriodEntry? {
            val reach = toleranceDays + 1L
            return periods.firstOrNull { entry ->
                val start = LocalDate.parse(entry.startDate)
                if (date.isBefore(start.minusDays(reach))) return@firstOrNull false
                val end = entry.endDate?.let { LocalDate.parse(it) } ?: return@firstOrNull true
                !date.isAfter(end.plusDays(reach))
            }
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

package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.dao.PeriodDao
import com.mapgie.goflo.data.database.dao.PeriodDayDao
import com.mapgie.goflo.data.database.dao.SymptomDao
import com.mapgie.goflo.data.database.entities.PeriodDayEntry
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** In-memory [PeriodDao] fake for exercising [PeriodRepository]'s suspend write paths. */
private class FakePeriodDao : PeriodDao {
    val periods = mutableListOf<PeriodEntry>()
    private var nextId = 1L

    override fun getAllPeriods(): Flow<List<PeriodEntry>> = flowOf(periods.sortedByDescending { it.startDate })
    override suspend fun getAllPeriodsOnce(): List<PeriodEntry> = periods.sortedBy { it.startDate }
    override fun getPeriodById(id: Long): Flow<PeriodEntry?> = flowOf(periods.firstOrNull { it.id == id })

    override suspend fun insertPeriod(period: PeriodEntry): Long {
        val id = if (period.id != 0L) period.id else nextId++
        periods.removeAll { it.id == id }
        periods.add(period.copy(id = id))
        return id
    }

    override suspend fun updatePeriod(period: PeriodEntry) {
        val index = periods.indexOfFirst { it.id == period.id }
        if (index >= 0) periods[index] = period
    }

    override suspend fun deletePeriod(period: PeriodEntry) {
        periods.removeAll { it.id == period.id }
    }

    override suspend fun deleteAllPeriods() { periods.clear() }
    override suspend fun countPeriods(): Int = periods.size
}

/** In-memory [PeriodDayDao] fake — enforces the unique-date constraint like the real table. */
private class FakePeriodDayDao : PeriodDayDao {
    val days = mutableListOf<PeriodDayEntry>()
    private var nextId = 1L

    override fun getAllDays(): Flow<List<PeriodDayEntry>> = flowOf(days.sortedBy { it.date })
    override suspend fun getAllDaysOnce(): List<PeriodDayEntry> = days.sortedBy { it.date }
    override suspend fun getDay(date: String): PeriodDayEntry? = days.firstOrNull { it.date == date }
    override suspend fun getDaysInRange(start: String, end: String): List<PeriodDayEntry> =
        days.filter { it.date in start..end }.sortedBy { it.date }

    override suspend fun insertDay(day: PeriodDayEntry): Long {
        if (days.any { it.date == day.date }) return -1L
        val id = nextId++
        days.add(day.copy(id = id))
        return id
    }

    override suspend fun deleteDay(date: String) { days.removeAll { it.date == date } }
    override suspend fun deleteDaysInRange(start: String, end: String) {
        days.removeAll { it.date in start..end }
    }
    override suspend fun deleteAllDays() { days.clear() }
    override suspend fun countDays(): Int = days.size
}

/** In-memory [SymptomDao] fake for exercising [PeriodRepository]'s suspend write paths. */
private class FakeSymptomDao : SymptomDao {
    val symptoms = mutableListOf<SymptomEntry>()
    private var nextId = 1L

    override fun getSymptomsForPeriod(periodId: Long): Flow<List<SymptomEntry>> =
        flowOf(symptoms.filter { it.periodId == periodId })
    override suspend fun getSymptomsForPeriodOnce(periodId: Long): List<SymptomEntry> =
        symptoms.filter { it.periodId == periodId }
    override suspend fun insertSymptom(symptom: SymptomEntry) {
        symptoms.add(symptom.copy(id = nextId++))
    }
    override suspend fun deleteSymptomsByPeriodId(periodId: Long) {
        symptoms.removeAll { it.periodId == periodId }
    }
    override suspend fun deleteAllSymptoms() { symptoms.clear() }
    override suspend fun getAllSymptoms(): List<SymptomEntry> = symptoms.toList()
    override fun getAllSymptomsFlow(): Flow<List<SymptomEntry>> = flowOf(symptoms.toList())
    override suspend fun bulkRenameSymptoms(oldLabel: String, newLabel: String) {
        val renamed = symptoms.map { if (it.symptomType == oldLabel) it.copy(symptomType = newLabel) else it }
        symptoms.clear()
        symptoms.addAll(renamed)
    }
}

class PeriodRepositoryTest {

    private fun entry(startDate: String, endDate: String? = null, id: Long = 0L) =
        PeriodEntry(id = id, startDate = startDate, endDate = endDate)

    private fun date(s: String): LocalDate = LocalDate.parse(s)

    private data class Fixture(
        val repo: PeriodRepository,
        val periodDao: FakePeriodDao,
        val dayDao: FakePeriodDayDao,
        val symptomDao: FakeSymptomDao,
    )

    private fun buildRepository(vararg seedDays: String): Fixture {
        val periodDao = FakePeriodDao()
        val dayDao = FakePeriodDayDao()
        val symptomDao = FakeSymptomDao()
        val repo = PeriodRepository(periodDao, symptomDao, dayDao)
        runBlocking {
            seedDays.forEach { dayDao.insertDay(PeriodDayEntry(date = it)) }
        }
        return Fixture(repo, periodDao, dayDao, symptomDao)
    }

    // ── logPeriodDay: starting, continuing, bridging ──────────────────────────

    @Test
    fun `logging a day starts a new ongoing period`() = runBlocking {
        val f = buildRepository()
        val today = date("2024-06-01")
        val episode = f.repo.logPeriodDay(today, toleranceDays = 1, today = today)
        assertNotNull(episode)
        assertEquals("2024-06-01", episode!!.startDate)
        assertNull(episode.endDate) // ongoing while the tolerance window is open
        assertEquals(1, f.periodDao.periods.size)
    }

    @Test
    fun `logging consecutive days extends the same period`() = runBlocking {
        val f = buildRepository()
        f.repo.logPeriodDay(date("2024-06-01"), 1, today = date("2024-06-01"))
        f.repo.logPeriodDay(date("2024-06-02"), 1, today = date("2024-06-02"))
        val episode = f.repo.logPeriodDay(date("2024-06-03"), 1, today = date("2024-06-03"))!!
        assertEquals(1, f.periodDao.periods.size)
        assertEquals("2024-06-01", episode.startDate)
        assertNull(episode.endDate)
        // The episode keeps its original identity as it grows.
        assertEquals(f.periodDao.periods.single().id, episode.id)
    }

    @Test
    fun `a single missed day within tolerance still continues the period`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02")
        // 3rd unlogged; logging the 4th (gap 2 = tolerance 1 + 1) continues.
        val episode = f.repo.logPeriodDay(date("2024-06-04"), 1, today = date("2024-06-04"))!!
        assertEquals(1, f.periodDao.periods.size)
        assertEquals("2024-06-01", episode.startDate)
    }

    @Test
    fun `a gap beyond tolerance starts a separate period`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02")
        val episode = f.repo.logPeriodDay(date("2024-06-05"), 1, today = date("2024-06-05"))!!
        assertEquals(2, f.periodDao.periods.size)
        assertEquals("2024-06-05", episode.startDate)
        // The earlier period is auto-closed at its last logged day.
        val earlier = f.periodDao.periods.sortedBy { it.startDate }.first()
        assertEquals("2024-06-02", earlier.endDate)
    }

    @Test
    fun `zero tolerance treats a one-day gap as a new period`() = runBlocking {
        val f = buildRepository("2024-06-01")
        f.repo.logPeriodDay(date("2024-06-03"), 0, today = date("2024-06-03"))
        assertEquals(2, f.periodDao.periods.size)
    }

    @Test
    fun `logging the gap day between two periods merges them into one`() = runBlocking {
        val f = buildRepository("2024-06-26", "2024-06-27", "2024-06-28", "2024-06-30")
        f.repo.reconcile(0, today = date("2024-06-30")) // derive two separate episodes first
        assertEquals(2, f.periodDao.periods.size)
        val merged = f.repo.logPeriodDay(date("2024-06-29"), 0, today = date("2024-06-30"))!!
        assertEquals(1, f.periodDao.periods.size)
        assertEquals("2024-06-26", merged.startDate)
    }

    @Test
    fun `merging preserves the earlier episode's id and combines notes`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-05")
        f.repo.reconcile(0, today = date("2024-06-05"))
        val byStart = f.periodDao.periods.sortedBy { it.startDate }
        f.periodDao.updatePeriod(byStart[0].copy(notes = "first"))
        f.periodDao.updatePeriod(byStart[1].copy(notes = "second"))
        val earlierId = byStart[0].id

        f.repo.logPeriodDay(date("2024-06-03"), 1, today = date("2024-06-05"))
        val survivor = f.periodDao.periods.single()
        assertEquals(earlierId, survivor.id)
        assertTrue(survivor.notes.contains("first") && survivor.notes.contains("second"))
    }

    @Test
    fun `retro-logging a day just before the start extends the period backwards`() = runBlocking {
        val f = buildRepository("2024-06-02", "2024-06-03")
        f.repo.reconcile(1, today = date("2024-06-03"))
        val episode = f.repo.logPeriodDay(date("2024-06-01"), 1, today = date("2024-06-03"))!!
        assertEquals(1, f.periodDao.periods.size)
        assertEquals("2024-06-01", episode.startDate)
    }

    // ── Auto-end (reconcile) ──────────────────────────────────────────────────

    @Test
    fun `a period is deemed ended once the tolerance window lapses`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02", "2024-06-03")
        // Two days after the last logged day (= tolerance 1 + 1): still open.
        f.repo.reconcile(1, today = date("2024-06-05"))
        assertNull(f.periodDao.periods.single().endDate)
        // Three days after: closed at the last logged day.
        f.repo.reconcile(1, today = date("2024-06-06"))
        assertEquals("2024-06-03", f.periodDao.periods.single().endDate)
    }

    @Test
    fun `reconcile repairs overlapping legacy episodes into one`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02", "2024-06-03")
        f.periodDao.insertPeriod(entry("2024-06-01", "2024-06-03"))
        f.periodDao.insertPeriod(entry("2024-06-02", "2024-06-03"))
        f.repo.reconcile(1, today = date("2024-07-01"))
        assertEquals(1, f.periodDao.periods.size)
        val survivor = f.periodDao.periods.single()
        assertEquals("2024-06-01", survivor.startDate)
        assertEquals("2024-06-03", survivor.endDate)
    }

    @Test
    fun `reconcile deletes an episode whose days were all removed`() = runBlocking {
        val f = buildRepository()
        f.periodDao.insertPeriod(entry("2024-06-01", "2024-06-03"))
        f.repo.reconcile(1, today = date("2024-07-01"))
        assertTrue(f.periodDao.periods.isEmpty())
    }

    // ── Explicit end dates ────────────────────────────────────────────────────

    @Test
    fun `logPeriodRange records every day and the explicit end`() = runBlocking {
        val f = buildRepository()
        val episode = f.repo.logPeriodRange(
            date("2024-06-01"), date("2024-06-04"), 1, today = date("2024-06-04")
        )!!
        assertEquals("2024-06-01", episode.startDate)
        assertEquals("2024-06-04", episode.endDate) // explicit even though window still open
        assertEquals(4, f.dayDao.days.size)
    }

    @Test
    fun `an explicit end may extend past the last logged day`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02")
        f.repo.reconcile(1, today = date("2024-06-02"))
        val id = f.periodDao.periods.single().id
        val episode = f.repo.updateEpisode(
            id, date("2024-06-01"), date("2024-06-05"), "notes", 1, today = date("2024-06-02")
        )!!
        assertEquals("2024-06-05", episode.endDate)
        // No day rows fabricated for the declared tail.
        assertEquals(2, f.dayDao.days.size)
        // The explicit end survives later reconciles.
        f.repo.reconcile(1, today = date("2024-07-01"))
        assertEquals("2024-06-05", f.periodDao.periods.single().endDate)
    }

    @Test
    fun `logging a day shortly after an explicit end reopens and extends the period`() = runBlocking {
        val f = buildRepository()
        f.repo.logPeriodRange(date("2024-06-01"), date("2024-06-03"), 1, today = date("2024-06-03"))
        val episode = f.repo.logPeriodDay(date("2024-06-04"), 1, today = date("2024-06-04"))!!
        assertEquals(1, f.periodDao.periods.size)
        assertEquals("2024-06-01", episode.startDate)
        assertNull(episode.endDate) // stale explicit end dropped; period open again
    }

    @Test
    fun `updateEpisode trims days outside the new range`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02", "2024-06-03", "2024-06-04")
        f.repo.reconcile(1, today = date("2024-06-04"))
        val id = f.periodDao.periods.single().id
        val episode = f.repo.updateEpisode(
            id, date("2024-06-02"), date("2024-06-03"), "", 1, today = date("2024-06-04")
        )!!
        assertEquals("2024-06-02", episode.startDate)
        assertEquals("2024-06-03", episode.endDate)
        assertEquals(listOf("2024-06-02", "2024-06-03"), f.dayDao.days.map { it.date }.sorted())
    }

    @Test
    fun `updateEpisode moving the start earlier materialises the new days`() = runBlocking {
        val f = buildRepository("2024-06-03", "2024-06-04")
        f.repo.reconcile(1, today = date("2024-06-04"))
        val id = f.periodDao.periods.single().id
        val episode = f.repo.updateEpisode(
            id, date("2024-06-01"), null, "", 1, today = date("2024-06-04")
        )!!
        assertEquals("2024-06-01", episode.startDate)
        assertEquals(4, f.dayDao.days.size)
    }

    // ── Removing days ─────────────────────────────────────────────────────────

    @Test
    fun `removing a middle day splits the period under zero tolerance`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02", "2024-06-03", "2024-06-04", "2024-06-05")
        f.repo.reconcile(0, today = date("2024-07-01"))
        assertEquals(1, f.periodDao.periods.size)
        f.repo.unlogPeriodDay(date("2024-06-03"), 0, today = date("2024-07-01"))
        val episodes = f.periodDao.periods.sortedBy { it.startDate }
        assertEquals(2, episodes.size)
        assertEquals("2024-06-02", episodes[0].endDate)
        assertEquals("2024-06-04", episodes[1].startDate)
        assertEquals("2024-06-05", episodes[1].endDate)
    }

    @Test
    fun `removing a middle day within tolerance keeps one period`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02", "2024-06-03")
        f.repo.reconcile(1, today = date("2024-07-01"))
        f.repo.unlogPeriodDay(date("2024-06-02"), 1, today = date("2024-07-01"))
        assertEquals(1, f.periodDao.periods.size)
    }

    @Test
    fun `removing the only day deletes the period`() = runBlocking {
        val f = buildRepository("2024-06-01")
        f.repo.reconcile(1, today = date("2024-06-01"))
        assertEquals(1, f.periodDao.periods.size)
        f.repo.unlogPeriodDay(date("2024-06-01"), 1, today = date("2024-06-01"))
        assertTrue(f.periodDao.periods.isEmpty())
    }

    // ── Delete / restore / merge ──────────────────────────────────────────────

    @Test
    fun `deletePeriod removes the episode and its day rows`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02", "2024-06-05", "2024-06-06")
        f.repo.reconcile(1, today = date("2024-07-01"))
        val first = f.periodDao.periods.sortedBy { it.startDate }.first()
        val removed = f.repo.deletePeriod(first, 1)
        assertEquals(listOf("2024-06-01", "2024-06-02"), removed)
        assertEquals(1, f.periodDao.periods.size)
        assertEquals(listOf("2024-06-05", "2024-06-06"), f.dayDao.days.map { it.date }.sorted())
    }

    @Test
    fun `deletePeriod on an open episode removes its whole day chain`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02", "2024-06-03")
        f.repo.reconcile(1, today = date("2024-06-03"))
        val open = f.periodDao.periods.single()
        assertNull(open.endDate)
        val removed = f.repo.deletePeriod(open, 1)
        assertEquals(3, removed.size)
        assertTrue(f.dayDao.days.isEmpty())
    }

    @Test
    fun `restorePeriod brings back the episode with its days`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02")
        f.repo.reconcile(1, today = date("2024-07-01"))
        val episode = f.periodDao.periods.single()
        val days = f.repo.deletePeriod(episode, 1)
        assertTrue(f.periodDao.periods.isEmpty())

        f.repo.restorePeriod(episode, listOf("Cramps"), days, 1, today = date("2024-07-01"))
        val restored = f.periodDao.periods.single()
        assertEquals("2024-06-01", restored.startDate)
        assertEquals("2024-06-02", restored.endDate)
        assertEquals(days, f.dayDao.days.map { it.date }.sorted())
        assertEquals(listOf("Cramps"), f.symptomDao.symptoms.map { it.symptomType })
    }

    @Test
    fun `mergePeriods fills the gap days and produces one continuous period`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-02", "2024-06-07", "2024-06-08")
        f.repo.reconcile(1, today = date("2024-07-01"))
        val episodes = f.periodDao.periods.sortedBy { it.startDate }
        assertEquals(2, episodes.size)

        val merged = f.repo.mergePeriods(episodes[0], episodes[1], 1, today = date("2024-07-01"))!!
        assertEquals(1, f.periodDao.periods.size)
        assertEquals("2024-06-01", merged.startDate)
        assertEquals("2024-06-08", merged.endDate)
        assertEquals(8, f.dayDao.days.size) // gap days materialised
    }

    // ── Import round-trip ─────────────────────────────────────────────────────

    @Test
    fun `import restores per-day data when the export carries days`() = runBlocking {
        val f = buildRepository()
        val json = """
            [{"startDate":"2024-06-01","endDate":"2024-06-04","flowLevel":"Medium","notes":"",
              "symptoms":[],"days":["2024-06-01","2024-06-03","2024-06-04"]}]
        """.trimIndent()
        val result = f.repo.importData(json, replace = false)
        assertTrue(result is ImportResult.Success)
        assertEquals(listOf("2024-06-01", "2024-06-03", "2024-06-04"), f.dayDao.days.map { it.date }.sorted())
    }

    @Test
    fun `import expands a legacy export without days into the full range`() = runBlocking {
        val f = buildRepository()
        val json = """[{"startDate":"2024-06-01","endDate":"2024-06-03","flowLevel":"Medium","symptoms":[]}]"""
        val result = f.repo.importData(json, replace = false)
        assertTrue(result is ImportResult.Success)
        assertEquals(3, f.dayDao.days.size)
    }

    @Test
    fun `export includes the logged days for each period`() = runBlocking {
        val f = buildRepository("2024-06-01", "2024-06-03")
        f.repo.reconcile(1, today = date("2024-07-01"))
        val json = f.repo.exportData()
        assertTrue(json.contains("\"days\""))
        assertTrue(json.contains("2024-06-03"))
    }

    // ── calculateAvgCycleLength ───────────────────────────────────────────────

    @Test
    fun `calculateAvgCycleLength returns 28 for empty list`() {
        assertEquals(28, PeriodRepository.calculateAvgCycleLength(emptyList()))
    }

    @Test
    fun `calculateAvgCycleLength calculates mean gap between starts`() {
        val periods = listOf(
            entry("2024-01-01"),
            entry("2024-01-29"),  // gap 28
            entry("2024-02-26"),  // gap 28
        )
        assertEquals(28, PeriodRepository.calculateAvgCycleLength(periods))
    }

    @Test
    fun `calculateAvgCycleLength clamps result to minimum 21`() {
        val periods = listOf(
            entry("2024-01-01"),
            entry("2024-01-17"),
            entry("2024-02-02"),
        )
        assertEquals(21, PeriodRepository.calculateAvgCycleLength(periods))
    }

    @Test
    fun `calculateAvgCycleLength filters outlier gaps above 60 days`() {
        val periods = listOf(
            entry("2024-01-01"),
            entry("2024-04-01"),  // gap 91
        )
        assertEquals(28, PeriodRepository.calculateAvgCycleLength(periods))
    }

    // ── predictNextStart / ovulationDate / activePeriod ───────────────────────

    @Test
    fun `predictNextStart adds avgCycle days to latest start`() {
        val result = PeriodRepository.predictNextStart(listOf(entry("2024-01-01")), 28)
        assertEquals(LocalDate.of(2024, 1, 29), result)
    }

    @Test
    fun `ovulationDate returns half avgCycle from latest start`() {
        val result = PeriodRepository.ovulationDate(listOf(entry("2024-01-01")), 28)
        assertEquals(LocalDate.of(2024, 1, 15), result)  // day 1 + 14
    }

    @Test
    fun `activePeriod returns entry with null endDate`() {
        val active = entry("2024-01-15", null, id = 2)
        val ended = entry("2024-01-01", "2024-01-05", id = 1)
        assertEquals(active, PeriodRepository.activePeriod(listOf(ended, active)))
    }

    @Test
    fun `activePeriod returns null when all periods have end dates`() {
        assertNull(PeriodRepository.activePeriod(listOf(entry("2024-01-01", "2024-01-05"))))
    }

    // ── periodForDate ─────────────────────────────────────────────────────────

    @Test
    fun `periodForDate matches a date inside a closed range`() {
        val period = entry("2024-01-01", "2024-01-05", id = 1)
        assertEquals(period, PeriodRepository.periodForDate(listOf(period), LocalDate.of(2024, 1, 3)))
    }

    @Test
    fun `periodForDate matches any future date for an ongoing period`() {
        val ongoing = entry("2024-01-01", null, id = 1)
        assertEquals(ongoing, PeriodRepository.periodForDate(listOf(ongoing), LocalDate.of(2030, 6, 15)))
    }

    @Test
    fun `periodForDate covers the tolerance window after an explicit end`() {
        val period = entry("2024-06-25", "2024-06-28", id = 1)
        // tolerance 1 → up to end + 2 still belongs to this period
        assertEquals(period, PeriodRepository.periodForDate(listOf(period), LocalDate.of(2024, 6, 29), 1))
        assertEquals(period, PeriodRepository.periodForDate(listOf(period), LocalDate.of(2024, 6, 30), 1))
        assertNull(PeriodRepository.periodForDate(listOf(period), LocalDate.of(2024, 7, 1), 1))
    }

    @Test
    fun `periodForDate covers the tolerance window before the start`() {
        val period = entry("2024-06-25", "2024-06-28", id = 1)
        assertEquals(period, PeriodRepository.periodForDate(listOf(period), LocalDate.of(2024, 6, 23), 1))
        assertNull(PeriodRepository.periodForDate(listOf(period), LocalDate.of(2024, 6, 22), 1))
    }

    @Test
    fun `periodForDate respects zero tolerance`() {
        val period = entry("2024-06-25", "2024-06-28", id = 1)
        assertEquals(period, PeriodRepository.periodForDate(listOf(period), LocalDate.of(2024, 6, 29), 0))
        assertNull(PeriodRepository.periodForDate(listOf(period), LocalDate.of(2024, 6, 30), 0))
    }

    @Test
    fun `periodForDate returns null when no period matches`() {
        val periods = listOf(entry("2024-01-01", "2024-01-05", id = 1))
        assertNull(PeriodRepository.periodForDate(periods, LocalDate.of(2024, 1, 10)))
    }

    // ── Full lifecycle scenario ───────────────────────────────────────────────

    @Test
    fun `day-by-day logging lifecycle derives one period that auto-ends`() = runBlocking {
        val f = buildRepository()
        // Day 1: period starts.
        f.repo.logPeriodDay(date("2024-06-01"), 1, today = date("2024-06-01"))
        // Day 2: logged with (conceptually) different flow — same period.
        f.repo.logPeriodDay(date("2024-06-02"), 1, today = date("2024-06-02"))
        // Day 3 missed, day 4 logged — still the same period (tolerance 1).
        f.repo.logPeriodDay(date("2024-06-04"), 1, today = date("2024-06-04"))
        assertEquals(1, f.periodDao.periods.size)
        assertNull(f.periodDao.periods.single().endDate)

        // Days pass with no logging: the daily reconcile deems the period ended.
        f.repo.reconcile(1, today = date("2024-06-08"))
        val closed = f.periodDao.periods.single()
        assertEquals("2024-06-01", closed.startDate)
        assertEquals("2024-06-04", closed.endDate)

        // Two weeks later a new day is logged: a fresh period begins.
        f.repo.logPeriodDay(date("2024-06-29"), 1, today = date("2024-06-29"))
        assertEquals(2, f.periodDao.periods.size)
        assertFalse(f.periodDao.periods.any { it.startDate == "2024-06-29" && it.endDate != null })
    }
}

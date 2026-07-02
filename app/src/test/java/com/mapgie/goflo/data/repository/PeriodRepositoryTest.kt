package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.dao.PeriodDao
import com.mapgie.goflo.data.database.dao.SymptomDao
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    // ── calculateAvgCycleLength ───────────────────────────────────────────────

    @Test
    fun `calculateAvgCycleLength returns 28 for empty list`() {
        assertEquals(28, PeriodRepository.calculateAvgCycleLength(emptyList()))
    }

    @Test
    fun `calculateAvgCycleLength returns 28 for single entry`() {
        assertEquals(28, PeriodRepository.calculateAvgCycleLength(listOf(entry("2024-01-01"))))
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
    fun `calculateAvgCycleLength handles unsorted input`() {
        val periods = listOf(
            entry("2024-02-26"),
            entry("2024-01-01"),
            entry("2024-01-29"),
        )
        assertEquals(28, PeriodRepository.calculateAvgCycleLength(periods))
    }

    @Test
    fun `calculateAvgCycleLength clamps result to minimum 21`() {
        // gaps of 16 days each — average 16, clamped to 21
        val periods = listOf(
            entry("2024-01-01"),
            entry("2024-01-17"),
            entry("2024-02-02"),
        )
        assertEquals(21, PeriodRepository.calculateAvgCycleLength(periods))
    }

    @Test
    fun `calculateAvgCycleLength clamps result to maximum 35`() {
        // gap of 55 days — within filter range but average clamped to 35
        val periods = listOf(
            entry("2024-01-01"),
            entry("2024-02-25"),  // gap 55
        )
        assertEquals(35, PeriodRepository.calculateAvgCycleLength(periods))
    }

    @Test
    fun `calculateAvgCycleLength filters outlier gaps above 60 days`() {
        // gap of 90 days is filtered; no valid gaps → default 28
        val periods = listOf(
            entry("2024-01-01"),
            entry("2024-04-01"),  // gap 91
        )
        assertEquals(28, PeriodRepository.calculateAvgCycleLength(periods))
    }

    // ── predictNextStart ──────────────────────────────────────────────────────

    @Test
    fun `predictNextStart returns null for empty list`() {
        assertNull(PeriodRepository.predictNextStart(emptyList(), 28))
    }

    @Test
    fun `predictNextStart adds avgCycle days to latest start`() {
        val result = PeriodRepository.predictNextStart(listOf(entry("2024-01-01")), 28)
        assertEquals(LocalDate.of(2024, 1, 29), result)
    }

    @Test
    fun `predictNextStart uses latest period when multiple exist`() {
        val periods = listOf(entry("2024-01-01"), entry("2024-01-29"))
        val result = PeriodRepository.predictNextStart(periods, 28)
        assertEquals(LocalDate.of(2024, 2, 26), result)
    }

    // ── ovulationDate ─────────────────────────────────────────────────────────

    @Test
    fun `ovulationDate returns null for empty list`() {
        assertNull(PeriodRepository.ovulationDate(emptyList(), 28))
    }

    @Test
    fun `ovulationDate returns half avgCycle from latest start`() {
        val result = PeriodRepository.ovulationDate(listOf(entry("2024-01-01")), 28)
        assertEquals(LocalDate.of(2024, 1, 15), result)  // day 1 + 14
    }

    @Test
    fun `ovulationDate uses latest period when multiple exist`() {
        val periods = listOf(entry("2024-01-01"), entry("2024-01-29"))
        val result = PeriodRepository.ovulationDate(periods, 28)
        assertEquals(LocalDate.of(2024, 2, 12), result)  // Jan 29 + 14
    }

    // ── activePeriod ──────────────────────────────────────────────────────────

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

    @Test
    fun `activePeriod returns null for empty list`() {
        assertNull(PeriodRepository.activePeriod(emptyList()))
    }

    // ── resolvePeriodForLogging / mergePeriods ───────────────────────────────

    private fun buildRepository(vararg seed: PeriodEntry): Triple<PeriodRepository, FakePeriodDao, FakeSymptomDao> {
        val periodDao = FakePeriodDao()
        val symptomDao = FakeSymptomDao()
        seed.forEach { periodDao.periods.add(it) }
        return Triple(PeriodRepository(periodDao, symptomDao), periodDao, symptomDao)
    }

    @Test
    fun `resolvePeriodForLogging returns id of period already covering the date`() = runBlocking {
        val (repo, _, _) = buildRepository(entry("2024-01-01", "2024-01-05", id = 1))
        assertEquals(1L, repo.resolvePeriodForLogging(LocalDate.of(2024, 1, 3)))
    }

    @Test
    fun `resolvePeriodForLogging extends a period's end date to the following day`() = runBlocking {
        val (repo, dao, _) = buildRepository(entry("2024-01-01", "2024-01-05", id = 1))
        val result = repo.resolvePeriodForLogging(LocalDate.of(2024, 1, 6))
        assertEquals(1L, result)
        assertEquals("2024-01-06", dao.periods.first { it.id == 1L }.endDate)
    }

    @Test
    fun `resolvePeriodForLogging pulls a period's start date back to the preceding day`() = runBlocking {
        val (repo, dao, _) = buildRepository(entry("2024-01-05", "2024-01-08", id = 1))
        val result = repo.resolvePeriodForLogging(LocalDate.of(2024, 1, 4))
        assertEquals(1L, result)
        assertEquals("2024-01-04", dao.periods.first { it.id == 1L }.startDate)
    }

    @Test
    fun `resolvePeriodForLogging merges two periods when the date bridges a one-day gap`() = runBlocking {
        // Reproduces the reported bug: a period ending 28 June and another starting
        // 30 June should be joined into one continuous period by logging 29 June.
        val (repo, dao, _) = buildRepository(
            entry("2024-06-26", "2024-06-28", id = 1),
            entry("2024-06-30", null, id = 2),
        )
        val result = repo.resolvePeriodForLogging(LocalDate.of(2024, 6, 29))
        assertEquals(1L, result)
        assertEquals(1, dao.periods.size)
        val merged = dao.periods.single()
        assertEquals("2024-06-26", merged.startDate)
        assertNull(merged.endDate) // the later (ongoing) period's null end date wins
    }

    @Test
    fun `resolvePeriodForLogging returns null when no period is adjacent or covering`() = runBlocking {
        val (repo, _, _) = buildRepository(entry("2024-01-01", "2024-01-05", id = 1))
        assertNull(repo.resolvePeriodForLogging(LocalDate.of(2024, 1, 10)))
    }

    @Test
    fun `mergePeriods keeps the earlier id, combines notes and symptoms, and deletes the later entry`() = runBlocking {
        val earlier = entry("2024-06-26", "2024-06-28", id = 1).copy(notes = "cramps")
        val later = entry("2024-06-30", "2024-07-01", id = 2).copy(notes = "headache")
        val (repo, dao, symptomDao) = buildRepository(earlier, later)
        symptomDao.symptoms.add(SymptomEntry(periodId = 1, symptomType = "Cramps"))
        symptomDao.symptoms.add(SymptomEntry(periodId = 2, symptomType = "Fatigue"))

        val mergedId = repo.mergePeriods(earlier, later)

        assertEquals(1L, mergedId)
        assertEquals(1, dao.periods.size)
        val merged = dao.periods.single()
        assertEquals("2024-06-26", merged.startDate)
        assertEquals("2024-07-01", merged.endDate)
        assertTrue(merged.notes.contains("cramps"))
        assertTrue(merged.notes.contains("headache"))
        val mergedSymptoms = symptomDao.symptoms.filter { it.periodId == 1L }.map { it.symptomType }.toSet()
        assertEquals(setOf("Cramps", "Fatigue"), mergedSymptoms)
    }
}

package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.entities.PeriodEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

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
}

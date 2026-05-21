package com.mapgie.goflo.data.repository

import com.mapgie.goflo.data.database.dao.PeriodDao
import com.mapgie.goflo.data.database.dao.SymptomDao
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
import com.mapgie.goflo.data.model.SymptomType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PeriodRepository(
    private val periodDao: PeriodDao,
    private val symptomDao: SymptomDao
) {
    fun getAllPeriods(): Flow<List<PeriodEntry>> = periodDao.getAllPeriods()

    fun getPeriodById(id: Long): Flow<PeriodEntry?> = periodDao.getPeriodById(id)

    fun getSymptomsForPeriod(periodId: Long): Flow<List<SymptomEntry>> =
        symptomDao.getSymptomsForPeriod(periodId)

    suspend fun insertPeriod(entry: PeriodEntry, symptoms: List<SymptomType>): Long {
        val id = periodDao.insertPeriod(entry)
        symptoms.forEach { symptomDao.insertSymptom(SymptomEntry(periodId = id, symptomType = it.name)) }
        return id
    }

    suspend fun updatePeriod(entry: PeriodEntry, symptoms: List<SymptomType>) {
        periodDao.updatePeriod(entry)
        symptomDao.deleteSymptomsByPeriodId(entry.id)
        symptoms.forEach { symptomDao.insertSymptom(SymptomEntry(periodId = entry.id, symptomType = it.name)) }
    }

    suspend fun deletePeriod(entry: PeriodEntry) {
        periodDao.deletePeriod(entry)
    }

    suspend fun getSymptomsOnce(periodId: Long): List<SymptomEntry> =
        symptomDao.getSymptomsForPeriodOnce(periodId)

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

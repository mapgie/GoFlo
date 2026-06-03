package com.mapgie.goflo.ui.screens.stats

import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingLog
import com.mapgie.goflo.data.repository.TrackingRepository
import com.mapgie.goflo.ui.util.decodeScaleLabels
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

// ── Shared chart-data computation utilities ───────────────────────────────────
//
// These are package-level functions extracted from StatsViewModel so that
// DashboardViewModel can compute chart data without duplicating the logic.

/**
 * Computes [StatsChartData] for the given inputs. All data-fetching is done
 * via [repository] (suspend calls); the result is returned directly.
 */
internal suspend fun computeChartData(
    category1: TrackingCategory,
    category2: TrackingCategory?,
    chartType: ChartType,
    repository: TrackingRepository,
    start: LocalDate,
    end: LocalDate,
    timeRange: TimeRange,
): StatsChartData {
    return when (chartType) {

        ChartType.PIE -> {
            val counts = repository.getValueCountsForCategory(category1.id, start, end)
            if (counts.isEmpty()) {
                StatsChartData.Empty
            } else {
                val total = counts.sumOf { it.count }.coerceAtLeast(1)
                StatsChartData.PieData(
                    counts.map { PieSlice(it.valueLabel, it.count, it.count.toFloat() / total) }
                )
            }
        }

        ChartType.TIME_SERIES -> {
            val logs = repository.getLogsForCategoryInRange(category1.id, start, end)
            val countMap: Map<Long, Int> = if (category1.categoryType == "increment") {
                logs.associate { log ->
                    log.id to (repository.getValuesForLog(log.id).firstOrNull()?.toIntOrNull() ?: 1)
                }
            } else emptyMap()
            val buckets = chartGroupByTimeBucket(logs, start, end, timeRange, countMap)
            if (buckets.isEmpty()) StatsChartData.Empty
            else StatsChartData.TimeSeriesData(buckets, category1.name)
        }

        ChartType.COMBO -> {
            if (category2 == null) {
                StatsChartData.Empty
            } else {
                val logs1 = repository.getLogsForCategoryInRange(category1.id, start, end)
                val logs2 = repository.getLogsForCategoryInRange(category2.id, start, end)
                val combos = chartBuildComboCounts(logs1, logs2, repository)
                if (combos.isEmpty()) StatsChartData.Empty
                else StatsChartData.ComboData(combos)
            }
        }

        ChartType.DUAL_TIME_SERIES -> {
            if (category2 == null) {
                StatsChartData.Empty
            } else {
                val logs1 = repository.getLogsForCategoryInRange(category1.id, start, end)
                val logs2 = repository.getLogsForCategoryInRange(category2.id, start, end)
                val buckets = chartBuildDualBuckets(logs1, logs2, start, end, timeRange)
                if (buckets.isEmpty()) StatsChartData.Empty
                else StatsChartData.DualTimeSeriesData(buckets, category1.name, category2.name, category1.colorToken, category2.colorToken)
            }
        }

        ChartType.NUMERIC_AVERAGE -> {
            val logs = repository.getLogsForCategoryInRange(category1.id, start, end)
            val logValues = logs.mapNotNull { log ->
                repository.getValuesForLog(log.id).firstOrNull()?.toFloatOrNull()
                    ?.let { log to it }
            }
            if (logValues.isEmpty()) {
                StatsChartData.Empty
            } else {
                val buckets = chartBuildNumericAverageBuckets(logValues, start, end, timeRange)
                val allVals = logValues.map { it.second }
                StatsChartData.NumericAverageData(
                    buckets      = buckets,
                    categoryName = category1.name,
                    globalMin    = allVals.min(),
                    globalMax    = allVals.max(),
                )
            }
        }

        ChartType.NUMERIC_DISTRIBUTION -> {
            val logs = repository.getLogsForCategoryInRange(category1.id, start, end)
            val counts = mutableMapOf<String, Int>()
            for (log in logs) {
                val label = repository.getValuesForLog(log.id).firstOrNull() ?: continue
                counts[label] = (counts[label] ?: 0) + 1
            }
            if (counts.isEmpty()) {
                StatsChartData.Empty
            } else {
                val scaleMap = category1.scaleLabels.decodeScaleLabels()
                val bars = counts.entries
                    .sortedBy { it.key.toFloatOrNull() ?: Float.MAX_VALUE }
                    .map { entry ->
                        val label = entry.key.toIntOrNull()?.let { scaleMap[it] }
                        NumericHistBar(label ?: entry.key, entry.value)
                    }
                StatsChartData.NumericDistributionData(bars, category1.name)
            }
        }

        ChartType.SCATTER -> {
            if (category2 == null || !category1.isNumeric || !category2.isNumeric) {
                StatsChartData.Empty
            } else {
                val logs1 = repository.getLogsForCategoryInRange(category1.id, start, end)
                val logs2 = repository.getLogsForCategoryInRange(category2.id, start, end)
                val byDate1 = logs1.associateBy { it.date }
                val byDate2 = logs2.associateBy { it.date }
                val points = mutableListOf<ScatterPoint>()
                for (date in (byDate1.keys intersect byDate2.keys)) {
                    val x = repository.getValuesForLog(byDate1[date]!!.id).firstOrNull()?.toFloatOrNull() ?: continue
                    val y = repository.getValuesForLog(byDate2[date]!!.id).firstOrNull()?.toFloatOrNull() ?: continue
                    points.add(ScatterPoint(x, y))
                }
                if (points.isEmpty()) StatsChartData.Empty
                else {
                    val useZeroBaseline = timeRange !is TimeRange.AllTime && timeRange !is TimeRange.YearToDate
                    StatsChartData.ScatterData(
                        points    = points,
                        xAxisName = category1.name,
                        yAxisName = category2.name,
                        xMin = if (useZeroBaseline) 0f else points.minOf { it.x },
                        xMax = points.maxOf { it.x },
                        yMin = if (useZeroBaseline) 0f else points.minOf { it.y },
                        yMax = points.maxOf { it.y },
                    )
                }
            }
        }

        ChartType.TIME_SCATTER -> {
            val logs = repository.getLogsForCategoryInRange(category1.id, start, end)
            val points = mutableListOf<TimeScatterPoint>()
            for (log in logs) {
                val date = LocalDate.parse(log.date)
                val value = repository.getValuesForLog(log.id).firstOrNull()?.toFloatOrNull() ?: continue
                val offset = ChronoUnit.DAYS.between(start, date).toInt()
                val label = date.format(DateTimeFormatter.ofPattern("d MMM"))
                points.add(TimeScatterPoint(dayOffset = offset, dateLabel = label, value = value))
            }
            if (points.isEmpty()) StatsChartData.Empty
            else {
                val useZeroBaseline = timeRange !is TimeRange.AllTime && timeRange !is TimeRange.YearToDate
                StatsChartData.TimeScatterData(
                    points = points,
                    yAxisName = category1.name,
                    yMin = if (useZeroBaseline) 0f else points.minOf { it.value },
                    yMax = points.maxOf { it.value },
                )
            }
        }

        ChartType.TRENDS -> {
            val counts = repository.getValueCountsForCategory(category1.id, start, end)
            if (counts.isEmpty()) StatsChartData.Empty
            else {
                val total = counts.sumOf { it.count }.coerceAtLeast(1)
                val bars = counts.sortedByDescending { it.count }.take(10).map { vc ->
                    TrendsBar(
                        label = vc.valueLabel,
                        count = vc.count,
                        percentage = (vc.count * 100 / total).coerceAtMost(100)
                    )
                }
                StatsChartData.TrendsData(bars, category1.name)
            }
        }

        ChartType.PHASE_SUMMARY -> StatsChartData.Empty
    }
}

// ── Time bucket helpers ───────────────────────────────────────────────────────

/**
 * Groups [logs] into time buckets. Uses daily buckets for SpecificMonth,
 * weekly buckets for ranges <= 90 days, monthly buckets otherwise.
 */
internal fun chartGroupByTimeBucket(
    logs: List<TrackingLog>,
    start: LocalDate,
    end: LocalDate,
    timeRange: TimeRange = TimeRange.AllTime,
    incrementCountMap: Map<Long, Int> = emptyMap(),
): List<TimeBucket> {
    if (logs.isEmpty()) return emptyList()
    return when {
        timeRange is TimeRange.SpecificMonth -> chartGroupByDay(logs, start, end, incrementCountMap)
        ChronoUnit.DAYS.between(start, end) <= 90 -> chartGroupByWeek(logs, start, end, incrementCountMap)
        else -> chartGroupByMonth(logs, start, end, incrementCountMap)
    }
}

internal fun chartGroupByDay(
    logs: List<TrackingLog>,
    start: LocalDate,
    end: LocalDate,
    incrementCountMap: Map<Long, Int> = emptyMap(),
): List<TimeBucket> {
    val days = mutableListOf<LocalDate>()
    var d = start
    while (!d.isAfter(end)) { days.add(d); d = d.plusDays(1) }
    val logsByDay = logs.groupBy { LocalDate.parse(it.date) }
    val shortFmt = DateTimeFormatter.ofPattern("d MMM")
    return days.map { day ->
        val dayLogs = logsByDay[day] ?: emptyList()
        val count = if (incrementCountMap.isNotEmpty())
            dayLogs.sumOf { incrementCountMap[it.id] ?: 1 }
        else dayLogs.size
        TimeBucket(label = day.format(shortFmt), count = count)
    }
}

internal fun chartGroupByMonth(
    logs: List<TrackingLog>,
    start: LocalDate,
    end: LocalDate,
    incrementCountMap: Map<Long, Int> = emptyMap(),
): List<TimeBucket> {
    val months = mutableListOf<YearMonth>()
    var current = YearMonth.from(start)
    val endMonth = YearMonth.from(end)
    while (!current.isAfter(endMonth)) {
        months.add(current)
        current = current.plusMonths(1)
    }
    val logsByMonth = logs.groupBy { log ->
        YearMonth.from(LocalDate.parse(log.date))
    }
    val shortFmt = DateTimeFormatter.ofPattern("MMM yy")
    return months.map { ym ->
        val monthLogs = logsByMonth[ym] ?: emptyList()
        val count = if (incrementCountMap.isNotEmpty())
            monthLogs.sumOf { incrementCountMap[it.id] ?: 1 }
        else monthLogs.size
        TimeBucket(label = ym.format(shortFmt), count = count)
    }
}

internal fun chartGroupByWeek(
    logs: List<TrackingLog>,
    start: LocalDate,
    end: LocalDate,
    incrementCountMap: Map<Long, Int> = emptyMap(),
): List<TimeBucket> {
    val weekFields = WeekFields.of(Locale.getDefault())
    val weeks = mutableListOf<LocalDate>()
    var weekStart = start.with(weekFields.dayOfWeek(), 1)
    val lastWeekStart = end.with(weekFields.dayOfWeek(), 1)
    while (!weekStart.isAfter(lastWeekStart)) {
        weeks.add(weekStart)
        weekStart = weekStart.plusWeeks(1)
    }
    val logsByWeek = logs.groupBy { log ->
        val date = LocalDate.parse(log.date)
        date.with(weekFields.dayOfWeek(), 1)
    }
    return weeks.map { ws ->
        val label = "W${ws.get(weekFields.weekOfWeekBasedYear())} '${ws.format(DateTimeFormatter.ofPattern("yy"))}"
        val weekLogs = logsByWeek[ws] ?: emptyList()
        val count = if (incrementCountMap.isNotEmpty())
            weekLogs.sumOf { incrementCountMap[it.id] ?: 1 }
        else weekLogs.size
        TimeBucket(label = label, count = count)
    }
}

// ── Numeric average bucket helpers ────────────────────────────────────────────

internal fun chartBuildNumericAverageBuckets(
    logValues: List<Pair<TrackingLog, Float>>,
    start: LocalDate,
    end: LocalDate,
    timeRange: TimeRange = TimeRange.AllTime
): List<NumericBucket> {
    if (logValues.isEmpty()) return emptyList()
    return when {
        timeRange is TimeRange.SpecificMonth -> chartBuildNumericDayBuckets(logValues, start, end)
        ChronoUnit.DAYS.between(start, end) <= 90 -> chartBuildNumericWeekBuckets(logValues, start, end)
        else -> chartBuildNumericMonthBuckets(logValues, start, end)
    }
}

internal fun chartBuildNumericDayBuckets(
    logValues: List<Pair<TrackingLog, Float>>,
    start: LocalDate,
    end: LocalDate
): List<NumericBucket> {
    val days = mutableListOf<LocalDate>()
    var d = start
    while (!d.isAfter(end)) { days.add(d); d = d.plusDays(1) }
    val byDay = logValues.groupBy { (log, _) -> LocalDate.parse(log.date) }
    val shortFmt = DateTimeFormatter.ofPattern("d MMM")
    return days.mapNotNull { day ->
        val entries = byDay[day] ?: return@mapNotNull null
        val avg = entries.map { it.second }.average().toFloat()
        NumericBucket(day.format(shortFmt), avg, entries.size)
    }
}

internal fun chartBuildNumericWeekBuckets(
    logValues: List<Pair<TrackingLog, Float>>,
    start: LocalDate,
    end: LocalDate
): List<NumericBucket> {
    val weekFields = WeekFields.of(Locale.getDefault())
    val weeks = mutableListOf<LocalDate>()
    var ws = start.with(weekFields.dayOfWeek(), 1)
    val lastWs = end.with(weekFields.dayOfWeek(), 1)
    while (!ws.isAfter(lastWs)) { weeks.add(ws); ws = ws.plusWeeks(1) }
    val byWeek = logValues.groupBy { (log, _) ->
        LocalDate.parse(log.date).with(weekFields.dayOfWeek(), 1)
    }
    return weeks.mapNotNull { weekStart ->
        val entries = byWeek[weekStart] ?: return@mapNotNull null
        val avg = entries.map { it.second }.average().toFloat()
        val label = "W${weekStart.get(weekFields.weekOfWeekBasedYear())} '${weekStart.format(DateTimeFormatter.ofPattern("yy"))}"
        NumericBucket(label, avg, entries.size)
    }
}

internal fun chartBuildNumericMonthBuckets(
    logValues: List<Pair<TrackingLog, Float>>,
    start: LocalDate,
    end: LocalDate
): List<NumericBucket> {
    val months = mutableListOf<YearMonth>()
    var m = YearMonth.from(start)
    val endMonth = YearMonth.from(end)
    while (!m.isAfter(endMonth)) { months.add(m); m = m.plusMonths(1) }
    val byMonth = logValues.groupBy { (log, _) ->
        YearMonth.from(LocalDate.parse(log.date))
    }
    val fmt = DateTimeFormatter.ofPattern("MMM yy")
    return months.mapNotNull { ym ->
        val entries = byMonth[ym] ?: return@mapNotNull null
        val avg = entries.map { it.second }.average().toFloat()
        NumericBucket(ym.format(fmt), avg, entries.size)
    }
}

// ── Combo count helpers ───────────────────────────────────────────────────────

internal suspend fun chartBuildComboCounts(
    logs1: List<TrackingLog>,
    logs2: List<TrackingLog>,
    repository: TrackingRepository,
): List<ComboBar> {
    val byDate1 = logs1.associateBy { it.date }
    val byDate2 = logs2.associateBy { it.date }
    val sharedDates = byDate1.keys intersect byDate2.keys
    val comboCounts = mutableMapOf<String, Int>()
    for (date in sharedDates) {
        val log1 = byDate1[date] ?: continue
        val log2 = byDate2[date] ?: continue
        val values1 = repository.getValuesForLog(log1.id).ifEmpty { listOf("(no value)") }
        val values2 = repository.getValuesForLog(log2.id).ifEmpty { listOf("(no value)") }
        for (v1 in values1) {
            for (v2 in values2) {
                val key = "$v1 + $v2"
                comboCounts[key] = (comboCounts[key] ?: 0) + 1
            }
        }
    }
    return comboCounts.entries
        .sortedByDescending { it.value }
        .map { ComboBar(it.key, it.value) }
}

// ── Dual time series helpers ──────────────────────────────────────────────────

internal fun chartBuildDualBuckets(
    logs1: List<TrackingLog>,
    logs2: List<TrackingLog>,
    start: LocalDate,
    end: LocalDate,
    timeRange: TimeRange = TimeRange.AllTime
): List<DualBucket> {
    return when {
        timeRange is TimeRange.SpecificMonth -> chartBuildDualDayBuckets(logs1, logs2, start, end)
        ChronoUnit.DAYS.between(start, end) <= 90 -> chartBuildDualWeekBuckets(logs1, logs2, start, end)
        else -> chartBuildDualMonthBuckets(logs1, logs2, start, end)
    }
}

internal fun chartBuildDualDayBuckets(
    logs1: List<TrackingLog>,
    logs2: List<TrackingLog>,
    start: LocalDate,
    end: LocalDate
): List<DualBucket> {
    val days = mutableListOf<LocalDate>()
    var d = start
    while (!d.isAfter(end)) { days.add(d); d = d.plusDays(1) }
    val logs1ByDay = logs1.groupBy { LocalDate.parse(it.date) }
    val logs2ByDay = logs2.groupBy { LocalDate.parse(it.date) }
    val shortFmt = DateTimeFormatter.ofPattern("d MMM")
    return days.map { day ->
        DualBucket(
            label = day.format(shortFmt),
            count1 = logs1ByDay[day]?.size ?: 0,
            count2 = logs2ByDay[day]?.size ?: 0
        )
    }
}

internal fun chartBuildDualWeekBuckets(
    logs1: List<TrackingLog>,
    logs2: List<TrackingLog>,
    start: LocalDate,
    end: LocalDate
): List<DualBucket> {
    val weekFields = WeekFields.of(Locale.getDefault())
    val weeks = mutableListOf<LocalDate>()
    var ws = start.with(weekFields.dayOfWeek(), 1)
    val lastWs = end.with(weekFields.dayOfWeek(), 1)
    while (!ws.isAfter(lastWs)) { weeks.add(ws); ws = ws.plusWeeks(1) }
    val logs1ByWeek = logs1.groupBy { LocalDate.parse(it.date).with(weekFields.dayOfWeek(), 1) }
    val logs2ByWeek = logs2.groupBy { LocalDate.parse(it.date).with(weekFields.dayOfWeek(), 1) }
    return weeks.map { weekStart ->
        val label = "W${weekStart.get(weekFields.weekOfWeekBasedYear())} '${weekStart.format(DateTimeFormatter.ofPattern("yy"))}"
        DualBucket(
            label = label,
            count1 = logs1ByWeek[weekStart]?.size ?: 0,
            count2 = logs2ByWeek[weekStart]?.size ?: 0
        )
    }
}

internal fun chartBuildDualMonthBuckets(
    logs1: List<TrackingLog>,
    logs2: List<TrackingLog>,
    start: LocalDate,
    end: LocalDate
): List<DualBucket> {
    val months = mutableListOf<YearMonth>()
    var m = YearMonth.from(start)
    val endMonth = YearMonth.from(end)
    while (!m.isAfter(endMonth)) { months.add(m); m = m.plusMonths(1) }
    val logs1ByMonth = logs1.groupBy { YearMonth.from(LocalDate.parse(it.date)) }
    val logs2ByMonth = logs2.groupBy { YearMonth.from(LocalDate.parse(it.date)) }
    val shortFmt = DateTimeFormatter.ofPattern("MMM yy")
    return months.map { ym ->
        DualBucket(
            label = ym.format(shortFmt),
            count1 = logs1ByMonth[ym]?.size ?: 0,
            count2 = logs2ByMonth[ym]?.size ?: 0
        )
    }
}

package com.mapgie.goflo.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.mapgie.goflo.ui.util.ordinalShade
import com.mapgie.goflo.ui.util.toCategoryColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ── Colour cycling ─────────────────────────────────────────────────────────────────────

@Composable
private fun chartColor(index: Int): Color {
    val cs = MaterialTheme.colorScheme
    // Order skips adjacent theme pairs (primary/secondary can be similar in some palettes)
    val base = listOf(cs.primary, cs.tertiary, cs.error, cs.secondary)
    return if (index < base.size) base[index]
    else base[index % base.size].copy(alpha = maxOf(0.5f, 1f - (index / base.size) * 0.2f))
}

// ── Pie chart ───────────────────────────────────────────────────────────────────────

/**
 * A Canvas-drawn pie chart with a wrapped legend below.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieChart(
    data: StatsChartData.PieData,
    modifier: Modifier = Modifier
) {
    val surface = MaterialTheme.colorScheme.surface
    val baseOrdinalColor = if (data.colorToken.isNotEmpty() && data.valueOrders.isNotEmpty()) {
        data.colorToken.toCategoryColor()
    } else null
    val orderedLabels = data.valueOrders.entries.sortedBy { it.value }.map { it.key }
    val colors = data.slices.mapIndexed { i, slice ->
        if (baseOrdinalColor != null) {
            val pos = orderedLabels.indexOf(slice.label)
            if (pos >= 0) ordinalShade(baseOrdinalColor, surface, pos, orderedLabels.size)
            else chartColor(i)
        } else chartColor(i)
    }
    val holeColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp)
        ) {
            val diameter = minOf(size.width, size.height) * 0.9f
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f
            data.slices.forEachIndexed { i, slice ->
                val sweep = slice.fraction * 360f
                drawArc(
                    color = colors[i],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
                startAngle += sweep
            }

            // Donut hole — draw on top of arcs in the card's surface colour
            val holeRadius = diameter * 0.30f
            drawCircle(
                color = holeColor,
                radius = holeRadius,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }

        // Legend
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            data.slices.forEachIndexed { i, slice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors[i])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${slice.label} (${(slice.fraction * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ── Bar chart (single time series) ───────────────────────────────────────────────

/**
 * Vertical bar chart for a single category over time.
 * Scrollable horizontally when there are many buckets.
 * [zoomLevel] 0=compact (28 dp), 1=normal (52 dp), 2=wide (80 dp).
 */
@Composable
fun BarChart(
    data: StatsChartData.TimeSeriesData,
    modifier: Modifier = Modifier,
    zoomLevel: Int = 1,
) {
    val barWidthDp = when (zoomLevel) { 0 -> 28.dp; 2 -> 80.dp; else -> 52.dp }
    val maxCount = data.buckets.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.Bottom
    ) {
        data.buckets.forEach { bucket ->
            BarColumn(
                count = bucket.count,
                maxCount = maxCount,
                label = bucket.label,
                barColor = barColor,
                widthDp = barWidthDp,
            )
        }
    }
}

// ── Dual bar chart (two time series) ─────────────────────────────────────────────────

/**
 * Vertical bar chart showing two categories side-by-side per time bucket.
 * [zoomLevel] 0=compact (40 dp), 1=normal (64 dp), 2=wide (96 dp).
 */
@Composable
fun DualBarChart(
    data: StatsChartData.DualTimeSeriesData,
    modifier: Modifier = Modifier,
    zoomLevel: Int = 1,
) {
    val maxCount = data.buckets
        .flatMap { listOf(it.count1, it.count2) }
        .maxOrNull()?.coerceAtLeast(1) ?: 1
    val color1 = if (data.colorToken1.isNotEmpty()) data.colorToken1.toCategoryColor()
                 else MaterialTheme.colorScheme.primary
    val color2 = if (data.colorToken2.isNotEmpty()) data.colorToken2.toCategoryColor()
                 else MaterialTheme.colorScheme.tertiary

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = color1, label = data.categoryName1)
            LegendItem(color = color2, label = data.categoryName2)
        }

        val bucketWidthDp = when (zoomLevel) { 0 -> 40.dp; 2 -> 96.dp; else -> 64.dp }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom
        ) {
            data.buckets.forEach { bucket ->
                Column(
                    modifier = Modifier.width(bucketWidthDp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        DualBarSegment(count = bucket.count1, maxCount = maxCount, color = color1, modifier = Modifier.weight(1f))
                        DualBarSegment(count = bucket.count2, maxCount = maxCount, color = color2, modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DualBarSegment(count: Int, maxCount: Int, color: Color, modifier: Modifier = Modifier) {
    val fraction = if (count == 0) 0f else count.toFloat() / maxCount.toFloat()
    Column(
        modifier = modifier.height(160.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (count > 0) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(color)
        )
    }
}

// ── Combo bar chart (horizontal, cross-category) ────────────────────────────────────────────

/**
 * Horizontal bar chart showing which value combinations co-occur most.
 * Uses a proportional fill inside a fixed-width container rather than weight
 * so that bars genuinely represent frequency relative to the maximum.
 */
@Composable
fun ComboBarChart(
    data: StatsChartData.ComboData,
    modifier: Modifier = Modifier
) {
    val maxCount = data.bars.firstOrNull()?.count?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        data.bars.take(12).forEach { bar ->
            val fraction = bar.count.toFloat() / maxCount.toFloat()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label column — fixed width so bars are always aligned
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(130.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Bar container — takes remaining space; fill is proportional inside
                Box(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(18.dp)
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(barColor)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = bar.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 20.dp)
                )
            }
        }
    }
}

// ── Shared helpers ──────────────────────────────────────────────────────────────────────

@Composable
private fun BarColumn(count: Int, maxCount: Int, label: String, barColor: Color, widthDp: androidx.compose.ui.unit.Dp = 52.dp) {
    val fraction = if (count == 0) 0f else count.toFloat() / maxCount.toFloat()
    Column(
        modifier = Modifier.width(widthDp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Count label above bar
        Text(
            text = if (count > 0) count.toString() else "",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(16.dp)
        )
        // Bar body — grows from bottom using Arrangement.Bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(barColor)
            )
        }
        // X-axis label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Numeric average chart ─────────────────────────────────────────────────────────────────

/**
 * Bar chart showing the **average** numeric value per time bucket.
 *
 * Bars are scaled relative to the range (globalMin..globalMax) across all
 * buckets so even a narrow range looks meaningful.  Buckets with no data are
 * omitted (the helper only emits buckets that have entries).
 */
@Composable
fun NumericAverageChart(data: StatsChartData.NumericAverageData, zoomLevel: Int = 1) {
    val primary = MaterialTheme.colorScheme.primary
    val rangeSpan = (data.globalMax - data.globalMin).coerceAtLeast(0.001f)
    val barWidthDp = when (zoomLevel) { 0 -> 28.dp; 2 -> 80.dp; else -> 52.dp }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "${data.categoryName} — average per period",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.buckets.forEach { bucket ->
                // Height fraction: 0.1 baseline so even min-value bars are visible
                val fraction = (0.1f + 0.9f * ((bucket.average - data.globalMin) / rangeSpan))
                    .coerceIn(0.05f, 1f)
                val avgLabel = if (bucket.average % 1f == 0f)
                    bucket.average.toInt().toString()
                else "%.1f".format(bucket.average)

                Column(
                    modifier = Modifier.width(barWidthDp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Average value label above bar
                    Text(
                        text = avgLabel,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(16.dp)
                    )
                    // Bar body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 6.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(primary)
                        )
                    }
                    // X-axis label
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// ── Numeric distribution chart ─────────────────────────────────────────────────────────

/**
 * Donut chart showing how often each discrete numeric value was logged, matching
 * the ring visual used for the categorical Distribution view.
 */
@Composable
fun NumericDistributionChart(data: StatsChartData.NumericDistributionData) {
    val total = data.bars.sumOf { it.count }.coerceAtLeast(1)
    val pieData = StatsChartData.PieData(
        slices = data.bars.map { bar ->
            PieSlice(bar.label, bar.count, bar.count.toFloat() / total)
        }
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${data.categoryName} — value distribution",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 0.dp)
        )
        PieChart(data = pieData)
    }
}

// ── Scatter plot ───────────────────────────────────────────────────────────────────────────

@Composable
fun ScatterPlot(data: StatsChartData.ScatterData, modifier: Modifier = Modifier) {
    val primary          = MaterialTheme.colorScheme.primary
    val outline          = MaterialTheme.colorScheme.outlineVariant
    val labelStyle       = MaterialTheme.typography.labelSmall
    val labelColor       = MaterialTheme.colorScheme.onSurfaceVariant

    fun fmtVal(v: Float) = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)

    val xRange = (data.xMax - data.xMin).coerceAtLeast(0.001f)
    val yRange = (data.yMax - data.yMin).coerceAtLeast(0.001f)
    val midY   = (data.yMin + data.yMax) / 2f
    val midX   = (data.xMin + data.xMax) / 2f

    Column(modifier = modifier.fillMaxWidth()) {
        // Y axis name above the plot
        Text(
            text = "↑  ${data.yAxisName}",
            style = labelStyle,
            color = labelColor,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        // Y tick labels + canvas
        Row(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Column(
                modifier = Modifier.width(44.dp).fillMaxHeight().padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(fmtVal(data.yMax), style = labelStyle, color = labelColor, textAlign = TextAlign.End)
                if (data.yMin != data.yMax) {
                    Text(fmtVal(midY), style = labelStyle, color = labelColor, textAlign = TextAlign.End)
                }
                Text(fmtVal(data.yMin), style = labelStyle, color = labelColor, textAlign = TextAlign.End)
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val plotW = size.width
                val plotH = size.height

                fun toCanvasX(v: Float) = (v - data.xMin) / xRange * plotW
                fun toCanvasY(v: Float) = plotH - (v - data.yMin) / yRange * plotH

                // Faint grid lines
                for (frac in listOf(0.25f, 0.5f, 0.75f)) {
                    drawLine(
                        outline.copy(alpha = 0.3f),
                        Offset(0f, frac * plotH), Offset(plotW, frac * plotH),
                        strokeWidth = 0.5.dp.toPx()
                    )
                    drawLine(
                        outline.copy(alpha = 0.3f),
                        Offset(frac * plotW, 0f), Offset(frac * plotW, plotH),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }

                // Axes
                val axisStroke = 1.5.dp.toPx()
                drawLine(outline, Offset(0f, 0f), Offset(0f, plotH), strokeWidth = axisStroke)
                drawLine(outline, Offset(0f, plotH), Offset(plotW, plotH), strokeWidth = axisStroke)

                // Data points
                val dotRadius = 4.5.dp.toPx()
                data.points.forEach { pt ->
                    drawCircle(
                        primary.copy(alpha = 0.75f),
                        dotRadius,
                        Offset(toCanvasX(pt.x), toCanvasY(pt.y))
                    )
                }
            }
        }

        // X axis tick labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(fmtVal(data.xMin), style = labelStyle, color = labelColor)
            if (data.xMin != data.xMax) {
                Text(fmtVal(midX), style = labelStyle, color = labelColor)
            }
            Text(fmtVal(data.xMax), style = labelStyle, color = labelColor)
        }

        // X axis name
        Text(
            text = "${data.xAxisName}  →",
            style = labelStyle,
            color = labelColor,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 2.dp)
        )
    }
}

// ── Time scatter chart (time on X, category value on Y) ──────────────────────

@Composable
fun TimeScatterChart(data: StatsChartData.TimeScatterData, modifier: Modifier = Modifier) {
    val primary    = MaterialTheme.colorScheme.primary
    val outline    = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    fun fmtVal(v: Float) = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)

    val maxOffset = data.points.maxOfOrNull { it.dayOffset }?.coerceAtLeast(1) ?: 1
    val yRange    = (data.yMax - data.yMin).coerceAtLeast(0.001f)
    val midY      = (data.yMin + data.yMax) / 2f

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "↑  ${data.yAxisName}",
            style = labelStyle,
            color = labelColor,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        Row(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Column(
                modifier = Modifier.width(44.dp).fillMaxHeight().padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(fmtVal(data.yMax), style = labelStyle, color = labelColor, textAlign = TextAlign.End)
                if (data.yMin != data.yMax) {
                    Text(fmtVal(midY), style = labelStyle, color = labelColor, textAlign = TextAlign.End)
                }
                Text(fmtVal(data.yMin), style = labelStyle, color = labelColor, textAlign = TextAlign.End)
            }
            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val plotW = size.width
                val plotH = size.height
                fun toCanvasX(offset: Int) = offset.toFloat() / maxOffset.toFloat() * plotW
                fun toCanvasY(v: Float) = plotH - (v - data.yMin) / yRange * plotH

                for (frac in listOf(0.25f, 0.5f, 0.75f)) {
                    drawLine(
                        outline.copy(alpha = 0.3f),
                        Offset(0f, frac * plotH), Offset(plotW, frac * plotH),
                        strokeWidth = 0.5.dp.toPx()
                    )
                    drawLine(
                        outline.copy(alpha = 0.3f),
                        Offset(frac * plotW, 0f), Offset(frac * plotW, plotH),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
                val axisStroke = 1.5.dp.toPx()
                drawLine(outline, Offset(0f, 0f), Offset(0f, plotH), strokeWidth = axisStroke)
                drawLine(outline, Offset(0f, plotH), Offset(plotW, plotH), strokeWidth = axisStroke)
                val dotRadius = 4.5.dp.toPx()
                data.points.forEach { pt ->
                    drawCircle(primary.copy(alpha = 0.75f), dotRadius, Offset(toCanvasX(pt.dayOffset), toCanvasY(pt.value)))
                }
            }
        }
        if (data.points.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    data.points.minByOrNull { it.dayOffset }?.dateLabel ?: "",
                    style = labelStyle, color = labelColor
                )
                Text(
                    data.points.maxByOrNull { it.dayOffset }?.dateLabel ?: "",
                    style = labelStyle, color = labelColor
                )
            }
        }
        Text(
            text = "Time  →",
            style = labelStyle,
            color = labelColor,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
        )
    }
}

// ── Trends chart (value frequency progress bars) ──────────────────────────────

@Composable
fun TrendsChart(data: StatsChartData.TrendsData) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val baseOrdinalColor = if (data.colorToken.isNotEmpty() && data.valueOrders.isNotEmpty()) {
        data.colorToken.toCategoryColor()
    } else null
    val orderedLabels = data.valueOrders.entries.sortedBy { it.value }.map { it.key }

    fun barColor(label: String): Color {
        if (baseOrdinalColor == null) return primary
        val pos = orderedLabels.indexOf(label)
        return if (pos >= 0) ordinalShade(baseOrdinalColor, surface, pos, orderedLabels.size)
               else primary
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "${data.categoryName} — most common values",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        data.bars.forEach { bar ->
            val color = barColor(bar.label)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(bar.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${bar.count}× · ${bar.percentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress   = { bar.percentage / 100f },
                    modifier   = Modifier.fillMaxWidth().height(4.dp),
                    color      = color,
                    trackColor = color.copy(alpha = 0.15f),
                )
            }
        }
    }
}

// ── Weekday chart ─────────────────────────────────────────────────────────────────────

@Composable
fun WeekdayChart(data: StatsChartData.WeekdayData, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val maxBarValue = if (data.isNumeric)
        data.bars.mapNotNull { it.avgValue }.maxOrNull() ?: 1f
    else
        data.bars.maxOfOrNull { it.count.toFloat() } ?: 1f
    val clampedMax = maxBarValue.coerceAtLeast(0.001f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.bars.forEach { bar ->
                val displayValue = bar.avgValue ?: bar.count.toFloat()
                val fraction = (displayValue / clampedMax).coerceIn(0f, 1f)
                val topLabel = when {
                    bar.count == 0 -> ""
                    bar.avgValue != null -> "%.1f".format(bar.avgValue)
                    else -> bar.count.toString()
                }
                Column(
                    modifier = Modifier.width(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = topLabel,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(16.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(barColor)
                        )
                    }
                    Text(
                        text = bar.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// ── Time-of-day chart ─────────────────────────────────────────────────────────────────

@Composable
fun TimeOfDayChart(data: StatsChartData.TimeOfDayData, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val maxBarValue = if (data.isNumeric)
        data.bars.mapNotNull { it.avgValue }.maxOrNull() ?: 1f
    else
        data.bars.maxOfOrNull { it.count.toFloat() } ?: 1f
    val clampedMax = maxBarValue.coerceAtLeast(0.001f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            data.bars.forEach { bar ->
                val displayValue = bar.avgValue ?: bar.count.toFloat()
                val fraction = (displayValue / clampedMax).coerceIn(0f, 1f)
                val topLabel = when {
                    bar.count == 0 -> ""
                    bar.avgValue != null -> "%.1f".format(bar.avgValue)
                    else -> bar.count.toString()
                }
                Column(
                    modifier = Modifier.width(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = topLabel,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(16.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(barColor)
                        )
                    }
                    Text(
                        text = bar.hourLabel,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// ── Time correlation chart ────────────────────────────────────────────────────────────

@Composable
fun TimeCorrelationChart(data: StatsChartData.TimeCorrelationData, modifier: Modifier = Modifier) {
    val color1 = if (data.colorToken1.isNotEmpty()) data.colorToken1.toCategoryColor()
                 else MaterialTheme.colorScheme.primary
    val color2 = if (data.colorToken2.isNotEmpty()) data.colorToken2.toCategoryColor()
                 else MaterialTheme.colorScheme.tertiary
    val outline    = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = color1, label = data.cat1Name)
            LegendItem(color = color2, label = data.cat2Name)
        }

        Text(
            text = "Hour of day  ↑",
            style = labelStyle,
            color = labelColor,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            // Y-axis: 12am at top, 12am at bottom (full 24h)
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("12am", "6am", "12pm", "6pm", "12am").forEach { label ->
                    Text(label, style = labelStyle, color = labelColor, textAlign = TextAlign.End)
                }
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val w = size.width
                val h = size.height

                // Horizontal grid lines at 6, 12, 18 hours
                for (gridHour in listOf(6f, 12f, 18f)) {
                    val y = gridHour / 24f * h
                    drawLine(outline.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), 0.5.dp.toPx())
                }
                // Axes
                drawLine(outline, Offset(0f, 0f), Offset(0f, h), 1.5.dp.toPx())
                drawLine(outline, Offset(0f, h), Offset(w, h), 1.5.dp.toPx())

                val dotRadius = 4.dp.toPx()
                val td = data.totalDays.toFloat()

                fun toX(dayOffset: Int) = if (td <= 0f) w / 2f else dayOffset / td * w
                fun toY(hour: Float) = hour / 24f * h

                // Cat2 first so cat1 draws on top
                data.points2.forEach { pt ->
                    drawCircle(color2.copy(alpha = 0.65f), dotRadius, Offset(toX(pt.dayOffset), toY(pt.hourFraction)))
                }
                data.points1.forEach { pt ->
                    drawCircle(color1.copy(alpha = 0.8f), dotRadius, Offset(toX(pt.dayOffset), toY(pt.hourFraction)))
                }
            }
        }

        Text(
            text = "Time  →",
            style = labelStyle,
            color = labelColor,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 2.dp, start = 36.dp)
        )
    }
}

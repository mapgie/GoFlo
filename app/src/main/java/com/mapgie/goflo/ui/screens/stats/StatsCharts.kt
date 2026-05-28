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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val base = listOf(cs.primary, cs.secondary, cs.tertiary, cs.error)
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
    val colors = data.slices.mapIndexed { i, _ -> chartColor(i) }
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
 */
@Composable
fun BarChart(
    data: StatsChartData.TimeSeriesData,
    modifier: Modifier = Modifier
) {
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
                barColor = barColor
            )
        }
    }
}

// ── Dual bar chart (two time series) ─────────────────────────────────────────────────

/**
 * Vertical bar chart showing two categories side-by-side per time bucket.
 */
@Composable
fun DualBarChart(
    data: StatsChartData.DualTimeSeriesData,
    modifier: Modifier = Modifier
) {
    val maxCount = data.buckets
        .flatMap { listOf(it.count1, it.count2) }
        .maxOrNull()?.coerceAtLeast(1) ?: 1
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = color1, label = data.categoryName1)
            LegendItem(color = color2, label = data.categoryName2)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom
        ) {
            data.buckets.forEach { bucket ->
                Column(
                    modifier = Modifier.width(64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .height(160.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        DualBarSegment(count = bucket.count1, maxCount = maxCount, color = color1)
                        DualBarSegment(count = bucket.count2, maxCount = maxCount, color = color2)
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
private fun DualBarSegment(count: Int, maxCount: Int, color: Color) {
    val fraction = if (count == 0) 0f else count.toFloat() / maxCount.toFloat()
    Column(
        modifier = Modifier
            .width(22.dp)
            .height(160.dp),
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
private fun BarColumn(count: Int, maxCount: Int, label: String, barColor: Color) {
    val fraction = if (count == 0) 0f else count.toFloat() / maxCount.toFloat()
    Column(
        modifier = Modifier.width(52.dp),
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
fun NumericAverageChart(data: StatsChartData.NumericAverageData) {
    val primary = MaterialTheme.colorScheme.primary
    val rangeSpan = (data.globalMax - data.globalMin).coerceAtLeast(0.001f)

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
                    modifier = Modifier.width(52.dp),
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
 * Horizontal bar chart showing **how many times** each numeric value was logged.
 * Values are sorted numerically so the chart reads like a histogram.
 * Uses [MaterialTheme.colorScheme.secondary] to visually distinguish it from
 * the text-category [ComboBarChart].
 */
@Composable
fun NumericDistributionChart(data: StatsChartData.NumericDistributionData) {
    val secondary = MaterialTheme.colorScheme.secondary
    val maxCount = data.bars.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "${data.categoryName} — value distribution",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        data.bars.forEach { bar ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.widthIn(min = 40.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(bar.count.toFloat() / maxCount)
                            .clip(RoundedCornerShape(4.dp))
                            .background(secondary)
                    )
                }
                Text(
                    text = "${bar.count}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

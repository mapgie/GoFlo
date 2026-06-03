package com.mapgie.goflo.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.components.BetaFeedbackBanner
import com.mapgie.goflo.ui.screens.stats.BarChart
import com.mapgie.goflo.ui.screens.stats.ComboBarChart
import com.mapgie.goflo.ui.screens.stats.DualBarChart
import com.mapgie.goflo.ui.screens.stats.NumericAverageChart
import com.mapgie.goflo.ui.screens.stats.NumericDistributionChart
import com.mapgie.goflo.ui.screens.stats.PieChart
import com.mapgie.goflo.ui.screens.stats.PinnedStat
import com.mapgie.goflo.ui.screens.stats.ScatterPlot
import com.mapgie.goflo.ui.screens.stats.StatsChartData
import com.mapgie.goflo.ui.screens.stats.TimeScatterChart
import com.mapgie.goflo.ui.screens.stats.TrendsChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
) {
    val items by viewModel.items.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            BetaFeedbackBanner()
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No pinned views yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Go to Stats, set up a chart, and tap \"Pin this view\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.pin.id }) { item ->
                        PinnedChartCard(
                            item = item,
                            onRemove = { viewModel.removePin(item.pin) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedChartCard(
    item: PinnedChartItem,
    onRemove: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row: label (left) + delete button (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.pin.label,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = formatPinSubtitle(item.pin),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove pin",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (val data = item.chartData) {
                    null -> {
                        CircularProgressIndicator()
                    }

                    is StatsChartData.Empty -> {
                        Text(
                            "No data found for the selected range",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is StatsChartData.Loading -> {
                        CircularProgressIndicator()
                    }

                    is StatsChartData.PieData -> {
                        PieChart(
                            data = data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is StatsChartData.TimeSeriesData -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = data.categoryName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            BarChart(data = data)
                        }
                    }

                    is StatsChartData.ComboData -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Most common combinations",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            ComboBarChart(data = data)
                        }
                    }

                    is StatsChartData.DualTimeSeriesData -> {
                        DualBarChart(
                            data = data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is StatsChartData.NumericAverageData -> {
                        NumericAverageChart(data = data)
                    }

                    is StatsChartData.NumericDistributionData -> {
                        NumericDistributionChart(data = data)
                    }

                    is StatsChartData.ScatterData -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${data.yAxisName} vs ${data.xAxisName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            ScatterPlot(data = data)
                        }
                    }

                    is StatsChartData.TimeScatterData -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${data.yAxisName} over time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            TimeScatterChart(data = data)
                        }
                    }

                    is StatsChartData.TrendsData -> {
                        TrendsChart(data = data)
                    }

                    is StatsChartData.PhaseSummaryData -> {
                        // Phase summary is only shown in the Stats screen, not pinned to the dashboard
                    }
                }
            }
        }
    }
}

private fun formatPinSubtitle(pin: PinnedStat): String {
    val chartLabel = pin.chartType
        .lowercase()
        .split('_')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    val rangeLabel = when {
        pin.timeRangeType == "ALL_TIME"  -> "All time"
        pin.timeRangeType == "YTD"       -> "Year to date"
        pin.timeRangeType.startsWith("YEAR:")  -> pin.timeRangeType.removePrefix("YEAR:")
        pin.timeRangeType.startsWith("MONTH:") -> {
            val ym = pin.timeRangeType.removePrefix("MONTH:")
            try {
                val parts = ym.split("-")
                val month = java.time.Month.of(parts[1].toInt())
                "${month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())} ${parts[0]}"
            } catch (e: Exception) { ym }
        }
        else -> pin.timeRangeType
    }
    return "$chartLabel · $rangeLabel"
}

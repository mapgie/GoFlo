package com.mapgie.goflo.ui.screens.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.preferences.AppPreferencesStore
import com.mapgie.goflo.ui.components.BetaFeedbackBanner
import com.mapgie.goflo.ui.screens.stats.PinnedStat
import com.mapgie.goflo.ui.screens.stats.encodePins
import com.mapgie.goflo.ui.screens.stats.parsePins
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    preferencesStore: AppPreferencesStore,
    onNavigateToStats: (PinnedStat) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pinnedJson by preferencesStore.preferences
        .map { it.pinnedStats }
        .collectAsState(initial = "")

    val pins = remember(pinnedJson) { parsePins(pinnedJson) }

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
            if (pins.isEmpty()) {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pins, key = { it.id }) { pin ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToStats(pin) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = pin.label,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = formatPinSubtitle(pin),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        val updated = pins.filter { it.id != pin.id }
                                        preferencesStore.setPinnedStats(encodePins(updated))
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove pin",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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

package com.mapgie.goflo.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.components.CalendarGrid
import com.mapgie.goflo.ui.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val displayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GoFlo", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(Screen.LogPeriod.newEntry) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log Period") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CalendarGrid(
                periodDays      = state.periodDays,
                predictedDays   = state.predictedDays,
                ovulationDay    = state.ovulationDay,
                ovulationWindow = state.ovulationWindow,
                onDayClick = { date ->
                    val period = state.periods.firstOrNull { p ->
                        val start = LocalDate.parse(p.startDate)
                        val end = p.endDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
                        !date.isBefore(start) && !date.isAfter(end)
                    }
                    if (period != null) {
                        onNavigate(Screen.LogPeriod.withId(period.id))
                    } else {
                        onNavigate(Screen.LogPeriod.newEntryForDate(date))
                    }
                }
            )

            CycleInfoCard(state = state)
        }
    }
}

@Composable
private fun CycleInfoCard(state: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.activePeriod != null) {
                InfoRow("Status", "Period active — day ${state.cycleDay ?: "?"}")
            } else {
                state.cycleDay?.let { InfoRow("Cycle day", "Day $it of ~${state.avgCycleLength}") }
            }

            state.predictedNextPeriod?.let {
                InfoRow("Next period", it.format(displayFormat))
            }

            if (state.ovulationWindow.isNotEmpty()) {
                val windowStart = state.ovulationWindow.min()
                val windowEnd   = state.ovulationWindow.max()
                val shortFmt    = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                InfoRow("Ovulation window", "${windowStart.format(shortFmt)} – ${windowEnd.format(shortFmt)}")
            } else {
                state.ovulationDay?.let {
                    InfoRow("Ovulation day", it.format(displayFormat))
                }
            }

            InfoRow("Avg cycle", "${state.avgCycleLength} days")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

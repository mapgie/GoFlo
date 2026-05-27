package com.mapgie.goflo.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.mapgie.goflo.ui.components.BannerTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.components.CalendarGrid
import com.mapgie.goflo.ui.components.DayLogSheet
import com.mapgie.goflo.ui.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val displayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val dayLogData by viewModel.dayLogData.collectAsState()

    // FAB long-press menu sheet
    var showLogMenu by rememberSaveable { mutableStateOf(false) }
    // Date to use when the menu is opened from a long-press on a specific calendar day
    var logMenuTargetDate by remember { mutableStateOf<LocalDate?>(null) }

    val logMenuSheetState = rememberModalBottomSheetState()

    // ── Quick Log helper ──────────────────────────────────────────────────────

    fun handleQuickLog(date: LocalDate) {
        when {
            state.quickLogCategoryId == -1L ->
                onNavigate(Screen.LogPeriod.newEntryForDate(date))
            else ->
                onNavigate(Screen.LogCategory.newEntry(state.quickLogCategoryId, date))
        }
    }

    // ── Log menu (FAB long-press) bottom sheet ────────────────────────────────

    if (showLogMenu) {
        val targetDate = logMenuTargetDate ?: LocalDate.now()
        ModalBottomSheet(
            onDismissRequest = { showLogMenu = false; logMenuTargetDate = null },
            sheetState = logMenuSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "What would you like to log?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                // Log Period option
                LogMenuOption(
                    label = "Log Period",
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    onClick = {
                        showLogMenu = false
                        logMenuTargetDate = null
                        onNavigate(Screen.LogPeriod.newEntryForDate(targetDate))
                    }
                )

                // One option per tracking category — icon and colour follow the current theme
                state.trackingCategories.forEach { category ->
                    val bubbleColor  = category.colorToken.toCategoryColor()
                    val iconOnColor  = category.colorToken.toCategoryOnColor()
                    LogMenuOption(
                        label = "Log ${category.name}",
                        icon  = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(bubbleColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = category.iconName.toCategoryIcon().vector,
                                    contentDescription = null,
                                    tint               = iconOnColor,
                                    modifier           = Modifier.size(20.dp)
                                )
                            }
                        },
                        onClick = {
                            showLogMenu = false
                            logMenuTargetDate = null
                            onNavigate(Screen.LogCategory.newEntry(category.id, targetDate))
                        }
                    )
                }
            }
        }
    }

    // ── Day Log bottom sheet ──────────────────────────────────────────────────

    dayLogData?.let { data ->
        DayLogSheet(
            date = data.date,
            period = data.period,
            periodSymptoms = data.periodSymptomLabels,
            trackingLogs = data.trackingLogs,
            onDismiss = { viewModel.clearSelectedDay() },
            onEditPeriod = { periodId ->
                viewModel.clearSelectedDay()
                onNavigate(Screen.LogPeriod.withId(periodId))
            },
            onEditTrackingLog = { categoryId, logId ->
                viewModel.clearSelectedDay()
                onNavigate(Screen.LogCategory.editEntry(categoryId, logId))
            },
            onLogMore = {
                logMenuTargetDate = data.date
                showLogMenu = true
            }
        )
    }

    // ── Main scaffold ─────────────────────────────────────────────────────────

    Scaffold(
        topBar = { BannerTopBar(bannerStyle = state.bannerStyle) },
        floatingActionButton = {
            // Custom FAB using Surface (non-clickable variant) + combinedClickable so that
            // both short-press (Quick Log) and long-press (Log menu) work reliably without
            // conflicting gesture recognisers.
            LogFab(
                onClick = { handleQuickLog(LocalDate.now()) },
                onLongClick = { logMenuTargetDate = null; showLogMenu = true }
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
                periodDays           = state.periodDays,
                predictedDays        = state.predictedDays,
                ovulationDay         = state.ovulationDay,
                ovulationWindow      = state.ovulationWindow,
                daysWithTrackingLogs = state.trackingLogDates,
                onDayClick = { date ->
                    if (date in state.daysWithAnyData) {
                        // Show summary of what's logged for this day
                        viewModel.selectDay(date)
                    } else {
                        // Empty day: Quick Log
                        handleQuickLog(date)
                    }
                },
                onDayLongClick = { date ->
                    // Long-press always opens Quick Log for that day
                    handleQuickLog(date)
                }
            )

            CycleInfoCard(state = state)
        }
    }
}

/** Extended-FAB look-alike with separate short-press and long-press handlers. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogFab(onClick: () -> Unit, onLongClick: () -> Unit) {
    // Extended FAB shape in Material3 is a full-rounded pill (very large corner radius)
    val shape = RoundedCornerShape(50)
    Surface(
        modifier = Modifier
            .shadow(elevation = 6.dp, shape = shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
            Text("Log…", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LogMenuOption(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Text(label, style = MaterialTheme.typography.bodyLarge)
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

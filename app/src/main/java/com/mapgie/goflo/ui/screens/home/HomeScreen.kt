package com.mapgie.goflo.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.BuildConfig
import com.mapgie.goflo.ui.components.CalendarGrid
import com.mapgie.goflo.ui.components.DayLogSheet
import com.mapgie.goflo.ui.navigation.Screen
import com.mapgie.goflo.ui.screens.settings.ChangelogDialog
import com.mapgie.goflo.ui.theme.ComfortaaFamily
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor
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
    val dayLogData by viewModel.dayLogData.collectAsState()
    val quickLogMessage by viewModel.quickLogMessage.collectAsState()

    var showChangelog by rememberSaveable { mutableStateOf(false) }
    if (showChangelog) {
        ChangelogDialog(onDismiss = { showChangelog = false })
    }

    // Speed dial state
    var showLogMenu by rememberSaveable { mutableStateOf(false) }
    // Date to use when menu is opened from "Log more…" on a specific calendar day
    var logMenuTargetDate by remember { mutableStateOf<LocalDate?>(null) }

    // Confirmation snackbar for instant ("Plus One") quick logs
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(quickLogMessage) {
        quickLogMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(message = msg, actionLabel = "Undo")
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastIncrement()
            }
            viewModel.clearQuickLogMessage()
        }
    }

    // ── Quick Log helper ──────────────────────────────────────────────────────

    fun handleQuickLog(date: LocalDate) {
        val id = state.quickLogCategoryId
        val cat = state.trackingCategories.firstOrNull { it.id == id }
        when {
            id == -1L ->
                onNavigate(Screen.LogPeriod.newEntryForDate(date))
            cat?.categoryType == "increment" ->
                // Instantly add one for the tapped day; no screen navigation.
                viewModel.incrementCategory(id, date)
            else ->
                onNavigate(Screen.LogCategory.newEntry(id, date))
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
                viewModel.clearSelectedDay()
                logMenuTargetDate = data.date
                showLogMenu = true
            },
            flowCategoryName = data.flowCategoryName,
            symptomsCategoryName = data.symptomsCategoryName,
        )
    }

    // ── Main scaffold ─────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment     = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text  = "GoFlo",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = ComfortaaFamily
                            )
                        )
                        Text(
                            text     = "Beta",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                        Icon(
                            imageVector        = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val targetDate = logMenuTargetDate ?: LocalDate.now()
            SpeedDial(
                expanded = showLogMenu,
                onToggle = {
                    showLogMenu = !showLogMenu
                    if (!showLogMenu) logMenuTargetDate = null
                },
                onLogPeriod = {
                    showLogMenu = false
                    logMenuTargetDate = null
                    onNavigate(Screen.LogPeriod.newEntryForDate(targetDate))
                },
                categories = state.trackingCategories,
                onLogCategory = { categoryId ->
                    showLogMenu = false
                    logMenuTargetDate = null
                    onNavigate(Screen.LogCategory.newEntry(categoryId, targetDate))
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val showOnboardingBanner = !state.onboardingBannerDismissed &&
                    state.periods.isEmpty() && state.trackingLogDates.isEmpty()
                AnimatedVisibility(
                    visible = showOnboardingBanner,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut(),
                ) {
                    OnboardingBanner(onDismiss = { viewModel.dismissOnboardingBanner() })
                }

                CalendarGrid(
                    periodDays           = state.periodDays,
                    predictedDays        = state.predictedDays,
                    ovulationDay         = state.ovulationDay,
                    ovulationWindow      = state.ovulationWindow,
                    daysWithTrackingLogs = state.trackingLogDates,
                    onDayClick = { date ->
                        if (date in state.daysWithAnyData) {
                            viewModel.selectDay(date)
                        } else {
                            handleQuickLog(date)
                        }
                    },
                    onDayLongClick = { date -> handleQuickLog(date) }
                )

                CycleInfoCard(state = state)

                TextButton(
                    onClick  = { showChangelog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text  = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Scrim — closes speed dial when tapped outside it
            AnimatedVisibility(
                visible = showLogMenu,
                enter   = fadeIn(),
                exit    = fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = { showLogMenu = false; logMenuTargetDate = null }
                        )
                )
            }
        }
    }
}

// ── Speed dial ────────────────────────────────────────────────────────────────

@Composable
private fun SpeedDial(
    expanded: Boolean,
    onToggle: () -> Unit,
    onLogPeriod: () -> Unit,
    categories: List<com.mapgie.goflo.data.database.entities.TrackingCategory>,
    onLogCategory: (Long) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Expanded items
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit    = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                // Tracking categories (reversed so first category is nearest the FAB)
                categories.asReversed().forEach { category ->
                    SpeedDialItem(
                        label          = category.name,
                        containerColor = category.colorToken.toCategoryColor(),
                        contentColor   = category.colorToken.toCategoryOnColor(),
                        onClick        = { onLogCategory(category.id) }
                    ) {
                        Icon(
                            imageVector        = category.iconName.toCategoryIcon().vector,
                            contentDescription = null,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }

                // Log Period — always at the top of the list
                SpeedDialItem(
                    label          = "Log Period",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick        = onLogPeriod
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            }
        }

        // Main Extended FAB — icon-only when closed, expands to show "Log" when speed dial is open
        ExtendedFloatingActionButton(
            onClick          = onToggle,
            expanded         = expanded,
            containerColor   = MaterialTheme.colorScheme.primaryContainer,
            contentColor     = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = {
                AnimatedContent(targetState = expanded) { open ->
                    Icon(
                        imageVector        = if (open) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (open) "Close menu" else "Open log menu"
                    )
                }
            },
            text = {
                Text(
                    text       = "Log",
                    style      = MaterialTheme.typography.titleMedium,
                    fontFamily = ComfortaaFamily,
                )
            }
        )
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.padding(end = 4.dp)
    ) {
        Surface(
            shape         = RoundedCornerShape(8.dp),
            color         = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            tonalElevation  = 2.dp,
        ) {
            Text(
                text     = label,
                style    = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        SmallFloatingActionButton(
            onClick        = onClick,
            containerColor = containerColor,
            contentColor   = contentColor,
        ) {
            icon()
        }
    }
}

// ── Onboarding banner ─────────────────────────────────────────────────────────

@Composable
private fun OnboardingBanner(onDismiss: () -> Unit) {
    Surface(
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        color         = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier             = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text     = "Long-press any day to start logging. Add your own categories in the Manage tab.",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick  = onDismiss,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

// ── Cycle info card ───────────────────────────────────────────────────────────

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

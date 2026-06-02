package com.mapgie.goflo.ui.screens.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.ui.components.BetaFeedbackBanner
import com.mapgie.goflo.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val displayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigate: (String) -> Unit
) {
    val periods by viewModel.periods.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData     = data,
                    containerColor   = MaterialTheme.colorScheme.inverseSurface,
                    contentColor     = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor      = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            BetaFeedbackBanner()
        if (periods.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = padding.calculateBottomPadding())
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No periods logged yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap \"Log Period\" on the home screen to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                items(periods, key = { it.id }) { period ->
                    SwipeablePeriodCard(
                        period  = period,
                        onDelete = {
                            // Stage the deletion — card disappears from the list immediately.
                            viewModel.stageDeletion(period)
                            // Show snackbar with Undo action from the screen-level scope
                            // so the coroutine survives after the card composable is disposed.
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message     = "Period deleted",
                                    actionLabel = "Undo",
                                    duration    = SnackbarDuration.Long,
                                )
                                when (result) {
                                    SnackbarResult.ActionPerformed -> viewModel.undoDeletion(period)
                                    SnackbarResult.Dismissed       -> viewModel.commitDeletion(period)
                                }
                            }
                        },
                        onClick = { onNavigate(Screen.LogPeriod.withId(period.id)) },
                        modifier = Modifier,
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }
        }
    }
}

// ── Symptom Trends ────────────────────────────────────────────────────────────

// ── Swipeable period card ─────────────────────────────────────────────────────

/**
 * Wraps [PeriodCard] in a [SwipeToDismissBox] (right-to-left only).
 *
 * When the swipe crosses the threshold and the user releases:
 *  1. [confirmValueChange] returns `true` — the card slides off the screen.
 *  2. [LaunchedEffect] fires [onDelete], which (at the screen level) stages the
 *     deletion so the card disappears from the [LazyColumn] and shows an Undo
 *     [Snackbar]. The snackbar is launched on the screen-level [rememberCoroutineScope]
 *     so it survives this composable being disposed.
 *  3. If the user taps Undo the period reappears; otherwise the DB write happens
 *     after the snackbar times out.
 *
 * A confirmation dialog is shown before the deletion is committed. If the user
 * cancels, the swipe state is reset. After confirmation the Undo snackbar acts
 * as a second safety net.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePeriodCard(
    period: PeriodEntry,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }
    )

    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            showConfirm = true
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = {
                showConfirm = false
                scope.launch { state.reset() }
            },
            title = { Text("Delete period?") },
            text  = { Text("This will permanently remove this period entry. You can undo immediately after.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onDelete()
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        scope.launch { state.reset() }
                    }
                ) { Text("Cancel") }
            }
        )
    }

    SwipeToDismissBox(
        state                    = state,
        modifier                 = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent        = {
            val swipeActive = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val bgColor by animateColorAsState(
                targetValue = if (swipeActive) MaterialTheme.colorScheme.errorContainer
                              else Color.Transparent,
                label       = "swipe_bg"
            )
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (swipeActive) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint               = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
    ) {
        PeriodCard(period = period, onClick = onClick)
    }
}

// ── Period card ───────────────────────────────────────────────────────────────

@Composable
private fun PeriodCard(period: PeriodEntry, onClick: () -> Unit) {
    val start = LocalDate.parse(period.startDate)
    val end = period.endDate?.let { LocalDate.parse(it) }
    val duration = if (end != null) "${ChronoUnit.DAYS.between(start, end) + 1} days" else "Ongoing"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text  = start.format(displayFormat),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text  = period.flowLevel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text  = if (end != null) "Until ${end.format(displayFormat)}  ·  $duration" else "Ongoing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (period.notes.isNotBlank()) {
                Text(
                    text     = period.notes,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

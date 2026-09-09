package com.mapgie.goflo.ui.screens.log

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.ui.components.ChipRow
import com.mapgie.goflo.ui.components.DatePickerDialogWrapper
import com.mapgie.goflo.ui.components.HairlineDivider
import com.mapgie.goflo.ui.components.ListCard
import com.mapgie.goflo.ui.components.ListRow
import com.mapgie.goflo.ui.components.MetricConfig
import com.mapgie.goflo.ui.components.MetricInput
import com.mapgie.goflo.ui.components.MetricValue
import com.mapgie.goflo.ui.components.PrimarySaveBar
import com.mapgie.goflo.ui.components.SectionHeader
import com.mapgie.goflo.ui.components.SelectableChip
import com.mapgie.goflo.ui.components.SwitchRow
import com.mapgie.goflo.ui.components.ToneHero
import com.mapgie.goflo.ui.components.roleContainerTint
import com.mapgie.goflo.ui.components.usesStepScale
import com.mapgie.goflo.ui.util.CategoryType
import com.mapgie.goflo.ui.util.effectiveColorToken
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryOnColor
import com.mapgie.goflo.ui.util.toCategoryType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val displayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

/**
 * The unified day screen: one screen logs a day, and a running period is a
 * state of that day rather than a separate destination.
 *
 * Off-period, the first tracked category leads as a tonal hero and the footer
 * is a "Period started today" switch. On-period, the Flow group slots in at
 * the top, the lead category compresses into the tracked list, and the footer
 * becomes a filled status row with an End action. Everything between renders
 * identically in both states.
 *
 * Opened for one category (speed dial, widget, a day-sheet entry) the screen
 * shows only that category's input until "Log more for this day" expands it
 * to the whole day.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogScreen(
    viewModel: LogViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onBack()
    }

    var showDayPicker by rememberSaveable { mutableStateOf(false) }
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showRemoveDayConfirm by rememberSaveable { mutableStateOf(false) }
    var showAddSymptomDialog by rememberSaveable { mutableStateOf(false) }
    var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    /** Category id the re-file sheet was opened from, or 0 when closed. */
    var refileSourceId by rememberSaveable { mutableStateOf(0L) }
    /** Category id awaiting delete-entry confirmation, or 0 when none. */
    var pendingDeleteEntryId by rememberSaveable { mutableStateOf(0L) }
    /** Day picked while unsaved changes exist, awaiting discard confirmation. */
    var pendingDaySwitch by rememberSaveable { mutableStateOf<String?>(null) }

    val handleBack: () -> Unit = {
        if (state.hasChanges) showUnsavedChangesDialog = true else onBack()
    }
    BackHandler(enabled = state.hasChanges) { showUnsavedChangesDialog = true }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showDayPicker && !state.isLoading) {
        DatePickerDialogWrapper(
            initial = state.date,
            onConfirm = { picked ->
                showDayPicker = false
                if (picked != state.date) {
                    if (state.hasChanges) pendingDaySwitch = picked.toString()
                    else viewModel.setDate(picked)
                }
            },
            onDismiss = { showDayPicker = false },
        )
    }

    if (showStartPicker && !state.isLoading) {
        DatePickerDialogWrapper(
            initial = state.episodeStart ?: state.date,
            onConfirm = { viewModel.setStartDate(it); showStartPicker = false },
            onDismiss = { showStartPicker = false },
        )
    }

    if (showEndPicker && !state.isLoading) {
        DatePickerDialogWrapper(
            initial = state.endDate ?: state.date,
            minDate = state.episodeStart ?: state.date,
            onConfirm = { viewModel.setEndDate(it); showEndPicker = false },
            onDismiss = { showEndPicker = false },
        )
    }

    pendingDaySwitch?.let { pendingIso ->
        AlertDialog(
            onDismissRequest = { pendingDaySwitch = null },
            title = { Text("Switch day?") },
            text = { Text("This day has unsaved changes. Switching to another day discards them.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = runCatching { LocalDate.parse(pendingIso) }.getOrNull()
                        pendingDaySwitch = null
                        target?.let { viewModel.setDate(it) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Discard and switch") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDaySwitch = null }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteConfirm && !state.isLoading) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete period?") },
            text = { Text("This will permanently remove this entire period, including every logged day in it.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; viewModel.deleteEpisode() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showRemoveDayConfirm && !state.isLoading) {
        AlertDialog(
            onDismissRequest = { showRemoveDayConfirm = false },
            title = { Text("Remove this day?") },
            text = { Text(
                "${state.date.format(displayFormat)} will no longer count as a period day. " +
                "Anything else logged for this day is kept."
            ) },
            confirmButton = {
                TextButton(
                    onClick = { showRemoveDayConfirm = false; viewModel.removeDay() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Remove day") }
            },
            dismissButton = { TextButton(onClick = { showRemoveDayConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("Do you want to save this entry before going back?") },
            confirmButton = {
                Button(onClick = { showUnsavedChangesDialog = false; viewModel.save() }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnsavedChangesDialog = false; onBack() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Discard") }
            },
        )
    }

    if (showAddSymptomDialog) {
        AddSymptomDialog(
            existingLabels = state.symptomOptions.map { it.label },
            selectedLabels = state.symptoms,
            onAdd = { name ->
                viewModel.addNewSymptomToLibrary(name)
                showAddSymptomDialog = false
            },
            onDismiss = { showAddSymptomDialog = false },
        )
    }

    if (pendingDeleteEntryId != 0L) {
        val cat = state.categories.firstOrNull { it.id == pendingDeleteEntryId }
        AlertDialog(
            onDismissRequest = { pendingDeleteEntryId = 0L },
            title = { Text("Delete this entry?") },
            text = { Text(
                "The ${cat?.name ?: "category"} entry for " +
                "${state.date.format(displayFormat)} will be permanently removed."
            ) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(pendingDeleteEntryId)
                        pendingDeleteEntryId = 0L
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteEntryId = 0L }) { Text("Cancel") } },
        )
    }

    if (refileSourceId != 0L) {
        RefileSheet(
            sourceId = refileSourceId,
            state = state,
            onPickCategory = { categoryId ->
                val from = refileSourceId
                refileSourceId = 0L
                viewModel.refileEntry(from, categoryId)
            },
            onDismiss = { refileSourceId = 0L },
        )
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            LogDayTopBar(
                state = state,
                onBack = handleBack,
                onTitleClick = { showDayPicker = true },
                showOverflowMenu = showOverflowMenu,
                onOverflowChange = { showOverflowMenu = it },
                onDisablePeriodTracking = {
                    viewModel.disablePeriodTracking()
                    onBack()
                },
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val focused = state.focusedCategoryId?.let { id ->
            state.categories.firstOrNull { it.id == id }
        }

        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (focused != null) {
                    // Opened for one thing: just that thing, and a way out to
                    // the rest of the day.
                    CategoryMetricSection(
                        category = focused,
                        state = state,
                        viewModel = viewModel,
                        hero = true,
                        onSwitchCategory = { refileSourceId = focused.id },
                        onDeleteEntry = { pendingDeleteEntryId = focused.id },
                    )
                    OutlinedButton(
                        onClick = viewModel::showFullDay,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Log more for this day") }
                    ErrorText(state.error)
                    return@Column
                }

                if (state.periodActive) {
                    PeriodDatesSection(
                        state = state,
                        onPickStart = { showStartPicker = true },
                        onPickEnd = { showEndPicker = true },
                        onClearEnd = { viewModel.setEndDate(null) },
                    )
                    FlowSection(state, viewModel)
                }

                // Pinned ("Log with period") categories render in the flow
                // context while the day is on-period.
                val pinned = if (state.periodActive) {
                    state.categories.filter { it.showInLogPeriod }
                } else emptyList()
                pinned.forEach { cat ->
                    CategoryMetricSection(
                        category = cat,
                        state = state,
                        viewModel = viewModel,
                        onSwitchCategory = { refileSourceId = cat.id },
                        onDeleteEntry = { pendingDeleteEntryId = cat.id },
                    )
                }

                // Off-period the first tracked category leads as the hero.
                val lead = if (!state.periodActive) {
                    state.categories.firstOrNull()
                } else null
                lead?.let { cat ->
                    CategoryMetricSection(
                        category = cat,
                        state = state,
                        viewModel = viewModel,
                        hero = true,
                        onSwitchCategory = { refileSourceId = cat.id },
                        onDeleteEntry = { pendingDeleteEntryId = cat.id },
                    )
                }

                SymptomsSection(state, viewModel, onAddSymptom = { showAddSymptomDialog = true })

                TrackingSections(
                    state = state,
                    viewModel = viewModel,
                    excludeIds = (pinned.map { it.id } + listOfNotNull(lead?.id)).toSet(),
                    onSwitchCategory = { refileSourceId = it },
                    onDeleteEntry = { pendingDeleteEntryId = it },
                )

                if (state.periodActive) {
                    SectionHeader(label = "Notes", value = "Optional")
                    OutlinedTextField(
                        value = state.periodNotes,
                        onValueChange = { if (it.length <= 500) viewModel.setPeriodNotes(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("How are you feeling? Any other details…") },
                        minLines = 3,
                        maxLines = 6,
                        supportingText = { Text("${state.periodNotes.length}/500") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        ),
                    )
                }

                PeriodFooter(
                    state = state,
                    onStartPeriod = viewModel::startPeriodToday,
                    onUndoStart = viewModel::undoStartPeriod,
                    onEndPeriod = viewModel::endPeriodOnThisDay,
                    onUndoEnd = viewModel::undoEndPeriod,
                )

                if (state.isPeriodDay || (state.episodeId != null && state.dayInEpisode)) {
                    OutlinedButton(
                        onClick = { showRemoveDayConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Remove this day from period") }
                    if (state.episodeId != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) { Text("Delete Entire Period") }
                    }
                }

                ErrorText(state.error)
            }

            PrimarySaveBar(
                label = if (state.date == LocalDate.now()) "Save today" else "Save day",
                role = MaterialTheme.colorScheme.primary,
                onRole = MaterialTheme.colorScheme.onPrimary,
                onClick = viewModel::save,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error == null) return
    Text(
        text = "Error: $error",
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogDayTopBar(
    state: LogUiState,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    showOverflowMenu: Boolean,
    onOverflowChange: (Boolean) -> Unit,
    onDisablePeriodTracking: () -> Unit,
) {
    val subtitle = if (state.isLoading) null else buildString {
        append(state.date.format(displayFormat))
        val dayNo = state.episodeDayNumber
        if (state.periodActive && dayNo != null) append(" · period day $dayNo")
    }
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .semantics { this.role = Role.Button }
                    .clickable(onClick = onTitleClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Column {
                    Text(if (state.date == LocalDate.now()) "Log today" else "Log day")
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Change day",
                    modifier = Modifier.padding(start = 4.dp).size(20.dp),
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (state.periodTrackingEnabled) {
                IconButton(onClick = { onOverflowChange(true) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { onOverflowChange(false) },
                ) {
                    DropdownMenuItem(
                        text = { Text("Disable period logging") },
                        onClick = {
                            onOverflowChange(false)
                            onDisablePeriodTracking()
                        },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

// ── Period dates ──────────────────────────────────────────────────────────────

@Composable
private fun PeriodDatesSection(
    state: LogUiState,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onClearEnd: () -> Unit,
) {
    if (state.episodeId != null) {
        SectionHeader(label = "Period dates")
        ListCard {
            ListRow(
                key = "Started",
                value = (state.episodeStart ?: state.date).format(displayFormat),
                valueEmphasis = true,
                onClick = onPickStart,
            )
            HairlineDivider()
            ListRow(
                key = "Ended",
                value = state.endDate?.format(displayFormat) ?: "Still ongoing",
                valueEmphasis = true,
                valueColor = if (state.endDate == null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                onClick = onPickEnd,
            )
        }
        if (state.endDate != null) {
            TextButton(onClick = onClearEnd) { Text("Clear end date (leave open)") }
        }
    } else {
        SectionHeader(label = "End date", value = "Optional")
        ListCard {
            ListRow(
                key = "Ends",
                value = state.endDate?.let { "Until ${it.format(displayFormat)}" } ?: "No end date",
                valueEmphasis = state.endDate != null,
                onClick = onPickEnd,
            )
        }
        if (state.endDate != null) {
            TextButton(onClick = onClearEnd) { Text("Clear end date") }
        }
        Text(
            "Without an end date, the period ends on its own after " +
                "${state.toleranceDays + 1} days with no period day logged. " +
                "Log each day to record how it changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Flow ──────────────────────────────────────────────────────────────────────

@Composable
private fun FlowSection(state: LogUiState, viewModel: LogViewModel) {
    val flowCat = state.flowCategory ?: return
    if (flowCat.isArchived) return
    val token = flowCat.effectiveColorToken(state.groups)
    val role = token.toCategoryColor()
    val onRole = token.toCategoryOnColor()

    if (flowCat.categoryType == "numeric_slider") {
        val config = metricConfigFor(flowCat, emptyList())
        val current = state.flowSliderValue?.toInt()
        val word = current?.let { config.stepLabels[it] } ?: state.selectedFlowLabel
        SectionHeader(label = state.flowCategoryName, value = word, valueColor = role)
        MetricInput(
            type = CategoryType.NUMERIC_SLIDER,
            config = config,
            value = if (config.usesStepScale()) MetricValue.Scale(current)
                else MetricValue.Continuous(state.flowSliderValue),
            role = role,
            onRole = onRole,
            onChange = { v ->
                when (v) {
                    is MetricValue.Scale -> v.step?.let { viewModel.setFlowSliderValue(it.toFloat()) }
                    is MetricValue.Continuous -> v.value?.let { viewModel.setFlowSliderValue(it) }
                    else -> {}
                }
            },
        )
    } else {
        SectionHeader(
            label = state.flowCategoryName,
            value = state.selectedFlowLabel,
            valueColor = role,
        )
        if (state.flowOptions.isNotEmpty()) {
            ChipRow(
                options = state.flowOptions.map { it.label },
                selected = setOf(state.selectedFlowLabel),
                role = role,
                onToggle = { viewModel.setFlowLevel(it) },
            )
        } else {
            Text(
                "No flow levels configured. Add levels in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Symptoms ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomsSection(
    state: LogUiState,
    viewModel: LogViewModel,
    onAddSymptom: () -> Unit,
) {
    val symptomsCat = state.symptomsCategory ?: return
    if (symptomsCat.isArchived) return
    val token = symptomsCat.effectiveColorToken(state.groups)
    val role = token.toCategoryColor()

    SectionHeader(
        label = state.symptomsCategoryName,
        value = state.symptoms.size.takeIf { it > 0 }?.let { "$it today" },
        valueColor = role,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.symptomOptions.forEach { option ->
            SelectableChip(
                label = option.label,
                selected = option.label in state.symptoms,
                onClick = { viewModel.toggleSymptom(option.label) },
            )
        }
        AssistChip(
            onClick = onAddSymptom,
            label = { Text("Add") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add symptom",
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            border = AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            ),
        )
    }
}

// ── Tracked metric sections ───────────────────────────────────────────────────

/**
 * Renders every remaining tracked category, organised by group: a group of
 * two or more categories renders as one card of rows (tap a row to open its
 * input below the card); a group of one, and lone ungrouped categories,
 * render as their own always-open section.
 */
@Composable
private fun TrackingSections(
    state: LogUiState,
    viewModel: LogViewModel,
    excludeIds: Set<Long>,
    onSwitchCategory: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
) {
    val shown = state.categories.filter { it.id !in excludeIds }
    if (shown.isEmpty()) return

    val groupIds = state.groups.map { it.id }.toSet()
    val grouped = shown
        .filter { cat -> cat.groupId.let { it != null && it in groupIds } }
        .groupBy { it.groupId }
    val ungrouped = shown.filter { cat -> cat.groupId.let { it == null || it !in groupIds } }

    state.groups.forEach { group ->
        val members = grouped[group.id] ?: return@forEach
        if (members.size >= 2) {
            GroupCardSection(
                title = group.name,
                members = members,
                state = state,
                viewModel = viewModel,
                onSwitchCategory = onSwitchCategory,
                onDeleteEntry = onDeleteEntry,
            )
        } else {
            members.forEach { cat ->
                CategoryMetricSection(
                    category = cat,
                    state = state,
                    viewModel = viewModel,
                    onSwitchCategory = { onSwitchCategory(cat.id) },
                    onDeleteEntry = { onDeleteEntry(cat.id) },
                )
            }
        }
    }

    if (ungrouped.size >= 2) {
        GroupCardSection(
            title = "Tracking",
            members = ungrouped,
            state = state,
            viewModel = viewModel,
            onSwitchCategory = onSwitchCategory,
            onDeleteEntry = onDeleteEntry,
        )
    } else {
        ungrouped.forEach { cat ->
            CategoryMetricSection(
                category = cat,
                state = state,
                viewModel = viewModel,
                onSwitchCategory = { onSwitchCategory(cat.id) },
                onDeleteEntry = { onDeleteEntry(cat.id) },
            )
        }
    }
}

/** One card of rows for a multi-category group, plus the active row's input. */
@Composable
private fun GroupCardSection(
    title: String,
    members: List<TrackingCategory>,
    state: LogUiState,
    viewModel: LogViewModel,
    onSwitchCategory: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
) {
    SectionHeader(label = title, value = "${members.size} metrics")
    ListCard {
        members.forEachIndexed { index, cat ->
            val entry = state.entries[cat.id] ?: DayMetricEntry()
            val config = metricConfigFor(cat, state.categoryValues[cat.id] ?: emptyList())
            val summary = entrySummary(cat, entry, config)
            val token = cat.effectiveColorToken(state.groups)
            ListRow(
                key = cat.name,
                value = summary ?: "Add",
                valueEmphasis = summary != null,
                valueColor = if (summary != null) token.toCategoryColor()
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {
                    viewModel.setActiveCategory(
                        if (state.activeCategoryId == cat.id) null else cat.id
                    )
                },
            )
            if (index < members.lastIndex) HairlineDivider()
        }
    }
    members.firstOrNull { it.id == state.activeCategoryId }?.let { active ->
        CategoryMetricSection(
            category = active,
            state = state,
            viewModel = viewModel,
            onSwitchCategory = { onSwitchCategory(active.id) },
            onDeleteEntry = { onDeleteEntry(active.id) },
        )
    }
}

/**
 * One tracked category's full input surface: a plain section header with the
 * current reading, the [MetricInput] for its type (or the timed-increment
 * timeline), "previously recorded" chips for stored labels no longer in the
 * catalog, the track-against-time checkbox, per-entry notes, a "move to
 * another category" action once something has been entered (opens the re-file
 * sheet), and a delete action when an entry already exists. As the off-period
 * [hero], the input nests inside a [ToneHero] that shows the current reading
 * as words.
 */
@Composable
private fun CategoryMetricSection(
    category: TrackingCategory,
    state: LogUiState,
    viewModel: LogViewModel,
    onSwitchCategory: () -> Unit,
    onDeleteEntry: () -> Unit,
    hero: Boolean = false,
) {
    val entry = state.entries[category.id] ?: DayMetricEntry()
    val availableValues = state.categoryValues[category.id] ?: emptyList()
    val config = metricConfigFor(category, availableValues)
    val token = category.effectiveColorToken(state.groups)
    val role = token.toCategoryColor()
    val onRole = token.toCategoryOnColor()
    val summary = entrySummary(category, entry, config)

    if (hero) {
        SectionHeader(label = category.name)
        ToneHero(
            word = summary ?: "Not logged yet",
            role = role,
        ) {
            MetricSectionBody(
                category, entry, availableValues, config, role, onRole, viewModel,
                onSwitchCategory, onDeleteEntry,
            )
        }
    } else {
        SectionHeader(label = category.name, value = summary, valueColor = role)
        MetricSectionBody(
            category, entry, availableValues, config, role, onRole, viewModel,
            onSwitchCategory, onDeleteEntry,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricSectionBody(
    category: TrackingCategory,
    entry: DayMetricEntry,
    availableValues: List<String>,
    config: MetricConfig,
    role: Color,
    onRole: Color,
    viewModel: LogViewModel,
    onSwitchCategory: () -> Unit,
    onDeleteEntry: () -> Unit,
) {
    val type = category.categoryType.toCategoryType()
    val isTimedIncrement = type == CategoryType.INCREMENT && category.trackAgainstTime

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isTimedIncrement) {
            // Per-tap immediate saves with a timeline, exactly as the category
            // screen renders it.
            TimedIncrementTimeline(
                category = category,
                entries = entry.timedEntries,
                onAddOne = { viewModel.addTimedIncrement(category.id) },
                onDeleteEntry = { viewModel.deleteTimedEntry(category.id, it) },
            )
            return@Column
        }

        if (type == CategoryType.DEFAULT && availableValues.isEmpty()) {
            Text(
                "No values defined for this category yet. You can add values in " +
                    "Settings → Tracking Categories.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            MetricInput(
                type = type,
                config = config,
                value = metricValueForEntry(type, config, entry),
                role = role,
                onRole = onRole,
                onChange = { v ->
                    when (v) {
                        is MetricValue.Choice -> viewModel.setEntrySelection(category.id, v.selected)
                        is MetricValue.Scale ->
                            v.step?.let { viewModel.setEntryNumeric(category.id, it.toFloat()) }
                        is MetricValue.Continuous ->
                            v.value?.let { viewModel.setEntryNumeric(category.id, it) }
                        is MetricValue.FreeNumber -> viewModel.setEntryFreeText(category.id, v.text)
                        is MetricValue.Count ->
                            viewModel.setEntryNumeric(category.id, v.count.toFloat())
                        is MetricValue.YesNo -> v.value?.let {
                            viewModel.setEntrySelection(category.id, setOf(if (it) "Yes" else "No"))
                        }
                        is MetricValue.TimeOfDay -> v.time?.let {
                            viewModel.setEntrySelection(category.id, setOf(it))
                        }
                    }
                },
            )
        }

        // Stored labels no longer offered by the catalog stay visible and
        // deselectable, exactly as on the category screen.
        if (type == CategoryType.DEFAULT) {
            val removedValues = entry.selectedValues.filter { it !in availableValues }
            if (removedValues.isNotEmpty()) {
                Text(
                    "Previously recorded (removed from options):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    removedValues.forEach { label ->
                        SelectableChip(
                            label = "$label (removed)",
                            selected = true,
                            onClick = { viewModel.toggleEntryValue(category.id, label) },
                        )
                    }
                }
            }
        }

        if (category.trackAgainstTime) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = entry.trackTime,
                    onCheckedChange = { viewModel.setEntryTrackTime(category.id, it) },
                )
                Text("Track against time", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Per-entry notes: shown when the entry already carries notes, or on
        // demand, so a dozen categories never means a dozen empty text boxes.
        var noteOpen by rememberSaveable(category.id) { mutableStateOf(false) }
        if (entry.notes.isNotEmpty() || noteOpen) {
            OutlinedTextField(
                value = entry.notes,
                onValueChange = { if (it.length <= 500) viewModel.setEntryNotes(category.id, it) },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                supportingText = {
                    if (entry.notes.isNotEmpty()) Text("${entry.notes.length}/500")
                },
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entry.notes.isEmpty() && !noteOpen) {
                TextButton(onClick = { noteOpen = true }) { Text("Add note") }
            }
            // "Logged the wrong thing?": only an unsaved value can be
            // re-filed, so the action appears once something is entered.
            if (entry.touched) {
                TextButton(onClick = onSwitchCategory) { Text("Move to another category") }
            }
            if (entry.existingLog != null) {
                TextButton(
                    onClick = onDeleteEntry,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete entry") }
            }
        }
    }
}

// ── Period footer ─────────────────────────────────────────────────────────────

/**
 * Off-period, or while a start is pending: a switch that marks this day as a
 * period day (starting, continuing, or extending one), applied on save.
 * Once the day is a stored period day: a filled status row naming the period
 * state, with End/Undo.
 */
@Composable
private fun PeriodFooter(
    state: LogUiState,
    onStartPeriod: () -> Unit,
    onUndoStart: () -> Unit,
    onEndPeriod: () -> Unit,
    onUndoEnd: () -> Unit,
) {
    if (state.startPeriodToday || !state.periodActive) {
        if (!state.periodTrackingEnabled) return
        val continues = state.continuesEpisodeStart
        val pending = state.startPeriodToday
        val isToday = state.date == LocalDate.now()
        val title = when {
            continues != null -> "Log as a period day"
            isToday -> "Period started today"
            else -> "Period started this day"
        }
        val subtitle = when {
            continues != null && state.date.isBefore(continues) ->
                "Moves the start of the period from ${continues.format(displayFormat)} to this day"
            continues != null ->
                "Continues the period started ${continues.format(displayFormat)}"
            pending && state.endDate != null ->
                "Starts a new period, until ${state.endDate.format(displayFormat)}"
            pending -> "Starts a new period. Save to log it."
            else -> null
        }
        ListCard {
            SwitchRow(
                title = title,
                subtitle = subtitle,
                checked = pending,
                role = MaterialTheme.colorScheme.primary,
                onRole = MaterialTheme.colorScheme.onPrimary,
                onCheckedChange = { if (it) onStartPeriod() else onUndoStart() },
            )
        }
        return
    }

    val endedToday = state.endDate != null && state.endDate == state.date &&
        state.loadedEndDate != state.endDate
    val title = when {
        endedToday -> "Period ends today"
        state.endDate == null -> "Period ongoing"
        else -> "Period recorded"
    }
    val since = (state.episodeStart ?: state.date).format(displayFormat)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Since $since",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            when {
                endedToday -> TextButton(onClick = onUndoEnd) { Text("Undo") }
                state.endDate == null -> TextButton(onClick = onEndPeriod) { Text("End") }
            }
        }
    }
}

// ── Re-file sheet ─────────────────────────────────────────────────────────────

/**
 * "Logged the wrong thing?": every category organised by group and tinted by
 * its role. Picking one re-files the value entered under [sourceId] under the
 * picked category, keeping the value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefileSheet(
    sourceId: Long,
    state: LogUiState,
    onPickCategory: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val selectedId = sourceId

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "File this entry under…",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "The value you entered is kept; only the category it is filed under changes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val groupIds = state.groups.map { it.id }.toSet()
            val byGroup = state.categories
                .filter { cat -> cat.groupId.let { it != null && it in groupIds } }
                .groupBy { it.groupId }
            val ungrouped = state.categories
                .filter { cat -> cat.groupId.let { it == null || it !in groupIds } }

            state.groups.forEach { group ->
                val members = byGroup[group.id] ?: return@forEach
                SheetGroupLabel(name = group.name, token = group.colorRole)
                members.forEach { cat ->
                    SheetCategoryRow(
                        category = cat,
                        state = state,
                        selected = cat.id == selectedId,
                        onPick = { onPickCategory(cat.id) },
                    )
                }
            }
            if (ungrouped.isNotEmpty()) {
                if (byGroup.isNotEmpty()) SheetGroupLabel(name = "Other", token = null)
                ungrouped.forEach { cat ->
                    SheetCategoryRow(
                        category = cat,
                        state = state,
                        selected = cat.id == selectedId,
                        onPick = { onPickCategory(cat.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetGroupLabel(name: String, token: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        if (token != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(token.toCategoryColor()),
            )
        }
        Text(
            text = name.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.11.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SheetCategoryRow(
    category: TrackingCategory,
    state: LogUiState,
    selected: Boolean,
    onPick: () -> Unit,
) {
    val token = category.effectiveColorToken(state.groups)
    val roleColor = token.toCategoryColor()
    val container = if (selected) {
        roleContainerTint(roleColor, MaterialTheme.colorScheme.surface)
    } else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
            }
            .clickable(onClick = onPick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(roleColor),
        )
        Text(
            text = category.name,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Value mapping helpers ─────────────────────────────────────────────────────

/** Maps a [DayMetricEntry] onto the [MetricValue] variant [MetricInput] expects. */
private fun metricValueForEntry(
    type: CategoryType,
    config: MetricConfig,
    entry: DayMetricEntry,
): MetricValue = when (type) {
    CategoryType.DEFAULT -> MetricValue.Choice(entry.selectedValues)
    CategoryType.NUMERIC_SLIDER ->
        if (config.usesStepScale()) MetricValue.Scale(entry.numericValue?.toInt())
        else MetricValue.Continuous(entry.numericValue)
    CategoryType.NUMERIC_FREE -> MetricValue.FreeNumber(entry.freeText)
    CategoryType.INCREMENT -> MetricValue.Count(entry.numericValue?.toInt() ?: 0)
    CategoryType.YES_NO -> MetricValue.YesNo(
        when {
            "Yes" in entry.selectedValues -> true
            "No" in entry.selectedValues -> false
            else -> null
        }
    )
    CategoryType.TIME -> MetricValue.TimeOfDay(entry.selectedValues.firstOrNull())
}

/** Words for the current reading, or null when nothing is set for the day. */
private fun entrySummary(
    category: TrackingCategory,
    entry: DayMetricEntry,
    config: MetricConfig,
): String? {
    fun withUnit(text: String): String =
        if (config.unit.isNullOrBlank()) text else "$text ${config.unit}"

    val type = category.categoryType.toCategoryType()
    if (type == CategoryType.INCREMENT && category.trackAgainstTime) {
        val n = entry.timedEntries.size
        return if (n > 0) withUnit(n.toString()) else null
    }
    return when (type) {
        CategoryType.NUMERIC_SLIDER -> entry.numericValue?.let { v ->
            if (!category.allowDecimals) {
                config.stepLabels[v.toInt()] ?: withUnit(v.toInt().toString())
            } else {
                withUnit("%.1f".format(v))
            }
        }
        CategoryType.NUMERIC_FREE ->
            entry.freeText.trim().takeIf { it.isNotEmpty() }?.let { withUnit(it) }
        CategoryType.INCREMENT ->
            entry.numericValue?.toInt()?.takeIf { it > 0 }?.let { withUnit(it.toString()) }
        CategoryType.YES_NO -> when {
            "Yes" in entry.selectedValues -> "Yes"
            "No" in entry.selectedValues -> "No"
            else -> null
        }
        CategoryType.TIME -> entry.selectedValues.firstOrNull()
        CategoryType.DEFAULT -> when {
            entry.selectedValues.isEmpty() -> null
            entry.selectedValues.size > 2 -> "${entry.selectedValues.size} selected"
            else -> entry.selectedValues.joinToString(", ")
        }
    }
}


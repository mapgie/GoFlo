package com.mapgie.goflo.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.CustomAlarm
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoryValuesScreen(
    viewModel: ManageCategoryValuesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNewAlarm: () -> Unit = {},
    onNavigateToEditAlarm: (Long) -> Unit = {},
    onNavigateToEditCategory: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    var showAddValue              by rememberSaveable { mutableStateOf(false) }
    var showHelp                  by rememberSaveable { mutableStateOf(false) }
    var renamingValue             by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteValue        by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingArchiveCategory    by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteCategory     by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isLoading, state.category) {
        if (!state.isLoading && state.category == null) onNavigateBack()
    }

    val valueToRename = state.values.firstOrNull { it.id == renamingValue }
    val valueToDelete = state.values.firstOrNull { it.id == pendingDeleteValue }

    if (showHelp) {
        CategoriesHelpDialog(onDismiss = { showHelp = false })
    }

    if (showAddValue) {
        AddValueDialog(
            categoryName   = state.category?.name ?: "",
            existingLabels = state.values.map { it.label },
            onAdd = { label ->
                viewModel.addValue(label)
                showAddValue = false
            },
            onDismiss = { showAddValue = false }
        )
    }

    if (valueToRename != null) {
        RenameValueDialog(
            value = valueToRename,
            onRename = { newLabel, fixHistorical ->
                viewModel.renameValue(valueToRename, newLabel, fixHistorical)
                renamingValue = null
            },
            onDelete = if (valueToRename.isSeeded) null else ({
                pendingDeleteValue = valueToRename.id
                renamingValue = null
            }),
            onDismiss = { renamingValue = null }
        )
    }

    if (valueToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteValue = null },
            title = { Text("Remove \"${valueToDelete.label}\"?") },
            text = {
                Text(
                    "\"${valueToDelete.label}\" will no longer appear as an option for new " +
                    "entries. Past entries that used it are preserved and will be shown as a " +
                    "removed value."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteValue(valueToDelete)
                    pendingDeleteValue = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteValue = null }) { Text("Cancel") }
            }
        )
    }

    // ── Archive/Unarchive category dialog ────────────────────────────────────────

    if (pendingArchiveCategory && state.category != null) {
        val cat = state.category!!
        if (cat.isArchived) {
            AlertDialog(
                onDismissRequest = { pendingArchiveCategory = false },
                title = { Text("Unarchive \"${cat.name}\"?") },
                text = { Text("${cat.name} will be restored to your active tracking categories.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.unarchiveCategory()
                        pendingArchiveCategory = false
                    }) { Text("Unarchive") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingArchiveCategory = false }) { Text("Cancel") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { pendingArchiveCategory = false },
                title = { Text("Archive \"${cat.name}\"?") },
                text = {
                    Text(
                        "${cat.name} will be hidden from tracking but all your logged data will " +
                        "be preserved. You can unarchive it here at any time."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.archiveCategory()
                        pendingArchiveCategory = false
                    }) { Text("Archive") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingArchiveCategory = false }) { Text("Cancel") }
                }
            )
        }
    }

    // ── Delete category dialog ────────────────────────────────────────────────

    if (pendingDeleteCategory && state.category != null) {
        val cat = state.category!!
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = false },
            title = { Text("Delete \"${cat.name}\"?") },
            text = {
                Text(
                    "This will permanently remove the ${cat.name} category and all log entries " +
                    "recorded for it. If you want to keep a copy of your data, export it before " +
                    "continuing. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory()
                        pendingDeleteCategory = false
                    }
                ) { Text("Delete Everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.category?.name ?: "Category") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    // One Edit action opens the Phase 7 category edit flow, which
                    // covers rename and appearance (the two former dialog actions)
                    // plus type, switches, reminders, and the danger zone.
                    IconButton(onClick = onNavigateToEditCategory) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit category",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (state.category?.isSystem == false) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (state.category?.isArchived == true) "Unarchive"
                                            else "Archive"
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        pendingArchiveCategory = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (state.category?.isArchived == true) Icons.Default.Unarchive
                                            else Icons.Default.Archive,
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete category",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        pendingDeleteCategory = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Help",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showHelp = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val category = state.category
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                // Per-type numeric settings, the rename/appearance dialogs, and
                // the per-category switches were consolidated into the Edit
                // flow (CategoryEditScreen) in Phase 8. This screen keeps what
                // only it does: the value catalog and the Flow selector mode.
                when (category?.categoryType) {
                    "numeric_slider", "numeric_free", "increment" -> NumericCategoryInfo(
                        category           = category,
                        modifier           = Modifier,
                        onToggleFlowSlider = { viewModel.setFlowSliderMode(it) },
                    )
                    else -> DefaultCategoryValues(
                        state              = state,
                        modifier           = Modifier,
                        onAddValue         = { showAddValue = true },
                        onRenameValue      = { renamingValue = it.id },
                        onToggleFlowSlider = { viewModel.setFlowSliderMode(it) },
                    )
                }
            }
            CategoryAlarmsSection(
                alarms = state.alarms,
                onAddAlarm = onNavigateToNewAlarm,
                onEditAlarm = onNavigateToEditAlarm,
            )
        }
    }
}

// ── Category alarms section ───────────────────────────────────────────────────

@Composable
private fun CategoryAlarmsSection(
    alarms: List<CustomAlarm>,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
) {
    HorizontalDivider()
    ListItem(
        headlineContent = { Text("Alarms") },
        supportingContent = {
            if (alarms.isEmpty()) Text("No alarms for this category")
            else Text("${alarms.size} alarm${if (alarms.size == 1) "" else "s"}")
        },
        leadingContent = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onAddAlarm) {
                    Icon(Icons.Default.Add, contentDescription = "Add alarm for this category")
                }
                if (alarms.isNotEmpty()) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    )
    alarms.forEach { alarm ->
        ListItem(
            headlineContent = {
                Text(
                    "%02d:%02d%s".format(
                        alarm.hour, alarm.minute,
                        if (alarm.label.isNotBlank()) " - ${alarm.label}" else ""
                    )
                )
            },
            supportingContent = {
                Text(alarmScheduleLabel(alarm))
            },
            trailingContent = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier
                .semantics { role = Role.Button }
                .clickable { onEditAlarm(alarm.id) }
                .padding(start = 16.dp),
        )
    }
    HorizontalDivider()
}

internal fun alarmScheduleLabel(alarm: CustomAlarm): String {
    val schedule = when (alarm.scheduleType) {
        "DAILY" -> "Every day"
        "DURING_PERIOD" -> "During period"
        "NOT_DURING_PERIOD" -> "Outside period"
        "DAYS_BEFORE_PERIOD" -> "${alarm.daysOffset} day${if (alarm.daysOffset == 1) "" else "s"} before period"
        "DAYS_AFTER_PERIOD" -> "${alarm.daysOffset} day${if (alarm.daysOffset == 1) "" else "s"} after period starts"
        "DAY_OF_PERIOD" -> "Day ${alarm.dayOfPeriod} of period"
        else -> alarm.scheduleType
    }
    val mode = when (alarm.alarmType) {
        "ALARM" -> "Alarm"
        "SILENT" -> "Silent"
        else -> "Notification"
    }
    return "$schedule · $mode"
}

// ── Default (text values) content ─────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefaultCategoryValues(
    state: ManageCategoryValuesUiState,
    modifier: Modifier,
    onAddValue: () -> Unit,
    onRenameValue: (TrackingValue) -> Unit,
    onToggleFlowSlider: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val category = state.category
        if (category?.systemKey == "flow") {
            FlowSliderRow(isSlider = false, onToggle = onToggleFlowSlider)
            HorizontalDivider()
        }

        Text(
            "Values in this category",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.values.forEach { value ->
                InputChip(
                    selected = false,
                    onClick = { onRenameValue(value) },
                    label = { Text(value.label) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit ${value.label}",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            AssistChip(
                onClick = onAddValue,
                label = { Text("Add") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add value",
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                },
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )
        }

        HorizontalDivider()
        EditFlowHint()
    }
}

// ── Numeric family (slider / input / Plus One) info ──────────────────────────
// The range, step labels, unit, and per-category switches these sections used
// to edit in place moved to the Edit flow (CategoryEditScreen) in Phase 8.

@Composable
private fun NumericCategoryInfo(
    category: TrackingCategory,
    modifier: Modifier,
    onToggleFlowSlider: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (category.systemKey == "flow") {
            FlowSliderRow(isSlider = category.categoryType == "numeric_slider", onToggle = onToggleFlowSlider)
            HorizontalDivider()
        }

        when (category.categoryType) {
            "increment" -> {
                Text(
                    "Plus One category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Plus One categories don't use predefined values. Each log records a " +
                    "running count for the day: use the + button on the home screen or the " +
                    "day screen to add to today's total.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            "numeric_slider" -> {
                Text(
                    "Slider scale category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "This category logs a position on a stepped scale, so it has no value " +
                    "list to manage. The range, step labels, and unit live in the " +
                    "category's settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> {
                Text(
                    "Numeric input category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "This category logs a typed number, so it has no value list to manage. " +
                    "The unit lives in the category's settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        EditFlowHint()
    }
}

/** Points at the Edit action, where everything this screen no longer edits lives. */
@Composable
private fun EditFlowHint() {
    Text(
        "Use the Edit action in the top bar to change this category's name, icon, " +
        "colour, input settings, reminders, and options.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ── Flow selector mode toggle ─────────────────────────────────────────────────
// Only this screen offers the built-in Flow category's chips/slider switch.

@Composable
private fun FlowSliderRow(isSlider: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Use slider", style = MaterialTheme.typography.titleSmall)
            Text(
                "Replace the Spotting/Light/Medium/Heavy selector with a numeric slider. Stats will treat each log as a number.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = isSlider, onCheckedChange = onToggle)
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun AddValueDialog(
    categoryName: String,
    existingLabels: List<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    val trimmed = text.trim()
    val alreadyExists = existingLabels.any { it.equals(trimmed, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add value to $categoryName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    label         = { Text("Value") },
                    placeholder   = { Text("e.g. Happy, Calm, Tired…") },
                    singleLine    = true,
                    isError       = alreadyExists,
                    supportingText = if (alreadyExists) ({ Text("Already exists") }) else null,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (trimmed.isNotBlank() && !alreadyExists) onAdd(trimmed) },
                enabled  = trimmed.isNotBlank() && !alreadyExists
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RenameValueDialog(
    value: TrackingValue,
    onRename: (String, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var newLabel by rememberSaveable { mutableStateOf(value.label) }
    val trimmed = newLabel.trim()
    val changed = trimmed.isNotBlank() && trimmed != value.label

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit \"${value.label}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = newLabel,
                    onValueChange = { newLabel = it },
                    label         = { Text("New label") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                if (changed) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "How should past entries be handled?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (changed) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = { onRename(trimmed, true) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Fix everywhere")
                    }
                    TextButton(onClick = { onRename(trimmed, false) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Rename option only")
                    }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (changed) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            } else if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

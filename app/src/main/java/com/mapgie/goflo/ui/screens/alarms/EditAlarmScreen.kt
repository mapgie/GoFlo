package com.mapgie.goflo.ui.screens.alarms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

private val SNOOZE_OPTIONS = listOf(5, 10, 15, 30, 60)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditAlarmScreen(
    viewModel: EditAlarmViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    when (state.step) {
        EditAlarmStep.PICK_TIME -> TimePickerStep(
            initialHour = state.hour,
            initialMinute = state.minute,
            onBack = onNavigateBack,
            onConfirm = { h, m -> viewModel.confirmTime(h, m) },
        )

        EditAlarmStep.CONFIGURE -> ConfigureStep(
            state = state,
            onBack = { viewModel.goBackToTimePicker() },
            onSetLabel = viewModel::setLabel,
            onSetAlarmType = viewModel::setAlarmType,
            onSetOverrideDnd = viewModel::setOverrideDnd,
            onSetRecurring = viewModel::setRecurring,
            onSetScheduleType = viewModel::setScheduleType,
            onSetDaysOffset = viewModel::setDaysOffset,
            onSetDayOfPeriod = viewModel::setDayOfPeriod,
            onSetSnoozeDuration = viewModel::setSnoozeDuration,
            onToggleCategory = viewModel::toggleCategory,
            onSave = { viewModel.save() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerStep(
    initialHour: Int,
    initialMinute: Int,
    onBack: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val timeState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Time") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            TimePicker(state = timeState)
            Button(
                onClick = { onConfirm(timeState.hour, timeState.minute) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Next")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ConfigureStep(
    state: EditAlarmUiState,
    onBack: () -> Unit,
    onSetLabel: (String) -> Unit,
    onSetAlarmType: (String) -> Unit,
    onSetOverrideDnd: (Boolean) -> Unit,
    onSetRecurring: (Boolean) -> Unit,
    onSetScheduleType: (String) -> Unit,
    onSetDaysOffset: (Int) -> Unit,
    onSetDayOfPeriod: (Int) -> Unit,
    onSetSnoozeDuration: (Int) -> Unit,
    onToggleCategory: (Long) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Alarm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Time summary
            ListItem(
                headlineContent = {
                    Text(
                        "%02d:%02d".format(state.hour, state.minute),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                supportingContent = { Text("Tap Back to change the time") },
            )

            HorizontalDivider()

            // Label
            ListItem(
                headlineContent = { Text("Label") },
                supportingContent = {
                    OutlinedTextField(
                        value = state.label,
                        onValueChange = onSetLabel,
                        placeholder = { Text("Optional name for this alarm") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )

            HorizontalDivider()

            // Delivery mode
            ListItem(
                headlineContent = { Text("Delivery") },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            when (state.alarmType) {
                                "ALARM" -> "Full-screen alarm with sound. Requires the Alarms and reminders permission."
                                "SILENT" -> "Full-screen interrupt with no sound or vibration. Requires dismiss or snooze."
                                else -> "Standard notification with Log and Snooze actions."
                            }
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("NOTIFICATION" to "Notification", "ALARM" to "Alarm", "SILENT" to "Silent")
                                .forEachIndexed { index, (value, label) ->
                                    SegmentedButton(
                                        selected = state.alarmType == value,
                                        onClick = { onSetAlarmType(value) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                    ) { Text(label) }
                                }
                        }
                    }
                },
            )

            if (state.alarmType != "SILENT") {
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Override Do Not Disturb") },
                    supportingContent = { Text("This alarm will sound even when DND is active") },
                    trailingContent = {
                        Switch(checked = state.overrideDnd, onCheckedChange = null)
                    },
                    modifier = Modifier
                        .semantics { role = Role.Switch }
                        .clickable { onSetOverrideDnd(!state.overrideDnd) },
                )
            }

            HorizontalDivider()

            // Snooze duration
            ListItem(
                headlineContent = { Text("Snooze duration") },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("How long to delay after snoozing")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SNOOZE_OPTIONS.forEachIndexed { index, minutes ->
                                SegmentedButton(
                                    selected = state.snoozeDurationMinutes == minutes,
                                    onClick = { onSetSnoozeDuration(minutes) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index, count = SNOOZE_OPTIONS.size
                                    ),
                                ) { Text("${minutes}m") }
                            }
                        }
                    }
                },
            )

            HorizontalDivider()

            // Recurring
            ListItem(
                headlineContent = { Text("Recurring") },
                supportingContent = {
                    Text(if (state.isRecurring) "Fires every day at the set time" else "Fires once, then stops")
                },
                trailingContent = {
                    Switch(checked = state.isRecurring, onCheckedChange = null)
                },
                modifier = Modifier
                    .semantics { role = Role.Switch }
                    .clickable { onSetRecurring(!state.isRecurring) },
            )

            if (state.isRecurring) {
                HorizontalDivider()

                // Schedule type
                ListItem(
                    headlineContent = { Text("Schedule") },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("When should this alarm fire?")
                            val scheduleTypes = listOf(
                                "DAILY" to "Every day",
                                "DURING_PERIOD" to "During period",
                                "NOT_DURING_PERIOD" to "Outside period",
                                "DAYS_BEFORE_PERIOD" to "Before period",
                                "DAYS_AFTER_PERIOD" to "After period starts",
                                "DAY_OF_PERIOD" to "Day of period",
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                scheduleTypes.forEach { (value, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics { role = Role.RadioButton }
                                            .clickable { onSetScheduleType(value) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        androidx.compose.material3.RadioButton(
                                            selected = state.scheduleType == value,
                                            onClick = null,
                                        )
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            when (state.scheduleType) {
                                "DAYS_BEFORE_PERIOD" -> {
                                    Text(
                                        "${state.daysOffset} day${if (state.daysOffset == 1) "" else "s"} before predicted period",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Slider(
                                        value = state.daysOffset.toFloat(),
                                        onValueChange = { onSetDaysOffset(it.toInt()) },
                                        valueRange = 1f..14f,
                                        steps = 12,
                                    )
                                }
                                "DAYS_AFTER_PERIOD" -> {
                                    Text(
                                        "${state.daysOffset} day${if (state.daysOffset == 1) "" else "s"} after period starts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Slider(
                                        value = state.daysOffset.toFloat(),
                                        onValueChange = { onSetDaysOffset(it.toInt()) },
                                        valueRange = 1f..14f,
                                        steps = 12,
                                    )
                                }
                                "DAY_OF_PERIOD" -> {
                                    Text(
                                        "Day ${state.dayOfPeriod} of period",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Slider(
                                        value = state.dayOfPeriod.toFloat(),
                                        onValueChange = { onSetDayOfPeriod(it.toInt()) },
                                        valueRange = 1f..14f,
                                        steps = 12,
                                    )
                                }
                            }
                        }
                    },
                )
            }

            HorizontalDivider()

            // Category association
            if (state.allCategories.isNotEmpty()) {
                ListItem(
                    headlineContent = { Text("Tracking reminders") },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select categories to log when this alarm fires")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                state.allCategories.forEach { category ->
                                    val selected = category.id in state.selectedCategoryIds
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onToggleCategory(category.id) },
                                        label = { Text(category.name) },
                                        modifier = Modifier.semantics { role = Role.Checkbox },
                                    )
                                }
                            }
                        }
                    },
                )
                HorizontalDivider()
            }

            // Save button
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !state.isLoading,
            ) {
                Text("Save alarm")
            }
        }
    }
}

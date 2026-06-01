package com.mapgie.goflo.ui.screens.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.screens.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val prefs by viewModel.prefs.collectAsState()
    val reminder = prefs.reminderSettings

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val timeState = rememberTimePickerState(
        initialHour   = reminder.reminderHour,
        initialMinute = reminder.reminderMinute
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.setReminderTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timeState) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent   = { Text("Before period alert") },
                supportingContent = { Text("Notify ${reminder.preperiodDaysBefore} day(s) before predicted start") },
                trailingContent   = { Switch(checked = reminder.preperiodEnabled, onCheckedChange = null) },
                modifier          = Modifier
                    .clickable { viewModel.setPreperiodEnabled(!reminder.preperiodEnabled) }
                    .semantics { role = Role.Switch }
            )

            if (reminder.preperiodEnabled) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("Days before: ${reminder.preperiodDaysBefore}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value         = reminder.preperiodDaysBefore.toFloat(),
                        onValueChange = { viewModel.setPreperiodDays(it.toInt()) },
                        valueRange    = 1f..7f,
                        steps         = 5
                    )
                }
            }

            HorizontalDivider()

            ListItem(
                headlineContent   = { Text("Ovulation window") },
                supportingContent = { Text("Notify around mid-cycle") },
                trailingContent   = { Switch(checked = reminder.ovulationEnabled, onCheckedChange = null) },
                modifier          = Modifier
                    .clickable { viewModel.setOvulationEnabled(!reminder.ovulationEnabled) }
                    .semantics { role = Role.Switch }
            )

            HorizontalDivider()

            ListItem(
                headlineContent   = { Text("Daily log reminder") },
                supportingContent = { Text("Remind to log while period is active") },
                trailingContent   = { Switch(checked = reminder.dailyDuringPeriodEnabled, onCheckedChange = null) },
                modifier          = Modifier
                    .clickable { viewModel.setDailyEnabled(!reminder.dailyDuringPeriodEnabled) }
                    .semantics { role = Role.Switch }
            )

            val anyEnabled = reminder.preperiodEnabled || reminder.ovulationEnabled || reminder.dailyDuringPeriodEnabled
            if (anyEnabled) {
                HorizontalDivider()
                ListItem(
                    headlineContent   = { Text("Reminder time") },
                    supportingContent = {
                        Text(
                            "%02d:%02d".format(reminder.reminderHour, reminder.reminderMinute),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent   = {
                        OutlinedButton(onClick = { showTimePicker = true }) { Text("Change") }
                    }
                )
            }
        }
    }
}

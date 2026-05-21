package com.mapgie.goflo.ui.screens.settings

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.components.SelectableChip
import com.mapgie.goflo.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val prefs by viewModel.prefs.collectAsState()
    val reminder = prefs.reminder
    val currentTheme = runCatching { AppTheme.valueOf(prefs.theme) }.getOrDefault(AppTheme.CORAL)

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val timeState = rememberTimePickerState(initialHour = reminder.reminderHour, initialMinute = reminder.reminderMinute)

    if (showTimePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.setReminderTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme section
            SettingSection(title = "Theme") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectableChip(
                        label = "Coral",
                        selected = currentTheme == AppTheme.CORAL,
                        onClick = { viewModel.setTheme(AppTheme.CORAL.name) }
                    )
                    SelectableChip(
                        label = "Turquoise",
                        selected = currentTheme == AppTheme.TURQUOISE,
                        onClick = { viewModel.setTheme(AppTheme.TURQUOISE.name) }
                    )
                    SelectableChip(
                        label = "Green",
                        selected = currentTheme == AppTheme.GREEN,
                        onClick = { viewModel.setTheme(AppTheme.GREEN.name) }
                    )
                }
            }

            HorizontalDivider()

            // Reminders section
            SettingSection(title = "Reminders") {
                // Pre-period
                SwitchRow(
                    label = "Before period alert",
                    subtitle = "Notify ${reminder.preperiodDaysBefore} day(s) before predicted start",
                    checked = reminder.preperiodEnabled,
                    onCheckedChange = viewModel::setPreperiodEnabled
                )
                if (reminder.preperiodEnabled) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            "Days before: ${reminder.preperiodDaysBefore}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = reminder.preperiodDaysBefore.toFloat(),
                            onValueChange = { viewModel.setPreperiodDays(it.toInt()) },
                            valueRange = 1f..7f,
                            steps = 5
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Ovulation
                SwitchRow(
                    label = "Ovulation window",
                    subtitle = "Notify around mid-cycle",
                    checked = reminder.ovulationEnabled,
                    onCheckedChange = viewModel::setOvulationEnabled
                )

                Spacer(Modifier.height(4.dp))

                // Daily during period
                SwitchRow(
                    label = "Daily log reminder",
                    subtitle = "Remind to log symptoms while period is active",
                    checked = reminder.dailyDuringPeriodEnabled,
                    onCheckedChange = viewModel::setDailyEnabled
                )

                Spacer(Modifier.height(8.dp))

                val anyEnabled = reminder.preperiodEnabled || reminder.ovulationEnabled || reminder.dailyDuringPeriodEnabled
                if (anyEnabled) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Reminder time", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "%02d:%02d".format(reminder.reminderHour, reminder.reminderMinute),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        androidx.compose.material3.OutlinedButton(onClick = { showTimePicker = true }) {
                            Text("Change")
                        }
                    }
                }
            }

            HorizontalDivider()

            // About section
            SettingSection(title = "About") {
                Text("GoFlo v1.0", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "All your data stays on your device — nothing is sent anywhere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

package com.mapgie.goflo.ui.screens.manage

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.screens.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val prefs by viewModel.prefs.collectAsState()
    val reminder = prefs.reminder

    val context = LocalContext.current
    val canScheduleExact = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else true
    }

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
            // Alarm permission warning (only relevant when ALARM mode is active)
            if (reminder.deliveryMode == "ALARM" && !canScheduleExact) {
                ListItem(
                    headlineContent = { Text("Exact alarms require a permission") },
                    supportingContent = { Text("Tap to grant the Alarms and reminders permission in Settings") },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.NotificationImportant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        headlineColor  = MaterialTheme.colorScheme.onErrorContainer,
                        supportingColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier
                        .clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                        .semantics { role = Role.Button }
                )
                HorizontalDivider()
            }

            // Delivery mode picker
            ListItem(
                headlineContent = { Text("Delivery mode") },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            when (reminder.deliveryMode) {
                                "ALARM"  -> "Full-screen alarm. Requires the Alarms and reminders permission."
                                "SILENT" -> "Silent notification. No sound or vibration. Appears in the shade only."
                                else     -> "Standard notification with sound."
                            }
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = reminder.deliveryMode == "NOTIFICATION",
                                onClick  = { viewModel.setReminderDeliveryMode("NOTIFICATION") },
                                shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            ) { Text("Notification") }
                            SegmentedButton(
                                selected = reminder.deliveryMode == "SILENT",
                                onClick  = { viewModel.setReminderDeliveryMode("SILENT") },
                                shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            ) { Text("Silent") }
                            SegmentedButton(
                                selected = reminder.deliveryMode == "ALARM",
                                onClick  = { viewModel.setReminderDeliveryMode("ALARM") },
                                shape    = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            ) { Text("Alarm") }
                        }
                    }
                }
            )

            if (reminder.deliveryMode == "ALARM") {
                HorizontalDivider()
                var labelText by rememberSaveable(reminder.alarmLabel) { mutableStateOf(reminder.alarmLabel) }
                ListItem(
                    headlineContent = { Text("Alarm name") },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("This text appears on the alarm screen when it fires.")
                            OutlinedTextField(
                                value = labelText,
                                onValueChange = { labelText = it },
                                placeholder = { Text("e.g. GoFlo reminder") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (labelText != reminder.alarmLabel) {
                                TextButton(onClick = { viewModel.setAlarmLabel(labelText) }) {
                                    Text("Apply")
                                }
                            }
                        }
                    }
                )
            }

            HorizontalDivider()

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

            HorizontalDivider()

            ListItem(
                headlineContent   = { Text("Daily check-ins") },
                supportingContent = { Text("Get a gentle reminder if a predicted period has passed or daily tracking is due") },
                trailingContent   = { Switch(checked = prefs.dailyCheckEnabled, onCheckedChange = null) },
                modifier          = Modifier
                    .semantics {
                        role = Role.Switch
                        stateDescription = if (prefs.dailyCheckEnabled) "On" else "Off"
                    }
                    .clickable { viewModel.setDailyCheckEnabled(!prefs.dailyCheckEnabled) }
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

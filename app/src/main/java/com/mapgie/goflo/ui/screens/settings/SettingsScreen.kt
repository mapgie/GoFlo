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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.preferences.hasPinSet
import com.mapgie.goflo.ui.components.SelectableChip
import com.mapgie.goflo.ui.screens.disclaimer.DisclaimerScreen
import com.mapgie.goflo.BuildConfig
import com.mapgie.goflo.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToPinSetup: (changing: Boolean) -> Unit,
    onNavigateToLicenses: () -> Unit
) {
    val prefs by viewModel.prefs.collectAsState()
    val security by viewModel.securitySettings.collectAsState()
    val reminder = prefs.reminder
    val currentTheme = runCatching { AppTheme.valueOf(prefs.theme) }.getOrDefault(AppTheme.CORAL)

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showRemovePinDialog by rememberSaveable { mutableStateOf(false) }
    var showDisclaimer by rememberSaveable { mutableStateOf(false) }
    var removePinInput by rememberSaveable { mutableStateOf("") }
    var removePinError by rememberSaveable { mutableStateOf(false) }

    val timeState = rememberTimePickerState(
        initialHour = reminder.reminderHour,
        initialMinute = reminder.reminderMinute
    )

    if (showDisclaimer) {
        DisclaimerScreen(onAcknowledge = { showDisclaimer = false })
        return
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timeState) }
        )
    }

    if (showRemovePinDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePinDialog = false; removePinInput = ""; removePinError = false },
            title = { Text("Remove PIN lock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your current PIN to confirm.")
                    OutlinedTextField(
                        value = removePinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { removePinInput = it; removePinError = false } },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = removePinError,
                        supportingText = if (removePinError) ({ Text("Incorrect PIN") }) else null
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removePin(removePinInput) { success ->
                            if (success) {
                                showRemovePinDialog = false
                                removePinInput = ""
                                removePinError = false
                            } else {
                                removePinError = true
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinDialog = false; removePinInput = ""; removePinError = false }) {
                    Text("Cancel")
                }
            }
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
            // ── Theme ──────────────────────────────────────────────────────────
            SettingSection(title = "Theme") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectableChip("Coral", currentTheme == AppTheme.CORAL) { viewModel.setTheme(AppTheme.CORAL.name) }
                    SelectableChip("Turquoise", currentTheme == AppTheme.TURQUOISE) { viewModel.setTheme(AppTheme.TURQUOISE.name) }
                    SelectableChip("Green", currentTheme == AppTheme.GREEN) { viewModel.setTheme(AppTheme.GREEN.name) }
                }
            }

            HorizontalDivider()

            // ── Reminders ──────────────────────────────────────────────────────
            SettingSection(title = "Reminders") {
                SwitchRow(
                    label = "Before period alert",
                    subtitle = "Notify ${reminder.preperiodDaysBefore} day(s) before predicted start",
                    checked = reminder.preperiodEnabled,
                    onCheckedChange = viewModel::setPreperiodEnabled
                )
                if (reminder.preperiodEnabled) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Days before: ${reminder.preperiodDaysBefore}", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = reminder.preperiodDaysBefore.toFloat(),
                            onValueChange = { viewModel.setPreperiodDays(it.toInt()) },
                            valueRange = 1f..7f, steps = 5
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                SwitchRow("Ovulation window", "Notify around mid-cycle", reminder.ovulationEnabled, viewModel::setOvulationEnabled)
                Spacer(Modifier.height(4.dp))
                SwitchRow("Daily log reminder", "Remind to log while period is active", reminder.dailyDuringPeriodEnabled, viewModel::setDailyEnabled)
                val anyEnabled = reminder.preperiodEnabled || reminder.ovulationEnabled || reminder.dailyDuringPeriodEnabled
                if (anyEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Reminder time", style = MaterialTheme.typography.bodyMedium)
                            Text("%02d:%02d".format(reminder.reminderHour, reminder.reminderMinute),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(onClick = { showTimePicker = true }) { Text("Change") }
                    }
                }
            }

            HorizontalDivider()

            // ── Security ───────────────────────────────────────────────────────
            SettingSection(title = "Security & Privacy") {
                if (!security.hasPinSet) {
                    Text("No PIN set — your data is accessible without authentication.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onNavigateToPinSetup(false) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Set PIN Lock")
                    }
                } else {
                    Text("PIN lock is enabled.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { onNavigateToPinSetup(true) }, modifier = Modifier.weight(1f)) {
                            Text("Change PIN")
                        }
                        OutlinedButton(
                            onClick = { showRemovePinDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Remove PIN") }
                    }
                    if (viewModel.isBiometricAvailable) {
                        Spacer(Modifier.height(8.dp))
                        SwitchRow(
                            label = "Biometric unlock",
                            subtitle = "Use fingerprint or face to unlock",
                            checked = security.biometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                OutlinedButton(onClick = { showDisclaimer = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("View Privacy & Medical Disclaimer")
                }
            }

            HorizontalDivider()

            // ── About ──────────────────────────────────────────────────────────
            SettingSection(title = "About") {
                Text("GoFlo v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text("All your data stays on your device — nothing is sent anywhere.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToLicenses, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Source Licences")
                }
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

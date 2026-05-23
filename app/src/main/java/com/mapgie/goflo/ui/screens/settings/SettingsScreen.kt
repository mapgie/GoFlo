package com.mapgie.goflo.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.mapgie.goflo.ui.theme.AppTheme
import com.mapgie.goflo.ui.theme.ThemeGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.BuildConfig
import com.mapgie.goflo.data.preferences.hasPinSet
import com.mapgie.goflo.data.repository.ImportResult
import com.mapgie.goflo.ui.components.SelectableChip
import com.mapgie.goflo.ui.screens.disclaimer.DisclaimerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToPinSetup: (changing: Boolean) -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val prefs by viewModel.prefs.collectAsState()
    val security by viewModel.securitySettings.collectAsState()
    val reminder = prefs.reminder
    val currentTheme = runCatching { AppTheme.valueOf(prefs.theme) }.getOrDefault(AppTheme.CORAL)

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showRemovePinDialog by rememberSaveable { mutableStateOf(false) }
    var showDisclaimer by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }
    var showChangelog by rememberSaveable { mutableStateOf(false) }
    // Import state
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var removePinInput by rememberSaveable { mutableStateOf("") }
    var removePinError by rememberSaveable { mutableStateOf(false) }

    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportOptionsDialog = true
        }
    }

    val timeState = rememberTimePickerState(
        initialHour = reminder.reminderHour,
        initialMinute = reminder.reminderMinute
    )

    if (showDisclaimer) {
        DisclaimerScreen(onAcknowledge = { showDisclaimer = false })
        return
    }

    if (showChangelog) {
        ChangelogDialog(onDismiss = { showChangelog = false })
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

    if (showImportOptionsDialog && pendingImportUri != null) {
        val uri = pendingImportUri!!
        AlertDialog(
            onDismissRequest = { showImportOptionsDialog = false; pendingImportUri = null },
            title = { Text("Import data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "How should the imported data be handled?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Merge — adds new periods; skips any whose start date already exists. " +
                        "Safe if you have already logged some entries on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Replace — deletes everything on this device first, then imports. " +
                        "Use when moving all data from your old phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                // Replace — primary action for phone-migration use case
                Button(
                    onClick = {
                        showImportOptionsDialog = false
                        viewModel.importData(uri, replace = true) { result ->
                            pendingImportUri = null
                            importResult = result
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Replace") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showImportOptionsDialog = false; pendingImportUri = null }) {
                        Text("Cancel")
                    }
                    OutlinedButton(
                        onClick = {
                            showImportOptionsDialog = false
                            viewModel.importData(uri, replace = false) { result ->
                                pendingImportUri = null
                                importResult = result
                            }
                        }
                    ) { Text("Merge") }
                }
            }
        )
    }

    importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { importResult = null },
            title = {
                Text(if (result is ImportResult.Success) "Import complete" else "Import failed")
            },
            text = {
                Text(
                    when (result) {
                        is ImportResult.Success -> buildString {
                            append("${result.imported} period(s) imported.")
                            if (result.skipped > 0) append("\n${result.skipped} skipped (already existed).")
                        }
                        is ImportResult.Failure -> result.message
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { importResult = null }) { Text("OK") }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "This will permanently remove all period logs, symptoms, and notes. " +
                    "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAllData {}
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
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
            // ── Appearance ────────────────────────────────────────────────────
            SettingSection(title = "Appearance") {
                ThemePickerSection(current = currentTheme, onSelect = { viewModel.setTheme(it.name) })
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

            // ── Cycle ──────────────────────────────────────────────────────────
            SettingSection(title = "Cycle") {
                val customEnabled = prefs.preferredCycleLength > 0
                SwitchRow(
                    label    = "Custom cycle length",
                    subtitle = if (customEnabled)
                        "Using ${prefs.preferredCycleLength} days"
                    else
                        "Auto — calculated from your history",
                    checked        = customEnabled,
                    onCheckedChange = { on ->
                        if (on) {
                            // Default to 28 days (or current override if already set)
                            viewModel.setPreferredCycleLength(
                                prefs.preferredCycleLength.coerceIn(21, 45).let { if (it == 0) 28 else it }
                            )
                        } else {
                            viewModel.setPreferredCycleLength(0)
                        }
                    }
                )
                if (customEnabled) {
                    Spacer(Modifier.height(4.dp))
                    // Local slider state — tracks the thumb position during dragging
                    // without triggering a DataStore write on every pixel of movement.
                    // The key resets the local value whenever the persisted preference
                    // changes from outside (e.g. toggling the switch off then on).
                    var sliderDays by remember(prefs.preferredCycleLength) {
                        mutableStateOf(prefs.preferredCycleLength.toFloat())
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            "Cycle length: ${sliderDays.toInt()} days",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value                = sliderDays,
                            onValueChange        = { sliderDays = it },
                            // DataStore write happens only when the drag ends — not on every frame.
                            onValueChangeFinished = {
                                viewModel.setPreferredCycleLength(sliderDays.toInt())
                            },
                            valueRange           = 21f..45f,
                            // steps = 23 gives 25 discrete tick positions (21–45 inclusive)
                            steps                = 23
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("21 days", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("45 days", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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

            // ── Data ───────────────────────────────────────────────────────────────
            SettingSection(title = "Data") {
                Text(
                    "Export or import all your data, or permanently delete everything stored on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.exportData { intent -> context.startActivity(intent) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Data (JSON)")
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { viewModel.exportCsv { intent -> context.startActivity(intent) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Data (CSV)")
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { importFilePicker.launch("application/json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Data")
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showDeleteAllDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All Data")
                }
            }

            HorizontalDivider()

            // ── About ──────────────────────────────────────────────────────────
            SettingSection(title = "About") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showChangelog = true }
                        .semantics { role = Role.Button }
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "GoFlo v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Tap to see changelog",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("All your data stays on your device — nothing is sent anywhere.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToPrivacy, modifier = Modifier.fillMaxWidth()) {
                    Text("Privacy Policy")
                }
                OutlinedButton(onClick = onNavigateToLicenses, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Source Licences")
                }
            }
        }
    }
}

// ── Theme picker ──────────────────────────────────────────────────────────────

/**
 * Grouped theme picker. Themes are bucketed by [ThemeGroup] and rendered as
 * labelled [FlowRow]s so chips wrap naturally on small screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePickerSection(current: AppTheme, onSelect: (AppTheme) -> Unit) {
    // Render groups in a fixed order; SYSTEM has no group label (shown inline).
    val orderedGroups = listOf(
        ThemeGroup.SYSTEM,
        ThemeGroup.LIGHT,
        ThemeGroup.DARK,
        ThemeGroup.HIGH_CONTRAST,
        ThemeGroup.COLOR_BLIND,
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (group in orderedGroups) {
            val themes = AppTheme.entries.filter { it.group == group }
            if (themes.isEmpty()) continue
            if (group.label.isNotEmpty()) {
                Text(
                    text  = group.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { theme ->
                    ThemeChip(
                        theme    = theme,
                        selected = theme == current,
                        onClick  = { onSelect(theme) },
                    )
                }
            }
        }
    }
}

/**
 * A [FilterChip] with a small colour-swatch circle showing [AppTheme.previewArgb]
 * so users can preview the theme hue before selecting it.
 */
@Composable
private fun ThemeChip(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val previewColor = Color(theme.previewArgb)
    FilterChip(
        selected    = selected,
        onClick     = onClick,
        label       = { Text(theme.displayName) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(previewColor)
                    // thin ring so a white/black swatch is still visible on any bg
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
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

package com.mapgie.goflo.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.BuildConfig
import com.mapgie.goflo.data.preferences.hasPinSet
import com.mapgie.goflo.data.repository.ImportResult
import com.mapgie.goflo.ui.screens.disclaimer.DisclaimerScreen
import com.mapgie.goflo.ui.theme.AppTheme

// ── Theme mode / palette helpers ──────────────────────────────────────────────

private enum class ThemeMode(val label: String) {
    LIGHT("Light"), DARK("Dark"), SYSTEM("Auto")
}

private enum class StandardPalette(
    val displayName: String,
    val lightTheme: AppTheme,
    val darkTheme: AppTheme,
    val previewArgb: Long,
) {
    CORAL("Coral", AppTheme.CORAL,     AppTheme.CORAL_DARK,     0xFFC15542L),
    TEAL ("Teal",  AppTheme.TURQUOISE, AppTheme.TURQUOISE_DARK, 0xFF00696FL),
    SAGE ("Sage",  AppTheme.GREEN,     AppTheme.GREEN_DARK,     0xFF386A20L),
}

private val AppTheme.themeMode: ThemeMode? get() = when (this) {
    AppTheme.SYSTEM                              -> ThemeMode.SYSTEM
    AppTheme.CORAL, AppTheme.TURQUOISE,
    AppTheme.GREEN                               -> ThemeMode.LIGHT
    AppTheme.CORAL_DARK, AppTheme.TURQUOISE_DARK,
    AppTheme.GREEN_DARK                          -> ThemeMode.DARK
    else                                         -> null  // accessibility
}

private val AppTheme.standardPalette: StandardPalette? get() = when (this) {
    AppTheme.CORAL,     AppTheme.CORAL_DARK     -> StandardPalette.CORAL
    AppTheme.TURQUOISE, AppTheme.TURQUOISE_DARK -> StandardPalette.TEAL
    AppTheme.GREEN,     AppTheme.GREEN_DARK     -> StandardPalette.SAGE
    else                                         -> null
}

private val AppTheme.summaryLabel: String get() = when (this) {
    AppTheme.SYSTEM              -> "Follow system"
    AppTheme.CORAL               -> "Coral · Light"
    AppTheme.TURQUOISE           -> "Teal · Light"
    AppTheme.GREEN               -> "Sage · Light"
    AppTheme.CORAL_DARK          -> "Coral · Dark"
    AppTheme.TURQUOISE_DARK      -> "Teal · Dark"
    AppTheme.GREEN_DARK          -> "Sage · Dark"
    AppTheme.HIGH_CONTRAST_LIGHT -> "High Contrast Light"
    AppTheme.HIGH_CONTRAST_DARK  -> "High Contrast Dark"
    AppTheme.BLUE_ORANGE         -> "Blue & Orange"
}

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToPinSetup: (changing: Boolean) -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToManageCategories: () -> Unit = {}
) {
    val context  = LocalContext.current
    val prefs    by viewModel.prefs.collectAsState()
    val security by viewModel.securitySettings.collectAsState()
    val categories by viewModel.trackingCategories.collectAsState()
    val reminder = prefs.reminder
    val currentTheme = runCatching { AppTheme.valueOf(prefs.theme) }.getOrDefault(AppTheme.CORAL)

    var showTimePicker        by rememberSaveable { mutableStateOf(false) }
    var showRemovePinDialog   by rememberSaveable { mutableStateOf(false) }
    var showDisclaimer        by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllDialog   by rememberSaveable { mutableStateOf(false) }
    var showChangelog         by rememberSaveable { mutableStateOf(false) }
    var pendingImportUri      by remember { mutableStateOf<Uri?>(null) }
    var showImportOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var importResult          by remember { mutableStateOf<ImportResult?>(null) }
    var removePinInput        by rememberSaveable { mutableStateOf("") }
    var removePinError        by rememberSaveable { mutableStateOf(false) }

    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) { pendingImportUri = uri; showImportOptionsDialog = true }
    }

    val timeState = rememberTimePickerState(
        initialHour   = reminder.reminderHour,
        initialMinute = reminder.reminderMinute
    )

    // ── Full-screen overlays ──────────────────────────────────────────────────

    if (showDisclaimer) {
        DisclaimerScreen(onAcknowledge = { showDisclaimer = false })
        return
    }

    if (showChangelog) {
        ChangelogDialog(onDismiss = { showChangelog = false })
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

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
            onDismissRequest = {
                showRemovePinDialog = false; removePinInput = ""; removePinError = false
            },
            title = { Text("Remove PIN lock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your current PIN to confirm.")
                    OutlinedTextField(
                        value          = removePinInput,
                        onValueChange  = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                removePinInput = it; removePinError = false
                            }
                        },
                        label               = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError             = removePinError,
                        supportingText      = if (removePinError) ({ Text("Incorrect PIN") }) else null
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removePin(removePinInput) { success ->
                            if (success) {
                                showRemovePinDialog = false; removePinInput = ""; removePinError = false
                            } else removePinError = true
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRemovePinDialog = false; removePinInput = ""; removePinError = false
                }) { Text("Cancel") }
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
                    Text("How should the imported data be handled?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Merge — adds new periods; skips any whose start date already exists. " +
                        "Safe if you have already logged some entries on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color  = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Replace — deletes everything on this device first, then imports. " +
                        "Use when moving all data from your old phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color  = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportOptionsDialog = false
                        viewModel.importData(uri, replace = true) { result ->
                            pendingImportUri = null; importResult = result
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
                    OutlinedButton(onClick = {
                        showImportOptionsDialog = false
                        viewModel.importData(uri, replace = false) { result ->
                            pendingImportUri = null; importResult = result
                        }
                    }) { Text("Merge") }
                }
            }
        )
    }

    importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { importResult = null },
            title = { Text(if (result is ImportResult.Success) "Import complete" else "Import failed") },
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
            confirmButton = { TextButton(onClick = { importResult = null }) { Text("OK") } }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title   = { Text("Delete all data?") },
            text    = {
                Text(
                    "This will permanently remove all period logs, symptoms, and notes. " +
                    "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteAllDialog = false; viewModel.deleteAllData {} },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Computed summaries for collapsed headers ───────────────────────────────

    val activeReminderCount = listOf(
        reminder.preperiodEnabled,
        reminder.ovulationEnabled,
        reminder.dailyDuringPeriodEnabled
    ).count { it }
    val reminderSummary = if (activeReminderCount == 0) "No reminders enabled"
    else "$activeReminderCount active · %02d:%02d".format(reminder.reminderHour, reminder.reminderMinute)

    val cycleSummary = if (prefs.preferredCycleLength > 0)
        "Custom: ${prefs.preferredCycleLength} days"
    else "Auto — calculated from history"

    val securitySummary = if (security.hasPinSet) "PIN lock enabled" else "No PIN set"

    // ── Scaffold ──────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor   = MaterialTheme.colorScheme.primaryContainer,
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── 1. Reminders ──────────────────────────────────────────────────
            CollapsibleSection(
                title   = "Reminders",
                icon    = Icons.Outlined.NotificationsNone,
                summary = reminderSummary
            ) {
                SwitchRow(
                    label          = "Before period alert",
                    subtitle       = "Notify ${reminder.preperiodDaysBefore} day(s) before predicted start",
                    checked        = reminder.preperiodEnabled,
                    onCheckedChange = viewModel::setPreperiodEnabled
                )
                if (reminder.preperiodEnabled) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            "Days before: ${reminder.preperiodDaysBefore}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value         = reminder.preperiodDaysBefore.toFloat(),
                            onValueChange = { viewModel.setPreperiodDays(it.toInt()) },
                            valueRange    = 1f..7f,
                            steps         = 5
                        )
                    }
                }

                SwitchRow(
                    label          = "Ovulation window",
                    subtitle       = "Notify around mid-cycle",
                    checked        = reminder.ovulationEnabled,
                    onCheckedChange = viewModel::setOvulationEnabled
                )

                SwitchRow(
                    label          = "Daily log reminder",
                    subtitle       = "Remind to log while period is active",
                    checked        = reminder.dailyDuringPeriodEnabled,
                    onCheckedChange = viewModel::setDailyEnabled
                )

                val anyEnabled = reminder.preperiodEnabled ||
                        reminder.ovulationEnabled ||
                        reminder.dailyDuringPeriodEnabled
                if (anyEnabled) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Reminder time", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "%02d:%02d".format(reminder.reminderHour, reminder.reminderMinute),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(onClick = { showTimePicker = true }) { Text("Change") }
                    }
                }
            }

            // ── 2. Cycle ──────────────────────────────────────────────────────
            CollapsibleSection(
                title   = "Cycle",
                icon    = Icons.Outlined.Autorenew,
                summary = cycleSummary
            ) {
                val customEnabled = prefs.preferredCycleLength > 0
                SwitchRow(
                    label    = "Custom cycle length",
                    subtitle = if (customEnabled)
                        "Using ${prefs.preferredCycleLength} days"
                    else
                        "Auto — calculated from your history",
                    checked        = customEnabled,
                    onCheckedChange = { on ->
                        viewModel.setPreferredCycleLength(
                            if (on) prefs.preferredCycleLength.coerceIn(21, 45).let { if (it == 0) 28 else it }
                            else 0
                        )
                    }
                )
                if (customEnabled) {
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
                            onValueChangeFinished = { viewModel.setPreferredCycleLength(sliderDays.toInt()) },
                            valueRange           = 21f..45f,
                            steps                = 23
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("21 days", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("45 days", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── 3. Tracking ───────────────────────────────────────────────────
            Card(
                onClick = onNavigateToManageCategories,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text("Tracking Categories", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${categories.size} categor${if (categories.size == 1) "y" else "ies"} · tap to manage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Quick Log default ─────────────────────────────────────────────
            CollapsibleSection(
                title   = "Quick Log",
                icon    = Icons.Default.Add,
                summary = run {
                    val cat = categories.firstOrNull { it.id == prefs.quickLogCategoryId }
                    if (cat != null) "Opens: Log ${cat.name}" else "Opens: Log Period"
                }
            ) {
                Text(
                    "When you tap the Log… button, which screen opens?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                // Log Period option
                val isPeriodDefault = prefs.quickLogCategoryId == -1L
                FilterChip(
                    selected = isPeriodDefault,
                    onClick = { viewModel.setQuickLogCategory(-1L) },
                    label = { Text("Log Period") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                // One chip per tracking category
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = prefs.quickLogCategoryId == category.id,
                            onClick = { viewModel.setQuickLogCategory(category.id) },
                            label = { Text("Log ${category.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // ── 4. Appearance ─────────────────────────────────────────────────
            CollapsibleSection(
                title   = "Appearance",
                icon    = Icons.Outlined.Palette,
                summary = currentTheme.summaryLabel
            ) {
                CompactThemePicker(
                    current  = currentTheme,
                    onSelect = { viewModel.setTheme(it.name) }
                )
            }

            // ── 5. Security & Privacy ─────────────────────────────────────────
            CollapsibleSection(
                title   = "Security & Privacy",
                icon    = Icons.Outlined.Lock,
                summary = securitySummary
            ) {
                if (!security.hasPinSet) {
                    Text(
                        "No PIN set — your data is accessible without authentication.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick  = { onNavigateToPinSetup(false) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Set PIN Lock") }
                } else {
                    Text(
                        "PIN lock is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick  = { onNavigateToPinSetup(true) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Change PIN") }
                        OutlinedButton(
                            onClick  = { showRemovePinDialog = true },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Remove PIN") }
                    }
                    if (viewModel.isBiometricAvailable) {
                        Spacer(Modifier.height(4.dp))
                        SwitchRow(
                            label          = "Biometric unlock",
                            subtitle       = "Use fingerprint or face to unlock",
                            checked        = security.biometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick  = { showDisclaimer = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("View Privacy & Medical Disclaimer") }
            }

            // ── 6. Data ───────────────────────────────────────────────────────
            CollapsibleSection(
                title   = "Data",
                icon    = Icons.Outlined.Storage,
                summary = "Export, import & manage your data"
            ) {
                Text(
                    "Back up or transfer your data. Export JSON to keep a full backup; " +
                    "CSV is useful for spreadsheets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                // Export row: two side-by-side buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = { viewModel.exportData { intent -> context.startActivity(intent) } },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export JSON") }
                    OutlinedButton(
                        onClick  = { viewModel.exportCsv { intent -> context.startActivity(intent) } },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export CSV") }
                }

                OutlinedButton(
                    onClick  = { importFilePicker.launch("application/json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import Data") }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick  = { showDeleteAllDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete All Data") }
            }

            // ── 7. About ──────────────────────────────────────────────────────
            CollapsibleSection(
                title   = "About",
                icon    = Icons.Outlined.Info,
                summary = "GoFlo v${BuildConfig.VERSION_NAME}"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showChangelog = true }
                        .semantics { role = Role.Button }
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text  = "GoFlo v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text  = "Tap to see changelog",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    "All your data stays on your device — nothing is sent anywhere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = onNavigateToPrivacy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Privacy Policy") }
                    OutlinedButton(
                        onClick  = onNavigateToLicenses,
                        modifier = Modifier.weight(1f)
                    ) { Text("Licences") }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Collapsible section card ──────────────────────────────────────────────────

@Composable
private fun CollapsibleSection(
    title:   String,
    icon:    ImageVector,
    summary: String,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label       = "chevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Header row — always visible, tappable to expand/collapse
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    modifier              = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(22.dp)
                    )
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        if (!expanded && summary.isNotEmpty()) {
                            Text(
                                text     = summary,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    imageVector        = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier           = Modifier
                        .size(20.dp)
                        .rotate(chevronAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier              = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    content()
                }
            }
        }
    }
}

// ── Compact theme picker ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactThemePicker(current: AppTheme, onSelect: (AppTheme) -> Unit) {
    val currentMode    = current.themeMode
    val currentPalette = current.standardPalette

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── Mode segmented control ────────────────────────────────────────────
        Text(
            "Mode",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            val modes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
            modes.forEachIndexed { index, mode ->
                val selected  = mode == currentMode
                val modeIcon  = when (mode) {
                    ThemeMode.LIGHT  -> Icons.Outlined.WbSunny
                    ThemeMode.DARK   -> Icons.Outlined.DarkMode
                    ThemeMode.SYSTEM -> Icons.Outlined.SettingsBrightness
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable {
                            when (mode) {
                                ThemeMode.SYSTEM -> onSelect(AppTheme.SYSTEM)
                                ThemeMode.LIGHT  -> {
                                    val palette = currentPalette ?: StandardPalette.CORAL
                                    onSelect(palette.lightTheme)
                                }
                                ThemeMode.DARK -> {
                                    val palette = currentPalette ?: StandardPalette.CORAL
                                    onSelect(palette.darkTheme)
                                }
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector        = modeIcon,
                            contentDescription = mode.label,
                            tint               = if (selected) MaterialTheme.colorScheme.onPrimary
                                                 else MaterialTheme.colorScheme.onSurface,
                            modifier           = Modifier.size(18.dp)
                        )
                        Text(
                            text  = mode.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // Divider between segments
                if (index < modes.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }

        // ── Palette circles (hidden when System is active) ────────────────────
        AnimatedVisibility(visible = currentMode != ThemeMode.SYSTEM) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Colour",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StandardPalette.entries.forEach { palette ->
                        val selected = palette == currentPalette
                        PaletteOption(
                            palette  = palette,
                            selected = selected,
                            onClick  = {
                                val mode = currentMode ?: ThemeMode.LIGHT
                                onSelect(
                                    if (mode == ThemeMode.LIGHT) palette.lightTheme
                                    else palette.darkTheme
                                )
                            }
                        )
                    }
                }
            }
        }

        // ── Accessibility themes ──────────────────────────────────────────────
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "Accessibility",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                AppTheme.HIGH_CONTRAST_LIGHT,
                AppTheme.HIGH_CONTRAST_DARK,
                AppTheme.BLUE_ORANGE
            ).forEach { theme ->
                ThemeChip(theme = theme, selected = current == theme, onClick = { onSelect(theme) })
            }
        }
    }
}

// ── Palette circle option ─────────────────────────────────────────────────────

@Composable
private fun PaletteOption(palette: StandardPalette, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(palette.previewArgb))
                .border(
                    width  = if (selected) 3.dp else 1.dp,
                    color  = if (selected) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape  = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text  = palette.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Theme chip (for accessibility themes) ─────────────────────────────────────

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
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor   = MaterialTheme.colorScheme.primary,
            selectedLabelColor       = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        )
    )
}

// ── Switch row ────────────────────────────────────────────────────────────────

@Composable
private fun SwitchRow(
    label:          String,
    subtitle:       String,
    checked:        Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label,    style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

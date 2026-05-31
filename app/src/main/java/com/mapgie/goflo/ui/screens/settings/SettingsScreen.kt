package com.mapgie.goflo.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mapgie.goflo.AppIconChoice
import com.mapgie.goflo.ui.components.BetaFeedbackBanner
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WbSunny
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.filled.Archive
import com.mapgie.goflo.BuildConfig
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.export.DateRangePreset
import com.mapgie.goflo.data.export.ExportConfig
import com.mapgie.goflo.data.export.ExportFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.mapgie.goflo.data.preferences.AppPreferences
import com.mapgie.goflo.data.preferences.ReminderSettings
import com.mapgie.goflo.data.preferences.SecuritySettings
import com.mapgie.goflo.data.preferences.hasPinSet
import com.mapgie.goflo.data.repository.ImportResult
import androidx.compose.material3.Checkbox
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
    val systemTheme: AppTheme,
    val previewArgb: Long,
    val accentArgb: Long,
) {
    // Classic
    CORAL        ("Coral",                  AppTheme.CORAL,        AppTheme.CORAL_DARK,        AppTheme.CORAL_SYSTEM,          0xFFC15542L, 0xFFB5307AL),
    TEAL         ("Teal",                   AppTheme.TURQUOISE,    AppTheme.TURQUOISE_DARK,    AppTheme.SYSTEM,                0xFF00696FL, 0xFF4E6078L),
    SAGE         ("Sage",                   AppTheme.GREEN,        AppTheme.GREEN_DARK,        AppTheme.GREEN_SYSTEM,          0xFF386A20L, 0xFF6B5BAEL),
    // Fun
    SUMMER_CANDY ("Summer Candy",           AppTheme.SUMMER_CANDY, AppTheme.SUMMER_CANDY_DARK, AppTheme.SUMMER_CANDY_SYSTEM,   0xFFC2185BL, 0xFF9B27AFL),
    BEACH_VIBES  ("Beach Vibes",            AppTheme.BEACH_VIBES,  AppTheme.BEACH_VIBES_DARK,  AppTheme.BEACH_VIBES_SYSTEM,    0xFF0D47A1L, 0xFFD4700AL),
    PEACH_MELBA  ("Peach Melba",            AppTheme.PEACH_MELBA,  AppTheme.PEACH_MELBA_DARK,  AppTheme.PEACH_MELBA_SYSTEM,    0xFFBF360CL, 0xFF9C5BA0L),
    DISCO        ("All-Night Disco Party",  AppTheme.DISCO,        AppTheme.DISCO_DARK,        AppTheme.DISCO_SYSTEM,          0xFF7B0EA0L, 0xFF76B900L),
    METAL_CHICK  ("Metal Chic",             AppTheme.METAL_CHICK,  AppTheme.METAL_CHICK_DARK,  AppTheme.METAL_CHICK_SYSTEM,    0xFF4A4A5AL, 0xFF6B2D3EL),
    WHIMSY       ("Whimsy Whispers",        AppTheme.WHIMSY,       AppTheme.WHIMSY_DARK,       AppTheme.WHIMSY_SYSTEM,         0xFF5050A0L, 0xFF2D7A6EL),
    COLOUR_HAPPY ("Colour Me Happy",        AppTheme.COLOUR_HAPPY, AppTheme.COLOUR_HAPPY_DARK, AppTheme.COLOUR_HAPPY_SYSTEM,   0xFFD63A26L, 0xFF00A8E8L),
    // Bold
    DRAGON_FIRE   ("Dragon Fire",    AppTheme.DRAGON_FIRE,   AppTheme.DRAGON_FIRE_DARK,   AppTheme.DRAGON_FIRE_SYSTEM,   0xFFB71C1CL, 0xFFE07800L),
    MIDNIGHT_NEON ("Midnight Neon",  AppTheme.MIDNIGHT_NEON, AppTheme.MIDNIGHT_NEON_DARK, AppTheme.MIDNIGHT_NEON_SYSTEM, 0xFF6200EAL, 0xFF76B900L),
    // Accessibility
    MAX_CONTRAST     ("Max Contrast",  AppTheme.HIGH_CONTRAST_LIGHT, AppTheme.HIGH_CONTRAST_DARK, AppTheme.HIGH_CONTRAST_LIGHT, 0xFF1A1A1AL, 0xFF000000L),
    BLUE_ORANGE_PAL  ("Blue & Orange", AppTheme.BLUE_ORANGE,         AppTheme.BLUE_ORANGE,        AppTheme.BLUE_ORANGE,         0xFF005FADL, 0xFF8B5000L),
}

private val AppTheme.themeMode: ThemeMode? get() = when (this) {
    AppTheme.SYSTEM,
    AppTheme.CORAL_SYSTEM,
    AppTheme.GREEN_SYSTEM,
    AppTheme.SUMMER_CANDY_SYSTEM,
    AppTheme.BEACH_VIBES_SYSTEM,
    AppTheme.PEACH_MELBA_SYSTEM,
    AppTheme.DISCO_SYSTEM,
    AppTheme.METAL_CHICK_SYSTEM,
    AppTheme.WHIMSY_SYSTEM,
    AppTheme.COLOUR_HAPPY_SYSTEM,
    AppTheme.DRAGON_FIRE_SYSTEM, AppTheme.MIDNIGHT_NEON_SYSTEM -> ThemeMode.SYSTEM
    AppTheme.CORAL, AppTheme.TURQUOISE,
    AppTheme.GREEN,
    AppTheme.SUMMER_CANDY, AppTheme.BEACH_VIBES,
    AppTheme.PEACH_MELBA, AppTheme.DISCO,
    AppTheme.METAL_CHICK, AppTheme.WHIMSY,
    AppTheme.COLOUR_HAPPY,
    AppTheme.DRAGON_FIRE, AppTheme.MIDNIGHT_NEON -> ThemeMode.LIGHT
    AppTheme.CORAL_DARK, AppTheme.TURQUOISE_DARK,
    AppTheme.GREEN_DARK,
    AppTheme.SUMMER_CANDY_DARK, AppTheme.BEACH_VIBES_DARK,
    AppTheme.PEACH_MELBA_DARK, AppTheme.DISCO_DARK,
    AppTheme.METAL_CHICK_DARK, AppTheme.WHIMSY_DARK,
    AppTheme.COLOUR_HAPPY_DARK,
    AppTheme.DRAGON_FIRE_DARK, AppTheme.MIDNIGHT_NEON_DARK -> ThemeMode.DARK
    else                                         -> null
}

private val AppTheme.standardPalette: StandardPalette? get() = when (this) {
    AppTheme.CORAL,           AppTheme.CORAL_DARK,
    AppTheme.CORAL_SYSTEM                                          -> StandardPalette.CORAL
    AppTheme.TURQUOISE,       AppTheme.TURQUOISE_DARK,
    AppTheme.SYSTEM                                                -> StandardPalette.TEAL
    AppTheme.GREEN,           AppTheme.GREEN_DARK,
    AppTheme.GREEN_SYSTEM                                          -> StandardPalette.SAGE
    AppTheme.SUMMER_CANDY,    AppTheme.SUMMER_CANDY_DARK,
    AppTheme.SUMMER_CANDY_SYSTEM                                   -> StandardPalette.SUMMER_CANDY
    AppTheme.BEACH_VIBES,     AppTheme.BEACH_VIBES_DARK,
    AppTheme.BEACH_VIBES_SYSTEM                                    -> StandardPalette.BEACH_VIBES
    AppTheme.PEACH_MELBA,     AppTheme.PEACH_MELBA_DARK,
    AppTheme.PEACH_MELBA_SYSTEM                                    -> StandardPalette.PEACH_MELBA
    AppTheme.DISCO,           AppTheme.DISCO_DARK,
    AppTheme.DISCO_SYSTEM                                          -> StandardPalette.DISCO
    AppTheme.METAL_CHICK,     AppTheme.METAL_CHICK_DARK,
    AppTheme.METAL_CHICK_SYSTEM                                    -> StandardPalette.METAL_CHICK
    AppTheme.WHIMSY,          AppTheme.WHIMSY_DARK,
    AppTheme.WHIMSY_SYSTEM                                         -> StandardPalette.WHIMSY
    AppTheme.COLOUR_HAPPY,    AppTheme.COLOUR_HAPPY_DARK,
    AppTheme.COLOUR_HAPPY_SYSTEM                                   -> StandardPalette.COLOUR_HAPPY
    AppTheme.DRAGON_FIRE,   AppTheme.DRAGON_FIRE_DARK,
    AppTheme.DRAGON_FIRE_SYSTEM                                   -> StandardPalette.DRAGON_FIRE
    AppTheme.MIDNIGHT_NEON, AppTheme.MIDNIGHT_NEON_DARK,
    AppTheme.MIDNIGHT_NEON_SYSTEM                                 -> StandardPalette.MIDNIGHT_NEON
    AppTheme.HIGH_CONTRAST_LIGHT,
    AppTheme.HIGH_CONTRAST_DARK                                   -> StandardPalette.MAX_CONTRAST
    AppTheme.BLUE_ORANGE                                          -> StandardPalette.BLUE_ORANGE_PAL
}

private val AppTheme.summaryLabel: String get() = when (this) {
    AppTheme.SYSTEM                -> "Teal · Auto"
    AppTheme.CORAL_SYSTEM          -> "Coral · Auto"
    AppTheme.GREEN_SYSTEM          -> "Sage · Auto"
    AppTheme.SUMMER_CANDY_SYSTEM   -> "Summer Candy · Auto"
    AppTheme.BEACH_VIBES_SYSTEM    -> "Beach Vibes · Auto"
    AppTheme.PEACH_MELBA_SYSTEM    -> "Peach Melba · Auto"
    AppTheme.DISCO_SYSTEM          -> "All-Night Disco Party · Auto"
    AppTheme.METAL_CHICK_SYSTEM    -> "Metal Chic · Auto"
    AppTheme.WHIMSY_SYSTEM         -> "Whimsy Whispers · Auto"
    AppTheme.COLOUR_HAPPY_SYSTEM   -> "Colour Me Happy · Auto"
    AppTheme.CORAL                 -> "Coral · Light"
    AppTheme.TURQUOISE             -> "Teal · Light"
    AppTheme.GREEN                 -> "Sage · Light"
    AppTheme.CORAL_DARK            -> "Coral · Dark"
    AppTheme.TURQUOISE_DARK        -> "Teal · Dark"
    AppTheme.GREEN_DARK            -> "Sage · Dark"
    AppTheme.SUMMER_CANDY          -> "Summer Candy · Light"
    AppTheme.SUMMER_CANDY_DARK     -> "Summer Candy · Dark"
    AppTheme.BEACH_VIBES           -> "Beach Vibes · Light"
    AppTheme.BEACH_VIBES_DARK      -> "Beach Vibes · Dark"
    AppTheme.PEACH_MELBA           -> "Peach Melba · Light"
    AppTheme.PEACH_MELBA_DARK      -> "Peach Melba · Dark"
    AppTheme.DISCO                 -> "All-Night Disco Party · Light"
    AppTheme.DISCO_DARK            -> "All-Night Disco Party · Dark"
    AppTheme.METAL_CHICK           -> "Metal Chic · Light"
    AppTheme.METAL_CHICK_DARK      -> "Metal Chic · Dark"
    AppTheme.WHIMSY                -> "Whimsy Whispers · Light"
    AppTheme.WHIMSY_DARK           -> "Whimsy Whispers · Dark"
    AppTheme.COLOUR_HAPPY          -> "Colour Me Happy · Light"
    AppTheme.COLOUR_HAPPY_DARK     -> "Colour Me Happy · Dark"
    AppTheme.DRAGON_FIRE_SYSTEM   -> "Dragon Fire · Auto"
    AppTheme.MIDNIGHT_NEON_SYSTEM -> "Midnight Neon · Auto"
    AppTheme.DRAGON_FIRE          -> "Dragon Fire · Light"
    AppTheme.DRAGON_FIRE_DARK     -> "Dragon Fire · Dark"
    AppTheme.MIDNIGHT_NEON        -> "Midnight Neon · Light"
    AppTheme.MIDNIGHT_NEON_DARK   -> "Midnight Neon · Dark"
    AppTheme.HIGH_CONTRAST_LIGHT   -> "Max Contrast · Light"
    AppTheme.HIGH_CONTRAST_DARK    -> "Max Contrast · Dark"
    AppTheme.BLUE_ORANGE           -> "Blue & Orange"
}

// ── Export date display format ─────────────────────────────────────────────────

private val exportDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

// ── Sub-screen routing ────────────────────────────────────────────────────────

private enum class SettingsSubScreen {
    NONE, CYCLE, QUICK_LOG, REMINDERS, APPEARANCE, SECURITY, DATA, EXPORT_DATA, WIDGETS, ABOUT
}

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToPinSetup: (changing: Boolean) -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToManageCategories: () -> Unit = {}
) {
    val context    = LocalContext.current
    val prefs      by viewModel.prefs.collectAsState()
    val security   by viewModel.securitySettings.collectAsState()
    val categories by viewModel.trackingCategories.collectAsState()
    val allCategoriesForExport by viewModel.allCategoriesForExport.collectAsState()
    val reminder = prefs.reminder
    val currentTheme = runCatching { AppTheme.valueOf(prefs.theme) }.getOrDefault(AppTheme.CORAL)

    val currentIconChoice = runCatching {
        AppIconChoice.valueOf(prefs.iconChoice)
    }.getOrDefault(AppIconChoice.DEFAULT)

    val mainListScrollState      = rememberScrollState()
    var currentSubScreen        by rememberSaveable { mutableStateOf(SettingsSubScreen.NONE) }
    var showTimePicker          by rememberSaveable { mutableStateOf(false) }
    var showRemovePinDialog     by rememberSaveable { mutableStateOf(false) }
    var customIconError         by remember { mutableStateOf<String?>(null) }
    var showDisclaimer          by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllDialog     by rememberSaveable { mutableStateOf(false) }
    var showChangelog           by rememberSaveable { mutableStateOf(false) }
    var pendingImportUri        by remember { mutableStateOf<Uri?>(null) }
    var showImportOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var importResult            by remember { mutableStateOf<ImportResult?>(null) }
    var removePinInput          by rememberSaveable { mutableStateOf("") }
    var removePinError          by rememberSaveable { mutableStateOf(false) }

    BackHandler(currentSubScreen != SettingsSubScreen.NONE) {
        currentSubScreen = when (currentSubScreen) {
            SettingsSubScreen.EXPORT_DATA -> SettingsSubScreen.DATA
            else -> SettingsSubScreen.NONE
        }
    }

    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) { pendingImportUri = uri; showImportOptionsDialog = true }
    }

    val customIconPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.createCustomIconShortcut(uri) { error -> customIconError = error }
        }
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

    if (showRemovePinDialog) {
        fun dismissRemovePin() { showRemovePinDialog = false; removePinInput = ""; removePinError = false }
        fun addDigit(digit: Int) {
            if (removePinInput.length >= 4) return
            val updated = removePinInput + digit.toString()
            removePinInput = updated
            removePinError = false
            if (updated.length == 4) {
                viewModel.removePin(updated) { success ->
                    if (success) dismissRemovePin() else { removePinInput = ""; removePinError = true }
                }
            }
        }
        BackHandler { dismissRemovePin() }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = { dismissRemovePin() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Remove PIN lock", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enter your current PIN to confirm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(40.dp))
                val dotColor = if (removePinError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (index < removePinInput.length) dotColor else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                if (removePinError) {
                    Spacer(Modifier.height(8.dp))
                    Text("Incorrect PIN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(40.dp))
                val padKeys = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    padKeys.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            row.forEach { digit ->
                                TextButton(onClick = { addDigit(digit) }, modifier = Modifier.size(72.dp)) {
                                    Text(digit.toString(), style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Spacer(Modifier.size(72.dp))
                        TextButton(onClick = { addDigit(0) }, modifier = Modifier.size(72.dp)) {
                            Text("0", style = MaterialTheme.typography.headlineMedium)
                        }
                        TextButton(
                            onClick = { if (removePinInput.isNotEmpty()) { removePinInput = removePinInput.dropLast(1); removePinError = false } },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Text("⌫", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
        return
    }

    // ── App-level dialogs (rendered above whichever sub-screen is active) ──────

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
                    "This will permanently remove all period logs, symptoms, tracking logs, " +
                    "and notes. Your category configuration is kept. This cannot be undone."
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

    customIconError?.let { message ->
        AlertDialog(
            onDismissRequest = { customIconError = null },
            title            = { Text("Couldn't create shortcut") },
            text             = { Text(message) },
            confirmButton    = { TextButton(onClick = { customIconError = null }) { Text("OK") } }
        )
    }

    // ── Route to sub-screens ──────────────────────────────────────────────────

    when (currentSubScreen) {
        SettingsSubScreen.NONE -> SettingsMainList(
            prefs                        = prefs,
            security                     = security,
            categories                   = categories,
            currentTheme                 = currentTheme,
            reminder                     = reminder,
            scrollState                  = mainListScrollState,
            onNavigateTo                 = { currentSubScreen = it },
            onNavigateToManageCategories = onNavigateToManageCategories,
            onOpenDiscord                = { openUrl(context, "https://discord.gg/xphnQCZeYq") },
            onOpenSupport                = { openUrl(context, "https://github.com/sponsors/mapgie") },
            onOpenPrivacy                = onNavigateToPrivacy
        )
        SettingsSubScreen.CYCLE -> CycleSubScreen(
            prefs     = prefs,
            viewModel = viewModel,
            onBack    = { currentSubScreen = SettingsSubScreen.NONE }
        )
        SettingsSubScreen.QUICK_LOG -> QuickLogSubScreen(
            prefs      = prefs,
            categories = categories,
            viewModel  = viewModel,
            onBack     = { currentSubScreen = SettingsSubScreen.NONE }
        )
        SettingsSubScreen.REMINDERS -> RemindersSubScreen(
            reminder        = reminder,
            viewModel       = viewModel,
            onShowTimePicker = { showTimePicker = true },
            onBack          = { currentSubScreen = SettingsSubScreen.NONE }
        )
        SettingsSubScreen.APPEARANCE -> AppearanceSubScreen(
            currentTheme      = currentTheme,
            prefs             = prefs,
            currentIconChoice = currentIconChoice,
            viewModel         = viewModel,
            onPickCustomImage = { customIconPicker.launch("image/*") },
            onBack            = { currentSubScreen = SettingsSubScreen.NONE }
        )
        SettingsSubScreen.SECURITY -> SecuritySubScreen(
            security              = security,
            viewModel             = viewModel,
            onNavigateToPinSetup  = onNavigateToPinSetup,
            onShowRemovePinDialog = { showRemovePinDialog = true },
            onBack                = { currentSubScreen = SettingsSubScreen.NONE }
        )
        SettingsSubScreen.DATA -> DataSubScreen(
            onNavigateToExport = { currentSubScreen = SettingsSubScreen.EXPORT_DATA },
            onShowImportPicker = { importFilePicker.launch("application/json") },
            onShowDeleteDialog = { showDeleteAllDialog = true },
            onBack             = { currentSubScreen = SettingsSubScreen.NONE }
        )
        SettingsSubScreen.EXPORT_DATA -> ExportDataSubScreen(
            categories = allCategoriesForExport,
            onExport   = { config ->
                viewModel.exportWithOptions(config) { intent -> context.startActivity(intent) }
                currentSubScreen = SettingsSubScreen.DATA
            },
            onBack = { currentSubScreen = SettingsSubScreen.DATA }
        )
        SettingsSubScreen.WIDGETS -> WidgetsSubScreen(
            prefs      = prefs,
            security   = security,
            categories = categories,
            viewModel  = viewModel,
            onBack     = { currentSubScreen = SettingsSubScreen.NONE }
        )
        SettingsSubScreen.ABOUT -> AboutSubScreen(
            onNavigateToPrivacy  = onNavigateToPrivacy,
            onNavigateToLicenses = onNavigateToLicenses,
            onShowChangelog      = { showChangelog = true },
            onShowDisclaimer     = { showDisclaimer = true },
            onBack               = { currentSubScreen = SettingsSubScreen.NONE }
        )
    }
}

// ── Settings main flat list ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainList(
    prefs:                        AppPreferences,
    security:                     SecuritySettings,
    categories:                   List<TrackingCategory>,
    currentTheme:                 AppTheme,
    reminder:                     ReminderSettings,
    scrollState:                  ScrollState,
    onNavigateTo:                 (SettingsSubScreen) -> Unit,
    onNavigateToManageCategories: () -> Unit,
    onOpenDiscord:                () -> Unit,
    onOpenSupport:                () -> Unit,
    onOpenPrivacy:                () -> Unit,
) {
    val cycleSummary = if (prefs.preferredCycleLength > 0)
        "Custom: ${prefs.preferredCycleLength} days"
    else "Auto — calculated from history"

    val activeReminderCount = listOf(
        reminder.preperiodEnabled,
        reminder.ovulationEnabled,
        reminder.dailyDuringPeriodEnabled
    ).count { it }
    val reminderSummary = if (activeReminderCount == 0) "No reminders enabled"
    else "$activeReminderCount active · %02d:%02d".format(reminder.reminderHour, reminder.reminderMinute)

    val securitySummary = if (security.hasPinSet) "PIN lock enabled" else "No PIN set"

    val quickLogSummary = if (prefs.quickLogCategoryId == -1L) "Tap → Period"
    else categories.firstOrNull { it.id == prefs.quickLogCategoryId }?.name
        ?.let { "Tap → $it" } ?: "Tap → Period"

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            BetaFeedbackBanner()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = padding.calculateBottomPadding())
            ) {

                // Medical device disclaimer (always visible at top)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.Top
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Info,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier           = Modifier.size(18.dp)
                        )
                        Text(
                            text  = "Not a medical device. GoFlo is for personal tracking only and is not a substitute for professional medical advice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // ── TRACKING ─────────────────────────────────────────────────
                SettingsSectionHeader("Tracking")
                SettingsNavItem(
                    title    = "What You Track",
                    subtitle = "Customise the symptoms & metrics you log",
                    icon     = Icons.Outlined.Tune,
                    onClick  = onNavigateToManageCategories
                )
                SettingsNavItem(
                    title    = "Cycle",
                    subtitle = cycleSummary,
                    icon     = Icons.Outlined.Autorenew,
                    onClick  = { onNavigateTo(SettingsSubScreen.CYCLE) }
                )
                SettingsNavItem(
                    title    = "One-Tap Quick Log",
                    subtitle = quickLogSummary,
                    icon     = Icons.Outlined.TouchApp,
                    onClick  = { onNavigateTo(SettingsSubScreen.QUICK_LOG) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // ── NOTIFICATIONS ─────────────────────────────────────────────
                SettingsSectionHeader("Notifications")
                SettingsNavItem(
                    title    = "Reminders",
                    subtitle = reminderSummary,
                    icon     = Icons.Outlined.NotificationsNone,
                    onClick  = { onNavigateTo(SettingsSubScreen.REMINDERS) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // ── PERSONALISATION ───────────────────────────────────────────
                SettingsSectionHeader("Personalisation")
                SettingsNavItem(
                    title    = "Appearance",
                    subtitle = currentTheme.summaryLabel,
                    icon     = Icons.Outlined.Palette,
                    onClick  = { onNavigateTo(SettingsSubScreen.APPEARANCE) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // ── PRIVACY & DATA ────────────────────────────────────────────
                SettingsSectionHeader("Privacy & Data")
                SettingsNavItem(
                    title    = "Security & Privacy",
                    subtitle = securitySummary,
                    icon     = Icons.Outlined.Lock,
                    iconTint = if (!security.hasPinSet) MaterialTheme.colorScheme.error else null,
                    onClick  = { onNavigateTo(SettingsSubScreen.SECURITY) }
                )
                SettingsNavItem(
                    title    = "Data & Backup",
                    subtitle = "Export, import & manage your data",
                    icon     = Icons.Outlined.Storage,
                    onClick  = { onNavigateTo(SettingsSubScreen.DATA) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // ── WIDGETS ───────────────────────────────────────────────────
                SettingsSectionHeader("Widgets")
                SettingsNavItem(
                    title    = "Home Screen Widgets",
                    subtitle = "Two widgets available",
                    icon     = Icons.Outlined.Widgets,
                    onClick  = { onNavigateTo(SettingsSubScreen.WIDGETS) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // ── HELP & FEEDBACK ───────────────────────────────────────────
                SettingsSectionHeader("Help & Feedback")
                SettingsNavItem(
                    title    = "Bug report / Feature suggestion",
                    subtitle = "Join the conversation on Discord",
                    icon     = Icons.Outlined.BugReport,
                    onClick  = onOpenDiscord
                )
                SupportCard(
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onSupport = onOpenSupport
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // ── ABOUT ─────────────────────────────────────────────────────
                SettingsSectionHeader("About")
                SettingsNavItem(
                    title    = "About GoFlo",
                    subtitle = "v${BuildConfig.VERSION_NAME}",
                    icon     = Icons.Outlined.Info,
                    onClick  = { onNavigateTo(SettingsSubScreen.ABOUT) }
                )

                // Privacy Policy — always visible at the very bottom
                OutlinedButton(
                    onClick  = onOpenPrivacy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Privacy Policy & Medical Disclaimer")
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Sub-screen: Cycle ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleSubScreen(
    prefs:     AppPreferences,
    viewModel: SettingsViewModel,
    onBack:    () -> Unit
) {
    SettingsSubScreenScaffold(title = "Cycle", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            val customEnabled = prefs.preferredCycleLength > 0

            ListItem(
                headlineContent    = { Text("Custom cycle length") },
                supportingContent  = {
                    Text(if (customEnabled) "Using ${prefs.preferredCycleLength} days"
                         else "Auto — calculated from your history")
                },
                trailingContent    = {
                    Switch(
                        checked         = customEnabled,
                        onCheckedChange = null
                    )
                },
                modifier = Modifier
                    .clickable {
                        viewModel.setPreferredCycleLength(
                            if (customEnabled) 0
                            else prefs.preferredCycleLength.coerceIn(21, 45).let { if (it == 0) 28 else it }
                        )
                    }
                    .semantics { role = Role.Switch }
            )

            if (customEnabled) {
                var sliderDays by remember(prefs.preferredCycleLength) {
                    mutableStateOf(prefs.preferredCycleLength.toFloat())
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Cycle length: ${sliderDays.toInt()} days",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value                 = sliderDays,
                        onValueChange         = { sliderDays = it },
                        onValueChangeFinished = { viewModel.setPreferredCycleLength(sliderDays.toInt()) },
                        valueRange            = 21f..45f,
                        steps                 = 23
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("21 days", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("45 days", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider()

            ListItem(
                headlineContent   = { Text("Show period predictions") },
                supportingContent = { Text("Display predicted future period days on the calendar") },
                trailingContent   = { Switch(checked = prefs.showPeriodPrediction, onCheckedChange = null) },
                modifier          = Modifier
                    .clickable { viewModel.setShowPeriodPrediction(!prefs.showPeriodPrediction) }
                    .semantics { role = Role.Switch }
            )

            HorizontalDivider()

            ListItem(
                headlineContent   = { Text("Show ovulation markers") },
                supportingContent = { Text("Display ovulation day and fertility window on the calendar") },
                trailingContent   = { Switch(checked = prefs.showOvulationMarkers, onCheckedChange = null) },
                modifier          = Modifier
                    .clickable { viewModel.setShowOvulationMarkers(!prefs.showOvulationMarkers) }
                    .semantics { role = Role.Switch }
            )
        }
    }
}

// ── Sub-screen: One-Tap Quick Log ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuickLogSubScreen(
    prefs:      AppPreferences,
    categories: List<TrackingCategory>,
    viewModel:  SettingsViewModel,
    onBack:     () -> Unit
) {
    SettingsSubScreenScaffold(title = "One-Tap Quick Log", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "When you tap the quick-add button, it logs:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = prefs.quickLogCategoryId == -1L,
                    onClick  = { viewModel.setQuickLogCategory(-1L) },
                    label    = { Text("Period") },
                    leadingIcon = if (prefs.quickLogCategoryId == -1L) {
                        { Icon(Icons.Default.Check, contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = prefs.quickLogCategoryId == cat.id,
                        onClick  = { viewModel.setQuickLogCategory(cat.id) },
                        label    = { Text(cat.name) },
                        leadingIcon = if (prefs.quickLogCategoryId == cat.id) {
                            { Icon(Icons.Default.Check, contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null
                    )
                }
            }
        }
    }
}

// ── Sub-screen: Reminders ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindersSubScreen(
    reminder:        ReminderSettings,
    viewModel:       SettingsViewModel,
    onShowTimePicker: () -> Unit,
    onBack:          () -> Unit
) {
    SettingsSubScreenScaffold(title = "Reminders", onBack = onBack) { padding ->
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
                        OutlinedButton(onClick = onShowTimePicker) { Text("Change") }
                    }
                )
            }
        }
    }
}

// ── Sub-screen: Appearance ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSubScreen(
    currentTheme:      AppTheme,
    prefs:             AppPreferences,
    currentIconChoice: AppIconChoice,
    viewModel:         SettingsViewModel,
    onPickCustomImage: () -> Unit,
    onBack:            () -> Unit
) {
    SettingsSubScreenScaffold(title = "Appearance", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactThemePicker(
                current      = currentTheme,
                wcagChecked  = prefs.wcagMode,
                onSelect     = { viewModel.setTheme(it.name) },
                onWcagToggle = { viewModel.setWcagMode(it) }
            )

            HorizontalDivider()

            AppIconPicker(
                currentChoice     = currentIconChoice,
                onSelect          = { viewModel.setIconChoice(it) },
                onPickCustomImage = onPickCustomImage
            )
        }
    }
}

// ── Sub-screen: Security & Privacy ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecuritySubScreen(
    security:             SecuritySettings,
    viewModel:            SettingsViewModel,
    onNavigateToPinSetup: (changing: Boolean) -> Unit,
    onShowRemovePinDialog: () -> Unit,
    onBack:               () -> Unit
) {
    SettingsSubScreenScaffold(title = "Security & Privacy", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!security.hasPinSet) {
                Text(
                    "No PIN set — your data is accessible without authentication.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick  = { onNavigateToPinSetup(true) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Change PIN") }
                    OutlinedButton(
                        onClick  = onShowRemovePinDialog,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Remove PIN") }
                }
                if (viewModel.isBiometricAvailable) {
                    HorizontalDivider()
                    SwitchRow(
                        label           = "Biometric unlock",
                        subtitle        = "Use fingerprint or face to unlock",
                        checked         = security.biometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                }
            }
        }
    }
}

// ── Sub-screen: Data & Backup ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataSubScreen(
    onNavigateToExport: () -> Unit,
    onShowImportPicker: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    onBack:             () -> Unit
) {
    SettingsSubScreenScaffold(title = "Data & Backup", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Back up or transfer your data. Export JSON to keep a full backup; " +
                "CSV is useful for spreadsheets.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick  = onNavigateToExport,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export Data") }

            OutlinedButton(
                onClick  = onShowImportPicker,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Import Data") }

            HorizontalDivider()

            OutlinedButton(
                onClick  = onShowDeleteDialog,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Delete All Data") }
        }
    }
}

// ── Sub-screen: Widgets ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WidgetsSubScreen(
    prefs:      AppPreferences,
    security:   SecuritySettings,
    categories: List<TrackingCategory>,
    viewModel:  SettingsViewModel,
    onBack:     () -> Unit
) {
    SettingsSubScreenScaffold(title = "Home Screen Widgets", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "GoFlo offers two home-screen widgets. Long-press your home screen " +
                "and choose Widgets to add them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── GoFlo Status (2×1) ────────────────────────────────────────────
            HorizontalDivider()
            Text("GoFlo Status (2×1)", style = MaterialTheme.typography.labelMedium)
            Text(
                "Shows your cycle status at a glance — current cycle day and days " +
                "until your next period.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ListItem(
                headlineContent   = { Text("Show data when PIN is set") },
                supportingContent = {
                    Text(
                        if (!security.hasPinSet)
                            "Set a PIN in Security & Privacy to enable this option"
                        else
                            "Show live cycle data on your home screen instead of the privacy placeholder"
                    )
                },
                trailingContent = {
                    Switch(
                        checked         = prefs.widgetDataVisible,
                        onCheckedChange = null,
                        enabled         = security.hasPinSet
                    )
                },
                modifier = if (security.hasPinSet) Modifier
                    .clickable { viewModel.setWidgetDataVisible(!prefs.widgetDataVisible) }
                    .semantics { role = Role.Switch }
                else Modifier
            )

            // ── Quick Log (4×2) ───────────────────────────────────────────────
            HorizontalDivider()
            Text("Quick Log (4×2)", style = MaterialTheme.typography.labelMedium)
            Text(
                "Shows up to four of your active tracking categories. Tap any button " +
                "to jump straight to today's log entry for that category.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (categories.isNotEmpty()) {
                Text(
                    "Choose which categories appear (up to 4). If none are chosen, " +
                    "the first four active categories are shown automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val selectedIds = prefs.widgetCategoryIds
                    .split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
                    .filter { it > 0L }
                    .toSet()

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat.id in selectedIds
                        val atLimit    = selectedIds.size >= 4 && !isSelected
                        FilterChip(
                            selected    = isSelected,
                            enabled     = !atLimit,
                            onClick     = {
                                val newIds = if (isSelected) selectedIds - cat.id else selectedIds + cat.id
                                viewModel.setWidgetCategoryIds(newIds.joinToString(","))
                            },
                            label       = { Text(cat.name) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

// ── Sub-screen: About ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSubScreen(
    onNavigateToPrivacy:  () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onShowChangelog:      () -> Unit,
    onShowDisclaimer:     () -> Unit,
    onBack:               () -> Unit
) {
    SettingsSubScreenScaffold(title = "About", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ListItem(
                headlineContent   = { Text("GoFlo v${BuildConfig.VERSION_NAME}") },
                supportingContent = { Text("Tap to see changelog", color = MaterialTheme.colorScheme.primary) },
                modifier          = Modifier.clickable(onClick = onShowChangelog)
            )

            HorizontalDivider()

            Text(
                "All your data stays on your device — nothing is sent anywhere.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = onNavigateToPrivacy,
                    modifier = Modifier.weight(1f)
                ) { Text("Privacy Policy") }
                OutlinedButton(
                    onClick  = onNavigateToLicenses,
                    modifier = Modifier.weight(1f)
                ) { Text("Licences") }
            }

            OutlinedButton(
                onClick  = onShowDisclaimer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Medical Disclaimer")
            }
        }
    }
}

// ── Sub-screen: Export Data ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExportDataSubScreen(
    categories: List<TrackingCategory>,
    onExport:   (ExportConfig) -> Unit,
    onBack:     () -> Unit,
) {
    var format              by rememberSaveable { mutableStateOf(ExportFormat.JSON) }
    var datePreset          by rememberSaveable { mutableStateOf(DateRangePreset.ALL_TIME) }
    var customStart         by remember { mutableStateOf<LocalDate?>(null) }
    var customEnd           by remember { mutableStateOf<LocalDate?>(null) }
    var includePeriods      by rememberSaveable { mutableStateOf(true) }
    var selectedCategoryIds by remember { mutableStateOf(categories.map { it.id }.toSet()) }
    var showStartPicker     by rememberSaveable { mutableStateOf(false) }
    var showEndPicker       by rememberSaveable { mutableStateOf(false) }

    if (showStartPicker) {
        ExportDatePickerDialog(
            initial   = customStart ?: LocalDate.now().minusYears(1),
            onConfirm = { customStart = it; showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        ExportDatePickerDialog(
            initial   = customEnd ?: LocalDate.now(),
            minDate   = customStart,
            onConfirm = { customEnd = it; showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }

    val customRangeReady = datePreset != DateRangePreset.CUSTOM || (customStart != null && customEnd != null)
    val hasAnything      = includePeriods || selectedCategoryIds.isNotEmpty()

    SettingsSubScreenScaffold(title = "Export Data", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Date range ───────────────────────────────────────────────
                Text("Date range", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    DateRangePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = datePreset == preset,
                            onClick  = { datePreset = preset },
                            label    = { Text(preset.label) }
                        )
                    }
                }
                if (datePreset == DateRangePreset.CUSTOM) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick  = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        ) { Text(customStart?.format(exportDateFmt) ?: "From") }
                        Text("–")
                        OutlinedButton(
                            onClick  = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
                        ) { Text(customEnd?.format(exportDateFmt) ?: "To") }
                    }
                }

                HorizontalDivider()

                // ── What to include ──────────────────────────────────────────
                Text("What to include", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = includePeriods,
                        onClick  = { includePeriods = !includePeriods },
                        label    = { Text("Periods") }
                    )
                    categories.forEach { cat ->
                        val selected = cat.id in selectedCategoryIds
                        FilterChip(
                            selected = selected,
                            onClick  = {
                                selectedCategoryIds = if (selected)
                                    selectedCategoryIds - cat.id
                                else
                                    selectedCategoryIds + cat.id
                            },
                            label = {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(cat.name)
                                    if (cat.isArchived) {
                                        Icon(
                                            Icons.Filled.Archive,
                                            contentDescription = "Archived",
                                            modifier           = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                HorizontalDivider()

                // ── Format ───────────────────────────────────────────────────
                Text("Format", style = MaterialTheme.typography.labelLarge)
                ExportFormat.entries.forEach { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { format = f }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = format == f, onClick = { format = f })
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(f.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when (f) {
                                    ExportFormat.JSON -> "Full backup — can be re-imported"
                                    ExportFormat.CSV  -> "Spreadsheet-friendly flat table"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Sticky Export button ─────────────────────────────────────────
            HorizontalDivider()
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick  = {
                        onExport(
                            ExportConfig(
                                format              = format,
                                includePeriods      = includePeriods,
                                selectedCategoryIds = selectedCategoryIds,
                                dateRangePreset     = datePreset,
                                customStartDate     = customStart,
                                customEndDate       = customEnd
                            )
                        )
                    },
                    enabled  = customRangeReady && hasAnything,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Export") }
            }
        }
    }
}

// ── Sub-screen scaffold wrapper ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSubScreenScaffold(
    title:   String,
    onBack:  () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor          = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor       = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        content = content
    )
}

// ── Flat list item — navigation ────────────────────────────────────────────────

@Composable
private fun SettingsNavItem(
    title:    String,
    subtitle: String,
    icon:     ImageVector,
    iconTint: Color? = null,
    onClick:  () -> Unit
) {
    ListItem(
        headlineContent   = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent    = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = iconTint ?: MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(24.dp)
                )
            }
        },
        trailingContent   = {
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ── Section header label ──────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp, end = 16.dp)
    )
}

// ── External link helper ──────────────────────────────────────────────────────

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

// ── Support card ───────────────────────────────────────────────────────────────

@Composable
private fun SupportCard(
    onSupport: () -> Unit,
    modifier:  Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier           = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Enjoying GoFlo?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "GoFlo is free and open source. If you're enjoying it, consider buying me a coffee.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            Button(onClick = onSupport) { Text("Support") }
        }
    }
}

// ── Compact theme picker ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactThemePicker(
    current:      AppTheme,
    wcagChecked:  Boolean,
    onSelect:     (AppTheme) -> Unit,
    onWcagToggle: (Boolean) -> Unit,
) {
    val currentMode    = current.themeMode
    val currentPalette = current.standardPalette

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

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
                                ThemeMode.SYSTEM -> {
                                    val palette = currentPalette ?: StandardPalette.TEAL
                                    onSelect(palette.systemTheme)
                                }
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .semantics { role = Role.Checkbox }
                .clickable { onWcagToggle(!wcagChecked) }
        ) {
            Checkbox(
                checked         = wcagChecked,
                onCheckedChange = onWcagToggle,
            )
            Text(
                text  = "WCAG accessible",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Colour",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
                maxItemsInEachRow     = 5,
            ) {
                StandardPalette.entries.forEach { palette ->
                    val selected = palette == currentPalette
                    PaletteOption(
                        palette  = palette,
                        selected = selected,
                        onClick  = {
                            val mode = currentMode ?: ThemeMode.LIGHT
                            onSelect(
                                when (mode) {
                                    ThemeMode.DARK   -> palette.darkTheme
                                    ThemeMode.SYSTEM -> palette.systemTheme
                                    else             -> palette.lightTheme
                                }
                            )
                        }
                    )
                }
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
        val primaryColor  = Color(palette.previewArgb)
        val accentColor   = Color(palette.accentArgb)
        val selRingColor  = MaterialTheme.colorScheme.primary
        val outlineColor  = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

        Box(
            modifier         = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(40.dp)) {
                val w = size.width
                val h = size.height
                val r = w / 2f

                clipPath(Path().apply { addOval(Rect(0f, 0f, w, h)) }) {
                    drawPath(
                        path  = Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, h); close() },
                        color = primaryColor,
                    )
                    drawPath(
                        path  = Path().apply { moveTo(w, 0f); lineTo(w, h); lineTo(0f, h); close() },
                        color = accentColor,
                    )
                }

                val strokePx = if (selected) 3.dp.toPx() else 1.dp.toPx()
                drawCircle(
                    color  = if (selected) selRingColor else outlineColor,
                    radius = r - strokePx / 2f,
                    style  = Stroke(width = strokePx),
                )
            }

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

// ── App icon picker ───────────────────────────────────────────────────────────

private val AppIconChoice.previewBg: Color get() = when (this) {
    AppIconChoice.DEFAULT -> Color(0xFFFFD5CBL)
    AppIconChoice.LEAF    -> Color(0xFFC8E6C9L)
    AppIconChoice.MOON    -> Color(0xFF1A237EL)
    AppIconChoice.STAR    -> Color(0xFF311B92L)
}

private val AppIconChoice.previewFg: Color get() = when (this) {
    AppIconChoice.DEFAULT -> Color(0xFFD9604AL)
    AppIconChoice.LEAF    -> Color(0xFF2E7D32L)
    AppIconChoice.MOON    -> Color(0xFFFFF8E1L)
    AppIconChoice.STAR    -> Color(0xFFFFD740L)
}

private val AppIconChoice.previewIcon: androidx.compose.ui.graphics.vector.ImageVector get() = when (this) {
    AppIconChoice.DEFAULT -> Icons.Filled.WaterDrop
    AppIconChoice.LEAF    -> Icons.Filled.Eco
    AppIconChoice.MOON    -> Icons.Filled.NightsStay
    AppIconChoice.STAR    -> Icons.Filled.AutoAwesome
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppIconPicker(
    currentChoice:     AppIconChoice,
    onSelect:          (AppIconChoice) -> Unit,
    onPickCustomImage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        FlowRow(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconChoiceCell(
                choice   = AppIconChoice.DEFAULT,
                selected = AppIconChoice.DEFAULT == currentChoice,
                onClick  = { onSelect(AppIconChoice.DEFAULT) }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "Discreet icons",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "These don't look like a period app, so GoFlo stays private on your home screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppIconChoice.entries.filter { it != AppIconChoice.DEFAULT }.forEach { choice ->
                IconChoiceCell(
                    choice   = choice,
                    selected = choice == currentChoice,
                    onClick  = { onSelect(choice) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "Your own icon",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Pick any image and GoFlo will create a home-screen shortcut that uses it as the icon. " +
            "The shortcut opens the app normally — you can then hide the original app icon in your launcher's settings.\n\n" +
            "Image requirements: PNG or JPEG · square · 512 × 512 px recommended · transparent background optional.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick  = onPickCustomImage,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create shortcut with custom image…")
        }
    }
}

@Composable
private fun IconChoiceCell(
    choice:   AppIconChoice,
    selected: Boolean,
    onClick:  () -> Unit,
) {
    val bg          = choice.previewBg
    val fg          = choice.previewFg
    val icon        = choice.previewIcon
    val border      = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val borderWidth = if (selected) 2.5.dp else 1.dp

    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .border(borderWidth, border, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = choice.displayName,
                tint               = fg,
                modifier           = Modifier.size(28.dp)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimary,
                        modifier           = Modifier.size(10.dp)
                    )
                }
            }
        }
        Text(
            text  = choice.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Switch row ────────────────────────────────────────────────────────────────

@Composable
private fun SwitchRow(
    label:           String,
    subtitle:        String,
    checked:         Boolean,
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

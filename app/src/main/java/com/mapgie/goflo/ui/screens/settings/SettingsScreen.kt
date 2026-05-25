package com.mapgie.goflo.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mapgie.goflo.AppIconChoice
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Autorenew
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import com.mapgie.goflo.BuildConfig
import com.mapgie.goflo.data.preferences.hasPinSet
import com.mapgie.goflo.data.repository.ImportResult
import com.mapgie.goflo.ui.components.BannerStylePreview
import com.mapgie.goflo.ui.screens.disclaimer.DisclaimerScreen
import com.mapgie.goflo.ui.theme.AppTheme
import com.mapgie.goflo.ui.theme.BannerStyle

// ── Theme mode / palette helpers ──────────────────────────────────────────────

private enum class ThemeMode(val label: String) {
    LIGHT("Light"), DARK("Dark"), SYSTEM("Auto")
}

private enum class StandardPalette(
    val displayName: String,
    val lightTheme: AppTheme,
    val darkTheme: AppTheme,
    val systemTheme: AppTheme,
    /** Primary colour shown in the top-left half of the diagonal swatch. */
    val previewArgb: Long,
    /**
     * Accent (tertiary or secondary) colour shown in the bottom-right half of the
     * diagonal swatch — gives each palette a two-tone preview so the swatches look
     * vibrant rather than flat single-colour circles.
     */
    val accentArgb: Long,
) {
    // Classic
    CORAL        ("Coral",                  AppTheme.CORAL,        AppTheme.CORAL_DARK,        AppTheme.CORAL_SYSTEM,          0xFFC15542L, 0xFFB85C00L), // coral-red + amber
    TEAL         ("Teal",                   AppTheme.TURQUOISE,    AppTheme.TURQUOISE_DARK,    AppTheme.SYSTEM,                0xFF00696FL, 0xFF4E6078L), // deep teal + slate-blue
    SAGE         ("Sage",                   AppTheme.GREEN,        AppTheme.GREEN_DARK,        AppTheme.GREEN_SYSTEM,          0xFF386A20L, 0xFF386669L), // forest-green + teal
    // Fun
    SUMMER_CANDY ("Summer Candy",           AppTheme.SUMMER_CANDY, AppTheme.SUMMER_CANDY_DARK, AppTheme.SUMMER_CANDY_SYSTEM,   0xFFC2185BL, 0xFF006064L), // raspberry + vivid teal
    BEACH_VIBES  ("Beach Vibes",            AppTheme.BEACH_VIBES,  AppTheme.BEACH_VIBES_DARK,  AppTheme.BEACH_VIBES_SYSTEM,    0xFF0D47A1L, 0xFF006064L), // deep ocean-blue + vivid teal
    PEACH_MELBA  ("Peach Melba",            AppTheme.PEACH_MELBA,  AppTheme.PEACH_MELBA_DARK,  AppTheme.PEACH_MELBA_SYSTEM,    0xFFBF360CL, 0xFF33691EL), // vivid terra-cotta + forest green
    DISCO        ("All-Night Disco Party",  AppTheme.DISCO,        AppTheme.DISCO_DARK,        AppTheme.DISCO_SYSTEM,          0xFF7B0EA0L, 0xFF8B6A00L), // deep-violet + disco-gold
    METAL_CHICK  ("Metal Chick",            AppTheme.METAL_CHICK,  AppTheme.METAL_CHICK_DARK,  AppTheme.METAL_CHICK_SYSTEM,    0xFF4A4A5AL, 0xFF6B2D3EL), // charcoal + burgundy
    WHIMSY       ("Whimsy Whispers",        AppTheme.WHIMSY,       AppTheme.WHIMSY_DARK,       AppTheme.WHIMSY_SYSTEM,         0xFF5050A0L, 0xFF2D7A6EL), // periwinkle + mint-teal
    COLOUR_HAPPY ("Colour Me Happy",        AppTheme.COLOUR_HAPPY, AppTheme.COLOUR_HAPPY_DARK, AppTheme.COLOUR_HAPPY_SYSTEM,   0xFFC13A00L, 0xFF1B6FA8L), // coral-orange + electric-blue
    // Bold
    DRAGON_FIRE   ("Dragon Fire",    AppTheme.DRAGON_FIRE,   AppTheme.DRAGON_FIRE_DARK,   AppTheme.DRAGON_FIRE_SYSTEM,   0xFFB71C1CL, 0xFF311B92L), // blood-red + electric-indigo
    MIDNIGHT_NEON ("Midnight Neon",  AppTheme.MIDNIGHT_NEON, AppTheme.MIDNIGHT_NEON_DARK, AppTheme.MIDNIGHT_NEON_SYSTEM, 0xFF6200EAL, 0xFFEA80FCL), // electric-violet + neon-pink
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
    else                                         -> null  // accessibility
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
    else                                                    -> null
}

private val AppTheme.summaryLabel: String get() = when (this) {
    AppTheme.SYSTEM                -> "Teal · Auto"
    AppTheme.CORAL_SYSTEM          -> "Coral · Auto"
    AppTheme.GREEN_SYSTEM          -> "Sage · Auto"
    AppTheme.SUMMER_CANDY_SYSTEM   -> "Summer Candy · Auto"
    AppTheme.BEACH_VIBES_SYSTEM    -> "Beach Vibes · Auto"
    AppTheme.PEACH_MELBA_SYSTEM    -> "Peach Melba · Auto"
    AppTheme.DISCO_SYSTEM          -> "All-Night Disco Party · Auto"
    AppTheme.METAL_CHICK_SYSTEM    -> "Metal Chick · Auto"
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
    AppTheme.METAL_CHICK           -> "Metal Chick · Light"
    AppTheme.METAL_CHICK_DARK      -> "Metal Chick · Dark"
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
    AppTheme.HIGH_CONTRAST_LIGHT   -> "High Contrast Light"
    AppTheme.HIGH_CONTRAST_DARK    -> "High Contrast Dark"
    AppTheme.BLUE_ORANGE           -> "Blue & Orange"
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
    val context    = LocalContext.current
    val prefs      by viewModel.prefs.collectAsState()
    val security   by viewModel.securitySettings.collectAsState()
    val categories by viewModel.trackingCategories.collectAsState()
    val reminder = prefs.reminder
    val currentTheme = runCatching { AppTheme.valueOf(prefs.theme) }.getOrDefault(AppTheme.CORAL)

    val currentBannerStyle = runCatching {
        BannerStyle.valueOf(prefs.bannerStyle)
    }.getOrDefault(BannerStyle.PLAIN)

    val currentIconChoice = runCatching {
        AppIconChoice.valueOf(prefs.iconChoice)
    }.getOrDefault(AppIconChoice.LEAF)

    var showTimePicker        by rememberSaveable { mutableStateOf(false) }
    var showRemovePinDialog   by rememberSaveable { mutableStateOf(false) }
    var customIconError       by remember { mutableStateOf<String?>(null) }
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

    customIconError?.let { message ->
        AlertDialog(
            onDismissRequest = { customIconError = null },
            title            = { Text("Couldn't create shortcut") },
            text             = { Text(message) },
            confirmButton    = { TextButton(onClick = { customIconError = null }) { Text("OK") } }
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

            // ── Medical device disclaimer banner (always visible at top) ─────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
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

            // ═══════════════════════════════════════════════════════════════════
            // TRACKING
            // Core tracking configuration — most users visit this group first
            // ═══════════════════════════════════════════════════════════════════
            SettingsSectionHeader("Tracking")

            // Cycle ────────────────────────────────────────────────────────────
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SwitchRow(
                    label           = "Show period predictions",
                    subtitle        = "Display predicted future period days on the calendar",
                    checked         = prefs.showPeriodPrediction,
                    onCheckedChange = { viewModel.setShowPeriodPrediction(it) }
                )

                SwitchRow(
                    label           = "Show ovulation markers",
                    subtitle        = "Display ovulation day and fertility window on the calendar",
                    checked         = prefs.showOvulationMarkers,
                    onCheckedChange = { viewModel.setShowOvulationMarkers(it) }
                )
            }

            // What You Track — nav card (high-value feature, given prominent nav treatment) ──
            SettingsNavCard(
                title    = "What You Track",
                subtitle = "Customise the symptoms & metrics you log",
                icon     = Icons.Outlined.Tune,
                onClick  = onNavigateToManageCategories
            )

            // One-Tap Quick Log ────────────────────────────────────────────────
            CollapsibleSection(
                title   = "One-Tap Quick Log",
                icon    = Icons.Outlined.TouchApp,
                summary = if (prefs.quickLogCategoryId == -1L) "Tap → Period"
                          else categories.firstOrNull { it.id == prefs.quickLogCategoryId }?.name
                              ?.let { "Tap → $it" } ?: "Tap → Period"
            ) {
                Text(
                    "When you tap the quick-add button, it logs:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                // Log Period option
                FilterChip(
                    selected = prefs.quickLogCategoryId == -1L,
                    onClick  = { viewModel.setQuickLogCategory(-1L) },
                    label    = { Text("Period") },
                    leadingIcon = if (prefs.quickLogCategoryId == -1L) {
                        { Icon(Icons.Default.Check, contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null
                )
                // One chip per tracking category
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

            // ═══════════════════════════════════════════════════════════════════
            // NOTIFICATIONS
            // ═══════════════════════════════════════════════════════════════════
            SettingsSectionHeader("Notifications")

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

            // ═══════════════════════════════════════════════════════════════════
            // PERSONALISATION
            // ═══════════════════════════════════════════════════════════════════
            SettingsSectionHeader("Personalisation")

            CollapsibleSection(
                title   = "Appearance",
                icon    = Icons.Outlined.Palette,
                summary = currentTheme.summaryLabel
            ) {
                CompactThemePicker(
                    current  = currentTheme,
                    onSelect = { viewModel.setTheme(it.name) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outlineVariant
                )

                AppIconPicker(
                    currentChoice       = currentIconChoice,
                    onSelect            = { viewModel.setIconChoice(it) },
                    onPickCustomImage   = { customIconPicker.launch("image/*") }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outlineVariant
                )

                BannerStylePicker(
                    current  = currentBannerStyle,
                    onSelect = { viewModel.setBannerStyle(it.name) }
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // PRIVACY & DATA
            // Security first — then data management
            // ═══════════════════════════════════════════════════════════════════
            SettingsSectionHeader("Privacy & Data")

            // Security & Privacy — icon tint shifts to error colour when unprotected
            CollapsibleSection(
                title    = "Security & Privacy",
                icon     = Icons.Outlined.Lock,
                summary  = securitySummary,
                iconTint = if (!security.hasPinSet) MaterialTheme.colorScheme.error else null
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
            }

            // Data & Backup (renamed from "Data" — title alone was too vague)
            CollapsibleSection(
                title   = "Data & Backup",
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

            // ═══════════════════════════════════════════════════════════════════
            // ABOUT
            // ═══════════════════════════════════════════════════════════════════
            SettingsSectionHeader("About")

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

            // Privacy & Disclaimer — always visible at the very bottom, never buried
            OutlinedButton(
                onClick  = onNavigateToPrivacy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
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

// ── Collapsible section card ──────────────────────────────────────────────────

@Composable
private fun CollapsibleSection(
    title:    String,
    icon:     ImageVector,
    summary:  String,
    iconTint: Color? = null,
    content:  @Composable () -> Unit
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
                        tint               = iconTint ?: MaterialTheme.colorScheme.primary,
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

// ── Section header label ──────────────────────────────────────────────────────

/** Subtle uppercase label that visually separates semantic groups. */
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelMedium,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

// ── Navigation card (forward-chevron rows) ─────────────────────────────────────

/**
 * A tappable card that navigates to another screen.
 * Visually distinct from [CollapsibleSection] — uses a ChevronRight instead of
 * the animated expand/collapse arrow, signalling "go somewhere" not "open here".
 */
@Composable
private fun SettingsNavCard(
    title:   String,
    subtitle: String,
    icon:    ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick  = onClick,
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.titleSmall)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                                ThemeMode.SYSTEM -> {
                                    // Preserve the current palette so "Auto" uses the
                                    // user's chosen colour, not the hardcoded teal default.
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
        val primaryColor  = Color(palette.previewArgb)
        val accentColor   = Color(palette.accentArgb)
        val selRingColor  = MaterialTheme.colorScheme.primary
        val outlineColor  = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

        Box(
            modifier         = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            // Diagonal-split circle: upper-left = primary, lower-right = accent.
            // Shows the palette's two key colours at a glance instead of a flat
            // single-colour disc.
            Canvas(modifier = Modifier.size(40.dp)) {
                val w = size.width
                val h = size.height
                val r = w / 2f

                clipPath(Path().apply { addOval(Rect(0f, 0f, w, h)) }) {
                    // Upper-left triangle — primary (period colour)
                    drawPath(
                        path  = Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, h); close() },
                        color = primaryColor,
                    )
                    // Lower-right triangle — accent (ovulation / tertiary colour)
                    drawPath(
                        path  = Path().apply { moveTo(w, 0f); lineTo(w, h); lineTo(0f, h); close() },
                        color = accentColor,
                    )
                }

                // Selection / outline ring drawn over the fill
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

// ── App icon picker ───────────────────────────────────────────────────────────

/** Preview background colour for each icon choice (matches the adaptive-icon background). */
private val AppIconChoice.previewBg: Color get() = when (this) {
    AppIconChoice.LEAF -> Color(0xFFC8E6C9L)
    AppIconChoice.MOON -> Color(0xFFC5CAE9L)
    AppIconChoice.STAR -> Color(0xFFFFF9C4L)
}

/** Icon foreground / tint colour for each choice. */
private val AppIconChoice.previewFg: Color get() = when (this) {
    AppIconChoice.LEAF -> Color(0xFF2E7D32L)
    AppIconChoice.MOON -> Color(0xFF3949ABL)
    AppIconChoice.STAR -> Color(0xFFF57C00L)
}

private val AppIconChoice.previewIcon: androidx.compose.ui.graphics.vector.ImageVector get() = when (this) {
    AppIconChoice.LEAF -> Icons.Filled.Eco
    AppIconChoice.MOON -> Icons.Filled.NightsStay
    AppIconChoice.STAR -> Icons.Filled.Star
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppIconPicker(
    currentChoice:     AppIconChoice,
    onSelect:          (AppIconChoice) -> Unit,
    onPickCustomImage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ── Shape icons ───────────────────────────────────────────────────────
        Text(
            "These don't look like a period app, so GoFlo stays private on your home screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppIconChoice.entries.forEach { choice ->
                IconChoiceCell(
                    choice   = choice,
                    selected = choice == currentChoice,
                    onClick  = { onSelect(choice) }
                )
            }
        }

        // ── Custom icon ───────────────────────────────────────────────────────
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
    val bg     = choice.previewBg
    val fg     = choice.previewFg
    val icon   = choice.previewIcon
    val border = if (selected) MaterialTheme.colorScheme.primary
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

// ── Banner style picker ───────────────────────────────────────────────────────

/**
 * A row of tappable mini-preview tiles, one per [BannerStyle].
 * Each tile renders a scaled-down canvas preview using live theme colours so the
 * user can see exactly how their active colour scheme looks with each decoration.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BannerStylePicker(current: BannerStyle, onSelect: (BannerStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Banner decoration",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BannerStyle.entries.forEach { style ->
                BannerStyleOption(
                    style    = style,
                    selected = style == current,
                    onClick  = { onSelect(style) },
                )
            }
        }
    }
}

@Composable
private fun BannerStyleOption(
    style:    BannerStyle,
    selected: Boolean,
    onClick:  () -> Unit,
) {
    val selRingColor  = MaterialTheme.colorScheme.primary
    val outlineColor  = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val contentColor  = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier            = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) selRingColor else outlineColor,
                    shape = RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            BannerStylePreview(style = style)

            if (selected) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint               = contentColor,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text  = style.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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

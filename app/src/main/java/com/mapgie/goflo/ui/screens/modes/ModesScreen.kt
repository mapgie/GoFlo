package com.mapgie.goflo.ui.screens.modes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.util.AppMode
import com.mapgie.goflo.ui.util.ModeFeature
import com.mapgie.goflo.ui.util.SuggestedCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModesScreen(
    viewModel: ModesViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    var pendingActivate  by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeactivate by rememberSaveable { mutableStateOf<String?>(null) }

    val modeToActivate   = pendingActivate?.let { AppMode.fromId(it) }
    val modeToDeactivate = pendingDeactivate?.let { AppMode.fromId(it) }

    // ── Deactivation dialog ───────────────────────────────────────────────────

    if (modeToDeactivate != null) {
        AlertDialog(
            onDismissRequest = { pendingDeactivate = null },
            title = { Text("Turn off ${modeToDeactivate.displayName}?") },
            text  = {
                Text(
                    "The categories added by this mode will stay in your list. " +
                    "You can archive them from Manage if you no longer need them."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deactivateMode(modeToDeactivate)
                    pendingDeactivate = null
                }) { Text("Turn off") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeactivate = null }) { Text("Cancel") }
            }
        )
    }

    // ── Activation sheet ──────────────────────────────────────────────────────

    if (modeToActivate != null) {
        ModeActivationSheet(
            mode                  = modeToActivate,
            existingModeKeys      = state.existingModeKeys,
            defaultTempCelsius    = state.temperatureUnitCelsius,
            onConfirm             = { selected, dateStr, startType, celsius ->
                viewModel.activateMode(
                    mode                   = modeToActivate,
                    selected               = selected,
                    pregnancyDateStr       = dateStr,
                    pregnancyStartType     = startType,
                    temperatureUnitCelsius = celsius,
                )
                pendingActivate = null
            },
            onDismiss = { pendingActivate = null },
        )
    }

    // ── Main list ─────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tracking Modes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor      = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Each mode suggests a set of tracking categories tailored to your needs. " +
                    "You choose which ones to add. Multiple modes can be active at once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(AppMode.entries, key = { it.id }) { mode ->
                val isActive = mode in state.activeModes
                ModeCard(
                    mode     = mode,
                    isActive = isActive,
                    onClick  = {
                        if (isActive) pendingDeactivate = mode.id
                        else          pendingActivate   = mode.id
                    }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Mode card ─────────────────────────────────────────────────────────────────

@Composable
private fun ModeCard(
    mode: AppMode,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isActive)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                stateDescription = if (isActive) "Active" else "Inactive"
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = mode.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isActive) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector        = Icons.Outlined.CheckCircle,
                    contentDescription = "Active",
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ── Activation bottom sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeActivationSheet(
    mode: AppMode,
    existingModeKeys: Set<String>,
    defaultTempCelsius: Boolean,
    onConfirm: (selected: List<SuggestedCategory>, dateStr: String, startType: String, celsius: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Checkbox state: modeKey -> checked
    val checked = remember {
        mutableStateMapOf<String, Boolean>().also { map ->
            mode.suggestedCategories.forEach { cat ->
                map[cat.modeKey] = cat.defaultChecked
            }
        }
    }

    // Pregnancy-specific state
    var pregnancyDateStr  by rememberSaveable { mutableStateOf("") }
    var pregnancyStartType by rememberSaveable { mutableStateOf("EDD") }

    // Fertility temperature unit
    var celsius by rememberSaveable { mutableStateOf(defaultTempCelsius) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Text(
                text  = "Set up ${mode.displayName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Features section
            if (mode.features.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "This mode enables:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                mode.features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(feature.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Temperature unit picker (Fertility only)
            if (mode == AppMode.FERTILITY) {
                HorizontalDivider()
                Text(
                    "Temperature unit:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected  = celsius,
                        onClick   = { celsius = true },
                        shape     = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label     = { Text("Celsius (°C)") },
                    )
                    SegmentedButton(
                        selected  = !celsius,
                        onClick   = { celsius = false },
                        shape     = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label     = { Text("Fahrenheit (°F)") },
                    )
                }
                // Adjust the BBT range label for display
                val rangeNote = if (celsius) "Typical range: 35.0 - 38.0 °C"
                                else         "Typical range: 96.0 - 100.0 °F"
                Text(
                    rangeNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Pregnancy date entry (Pregnancy only)
            if (mode == AppMode.PREGNANCY) {
                HorizontalDivider()
                Text(
                    "Your pregnancy date:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = pregnancyStartType == "EDD",
                        onClick  = { pregnancyStartType = "EDD" },
                        shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label    = { Text("Due date") },
                    )
                    SegmentedButton(
                        selected = pregnancyStartType == "LMP",
                        onClick  = { pregnancyStartType = "LMP" },
                        shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label    = { Text("Last period") },
                    )
                }
                val label = if (pregnancyStartType == "EDD") "Expected due date (YYYY-MM-DD)"
                            else                              "First day of last period (YYYY-MM-DD)"
                OutlinedTextField(
                    value         = pregnancyDateStr,
                    onValueChange = { pregnancyDateStr = it },
                    label         = { Text(label) },
                    placeholder   = { Text("e.g. 2026-12-01") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "You can update this later in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Category checklist
            if (mode.suggestedCategories.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Categories to add:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                mode.suggestedCategories.forEach { cat ->
                    val alreadyAdded = cat.modeKey in existingModeKeys
                    val isChecked    = checked[cat.modeKey] ?: cat.defaultChecked

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        ListItem(
                            headlineContent   = { Text(cat.name, fontWeight = if (alreadyAdded) FontWeight.Normal else FontWeight.Medium) },
                            supportingContent = {
                                Text(
                                    if (alreadyAdded) "Already in your categories" else cat.description,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked         = alreadyAdded || isChecked,
                                    onCheckedChange = null,
                                    enabled         = !alreadyAdded,
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            modifier = if (alreadyAdded) Modifier else Modifier
                                .semantics { role = Role.Checkbox }
                                .toggleable(
                                    value    = isChecked,
                                    onValueChange = { checked[cat.modeKey] = it },
                                ),
                        )
                    }
                }
            }

            HorizontalDivider()

            Button(
                onClick = {
                    val selected = mode.suggestedCategories.filter { cat ->
                        cat.modeKey !in existingModeKeys && (checked[cat.modeKey] ?: cat.defaultChecked)
                    }
                    onConfirm(selected, pregnancyDateStr, pregnancyStartType, celsius)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Turn on ${mode.displayName}")
            }
        }
    }
}

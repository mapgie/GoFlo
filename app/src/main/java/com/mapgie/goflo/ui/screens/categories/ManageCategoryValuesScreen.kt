package com.mapgie.goflo.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoryValuesScreen(
    viewModel: ManageCategoryValuesViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var showAddValue       by rememberSaveable { mutableStateOf(false) }
    var showRenameCategory by rememberSaveable { mutableStateOf(false) }
    var renamingValue      by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteValue by rememberSaveable { mutableStateOf<Long?>(null) }

    val valueToRename = state.values.firstOrNull { it.id == renamingValue }
    val valueToDelete = state.values.firstOrNull { it.id == pendingDeleteValue }

    if (showAddValue) {
        AddValueDialog(
            categoryName   = state.category?.name ?: "",
            existingLabels = state.values.map { it.label },
            onAdd = { label ->
                viewModel.addValue(label)
                showAddValue = false
            },
            onDismiss = { showAddValue = false }
        )
    }

    if (showRenameCategory && state.category != null) {
        RenameCategoryDialog(
            currentName = state.category!!.name,
            onRename = { newName ->
                viewModel.renameCategory(newName)
                showRenameCategory = false
            },
            onDismiss = { showRenameCategory = false }
        )
    }

    if (valueToRename != null) {
        RenameValueDialog(
            value = valueToRename,
            onRename = { newLabel, fixHistorical ->
                viewModel.renameValue(valueToRename, newLabel, fixHistorical)
                renamingValue = null
            },
            onDismiss = { renamingValue = null }
        )
    }

    if (valueToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteValue = null },
            title = { Text("Remove \"${valueToDelete.label}\"?") },
            text = {
                Text(
                    "\"${valueToDelete.label}\" will no longer appear as an option for new " +
                    "entries. Past entries that used it are preserved and will be shown as a " +
                    "removed value."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteValue(valueToDelete)
                    pendingDeleteValue = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteValue = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.category?.name ?: "Category")
                        if (state.category?.isSystem == false) {
                            IconButton(onClick = { showRenameCategory = true }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Rename category",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val category = state.category
        when (category?.categoryType) {
            "numeric_slider" -> NumericSliderSettings(
                category  = category,
                modifier  = Modifier.padding(padding),
                onSave    = { min, max, decimals, unit ->
                    viewModel.updateNumericSettings(min, max, decimals, unit)
                }
            )
            "numeric_free" -> NumericFreeSettings(
                category = category,
                modifier = Modifier.padding(padding),
                onSave   = { unit -> viewModel.updateUnit(unit) }
            )
            else -> DefaultCategoryValues(
                state             = state,
                modifier          = Modifier.padding(padding),
                onAddValue        = { showAddValue = true },
                onRenameValue     = { renamingValue = it.id },
                onDeleteValue     = { pendingDeleteValue = it.id }
            )
        }
    }
}

// ── Default (text values) content ─────────────────────────────────────────────

@Composable
private fun DefaultCategoryValues(
    state: ManageCategoryValuesUiState,
    modifier: Modifier,
    onAddValue: () -> Unit,
    onRenameValue: (TrackingValue) -> Unit,
    onDeleteValue: (TrackingValue) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Values in this category:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.values.isEmpty()) {
            Text(
                "No values yet. Add some below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.values, key = { it.id }) { value ->
                    ValueRow(
                        value    = value,
                        onRename = { onRenameValue(value) },
                        onDelete = { onDeleteValue(value) }
                    )
                }
            }
        }

        HorizontalDivider()

        Button(onClick = onAddValue, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add a value")
        }
    }
}

// ── Numeric slider settings content ──────────────────────────────────────────

@Composable
private fun NumericSliderSettings(
    category: TrackingCategory,
    modifier: Modifier,
    onSave: (min: Float, max: Float, allowDecimals: Boolean, unit: String) -> Unit,
) {
    var minText       by rememberSaveable {
        mutableStateOf(
            if (category.allowDecimals) "%.1f".format(category.numericMin)
            else category.numericMin.toInt().toString()
        )
    }
    var maxText       by rememberSaveable {
        mutableStateOf(
            if (category.allowDecimals) "%.1f".format(category.numericMax)
            else category.numericMax.toInt().toString()
        )
    }
    var allowDecimals by rememberSaveable { mutableStateOf(category.allowDecimals) }
    var unit          by rememberSaveable { mutableStateOf(category.numericUnit) }

    val canSave by remember(minText, maxText) {
        derivedStateOf {
            minText.toFloatOrNull() != null && maxText.toFloatOrNull() != null &&
            (minText.toFloatOrNull() ?: 0f) < (maxText.toFloatOrNull() ?: 10f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Numeric Slider settings",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value         = unit,
            onValueChange = { unit = it },
            label         = { Text("Unit / Key (optional)") },
            placeholder   = { Text("e.g. °C, bpm, kg…") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value           = minText,
                onValueChange   = { minText = it },
                label           = { Text("Min") },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier        = Modifier.weight(1f)
            )
            OutlinedTextField(
                value           = maxText,
                onValueChange   = { maxText = it },
                label           = { Text("Max") },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier        = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Allow decimals", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Slider snaps to 0.1 steps instead of whole numbers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = allowDecimals, onCheckedChange = { allowDecimals = it })
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick  = {
                if (canSave) onSave(
                    minText.toFloatOrNull() ?: 0f,
                    maxText.toFloatOrNull() ?: 10f,
                    allowDecimals,
                    unit.trim()
                )
            },
            enabled  = canSave,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}

// ── Numeric free-input settings content ──────────────────────────────────────

@Composable
private fun NumericFreeSettings(
    category: TrackingCategory,
    modifier: Modifier,
    onSave: (unit: String) -> Unit,
) {
    var unit by rememberSaveable { mutableStateOf(category.numericUnit) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Numeric Input settings",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value         = unit,
            onValueChange = { unit = it },
            label         = { Text("Unit / Key (optional)") },
            placeholder   = { Text("e.g. °C, bpm, kg…") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth()
        )

        Button(
            onClick  = { onSave(unit.trim()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}

// ── Shared value row ──────────────────────────────────────────────────────────

@Composable
private fun ValueRow(
    value: TrackingValue,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = value.label,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Row {
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename ${value.label}",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${value.label}",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun AddValueDialog(
    categoryName: String,
    existingLabels: List<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    val trimmed = text.trim()
    val alreadyExists = existingLabels.any { it.equals(trimmed, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add value to $categoryName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    label         = { Text("Value") },
                    placeholder   = { Text("e.g. Happy, Calm, Tired…") },
                    singleLine    = true,
                    isError       = alreadyExists,
                    supportingText = if (alreadyExists) ({ Text("Already exists") }) else null,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (trimmed.isNotBlank() && !alreadyExists) onAdd(trimmed) },
                enabled  = trimmed.isNotBlank() && !alreadyExists
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RenameCategoryDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename category") },
        text = {
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Category name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.isNotBlank()) onRename(name) },
                enabled  = name.isNotBlank() && name != currentName
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RenameValueDialog(
    value: TrackingValue,
    onRename: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var newLabel by rememberSaveable { mutableStateOf(value.label) }
    val trimmed = newLabel.trim()
    val changed = trimmed.isNotBlank() && trimmed != value.label

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename \"${value.label}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = newLabel,
                    onValueChange = { newLabel = it },
                    label         = { Text("New label") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                if (changed) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "How should past entries be handled?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (changed) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = { onRename(trimmed, true) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Fix everywhere")
                    }
                    TextButton(onClick = { onRename(trimmed, false) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Rename option only")
                    }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (changed) TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

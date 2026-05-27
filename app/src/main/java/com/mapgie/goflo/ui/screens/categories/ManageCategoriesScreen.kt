package com.mapgie.goflo.ui.screens.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.ui.util.CATEGORY_COLOR_OPTIONS
import com.mapgie.goflo.ui.util.CategoryColor
import com.mapgie.goflo.ui.util.CategoryIcon
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor
import com.mapgie.goflo.ui.util.toHexColorKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: ManageCategoriesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var showAddDialog         by rememberSaveable { mutableStateOf(false) }
    var pendingDelete         by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingEditAppearance by rememberSaveable { mutableStateOf<Long?>(null) }

    val categoryToDelete         = state.categories.firstOrNull { it.id == pendingDelete }
    val categoryToEditAppearance = state.categories.firstOrNull { it.id == pendingEditAppearance }

    // ── Add category dialog ───────────────────────────────────────────────────

    if (showAddDialog) {
        AddCategoryDialog(
            onAdd = { name, iconName, colorToken, isNumeric, numericMin, numericMax, allowDecimals ->
                viewModel.addCategory(name, iconName, colorToken, isNumeric, numericMin, numericMax, allowDecimals)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // ── Edit appearance dialog ────────────────────────────────────────────────

    if (categoryToEditAppearance != null) {
        EditAppearanceDialog(
            category = categoryToEditAppearance,
            onSave = { name, iconName, colorToken, isNumeric, numericMin, numericMax, allowDecimals ->
                viewModel.updateCategoryNameAndAppearance(
                    categoryToEditAppearance.id, name, iconName, colorToken,
                    isNumeric, numericMin, numericMax, allowDecimals
                )
                pendingEditAppearance = null
            },
            onDismiss = { pendingEditAppearance = null }
        )
    }

    // ── Delete confirmation ───────────────────────────────────────────────────

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${categoryToDelete.name}\"?") },
            text = {
                Text(
                    "This will permanently remove the ${categoryToDelete.name} category and all " +
                    "log entries recorded for it. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(categoryToDelete)
                        pendingDelete = null
                    }
                ) { Text("Delete Everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tracking Categories") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        }
    ) { padding ->
        if (state.categories.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No tracking categories yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap + to add your first category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.categories, key = { it.id }) { category ->
                    CategoryRow(
                        category         = category,
                        onClick          = { onNavigateToCategory(category.id) },
                        onEditAppearance = { pendingEditAppearance = category.id },
                        onDelete         = { pendingDelete = category.id }
                    )
                }
            }
        }
    }
}

// ── Category row ──────────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(
    category: TrackingCategory,
    onClick: () -> Unit,
    onEditAppearance: () -> Unit,
    onDelete: () -> Unit
) {
    val bubbleColor = category.colorToken.toCategoryColor()
    val iconTint    = category.colorToken.toCategoryOnColor()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bubbleColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = category.iconName.toCategoryIcon().vector,
                    contentDescription = null,
                    tint               = iconTint,
                    modifier           = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text  = category.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text  = buildString {
                        if (category.isSystem) append("Built-in · ")
                        if (category.isNumeric) {
                            val rangeStr = if (category.allowDecimals)
                                "%.1f – %.1f".format(category.numericMin, category.numericMax)
                            else
                                "${category.numericMin.toInt()} – ${category.numericMax.toInt()}"
                            append("Numeric ($rangeStr) · tap to edit range")
                        } else {
                            append("Tap to manage values")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEditAppearance) {
                Icon(
                    imageVector        = Icons.Outlined.Palette,
                    contentDescription = "Edit icon & colour",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(20.dp)
                )
            }

            if (!category.isSystem) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Delete ${category.name}",
                        tint               = MaterialTheme.colorScheme.error,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Add category dialog ───────────────────────────────────────────────────────

@Composable
private fun AddCategoryDialog(
    onAdd: (name: String, iconName: String, colorToken: String,
            isNumeric: Boolean, numericMin: Float, numericMax: Float, allowDecimals: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name            by rememberSaveable { mutableStateOf("") }
    var selectedIconKey by rememberSaveable { mutableStateOf(CategoryIcon.CATEGORY.key) }
    var selectedToken   by rememberSaveable { mutableStateOf(CategoryColor.SECONDARY.key) }
    var isNumeric       by rememberSaveable { mutableStateOf(false) }
    var minText         by rememberSaveable { mutableStateOf("0") }
    var maxText         by rememberSaveable { mutableStateOf("10") }
    var allowDecimals   by rememberSaveable { mutableStateOf(false) }

    val canAdd by remember(name, isNumeric, minText, maxText) {
        derivedStateOf {
            name.isNotBlank() && (!isNumeric || (
                minText.toFloatOrNull() != null && maxText.toFloatOrNull() != null &&
                (minText.toFloatOrNull() ?: 0f) < (maxText.toFloatOrNull() ?: 10f)
            ))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("New Category", style = MaterialTheme.typography.headlineSmall)

                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    placeholder   = { Text("e.g. Mood, Sleep, Exercise…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryIconGrid(selectedKey = selectedIconKey, onSelect = { selectedIconKey = it })

                Text("Colour", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryColorPicker(selectedToken = selectedToken, onSelect = { selectedToken = it })

                HorizontalDivider()
                NumericSettingsSection(
                    isNumeric     = isNumeric,
                    onToggle      = { isNumeric = it },
                    minText       = minText,
                    onMinChange   = { minText = it },
                    maxText       = maxText,
                    onMaxChange   = { maxText = it },
                    allowDecimals = allowDecimals,
                    onDecimalsToggle = { allowDecimals = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (canAdd) onAdd(
                                name, selectedIconKey, selectedToken,
                                isNumeric,
                                minText.toFloatOrNull() ?: 0f,
                                maxText.toFloatOrNull() ?: 10f,
                                allowDecimals
                            )
                        },
                        enabled = canAdd
                    ) { Text("Add") }
                }
            }
        }
    }
}

// ── Edit appearance dialog ────────────────────────────────────────────────────

@Composable
private fun EditAppearanceDialog(
    category: TrackingCategory,
    onSave: (name: String, iconName: String, colorToken: String,
             isNumeric: Boolean, numericMin: Float, numericMax: Float, allowDecimals: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name            by rememberSaveable { mutableStateOf(category.name) }
    var selectedIconKey by rememberSaveable { mutableStateOf(category.iconName) }
    var selectedToken   by rememberSaveable { mutableStateOf(category.colorToken) }
    var isNumeric       by rememberSaveable { mutableStateOf(category.isNumeric) }
    var minText         by rememberSaveable {
        mutableStateOf(if (category.allowDecimals) "%.1f".format(category.numericMin) else category.numericMin.toInt().toString())
    }
    var maxText         by rememberSaveable {
        mutableStateOf(if (category.allowDecimals) "%.1f".format(category.numericMax) else category.numericMax.toInt().toString())
    }
    var allowDecimals   by rememberSaveable { mutableStateOf(category.allowDecimals) }

    val previewBubble = selectedToken.toCategoryColor()
    val previewIcon   = selectedToken.toCategoryOnColor()

    val canSave by remember(name, isNumeric, minText, maxText) {
        derivedStateOf {
            name.isNotBlank() && (!isNumeric || (
                minText.toFloatOrNull() != null && maxText.toFloatOrNull() != null &&
                (minText.toFloatOrNull() ?: 0f) < (maxText.toFloatOrNull() ?: 10f)
            ))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(previewBubble),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = selectedIconKey.toCategoryIcon().vector,
                            contentDescription = null,
                            tint               = previewIcon,
                            modifier           = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text  = "Customise",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = name.ifBlank { category.name },
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryIconGrid(selectedKey = selectedIconKey, onSelect = { selectedIconKey = it })

                Text("Colour", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryColorPicker(selectedToken = selectedToken, onSelect = { selectedToken = it })

                HorizontalDivider()
                if (!category.isSystem) {
                    NumericSettingsSection(
                        isNumeric        = isNumeric,
                        onToggle         = { isNumeric = it },
                        minText          = minText,
                        onMinChange      = { minText = it },
                        maxText          = maxText,
                        onMaxChange      = { maxText = it },
                        allowDecimals    = allowDecimals,
                        onDecimalsToggle = { allowDecimals = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (canSave) onSave(
                                name, selectedIconKey, selectedToken,
                                isNumeric,
                                minText.toFloatOrNull() ?: 0f,
                                maxText.toFloatOrNull() ?: 10f,
                                allowDecimals
                            )
                        },
                        enabled = canSave
                    ) { Text("Save") }
                }
            }
        }
    }
}

// ── Numeric settings section ──────────────────────────────────────────────────

@Composable
private fun NumericSettingsSection(
    isNumeric: Boolean,
    onToggle: (Boolean) -> Unit,
    minText: String,
    onMinChange: (String) -> Unit,
    maxText: String,
    onMaxChange: (String) -> Unit,
    allowDecimals: Boolean,
    onDecimalsToggle: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Numeric input", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Use a slider instead of text options",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = isNumeric, onCheckedChange = onToggle)
        }

        AnimatedVisibility(
            visible = isNumeric,
            enter = expandVertically() + fadeIn(),
            exit  = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value         = minText,
                        onValueChange = { onMinChange(it) },
                        label         = { Text("Min") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier      = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value         = maxText,
                        onValueChange = { onMaxChange(it) },
                        label         = { Text("Max") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier      = Modifier.weight(1f)
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
                    Switch(checked = allowDecimals, onCheckedChange = onDecimalsToggle)
                }
            }
        }
    }
}

// ── Shared picker components ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryIconGrid(selectedKey: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement   = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow     = 5,
    ) {
        CategoryIcon.entries.forEach { icon ->
            val isSelected = icon.key == selectedKey
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(icon.key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon.vector,
                    contentDescription = icon.displayName,
                    tint               = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(26.dp)
                )
            }
        }
    }
}

private fun isCustomColorToken(token: String): Boolean {
    if (token.length != 8) return false
    val categoryColorKeys = CategoryColor.entries.map { it.key }.toSet()
    if (token in categoryColorKeys) return false
    val extendedHexKeys = CATEGORY_COLOR_OPTIONS.map { it.toHexColorKey() }.toSet()
    return token !in extendedHexKeys
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryColorPicker(selectedToken: String, onSelect: (String) -> Unit) {
    var showFullPicker by rememberSaveable { mutableStateOf(false) }

    val hasCustomColor by remember(selectedToken) {
        derivedStateOf { isCustomColorToken(selectedToken) }
    }

    if (showFullPicker) {
        val initialColor = if (hasCustomColor) {
            runCatching { android.graphics.Color.parseColor("#$selectedToken") }
                .getOrDefault(android.graphics.Color.RED)
        } else {
            android.graphics.Color.RED
        }
        FullColorPickerDialog(
            initialColor = initialColor,
            onDismiss = { showFullPicker = false },
            onColorSelected = { hexKey ->
                onSelect(hexKey)
                showFullPicker = false
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            CategoryColor.entries.forEach { colorOption ->
                val isSelected    = colorOption.key == selectedToken
                val swatchColor   = colorOption.key.toCategoryColor()
                val onSwatchColor = colorOption.key.toCategoryOnColor()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .then(
                                if (isSelected)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                else Modifier
                            )
                            .clickable { onSelect(colorOption.key) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector        = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint               = onSwatchColor,
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text  = colorOption.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text  = "More colours",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow     = 6,
        ) {
            CATEGORY_COLOR_OPTIONS.forEach { argb ->
                val hexKey        = argb.toHexColorKey()
                val isSelected    = hexKey == selectedToken
                val swatchColor   = Color(argb)
                val onSwatchColor = if (swatchColor.luminance() > 0.35f) Color(0xFF1C1B1F) else Color.White

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .then(
                            if (isSelected)
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                        .clickable { onSelect(hexKey) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint               = onSwatchColor,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outline
            if (hasCustomColor) {
                val customArgb = runCatching { selectedToken.toLong(16).toInt() }.getOrDefault(0)
                val customColor = Color(customArgb)
                val onCustomColor = if (customColor.luminance() > 0.35f) Color(0xFF1C1B1F) else Color.White
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(customColor)
                        .border(3.dp, primaryColor, CircleShape)
                        .clickable { showFullPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = "Custom colour selected",
                        tint               = onCustomColor,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { showFullPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(38.dp)) {
                        val strokePx = 2.dp.toPx()
                        val radius = (size.minDimension / 2f) - strokePx / 2f
                        drawCircle(
                            color  = outlineColor,
                            radius = radius,
                            style  = Stroke(
                                width      = strokePx,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(6f, 4f), 0f
                                )
                            )
                        )
                    }
                    Icon(
                        imageVector        = Icons.Default.Add,
                        contentDescription = "Pick custom colour",
                        tint               = outlineColor,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Full HSV colour picker dialog ─────────────────────────────────────────────

@Composable
private fun FullColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val initHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor, initHsv)

    var hue        by remember { mutableStateOf(initHsv[0]) }
    var saturation by remember { mutableStateOf(initHsv[1]) }
    var value      by remember { mutableStateOf(initHsv[2]) }

    val currentArgb by remember(hue, saturation, value) {
        derivedStateOf {
            android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        }
    }
    var hexInput by remember(currentArgb) {
        mutableStateOf("%06X".format(currentArgb and 0xFFFFFF))
    }
    var hexError by remember { mutableStateOf(false) }

    fun applyHexInput(input: String) {
        hexInput = input.uppercase().filter { it.isLetterOrDigit() }.take(6)
        if (hexInput.length == 6) {
            runCatching {
                val parsed = android.graphics.Color.parseColor("#$hexInput")
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(parsed, hsv)
                hue        = hsv[0]
                saturation = hsv[1]
                value      = hsv[2]
                hexError   = false
            }.onFailure { hexError = true }
        } else {
            hexError = hexInput.isNotEmpty()
        }
    }

    val previewColor = Color(currentArgb or (0xFF shl 24))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Custom Colour", style = MaterialTheme.typography.headlineSmall)

                SaturationValuePanel(
                    hue        = hue,
                    saturation = saturation,
                    value      = value,
                    onChanged  = { s, v -> saturation = s; value = v }
                )

                HueSlider(
                    hue       = hue,
                    onChanged = { hue = it }
                )

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                    )
                    OutlinedTextField(
                        value         = hexInput,
                        onValueChange = { applyHexInput(it) },
                        label         = { Text("Hex") },
                        prefix        = { Text("#") },
                        singleLine    = true,
                        isError       = hexError,
                        modifier      = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val argb  = currentArgb
                        val hexKey = "FF%06X".format(argb and 0xFFFFFF)
                        onColorSelected(hexKey)
                    }) { Text("Done") }
                }
            }
        }
    }
}

// ── Saturation/Value panel ────────────────────────────────────────────────────

@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChanged: (saturation: Float, value: Float) -> Unit
) {
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    fun updateFromOffset(offset: Offset) {
                        val s = (offset.x / w).coerceIn(0f, 1f)
                        val v = (1f - offset.y / h).coerceIn(0f, 1f)
                        onChanged(s, v)
                    }
                    updateFromOffset(down.position)
                    drag(down.id) { change ->
                        updateFromOffset(change.position)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(listOf(Color.White, hueColor))
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                )
        )
        val thumbX = saturation
        val thumbY = 1f - value
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = thumbX * size.width
            val cy = thumbY * size.height
            drawCircle(
                color  = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(cx, cy),
                style  = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color  = Color.Black,
                radius = 12.dp.toPx(),
                center = Offset(cx, cy),
                style  = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

// ── Hue slider ────────────────────────────────────────────────────────────────

@Composable
private fun HueSlider(
    hue: Float,
    onChanged: (Float) -> Unit
) {
    val hueColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFF8000),
            Color(0xFFFFFF00),
            Color(0xFF80FF00),
            Color(0xFF00FF00),
            Color(0xFF00FF80),
            Color(0xFF00FFFF),
            Color(0xFF0080FF),
            Color(0xFF0000FF),
            Color(0xFF8000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0080),
            Color(0xFFFF0000),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(hueColors))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun updateFromOffset(offset: Offset) {
                        val h = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                        onChanged(h)
                    }
                    updateFromOffset(down.position)
                    drag(down.id) { change ->
                        updateFromOffset(change.position)
                    }
                }
            }
    ) {
        val thumbX = hue / 360f
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = thumbX * size.width
            val cy = size.height / 2f
            drawCircle(
                color  = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(cx, cy),
                style  = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color  = Color.Black,
                radius = 12.dp.toPx(),
                center = Offset(cx, cy),
                style  = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

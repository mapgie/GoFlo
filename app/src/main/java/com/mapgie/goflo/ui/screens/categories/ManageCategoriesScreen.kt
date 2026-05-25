package com.mapgie.goflo.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
            onAdd = { name, iconName, colorToken ->
                viewModel.addCategory(name, iconName, colorToken)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // ── Edit appearance dialog ────────────────────────────────────────────────

    if (categoryToEditAppearance != null) {
        EditAppearanceDialog(
            category = categoryToEditAppearance,
            onSave = { iconName, colorToken ->
                viewModel.updateCategoryAppearance(categoryToEditAppearance.id, iconName, colorToken)
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
    // Resolve theme-relative colours here so Modifier.background can use them
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
            // Coloured icon bubble — resolves from the current theme
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
                    text  = if (category.isSystem) "Built-in · tap to manage values"
                            else "Tap to manage values",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Edit appearance (all categories)
            IconButton(onClick = onEditAppearance) {
                Icon(
                    imageVector        = Icons.Outlined.Palette,
                    contentDescription = "Edit icon & colour",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(20.dp)
                )
            }

            // Delete (custom categories only)
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
    onAdd: (name: String, iconName: String, colorToken: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name            by rememberSaveable { mutableStateOf("") }
    var selectedIconKey by rememberSaveable { mutableStateOf(CategoryIcon.CATEGORY.key) }
    var selectedToken   by rememberSaveable { mutableStateOf(CategoryColor.SECONDARY.key) }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onAdd(name, selectedIconKey, selectedToken) },
                        enabled = name.isNotBlank()
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
    onSave: (iconName: String, colorToken: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIconKey by rememberSaveable { mutableStateOf(category.iconName) }
    var selectedToken   by rememberSaveable { mutableStateOf(category.colorToken) }

    val previewBubble = selectedToken.toCategoryColor()
    val previewIcon   = selectedToken.toCategoryOnColor()

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
                // Live preview
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
                            text  = category.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }

                HorizontalDivider()

                Text("Icon", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryIconGrid(selectedKey = selectedIconKey, onSelect = { selectedIconKey = it })

                Text("Colour", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryColorPicker(selectedToken = selectedToken, onSelect = { selectedToken = it })

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(selectedIconKey, selectedToken) }) { Text("Save") }
                }
            }
        }
    }
}

// ── Shared picker components ──────────────────────────────────────────────────

/** Grid of all [CategoryIcon] options; highlights the currently selected one. */
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

/**
 * Two-section colour picker:
 *
 * 1. **Themed** — four labelled swatches from [MaterialTheme.colorScheme] that
 *    follow the user's chosen palette and light/dark mode automatically.
 *
 * 2. **More colours** — twelve fixed-ARGB swatches for users who track more
 *    than four things and want distinct colours beyond the theme slots.
 *
 * [selectedToken] is either a [CategoryColor] key ("primary", …) or an 8-char
 * uppercase hex string produced by [Int.toHexColorKey] ("FFE53935", …).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryColorPicker(selectedToken: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── Themed swatches ───────────────────────────────────────────────────
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

        // ── Extended palette ──────────────────────────────────────────────────
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
                // Luminance check: choose black or white icon tint without MaterialTheme
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
        }
    }
}

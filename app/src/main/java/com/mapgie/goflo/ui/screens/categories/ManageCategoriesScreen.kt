package com.mapgie.goflo.ui.screens.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.ui.util.CATEGORY_COLOR_OPTIONS
import com.mapgie.goflo.ui.util.CategoryColor
import com.mapgie.goflo.ui.util.CategoryIcon
import com.mapgie.goflo.ui.util.CategoryType
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
    var pendingArchive        by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingEditAppearance by rememberSaveable { mutableStateOf<Long?>(null) }

    val categoryToDelete         = state.categories.firstOrNull { it.id == pendingDelete }
    val categoryToArchive        = state.categories.firstOrNull { it.id == pendingArchive }
    val categoryToEditAppearance = state.categories.firstOrNull { it.id == pendingEditAppearance }

    fun requestArchive(category: TrackingCategory) {
        if (!category.isArchived && state.archiveWarningDisabled) {
            viewModel.archiveCategory(category)
        } else {
            pendingArchive = category.id
        }
    }

    // ── Add category dialog ───────────────────────────────────────────────────

    if (showAddDialog) {
        AddCategoryDialog(
            onAdd = { name, iconName, colorToken, categoryType, numericMin, numericMax, allowDecimals, numericUnit, allowMultiple, showInLogPeriod ->
                viewModel.addCategory(
                    name            = name,
                    iconName        = iconName,
                    colorToken      = colorToken,
                    categoryType    = categoryType,
                    numericMin      = numericMin,
                    numericMax      = numericMax,
                    allowDecimals   = allowDecimals,
                    numericUnit     = numericUnit,
                    allowMultiple   = allowMultiple,
                    showInLogPeriod = showInLogPeriod,
                    onCreated    = { newId ->
                        showAddDialog = false
                        // Numeric categories have all settings configured in the creation
                        // dialog; navigating to the values screen would only confuse the
                        // user with a redundant "Save" prompt. Default categories need to
                        // go there so the user can add their value options.
                        if (categoryType == CategoryType.DEFAULT.key) {
                            onNavigateToCategory(newId)
                        }
                    }
                )
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // ── Edit appearance dialog ────────────────────────────────────────────────

    if (categoryToEditAppearance != null) {
        EditAppearanceDialog(
            category = categoryToEditAppearance,
            onSave = { iconName, colorToken ->
                viewModel.updateCategoryAppearance(
                    id         = categoryToEditAppearance.id,
                    iconName   = iconName,
                    colorToken = colorToken,
                )
                pendingEditAppearance = null
            },
            onDismiss = { pendingEditAppearance = null }
        )
    }

    // ── Archive confirmation ──────────────────────────────────────────────────

    if (categoryToArchive != null) {
        if (categoryToArchive.isArchived) {
            AlertDialog(
                onDismissRequest = { pendingArchive = null },
                title = { Text("Unarchive \"${categoryToArchive.name}\"?") },
                text = { Text("${categoryToArchive.name} will be restored to your active tracking categories.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.unarchiveCategory(categoryToArchive)
                        pendingArchive = null
                    }) { Text("Unarchive") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingArchive = null }) { Text("Cancel") }
                }
            )
        } else {
            var doNotShowAgain by rememberSaveable { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { pendingArchive = null },
                title = { Text("Archive \"${categoryToArchive.name}\"?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "${categoryToArchive.name} will be hidden from tracking but all your " +
                            "logged data will be preserved. You can unarchive it here at any time."
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { doNotShowAgain = !doNotShowAgain }
                                .semantics { role = Role.Checkbox }
                        ) {
                            Checkbox(
                                checked = doNotShowAgain,
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Don't show this again", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (doNotShowAgain) viewModel.setArchiveWarningDisabled(true)
                        viewModel.archiveCategory(categoryToArchive)
                        pendingArchive = null
                    }) { Text("Archive") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingArchive = null }) { Text("Cancel") }
                }
            )
        }
    }

    // ── Delete confirmation ───────────────────────────────────────────────────

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${categoryToDelete.name}\"?") },
            text = {
                Text(
                    "This will permanently remove the ${categoryToDelete.name} category and all " +
                    "log entries recorded for it. If you want to keep a copy of your data, " +
                    "export it before continuing. This cannot be undone."
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
            val active   = state.categories.filter { !it.isArchived }
            val archived = state.categories.filter { it.isArchived }
            var archivedExpanded by rememberSaveable { mutableStateOf(false) }

            val lazyListState = rememberLazyListState()
            val localActive = remember { mutableStateListOf<TrackingCategory>() }
            var draggedIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(active) {
                if (draggedIndex == null) {
                    localActive.clear()
                    localActive.addAll(active)
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(localActive, key = { it.id }) { category ->
                    val dragModifier = Modifier.pointerInput(category.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIndex = localActive.indexOfFirst { it.id == category.id }
                                    .takeIf { it >= 0 }
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val idx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                dragOffsetY += dragAmount.y
                                val itemH = lazyListState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == localActive.getOrNull(idx)?.id }
                                    ?.size?.toFloat() ?: 0f
                                if (itemH > 0f) {
                                    when {
                                        dragOffsetY > itemH / 2 && idx < localActive.size - 1 -> {
                                            localActive.add(idx + 1, localActive.removeAt(idx))
                                            draggedIndex = idx + 1
                                            dragOffsetY -= itemH
                                        }
                                        dragOffsetY < -(itemH / 2) && idx > 0 -> {
                                            localActive.add(idx - 1, localActive.removeAt(idx))
                                            draggedIndex = idx - 1
                                            dragOffsetY += itemH
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedIndex = null
                                dragOffsetY = 0f
                                viewModel.reorderCategories(localActive.map { it.id })
                            },
                            onDragCancel = {
                                draggedIndex = null
                                dragOffsetY = 0f
                                localActive.clear()
                                localActive.addAll(active)
                            }
                        )
                    }
                    SwipeableCategoryRow(
                        category         = category,
                        onClick          = { onNavigateToCategory(category.id) },
                        onEditAppearance = { pendingEditAppearance = category.id },
                        onArchiveToggle  = { requestArchive(category) },
                        onDelete         = { pendingDelete = category.id },
                        modifier         = Modifier.animateItem(),
                        dragModifier     = dragModifier,
                    )
                }

                if (archived.isNotEmpty()) {
                    item(key = "archived_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { archivedExpanded = !archivedExpanded }
                                .padding(vertical = 8.dp)
                                .semantics {
                                    role = Role.Button
                                    stateDescription = if (archivedExpanded) "Expanded" else "Collapsed"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text  = "Archived (${archived.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (archivedExpanded) Icons.Default.ExpandLess
                                              else Icons.Default.ExpandMore,
                                contentDescription = if (archivedExpanded) "Collapse archived" else "Expand archived",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (archivedExpanded) {
                        items(archived, key = { it.id }) { category ->
                            SwipeableCategoryRow(
                                category         = category,
                                onClick          = { onNavigateToCategory(category.id) },
                                onEditAppearance = { pendingEditAppearance = category.id },
                                onArchiveToggle  = { pendingArchive = category.id },
                                onDelete         = { pendingDelete = category.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Category row ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCategoryRow(
    category: TrackingCategory,
    onClick: () -> Unit,
    onEditAppearance: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier? = null,
) {
    if (category.isSystem) {
        CategoryRow(
            category = category,
            onClick = onClick,
            onEditAppearance = onEditAppearance,
            modifier = modifier,
            dragModifier = dragModifier,
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onArchiveToggle(); false }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.targetValue
            val bgColor by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                },
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (category.isArchived) Icons.Default.Unarchive
                                          else Icons.Default.Archive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (category.isArchived) "Unarchive" else "Archive",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    SwipeToDismissBoxValue.EndToStart -> Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> {}
                }
            }
        }
    ) {
        CategoryRow(
            category = category,
            onClick = onClick,
            onEditAppearance = onEditAppearance,
            dragModifier = dragModifier,
        )
    }
}

@Composable
private fun CategoryRow(
    category: TrackingCategory,
    onClick: () -> Unit,
    onEditAppearance: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier? = null,
) {
    val bubbleColor = category.colorToken.toCategoryColor()
    val iconTint    = category.colorToken.toCategoryOnColor()

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (category.isArchived) 0.55f else 1f),
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
                    text  = buildCategorySubtitle(category),
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

            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (dragModifier != null) {
                Icon(
                    imageVector        = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = dragModifier
                        .size(44.dp)
                        .padding(10.dp)
                )
            }
        }
    }
}

private fun buildCategorySubtitle(category: TrackingCategory): String = buildString {
    if (category.isSystem) append("Built-in · ")
    if (category.showInLogPeriod) append("Log with period · ")
    when (category.categoryType) {
        "numeric_slider" -> {
            append("Slider scale")
            if (category.numericUnit.isNotBlank()) append(" (${category.numericUnit})")
        }
        "numeric_free" -> {
            append("Numeric · Input")
            if (category.numericUnit.isNotBlank()) append(" (${category.numericUnit})")
        }
        "increment" -> {
            append("Plus One · tap to add")
            if (category.numericUnit.isNotBlank()) append(" (${category.numericUnit})")
        }
        else -> append("Tap to manage values")
    }
}

// ── Add category dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCategoryDialog(
    onAdd: (name: String, iconName: String, colorToken: String,
            categoryType: String, numericMin: Float, numericMax: Float,
            allowDecimals: Boolean, numericUnit: String, allowMultiple: Boolean,
            showInLogPeriod: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name             by rememberSaveable { mutableStateOf("") }
    var selectedType     by rememberSaveable { mutableStateOf(CategoryType.DEFAULT.key) }
    var numericUnit      by rememberSaveable { mutableStateOf("") }
    var selectedIconKey  by rememberSaveable { mutableStateOf(CategoryIcon.CATEGORY.key) }
    var selectedToken    by rememberSaveable { mutableStateOf(CategoryColor.SECONDARY.key) }
    var minText          by rememberSaveable { mutableStateOf("0") }
    var maxText          by rememberSaveable { mutableStateOf("10") }
    var allowDecimals    by rememberSaveable { mutableStateOf(false) }
    var allowMultiple    by rememberSaveable { mutableStateOf(false) }
    var showInLogPeriod  by rememberSaveable { mutableStateOf(false) }

    val isNumericType = selectedType != CategoryType.DEFAULT.key
    // Only the slider type uses a min/max range — free input and increment do not.
    val isSliderType = selectedType == CategoryType.NUMERIC_SLIDER.key

    val canAdd by remember(name, isSliderType, minText, maxText) {
        derivedStateOf {
            name.isNotBlank() && (!isSliderType || (
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

                // Name
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    placeholder   = { Text("e.g. Mood, Sleep, Exercise…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                // Type selector
                Text(
                    "Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryType.entries.forEach { type ->
                        FilterChip(
                            selected  = selectedType == type.key,
                            onClick   = { selectedType = type.key },
                            label     = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Unit field — shown for any numeric/counter type
                AnimatedVisibility(
                    visible = isNumericType,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value         = numericUnit,
                        onValueChange = { numericUnit = it },
                        label         = { Text("Unit / Key (optional)") },
                        placeholder   = { Text("e.g. °C, bpm, coffees…") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }

                // Icon
                Text("Icon", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryIconGrid(selectedKey = selectedIconKey, onSelect = { selectedIconKey = it })

                // Colour
                Text("Colour", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryColorPicker(selectedToken = selectedToken, onSelect = { selectedToken = it })

                // ── Numeric range settings (slider only) ──────────────────────
                AnimatedVisibility(
                    visible = isSliderType,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    HorizontalDivider()
                }
                AnimatedVisibility(
                    visible = isSliderType,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    NumericSettingsSection(
                        minText          = minText,
                        onMinChange      = { minText = it },
                        maxText          = maxText,
                        onMaxChange      = { maxText = it },
                        allowDecimals    = allowDecimals,
                        onDecimalsToggle = { allowDecimals = it }
                    )
                }

                // Allow multiple per day (not applicable to Plus One — its counter always uses a single daily log)
                if (selectedType != CategoryType.INCREMENT.key) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Allow multiple per day", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Log this category more than once on the same day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = allowMultiple, onCheckedChange = { allowMultiple = it })
                    }
                }

                // Log with period
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Log with period", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Show this category on the Log Period screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = showInLogPeriod, onCheckedChange = { showInLogPeriod = it })
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
                            if (canAdd) onAdd(
                                name, selectedIconKey, selectedToken,
                                selectedType,
                                minText.toFloatOrNull() ?: 0f,
                                maxText.toFloatOrNull() ?: 10f,
                                allowDecimals,
                                numericUnit.trim(),
                                allowMultiple && selectedType != CategoryType.INCREMENT.key,
                                showInLogPeriod
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
                            text  = "Appearance",
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

// ── Numeric settings section ──────────────────────────────────────────────────

/**
 * Reusable block shown in both the Add and Edit dialogs for configuring numeric mode.
 * Shows min/max/decimal fields directly; the caller's AnimatedVisibility handles
 * section visibility based on the selected category type.
 */
@Composable
private fun NumericSettingsSection(
    minText: String, onMinChange: (String) -> Unit,
    maxText: String, onMaxChange: (String) -> Unit,
    allowDecimals: Boolean, onDecimalsToggle: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Min / Max side-by-side
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
        // Decimal toggle
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
                    .clickable { onSelect(icon.key) }
                    .semantics {
                        role = Role.RadioButton
                        selected = isSelected
                        contentDescription = icon.displayName
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon.vector,
                    contentDescription = null,
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

        // Themed swatches
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
                            .clickable { onSelect(colorOption.key) }
                            .semantics {
                                role = Role.RadioButton
                                selected = isSelected
                                contentDescription = colorOption.displayName
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector        = Icons.Default.Check,
                                contentDescription = null,
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

        // Extended palette
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
                        .clickable { onSelect(hexKey) }
                        .semantics {
                            role = Role.RadioButton
                            selected = isSelected
                            contentDescription = "#$hexKey"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = null,
                            tint               = onSwatchColor,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Custom colour slot
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
                        .clickable { showFullPicker = true }
                        .semantics {
                            role = Role.Button
                            contentDescription = "Custom colour (selected). Tap to change"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = null,
                        tint               = onCustomColor,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { showFullPicker = true }
                        .semantics {
                            role = Role.Button
                            contentDescription = "Choose custom colour"
                        },
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

                HueSlider(hue = hue, onChanged = { hue = it })

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
                        val argb   = currentArgb
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
                    drag(down.id) { change -> updateFromOffset(change.position) }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )
        val thumbX = saturation
        val thumbY = 1f - value
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = thumbX * size.width
            val cy = thumbY * size.height
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = Color.Black, radius = 12.dp.toPx(), center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()))
        }
    }
}

// ── Hue slider ────────────────────────────────────────────────────────────────

@Composable
private fun HueSlider(hue: Float, onChanged: (Float) -> Unit) {
    val hueColors = remember {
        listOf(
            Color(0xFFFF0000), Color(0xFFFF8000), Color(0xFFFFFF00), Color(0xFF80FF00),
            Color(0xFF00FF00), Color(0xFF00FF80), Color(0xFF00FFFF), Color(0xFF0080FF),
            Color(0xFF0000FF), Color(0xFF8000FF), Color(0xFFFF00FF), Color(0xFFFF0080),
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
                    drag(down.id) { change -> updateFromOffset(change.position) }
                }
            }
    ) {
        val thumbX = hue / 360f
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = thumbX * size.width
            val cy = size.height / 2f
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = Color.Black, radius = 12.dp.toPx(), center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()))
        }
    }
}

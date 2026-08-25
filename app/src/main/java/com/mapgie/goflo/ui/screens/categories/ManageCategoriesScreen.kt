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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mapgie.goflo.data.database.entities.Group
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.ui.components.HairlineDivider
import com.mapgie.goflo.ui.components.ListCard
import com.mapgie.goflo.ui.components.ListRow
import com.mapgie.goflo.ui.components.RolePicker
import com.mapgie.goflo.ui.components.SegmentedToggle
import com.mapgie.goflo.ui.components.SwitchRow
import com.mapgie.goflo.ui.components.roleContainerTint
import com.mapgie.goflo.ui.util.CATEGORY_COLOR_OPTIONS
import com.mapgie.goflo.ui.util.CategoryColor
import com.mapgie.goflo.ui.util.CategoryIcon
import com.mapgie.goflo.ui.util.CategoryType
import com.mapgie.goflo.ui.util.effectiveColorToken
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor
import com.mapgie.goflo.ui.util.toHexColorKey

/**
 * The "What You Track" management home (logging redesign Phase 6).
 *
 * A Grouped/Ungrouped segmented view over the category list: the Grouped tab
 * renders one role-tinted card per [Group] with its member categories, the
 * Ungrouped tab renders loose categories neutrally with an "Add to group"
 * affordance. Every pre-redesign management action stays reachable: tap a
 * category to manage its values and settings, swipe right to archive/restore,
 * swipe left to delete (system categories protected), and the toolbar reorder
 * mode keeps the global drag-to-reorder list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: ManageCategoriesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (Long) -> Unit,
    onNavigateToCreateCategory: (groupId: Long?) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    var selectedTab   by rememberSaveable { mutableStateOf(0) }
    var showHelp      by rememberSaveable { mutableStateOf(false) }
    var pendingDelete  by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingArchive by rememberSaveable { mutableStateOf<Long?>(null) }
    var reorderMode   by rememberSaveable { mutableStateOf(false) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }

    // Group management state.
    var addToGroupCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var addMembersGroupId    by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingGroupId       by rememberSaveable { mutableStateOf<Long?>(null) }
    var showNewGroupEditor   by rememberSaveable { mutableStateOf(false) }
    var newGroupFileCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var newGroupAdoptColor     by rememberSaveable { mutableStateOf(true) }
    var pendingDeleteGroup     by rememberSaveable { mutableStateOf<Long?>(null) }

    val categoryToDelete  = state.categories.firstOrNull { it.id == pendingDelete }
    val categoryToArchive = state.categories.firstOrNull { it.id == pendingArchive }

    val active   = state.categories.filter { !it.isArchived }
    val archived = state.categories.filter { it.isArchived }
    val groupIds = state.groups.map { it.id }.toSet()
    val ungroupedActive = active.filter { it.groupId == null || it.groupId !in groupIds }

    fun requestArchive(category: TrackingCategory) {
        // Always show a confirmation for built-in categories regardless of warning preference
        if (!category.isArchived && !category.isSystem && state.archiveWarningDisabled) {
            viewModel.archiveCategory(category)
        } else {
            pendingArchive = category.id
        }
    }

    // ── Archive confirmation ──────────────────────────────────────────────────

    if (categoryToArchive != null) {
        if (categoryToArchive.isArchived) {
            AlertDialog(
                onDismissRequest = { pendingArchive = null },
                title = { Text("Restore \"${categoryToArchive.name}\"?") },
                text = { Text("${categoryToArchive.name} will be restored to your active tracking categories.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.unarchiveCategory(categoryToArchive)
                        pendingArchive = null
                    }) { Text("Restore") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingArchive = null }) { Text("Cancel") }
                }
            )
        } else if (categoryToArchive.isSystem) {
            AlertDialog(
                onDismissRequest = { pendingArchive = null },
                title = { Text("Hide \"${categoryToArchive.name}\"?") },
                text = {
                    Text(
                        "${categoryToArchive.name} is a built-in category. Hiding it will remove it " +
                        "from your tracking screen. All your logged data is kept. You can restore it " +
                        "from the Archived section at any time."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.archiveCategory(categoryToArchive)
                        pendingArchive = null
                    }) { Text("Hide") }
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

    if (showHelp) {
        CategoriesHelpDialog(onDismiss = { showHelp = false })
    }

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

    // ── Delete group confirmation ─────────────────────────────────────────────

    val groupToDelete = state.groups.firstOrNull { it.id == pendingDeleteGroup }
    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteGroup = null },
            title = { Text("Delete group \"${groupToDelete.name}\"?") },
            text = {
                Text(
                    "Categories filed under this group are kept: they become ungrouped and keep " +
                    "all their logged entries. Only the group itself is removed."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(groupToDelete.id)
                    pendingDeleteGroup = null
                }) { Text("Delete Group", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGroup = null }) { Text("Cancel") }
            }
        )
    }

    // ── Add-to-group sheet (category-centric) ─────────────────────────────────

    val addToGroupCategory = state.categories.firstOrNull { it.id == addToGroupCategoryId }
    if (addToGroupCategory != null) {
        AddToGroupSheet(
            category = addToGroupCategory,
            groups = state.groups,
            categories = state.categories,
            currentGroup = state.groups.firstOrNull { it.id == addToGroupCategory.groupId },
            onPickGroup = { groupId, adopt ->
                viewModel.assignCategoryToGroup(addToGroupCategory, groupId, adopt)
                addToGroupCategoryId = null
            },
            onRemoveFromGroup = {
                viewModel.unassignCategory(addToGroupCategory.id)
                addToGroupCategoryId = null
            },
            onNewGroup = { adopt ->
                newGroupFileCategoryId = addToGroupCategory.id
                newGroupAdoptColor = adopt
                addToGroupCategoryId = null
                showNewGroupEditor = true
            },
            onDismiss = { addToGroupCategoryId = null }
        )
    }

    // ── Add-member sheet (group-centric) ──────────────────────────────────────

    val addMembersGroup = state.groups.firstOrNull { it.id == addMembersGroupId }
    if (addMembersGroup != null) {
        AddMemberSheet(
            group = addMembersGroup,
            candidates = active
                .filter { it.groupId != addMembersGroup.id }
                .map { cat -> cat to state.groups.firstOrNull { it.id == cat.groupId } },
            onPick = { category, adopt ->
                viewModel.assignCategoryToGroup(category, addMembersGroup.id, adopt)
                addMembersGroupId = null
            },
            onNewCategory = {
                // The 2-step create flow (CategoryEditScreen) loads the group
                // itself to pre-select its default input type and file the
                // category on save.
                val groupId = addMembersGroup.id
                addMembersGroupId = null
                onNavigateToCreateCategory(groupId)
            },
            onDismiss = { addMembersGroupId = null }
        )
    }

    // ── Group editor (create / edit) ──────────────────────────────────────────

    if (showNewGroupEditor) {
        val filingCategory = state.categories.firstOrNull { it.id == newGroupFileCategoryId }
        GroupEditorDialog(
            group = null,
            members = emptyList(),
            canMoveUp = false,
            canMoveDown = false,
            filingCategoryName = filingCategory?.name,
            onSave = { name, colorRole, defaultInputType ->
                // Capture before the state resets below: onCreated fires after the
                // repository insert completes, by which point the vars are cleared.
                val fileCategory = filingCategory
                val adopt = newGroupAdoptColor
                viewModel.addGroup(name, colorRole, defaultInputType) { newId ->
                    fileCategory?.let { viewModel.assignCategoryToGroup(it, newId, adopt) }
                }
                showNewGroupEditor = false
                newGroupFileCategoryId = null
                newGroupAdoptColor = true
            },
            onRemoveMember = {},
            onMove = {},
            onDeleteGroup = {},
            onDismiss = {
                showNewGroupEditor = false
                newGroupFileCategoryId = null
                newGroupAdoptColor = true
            }
        )
    }

    val editingGroup = state.groups.firstOrNull { it.id == editingGroupId }
    if (editingGroup != null) {
        val groupIndex = state.groups.indexOfFirst { it.id == editingGroup.id }
        GroupEditorDialog(
            group = editingGroup,
            members = state.categories.filter { it.groupId == editingGroup.id },
            canMoveUp = groupIndex > 0,
            canMoveDown = groupIndex >= 0 && groupIndex < state.groups.size - 1,
            filingCategoryName = null,
            onSave = { name, colorRole, defaultInputType ->
                viewModel.updateGroup(editingGroup.id, name, colorRole, defaultInputType)
                editingGroupId = null
            },
            onRemoveMember = { category -> viewModel.unassignCategory(category.id) },
            onMove = { delta -> viewModel.moveGroup(editingGroup.id, delta) },
            onDeleteGroup = {
                pendingDeleteGroup = editingGroup.id
                editingGroupId = null
            },
            onDismiss = { editingGroupId = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What You Track") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { reorderMode = !reorderMode },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (reorderMode) "Exit reorder mode" else "Reorder categories"
                        }
                    ) {
                        Icon(
                            Icons.Default.Reorder,
                            contentDescription = null,
                            tint = if (reorderMode) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = "Help",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToCreateCategory(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Category") },
                modifier = Modifier.semantics { contentDescription = "Add category" }
            )
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
                if (reorderMode) {
                    // Reorder mode keeps the pre-redesign flat drag list: every
                    // active category in one list, long-press the handle to move.
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
                            category        = category,
                            onClick         = { onNavigateToCategory(category.id) },
                            onArchiveToggle = { requestArchive(category) },
                            onDelete        = { pendingDelete = category.id },
                            modifier        = Modifier.animateItem(),
                            dragModifier    = dragModifier,
                            reorderMode     = true,
                        )
                    }
                } else {
                    item(key = "view_header") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = countsLabel(active.size, state.groups.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            )
                            SegmentedToggle(
                                options = listOf("Grouped", "Ungrouped"),
                                selected = selectedTab,
                                onSelect = { selectedTab = it },
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        // ── Grouped view ──────────────────────────────────────
                        if (state.groups.isEmpty()) {
                            item(key = "no_groups") {
                                Text(
                                    text = "No groups yet. A group collects related categories under " +
                                        "one card and gives them a shared colour role.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        items(state.groups, key = { "group_${it.id}" }) { group ->
                            GroupCard(
                                group = group,
                                members = active.filter { it.groupId == group.id },
                                groups = state.groups,
                                onEdit = { editingGroupId = group.id },
                                onAddMember = { addMembersGroupId = group.id },
                                onCategoryClick = onNavigateToCategory,
                                onArchiveToggle = { requestArchive(it) },
                                onDelete = { pendingDelete = it.id },
                            )
                        }
                        item(key = "new_group") {
                            OutlinedButton(
                                onClick = {
                                    newGroupFileCategoryId = null
                                    showNewGroupEditor = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("New group")
                            }
                        }
                    } else {
                        // ── Ungrouped view ────────────────────────────────────
                        if (ungroupedActive.isEmpty()) {
                            item(key = "all_filed") {
                                Text(
                                    text = "Every category is filed into a group.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        items(ungroupedActive, key = { "cat_${it.id}" }) { category ->
                            SwipeableCategoryRow(
                                category        = category,
                                onClick         = { onNavigateToCategory(category.id) },
                                onArchiveToggle = { requestArchive(category) },
                                onDelete        = { pendingDelete = category.id },
                                containerColor  = MaterialTheme.colorScheme.surfaceVariant,
                                trailingAction  = {
                                    TextButton(
                                        onClick = { addToGroupCategoryId = category.id },
                                        modifier = Modifier.semantics {
                                            contentDescription = "Add ${category.name} to a group"
                                        }
                                    ) { Text("Add to group") }
                                },
                            )
                        }
                        if (ungroupedActive.isNotEmpty()) {
                            item(key = "ungrouped_note") {
                                Text(
                                    text = "Ungrouped categories still log as normal. They sit in " +
                                        "this neutral list until you file them into a group.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        item(key = "new_category") {
                            OutlinedButton(
                                onClick = { onNavigateToCreateCategory(null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("New category")
                            }
                        }
                    }
                }

                archivedSection(
                    archived = archived,
                    expanded = archivedExpanded,
                    onToggleExpanded = { archivedExpanded = !archivedExpanded },
                    onNavigateToCategory = onNavigateToCategory,
                    onArchiveToggle = { pendingArchive = it.id },
                    onDelete = { pendingDelete = it.id },
                )

                // Keep the last row reachable above the extended FAB.
                item(key = "fab_spacer") { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

private fun countsLabel(categoryCount: Int, groupCount: Int): String {
    val cats = if (categoryCount == 1) "1 category" else "$categoryCount categories"
    val groups = if (groupCount == 1) "1 group" else "$groupCount groups"
    return "$cats · $groups"
}

// ── Archived section (shared by reorder mode and both tabs) ───────────────────

private fun LazyListScope.archivedSection(
    archived: List<TrackingCategory>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onNavigateToCategory: (Long) -> Unit,
    onArchiveToggle: (TrackingCategory) -> Unit,
    onDelete: (TrackingCategory) -> Unit,
) {
    if (archived.isEmpty()) return

    item(key = "archived_header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(vertical = 8.dp)
                .semantics {
                    role = Role.Button
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
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
                imageVector = if (expanded) Icons.Default.ExpandLess
                              else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse archived" else "Expand archived",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (expanded) {
        items(archived, key = { it.id }) { category ->
            SwipeableCategoryRow(
                category        = category,
                onClick         = { onNavigateToCategory(category.id) },
                onArchiveToggle = { onArchiveToggle(category) },
                onDelete        = { onDelete(category) }
            )
        }
    }
}

// ── Group card ────────────────────────────────────────────────────────────────

/**
 * One role-tinted card per group: colour dot + name + Edit in the header, the
 * member categories as swipeable rows, and an inline add-category affordance.
 * Member bubbles resolve through [effectiveColorToken], so inherit-categories
 * adopt the group role live.
 */
@Composable
private fun GroupCard(
    group: Group,
    members: List<TrackingCategory>,
    groups: List<Group>,
    onEdit: () -> Unit,
    onAddMember: () -> Unit,
    onCategoryClick: (Long) -> Unit,
    onArchiveToggle: (TrackingCategory) -> Unit,
    onDelete: (TrackingCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val roleColor = group.colorRole.toCategoryColor()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = roleContainerTint(roleColor, MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(start = 16.dp, end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(roleColor)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.semantics {
                        contentDescription = "Edit group ${group.name}"
                    }
                ) { Text("Edit") }
            }

            members.forEach { category ->
                key(category.id) {
                    SwipeableCategoryRow(
                        category        = category,
                        onClick         = { onCategoryClick(category.id) },
                        onArchiveToggle = { onArchiveToggle(category) },
                        onDelete        = { onDelete(category) },
                        colorToken      = category.effectiveColorToken(groups),
                        containerColor  = Color.Transparent,
                    )
                }
            }
            if (members.isEmpty()) {
                Text(
                    text = "No categories in this group yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button }
                    .clickable(onClick = onAddMember)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Add category to this group",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Add-to-group sheet (category-centric) ─────────────────────────────────────

/**
 * Files one category into a group: pick an existing group, jump to group
 * creation pre-filled with this category, or unfile it. The colour switch
 * controls whether filing also sets the category's token to the "inherit"
 * sentinel so it adopts (and follows) the group's colour role.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToGroupSheet(
    category: TrackingCategory,
    groups: List<Group>,
    categories: List<TrackingCategory>,
    currentGroup: Group?,
    onPickGroup: (Long, Boolean) -> Unit,
    onRemoveFromGroup: () -> Unit,
    onNewGroup: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var adoptColor by rememberSaveable { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Add \"${category.name}\" to…",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = currentGroup?.let { "Currently in ${it.name}" } ?: "Currently ungrouped",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ListCard {
                SwitchRow(
                    title = "Use the group's colour",
                    subtitle = "The category follows its group's colour role from now on",
                    checked = adoptColor,
                    role = MaterialTheme.colorScheme.primary,
                    onRole = MaterialTheme.colorScheme.onPrimary,
                    onCheckedChange = { adoptColor = it },
                )
            }

            ListCard {
                groups.forEachIndexed { index, group ->
                    if (index > 0) HairlineDivider()
                    GroupPickRow(
                        group = group,
                        memberCount = categories.count { it.groupId == group.id && !it.isArchived },
                        isCurrent = group.id == currentGroup?.id,
                        onClick = { onPickGroup(group.id, adoptColor) },
                    )
                }
                if (groups.isNotEmpty()) HairlineDivider()
                ListRow(
                    key = "New group…",
                    onClick = { onNewGroup(adoptColor) },
                )
                if (currentGroup != null) {
                    HairlineDivider()
                    ListRow(
                        key = "Remove from group",
                        onClick = onRemoveFromGroup,
                    )
                }
            }

            Text(
                text = "\"${category.name}\" keeps all its past entries. Filing only changes " +
                    "where it appears and, if the colour switch is on, its colour.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GroupPickRow(
    group: Group,
    memberCount: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val dotColor = group.colorRole.toCategoryColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = group.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = when {
                isCurrent -> "Current"
                memberCount == 1 -> "1 category"
                else -> "$memberCount categories"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Add-member sheet (group-centric) ──────────────────────────────────────────

/**
 * The "+ Add category to this group" picker: lists every active category not
 * already in this group (ungrouped first, then members of other groups, each
 * labelled with where it currently lives), plus a "New category" row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberSheet(
    group: Group,
    candidates: List<Pair<TrackingCategory, Group?>>,
    onPick: (TrackingCategory, Boolean) -> Unit,
    onNewCategory: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var adoptColor by rememberSaveable { mutableStateOf(true) }
    val sorted = candidates.sortedBy { (_, currentGroup) -> if (currentGroup == null) 0 else 1 }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Add a category to \"${group.name}\"",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Filing keeps every past entry. It only changes where the category " +
                    "appears and, if the colour switch is on, its colour.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ListCard {
                SwitchRow(
                    title = "Use the group's colour",
                    subtitle = "Filed categories follow this group's colour role",
                    checked = adoptColor,
                    role = MaterialTheme.colorScheme.primary,
                    onRole = MaterialTheme.colorScheme.onPrimary,
                    onCheckedChange = { adoptColor = it },
                )
            }

            if (sorted.isEmpty()) {
                Text(
                    text = "Every active category is already in this group.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ListCard {
                sorted.forEachIndexed { index, (category, currentGroup) ->
                    if (index > 0) HairlineDivider()
                    ListRow(
                        key = category.name,
                        value = currentGroup?.let { "In ${it.name}" } ?: "Ungrouped",
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onPick(category, adoptColor) },
                    )
                }
                if (sorted.isNotEmpty()) HairlineDivider()
                ListRow(
                    key = "New category…",
                    onClick = onNewCategory,
                )
            }
        }
    }
}

// ── Group editor dialog (create / edit) ───────────────────────────────────────

/**
 * Creates or edits a [Group]: name, in-theme colour role (never a hex: the
 * fixed-colour track is hidden), and the default input type pre-selected when
 * creating a category inside the group. Edit mode adds the member list with
 * unfiling, move up/down reordering, and delete (members are kept).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupEditorDialog(
    group: Group?,
    members: List<TrackingCategory>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    filingCategoryName: String?,
    onSave: (name: String, colorRole: String, defaultInputType: String) -> Unit,
    onRemoveMember: (TrackingCategory) -> Unit,
    onMove: (Int) -> Unit,
    onDeleteGroup: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(group?.id) { mutableStateOf(group?.name ?: "") }
    var selectedRole by rememberSaveable(group?.id) {
        mutableStateOf(group?.colorRole ?: CategoryColor.PRIMARY.key)
    }
    var selectedType by rememberSaveable(group?.id) {
        mutableStateOf(group?.defaultInputType ?: CategoryType.DEFAULT.key)
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
                Text(
                    text = if (group == null) "New Group" else "Edit Group",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (filingCategoryName != null) {
                    Text(
                        text = "\"$filingCategoryName\" will be filed into this group.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    placeholder   = { Text("e.g. Body, Sleep, Environment…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                RolePicker(
                    selectedToken = selectedRole,
                    onPick = { selectedRole = it },
                    showFixedSection = false,
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Default input type",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Pre-selected when you create a category inside this group.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

                if (group != null) {
                    HorizontalDivider()
                    Text(
                        "Categories in this group",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (members.isEmpty()) {
                        Text(
                            "None yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    members.forEach { member ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { onRemoveMember(member) },
                                modifier = Modifier.semantics {
                                    contentDescription = "Remove ${member.name} from group"
                                }
                            ) { Text("Remove") }
                        }
                    }

                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Move up")
                        }
                        TextButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Move down")
                        }
                    }
                    TextButton(onClick = onDeleteGroup) {
                        Text("Delete group", color = MaterialTheme.colorScheme.error)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onSave(name, selectedRole, selectedType) },
                        enabled = name.isNotBlank()
                    ) { Text(if (group == null) "Add" else "Save") }
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
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier? = null,
    reorderMode: Boolean = false,
    colorToken: String = category.colorToken,
    containerColor: Color? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val archiveLabel = when {
        category.isArchived -> if (category.isSystem) "Restore" else "Unarchive"
        else                -> if (category.isSystem) "Hide"    else "Archive"
    }
    val archiveIcon = if (category.isArchived) Icons.Default.Unarchive else Icons.Default.Archive

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onArchiveToggle(); false }
                SwipeToDismissBoxValue.EndToStart -> { if (!category.isSystem) onDelete(); false }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = !reorderMode,
        enableDismissFromEndToStart = !reorderMode && !category.isSystem,
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
                    .clip(RoundedCornerShape(12.dp))
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
                            imageVector = archiveIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = archiveLabel,
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
            dragModifier = dragModifier,
            reorderMode = reorderMode,
            colorToken = colorToken,
            containerColor = containerColor,
            trailingAction = trailingAction,
        )
    }
}

@Composable
private fun CategoryRow(
    category: TrackingCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier? = null,
    reorderMode: Boolean = false,
    colorToken: String = category.colorToken,
    containerColor: Color? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val bubbleColor = colorToken.toCategoryColor()
    val iconTint    = colorToken.toCategoryOnColor()

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (category.isArchived) 0.55f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceContainerLow
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
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text  = buildCategorySubtitle(category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                reorderMode && dragModifier != null -> Icon(
                    imageVector        = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = dragModifier
                        .size(44.dp)
                        .padding(10.dp)
                )
                trailingAction != null -> trailingAction()
                else -> Icon(
                    imageVector        = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
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
// Superseded by CategoryEditScreen (logging redesign Phase 7): every create
// entry point now navigates to the 2-step flow instead of opening this dialog.
// Kept unreferenced until Phase 8 removes it against the parity checklist.

@Suppress("unused")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCategoryDialog(
    onAdd: (name: String, iconName: String, colorToken: String,
            categoryType: String, numericMin: Float, numericMax: Float,
            allowDecimals: Boolean, numericUnit: String, allowMultiple: Boolean,
            showInLogPeriod: Boolean) -> Unit,
    onDismiss: () -> Unit,
    initialType: String = CategoryType.DEFAULT.key,
) {
    var name             by rememberSaveable { mutableStateOf("") }
    var selectedType     by rememberSaveable { mutableStateOf(initialType) }
    var numericUnit      by rememberSaveable { mutableStateOf("") }
    var selectedIconKey  by rememberSaveable { mutableStateOf(CategoryIcon.CATEGORY.key) }
    var selectedToken    by rememberSaveable { mutableStateOf(CategoryColor.SECONDARY.key) }
    var minText          by rememberSaveable { mutableStateOf("1") }
    var maxText          by rememberSaveable { mutableStateOf("5") }
    var allowDecimals    by rememberSaveable { mutableStateOf(false) }
    var allowMultiple    by rememberSaveable { mutableStateOf(false) }
    var showInLogPeriod  by rememberSaveable { mutableStateOf(false) }

    // Only the numeric family carries a unit; Yes/No and Time store fixed
    // labels ("Yes"/"No", "HH:mm") and need no extra configuration at all.
    val isNumericType = selectedType == CategoryType.NUMERIC_SLIDER.key ||
        selectedType == CategoryType.NUMERIC_FREE.key ||
        selectedType == CategoryType.INCREMENT.key
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
// Superseded by CategoryEditScreen (logging redesign Phase 7), which covers icon
// and colour editing. Kept unreferenced until Phase 8 removes it against the
// parity checklist.

@Suppress("unused")
@Composable
internal fun EditAppearanceDialog(
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

internal fun isCustomColorToken(token: String): Boolean {
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

        // In-theme roles: pill chips that re-theme with the active palette.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = "In-theme roles",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text  = "Re-theme automatically",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
        ) {
            CategoryColor.entries.forEach { colorOption ->
                val isSelected  = colorOption.key == selectedToken
                val roleColor   = colorOption.key.toCategoryColor()
                val onRoleColor = colorOption.key.toCategoryOnColor()

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (isSelected) Modifier.background(roleColor)
                            else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                        )
                        .clickable { onSelect(colorOption.key) }
                        .semantics {
                            role = Role.RadioButton
                            selected = isSelected
                            contentDescription = colorOption.displayName
                        }
                        .padding(horizontal = 14.dp),
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = null,
                            tint               = onRoleColor,
                            modifier           = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(roleColor)
                        )
                    }
                    Text(
                        text  = colorOption.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else null,
                        color = if (isSelected) onRoleColor else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Fixed colours: deliberately exempt from theme changes.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = "Fixed colour",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text  = "Stays put on theme change",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
// Internal: also opened from CategoryEditScreen's custom fixed-colour slot.

@Composable
internal fun FullColorPickerDialog(
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

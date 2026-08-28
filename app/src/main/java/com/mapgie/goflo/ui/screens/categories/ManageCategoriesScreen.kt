package com.mapgie.goflo.ui.screens.categories

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.mapgie.goflo.ui.util.CategoryColor
import com.mapgie.goflo.ui.util.CategoryType
import com.mapgie.goflo.ui.util.effectiveColorToken
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor

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
    // A hex token (fixed swatch or custom picker) is a deliberate colour choice;
    // filing must not silently replace it, so the adopt switch starts off.
    val hasOwnFixedColor = isFixedColorToken(category.colorToken)
    var adoptColor by rememberSaveable { mutableStateOf(!hasOwnFixedColor) }

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
                    subtitle = if (hasOwnFixedColor) {
                        "This category has its own custom colour. Turning this on replaces it with the group's colour role."
                    } else {
                        "The category follows its group's colour role from now on"
                    },
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
                    subtitle = "Filed categories follow this group's colour role. " +
                        "Categories with their own custom colour keep it.",
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
                        // One switch covers every candidate, so a deliberately chosen
                        // hex colour is protected here per category; adopting for such
                        // a category stays available from its own Add-to-group sheet.
                        onClick = { onPick(category, adoptColor && !isFixedColorToken(category.colorToken)) },
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

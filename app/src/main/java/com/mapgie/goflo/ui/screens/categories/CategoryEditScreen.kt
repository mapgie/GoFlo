package com.mapgie.goflo.ui.screens.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.data.database.entities.CustomAlarm
import com.mapgie.goflo.ui.components.HairlineDivider
import com.mapgie.goflo.ui.components.IconPicker
import com.mapgie.goflo.ui.components.ListCard
import com.mapgie.goflo.ui.components.ListRow
import com.mapgie.goflo.ui.components.PrimarySaveBar
import com.mapgie.goflo.ui.components.RolePicker
import com.mapgie.goflo.ui.components.SectionHeader
import com.mapgie.goflo.ui.components.SwitchRow
import com.mapgie.goflo.ui.util.COLOR_TOKEN_INHERIT
import com.mapgie.goflo.ui.util.CategoryColor
import com.mapgie.goflo.ui.util.CategoryIcon
import com.mapgie.goflo.ui.util.CategoryType
import com.mapgie.goflo.ui.util.decodeScaleLabels
import com.mapgie.goflo.ui.util.encodeScaleLabels
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryIcon
import com.mapgie.goflo.ui.util.toCategoryOnColor

/**
 * The 2-step category create/edit flow (logging redesign Phase 7, rows 5/6).
 *
 * Step 1 stays short: name, icon, colour role (or fixed colour), input type,
 * and the per-category switches. Step 2 exists only for the stepped-scale type
 * (`numeric_slider`): min/max range, optional per-step word labels, and the
 * decimals switch.
 *
 * Edit mode uses the same surface prefilled, and adds:
 * - a Reminders section wired to the existing CustomAlarm system (rows open
 *   the existing EditAlarm screen; switches toggle scheduling in place),
 * - a "Scale settings" row linking into step 2,
 * - a danger-zone delete-with-history row (system categories protected).
 *
 * The input type is editable only until the category has at least one logged
 * entry (owner decision, PLAN.md paragraph 8 item 2); after that it locks and
 * the screen says why. Alarms appear on edit, never on first creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    viewModel: CategoryEditViewModel,
    onNavigateBack: () -> Unit,
    onCreated: (newId: Long, categoryType: String) -> Unit = { _, _ -> },
    onNavigateToNewAlarm: () -> Unit = {},
    onNavigateToEditAlarm: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val isEditing = viewModel.isEditing
    val category = state.category
    val group = state.group

    // The category was deleted (danger zone, or elsewhere) — leave the screen.
    LaunchedEffect(state.isLoading, category) {
        if (isEditing && !state.isLoading && category == null) onNavigateBack()
    }

    if (state.isLoading || (isEditing && category == null)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // ── Form state (initialised once the category/group context is loaded) ────

    var step by rememberSaveable(category?.id) { mutableStateOf(1) }

    var name by rememberSaveable(category?.id) { mutableStateOf(category?.name ?: "") }
    var selectedIconKey by rememberSaveable(category?.id) {
        mutableStateOf(category?.iconName ?: CategoryIcon.CATEGORY.key)
    }
    // The role picker never shows the "inherit" sentinel; it is represented by
    // the use-group-colour switch instead, with the group's own role preselected
    // underneath so switching it off lands somewhere sensible.
    var selectedToken by rememberSaveable(category?.id) {
        mutableStateOf(
            when {
                category == null -> group?.colorRole ?: CategoryColor.SECONDARY.key
                category.colorToken == COLOR_TOKEN_INHERIT ->
                    group?.colorRole ?: CategoryColor.SECONDARY.key
                else -> category.colorToken
            }
        )
    }
    var useGroupColor by rememberSaveable(category?.id) {
        mutableStateOf(
            if (category == null) group != null
            else category.colorToken == COLOR_TOKEN_INHERIT && group != null
        )
    }
    var selectedTypeKey by rememberSaveable(category?.id) {
        mutableStateOf(category?.categoryType ?: group?.defaultInputType ?: CategoryType.DEFAULT.key)
    }
    var numericUnit by rememberSaveable(category?.id) { mutableStateOf(category?.numericUnit ?: "") }
    var allowDecimals by rememberSaveable(category?.id) { mutableStateOf(category?.allowDecimals ?: false) }
    var minText by rememberSaveable(category?.id) {
        mutableStateOf(
            category?.let {
                if (it.allowDecimals) "%.1f".format(it.numericMin) else it.numericMin.toInt().toString()
            } ?: "1"
        )
    }
    var maxText by rememberSaveable(category?.id) {
        mutableStateOf(
            category?.let {
                if (it.allowDecimals) "%.1f".format(it.numericMax) else it.numericMax.toInt().toString()
            } ?: "5"
        )
    }
    var allowMultiple by rememberSaveable(category?.id) { mutableStateOf(category?.allowMultiple ?: false) }
    var showInLogPeriod by rememberSaveable(category?.id) { mutableStateOf(category?.showInLogPeriod ?: false) }
    var trackAgainstTime by rememberSaveable(category?.id) { mutableStateOf(category?.trackAgainstTime ?: false) }
    val labels = remember(category?.id) {
        mutableStateMapOf<Int, String>().apply {
            putAll(category?.scaleLabels?.decodeScaleLabels() ?: emptyMap())
        }
    }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    // ── Derived form facts ────────────────────────────────────────────────────

    val isSliderType = selectedTypeKey == CategoryType.NUMERIC_SLIDER.key
    val isNumericFamily = isSliderType ||
        selectedTypeKey == CategoryType.NUMERIC_FREE.key ||
        selectedTypeKey == CategoryType.INCREMENT.key
    val typeLocked = isEditing && (state.hasLogs || category?.isSystem == true)

    val minValue = minText.toFloatOrNull()
    val maxValue = maxText.toFloatOrNull()
    val rangeValid = !isSliderType || (minValue != null && maxValue != null && minValue < maxValue)
    val minInt = minText.toIntOrNull()
    val maxInt = maxText.toIntOrNull()
    val canLabelSteps = !allowDecimals && minInt != null && maxInt != null &&
        maxInt > minInt && (maxInt - minInt) <= 20

    val effectiveToken = if (useGroupColor && group != null) group.colorRole else selectedToken
    val bubbleColor = effectiveToken.toCategoryColor()
    val onBubbleColor = effectiveToken.toCategoryOnColor()

    fun doSave() {
        viewModel.save(
            name             = name,
            iconName         = selectedIconKey,
            colorToken       = if (useGroupColor && group != null) COLOR_TOKEN_INHERIT else selectedToken,
            categoryType     = selectedTypeKey,
            numericMin       = minValue ?: (category?.numericMin ?: 0f),
            numericMax       = maxValue ?: (category?.numericMax ?: 10f),
            allowDecimals    = allowDecimals,
            numericUnit      = numericUnit.trim(),
            scaleLabels      = if (isSliderType && canLabelSteps) {
                labels.filterKeys { it in minInt!!..maxInt!! }.encodeScaleLabels()
            } else if (isSliderType) {
                category?.scaleLabels ?: ""
            } else {
                ""
            },
            allowMultiple    = allowMultiple && selectedTypeKey != CategoryType.INCREMENT.key,
            showInLogPeriod  = showInLogPeriod,
            trackAgainstTime = trackAgainstTime,
            onSaved          = { id ->
                if (isEditing) onNavigateBack() else onCreated(id, selectedTypeKey)
            },
        )
    }

    // Back from step 2 returns to step 1 with the form intact.
    BackHandler(enabled = step == 2) { step = 1 }

    // ── Delete confirmation ───────────────────────────────────────────────────

    if (showDeleteConfirm && category != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${category.name}\"?") },
            text = {
                Text(
                    "This will permanently remove the ${category.name} category and all " +
                    "log entries recorded for it. If you want to keep a copy of your data, " +
                    "export it before continuing. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteCategory()
                }) { Text("Delete Everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            step == 2 -> "Scale settings"
                            isEditing -> "Edit category"
                            else -> "New category"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (step == 2) step = 1 else onNavigateBack() }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (step == 1) {
                    // ── Step 1: basics ────────────────────────────────────────

                    if (!isEditing && isSliderType) {
                        Text(
                            text = "Step 1 of 2. Range and step labels come on the next screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Live preview: bubble + name, resolving the group colour
                    // when the inherit switch is on.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(bubbleColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = selectedIconKey.toCategoryIcon().vector,
                                contentDescription = null,
                                tint = onBubbleColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            if (group != null) {
                                Text(
                                    text = "In ${group.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = name.ifBlank { "New category" },
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        placeholder = { Text("e.g. Mood, Sleep, Exercise…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SectionHeader(label = "Icon")
                    IconPicker(
                        selectedKey = selectedIconKey,
                        role = bubbleColor,
                        onRole = onBubbleColor,
                        onPick = { selectedIconKey = it.key },
                    )

                    SectionHeader(label = "Colour")
                    if (group != null) {
                        ListCard {
                            SwitchRow(
                                title = "Use the group's colour",
                                subtitle = "Follows the ${group.name} colour role from now on",
                                checked = useGroupColor,
                                role = MaterialTheme.colorScheme.primary,
                                onRole = MaterialTheme.colorScheme.onPrimary,
                                onCheckedChange = { useGroupColor = it },
                            )
                        }
                    }
                    if (!useGroupColor || group == null) {
                        RolePicker(
                            selectedToken = selectedToken,
                            onPick = { selectedToken = it },
                            extraFixedSlot = {
                                CustomColorSlot(
                                    selectedToken = selectedToken,
                                    onPick = { selectedToken = it },
                                )
                            },
                        )
                    }

                    InputTypeSection(
                        selectedTypeKey = selectedTypeKey,
                        typeLocked = typeLocked,
                        lockedBecauseSystem = category?.isSystem == true,
                        onPick = { selectedTypeKey = it },
                    )

                    if (isEditing && isSliderType) {
                        ListCard {
                            ListRow(
                                key = "Scale settings",
                                value = "$minText to $maxText",
                                onClick = { step = 2 },
                            )
                        }
                    }

                    if (isNumericFamily) {
                        OutlinedTextField(
                            value = numericUnit,
                            onValueChange = { numericUnit = it },
                            label = { Text("Unit / Key (optional)") },
                            placeholder = { Text("e.g. °C, bpm, coffees…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ── Options ───────────────────────────────────────────────

                    val showAllowMultiple = selectedTypeKey != CategoryType.INCREMENT.key &&
                        category?.isSystem != true
                    val showLogWithPeriod = category?.isSystem != true
                    SectionHeader(label = "Options")
                    ListCard {
                        var first = true
                        if (showAllowMultiple) {
                            first = false
                            SwitchRow(
                                title = "Allow multiple per day",
                                subtitle = "Log it several times a day. Each entry keeps its time.",
                                checked = allowMultiple,
                                role = MaterialTheme.colorScheme.primary,
                                onRole = MaterialTheme.colorScheme.onPrimary,
                                onCheckedChange = { allowMultiple = it },
                            )
                        }
                        if (showLogWithPeriod) {
                            if (!first) HairlineDivider()
                            first = false
                            SwitchRow(
                                title = "Log with period",
                                subtitle = "Surface it in the flow context while a period runs",
                                checked = showInLogPeriod,
                                role = MaterialTheme.colorScheme.primary,
                                onRole = MaterialTheme.colorScheme.onPrimary,
                                onCheckedChange = { showInLogPeriod = it },
                            )
                        }
                        if (!first) HairlineDivider()
                        SwitchRow(
                            title = "Track against time",
                            subtitle = "Record the time of each entry to view them by time of day",
                            checked = trackAgainstTime,
                            role = MaterialTheme.colorScheme.primary,
                            onRole = MaterialTheme.colorScheme.onPrimary,
                            onCheckedChange = { trackAgainstTime = it },
                        )
                    }

                    // ── Reminders (edit only) ─────────────────────────────────

                    if (isEditing) {
                        RemindersSection(
                            alarms = state.alarms,
                            onAddAlarm = onNavigateToNewAlarm,
                            onEditAlarm = onNavigateToEditAlarm,
                            onToggleAlarm = { id, enabled -> viewModel.setAlarmEnabled(id, enabled) },
                        )
                    }

                    // ── Danger zone (edit only, never system) ─────────────────

                    if (isEditing && category != null && !category.isSystem) {
                        SectionHeader(label = "Danger zone")
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .semantics { role = Role.Button }
                                    .clickable { showDeleteConfirm = true }
                                    .padding(horizontal = 16.dp),
                            ) {
                                Text(
                                    text = "Delete category and its history",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                } else {
                    // ── Step 2: scale settings (numeric_slider only) ──────────

                    if (!isEditing) {
                        Text(
                            text = "Step 2 of 2. ${name.ifBlank { "New category" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    SectionHeader(
                        label = "Range",
                        value = if (rangeValid) "$minText to $maxText" else null,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { minText = it },
                            label = { Text("Min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxText,
                            onValueChange = { maxText = it },
                            label = { Text("Max") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!rangeValid) {
                        Text(
                            text = "Enter numbers with min below max.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                        )
                    }

                    SectionHeader(label = "Step labels", value = "Optional words")
                    if (canLabelSteps) {
                        ListCard {
                            (minInt!!..maxInt!!).forEachIndexed { index, stepValue ->
                                if (index > 0) HairlineDivider()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = stepValue.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    OutlinedTextField(
                                        value = labels[stepValue] ?: "",
                                        onValueChange = { v ->
                                            if (v.isBlank()) labels.remove(stepValue)
                                            else labels[stepValue] = v
                                        },
                                        placeholder = { Text("Add word…") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Words like Barely there or Unbearable appear behind each " +
                                "step and in the Stats distribution chart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "Use whole numbers with a range of 20 steps or fewer " +
                                "(decimals off) to label individual steps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    ListCard {
                        SwitchRow(
                            title = "Allow decimals",
                            subtitle = "Log values like 3.5 between steps",
                            checked = allowDecimals,
                            role = MaterialTheme.colorScheme.primary,
                            onRole = MaterialTheme.colorScheme.onPrimary,
                            onCheckedChange = { enabled ->
                                allowDecimals = enabled
                                // Reformat Min/Max so parse-based gating (whole-number
                                // label editor) matches the new mode. See LESSONS.md.
                                fun reformat(text: String): String =
                                    text.toFloatOrNull()?.let {
                                        if (enabled) "%.1f".format(it) else it.toInt().toString()
                                    } ?: text
                                minText = reformat(minText)
                                maxText = reformat(maxText)
                            },
                        )
                    }
                }
            }

            PrimarySaveBar(
                label = when {
                    step == 1 && !isEditing && isSliderType -> "Next"
                    isEditing -> "Save"
                    else -> "Add category"
                },
                role = MaterialTheme.colorScheme.primary,
                onRole = MaterialTheme.colorScheme.onPrimary,
                enabled = name.isNotBlank() && (rangeValid || (step == 1 && !isEditing && isSliderType)),
                onClick = {
                    if (step == 1 && !isEditing && isSliderType) step = 2 else doSave()
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ── Input type section ────────────────────────────────────────────────────────

/**
 * The input-type selector: chips over every [CategoryType] while the type is
 * still editable, or a read-only row plus a plain sentence explaining the lock
 * once the category has logged entries (or is built-in).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputTypeSection(
    selectedTypeKey: String,
    typeLocked: Boolean,
    lockedBecauseSystem: Boolean,
    onPick: (String) -> Unit,
) {
    val selectedType = CategoryType.entries.firstOrNull { it.key == selectedTypeKey }
    SectionHeader(label = "Input type", value = selectedType?.displayName)
    if (typeLocked) {
        Text(
            text = if (lockedBecauseSystem) {
                "Built-in categories keep their input type."
            } else {
                "The input type is locked because this category already has logged entries."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CategoryType.entries.forEach { type ->
                FilterChip(
                    selected = selectedTypeKey == type.key,
                    onClick = { onPick(type.key) },
                    label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (selectedTypeKey == type.key) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                )
            }
        }
        Text(
            text = typeHelperLine(selectedTypeKey),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun typeHelperLine(typeKey: String): String = when (typeKey) {
    CategoryType.NUMERIC_SLIDER.key -> "A stepped scale with a set range and optional word labels."
    CategoryType.NUMERIC_FREE.key   -> "Type an exact number, like weight or temperature."
    CategoryType.INCREMENT.key      -> "A running count for the day. Tap to add one."
    CategoryType.YES_NO.key         -> "One yes or no answer for the day."
    CategoryType.TIME.key           -> "A time of day, like when you woke up."
    else                            -> "Pick from a list of options you define, like Happy or Tired."
}

// ── Reminders section (edit only) ─────────────────────────────────────────────

/**
 * Lists the custom alarms linked to this category. Rows open the existing
 * EditAlarm screen; the trailing switch enables/disables scheduling in place.
 * "+ Add alarm" opens EditAlarm pre-linked to this category. This section only
 * surfaces the existing CustomAlarm system — it schedules nothing itself.
 */
@Composable
private fun RemindersSection(
    alarms: List<CustomAlarm>,
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionHeader(label = "Reminders", modifier = Modifier.weight(1f))
        TextButton(
            onClick = onAddAlarm,
            modifier = Modifier.semantics {
                contentDescription = "Add alarm for this category"
            }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Add alarm")
        }
    }
    if (alarms.isEmpty()) {
        Text(
            text = "No reminders yet. Add one to get a nudge to log this category.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        ListCard {
            alarms.forEachIndexed { index, alarm ->
                if (index > 0) HairlineDivider()
                val time = "%02d:%02d".format(alarm.hour, alarm.minute)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Edit reminder at $time"
                        }
                        .clickable { onEditAlarm(alarm.id) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (alarm.label.isNotBlank()) "$time · ${alarm.label}" else time,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = alarmScheduleLabel(alarm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = { onToggleAlarm(alarm.id, it) },
                        modifier = Modifier.semantics {
                            contentDescription = "Reminder at $time enabled"
                        },
                    )
                }
            }
        }
    }
}

// ── Custom fixed-colour slot ──────────────────────────────────────────────────

/**
 * The custom-hex slot appended to the RolePicker's fixed track: shows the
 * current custom colour when one is selected, or a plus tile otherwise, and
 * opens the existing full HSV picker dialog either way.
 */
@Composable
private fun CustomColorSlot(
    selectedToken: String,
    onPick: (String) -> Unit,
) {
    var showFullPicker by rememberSaveable { mutableStateOf(false) }
    val hasCustomColor = isCustomColorToken(selectedToken)

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
                onPick(hexKey)
                showFullPicker = false
            }
        )
    }

    if (hasCustomColor) {
        val customColor = runCatching { Color(selectedToken.toLong(16)) }
            .getOrDefault(MaterialTheme.colorScheme.secondary)
        // WCAG: light custom colours get a near-black check, dark ones white.
        val onCustomColor = if (customColor.luminance() > 0.35f) Color(0xFF1C1B1F) else Color.White
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription = "Custom colour (selected). Tap to change"
                }
                .clickable { showFullPicker = true },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(customColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = onCustomColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription = "Choose custom colour"
                }
                .clickable { showFullPicker = true },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

package com.mapgie.goflo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One appended reading in a multiple-per-day [Timeline]:
 * "08:10   Level 2   mild". The time is always shown as text, so the entry is
 * parseable without colour (colour-blind and greyscale safe).
 */
data class TimelineEntryData(
    val id: Long,
    val time: String,
    val value: String,
    val sub: String? = null,
)

/**
 * The multiple-per-day list: when a category allows multiple entries, the day
 * becomes a card of timestamped readings plus an append row. Domain state is
 * hoisted; the component only renders [entries] and reports taps.
 *
 * [appendHint] is the small right-aligned helper on the append row, typically
 * the current time ("now, 21:30"). Pass [onEditEntry]/[onDeleteEntry] to give
 * each row an overflow menu.
 */
@Composable
fun Timeline(
    entries: List<TimelineEntryData>,
    role: Color,
    onAppend: () -> Unit,
    modifier: Modifier = Modifier,
    appendLabel: String = "Log another",
    appendHint: String? = null,
    onEditEntry: ((TimelineEntryData) -> Unit)? = null,
    onDeleteEntry: ((TimelineEntryData) -> Unit)? = null,
) {
    ListCard(modifier = modifier) {
        entries.forEach { entry ->
            TimelineEntry(
                time = entry.time,
                value = entry.value,
                role = role,
                sub = entry.sub,
                onEdit = onEditEntry?.let { edit -> { edit(entry) } },
                onDelete = onDeleteEntry?.let { delete -> { delete(entry) } },
            )
            HairlineDivider()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .semantics { this.role = Role.Button }
                .clickable(onClick = onAppend)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = role,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = appendLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = role,
                modifier = Modifier.weight(1f),
            )
            if (appendHint != null) {
                Text(
                    text = appendHint,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * One row of a [Timeline]: leading time in the category's [role] colour,
 * value plus optional sub-line, and an overflow menu when [onEdit] or
 * [onDelete] is provided. Menu visibility is transient presentation state and
 * lives inside the row; domain state stays hoisted.
 */
@Composable
fun TimelineEntry(
    time: String,
    value: String,
    role: Color,
    modifier: Modifier = Modifier,
    sub: String? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = time,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = role,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (sub != null) {
                Text(
                    text = sub,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onEdit != null || onDelete != null) {
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options for the $time entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun TimelinePreviewContent() {
    SectionHeader(label = "Today", value = "3 logged")
    Timeline(
        entries = listOf(
            TimelineEntryData(id = 1, time = "08:10", value = "Level 2", sub = "mild"),
            TimelineEntryData(id = 2, time = "13:40", value = "Level 4", sub = "after lunch"),
            TimelineEntryData(id = 3, time = "19:05", value = "Level 1", sub = "faded"),
        ),
        role = MaterialTheme.colorScheme.primary,
        onAppend = {},
        appendHint = "now, 21:30",
        onEditEntry = {},
        onDeleteEntry = {},
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun TimelinePreviewLight() {
    ComponentPreviewSurface { TimelinePreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun TimelinePreviewDark() {
    ComponentPreviewSurface(dark = true) { TimelinePreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun TimelinePreviewLarge() {
    ComponentPreviewSurface { TimelinePreviewContent() }
}

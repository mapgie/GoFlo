package com.mapgie.goflo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

/**
 * Mutually exclusive segments built on Material's single-choice segmented
 * buttons (radio-button semantics and an active checkmark come built in, so
 * the selected state is never colour-only).
 *
 * Powers the Grouped/Ungrouped management toggle and the Yes/No input type.
 * Pass [role] to tint the active segment with a category's role; leave null
 * for the neutral Material default (management surfaces).
 */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    role: Color? = null,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = index == selected,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = if (role != null) {
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = roleContainerTint(role, MaterialTheme.colorScheme.surface),
                        activeContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    SegmentedButtonDefaults.colors()
                },
            ) {
                Text(option, fontSize = 13.5.sp)
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun SegmentedTogglePreviewContent() {
    SegmentedToggle(
        options = listOf("Grouped", "Ungrouped"),
        selected = 0,
        onSelect = {},
    )
    SectionHeader(label = "Yes / no", value = "Yes", valueColor = MaterialTheme.colorScheme.primary)
    SegmentedToggle(
        options = listOf("Yes", "No"),
        selected = 0,
        onSelect = {},
        role = MaterialTheme.colorScheme.primary,
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SegmentedTogglePreviewLight() {
    ComponentPreviewSurface { SegmentedTogglePreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun SegmentedTogglePreviewDark() {
    ComponentPreviewSurface(dark = true) { SegmentedTogglePreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun SegmentedTogglePreviewLarge() {
    ComponentPreviewSurface { SegmentedTogglePreviewContent() }
}

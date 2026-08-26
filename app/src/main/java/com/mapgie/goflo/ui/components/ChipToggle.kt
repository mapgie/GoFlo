package com.mapgie.goflo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A state chip in the redesign's selection language: selected fills with a
 * tonal container of [role] and gains a leading check; unselected stays a
 * hairline outline. The check means selection is never colour-only.
 *
 * Built on Material's [FilterChip], which carries checkbox-style toggle
 * semantics and the minimum interactive size for the tap target.
 */
@Composable
fun ChipToggle(
    text: String,
    selected: Boolean,
    role: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = roleContainerTint(role, MaterialTheme.colorScheme.surface)
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(text, fontSize = 13.5.sp) },
        modifier = modifier,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = container,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            selectedBorderColor = Color.Transparent,
        ),
    )
}

/**
 * A wrapping row of [ChipToggle]s for multi-select values (symptoms, text
 * catalog values). State is hoisted: [selected] holds the chosen labels and
 * [onToggle] fires with the tapped label.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    options: List<String>,
    selected: Set<String>,
    role: Color,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            ChipToggle(
                text = option,
                selected = option in selected,
                role = role,
                onToggle = { onToggle(option) },
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun ChipRowPreviewContent() {
    SectionHeader(label = "Symptoms", value = "2 today")
    ChipRow(
        options = listOf("Cramps", "Nausea", "Headache", "Bloating", "Fatigue", "Back pain", "Mood swings"),
        selected = setOf("Cramps", "Nausea"),
        role = MaterialTheme.colorScheme.primary,
        onToggle = {},
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ChipRowPreviewLight() {
    ComponentPreviewSurface { ChipRowPreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ChipRowPreviewDark() {
    ComponentPreviewSurface(dark = true) { ChipRowPreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun ChipRowPreviewLarge() {
    ComponentPreviewSurface { ChipRowPreviewContent() }
}

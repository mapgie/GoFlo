package com.mapgie.goflo.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// High-contrast selected state per LESSONS.md: filled primary colour + white label
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    onClick: () -> Unit
) {
    val effectiveContainer = containerColor ?: MaterialTheme.colorScheme.primary
    val effectiveContent   = contentColor   ?: MaterialTheme.colorScheme.onPrimary
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = effectiveContainer,
            selectedLabelColor     = effectiveContent
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            selectedBorderColor = Color.Transparent,
        )
    )
}

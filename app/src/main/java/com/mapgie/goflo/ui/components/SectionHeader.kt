package com.mapgie.goflo.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The small uppercase label that sits above every group of controls, with the
 * group's current value right-aligned ("FLOW    Medium").
 *
 * Redesign rule: section titles are small uppercase labels; the current value
 * lives in the header, not inside the control. Pass the category's role colour
 * as [valueColor] when the value is a live reading; leave the muted default for
 * meta values like "Optional" or "Not today".
 */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.11.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeaderPreviewContent() {
    SectionHeader(
        label = "Flow",
        value = "Medium",
        valueColor = MaterialTheme.colorScheme.primary,
    )
    SectionHeader(label = "Notes", value = "Optional")
    SectionHeader(label = "Dates")
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SectionHeaderPreviewLight() {
    ComponentPreviewSurface { SectionHeaderPreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun SectionHeaderPreviewDark() {
    ComponentPreviewSurface(dark = true) { SectionHeaderPreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun SectionHeaderPreviewLarge() {
    ComponentPreviewSurface { SectionHeaderPreviewContent() }
}

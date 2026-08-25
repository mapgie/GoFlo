package com.mapgie.goflo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.util.CategoryIcon

/**
 * The icon grid for category create/edit: 48dp rounded tiles, one per
 * [CategoryIcon]. The selected tile fills with the category's [role] colour
 * and every tile announces its display name with radio-button semantics, so
 * selection is neither colour-only nor unlabelled.
 *
 * State is hoisted: [selectedKey] is the stored [CategoryIcon.key] and
 * [onPick] fires with the tapped icon.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(
    selectedKey: String?,
    role: Color,
    onRole: Color,
    onPick: (CategoryIcon) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<CategoryIcon> = CategoryIcon.entries,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icons.forEach { icon ->
            val isSelected = icon.key == selectedKey
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) Modifier.background(role)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                    .semantics {
                        this.role = Role.RadioButton
                        this.selected = isSelected
                        this.contentDescription = icon.displayName
                    }
                    .clickable { onPick(icon) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon.vector,
                    contentDescription = null,
                    tint = if (isSelected) onRole else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun IconPickerPreviewContent() {
    SectionHeader(label = "Icon")
    IconPicker(
        selectedKey = CategoryIcon.HEART.key,
        role = MaterialTheme.colorScheme.primary,
        onRole = MaterialTheme.colorScheme.onPrimary,
        onPick = {},
        icons = CategoryIcon.entries.take(10),
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun IconPickerPreviewLight() {
    ComponentPreviewSurface { IconPickerPreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun IconPickerPreviewDark() {
    ComponentPreviewSurface(dark = true) { IconPickerPreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun IconPickerPreviewLarge() {
    ComponentPreviewSurface { IconPickerPreviewContent() }
}

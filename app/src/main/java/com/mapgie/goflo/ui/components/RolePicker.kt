package com.mapgie.goflo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapgie.goflo.ui.util.CATEGORY_COLOR_OPTIONS
import com.mapgie.goflo.ui.util.CategoryColor
import com.mapgie.goflo.ui.util.toCategoryColor
import com.mapgie.goflo.ui.util.toCategoryOnColor
import com.mapgie.goflo.ui.util.toHexColorKey

/**
 * The "Pick a colour" control: six in-theme role pills (which re-theme with
 * the active palette) above a track of fixed swatches (deliberately exempt
 * from theme changes).
 *
 * Factored out of the Phase 1 inline picker in ManageCategoriesScreen; that
 * screen keeps its own copy until a later phase rewires it. [extraFixedSlot]
 * lets the create/edit flow append its custom-colour swatch to the fixed
 * track.
 *
 * State is hoisted: [selectedToken] is a [CategoryColor] key or an 8-char hex
 * key, and [onPick] fires with the tapped token.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RolePicker(
    selectedToken: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    roles: List<CategoryColor> = CategoryColor.entries,
    fixedColors: List<Int> = CATEGORY_COLOR_OPTIONS,
    extraFixedSlot: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(label = "In-theme roles", value = "Re-theme automatically")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            roles.forEach { colorOption ->
                RolePill(
                    option = colorOption,
                    isSelected = colorOption.key == selectedToken,
                    onPick = onPick,
                )
            }
        }
        HairlineDivider()
        SectionHeader(label = "Fixed colour", value = "Stays put on theme change")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            fixedColors.forEach { argb ->
                FixedSwatch(
                    argb = argb,
                    isSelected = argb.toHexColorKey() == selectedToken,
                    onPick = onPick,
                )
            }
            extraFixedSlot?.invoke()
        }
    }
}

@Composable
private fun RolePill(
    option: CategoryColor,
    isSelected: Boolean,
    onPick: (String) -> Unit,
) {
    val roleColor = option.key.toCategoryColor()
    val onRoleColor = option.key.toCategoryOnColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .then(
                if (isSelected) Modifier.background(roleColor)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            )
            .semantics {
                this.role = Role.RadioButton
                this.selected = isSelected
                this.contentDescription = option.displayName
            }
            .clickable { onPick(option.key) }
            .padding(horizontal = 14.dp),
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = onRoleColor,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(roleColor),
            )
        }
        Text(
            text = option.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else null,
            color = if (isSelected) onRoleColor else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FixedSwatch(
    argb: Int,
    isSelected: Boolean,
    onPick: (String) -> Unit,
) {
    val hexKey = argb.toHexColorKey()
    val swatchColor = Color(argb)
    val onSwatchColor = if (swatchColor.luminance() > 0.35f) Color(0xFF1C1B1F) else Color.White
    // 48dp outer tap target around a 38dp visible swatch.
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics {
                this.role = Role.RadioButton
                this.selected = isSelected
                this.contentDescription = "Fixed colour $hexKey"
            }
            .clickable { onPick(hexKey) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .then(
                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = onSwatchColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Light", showBackground = true)
@Composable
private fun RolePickerPreviewLight() {
    ComponentPreviewSurface {
        RolePicker(selectedToken = "primary", onPick = {})
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun RolePickerPreviewDark() {
    ComponentPreviewSurface(dark = true) {
        RolePicker(selectedToken = CATEGORY_COLOR_OPTIONS.first().toHexColorKey(), onPick = {})
    }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun RolePickerPreviewLarge() {
    ComponentPreviewSurface {
        RolePicker(selectedToken = "quaternary", onPick = {})
    }
}

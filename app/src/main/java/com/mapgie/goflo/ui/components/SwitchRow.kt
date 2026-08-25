package com.mapgie.goflo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A full-width toggle row: title (plus optional one-line consequence subtitle)
 * on the left, a switch tinted with the category's [role] on the right.
 *
 * Shared by allow-multiple, log-with-period, allow-decimals, and alarm-enable.
 * The whole row is the tap target and carries switch semantics with an
 * explicit On/Off state description; the inner Switch has no click handler of
 * its own, so TalkBack sees exactly one focusable control.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    role: Color,
    onRole: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics { stateDescription = if (checked) "On" else "Off" }
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = role,
                checkedThumbColor = onRole,
            ),
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun SwitchRowPreviewContent() {
    ListCard {
        SwitchRow(
            title = "Allow multiple per day",
            subtitle = "Log it several times, each keeps the time of entry",
            checked = true,
            role = MaterialTheme.colorScheme.primary,
            onRole = MaterialTheme.colorScheme.onPrimary,
            onCheckedChange = {},
        )
        HairlineDivider()
        SwitchRow(
            title = "Log with period",
            subtitle = "Surface it in the flow context while a period runs",
            checked = false,
            role = MaterialTheme.colorScheme.primary,
            onRole = MaterialTheme.colorScheme.onPrimary,
            onCheckedChange = {},
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun SwitchRowPreviewLight() {
    ComponentPreviewSurface { SwitchRowPreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun SwitchRowPreviewDark() {
    ComponentPreviewSurface(dark = true) { SwitchRowPreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun SwitchRowPreviewLarge() {
    ComponentPreviewSurface { SwitchRowPreviewContent() }
}

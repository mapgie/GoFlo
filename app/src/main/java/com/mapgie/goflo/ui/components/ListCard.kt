package com.mapgie.goflo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The white rounded card with hairline-divided rows: "one card per idea, not
 * per field". Powers Dates, grouped tracked metrics, alarms, step labels, and
 * add-to-group lists.
 *
 * Flat by design: a 1px hairline outline instead of a drop shadow. Compose
 * [ListRow]s (separated by [HairlineDivider]) inside the content lambda.
 */
@Composable
fun ListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
    ) {
        Column(content = content)
    }
}

/** The 1px low-contrast divider between [ListRow]s (about 7% onSurface). */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
    )
}

/**
 * One key/value row inside a [ListCard]: label on the left, tabular value on
 * the right, optional trailing slot (defaults to a chevron when clickable).
 *
 * Rows are at least 52dp tall. A clickable row announces as a button.
 */
@Composable
fun ListRow(
    key: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valueEmphasis: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = key,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = if (valueEmphasis) FontWeight.SemiBold else FontWeight.Normal,
                color = valueColor,
                // Tabular figures so date/number columns align across rows.
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun ListCardPreviewContent() {
    ListCard {
        ListRow(key = "Started", value = "Aug 6, 2026", valueEmphasis = true, onClick = {})
        HairlineDivider()
        ListRow(
            key = "Ended",
            value = "Still ongoing",
            valueEmphasis = true,
            valueColor = MaterialTheme.colorScheme.primary,
            onClick = {},
        )
    }
    ListCard {
        ListRow(key = "Weather", value = "Cloudy")
        HairlineDivider()
        ListRow(key = "Rainfall", value = "12 mm")
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ListCardPreviewLight() {
    ComponentPreviewSurface { ListCardPreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ListCardPreviewDark() {
    ComponentPreviewSurface(dark = true) { ListCardPreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun ListCardPreviewLarge() {
    ComponentPreviewSurface { ListCardPreviewContent() }
}

package com.mapgie.goflo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The sticky bottom action: a full-width 52dp pill in the screen's [role]
 * colour, with a gradient fade of the background over the scrolling content
 * above it so list items dissolve under the bar instead of clipping.
 *
 * Place it bottom-aligned over the scroll container (e.g. in a Box). The
 * label is always text, so no extra content description is needed.
 */
@Composable
fun PrimarySaveBar(
    label: String,
    role: Color,
    onRole: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val background = MaterialTheme.colorScheme.background
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(background.copy(alpha = 0f), background)
                    )
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = role,
                    contentColor = onRole,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun PrimarySaveBarPreviewContent() {
    PrimarySaveBar(
        label = "Save log",
        role = MaterialTheme.colorScheme.primary,
        onRole = MaterialTheme.colorScheme.onPrimary,
        onClick = {},
    )
    PrimarySaveBar(
        label = "Add entry",
        role = MaterialTheme.colorScheme.secondary,
        onRole = MaterialTheme.colorScheme.onSecondary,
        onClick = {},
        enabled = false,
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun PrimarySaveBarPreviewLight() {
    ComponentPreviewSurface { PrimarySaveBarPreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun PrimarySaveBarPreviewDark() {
    ComponentPreviewSurface(dark = true) { PrimarySaveBarPreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun PrimarySaveBarPreviewLarge() {
    ComponentPreviewSurface { PrimarySaveBarPreviewContent() }
}

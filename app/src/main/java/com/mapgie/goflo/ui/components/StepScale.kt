package com.mapgie.goflo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Discrete rising tap-steps: the redesign's replacement for rating sliders.
 * One component serves flow, rage, severity, and day-overall; [range],
 * [labels], and [role] are the only differences between them.
 *
 * A single tap selects a step; there is no drag. Step height rises left to
 * right so magnitude is never colour-only, and the selected step also gets a
 * bold caption. Selection changes are instant (no animation), which honours
 * reduce-motion by construction.
 *
 * Accessibility: the whole scale exposes as ONE control announcing
 * "<name>, <selected word>, <n> of <total>", with one custom action per step,
 * instead of N unlabelled buttons.
 */
@Composable
fun StepScale(
    name: String,
    range: IntRange,
    value: Int?,
    role: Color,
    onRole: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labels: List<String>? = null,
    endLabels: Pair<String, String>? = null,
) {
    val steps = range.toList()
    val total = steps.size
    val selectedIndex = value?.let { v -> steps.indexOf(v).takeIf { it >= 0 } }

    fun captionFor(index: Int): String =
        labels?.getOrNull(index) ?: steps[index].toString()

    val description = if (selectedIndex == null) {
        "$name, not set"
    } else {
        "$name, ${captionFor(selectedIndex)}, ${selectedIndex + 1} of $total"
    }
    val stepActions = steps.mapIndexed { index, step ->
        CustomAccessibilityAction(label = "Set to ${captionFor(index)}") {
            onSelect(step)
            true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                this.role = Role.RadioButton
                this.contentDescription = description
                this.customActions = stepActions
            },
    ) {
        // Tap targets fill the full 48dp row height even where the visible bar
        // is shorter.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            steps.forEachIndexed { index, step ->
                val isSelected = index == selectedIndex
                val rise = if (total <= 1) 1f else index.toFloat() / (total - 1)
                val barHeight = (22 + 24 * rise).dp
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { this.role = Role.RadioButton }
                        .clickable { onSelect(step) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) role
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            steps.forEachIndexed { index, _ ->
                val isSelected = index == selectedIndex
                Text(
                    text = captionFor(index),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) role else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (endLabels != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            ) {
                Text(
                    text = endLabels.first,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = endLabels.second,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun StepScalePreviewContent() {
    SectionHeader(label = "Flow", value = "Medium", valueColor = MaterialTheme.colorScheme.primary)
    StepScale(
        name = "Flow",
        range = 1..4,
        value = 3,
        role = MaterialTheme.colorScheme.primary,
        onRole = MaterialTheme.colorScheme.onPrimary,
        onSelect = {},
        labels = listOf("Spot", "Light", "Med", "Heavy"),
    )
    SectionHeader(label = "Rage", value = "Not today")
    StepScale(
        name = "Rage",
        range = 0..5,
        value = null,
        role = MaterialTheme.colorScheme.secondary,
        onRole = MaterialTheme.colorScheme.onSecondary,
        onSelect = {},
        endLabels = "Calm" to "Volcanic",
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun StepScalePreviewLight() {
    ComponentPreviewSurface { StepScalePreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StepScalePreviewDark() {
    ComponentPreviewSurface(dark = true) { StepScalePreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun StepScalePreviewLarge() {
    ComponentPreviewSurface { StepScalePreviewContent() }
}

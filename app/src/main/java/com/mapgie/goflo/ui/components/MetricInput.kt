package com.mapgie.goflo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.goflo.ui.util.CategoryType

/**
 * The value a [MetricInput] holds, one variant per input model.
 *
 * The Yes/No and Time variants are defined now so Phase 4 can add the
 * matching [CategoryType] entries without reshaping this API; nothing renders
 * them yet.
 */
sealed interface MetricValue {
    /** Multi-select text labels (the `default` chip input). */
    data class Choice(val selected: Set<String>) : MetricValue

    /** A discrete whole-number step on a rating scale. */
    data class Scale(val step: Int?) : MetricValue

    /** A genuine continuous reading (weight, temperature). */
    data class Continuous(val value: Float?) : MetricValue

    /** Free numeric text, kept as typed until save-time parsing. */
    data class FreeNumber(val text: String) : MetricValue

    /** A per-day tally (the `increment` input). */
    data class Count(val count: Int) : MetricValue

    /** Yes/No state; null until the user answers. Rendered from Phase 4. */
    data class YesNo(val value: Boolean?) : MetricValue

    /** A time of day as "HH:mm"; null until picked. Rendered from Phase 4. */
    data class TimeOfDay(val time: String?) : MetricValue
}

/**
 * Everything a [MetricInput] needs to render a category's input control,
 * lifted from the category row by the caller.
 */
data class MetricConfig(
    val name: String,
    val options: List<String> = emptyList(),
    val min: Int = 1,
    val max: Int = 5,
    val stepLabels: Map<Int, String> = emptyMap(),
    val unit: String? = null,
    val allowDecimals: Boolean = false,
    val endLabels: Pair<String, String>? = null,
)

/**
 * The one input facade: screens render a metric through this and never branch
 * on the category type themselves.
 *
 * Phase 3 stub: the switch exists and returns a working control per type, but
 * full logging behaviour (save blocking, timed timelines, value formatting)
 * lands in Phase 4, which is also where the `yes_no` and `time` types join
 * [CategoryType]. The "kill the slider" rule is applied here: a discrete
 * whole-step scale renders as a [StepScale]; only a genuinely continuous
 * measure (decimals allowed, or a wide range) keeps a real slider.
 */
@Composable
fun MetricInput(
    type: CategoryType,
    config: MetricConfig,
    value: MetricValue?,
    role: Color,
    onRole: Color,
    onChange: (MetricValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (type) {
        CategoryType.DEFAULT -> ChipRow(
            options = config.options,
            selected = (value as? MetricValue.Choice)?.selected ?: emptySet(),
            role = role,
            onToggle = { option ->
                val current = (value as? MetricValue.Choice)?.selected ?: emptySet()
                val next = if (option in current) current - option else current + option
                onChange(MetricValue.Choice(next))
            },
            modifier = modifier,
        )

        CategoryType.NUMERIC_SLIDER -> {
            val stepCount = config.max - config.min + 1
            val isContinuous = config.allowDecimals || stepCount > 10
            if (isContinuous) {
                val current = (value as? MetricValue.Continuous)?.value ?: config.min.toFloat()
                Slider(
                    value = current.coerceIn(config.min.toFloat(), config.max.toFloat()),
                    onValueChange = { onChange(MetricValue.Continuous(it)) },
                    valueRange = config.min.toFloat()..config.max.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = role,
                        activeTrackColor = role,
                    ),
                    modifier = modifier.fillMaxWidth(),
                )
            } else {
                val range = config.min..config.max
                val labels = if (config.stepLabels.isEmpty()) null
                else range.map { config.stepLabels[it] ?: it.toString() }
                StepScale(
                    name = config.name,
                    range = range,
                    value = (value as? MetricValue.Scale)?.step,
                    role = role,
                    onRole = onRole,
                    onSelect = { onChange(MetricValue.Scale(it)) },
                    labels = labels,
                    endLabels = config.endLabels,
                    modifier = modifier,
                )
            }
        }

        CategoryType.NUMERIC_FREE -> OutlinedTextField(
            value = (value as? MetricValue.FreeNumber)?.text ?: "",
            onValueChange = { onChange(MetricValue.FreeNumber(it)) },
            label = { Text(config.unit ?: "Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = modifier.fillMaxWidth(),
        )

        CategoryType.INCREMENT -> {
            val count = (value as? MetricValue.Count)?.count ?: 0
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                OutlinedIconButton(
                    onClick = { onChange(MetricValue.Count((count - 1).coerceAtLeast(0))) },
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease ${config.name}",
                    )
                }
                Text(
                    text = count.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
                FilledIconButton(
                    onClick = { onChange(MetricValue.Count(count + 1)) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = role,
                        contentColor = onRole,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase ${config.name}",
                    )
                }
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun MetricInputPreviewContent() {
    SectionHeader(label = "Symptoms", value = "1 today")
    MetricInput(
        type = CategoryType.DEFAULT,
        config = MetricConfig(name = "Symptoms", options = listOf("Cramps", "Nausea", "Headache")),
        value = MetricValue.Choice(setOf("Nausea")),
        role = MaterialTheme.colorScheme.primary,
        onRole = MaterialTheme.colorScheme.onPrimary,
        onChange = {},
    )
    SectionHeader(label = "Severity", value = "3 of 5", valueColor = MaterialTheme.colorScheme.primary)
    MetricInput(
        type = CategoryType.NUMERIC_SLIDER,
        config = MetricConfig(name = "Severity", min = 1, max = 5),
        value = MetricValue.Scale(3),
        role = MaterialTheme.colorScheme.primary,
        onRole = MaterialTheme.colorScheme.onPrimary,
        onChange = {},
    )
    SectionHeader(label = "Weight")
    MetricInput(
        type = CategoryType.NUMERIC_FREE,
        config = MetricConfig(name = "Weight", unit = "kg"),
        value = MetricValue.FreeNumber("72.4"),
        role = MaterialTheme.colorScheme.primary,
        onRole = MaterialTheme.colorScheme.onPrimary,
        onChange = {},
    )
    SectionHeader(label = "Count", value = "6 glasses of water")
    MetricInput(
        type = CategoryType.INCREMENT,
        config = MetricConfig(name = "Water"),
        value = MetricValue.Count(6),
        role = MaterialTheme.colorScheme.primary,
        onRole = MaterialTheme.colorScheme.onPrimary,
        onChange = {},
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun MetricInputPreviewLight() {
    ComponentPreviewSurface { MetricInputPreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun MetricInputPreviewDark() {
    ComponentPreviewSurface(dark = true) { MetricInputPreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun MetricInputPreviewLarge() {
    ComponentPreviewSurface { MetricInputPreviewContent() }
}

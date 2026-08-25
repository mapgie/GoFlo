package com.mapgie.goflo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * ### Storage encoding (PLAN.md §8 decision #3, resolved by the owner)
 * Every variant round-trips to the same store as before: plain value-label
 * strings in `tracking_log_values`. The two Phase 4 types follow suit with
 * NO new columns and NO migration:
 * - [YesNo] persists as the literal label "Yes" or "No".
 * - [TimeOfDay] persists as a 24-hour "HH:mm" label.
 * Stats already counts value labels, so Yes/No charts work for free; a time
 * label is display-only in Stats (accepted).
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

    /** Yes/No state; null until the user answers. Stored as "Yes"/"No". */
    data class YesNo(val value: Boolean?) : MetricValue

    /** A time of day as "HH:mm"; null until picked. Stored as "HH:mm". */
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
 * Whether a `numeric_slider` category renders as discrete tap-steps
 * ([StepScale]) rather than a continuous [Slider]. The "kill the slider" rule:
 * a whole-step scale of up to 10 steps is a rating and gets tap-steps; a
 * decimal or wider range is a genuine measure and keeps a real slider.
 *
 * Exposed so callers (the log screens) can decide which [MetricValue] variant
 * mirrors their state without duplicating the threshold.
 */
fun MetricConfig.usesStepScale(): Boolean =
    !allowDecimals && (max - min + 1) in 2..10

/** Reads the numeric reading out of whichever slider-family variant holds it. */
private fun MetricValue?.numericOrNull(): Float? = when (this) {
    is MetricValue.Scale -> step?.toFloat()
    is MetricValue.Continuous -> value
    is MetricValue.Count -> count.toFloat()
    else -> null
}

/**
 * The one input facade: screens render a metric through this and never branch
 * on the category type themselves.
 *
 * Behaviour parity with the pre-redesign per-type sections is the contract:
 * - `numeric_slider`: whole-step behaviour is kept (tap-steps for ratings,
 *   a stepped slider for whole ranges wider than 10, continuous only with
 *   decimals), scale labels still caption the steps, and an unset value still
 *   displays as the range minimum (which is also what saves).
 * - `numeric_free`: unit label + decimal keyboard; the caller keeps its
 *   empty-input-blocks-save rule (the facade never fabricates a value).
 * - `increment`: count never drops below 0; the caller keeps its
 *   count-of-zero-blocks-save rule. Timed increment (per-tap immediate saves)
 *   is a screen-level flow rendered with [Timeline], not through this facade.
 * - `yes_no` / `time`: new in Phase 4, see [MetricValue] for the storage
 *   encoding.
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
            if (config.usesStepScale()) {
                val range = config.min..config.max
                val labels = if (config.stepLabels.isEmpty()) null
                else range.map { config.stepLabels[it] ?: it.toString() }
                StepScale(
                    name = config.name,
                    range = range,
                    value = value.numericOrNull()?.toInt(),
                    role = role,
                    onRole = onRole,
                    onSelect = { onChange(MetricValue.Scale(it)) },
                    labels = labels,
                    endLabels = config.endLabels,
                    modifier = modifier,
                )
            } else {
                ContinuousSlider(
                    config = config,
                    value = value.numericOrNull(),
                    role = role,
                    onChange = { onChange(MetricValue.Continuous(it)) },
                    modifier = modifier,
                )
            }
        }

        CategoryType.NUMERIC_FREE -> OutlinedTextField(
            value = (value as? MetricValue.FreeNumber)?.text ?: "",
            onValueChange = { onChange(MetricValue.FreeNumber(it)) },
            label = { Text(if (config.unit.isNullOrBlank()) "Value" else config.unit) },
            placeholder = { Text("Enter a number") },
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
                    enabled = count > 0,
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease ${config.name}",
                    )
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                    )
                    if (!config.unit.isNullOrBlank()) {
                        Text(
                            text = config.unit,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
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

        CategoryType.YES_NO -> SegmentedToggle(
            options = listOf("Yes", "No"),
            selected = when ((value as? MetricValue.YesNo)?.value) {
                true -> 0
                false -> 1
                null -> -1
            },
            onSelect = { onChange(MetricValue.YesNo(it == 0)) },
            role = role,
            modifier = modifier,
        )

        // Label stays the generic "Time": both log screens already frame the
        // input with the category's name, so repeating it would read doubled.
        CategoryType.TIME -> TimeField(
            value = (value as? MetricValue.TimeOfDay)?.time,
            role = role,
            onChange = { onChange(MetricValue.TimeOfDay(it)) },
            modifier = modifier,
        )
    }
}

/**
 * The slider kept for genuine measures: continuous when decimals are allowed,
 * whole-number stepped when the range is wider than [StepScale] comfortably
 * fits. Mirrors the pre-redesign slider exactly: large readout (falling back
 * to the range minimum, which is also the value that saves), optional scale
 * label for the current whole value, min/max end labels, and a hint until the
 * user sets a value.
 */
@Composable
private fun ContinuousSlider(
    config: MetricConfig,
    value: Float?,
    role: Color,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val min = config.min.toFloat()
    val max = config.max.toFloat()
    val sliderValue = (value ?: min).coerceIn(min, max)

    // Steps: 0 = continuous (for decimals), otherwise whole-number steps.
    val steps = if (config.allowDecimals) 0 else {
        val range = (max - min).toInt()
        if (range > 1) range - 1 else 0
    }

    fun format(v: Float): String =
        if (config.allowDecimals) "%.1f".format(v) else v.toInt().toString()

    val scaleLabel = if (!config.allowDecimals) config.stepLabels[sliderValue.toInt()] else null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = if (config.unit.isNullOrBlank()) format(sliderValue)
                else "${format(sliderValue)} ${config.unit}",
                style = MaterialTheme.typography.headlineMedium,
                color = role,
            )
            if (scaleLabel != null) {
                Text(
                    text = scaleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Slider(
            value = sliderValue,
            onValueChange = onChange,
            valueRange = min..max,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = role,
                activeTrackColor = role,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = format(min),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (value == null) {
                Text(
                    text = "Drag to set a value",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = format(max),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    SectionHeader(label = "Temperature")
    MetricInput(
        type = CategoryType.NUMERIC_SLIDER,
        config = MetricConfig(name = "Temperature", min = 35, max = 39, allowDecimals = true, unit = "C"),
        value = MetricValue.Continuous(36.6f),
        role = MaterialTheme.colorScheme.tertiary,
        onRole = MaterialTheme.colorScheme.onTertiary,
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
        config = MetricConfig(name = "Water", unit = "glasses"),
        value = MetricValue.Count(6),
        role = MaterialTheme.colorScheme.primary,
        onRole = MaterialTheme.colorScheme.onPrimary,
        onChange = {},
    )
    SectionHeader(label = "Took medication", value = "Yes", valueColor = MaterialTheme.colorScheme.primary)
    MetricInput(
        type = CategoryType.YES_NO,
        config = MetricConfig(name = "Took medication"),
        value = MetricValue.YesNo(true),
        role = MaterialTheme.colorScheme.primary,
        onRole = MaterialTheme.colorScheme.onPrimary,
        onChange = {},
    )
    SectionHeader(label = "Woke up", value = "07:45", valueColor = MaterialTheme.colorScheme.secondary)
    MetricInput(
        type = CategoryType.TIME,
        config = MetricConfig(name = "Woke up"),
        value = MetricValue.TimeOfDay("07:45"),
        role = MaterialTheme.colorScheme.secondary,
        onRole = MaterialTheme.colorScheme.onSecondary,
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

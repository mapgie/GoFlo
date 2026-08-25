package com.mapgie.goflo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val storageFormat = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Tap-to-pick time-of-day input for the "time" category type. Shows the picked
 * time (24-hour "HH:mm", exactly the stored value-label string) or a hint when
 * unset, and opens a Material time picker dialog on tap.
 *
 * State is hoisted: [value] is the stored "HH:mm" string or null, [onChange]
 * fires with the newly picked "HH:mm". The whole field is one Button-role
 * control announcing its label and current value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    value: String?,
    role: Color,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Time",
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    if (showPicker) {
        val initial = value?.let { runCatching { LocalTime.parse(it, storageFormat) }.getOrNull() }
            ?: LocalTime.now()
        val pickerState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select time") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    onChange("%02d:%02d".format(pickerState.hour, pickerState.minute))
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.role = Role.Button }
            .clickable { showPicker = true },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = role,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = role,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            } else {
                Text(
                    text = "Tap to set",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun TimeFieldPreviewContent() {
    SectionHeader(label = "Woke up", value = "07:45", valueColor = MaterialTheme.colorScheme.primary)
    TimeField(
        value = "07:45",
        role = MaterialTheme.colorScheme.primary,
        onChange = {},
        label = "Woke up",
    )
    SectionHeader(label = "Bedtime")
    TimeField(
        value = null,
        role = MaterialTheme.colorScheme.secondary,
        onChange = {},
        label = "Bedtime",
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun TimeFieldPreviewLight() {
    ComponentPreviewSurface { TimeFieldPreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun TimeFieldPreviewDark() {
    ComponentPreviewSurface(dark = true) { TimeFieldPreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun TimeFieldPreviewLarge() {
    ComponentPreviewSurface { TimeFieldPreviewContent() }
}

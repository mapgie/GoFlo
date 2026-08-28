package com.mapgie.goflo.ui.screens.categories

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mapgie.goflo.ui.util.CATEGORY_COLOR_OPTIONS
import com.mapgie.goflo.ui.util.CategoryColor
import com.mapgie.goflo.ui.util.toHexColorKey

// The full HSV colour picker and the colour-token classification helpers,
// shared by ManageCategoriesScreen (adopt-colour rules) and
// CategoryEditScreen's custom fixed-colour slot. Relocated here in Phase 8
// when the superseded add/edit-appearance dialogs left
// ManageCategoriesScreen.kt.

/**
 * True when the token is a stored hex colour, i.e. any deliberately chosen
 * non-theme colour: a fixed swatch or a custom picker colour. Unlike
 * [isCustomColorToken] this includes the fixed swatches. The length check is
 * not sufficient on its own because "tertiary" is also 8 characters.
 */
internal fun isFixedColorToken(token: String): Boolean {
    if (token.length != 8) return false
    return CategoryColor.entries.none { it.key == token }
}

internal fun isCustomColorToken(token: String): Boolean {
    if (token.length != 8) return false
    val categoryColorKeys = CategoryColor.entries.map { it.key }.toSet()
    if (token in categoryColorKeys) return false
    val extendedHexKeys = CATEGORY_COLOR_OPTIONS.map { it.toHexColorKey() }.toSet()
    return token !in extendedHexKeys
}

// ── Full HSV colour picker dialog ─────────────────────────────────────────────

@Composable
internal fun FullColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val initHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor, initHsv)

    var hue        by remember { mutableStateOf(initHsv[0]) }
    var saturation by remember { mutableStateOf(initHsv[1]) }
    var value      by remember { mutableStateOf(initHsv[2]) }

    val currentArgb by remember(hue, saturation, value) {
        derivedStateOf {
            android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        }
    }
    var hexInput by remember(currentArgb) {
        mutableStateOf("%06X".format(currentArgb and 0xFFFFFF))
    }
    var hexError by remember { mutableStateOf(false) }

    fun applyHexInput(input: String) {
        hexInput = input.uppercase().filter { it.isLetterOrDigit() }.take(6)
        if (hexInput.length == 6) {
            runCatching {
                val parsed = android.graphics.Color.parseColor("#$hexInput")
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(parsed, hsv)
                hue        = hsv[0]
                saturation = hsv[1]
                value      = hsv[2]
                hexError   = false
            }.onFailure { hexError = true }
        } else {
            hexError = hexInput.isNotEmpty()
        }
    }

    val previewColor = Color(currentArgb or (0xFF shl 24))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Custom Colour", style = MaterialTheme.typography.headlineSmall)

                SaturationValuePanel(
                    hue        = hue,
                    saturation = saturation,
                    value      = value,
                    onChanged  = { s, v -> saturation = s; value = v }
                )

                HueSlider(hue = hue, onChanged = { hue = it })

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                    )
                    OutlinedTextField(
                        value         = hexInput,
                        onValueChange = { applyHexInput(it) },
                        label         = { Text("Hex") },
                        prefix        = { Text("#") },
                        singleLine    = true,
                        isError       = hexError,
                        modifier      = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val argb   = currentArgb
                        val hexKey = "FF%06X".format(argb and 0xFFFFFF)
                        onColorSelected(hexKey)
                    }) { Text("Done") }
                }
            }
        }
    }
}

// ── Saturation/Value panel ────────────────────────────────────────────────────

@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChanged: (saturation: Float, value: Float) -> Unit
) {
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    fun updateFromOffset(offset: Offset) {
                        val s = (offset.x / w).coerceIn(0f, 1f)
                        val v = (1f - offset.y / h).coerceIn(0f, 1f)
                        onChanged(s, v)
                    }
                    updateFromOffset(down.position)
                    drag(down.id) { change -> updateFromOffset(change.position) }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )
        val thumbX = saturation
        val thumbY = 1f - value
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = thumbX * size.width
            val cy = thumbY * size.height
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = Color.Black, radius = 12.dp.toPx(), center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()))
        }
    }
}

// ── Hue slider ────────────────────────────────────────────────────────────────

@Composable
private fun HueSlider(hue: Float, onChanged: (Float) -> Unit) {
    val hueColors = remember {
        listOf(
            Color(0xFFFF0000), Color(0xFFFF8000), Color(0xFFFFFF00), Color(0xFF80FF00),
            Color(0xFF00FF00), Color(0xFF00FF80), Color(0xFF00FFFF), Color(0xFF0080FF),
            Color(0xFF0000FF), Color(0xFF8000FF), Color(0xFFFF00FF), Color(0xFFFF0080),
            Color(0xFFFF0000),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(hueColors))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun updateFromOffset(offset: Offset) {
                        val h = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                        onChanged(h)
                    }
                    updateFromOffset(down.position)
                    drag(down.id) { change -> updateFromOffset(change.position) }
                }
            }
    ) {
        val thumbX = hue / 360f
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = thumbX * size.width
            val cy = size.height / 2f
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = Color.Black, radius = 12.dp.toPx(), center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()))
        }
    }
}

package com.mapgie.goflo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.goflo.ui.theme.ComfortaaFamily

/**
 * The tonal container that makes one metric the page: a big reading as words
 * ("Barely noticeable", "Pretty good") on a container tint of the metric's
 * [role] (blue for facts, amber for feelings; the user's group choice).
 *
 * The hero word uses the Comfortaa brand family, applied explicitly because
 * GoFloTypography does not wire it in. The word itself is onSurface so it
 * stays readable on the tint in every palette; the role communicates through
 * the container.
 *
 * [content] slots the metric's input control (typically a [StepScale]) under
 * the word.
 */
@Composable
fun ToneHero(
    word: String,
    role: Color,
    modifier: Modifier = Modifier,
    caption: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val container = roleContainerTint(role, MaterialTheme.colorScheme.surface)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = word,
            fontFamily = ComfortaaFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 23.sp,
            lineHeight = 30.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (caption != null) {
            Text(
                text = caption,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content?.invoke(this)
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Composable
private fun ToneHeroPreviewContent() {
    ToneHero(
        word = "Barely noticeable",
        role = MaterialTheme.colorScheme.primary,
        caption = "How bad is it right now?",
    ) {
        StepScale(
            name = "Nose symptoms",
            range = 1..5,
            value = 1,
            role = MaterialTheme.colorScheme.primary,
            onRole = MaterialTheme.colorScheme.onPrimary,
            onSelect = {},
            endLabels = "Barely there" to "Unbearable",
        )
    }
    ToneHero(
        word = "Pretty good",
        role = MaterialTheme.colorScheme.secondary,
        caption = "How did today feel overall?",
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ToneHeroPreviewLight() {
    ComponentPreviewSurface { ToneHeroPreviewContent() }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ToneHeroPreviewDark() {
    ComponentPreviewSurface(dark = true) { ToneHeroPreviewContent() }
}

@Preview(name = "Light 200%", showBackground = true, fontScale = 2f)
@Composable
private fun ToneHeroPreviewLarge() {
    ComponentPreviewSurface { ToneHeroPreviewContent() }
}

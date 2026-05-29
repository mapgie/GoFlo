package com.mapgie.goflo.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private const val ISSUES_URL = "https://github.com/mapgie/GoFlo/issues"
private const val URL_TAG = "url"

/**
 * A slim, full-width banner shown directly beneath the top app bar.
 *
 * Reads: "Thank you for using this Beta version. Feedback / Bug reports /
 * Feature suggestions welcome." — where the second sentence is a hyperlink to
 * the project's GitHub Issues page.
 */
@Composable
fun BetaFeedbackBanner(modifier: Modifier = Modifier) {
    val context   = LocalContext.current
    val baseColor = MaterialTheme.colorScheme.onSecondaryContainer
    val linkColor = MaterialTheme.colorScheme.primary
    val textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center)

    val annotated = remember(baseColor, linkColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = baseColor)) {
                append("Thank you for using this Beta version. ")
            }
            pushStringAnnotation(tag = URL_TAG, annotation = ISSUES_URL)
            withStyle(
                SpanStyle(
                    color          = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("Feedback / Bug reports / Feature suggestions welcome.")
            }
            pop()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color    = MaterialTheme.colorScheme.secondaryContainer
    ) {
        ClickableText(
            text     = annotated,
            style    = textStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) { offset ->
            annotated.getStringAnnotations(tag = URL_TAG, start = offset, end = offset)
                .firstOrNull()
                ?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item))) }
        }
    }
}

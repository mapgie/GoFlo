package com.mapgie.goflo.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private const val ISSUES_URL = "https://discord.gg/xphnQCZeYq"

/**
 * A slim, full-width banner shown directly beneath the top app bar.
 *
 * Reads: "Thank you for using this Beta version. Feedback is encouraged ♥" —
 * where the middle phrase is a hyperlink to Discord. The heart sits outside the
 * hyperlink so tapping it does not open the browser.
 *
 * Uses [LinkAnnotation] so the link is announced with the correct link role to
 * accessibility services and exposes a proper, full-size touch region.
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
            withLink(
                LinkAnnotation.Url(
                    url    = ISSUES_URL,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color          = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                ) {
                    val url = (it as LinkAnnotation.Url).url
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            ) {
                append("Feedback is encouraged")
            }
            withStyle(SpanStyle(color = baseColor)) {
                append(" ♥")
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color    = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text     = annotated,
            style    = textStyle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

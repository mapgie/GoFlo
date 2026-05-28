package com.mapgie.goflo.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A single parsed entry from CHANGELOG.md, e.g. [0.2.1-beta.1] - 2026-05-22 plus its body. */
data class ChangelogEntry(val header: String, val body: String)

/**
 * Parses [content] (CHANGELOG.md text) and returns up to [maxEntries] of the most recent
 * version entries (lines starting with `## [`), in the order they appear in the file.
 */
internal fun parseChangelog(content: String, maxEntries: Int = 5): List<ChangelogEntry> {
    val entries = mutableListOf<ChangelogEntry>()
    var currentHeader: String? = null
    val currentBody = StringBuilder()

    for (line in content.lines()) {
        when {
            line.startsWith("## [") -> {
                // Flush previous entry before starting a new one
                currentHeader?.let {
                    entries.add(ChangelogEntry(it, currentBody.toString().trimEnd()))
                }
                if (entries.size >= maxEntries) break
                currentHeader = line.removePrefix("## ")
                currentBody.clear()
            }
            currentHeader != null && line.trimEnd() != "---" -> {
                currentBody.appendLine(line)
            }
        }
    }

    // Flush the final entry if we haven't hit the limit
    if (currentHeader != null && entries.size < maxEntries) {
        entries.add(ChangelogEntry(currentHeader!!, currentBody.toString().trimEnd()))
    }

    return entries.take(maxEntries)
}

/**
 * Renders a markdown body string (### headings, - bullets, **bold**) as Compose UI.
 * Handles the subset of markdown used in CHANGELOG.md bodies.
 */
@Composable
private fun ChangelogBody(body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        body.lines().forEach { line ->
            when {
                line.startsWith("### ") -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val content = line.removePrefix("- ").removePrefix("* ")
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = renderInlineMarkdown(content),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(Modifier.height(2.dp))
                }
                else -> {
                    Text(
                        text = renderInlineMarkdown(line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Converts `**bold**` markers in [text] to a bold [AnnotatedString]. */
private fun renderInlineMarkdown(text: String) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", i + 2)
            if (end != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(text.substring(i + 2, end))
                pop()
                i = end + 2
            } else {
                append(text[i])
                i++
            }
        } else {
            append(text[i])
            i++
        }
    }
}

/**
 * AlertDialog that shows the last [maxEntries] changelog entries read from
 * `assets/CHANGELOG.md`.
 */
@Composable
fun ChangelogDialog(onDismiss: () -> Unit, maxEntries: Int = 5) {
    val context = LocalContext.current
    val entries = remember(maxEntries) {
        runCatching {
            val text = context.assets.open("CHANGELOG.md").bufferedReader().readText()
            parseChangelog(text, maxEntries)
        }.getOrDefault(emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's New") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (entries.isEmpty()) {
                    Text(
                        "No changelog available.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    entries.forEachIndexed { index, entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = entry.header,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            ChangelogBody(entry.body)
                        }
                        if (index < entries.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

package com.mapgie.goflo.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CategoriesHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Using Categories") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HelpSection(
                    "Grouped and Ungrouped",
                    "The Grouped view shows one card per group, tinted with the group's colour. The Ungrouped view lists categories that are not in any group. Both views show the same categories; nothing is hidden by switching."
                )
                HelpSection(
                    "Groups",
                    "A group collects related categories and gives them a shared colour role. Use Add to group to file a category, and Edit on a group card to rename it, change its colour, reorder it, or delete it. Deleting a group keeps its categories: they just become ungrouped."
                )
                HelpSection(
                    "Category types",
                    "Default: choose from a list of named values you define. Slider and Numeric Input: record a number. Plus One: tap to add to a daily count."
                )
                HelpSection(
                    "Values and order",
                    "For Default categories, tap the category to add or rename its values. The order of values in that list affects how they appear in Stats: the first value gets the lightest shade of the category colour, and the last gets the full colour."
                )
                HelpSection(
                    "Reorder categories",
                    "Tap the reorder button in the top bar to show every active category in one list, then long-press the drag handle on the right side of a row to pick it up and drag it to a new position."
                )
                HelpSection(
                    "Archive",
                    "Archiving hides a category from the logging screen without removing any of your data. You can restore it at any time from the Archived section at the bottom of this screen."
                )
                HelpSection(
                    "Delete",
                    "Swipe left on a custom category to delete it. Deleting permanently removes the category and all its logged data. This cannot be undone."
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

@Composable
internal fun HelpSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

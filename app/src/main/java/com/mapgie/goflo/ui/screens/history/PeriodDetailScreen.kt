package com.mapgie.goflo.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.goflo.ui.components.HairlineDivider
import com.mapgie.goflo.ui.components.ListCard
import com.mapgie.goflo.ui.components.SectionHeader
import com.mapgie.goflo.ui.components.ToneHero
import com.mapgie.goflo.ui.navigation.Screen
import com.mapgie.goflo.ui.util.effectiveColorToken
import com.mapgie.goflo.ui.util.toCategoryColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val monthDay = DateTimeFormatter.ofPattern("MMM d")
private val monthDayYear = DateTimeFormatter.ofPattern("MMM d, yyyy")

/**
 * One period episode expanded into its individual days.
 *
 * A tonal hero words the episode's range and length; a single list card holds
 * one row per logged day (day number, date, the day's flow as a word in the
 * Flow category's role colour, and a compact line for symptoms and other
 * logged categories). Tapping a day opens the unified day screen for that
 * date; the top bar's edit action opens the same screen on the first day,
 * where the episode's dates, notes, and deletion live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDetailScreen(
    viewModel: PeriodDetailViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.notFound) {
        if (state.notFound) onBack()
    }

    // Day-level data is a one-shot read: refresh whenever the screen returns
    // to composition after a day or the episode was edited underneath it.
    var composedBefore by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (composedBefore) viewModel.refresh() else composedBefore = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Period") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.startDate?.let { firstDay ->
                        IconButton(onClick = { onNavigate(Screen.LogDay.forDate(firstDay)) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Open first day")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        val period = state.period
        val start = state.startDate
        if (period == null || start == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isLoading) CircularProgressIndicator()
            }
            return@Scaffold
        }

        val flowToken = state.flowCategory?.effectiveColorToken(state.groups) ?: "primary"
        val flowRole = flowToken.toCategoryColor()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            ToneHero(
                word = rangeWording(start, state.endDate),
                role = flowRole,
                caption = summaryCaption(
                    lengthDays = state.lengthDays,
                    ongoing = state.endDate == null,
                    cycleLengthDays = state.cycleLengthDays,
                ),
            )

            SectionHeader(
                label = "Day by day",
                value = if (state.days.size == 1) "1 day logged" else "${state.days.size} days logged",
            )
            ListCard {
                state.days.forEachIndexed { index, day ->
                    if (index > 0) HairlineDivider()
                    PeriodDayRow(
                        day = day,
                        dateText = formatDayDate(day.date),
                        flowRole = flowRole,
                        onClick = { onNavigate(Screen.LogDay.forDate(day.date)) },
                    )
                }
            }

            if (period.notes.isNotBlank()) {
                SectionHeader(label = "Notes")
                ListCard {
                    Text(
                        text = period.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Day row ───────────────────────────────────────────────────────────────────

/**
 * One logged day: "Day N" with its date, the day's flow as a word in the Flow
 * category's role colour (the word carries the meaning; the colour reinforces
 * it), and a muted second line for symptoms and other logged categories.
 */
@Composable
private fun PeriodDayRow(
    day: PeriodDayDetail,
    dateText: String,
    flowRole: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { this.role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Day ${day.dayNumber}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = dateText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Tabular figures so the date column aligns across rows.
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }
            val secondary = buildList {
                if (day.symptoms.isNotEmpty()) add(day.symptoms.joinToString(", "))
                if (day.otherLoggedCount > 0) {
                    add(
                        if (day.otherLoggedCount == 1) "1 more logged"
                        else "${day.otherLoggedCount} more logged"
                    )
                }
            }.joinToString(" · ")
            if (secondary.isNotEmpty()) {
                Text(
                    text = secondary,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = day.flowLabel ?: "Not logged",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (day.flowLabel != null) flowRole
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Wording helpers ───────────────────────────────────────────────────────────

/** "Mar 3 to Mar 8", "Mar 3 to Mar 8, 2025", or "Started Mar 3, ongoing". */
private fun rangeWording(start: LocalDate, end: LocalDate?): String {
    val currentYear = LocalDate.now().year
    if (end == null) {
        val startText =
            if (start.year == currentYear) monthDay.format(start) else monthDayYear.format(start)
        return "Started $startText, ongoing"
    }
    return when {
        start.year != end.year ->
            "${monthDayYear.format(start)} to ${monthDayYear.format(end)}"
        start.year != currentYear ->
            "${monthDay.format(start)} to ${monthDay.format(end)}, ${end.year}"
        else ->
            "${monthDay.format(start)} to ${monthDay.format(end)}"
    }
}

/** "6 days", "4 days so far", with " · 28-day cycle" appended when known. */
private fun summaryCaption(lengthDays: Int, ongoing: Boolean, cycleLengthDays: Int?): String {
    val length = when {
        ongoing && lengthDays == 1 -> "1 day so far"
        ongoing -> "$lengthDays days so far"
        lengthDays == 1 -> "1 day"
        else -> "$lengthDays days"
    }
    return if (cycleLengthDays != null) "$length · $cycleLengthDays-day cycle" else length
}

/** "Mar 5" for current-year dates, "Mar 5, 2025" otherwise. */
private fun formatDayDate(date: LocalDate): String =
    if (date.year == LocalDate.now().year) monthDay.format(date) else monthDayYear.format(date)

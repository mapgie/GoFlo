package com.mapgie.goflo.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.time.format.DateTimeFormatter

@Composable
fun CalendarGrid(
    periodDays: Set<LocalDate>,
    predictedDays: Set<LocalDate>,
    ovulationDay: LocalDate?,
    /** Full ±2-day fertility window; each date in this set gets a small indicator dot. */
    ovulationWindow: Set<LocalDate> = emptySet(),
    /** Dates that have at least one tracking category log entry (non-period). */
    daysWithTrackingLogs: Set<LocalDate> = emptySet(),
    today: LocalDate = LocalDate.now(),
    /** Called on a normal tap. */
    onDayClick: (LocalDate) -> Unit,
    /** Called on a long-press. Opens Quick Log for that day. */
    onDayLongClick: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var displayMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    val currentMonth = YearMonth.now()

    Column(modifier = modifier) {
        MonthHeader(
            month = displayMonth,
            onPrev = { displayMonth = displayMonth.minusMonths(1) },
            onNext = { displayMonth = displayMonth.plusMonths(1) },
            showTodayButton = displayMonth != currentMonth,
            onJumpToToday = { displayMonth = currentMonth }
        )
        Spacer(Modifier.height(8.dp))
        DayOfWeekRow()
        Spacer(Modifier.height(4.dp))
        CalendarDays(
            month = displayMonth,
            periodDays = periodDays,
            predictedDays = predictedDays,
            ovulationDay = ovulationDay,
            ovulationWindow = ovulationWindow,
            daysWithTrackingLogs = daysWithTrackingLogs,
            today = today,
            onDayClick = onDayClick,
            onDayLongClick = onDayLongClick,
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    showTodayButton: Boolean,
    onJumpToToday: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
            }
            Text(
                text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
            }
        }
        // "Today" button: only visible when browsing away from the current month.
        if (showTodayButton) {
            TextButton(
                onClick = onJumpToToday,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DayOfWeekRow() {
    val days = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    Row(Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarDays(
    month: YearMonth,
    periodDays: Set<LocalDate>,
    predictedDays: Set<LocalDate>,
    ovulationDay: LocalDate?,
    ovulationWindow: Set<LocalDate>,
    daysWithTrackingLogs: Set<LocalDate>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    onDayLongClick: (LocalDate) -> Unit,
) {
    val firstDayOfMonth = month.atDay(1)
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
    val totalDays = month.lengthOfMonth()
    val cells = startOffset + totalDays
    val rows = (cells + 6) / 7

    Column {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val index = row * 7 + col
                    val dayNum = index - startOffset + 1
                    if (dayNum in 1..totalDays) {
                        val date = month.atDay(dayNum)
                        DayCell(
                            date = date,
                            isPeriod    = date in periodDays,
                            isPredicted = date in predictedDays,
                            isOvulation = date == ovulationDay,
                            isOvulationWindow = date in ovulationWindow && date != ovulationDay,
                            hasTrackingLog = date in daysWithTrackingLogs && date !in periodDays,
                            isToday     = date == today,
                            onClick     = { onDayClick(date) },
                            onLongClick = { onDayLongClick(date) },
                            modifier    = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private val accessibilityDateFormat = DateTimeFormatter.ofPattern("MMMM d")

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    date: LocalDate,
    isPeriod: Boolean,
    isPredicted: Boolean,
    isOvulation: Boolean,
    /** True for the ±2 days surrounding the peak ovulation day. */
    isOvulationWindow: Boolean,
    /** True when the day has a tracking log entry but is not a period day. */
    hasTrackingLog: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary   = MaterialTheme.colorScheme.primary
    // Ovulation markers use tertiary so they're visually distinct from period circles.
    val ovulColor = MaterialTheme.colorScheme.tertiary

    // Build a human-readable description for screen readers so users know the
    // date state without relying on colour or shape alone.
    val cellDescription = buildString {
        append(date.format(accessibilityDateFormat))
        if (isToday) append(", today")
        when {
            isPeriod    -> append(", period day")
            isPredicted -> append(", predicted period")
        }
        when {
            isOvulation       -> append(", ovulation day")
            isOvulationWindow -> append(", fertility window")
        }
        if (hasTrackingLog) append(", has tracking entry")
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            // Full cell is the touch target (≥48 dp on typical phones) so users
            // don't have to hit the 36 dp inner circle precisely.
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            // clearAndSetSemantics replaces all child semantics so TalkBack reads
            // only this description, not the raw day-number Text inside the Box.
            .clearAndSetSemantics { contentDescription = cellDescription }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        val circleModifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                when {
                    isPeriod    -> Modifier.background(primary)
                    isPredicted -> Modifier.border(1.5.dp, primary.copy(alpha = 0.5f), CircleShape)
                    else        -> Modifier
                }
            )

        Box(circleModifier, contentAlignment = Alignment.Center) {
            val textColor = when {
                isPeriod -> MaterialTheme.colorScheme.onPrimary
                isToday  -> primary
                else     -> MaterialTheme.colorScheme.onSurface
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                when {
                    isOvulation -> // 6 dp peak-day dot in tertiary colour
                        Box(Modifier.size(6.dp).clip(CircleShape)
                            .background(if (isPeriod) Color.White else ovulColor))
                    isOvulationWindow -> // 4 dp softer dot for surrounding window days
                        Box(Modifier.size(4.dp).clip(CircleShape)
                            .background((if (isPeriod) Color.White else ovulColor).copy(alpha = 0.5f)))
                    hasTrackingLog -> // 4 dp secondary-coloured dot for tracking entries
                        Box(Modifier.size(4.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary))
                }
            }
        }

        // Underline for today — shape-based cue that doesn't rely on colour alone.
        if (isToday && !isPeriod) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 16.dp, height = 2.dp)
                    .clip(CircleShape)
                    .background(primary)
            )
        }
    }
}

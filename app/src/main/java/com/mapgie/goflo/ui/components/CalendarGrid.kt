package com.mapgie.goflo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarGrid(
    periodDays: Set<LocalDate>,
    predictedDays: Set<LocalDate>,
    ovulationDay: LocalDate?,
    today: LocalDate = LocalDate.now(),
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }

    Column(modifier = modifier) {
        MonthHeader(
            month = displayMonth,
            onPrev = { displayMonth = displayMonth.minusMonths(1) },
            onNext = { displayMonth = displayMonth.plusMonths(1) }
        )
        Spacer(Modifier.height(8.dp))
        DayOfWeekRow()
        Spacer(Modifier.height(4.dp))
        CalendarDays(
            month = displayMonth,
            periodDays = periodDays,
            predictedDays = predictedDays,
            ovulationDay = ovulationDay,
            today = today,
            onDayClick = onDayClick
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
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
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit
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
                            isPeriod = date in periodDays,
                            isPredicted = date in predictedDays,
                            isOvulation = date == ovulationDay,
                            isToday = date == today,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f)
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

@Composable
private fun DayCell(
    date: LocalDate,
    isPeriod: Boolean,
    isPredicted: Boolean,
    isOvulation: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    // Build a human-readable description for screen readers so users know the
    // date state without relying on colour or shape alone.
    val cellDescription = buildString {
        append(date.format(accessibilityDateFormat))
        if (isToday) append(", today")
        when {
            isPeriod    -> append(", period day")
            isPredicted -> append(", predicted period")
        }
        if (isOvulation) append(", ovulation window")
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            // Full cell is the touch target (≥48 dp on typical phones) so users
            // don't have to hit the 36 dp inner circle precisely.
            .clickable(onClick = onClick)
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
                if (isOvulation) {
                    // 6 dp keeps the dot subtle while remaining perceptible at
                    // small sizes; white on period days, primary elsewhere.
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPeriod) Color.White else primary)
                    )
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

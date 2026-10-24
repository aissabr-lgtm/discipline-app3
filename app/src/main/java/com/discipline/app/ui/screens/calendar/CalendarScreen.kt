package com.discipline.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.discipline.app.ui.components.ColorScaleLegend
import com.discipline.app.ui.components.DisciplineCard
import com.discipline.app.ui.theme.ScoreColorNone
import com.discipline.app.ui.theme.getScoreComposeColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val monthTitle = uiState.selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & View Mode Switcher
        item {
            Column {
                Text(
                    text = "Calendar Performance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.viewMode == CalendarViewMode.MONTH,
                        onClick = { viewModel.setViewMode(CalendarViewMode.MONTH) },
                        label = { Text("Month") }
                    )
                    FilterChip(
                        selected = uiState.viewMode == CalendarViewMode.WEEK,
                        onClick = { viewModel.setViewMode(CalendarViewMode.WEEK) },
                        label = { Text("Week") }
                    )
                    FilterChip(
                        selected = uiState.viewMode == CalendarViewMode.YEAR,
                        onClick = { viewModel.setViewMode(CalendarViewMode.YEAR) },
                        label = { Text("Year Overview") }
                    )
                }
            }
        }

        // Period Switcher Navigation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Previous Month"
                    )
                }
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = "Next Month"
                    )
                }
            }
        }

        // Calendar Content based on view mode
        item {
            when (uiState.viewMode) {
                CalendarViewMode.MONTH -> {
                    MonthGridView(
                        yearMonth = uiState.selectedYearMonth,
                        scoresMap = uiState.monthScores,
                        onDateSelected = onDateSelected
                    )
                }
                CalendarViewMode.WEEK -> {
                    WeekGridView(
                        scoresMap = uiState.monthScores,
                        onDateSelected = onDateSelected
                    )
                }
                CalendarViewMode.YEAR -> {
                    YearHeatmapView(
                        year = uiState.selectedYearMonth.year,
                        scoresMap = uiState.monthScores,
                        onDateSelected = onDateSelected
                    )
                }
            }
        }

        // Color Scale Legend
        item {
            ColorScaleLegend()
        }
    }
}

@Composable
fun MonthGridView(
    yearMonth: YearMonth,
    scoresMap: Map<String, com.discipline.app.data.model.DailyScore>,
    onDateSelected: (String) -> Unit
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1 // 0 for Mon, 6 for Sun

    val weekdays = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )

    DisciplineCard(modifier = Modifier.fillMaxWidth()) {
        // Weekday header labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (day in weekdays) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Calendar Days Grid
        val totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7
        val todayStr = LocalDate.now().toString()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0 until (totalCells / 7)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - dayOfWeekOffset + 1

                        if (dayNumber in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNumber)
                            val dateStr = date.toString()
                            val dailyScore = scoresMap[dateStr]
                            val cellColor = if (dailyScore != null) {
                                getScoreComposeColor(dailyScore.colorHex)
                            } else {
                                ScoreColorNone
                            }
                            val isToday = dateStr == todayStr

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cellColor.copy(alpha = 0.22f))
                                    .border(
                                        width = if (isToday) 2.dp else 1.dp,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else cellColor.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onDateSelected(dateStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(cellColor)
                                    )
                                }
                            }
                        } else {
                            // Blank filler cell
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekGridView(
    scoresMap: Map<String, com.discipline.app.data.model.DailyScore>,
    onDateSelected: (String) -> Unit
) {
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

    DisciplineCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Current Week Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (date in weekDays) {
                val dateStr = date.toString()
                val score = scoresMap[dateStr]
                val color = if (score != null) getScoreComposeColor(score.colorHex) else ScoreColorNone
                val isToday = dateStr == today.toString()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.22f))
                        .border(
                            width = if (isToday) 2.dp else 1.dp,
                            color = if (isToday) MaterialTheme.colorScheme.primary else color.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onDateSelected(dateStr) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (score?.score != null) "${score.score.toInt()}%" else "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun YearHeatmapView(
    year: Int,
    scoresMap: Map<String, com.discipline.app.data.model.DailyScore>,
    onDateSelected: (String) -> Unit
) {
    DisciplineCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$year Year Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Daily discipline across the months",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Heatmap Grid: 12 months in mini blocks
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (monthVal in 1..12) {
                val ym = YearMonth.of(year, monthVal)
                val monthName = ym.format(DateTimeFormatter.ofPattern("MMM"))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(36.dp)
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (d in 1..ym.lengthOfMonth()) {
                            val dateStr = ym.atDay(d).toString()
                            val score = scoresMap[dateStr]
                            val color = if (score != null) getScoreComposeColor(score.colorHex) else ScoreColorNone
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                                    .clickable { onDateSelected(dateStr) }
                            )
                        }
                    }
                }
            }
        }
    }
}

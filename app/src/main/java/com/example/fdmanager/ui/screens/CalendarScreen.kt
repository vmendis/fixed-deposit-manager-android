package com.example.fdmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.domain.Dates
import com.example.fdmanager.domain.FdQueries
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.components.EmptyState
import com.example.fdmanager.ui.components.FdCard
import com.example.fdmanager.ui.components.SectionHeader
import com.example.fdmanager.ui.theme.statusPalette
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
private val WEEKDAYS = listOf("M", "T", "W", "T", "F", "S", "S") // Monday-first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    vm: FdViewModel,
    onOpenFd: (String) -> Unit
) {
    val fds by vm.fds.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(today) }

    val monthMats = remember(fds, month) { FdQueries.maturitiesInMonth(fds, month) }
    val palette = statusPalette()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Maturity calendar", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        LazyColumn(Modifier.fillMaxSize()) {
            // ---- Month card ----
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        // Month nav row
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                month = month.minusMonths(1)
                                if (selected != null && YearMonth.from(selected) != month) selected = null
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                            }
                            Text(
                                month.format(MONTH_FORMAT),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(onClick = {
                                month = month.plusMonths(1)
                                if (selected != null && YearMonth.from(selected) != month) selected = null
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                            }
                        }

                        // Weekday header
                        Row(Modifier.fillMaxWidth()) {
                            WEEKDAYS.forEach { d ->
                                Text(
                                    d,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))

                        // Day grid (Monday-first)
                        val offset = month.atDay(1).dayOfWeek.value - 1
                        val daysInMonth = month.lengthOfMonth()
                        val rows = (offset + daysInMonth + 6) / 7

                        for (row in 0 until rows) {
                            Row(Modifier.fillMaxWidth()) {
                                for (col in 0 until 7) {
                                    val dayNum = row * 7 + col - offset + 1
                                    if (dayNum < 1 || dayNum > daysInMonth) {
                                        Box(Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val date = month.atDay(dayNum)
                                        val dayFds = monthMats[date].orEmpty()
                                        DayCell(
                                            date = date,
                                            isToday = date == today,
                                            isSelected = date == selected,
                                            maturities = dayFds,
                                            statusColor = { status ->
                                                when (status) {
                                                    FdStatus.ACTIVE -> palette.active
                                                    FdStatus.MATURED -> palette.matured
                                                    FdStatus.RENEWED -> palette.renewed
                                                }
                                            },
                                            onClick = { selected = date },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${monthMats.values.sumOf { it.size }} maturity event(s) this month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Selected day list ----
            item {
                SectionHeader(
                    title = selected?.let { "Maturing on ${Dates.format(it)}" } ?: "Select a day",
                )
            }

            val dayFds = selected?.let { monthMats[it] }
                .takeIf { selected != null && YearMonth.from(selected) == month }

            if (selected == null || YearMonth.from(selected) != month) {
                item {
                    EmptyState(Icons.Filled.Event, "Pick a day", "Days with dots have FD maturities.")
                }
            } else if (dayFds.isNullOrEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.Event,
                        "Quiet day",
                        "No FDs mature on ${Dates.format(selected!!)}."
                    )
                }
            } else {
                items(dayFds, key = { it.id }) { fd ->
                    FdCard(fd = fd, today = today, onClick = { onOpenFd(fd.id) })
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    maturities: List<FixedDeposit>,
    statusColor: (FdStatus) -> Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (maturities.isNotEmpty()) {
                Row {
                    maturities.take(3).forEach { fd ->
                        Box(
                            Modifier
                                .padding(horizontal = 1.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else statusColor(fd.status)
                                )
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

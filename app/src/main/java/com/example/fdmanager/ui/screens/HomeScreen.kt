package com.example.fdmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fdmanager.data.model.BankSummary
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.domain.Dates
import com.example.fdmanager.domain.FdMath
import com.example.fdmanager.domain.FdQueries
import com.example.fdmanager.domain.Lkr
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.components.BankMonogram
import com.example.fdmanager.ui.components.CountdownChip
import com.example.fdmanager.ui.components.EmptyState
import com.example.fdmanager.ui.components.SectionHeader
import com.example.fdmanager.ui.components.StatMini
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: FdViewModel,
    onOpenBank: (String) -> Unit,
    onOpenFd: (String) -> Unit,
    onOpenBin: () -> Unit
) {
    val fds by vm.fds.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    val summaries = remember(fds) { FdQueries.bankSummaries(fds) }
    val total = remember(fds) { FdQueries.totalInvested(fds) }
    val activeCount = remember(fds) { FdQueries.visible(fds).count { it.status == FdStatus.ACTIVE } }
    val nextMaturity = remember(fds) {
        FdQueries.visible(fds).filter { it.status == FdStatus.ACTIVE }.minByOrNull { it.maturityDate }
    }
    val maturing = remember(fds, settings) {
        FdQueries.maturingSoon(fds, today, settings.notifyLeadDays)
    }
    var showMaturing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("FD Manager", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { showMaturing = true }) {
                    BadgedBox(badge = {
                        if (maturing.isNotEmpty() && settings.notificationsEnabled) {
                            Badge { Text("${maturing.size}") }
                        }
                    }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Maturing soon")
                    }
                }
                IconButton(onClick = onOpenBin) {
                    Icon(Icons.Filled.Delete, contentDescription = "Recycle bin")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ---- Hero: total invested ----
            item {
                ElevatedCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            "Total invested",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            Lkr.words(total),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            Lkr.exact(total),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            StatMini(
                                "Active FDs", "$activeCount",
                                Modifier.weight(1f),
                                onColor = MaterialTheme.colorScheme.onSurface
                            )
                            StatMini(
                                "Banks", "${summaries.size}",
                                Modifier.weight(1f),
                                onColor = MaterialTheme.colorScheme.onSurface
                            )
                            StatMini(
                                "Next maturity",
                                nextMaturity?.let {
                                    FdMath.countdownShort(FdMath.daysUntil(it.maturityDate, today))
                                } ?: "—",
                                Modifier.weight(1f),
                                onColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // ---- Maturing soon strip ----
            if (settings.notificationsEnabled && maturing.isNotEmpty()) {
                item {
                    SectionHeader(
                        "Maturing soon",
                        subtitle = "Within ${settings.notifyLeadDays} days — renewal time"
                    )
                }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(maturing, key = { it.id }) { fd ->
                            val days = FdMath.daysUntil(fd.maturityDate, today)
                            ElevatedCard(
                                onClick = { onOpenFd(fd.id) },
                                modifier = Modifier
                                    .width(210.dp)
                                    .padding(end = 12.dp, bottom = 4.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        fd.bank,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        fd.fdNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${Lkr.compact(fd.amount)}  •  ${Dates.format(fd.maturityDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    CountdownChip(days)
                                }
                            }
                        }
                    }
                }
            }

            // ---- Bank summary cards ----
            item {
                SectionHeader("By bank", subtitle = "${summaries.size} banks")
            }

            if (summaries.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.AccountBalance,
                        "No deposits yet",
                        "Tap + to add your first fixed deposit."
                    )
                }
            } else {
                items(summaries, key = { it.bank }) { summary ->
                    BankSummaryCard(summary = summary, onClick = { onOpenBank(summary.bank) })
                }
            }
        }
    }

    // ---- Maturing soon dialog ----
    if (showMaturing) {
        AlertDialog(
            onDismissRequest = { showMaturing = false },
            confirmButton = {
                TextButton(onClick = { showMaturing = false }) { Text("Close") }
            },
            title = { Text("Maturing soon") },
            text = {
                if (maturing.isEmpty()) {
                    Text("No FDs mature within ${settings.notifyLeadDays} days.")
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        maturing.forEach { fd ->
                            val days = FdMath.daysUntil(fd.maturityDate, today)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { showMaturing = false; onOpenFd(fd.id) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(fd.fdNumber, fontWeight = FontWeight.Bold)
                                    Text(
                                        fd.bank,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                CountdownChip(days)
                            }
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankSummaryCard(summary: BankSummary, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("summary:${summary.bank}")
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BankMonogram(bank = summary.bank, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    summary.bank,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${summary.activeCount} active • ${summary.fdCount} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Lkr.compact(summary.totalInvested),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "invested",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

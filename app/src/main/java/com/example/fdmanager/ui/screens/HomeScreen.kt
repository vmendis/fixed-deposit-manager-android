package com.example.fdmanager.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vssbox.fdmanager.R
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
import com.example.fdmanager.ui.components.FullScreenError
import com.example.fdmanager.ui.components.FullScreenLoading
import com.example.fdmanager.ui.components.InstitutionRegistry
import com.example.fdmanager.ui.components.LOADSTATE_GATE
import com.example.fdmanager.ui.components.LoadingList
import com.example.fdmanager.ui.components.SectionHeader
import com.example.fdmanager.ui.components.StatMini
import com.example.fdmanager.ui.components.SyncStateChip
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: FdViewModel,
    onOpenBank: (String) -> Unit,
    onOpenFd: (String) -> Unit,
    onOpenBin: () -> Unit,
    onAddFd: (() -> Unit)? = null // #1 empty state CTA
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
    // #3 sync-state chip — mock states for design review, 0 quota, swappable to FirebaseSyncRepository Phase 2
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    val lastSyncError by vm.lastSyncError.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // SYNCSTATE_GATE = syncstate-0300r9 — freshness marker for V30
    // Issue #21 fix: Back from bank list returned mid-scroll with hero cut off (LazyColumn state preserved via NavHost saveState/restoreState)
    // Fix: use explicit non-saveable LazyListState so returning from FdListScreen resets to top (hero visible)
    // Alternative considered: smooth scroll to top on back, but reset is simpler and matches expected UX
    // HOME_SCROLL_GATE = home-scroll-0100r9 — freshness marker for V23
    val listState = rememberLazyListState()
    // Ensure top visible when Home re-enters composition (back from bank list)
    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }
    // Show Snackbar for sync errors with Retry action — per #3 acceptance
    LaunchedEffect(lastSyncError) {
        lastSyncError?.let { error ->
            val result = snackbarHost.showSnackbar(
                message = error,
                actionLabel = "Retry",
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) {
                scope.launch {
                    vm.retrySync()
                }
            }
        }
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("FD Manager", fontWeight = FontWeight.Bold)
                    Text(
                        "by vssbox",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            },
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
            state = listState,
            modifier = Modifier.fillMaxSize().testTag("home-scroll-0100r9"),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ---- Sync-state chip #3 — persistent small indicator as first item ----
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("syncstate-0300r9"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SyncStateChip(state = syncState, modifier = Modifier.testTag("syncChip"))
                    Text(
                        text = when (syncState) {
                            com.example.fdmanager.domain.SyncState.SAVED -> "Local changes saved"
                            com.example.fdmanager.domain.SyncState.SYNCING -> "Syncing to cloud (mock)…"
                            com.example.fdmanager.domain.SyncState.OFFLINE -> "Offline — local only"
                            com.example.fdmanager.domain.SyncState.ERROR -> "Sync failed — check Snackbar for Retry"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("syncStateText")
                    )
                }
            }

            // ---- #4 Loading & error patterns — debug toggles for review ----
            if (settings.isLoadingDebug) {
                item {
                    FullScreenLoading(
                        message = "Loading FDs (mock #4)...",
                        modifier = Modifier.fillMaxWidth().height(200.dp).testTag(LOADSTATE_GATE)
                    )
                }
                item {
                    LoadingList(count = 3, modifier = Modifier.fillMaxWidth())
                }
            }
            if (settings.isErrorDebug) {
                item {
                    FullScreenError(
                        title = "Failed to load",
                        message = settings.debugErrorMessage,
                        onRetry = { vm.clearLoadingErrorDebug() },
                        modifier = Modifier.fillMaxWidth().height(300.dp).testTag(LOADSTATE_GATE)
                    )
                }
            }

            // ---- Hero: total invested ---- (hidden when loading/error debug for review)
            if (!settings.isLoadingDebug && !settings.isErrorDebug) {
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
                                "Institutions", "${summaries.size}",
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

            // ---- Institution summary cards — CBSL-only ----
            item {
                SectionHeader("By institution", subtitle = "${summaries.size} institutions • CBSL-regulated only")
            }

            if (summaries.isEmpty()) {
                item {
                    // #1 empty state with CTA — illustration-free, clear copy + primary action
                    EmptyState(
                        Icons.Filled.AccountBalance,
                        "No deposits yet",
                        if (settings.isDemoMode) "Tap + to add your first fixed deposit, or turn off demo mode in Settings for a clean start." else "Welcome! Add your first fixed deposit to see totals, maturing soon, and by institution. Your data is encrypted and only you can see it.",
                        actionLabel = if (onAddFd != null) "Add your first FD" else null,
                        onAction = onAddFd
                    )
                }
            } else {
                items(summaries, key = { it.bank }) { summary ->
                    BankSummaryCard(summary = summary, onClick = { onOpenBank(summary.bank) })
                }
            }

            // #27 branding — Home footer Built by vssbox with cube 16dp + ©
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp)
                        .testTag("builtByVssboxHome"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vssbox_cube),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Built by vssbox • © 2026 vssbox • FD Manager by vssbox",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            } // end if !loading && !error — #4
        }
        }

        // Snackbar for sync errors #3
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
    val identity = InstitutionRegistry.find(summary.bank)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${summary.activeCount} active • ${summary.fdCount} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (identity != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "• ${identity.type.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Lkr.words(summary.totalInvested),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    Lkr.exact(summary.totalInvested),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "invested",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

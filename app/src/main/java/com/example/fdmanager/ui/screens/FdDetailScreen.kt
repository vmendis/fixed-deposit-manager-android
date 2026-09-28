package com.example.fdmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.domain.Dates
import com.example.fdmanager.domain.FdMath
import com.example.fdmanager.domain.FdQueries
import com.example.fdmanager.domain.Lkr
import androidx.compose.ui.platform.testTag
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.components.BankMonogram
import com.example.fdmanager.ui.components.CountdownChip
import com.example.fdmanager.ui.components.InfoRow
import com.example.fdmanager.ui.components.StatusChip
import com.example.fdmanager.ui.theme.statusPalette
import java.time.LocalDate

private fun durationLabel(months: Int): String =
    if (months % 12 == 0) "$months months (${months / 12} yr)" else "$months months"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdDetailScreen(
    vm: FdViewModel,
    fdId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenFd: (String) -> Unit
) {
    val fds by vm.fds.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val fd = FdQueries.byId(fds, fdId)

    // If the FD was deleted from elsewhere, leave quietly.
    if (fd == null || fd.isDeleted) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val days = FdMath.daysUntil(fd.maturityDate, today)
    val interest = FdMath.interestEarned(fd.amount, fd.interestRate, fd.durationMonths)
    val maturityValue = fd.amount + interest
    val chain = remember(fds, fdId) { FdQueries.renewalChain(fds, fdId) }
    var showDelete by remember { mutableStateOf(false) }
    var showRenew by remember { mutableStateOf(false) }
    val palette = statusPalette()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = { Text(fd.fdNumber, fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { onEdit(fd.id) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ---- Header ----
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BankMonogram(bank = fd.bank, size = 52.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        fd.bank,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Lkr.full(fd.amount),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        StatusChip(fd.status)
                        Spacer(Modifier.width(8.dp))
                        CountdownChip(days)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        FdMath.countdownLabel(days),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---- Returns ----
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Returns (estimate)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Interest rate", "${FdMath.formatRate(fd.interestRate)}% p.a.")
                    InfoRow("Est. interest earned", Lkr.full(interest))
                    InfoRow("Est. maturity value", Lkr.full(maturityValue))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Simple-interest estimate — the actual payout depends on the bank's terms.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---- Details ----
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoRow("FD number", fd.fdNumber)
                    InfoRow("Bank", fd.bank)
                    if (fd.branch != null) {
                        InfoRow("Branch", fd.branch + (fd.branchCode?.let { " ($it)" } ?: ""))
                    }
                    InfoRow("Opened", Dates.format(fd.openedDate))
                    InfoRow("Duration", durationLabel(fd.durationMonths))
                    InfoRow("Maturity", Dates.format(fd.maturityDate))
                    InfoRow("Auto-renew", if (fd.autoRenew) "On" else "Off")
                }
            }

            if (fd.autoRenew && fd.status == FdStatus.ACTIVE) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Autorenew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Auto-renewal is on — this FD is set to renew with the same terms at maturity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---- Renewal history ----
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Renewal history",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    if (chain.size <= 1) {
                        Text(
                            "No renewals yet. Renewing this FD will start a history chain here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        chain.forEach { link ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = link.id != fdId) { onOpenFd(link.id) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (icon, tint) = when (link.status) {
                                    FdStatus.ACTIVE -> Icons.Filled.CheckCircle to palette.active
                                    FdStatus.MATURED -> Icons.Filled.Warning to palette.matured
                                    FdStatus.RENEWED -> Icons.Filled.History to palette.renewed
                                }
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        link.fdNumber + if (link.id == fdId) "   (this FD)" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${Dates.format(link.openedDate)} → ${Dates.format(link.maturityDate)}  •  " +
                                            "${FdMath.formatRate(link.interestRate)}%  •  ${Lkr.compact(link.amount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Oldest → newest",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Actions ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showRenew = true },
                    enabled = fd.status != FdStatus.RENEWED,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Renew")
                }
                Button(
                    onClick = { showDelete = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete FD?") },
            text = { Text("${fd.fdNumber} will be moved to the recycle bin. Nothing is permanently lost.") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("dialogConfirm"),
                    onClick = {
                        showDelete = false
                        vm.softDelete(fd.id)
                        onBack()
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(modifier = Modifier.testTag("dialogDismiss"), onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenew) {
        AlertDialog(
            onDismissRequest = { showRenew = false },
            title = { Text("Renew FD?") },
            text = {
                Text(
                    "${fd.fdNumber} will be marked as renewed. A new FD opens on ${Dates.format(fd.maturityDate)} " +
                        "for ${Lkr.full(fd.amount)} at ${FdMath.formatRate(fd.interestRate)}% for ${fd.durationMonths} months."
                )
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("dialogConfirm"),
                    onClick = {
                        showRenew = false
                        val newId = vm.renew(fd.id)
                        onOpenFd(newId)
                    }
                ) { Text("Renew") }
            },
            dismissButton = {
                TextButton(modifier = Modifier.testTag("dialogDismiss"), onClick = { showRenew = false }) { Text("Cancel") }
            }
        )
    }
}

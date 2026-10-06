package com.example.fdmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.RenewOption
import com.example.fdmanager.domain.Dates
import com.example.fdmanager.domain.FdMath
import com.example.fdmanager.domain.Lkr
import androidx.compose.ui.platform.testTag
import com.example.fdmanager.ui.theme.statusPalette
import java.time.LocalDate

@Composable
fun smallChipColors(status: FdStatus): Pair<Color, Color> {
    val p = statusPalette()
    return when (status) {
        FdStatus.ACTIVE -> p.activeContainer to p.active
        FdStatus.MATURED -> p.maturedContainer to p.matured
        FdStatus.RENEWED -> p.renewedContainer to p.renewed
    }
}

@Composable
fun StatusChip(status: FdStatus) {
    val (bg, fg) = smallChipColors(status)
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Days-to-maturity chip: red when urgent/overdue, amber within a month, neutral otherwise. */
@Composable
fun CountdownChip(days: Long) {
    val p = statusPalette()
    val (bg, fg) = when {
        days <= 5 -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        days <= 30 -> p.maturedContainer to p.matured
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = FdMath.countdownShort(days),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Shared FD card used by the bank list, calendar and dialogs.
 * Overflow menu (edit / renew / delete) is shown only when the callbacks are provided.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdCard(
    fd: FixedDeposit,
    today: LocalDate,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onRenew: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    val days = FdMath.daysUntil(fd.maturityDate, today)

    ElevatedCard(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    fd.fdNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusChip(fd.status)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Lkr.full(fd.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                CountdownChip(days)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // Issue #17 + #25 (R1): tenor replaces payout in compact meta — spec 4.2 card shows "9.5% p.a. • 100 days • matures ..."
                    "${FdMath.formatRate(fd.interestRate)}% p.a.  •  ${FdMath.tenorLabel(fd)}  •  matures ${Dates.format(fd.maturityDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (fd.autoRenew) {
                    Icon(
                        Icons.Filled.Autorenew, contentDescription = "Auto-renew on",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)
                    )
                }
                if (onEdit != null || onRenew != null || onDelete != null) {
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More actions", modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (onEdit != null) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                    onClick = { menuOpen = false; onEdit() }
                                )
                            }
                            if (onRenew != null && fd.status != FdStatus.RENEWED) {
                                DropdownMenuItem(
                                    text = { Text("Renew") },
                                    leadingIcon = { Icon(Icons.Filled.Autorenew, null) },
                                    onClick = { menuOpen = false; onRenew() }
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                    onClick = { menuOpen = false; onDelete() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Small label/value stat used in the dashboard hero card. */
@Composable
fun StatMini(label: String, value: String, modifier: Modifier = Modifier, onColor: Color = Color.Unspecified) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = onColor.copy(alpha = 0.8f))
    }
}

/**
 * Issue #25 (R2) — shared renew confirmation. Two payout options, pre-selected from the
 * FD's stored instruction (owner answer: stored instruction, dialog can override).
 * Confirm/dismiss keep the `dialogConfirm` / `dialogDismiss` tags (selector contract).
 */
@Composable
fun RenewDialog(
    fd: FixedDeposit,
    onConfirm: (RenewOption) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(fd.id) { mutableStateOf(fd.renewOption) }
    val interest = FdMath.interestEarned(fd)
    val nextAmount = if (selected == RenewOption.CAPITALIZE) fd.amount + interest else fd.amount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renew FD?") },
        text = {
            Column {
                Text(
                    "${fd.fdNumber} will be marked as renewed. A new FD opens on ${Dates.format(fd.maturityDate)} " +
                        "for ${Lkr.full(nextAmount)} at ${FdMath.formatRate(fd.interestRate)}% for " +
                        "${FdMath.tenorLabel(fd)}, linked to this one."
                )
                Spacer(Modifier.height(12.dp))
                RenewOption.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = selected == option, onClick = { selected = option })
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text(option.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when (option) {
                                    RenewOption.CAPITALIZE ->
                                        "Interest ${Lkr.full(interest)} is added to the new principal."
                                    RenewOption.PAYOUT ->
                                        "Interest ${Lkr.full(interest)} is paid out — reopens with ${Lkr.full(fd.amount)}."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("dialogConfirm"),
                onClick = { onConfirm(selected) }
            ) { Text("Renew") }
        },
        dismissButton = {
            TextButton(modifier = Modifier.testTag("dialogDismiss"), onClick = onDismiss) { Text("Cancel") }
        }
    )
}

package com.example.fdmanager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.SortOption
import com.example.fdmanager.data.model.StatusFilter
import com.example.fdmanager.domain.Dates
import com.example.fdmanager.domain.FdQueries
import com.example.fdmanager.domain.Lkr
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.components.EmptyState
import com.example.fdmanager.ui.components.FdCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdListScreen(
    vm: FdViewModel,
    bank: String,
    onBack: () -> Unit,
    onOpenFd: (String) -> Unit,
    onEditFd: (String) -> Unit,
    onAdd: () -> Unit
) {
    val fds by vm.fds.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var filter by remember { mutableStateOf(StatusFilter.ALL) }
    var sort by remember { mutableStateOf(SortOption.MATURITY_ASC) }
    var sortMenu by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FixedDeposit?>(null) }
    var renewTarget by remember { mutableStateOf<FixedDeposit?>(null) }

    val list = remember(fds, bank, filter, sort) {
        FdQueries.forBank(fds, bank, filter, sort)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = { Text(bank, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            actions = {
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                trailingIcon = {
                                    if (option == sort) Icon(Icons.Filled.Check, contentDescription = null)
                                },
                                onClick = { sort = option; sortMenu = false }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Status filter chips
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            StatusFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) }
                )
                Spacer(Modifier.width(8.dp))
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.AccountBalanceWallet,
                        "No FDs here",
                        if (filter == StatusFilter.ALL) "This bank has no deposits." else "No ${filter.label.lowercase()} FDs — try another filter."
                    )
                }
            } else {
                items(list, key = { it.id }) { fd ->
                    FdCard(
                        fd = fd,
                        today = today,
                        onClick = { onOpenFd(fd.id) },
                        onEdit = { onEditFd(fd.id) },
                        onRenew = { renewTarget = fd },
                        onDelete = { deleteTarget = fd }
                    )
                }
            }
        }
    }

    // ---- Confirm dialogs ----
    deleteTarget?.let { fd ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete FD?") },
            text = {
                Text("${fd.fdNumber} (${Lkr.full(fd.amount)}) will be moved to the recycle bin. You can restore it any time — nothing is permanently lost.")
            },
            confirmButton = {
                TextButton(onClick = { vm.softDelete(fd.id); deleteTarget = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    renewTarget?.let { fd ->
        AlertDialog(
            onDismissRequest = { renewTarget = null },
            title = { Text("Renew FD?") },
            text = {
                Text(
                    "${fd.fdNumber} will be marked as renewed. A new FD opens on ${Dates.format(fd.maturityDate)} " +
                        "for ${Lkr.full(fd.amount)} at ${fd.interestRate}% for ${fd.durationMonths} months, linked to this one."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newId = vm.renew(fd.id)
                    renewTarget = null
                    onOpenFd(newId)
                }) { Text("Renew") }
            },
            dismissButton = {
                TextButton(onClick = { renewTarget = null }) { Text("Cancel") }
            }
        )
    }
}

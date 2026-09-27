package com.example.fdmanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.domain.Dates
import com.example.fdmanager.domain.FdQueries
import com.example.fdmanager.domain.Lkr
import androidx.compose.ui.platform.testTag
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    vm: FdViewModel,
    onBack: () -> Unit
) {
    val fds by vm.fds.collectAsStateWithLifecycle()
    val bin = remember(fds) { FdQueries.bin(fds) }
    var purgeTarget by remember { mutableStateOf<FixedDeposit?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = { Text("Recycle bin", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            if (bin.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.RestoreFromTrash,
                        "Bin is empty",
                        "Deleted FDs land here thanks to soft-delete — restored with one tap."
                    )
                }
            } else {
                items(bin, key = { it.id }) { fd ->
                    ElevatedCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("bin:${fd.fdNumber}")
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    fd.fdNumber,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    fd.bank,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${Lkr.full(fd.amount)}  •  matured ${Dates.format(fd.maturityDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { vm.restore(fd.id) }) { Text("Restore") }
                            IconButton(onClick = { purgeTarget = fd }) {
                                Icon(
                                    Icons.Filled.DeleteForever,
                                    contentDescription = "Delete forever",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    purgeTarget?.let { fd ->
        AlertDialog(
            onDismissRequest = { purgeTarget = null },
            title = { Text("Delete forever?") },
            text = { Text("${fd.fdNumber} will be permanently removed. This bypasses the soft-delete safeguard and cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteForever(fd.id); purgeTarget = null }) {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { purgeTarget = null }) { Text("Cancel") }
            }
        )
    }
}

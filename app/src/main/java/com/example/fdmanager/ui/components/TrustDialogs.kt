package com.example.fdmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fdmanager.domain.DataWipe
import com.example.fdmanager.domain.ExportFormat
import com.example.fdmanager.domain.WipeCategory
import com.example.fdmanager.domain.WipeSelection

/**
 * #5 Trust UX — the two dialogs behind the Data section.
 *
 * Selector contract:
 *   exportFormatDialog / exportCsv / exportJson / exportCancel
 *   wipeDataDialog / wipeConfirm / wipeCancel / wipeTypedInput / wipeCategory_<NAME>
 */

@Composable
fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onPick: (ExportFormat) -> Unit
) {
    var selected by remember { mutableStateOf(ExportFormat.CSV) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("exportFormatDialog"),
        title = { Text("Export deposits", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Choose a format, then pick where to save it.",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ExportFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = selected == format,
                                onValueChange = { selected = format }
                            )
                            .padding(vertical = 6.dp)
                            .testTag("exportFormat_${format.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == format,
                            onClick = { selected = format }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(format.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                "." + format.extension,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("exportConfirm"),
                onClick = { onPick(selected) }
            ) { Text("Choose location") }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag("exportCancel"),
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}

/**
 * Destructive-action dialog. Confirm stays disabled until a category is ticked AND the owner has
 * typed the exact phrase — a single tap must never be able to erase everything.
 */
@Composable
fun DeleteAllDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (WipeSelection) -> Unit
) {
    var selection by remember { mutableStateOf(WipeSelection.NOTHING) }
    var typed by remember { mutableStateOf("") }

    val canWipe = DataWipe.canWipe(selection, typed)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("wipeDataDialog"),
        title = { Text("Delete data", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Choose what to remove. This cannot be undone — export first if you need a copy.",
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                WipeCategory.entries.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = selection.includes(category),
                                onValueChange = { checked ->
                                    val next = if (checked) {
                                        selection.categories + category
                                    } else {
                                        selection.categories - category
                                    }
                                    selection = WipeSelection(next)
                                }
                            )
                            .padding(vertical = 4.dp)
                            .testTag("wipeCategory_${category.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selection.includes(category),
                            onCheckedChange = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(category.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                category.detail,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text("Type ${DataWipe.CONFIRM_PHRASE} to confirm:")
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("wipeTypedInput")
                )

                if (!selection.isEmpty) {
                    Text(
                        "Will remove: ${DataWipe.summary(selection)}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("wipeConfirm"),
                enabled = canWipe,
                onClick = { onConfirm(selection) }
            ) { Text("Delete", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag("wipeCancel"),
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}

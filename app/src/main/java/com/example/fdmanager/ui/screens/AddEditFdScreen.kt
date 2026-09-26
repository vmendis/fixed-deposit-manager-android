package com.example.fdmanager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.SRI_LANKAN_BANKS
import com.example.fdmanager.domain.Dates
import com.example.fdmanager.domain.FdMath
import com.example.fdmanager.domain.FdQueries
import com.example.fdmanager.domain.Lkr
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.components.InfoRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val DURATION_PRESETS = listOf(3, 6, 12, 24, 36, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFdScreen(
    vm: FdViewModel,
    fdId: String?,
    onBack: () -> Unit
) {
    val fds by vm.fds.collectAsStateWithLifecycle()
    val editing = fdId?.let { FdQueries.byId(fds, it) }
    val today = remember { LocalDate.now() }

    var attempted by remember { mutableStateOf(false) }
    var fdNumber by remember { mutableStateOf(editing?.fdNumber ?: "") }
    var bank by remember { mutableStateOf(editing?.bank ?: SRI_LANKAN_BANKS[0]) }
    var amountText by remember {
        mutableStateOf(editing?.let { FdMath.formatRate(it.amount) } ?: "")
    }
    var rateText by remember {
        mutableStateOf(editing?.let { FdMath.formatRate(it.interestRate) } ?: "")
    }
    var customMode by remember {
        mutableStateOf(editing != null && editing.durationMonths !in DURATION_PRESETS)
    }
    var selectedDuration by remember {
        mutableStateOf(
            if (editing != null && editing.durationMonths in DURATION_PRESETS) editing.durationMonths else 12
        )
    }
    var customDurationText by remember {
        mutableStateOf(
            if (editing != null && editing.durationMonths !in DURATION_PRESETS) editing.durationMonths.toString() else ""
        )
    }
    var openedDate by remember { mutableStateOf(editing?.openedDate ?: LocalDate.now()) }
    var branch by remember { mutableStateOf(editing?.branch ?: "") }
    var branchCode by remember { mutableStateOf(editing?.branchCode ?: "") }
    var autoRenew by remember { mutableStateOf(editing?.autoRenew ?: false) }
    var bankMenu by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    // ---- Derived values & validation ----
    val amount = amountText.toDoubleOrNull()
    val rate = rateText.toDoubleOrNull()
    val duration: Int? = if (customMode) customDurationText.toIntOrNull() else selectedDuration

    val fdNumberValid = fdNumber.isNotBlank()
    val amountValid = amount != null && amount > 0
    val rateValid = rate != null && rate > 0 && rate <= 30
    val durationValid = duration != null && duration > 0
    val allValid = fdNumberValid && amountValid && rateValid && durationValid

    val previewReady = amountValid && rateValid && durationValid
    val previewMaturity = if (durationValid) FdMath.maturityDate(openedDate, duration!!) else null

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = { Text(if (editing == null) "Add FD" else "Edit FD") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = fdNumber,
                onValueChange = { fdNumber = it },
                label = { Text("FD number") },
                singleLine = true,
                isError = attempted && !fdNumberValid,
                supportingText = {
                    if (attempted && !fdNumberValid) Text("Required — e.g. NSB-78412")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Bank selector
            ExposedDropdownMenuBox(
                expanded = bankMenu,
                onExpandedChange = { bankMenu = !bankMenu }
            ) {
                OutlinedTextField(
                    value = bank,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bank") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bankMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = bankMenu,
                    onDismissRequest = { bankMenu = false }
                ) {
                    SRI_LANKAN_BANKS.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b) },
                            onClick = { bank = b; bankMenu = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text("Rs ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = attempted && !amountValid,
                supportingText = {
                    if (attempted && !amountValid) Text("Enter an amount greater than 0")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Interest rate") },
                suffix = { Text("% p.a.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = attempted && !rateValid,
                supportingText = {
                    if (attempted && !rateValid) Text("Enter a rate between 0 and 30")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // Duration
            Text("Duration", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                DURATION_PRESETS.forEach { months ->
                    FilterChip(
                        selected = !customMode && selectedDuration == months,
                        onClick = { customMode = false; selectedDuration = months },
                        label = { Text("${months}m") }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                FilterChip(
                    selected = customMode,
                    onClick = { customMode = true },
                    label = { Text("Custom") }
                )
            }
            if (customMode) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customDurationText,
                    onValueChange = { customDurationText = it.filter { c -> c.isDigit() } },
                    label = { Text("Duration (months)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = attempted && !durationValid,
                    supportingText = {
                        if (attempted && !durationValid) Text("Enter a duration greater than 0")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(8.dp))

            // Opened date
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Opened on  ${Dates.format(openedDate)}")
            }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Branch (optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = branchCode,
                    onValueChange = { branchCode = it },
                    label = { Text("Code") },
                    singleLine = true,
                    modifier = Modifier.width(92.dp)
                )
            }
            Spacer(Modifier.height(4.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-renew", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Renew with the same terms at maturity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoRenew, onCheckedChange = { autoRenew = it })
            }
            Spacer(Modifier.height(12.dp))

            // ---- Live maturity preview ----
            if (previewReady && previewMaturity != null) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Maturity preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Matures on", Dates.format(previewMaturity))
                        InfoRow(
                            "Est. interest",
                            Lkr.full(FdMath.interestEarned(amount!!, rate!!, duration!!))
                        )
                        InfoRow(
                            "Est. maturity value",
                            Lkr.full(FdMath.maturityValue(amount!!, rate!!, duration!!))
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            FdMath.countdownLabel(FdMath.daysUntil(previewMaturity, today)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    attempted = true
                    if (allValid) {
                        val months = duration!!
                        val fd = FixedDeposit(
                            id = editing?.id ?: "",
                            fdNumber = fdNumber.trim(),
                            bank = bank,
                            amount = amount!!,
                            openedDate = openedDate,
                            durationMonths = months,
                            interestRate = rate!!,
                            maturityDate = FdMath.maturityDate(openedDate, months),
                            branch = branch.ifBlank { null },
                            branchCode = branchCode.ifBlank { null },
                            autoRenew = autoRenew,
                            isActive = true,
                            status = editing?.status ?: FdStatus.ACTIVE,
                            parentFdId = editing?.parentFdId,
                            createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                            isDeleted = false
                        )
                        if (editing == null) vm.addFd(fd) else vm.updateFd(fd)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (editing == null) "Save FD" else "Save changes")
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = openedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        openedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

package com.example.fdmanager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.fdmanager.data.model.PayoutFrequency
import com.example.fdmanager.data.model.RenewOption
import com.example.fdmanager.data.model.SRI_LANKAN_BANKS
import com.example.fdmanager.domain.Dates
import com.example.fdmanager.domain.FdMath
import com.example.fdmanager.domain.FdQueries
import com.example.fdmanager.domain.Lkr
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.components.BankMonogram
import com.example.fdmanager.ui.components.InfoRow
import com.example.fdmanager.ui.components.InstitutionRegistry
import com.example.fdmanager.ui.components.InstitutionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val MONTH_PRESETS = listOf(1, 3, 6, 12, 24, 36, 60)
private val DAY_PRESETS = listOf(30, 60, 90, 100, 180, 300, 364)

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
    var bank by remember {
        mutableStateOf(
            editing?.bank?.takeIf { InstitutionRegistry.isCBSLRegulated(it) }
                ?: editing?.bank
                ?: InstitutionRegistry.find("National Savings Bank (NSB)")?.displayName
                ?: InstitutionRegistry.find("National Savings Bank")?.displayName
                ?: InstitutionRegistry.allDisplayNames.firstOrNull()
                ?: SRI_LANKAN_BANKS[0]
        )
    }
    var amountText by remember {
        mutableStateOf(editing?.let { FdMath.formatRate(it.amount) } ?: "")
    }
    var rateText by remember {
        mutableStateOf(editing?.let { FdMath.formatRate(it.interestRate) } ?: "")
    }
    // Issue #17 — tenor unit + day-based
    var tenorUnit by remember {
        mutableStateOf(editing?.tenorUnit ?: com.example.fdmanager.data.model.TenorUnit.MONTHS)
    }
    var customMonthMode by remember {
        mutableStateOf(editing != null && editing.tenorUnit == com.example.fdmanager.data.model.TenorUnit.MONTHS && editing.durationMonths !in MONTH_PRESETS)
    }
    var selectedMonthDuration by remember {
        mutableStateOf(
            if (editing != null && editing.tenorUnit == com.example.fdmanager.data.model.TenorUnit.MONTHS && editing.durationMonths in MONTH_PRESETS) editing.durationMonths else 12
        )
    }
    var customMonthText by remember {
        mutableStateOf(
            if (editing != null && editing.tenorUnit == com.example.fdmanager.data.model.TenorUnit.MONTHS && editing.durationMonths !in MONTH_PRESETS) editing.durationMonths.toString() else ""
        )
    }
    var customDayMode by remember {
        mutableStateOf(editing != null && editing.tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS && (editing.durationDays ?: 0) !in DAY_PRESETS)
    }
    var selectedDayDuration by remember {
        mutableStateOf(
            if (editing != null && editing.tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS && (editing.durationDays ?: 0) in DAY_PRESETS) editing.durationDays!! else 100
        )
    }
    var customDayText by remember {
        mutableStateOf(
            if (editing != null && editing.tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS && (editing.durationDays ?: 0) !in DAY_PRESETS) (editing.durationDays ?: 0).toString() else ""
        )
    }
    var openedDate by remember { mutableStateOf(editing?.openedDate ?: LocalDate.now()) }
    var branch by remember { mutableStateOf(editing?.branch ?: "") }
    var branchCode by remember { mutableStateOf(editing?.branchCode ?: "") }
    var payoutFrequency by remember { mutableStateOf(editing?.payoutFrequency) }
    var renewOption by remember { mutableStateOf(editing?.renewOption ?: RenewOption.CAPITALIZE) }
    var autoRenew by remember { mutableStateOf(editing?.autoRenew ?: false) }
    var showInstitutionPicker by remember { mutableStateOf(false) }
    var institutionSearchQuery by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }

    // ---- Derived values & validation — CBSL-only guardrail + tenor ----
    val amount = amountText.toDoubleOrNull()
    val rate = rateText.toDoubleOrNull()
    val durationMonths: Int? = if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.MONTHS) {
        if (customMonthMode) customMonthText.toIntOrNull() else selectedMonthDuration
    } else null
    val durationDays: Int? = if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS) {
        if (customDayMode) customDayText.toIntOrNull() else selectedDayDuration
    } else null
    val durationValid = if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.MONTHS) {
        durationMonths != null && durationMonths > 0 && durationMonths <= 120
    } else {
        durationDays != null && durationDays > 0 && durationDays <= 999
    }

    val fdNumberValid = fdNumber.isNotBlank()
    val amountValid = amount != null && amount > 0
    val rateValid = rate != null && rate > 0 && rate <= 30
    val payoutValid = payoutFrequency != null
    val bankValid = InstitutionRegistry.isCBSLRegulated(bank)
    val allValid = fdNumberValid && amountValid && rateValid && durationValid && payoutValid && bankValid

    val previewReady = amountValid && rateValid && durationValid
    val previewMaturity = if (durationValid) {
        if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS) {
            FdMath.maturityDate(openedDate, 0, durationDays, tenorUnit)
        } else {
            FdMath.maturityDate(openedDate, durationMonths!!, null, tenorUnit)
        }
    } else null

    val filteredBanks = remember(institutionSearchQuery) {
        val q = institutionSearchQuery.trim()
        if (q.isBlank()) InstitutionRegistry.allBanks
        else InstitutionRegistry.allBanks.filter { inst ->
            InstitutionRegistry.find(inst.displayName) != null && (
                inst.displayName.contains(q, ignoreCase = true) ||
                inst.code.contains(q, ignoreCase = true) ||
                InstitutionRegistry.find(q)?.displayName == inst.displayName
            )
        }
    }
    val filteredFinance = remember(institutionSearchQuery) {
        val q = institutionSearchQuery.trim()
        if (q.isBlank()) InstitutionRegistry.allFinanceCompanies
        else InstitutionRegistry.allFinanceCompanies.filter { inst ->
            inst.displayName.contains(q, ignoreCase = true) ||
            inst.code.contains(q, ignoreCase = true)
        }
    }

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

            // ---- CBSL-only Institution selector ----
            Text(
                "Institution (CBSL-regulated only)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = bank,
                onValueChange = {},
                readOnly = true,
                label = { Text("Institution") },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "CBSL info")
                        }
                        IconButton(onClick = { showInstitutionPicker = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Pick institution")
                        }
                    }
                },
                isError = attempted && !bankValid,
                supportingText = {
                    if (attempted && !bankValid) {
                        Text(
                            "For your safety, FD Manager only tracks FDs at CBSL regulated institutions. " +
                                "Check spelling or tap Request addition. Learn more: cbsl.gov.lk",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        val type = InstitutionRegistry.find(bank)?.type
                        if (type != null) {
                            Text(
                                "${type.label} • CBSL licensed • Updated ${InstitutionRegistry.LAST_UPDATED}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (editing != null && !InstitutionRegistry.isCBSLRegulated(editing.bank)) {
                Spacer(Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Legacy institution not in CBSL list",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "\"${editing.bank}\" is not in the current CBSL regulated list. Please select a CBSL-regulated institution to continue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
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

            // Tenor — Issue #17 Months | Days
            Text("Tenor", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                com.example.fdmanager.data.model.TenorUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = tenorUnit == unit,
                        onClick = { tenorUnit = unit },
                        label = { Text(unit.label) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.MONTHS) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    MONTH_PRESETS.forEach { months ->
                        FilterChip(
                            selected = !customMonthMode && selectedMonthDuration == months,
                            onClick = { customMonthMode = false; selectedMonthDuration = months },
                            label = { Text(if (months == 1) "1m" else "${months}m") }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    FilterChip(
                        selected = customMonthMode,
                        onClick = { customMonthMode = true },
                        label = { Text("Custom") }
                    )
                }
                if (customMonthMode) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customMonthText,
                        onValueChange = { customMonthText = it.filter { c -> c.isDigit() } },
                        label = { Text("Duration (months 1–120)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = attempted && !durationValid,
                        supportingText = {
                            if (attempted && !durationValid) Text("Enter months 1–120")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    DAY_PRESETS.forEach { days ->
                        FilterChip(
                            selected = !customDayMode && selectedDayDuration == days,
                            onClick = { customDayMode = false; selectedDayDuration = days },
                            label = { Text("${days}d") }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    FilterChip(
                        selected = customDayMode,
                        onClick = { customDayMode = true },
                        label = { Text("Custom") }
                    )
                }
                if (customDayMode) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customDayText,
                        onValueChange = { customDayText = it.filter { c -> c.isDigit() } },
                        label = { Text("Duration (days 1–999)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = attempted && !durationValid,
                        supportingText = {
                            if (attempted && !durationValid) Text("Enter days 1–999 (100/300-day NBFI specials)")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Interest payout frequency (required)
            Text("Interest payout", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                PayoutFrequency.entries.forEach { p ->
                    FilterChip(
                        selected = payoutFrequency == p,
                        onClick = { payoutFrequency = p },
                        label = { Text(p.label) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
            if (attempted && !payoutValid) {
                Text(
                    "Select when interest is paid",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(8.dp))

            // Stored renewal instruction
            Text("On renewal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                RenewOption.entries.forEach { o ->
                    FilterChip(
                        selected = renewOption == o,
                        onClick = { renewOption = o },
                        label = { Text(o.label) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
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
                        "Renews automatically at maturity per your renewal setting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoRenew, onCheckedChange = { autoRenew = it })
            }
            Spacer(Modifier.height(12.dp))

            // Live maturity preview
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
                        InfoRow("Tenor", FdMath.tenorLabel(durationMonths ?: 0, durationDays, tenorUnit))
                        InfoRow(
                            "Est. interest",
                            Lkr.full(
                                if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS) {
                                    FdMath.interestEarned(amount!!, rate!!, 0, durationDays, tenorUnit)
                                } else {
                                    FdMath.interestEarned(amount!!, rate!!, durationMonths!!)
                                }
                            )
                        )
                        InfoRow(
                            "Est. maturity value",
                            Lkr.full(
                                if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS) {
                                    amount!! + FdMath.interestEarned(amount!!, rate!!, 0, durationDays, tenorUnit)
                                } else {
                                    FdMath.maturityValue(amount!!, rate!!, durationMonths!!)
                                }
                            )
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
                        val m = if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.MONTHS) durationMonths!! else 0
                        val d = if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS) durationDays else null
                        val mat = if (tenorUnit == com.example.fdmanager.data.model.TenorUnit.DAYS) {
                            FdMath.maturityDate(openedDate, 0, d, tenorUnit)
                        } else {
                            FdMath.maturityDate(openedDate, m, null, tenorUnit)
                        }
                        val fd = FixedDeposit(
                            id = editing?.id ?: "",
                            fdNumber = fdNumber.trim(),
                            bank = bank.trim(),
                            amount = amount!!,
                            openedDate = openedDate,
                            durationMonths = m,
                            interestRate = rate!!,
                            maturityDate = mat,
                            branch = branch.ifBlank { null },
                            branchCode = branchCode.ifBlank { null },
                            payoutFrequency = payoutFrequency!!,
                            renewOption = renewOption,
                            autoRenew = autoRenew,
                            isActive = true,
                            status = editing?.status ?: FdStatus.ACTIVE,
                            parentFdId = editing?.parentFdId,
                            createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                            isDeleted = false,
                            durationDays = d,
                            tenorUnit = tenorUnit
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

    if (showInstitutionPicker) {
        AlertDialog(
            onDismissRequest = { showInstitutionPicker = false },
            title = { Text("Select institution (CBSL-only)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = institutionSearchQuery,
                        onValueChange = { institutionSearchQuery = it },
                        label = { Text("Search banks & finance companies") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.height(360.dp)) {
                        if (filteredBanks.isNotEmpty()) {
                            item {
                                Text(
                                    "Banks (${filteredBanks.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(filteredBanks) { inst ->
                                InstitutionRow(
                                    inst = inst,
                                    onClick = {
                                        bank = inst.displayName
                                        showInstitutionPicker = false
                                        institutionSearchQuery = ""
                                    }
                                )
                            }
                            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                        }
                        if (filteredFinance.isNotEmpty()) {
                            item {
                                Text(
                                    "Finance Companies (${filteredFinance.size}) — CBSL licensed",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(filteredFinance) { inst ->
                                InstitutionRow(
                                    inst = inst,
                                    onClick = {
                                        bank = inst.displayName
                                        showInstitutionPicker = false
                                        institutionSearchQuery = ""
                                    }
                                )
                            }
                        }
                        if (filteredBanks.isEmpty() && filteredFinance.isEmpty()) {
                            item {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Not found in CBSL regulated list",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Check spelling or tap Request addition if it's CBSL-licensed. For your safety, FD Manager only tracks FDs at CBSL regulated institutions.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            showInstitutionPicker = false
                                            showRequestDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Request addition")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstitutionPicker = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showInstitutionPicker = false
                    showRequestDialog = true
                }) { Text("Request addition") }
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("CBSL-regulated institutions only") },
            text = {
                Column {
                    Text(
                        "For your safety, FD Manager only tracks Fixed Deposits at CBSL-regulated institutions:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("• Licensed Commercial Banks (24)\n• Licensed Specialised Banks (6)\n• Licensed Finance Companies (31 allowed, Nation Lanka Finance excluded per CBSL prohibition)", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Source: CBSL Notice as at ${InstitutionRegistry.LAST_UPDATED} — ${InstitutionRegistry.SOURCE_URL}\n" +
                            "Total allowed: ${InstitutionRegistry.allInstitutions.size} institutions.\n" +
                            "Last updated: ${InstitutionRegistry.LAST_UPDATED}\n\n" +
                            "If your institution is CBSL-licensed but missing, use Request addition. We verify against cbsl.gov.lk and add in next release. Non-regulated entities cannot be stored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Got it") }
            }
        )
    }

    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = { Text("Request addition") },
            text = {
                Column {
                    Text(
                        "If your institution is CBSL-licensed but not in the list, please contact support with the institution name. We verify against cbsl.gov.lk and add it in the next app release.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    if (institutionSearchQuery.isNotBlank()) {
                        Text(
                            "You searched for: \"${institutionSearchQuery}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        "For your safety, non-CBSL regulated entities cannot be added.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRequestDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun InstitutionRow(
    inst: com.example.fdmanager.ui.components.InstitutionIdentity,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BankMonogram(bank = inst.displayName, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(inst.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${inst.code} • ${inst.type.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onClick) { Text("Select") }
    }
}

package com.example.fdmanager.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vssbox.fdmanager.R
import com.example.fdmanager.data.ThemeMode
import com.example.fdmanager.domain.DataWipe
import com.example.fdmanager.domain.ExportFormat
import com.example.fdmanager.domain.FdExport
import com.example.fdmanager.domain.WipeSelection
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.components.DeleteAllDataDialog
import com.example.fdmanager.data.DevicePrefs
import com.example.fdmanager.domain.SessionGate
import com.example.fdmanager.ui.components.ExportFormatDialog
import com.example.fdmanager.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: FdViewModel,
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    // #9 hotfix — the Account section reads the REAL auth state. The fake path maps its
    // mock flag into authState, so mock behavior (and every Tier2 assertion) is unchanged.
    val auth by vm.authState.collectAsStateWithLifecycle()
    // #9 round 3 — the session gate must also cover data egress/destruction INSIDE Settings.
    // Settings itself stays reachable while locked (it hosts the Sign in row), so the
    // Export / Delete / Reset rows redirect to sign-in instead of acting. Found in TI-009 §4
    // acceptance: a signed-out device could still export every deposit.
    val hasAccount by DevicePrefs.hasAccountOnDevice.collectAsStateWithLifecycle()
    val sessionLocked = SessionGate.isLocked(hasAccount, auth.isLoggedIn)
    // #10 — while the Firestore backend is live, demo data stays LOCAL-ONLY (owner decision):
    // the two demo rows explain instead of silently mutating an invisible local store.
    val cloudActive = auth.isLoggedIn && vm.isCloudActive()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun placeholder(msg: String) = scope.launch { snackbarHost.showSnackbar(msg) }

    // #5 trust UX — export + delete-all state
    val context = LocalContext.current
    val fds by vm.fds.collectAsStateWithLifecycle()
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var showWipeDialog by remember { mutableStateOf(false) }

    // #5 trust UX — SAF write. Kept in one place so the mime type chosen at launch can never
    // disagree with the format actually serialised.
    fun writeExport(uri: android.net.Uri?) {
        if (uri == null) {
            placeholder("Export cancelled.")
            return
        }
        val format = pendingFormat
        val content = if (format == ExportFormat.CSV) FdExport.toCsv(fds) else FdExport.toJson(fds)
        val written = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: error("no output stream")
        }
        if (written.isSuccess) {
            placeholder("Exported ${fds.size} deposits as ${format.name}.")
        } else {
            placeholder("Export failed: ${written.exceptionOrNull()?.message ?: "unknown error"}")
        }
    }

    // "*/*" because the contract's mime type is fixed at construction while the user picks the
    // format in the dialog; the suggested file name carries the extension.
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri -> writeExport(uri) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ---- Account (#2 mock era; #9 real backend; hotfix: authState-driven) ----
                SectionHeader(
                    "Account",
                    subtitle = when {
                        auth.isLoggedIn -> auth.email ?: "Signed in"
                        vm.isAuthBackendReal() -> "Not signed in"
                        else -> "Mock session - no backend"
                    }
                )
                if (!auth.isLoggedIn) {
                    SettingRow(
                        title = "Sign in",
                        subtitle = if (vm.isAuthBackendReal()) "Firebase Authentication"
                                   else "Mock auth - fake session, 0 quota. Phase 2 -> Firebase Auth",
                        onClick = onSignIn,
                        testTag = "accountSignIn",
                        trailing = {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                } else {
                    SettingRow(
                        title = auth.email ?: "Signed in",
                        subtitle = if (vm.isAuthBackendReal()) "Signed in - tap to sign out"
                                   else "Mock session active - tap to sign out",
                        onClick = {
                            vm.mockSignOut()
                            scope.launch {
                                snackbarHost.showSnackbar(
                                    if (vm.isAuthBackendReal()) "Signed out" else "Signed out (mock)"
                                )
                            }
                            onSignOut()
                        },
                        testTag = "accountSignOut",
                        titleTestTag = "accountEmail",
                        trailing = {
                            Icon(
                                Icons.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---- Appearance ----
                SectionHeader("Appearance")
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.setThemeMode(mode) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---- Notifications ----
                SectionHeader("Notifications", subtitle = "Maturity alerts")
                SettingSwitchRow(
                    title = "Maturity reminders",
                    subtitle = "Highlight FDs close to maturity",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { vm.setNotificationsEnabled(it) }
                )
                if (settings.notificationsEnabled) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Notify ${settings.notifyLeadDays} days before maturity",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Slider(
                            value = settings.notifyLeadDays.toFloat(),
                            onValueChange = { vm.setNotifyLeadDays(it.toInt()) },
                            valueRange = 1f..30f,
                            steps = 28,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Used by the bell and strip on Home. Push alerts (10:00 AM daily) arrive in a later release.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---- Security ----
                SectionHeader("Security")
                SettingSwitchRow(
                    title = "Biometric lock",
                    subtitle = if (settings.biometricEnabled) {
                        "Lock screen on every launch (mock biometrics — real ones arrive with the backend)"
                    } else {
                        "Lock the app when it starts — mock unlock for now"
                    },
                    checked = settings.biometricEnabled,
                    onCheckedChange = { enabled ->
                        vm.setBiometricEnabled(enabled)
                        if (enabled) {
                            placeholder("Lock enabled — the lock screen now shows on every launch.")
                        }
                    }
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---- Data ----
                SectionHeader("Data")
                SettingRow(
                    title = "Currency",
                    subtitle = "Sri Lankan Rupee",
                    trailing = { Text("LKR (Rs)", fontWeight = FontWeight.SemiBold) }
                )
                SettingRow(
                    testTag = "exportRow",
                    title = "Export deposits",
                    subtitle = if (sessionLocked) "Signed out - sign in to export"
                        else "CSV or JSON — saves wherever you choose (${fds.size} records)",
                    onClick = { if (sessionLocked) onSignIn() else showExportDialog = true },
                    trailing = {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
                SettingRow(
                    testTag = "deleteDataRow",
                    title = "Delete data",
                    subtitle = if (sessionLocked) "Signed out - sign in to manage data"
                        else "Choose what to remove. Requires typing ${DataWipe.CONFIRM_PHRASE}",
                    onClick = { if (sessionLocked) onSignIn() else showWipeDialog = true },
                    trailing = {
                        Icon(
                            Icons.Filled.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
                SettingRow(
                    title = "Reset demo data",
                    // #9 round 3 — same defect class as Export/Delete: while locked this row
                    // would OVERWRITE the signed-out owner's deposits with sample data.
                    // #10 — while the cloud session is live, demo data is local-only.
                    subtitle = if (sessionLocked) "Signed out - sign in to reset data"
                        else if (cloudActive) "Demo data is local-only - sign out to use"
                        else "Restore the original sample FDs",
                    onClick = {
                        if (sessionLocked) onSignIn() else if (cloudActive) {
                            placeholder("Demo data is local-only - sign out to use it.")
                        } else {
                            vm.resetDemoData()
                            placeholder("Demo data restored.")
                        }
                    },
                    trailing = {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
                SettingRow(
                    title = "Show onboarding",
                    subtitle = "Replay vssbox welcome screen (hasSeenOnboarding=false)",
                    onClick = {
                        vm.setHasSeenOnboarding(false)
                        placeholder("Onboarding will show on next launch - restart app or set true to skip.")
                    }
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---- About ---- vssbox branding #27
                SectionHeader("About")
                Column(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("aboutBranding"),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.vssbox_cube),
                            contentDescription = "vssbox logo",
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("vssboxLogoAbout")
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "vssbox",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "FD Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "by vssbox",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Version 1.0 - front-end mock - © 2026 vssbox",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Secure box for your deposits - Track CBSL-regulated FDs (24 LCB + 6 LSB + 31 LFC = 61 allowed; 32 LFC listed, 1 prohibited). Runs entirely on local sample data. No account, no backend, no real money - FDs shown are fictional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // #5 trust UX — privacy one-liner, cross-referenced by onboarding
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Privacy: your deposits never leave this device. Nothing is uploaded, shared or sold — export and delete are entirely yours to control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("privacyLine")
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Built by vssbox",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "vssbox.com (coming soon)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Demo mode toggle #1 onboarding
                SectionHeader("Demo")
                SettingSwitchRow(
                    title = "Demo mode",
                    // #10 — cloud session live → demo is local-only; the switch explains instead of toggling.
                    // #10 follow-up (F6): render DISABLED under a cloud session instead of a live-looking
                    // switch that swallows taps (it could show a stale ON from the onboarding demo path).
                    subtitle = if (cloudActive) "Demo data is local-only - sign out to use"
                        else if (settings.isDemoMode) "Sample FDs visible (opt-in demo)" else "Hide sample data",
                    checked = settings.isDemoMode,
                    enabled = !cloudActive,
                    onCheckedChange = {
                        if (cloudActive) placeholder("Demo data is local-only - sign out to use it.")
                        else vm.setDemoMode(it)
                    }
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---- Sync state demo #3 — mock states for design review, 0 quota ----
                SectionHeader(
                    "Sync (mock) #3",
                    subtitle = "Demo triggers for Saved/Syncing/Offline/Error chip",
                    modifier = Modifier.testTag("syncMockSection")
                )
                SettingRow(
                    title = "Sync state: ${settings.syncStateName}",
                    subtitle = "Tap to cycle Saved → Syncing → Offline → Error (mock, 0 quota)",
                    onClick = {
                        val next = when (settings.syncStateName) {
                            "SAVED" -> com.example.fdmanager.domain.SyncState.SYNCING
                            "SYNCING" -> com.example.fdmanager.domain.SyncState.OFFLINE
                            "OFFLINE" -> com.example.fdmanager.domain.SyncState.ERROR
                            else -> com.example.fdmanager.domain.SyncState.SAVED
                        }
                        vm.setSyncState(next)
                        scope.launch { snackbarHost.showSnackbar("Sync state → ${next.name} (mock)") }
                    },
                    testTag = "syncStateRow"
                )
                SettingRow(
                    title = "Simulate sync error",
                    subtitle = "Shows Snackbar with Retry — retry re-runs last mock operation",
                    onClick = {
                        vm.simulateSyncError()
                        scope.launch { snackbarHost.showSnackbar("Simulated error — check Home for Snackbar Retry (mock)") }
                    },
                    testTag = "simulateSyncError"
                )
                SettingRow(
                    title = "Simulate offline",
                    subtitle = "Chip shows Offline — local only",
                    onClick = {
                        vm.simulateOffline()
                        scope.launch { snackbarHost.showSnackbar("Offline mode (mock)") }
                    },
                    testTag = "simulateOffline"
                )
                SettingRow(
                    title = "Clear sync error",
                    subtitle = "Reset to Saved",
                    onClick = {
                        vm.clearSyncError()
                        scope.launch { snackbarHost.showSnackbar("Sync cleared → Saved (mock)") }
                    },
                    testTag = "clearSyncError"
                )
                com.example.fdmanager.ui.components.SyncStateChipWithDescription(
                    state = com.example.fdmanager.domain.SyncState.fromName(settings.syncStateName),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---- Loading & Error debug #4 — shared pattern, toggled by debug switch for review ----
                SectionHeader(
                    "Loading & Error (debug #4)",
                    subtitle = "Shared skeleton/error — Home, list, detail, calendar (mock, 0 quota)",
                    modifier = Modifier.testTag("loadingErrorSection")
                )
                SettingSwitchRow(
                    title = "Show loading state",
                    subtitle = "Skeleton + spinner — Home, list, calendar show loading (mock #4)",
                    checked = settings.isLoadingDebug,
                    onCheckedChange = { vm.setLoadingDebug(it) },
                    testTag = "showLoadingState"
                )
                SettingSwitchRow(
                    title = "Show error state",
                    subtitle = "Full-screen error with Retry — ${settings.debugErrorMessage}",
                    checked = settings.isErrorDebug,
                    onCheckedChange = { vm.setErrorDebug(it) },
                    testTag = "showErrorState"
                )
                SettingRow(
                    title = "Clear loading/error debug",
                    subtitle = "Reset to idle — show normal data",
                    onClick = {
                        vm.clearLoadingErrorDebug()
                        scope.launch { snackbarHost.showSnackbar("Loading/error debug cleared → Idle (mock #4)") }
                    },
                    testTag = "clearLoadingErrorDebug"
                )
                // Preview of shared components
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).testTag("loadingErrorPreview").testTag("loadstate-0400r9")) {
                    Text(
                        "Preview — shared implementation:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    com.example.fdmanager.ui.components.LoadingSkeletonCard()
                    Spacer(Modifier.height(8.dp))
                    com.example.fdmanager.ui.components.FullScreenError(
                        title = "Preview error",
                        message = settings.debugErrorMessage,
                        onRetry = { vm.clearLoadingErrorDebug() },
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }

                Spacer(Modifier.height(48.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // #5 trust UX — export format chooser
        if (showExportDialog) {
            ExportFormatDialog(
                onDismiss = { showExportDialog = false },
                onPick = { format ->
                    pendingFormat = format
                    showExportDialog = false
                    exportLauncher.launch(FdExport.suggestedFileName(format))
                }
            )
        }

        // #5 trust UX — delete-all with per-category choice + typed confirmation
        if (showWipeDialog) {
            DeleteAllDataDialog(
                onDismiss = { showWipeDialog = false },
                onConfirm = { selection: WipeSelection ->
                    if (selection.clearsDeposits) vm.clearAllFds()
                    vm.applyDataWipe(selection)
                    showWipeDialog = false
                    placeholder("Deleted: ${DataWipe.summary(selection)}")
                }
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null,
    enabled: Boolean = true
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = if (testTag != null) Modifier.testTag("${testTag}Switch") else Modifier
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    testTag: String? = null,
    titleTestTag: String? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = if (titleTestTag != null) Modifier.testTag(titleTestTag) else Modifier
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
    }
}

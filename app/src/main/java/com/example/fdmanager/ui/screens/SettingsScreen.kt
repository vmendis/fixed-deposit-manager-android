package com.example.fdmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fdmanager.data.ThemeMode
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: FdViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun placeholder(msg: String) = scope.launch { snackbarHost.showSnackbar(msg) }

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
                            "Used by the bell and strip on Home. Push alerts (10:00 AM daily) switch on once the Firebase backend is connected.",
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
                    subtitle = "Requires Firebase Authentication — wired up later",
                    checked = false,
                    onCheckedChange = {
                        placeholder("Biometrics need the auth backend — disabled in this mock.")
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
                    title = "Export to CSV",
                    subtitle = "Planned enhancement",
                    onClick = { placeholder("CSV export is a planned backend-era feature.") },
                    trailing = {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
                SettingRow(
                    title = "Reset demo data",
                    subtitle = "Restore the original sample FDs",
                    onClick = {
                        vm.resetDemoData()
                        placeholder("Demo data restored.")
                    },
                    trailing = {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // ---- About ----
                SectionHeader("About")
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("FD Manager", fontWeight = FontWeight.Bold)
                    Text(
                        "Version 1.0 · front-end mock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Runs entirely on local sample data. No account, no backend, no real money — FDs shown are fictional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(48.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
        trailing?.invoke()
    }
}

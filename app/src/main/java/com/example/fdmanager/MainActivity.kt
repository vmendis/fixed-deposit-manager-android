package com.example.fdmanager

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fdmanager.data.SettingsStore
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.screens.AddEditFdScreen
import com.example.fdmanager.ui.screens.CalendarScreen
import com.example.fdmanager.ui.screens.FdDetailScreen
import com.example.fdmanager.ui.screens.FdListScreen
import com.example.fdmanager.ui.screens.HomeScreen
import com.example.fdmanager.ui.screens.RecycleBinScreen
import com.example.fdmanager.ui.screens.SettingsScreen
import com.example.fdmanager.ui.theme.FdManagerTheme

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val BIN = "bin"
    const val ADD = "add"
    const val BANK = "bank/{bank}"
    const val FD_DETAIL = "fd/{fdId}"
    const val EDIT = "edit/{fdId}"

    fun bank(bank: String) = "bank/${Uri.encode(bank)}"
    fun fdDetail(id: String) = "fd/$id"
    fun edit(id: String) = "edit/$id"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by SettingsStore.state.collectAsStateWithLifecycle()
            FdManagerTheme(mode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FdManagerApp()
                }
            }
        }
    }
}

private data class TabSpec(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    TabSpec(Routes.HOME, "Home", Icons.Filled.Home),
    TabSpec(Routes.CALENDAR, "Calendar", Icons.Filled.DateRange),
    TabSpec(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun FdManagerApp() {
    val nav = rememberNavController()
    val vm: FdViewModel = viewModel()

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute in TABS.map { it.route }

    Scaffold(
        bottomBar = {
            if (showChrome) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.HOME) {
                FloatingActionButton(onClick = { nav.navigate(Routes.ADD) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add FD")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onOpenBank = { nav.navigate(Routes.bank(it)) },
                    onOpenFd = { nav.navigate(Routes.fdDetail(it)) },
                    onOpenBin = { nav.navigate(Routes.BIN) }
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    vm = vm,
                    onOpenFd = { nav.navigate(Routes.fdDetail(it)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm = vm)
            }
            composable(Routes.BIN) {
                RecycleBinScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.BANK) { entry ->
                val bank = entry.arguments?.getString("bank").orEmpty()
                FdListScreen(
                    vm = vm,
                    bank = bank,
                    onBack = { nav.popBackStack() },
                    onOpenFd = { nav.navigate(Routes.fdDetail(it)) },
                    onEditFd = { nav.navigate(Routes.edit(it)) },
                    onAdd = { nav.navigate(Routes.ADD) }
                )
            }
            composable(Routes.FD_DETAIL) { entry ->
                val id = entry.arguments?.getString("fdId").orEmpty()
                FdDetailScreen(
                    vm = vm,
                    fdId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.edit(it)) },
                    onOpenFd = { nav.navigate(Routes.fdDetail(it)) }
                )
            }
            composable(Routes.ADD) {
                AddEditFdScreen(vm = vm, fdId = null, onBack = { nav.popBackStack() })
            }
            composable(Routes.EDIT) { entry ->
                val id = entry.arguments?.getString("fdId").orEmpty()
                AddEditFdScreen(vm = vm, fdId = id, onBack = { nav.popBackStack() })
            }
        }
    }
}

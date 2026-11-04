package com.example.fdmanager

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fdmanager.data.AuthRepositoryProvider
import com.example.fdmanager.data.DevicePrefs
import com.example.fdmanager.data.FirebaseProbe
import com.example.fdmanager.data.SettingsStore
import com.example.fdmanager.domain.SessionGate
import com.example.fdmanager.ui.FdViewModel
import com.example.fdmanager.ui.screens.AddEditFdScreen
import com.example.fdmanager.ui.screens.CalendarScreen
import com.example.fdmanager.ui.screens.FdDetailScreen
import com.example.fdmanager.ui.screens.FdListScreen
import com.example.fdmanager.ui.screens.ForgotPasswordScreen
import com.example.fdmanager.ui.screens.HomeScreen
import com.example.fdmanager.ui.screens.LockScreen
import com.example.fdmanager.ui.screens.OnboardingScreen
import com.example.fdmanager.ui.screens.RecycleBinScreen
import com.example.fdmanager.ui.screens.SettingsScreen
import com.example.fdmanager.ui.screens.SignInScreen
import com.example.fdmanager.ui.screens.SignUpScreen
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
    const val ONBOARDING = "onboarding" // #27 vssbox branding on new-user screen
    const val LOCK = "lock" // #5 trust UX — app lock, shown on cold start when enabled
    const val SIGN_IN = "signin" // #2 mock auth
    const val SIGN_UP = "signup"
    const val FORGOT = "forgot"

    fun bank(bank: String) = "bank/${Uri.encode(bank)}"
    fun fdDetail(id: String) = "fd/$id"
    fun edit(id: String) = "edit/$id"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // #9 — must run before any ViewModel exists: persisted has-account flag + real/fake auth selection
        DevicePrefs.init(applicationContext)
        AuthRepositoryProvider.init(applicationContext)
        // #10 — AFTER auth: picks in-memory vs Firestore per session, runs one-shot migration
        com.example.fdmanager.data.FdRepositoryProvider.init(applicationContext)
        FirebaseProbe.run(applicationContext) // debug-only Firestore connectivity probe (roadmap #8); no-op in release and when unconfigured
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

/**
 * #9 — session gate for ACCOUNT-HOLDERS only (SESSION_GATE = session-0900r9).
 * A device that never used a real account never sees this screen.
 */
@Composable
private fun SessionGateScreen(onSignIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("session-0900r9"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Signed out", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your deposits stay safely on this device. Sign in to see them again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSignIn, modifier = Modifier.testTag("sessionGateSignIn")) {
            Text("Sign in to continue")
        }
    }
}

@Composable
fun FdManagerApp() {
    val nav = rememberNavController()
    val vm: FdViewModel = viewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()

    // #9 — gate data screens when an account exists on this device but is signed out.
    val authState by vm.authState.collectAsStateWithLifecycle()
    val hasAccount by DevicePrefs.hasAccountOnDevice.collectAsStateWithLifecycle()
    val sessionLocked = SessionGate.isLocked(hasAccount, authState.isLoggedIn)

    // #1 onboarding funnel + #27 vssbox branding — fresh install → welcome → add first FD → Home
    // #5 trust UX — app lock wins over onboarding: an enabled lock must gate the app on cold start
    // ONBOARDING_GATE = onboarding-0100r9, HOME_SCROLL_GATE = home-scroll-0100r9, DEMO_GATE = demo-0100r9, AUTH_GATE = auth-0100r9
    // LOCK_GATE = lock-0500r9
    val startDest = when {
        settings.biometricEnabled -> Routes.LOCK
        !settings.hasSeenOnboarding -> Routes.ONBOARDING
        else -> Routes.HOME
    }

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
            if (currentRoute == Routes.HOME && !sessionLocked) {
                FloatingActionButton(onClick = { nav.navigate(Routes.ADD) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add FD")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startDest,
            modifier = Modifier.padding(padding)
        ) {
            // #1 onboarding funnel — welcome screen with trust line, vssbox branding, CTA to Add first FD
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onGetStarted = {
                        vm.setHasSeenOnboarding(true)
                        nav.navigate(Routes.ADD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                    onExploreDemo = {
                        vm.setHasSeenOnboarding(true)
                        vm.toggleDemoMode(true)
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            // #5 trust UX — app lock. Cold-start gate when Settings → Security → Biometric lock is on.
            composable(Routes.LOCK) {
                LockScreen(
                    onUnlock = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.LOCK) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                if (sessionLocked) SessionGateScreen(onSignIn = { nav.navigate(Routes.SIGN_IN) })
                else HomeScreen(
                    vm = vm,
                    onOpenBank = { nav.navigate(Routes.bank(it)) },
                    onOpenFd = { nav.navigate(Routes.fdDetail(it)) },
                    onOpenBin = { nav.navigate(Routes.BIN) },
                    onAddFd = { nav.navigate(Routes.ADD) }
                )
            }
            composable(Routes.CALENDAR) {
                if (sessionLocked) SessionGateScreen(onSignIn = { nav.navigate(Routes.SIGN_IN) })
                else CalendarScreen(
                    vm = vm,
                    onOpenFd = { nav.navigate(Routes.fdDetail(it)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    onSignIn = { nav.navigate(Routes.SIGN_IN) },
                    onSignOut = { /* stay in settings */ }
                )
            }
            // #2 mock auth flow — UI only, fake session, 0 quota, swappable to FirebaseAuthRepository in Phase 2
            composable(Routes.SIGN_IN) {
                SignInScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onSignUp = { nav.navigate(Routes.SIGN_UP) },
                    onForgot = { nav.navigate(Routes.FORGOT) },
                    onSignedIn = {
                        // Pop SIGN_IN -> reveal SETTINGS. Use simple popBackStack for reliability with bottom-nav saveState.
                        if (!nav.popBackStack()) {
                            nav.navigate(Routes.SETTINGS) {
                                popUpTo(Routes.SETTINGS) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(Routes.SIGN_UP) {
                SignUpScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onSignIn = {
                        if (!nav.popBackStack()) {
                            nav.navigate(Routes.SIGN_IN) {
                                popUpTo(Routes.SIGN_IN) { inclusive = true }
                            }
                        }
                    },
                    onSignedUp = {
                        if (!nav.popBackStack(Routes.SETTINGS, inclusive = false)) {
                            nav.navigate(Routes.SETTINGS) {
                                popUpTo(Routes.SETTINGS) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        // Extra pop if we came via SIGN_IN -> SIGN_UP, ensure we land on SETTINGS
                        nav.popBackStack(Routes.SETTINGS, inclusive = false)
                    }
                )
            }
            composable(Routes.FORGOT) {
                ForgotPasswordScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onSent = {
                        if (!nav.popBackStack(Routes.SIGN_IN, inclusive = false)) {
                            nav.navigate(Routes.SIGN_IN) {
                                popUpTo(Routes.SIGN_IN) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(Routes.BIN) {
                if (sessionLocked) SessionGateScreen(onSignIn = { nav.navigate(Routes.SIGN_IN) })
                else RecycleBinScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.BANK) { entry ->
                val bank = entry.arguments?.getString("bank").orEmpty()
                if (sessionLocked) SessionGateScreen(onSignIn = { nav.navigate(Routes.SIGN_IN) })
                else FdListScreen(
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
                if (sessionLocked) SessionGateScreen(onSignIn = { nav.navigate(Routes.SIGN_IN) })
                else FdDetailScreen(
                    vm = vm,
                    fdId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.edit(it)) },
                    onOpenFd = { nav.navigate(Routes.fdDetail(it)) }
                )
            }
            composable(Routes.ADD) {
                if (sessionLocked) SessionGateScreen(onSignIn = { nav.navigate(Routes.SIGN_IN) })
                else AddEditFdScreen(vm = vm, fdId = null, onBack = { nav.popBackStack() })
            }
            composable(Routes.EDIT) { entry ->
                val id = entry.arguments?.getString("fdId").orEmpty()
                if (sessionLocked) SessionGateScreen(onSignIn = { nav.navigate(Routes.SIGN_IN) })
                else AddEditFdScreen(vm = vm, fdId = id, onBack = { nav.popBackStack() })
            }
        }
    }
}

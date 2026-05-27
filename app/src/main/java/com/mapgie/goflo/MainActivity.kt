package com.mapgie.goflo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mapgie.goflo.data.preferences.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.mapgie.goflo.ui.AppState
import com.mapgie.goflo.ui.MainViewModel
import com.mapgie.goflo.ui.navigation.Screen
import com.mapgie.goflo.ui.screens.auth.LockScreen
import com.mapgie.goflo.ui.screens.auth.LockViewModel
import com.mapgie.goflo.ui.screens.auth.PinSetupScreen
import com.mapgie.goflo.ui.screens.auth.PinSetupViewModel
import com.mapgie.goflo.ui.screens.disclaimer.DisclaimerScreen
import com.mapgie.goflo.ui.screens.categories.ManageCategoriesScreen
import com.mapgie.goflo.ui.screens.categories.ManageCategoriesViewModel
import com.mapgie.goflo.ui.screens.categories.ManageCategoryValuesScreen
import com.mapgie.goflo.ui.screens.categories.ManageCategoryValuesViewModel
import com.mapgie.goflo.ui.screens.history.HistoryScreen
import com.mapgie.goflo.ui.screens.history.HistoryViewModel
import com.mapgie.goflo.ui.screens.home.HomeScreen
import com.mapgie.goflo.ui.screens.home.HomeViewModel
import com.mapgie.goflo.ui.screens.log.LogCategoryScreen
import com.mapgie.goflo.ui.screens.log.LogCategoryViewModel
import com.mapgie.goflo.ui.screens.settings.SettingsScreen
import com.mapgie.goflo.ui.screens.settings.SettingsViewModel
import android.annotation.SuppressLint
import com.mapgie.goflo.ui.theme.AppTheme
import com.mapgie.goflo.ui.theme.GoFloTheme
import com.mapgie.goflo.AppIconChoice
import com.mapgie.goflo.AppIconManager

class MainActivity : ComponentActivity() {

    // False positive: lint check targets FragmentActivity users with old fragment versions.
    // MainActivity extends ComponentActivity directly; no fragment dependency is present.
    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val app = application as GoFloApplication
        val initialPrefs = runBlocking { app.preferencesStore.preferences.first() }

        // Apply the user's saved icon choice once on startup (handles reinstalls /
        // device restores where the manifest resets all aliases to their defaults).
        // This is NOT re-run on theme change, which was the source of the crash.
        val savedIconChoice = runCatching {
            AppIconChoice.valueOf(initialPrefs.iconChoice)
        }.getOrDefault(AppIconChoice.LEAF)
        AppIconManager.applyIcon(this, savedIconChoice)

        setContent {
            val mainVm: MainViewModel = viewModel(
                factory = MainViewModel.Factory(app.securityPreferences, app)
            )
            val appState by mainVm.appState.collectAsState()

            val appPrefs by app.preferencesStore.preferences.collectAsState(initial = initialPrefs)
            val currentTheme = runCatching { AppTheme.valueOf(appPrefs.theme) }.getOrDefault(AppTheme.CORAL)

            GoFloTheme(appTheme = currentTheme, wcag = appPrefs.wcagMode) {
                when (appState) {
                    AppState.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    AppState.DISCLAIMER -> DisclaimerScreen(
                        onAcknowledge = { mainVm.acknowledgeDisclaimer() }
                    )

                    AppState.LOCKED -> {
                        val lockVm: LockViewModel = viewModel(
                            factory = LockViewModel.Factory(app.securityPreferences)
                        )
                        LockScreen(viewModel = lockVm, onUnlocked = { mainVm.onUnlocked() })
                    }

                    AppState.READY -> MainNavHost(app = app, currentTheme = currentTheme)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Re-evaluate lock state when the app returns from background.
        val app = application as GoFloApplication
        // The ViewModel is already created; find it via the store. We retrieve it via
        // the activity's ViewModelStore rather than recreating.
        val mainVm = androidx.lifecycle.ViewModelProvider(
            this,
            MainViewModel.Factory(app.securityPreferences, app)
        )[MainViewModel::class.java]
        mainVm.onActivityStart()
    }
}

@androidx.compose.runtime.Composable
private fun MainNavHost(app: GoFloApplication, currentTheme: AppTheme) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomNavRoutes = listOf(Screen.Home.route, Screen.History.route, Screen.Settings.route)
    val showBottomBar = bottomNavRoutes.any { currentRoute?.startsWith(it) == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = { navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        } },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.History.route,
                        onClick = { navController.navigate(Screen.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        } },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "History") },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        } },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.repository, app.trackingRepository, app.preferencesStore))
                HomeScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
            }

            composable(Screen.History.route) {
                val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(app.repository))
                HistoryScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
            }

            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        store = app.preferencesStore,
                        securityPreferences = app.securityPreferences,
                        repository = app.repository,
                        trackingRepository = app.trackingRepository,
                        context = app.applicationContext
                    )
                )
                SettingsScreen(
                    viewModel = vm,
                    onNavigateToPinSetup = { changing ->
                        navController.navigate(if (changing) Screen.PinSetup.changePin else Screen.PinSetup.newPin)
                    },
                    onNavigateToLicenses = { navController.navigate(Screen.Licenses.route) },
                    onNavigateToPrivacy  = { navController.navigate(Screen.Privacy.route) },
                    onNavigateToManageCategories = { navController.navigate(Screen.ManageCategories.route) }
                )
            }

            composable(
                route = Screen.LogPeriod.route,
                arguments = listOf(
                    navArgument("periodId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("startDate") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStack ->
                val periodId = backStack.arguments?.getLong("periodId") ?: -1L
                val startDateStr = backStack.arguments?.getString("startDate")
                val prefilledDate = startDateStr?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                val vm: com.mapgie.goflo.ui.screens.log.LogPeriodViewModel = viewModel(
                    key = "log_${periodId}_${startDateStr}",
                    factory = com.mapgie.goflo.ui.screens.log.LogPeriodViewModel.Factory(app.repository, periodId, prefilledDate)
                )
                com.mapgie.goflo.ui.screens.log.LogPeriodScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.PinSetup.route,
                arguments = listOf(navArgument("changing") {
                    type = NavType.BoolType; defaultValue = false
                })
            ) { backStack ->
                val isChanging = backStack.arguments?.getBoolean("changing") ?: false
                val vm: PinSetupViewModel = viewModel(
                    key = "pin_setup_$isChanging",
                    factory = PinSetupViewModel.Factory(app.securityPreferences, isChanging)
                )
                PinSetupScreen(
                    viewModel = vm,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Licenses.route) {
                com.mapgie.goflo.ui.screens.licenses.LicensesScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Privacy.route) {
                com.mapgie.goflo.ui.screens.disclaimer.DisclaimerScreen(
                    onAcknowledge = { navController.popBackStack() }
                )
            }

            // ── Tracking categories management ────────────────────────────────

            composable(Screen.ManageCategories.route) {
                val vm: ManageCategoriesViewModel = viewModel(
                    factory = ManageCategoriesViewModel.Factory(app.trackingRepository)
                )
                ManageCategoriesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategory = { categoryId ->
                        navController.navigate(Screen.ManageCategoryValues.forCategory(categoryId))
                    }
                )
            }

            composable(
                route = Screen.ManageCategoryValues.route,
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
            ) { backStack ->
                val categoryId = backStack.arguments?.getLong("categoryId") ?: return@composable
                val vm: ManageCategoryValuesViewModel = viewModel(
                    key = "manage_cat_$categoryId",
                    factory = ManageCategoryValuesViewModel.Factory(categoryId, app.trackingRepository)
                )
                ManageCategoryValuesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Per-day category logging ──────────────────────────────────────

            composable(
                route = Screen.LogCategory.route,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.LongType },
                    navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("logId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStack ->
                val categoryId = backStack.arguments?.getLong("categoryId") ?: return@composable
                val dateStr = backStack.arguments?.getString("date")
                val logId = backStack.arguments?.getLong("logId")?.takeIf { it != -1L }
                val prefilledDate = dateStr?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                val vm: LogCategoryViewModel = viewModel(
                    key = "log_cat_${categoryId}_${dateStr}_${logId}",
                    factory = LogCategoryViewModel.Factory(
                        categoryId = categoryId,
                        prefilledDate = prefilledDate,
                        existingLogId = logId,
                        repository = app.trackingRepository
                    )
                )
                LogCategoryScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

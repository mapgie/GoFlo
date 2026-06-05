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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
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
import com.mapgie.goflo.ui.screens.dashboard.DashboardScreen
import com.mapgie.goflo.ui.screens.dashboard.DashboardViewModel
import com.mapgie.goflo.ui.screens.settings.PrivacyPolicyScreen
import com.mapgie.goflo.ui.screens.settings.SettingsScreen
import com.mapgie.goflo.ui.screens.settings.SettingsViewModel
import com.mapgie.goflo.ui.screens.alarms.CustomAlarmsScreen
import com.mapgie.goflo.ui.screens.alarms.CustomAlarmsViewModel
import com.mapgie.goflo.ui.screens.alarms.EditAlarmScreen
import com.mapgie.goflo.ui.screens.alarms.EditAlarmViewModel
import com.mapgie.goflo.ui.screens.manage.ManageCycleScreen
import com.mapgie.goflo.ui.screens.manage.ManageQuickLogScreen
import com.mapgie.goflo.ui.screens.manage.ManageScreen
import com.mapgie.goflo.ui.screens.manage.RemindersScreen
import com.mapgie.goflo.ui.screens.modes.ModesScreen
import com.mapgie.goflo.ui.screens.modes.ModesViewModel
import com.mapgie.goflo.ui.screens.stats.HeatmapScreen
import com.mapgie.goflo.ui.screens.stats.HeatmapViewModel
import com.mapgie.goflo.ui.screens.stats.StatsScreen
import com.mapgie.goflo.ui.screens.stats.StatsViewModel
import android.annotation.SuppressLint
import com.mapgie.goflo.ui.theme.AppTheme
import com.mapgie.goflo.ui.theme.GoFloTheme
import com.mapgie.goflo.AppIconChoice
import com.mapgie.goflo.AppIconManager
import com.mapgie.goflo.widget.QuickLogWidget
import androidx.compose.runtime.LaunchedEffect
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration

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
        }.getOrDefault(AppIconChoice.DEFAULT)
        AppIconManager.applyIcon(this, savedIconChoice)

        setContent {
            val mainVm: MainViewModel = viewModel(
                factory = MainViewModel.Factory(app.securityPreferences, app)
            )
            val appState by mainVm.appState.collectAsState()

            val appPrefs by app.preferencesStore.preferences.collectAsState(initial = initialPrefs)
            val currentTheme = runCatching { AppTheme.valueOf(appPrefs.theme) }.getOrDefault(AppTheme.CORAL)
            val customHues = if (currentTheme == AppTheme.CUSTOM) {
                Triple(appPrefs.customPrimaryHue, appPrefs.customSecondaryHue, appPrefs.customTertiaryHue)
            } else null

            GoFloTheme(appTheme = currentTheme, wcag = appPrefs.wcagMode, customHues = customHues) {
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

                    AppState.READY -> {
                        val pendingCategoryId = intent.getLongExtra(QuickLogWidget.EXTRA_CATEGORY_ID, -1L)
                        MainNavHost(app = app, currentTheme = currentTheme, pendingCategoryId = pendingCategoryId)
                    }
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
private fun MainNavHost(app: GoFloApplication, currentTheme: AppTheme, pendingCategoryId: Long = -1L) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val appPrefs by app.preferencesStore.preferences.collectAsState(initial = AppPreferences())
    val dashboardEnabled = appPrefs.dashboardEnabled

    // Deep-link from the Quick Log widget: navigate to the category log screen for today.
    LaunchedEffect(pendingCategoryId) {
        if (pendingCategoryId != -1L) {
            navController.navigate(
                Screen.LogCategory.newEntry(pendingCategoryId, java.time.LocalDate.now())
            )
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomNavRoutes = buildList {
        add(Screen.Home.route)
        add(Screen.History.route)
        if (dashboardEnabled) add(Screen.Dashboard.route)
        add(Screen.Stats.route)
        add(Screen.Manage.route)
        add(Screen.Settings.route)
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showBottomBar = bottomNavRoutes.any { currentRoute?.startsWith(it) == true } && !isLandscape

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route || currentRoute == Screen.Settings.route,
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
                    if (dashboardEnabled) {
                        NavigationBarItem(
                            selected = currentRoute == Screen.Dashboard.route,
                            onClick = { navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            } },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") }
                        )
                    }
                    NavigationBarItem(
                        selected = currentRoute == Screen.Stats.route,
                        onClick = { navController.navigate(Screen.Stats.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        } },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                        label = { Text("Stats") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Manage.route,
                        onClick = { navController.navigate(Screen.Manage.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        } },
                        icon = { Icon(Icons.Outlined.Tune, contentDescription = "Manage") },
                        label = { Text("Manage") }
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
                val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(app.repository, app))
                HistoryScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
            }

            composable(Screen.Stats.route) {
                val vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory(app.trackingRepository, app.preferencesStore, app.repository))
                StatsScreen(
                    viewModel = vm,
                    dashboardEnabled = dashboardEnabled,
                    onToggleDashboard = {
                        scope.launch { app.preferencesStore.setDashboardEnabled(!dashboardEnabled) }
                    },
                    onPinStat = { vm.pinCurrentView() },
                    onOpenHeatmap = { navController.navigate(Screen.Heatmap.route) },
                )
            }

            composable(Screen.Heatmap.route) {
                val vm: HeatmapViewModel = viewModel(
                    factory = HeatmapViewModel.Factory(app.trackingRepository, app.preferencesStore)
                )
                HeatmapScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(app.preferencesStore, app.trackingRepository)
                )
                DashboardScreen(viewModel = vm)
            }

            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        store = app.preferencesStore,
                        securityPreferences = app.securityPreferences,
                        repository = app.repository,
                        trackingRepository = app.trackingRepository,
                        alarmRepository = app.customAlarmRepository,
                        context = app.applicationContext
                    )
                )
                SettingsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
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
                    factory = com.mapgie.goflo.ui.screens.log.LogPeriodViewModel.Factory(app.repository, periodId, prefilledDate, app.trackingRepository, app)
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
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }

            // ── Manage tab ────────────────────────────────────────────────────────

            composable(Screen.Manage.route) {
                ManageScreen(
                    onNavigateToCategories = { navController.navigate(Screen.ManageCategories.route) },
                    onNavigateToReminders  = { navController.navigate(Screen.Reminders.route) },
                    onNavigateToCycle      = { navController.navigate(Screen.ManageCycle.route) },
                    onNavigateToQuickLog   = { navController.navigate(Screen.ManageQuickLog.route) },
                    onNavigateToModes      = { navController.navigate(Screen.TrackingModes.route) },
                    onNavigateToAlarms     = { navController.navigate(Screen.CustomAlarms.route) },
                )
            }

            composable(Screen.TrackingModes.route) {
                val vm: ModesViewModel = viewModel(
                    factory = ModesViewModel.Factory(app.trackingRepository, app.repository, app.preferencesStore)
                )
                ModesScreen(
                    viewModel      = vm,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Reminders.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        store                = app.preferencesStore,
                        securityPreferences  = app.securityPreferences,
                        repository           = app.repository,
                        trackingRepository   = app.trackingRepository,
                        alarmRepository      = app.customAlarmRepository,
                        context              = app.applicationContext
                    )
                )
                RemindersScreen(
                    viewModel = vm,
                    onBack    = { navController.popBackStack() }
                )
            }

            composable(Screen.ManageCycle.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        store                = app.preferencesStore,
                        securityPreferences  = app.securityPreferences,
                        repository           = app.repository,
                        trackingRepository   = app.trackingRepository,
                        alarmRepository      = app.customAlarmRepository,
                        context              = app.applicationContext
                    )
                )
                ManageCycleScreen(
                    viewModel = vm,
                    onBack    = { navController.popBackStack() }
                )
            }

            composable(Screen.ManageQuickLog.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        store                = app.preferencesStore,
                        securityPreferences  = app.securityPreferences,
                        repository           = app.repository,
                        trackingRepository   = app.trackingRepository,
                        alarmRepository      = app.customAlarmRepository,
                        context              = app.applicationContext
                    )
                )
                ManageQuickLogScreen(
                    viewModel = vm,
                    onBack    = { navController.popBackStack() }
                )
            }

            // ── Custom alarms ─────────────────────────────────────────────────────

            composable(Screen.CustomAlarms.route) {
                val vm: CustomAlarmsViewModel = viewModel(
                    factory = CustomAlarmsViewModel.Factory(
                        app.customAlarmRepository, app.trackingRepository, app.applicationContext
                    )
                )
                CustomAlarmsScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNewAlarm = { navController.navigate(Screen.EditAlarm.newAlarm) },
                    onNavigateToEditAlarm = { alarmId ->
                        navController.navigate(Screen.EditAlarm.forAlarm(alarmId))
                    },
                )
            }

            composable(
                route = Screen.EditAlarm.route,
                arguments = listOf(
                    navArgument("alarmId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
                )
            ) { backStack ->
                val alarmId = backStack.arguments?.getLong("alarmId") ?: -1L
                val categoryId = backStack.arguments?.getLong("categoryId") ?: -1L
                val vm: EditAlarmViewModel = viewModel(
                    key = "edit_alarm_${alarmId}_$categoryId",
                    factory = EditAlarmViewModel.Factory(
                        app.customAlarmRepository, app.trackingRepository,
                        app.applicationContext, alarmId, categoryId
                    )
                )
                EditAlarmScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // ── Tracking categories management ────────────────────────────────────

            composable(Screen.ManageCategories.route) {
                val vm: ManageCategoriesViewModel = viewModel(
                    factory = ManageCategoriesViewModel.Factory(app.trackingRepository, app.preferencesStore)
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
                    factory = ManageCategoryValuesViewModel.Factory(
                        categoryId, app.trackingRepository, app.customAlarmRepository
                    )
                )
                ManageCategoryValuesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNewAlarm = {
                        navController.navigate(Screen.EditAlarm.newForCategory(categoryId))
                    },
                    onNavigateToEditAlarm = { alarmId ->
                        navController.navigate(Screen.EditAlarm.forAlarm(alarmId))
                    },
                )
            }

            // ── Per-day category logging ─────────────────────────────────────────

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

package com.mapgie.goflo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.mapgie.goflo.ui.navigation.Screen
import com.mapgie.goflo.ui.screens.history.HistoryScreen
import com.mapgie.goflo.ui.screens.history.HistoryViewModel
import com.mapgie.goflo.ui.screens.home.HomeScreen
import com.mapgie.goflo.ui.screens.home.HomeViewModel
import com.mapgie.goflo.ui.screens.log.LogPeriodScreen
import com.mapgie.goflo.ui.screens.log.LogPeriodViewModel
import com.mapgie.goflo.ui.screens.settings.SettingsScreen
import com.mapgie.goflo.ui.screens.settings.SettingsViewModel
import com.mapgie.goflo.ui.theme.AppTheme
import com.mapgie.goflo.ui.theme.GoFloTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val app = application as GoFloApplication

        setContent {
            val prefsStore = app.preferencesStore
            val appPrefs by prefsStore.preferences.collectAsState(initial = com.mapgie.goflo.data.preferences.AppPreferences())
            val currentTheme = runCatching { AppTheme.valueOf(appPrefs.theme) }.getOrDefault(AppTheme.CORAL)

            GoFloTheme(appTheme = currentTheme) {
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
                                    onClick = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, null) },
                                    label = { Text("Home") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.History.route,
                                    onClick = {
                                        navController.navigate(Screen.History.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.DateRange, null) },
                                    label = { Text("History") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.Settings.route,
                                    onClick = {
                                        navController.navigate(Screen.Settings.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Settings, null) },
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
                            val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.repository))
                            HomeScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
                        }

                        composable(Screen.History.route) {
                            val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(app.repository))
                            HistoryScreen(viewModel = vm, onNavigate = { navController.navigate(it) })
                        }

                        composable(Screen.Settings.route) {
                            val vm: SettingsViewModel = viewModel(
                                factory = SettingsViewModel.Factory(
                                    store = prefsStore,
                                    repository = app.repository,
                                    context = applicationContext
                                )
                            )
                            SettingsScreen(viewModel = vm)
                        }

                        composable(
                            route = Screen.LogPeriod.route,
                            arguments = listOf(navArgument("periodId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            })
                        ) { backStack ->
                            val periodId = backStack.arguments?.getLong("periodId") ?: -1L
                            val vm: LogPeriodViewModel = viewModel(
                                key = "log_$periodId",
                                factory = LogPeriodViewModel.Factory(app.repository, periodId)
                            )
                            LogPeriodScreen(viewModel = vm, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

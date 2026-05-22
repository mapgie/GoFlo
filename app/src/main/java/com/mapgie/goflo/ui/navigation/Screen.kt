package com.mapgie.goflo.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object LogPeriod : Screen("log_period?periodId={periodId}") {
        fun withId(periodId: Long) = "log_period?periodId=$periodId"
        val newEntry = "log_period?periodId=-1"
    }
    data object PinSetup : Screen("pin_setup?changing={changing}") {
        val newPin = "pin_setup?changing=false"
        val changePin = "pin_setup?changing=true"
    }
    data object Licenses : Screen("licenses")
}

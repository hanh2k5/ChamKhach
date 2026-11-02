package com.telesales.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.telesales.assistant.ui.theme.TelesalesTheme
import com.telesales.assistant.ui.settings.SettingsRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.telesales.assistant.ui.customers.CustomersRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as TelesalesApplication).container
        setContent {
            TelesalesTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "customers") {
                    composable("customers") {
                        CustomersRoute(
                            repository = container.customerRepository,
                            onSettings = { navController.navigate("settings") },
                        )
                    }
                    composable("settings") {
                        SettingsRoute(
                            settingsStore = container.settingsStore,
                            apiServiceFactory = container.apiServiceFactory,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}


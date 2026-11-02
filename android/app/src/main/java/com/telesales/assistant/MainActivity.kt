package com.telesales.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.telesales.assistant.ui.theme.TelesalesTheme
import com.telesales.assistant.ui.settings.SettingsRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.telesales.assistant.ui.customers.CustomersRoute
import com.telesales.assistant.ui.customerform.CustomerFormRoute
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as TelesalesApplication).container
        setContent {
            TelesalesTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "customers") {
                    composable("customers") { entry ->
                        val shouldRefresh by entry.savedStateHandle.getStateFlow("refreshCustomers", false).collectAsState()
                        CustomersRoute(
                            repository = container.customerRepository,
                            onSettings = { navController.navigate("settings") },
                            onAddCustomer = { navController.navigate("customerForm/new") },
                            onEditCustomer = { customerId -> navController.navigate("customerForm/$customerId") },
                            shouldRefresh = shouldRefresh,
                            onRefreshConsumed = { entry.savedStateHandle["refreshCustomers"] = false },
                        )
                    }
                    composable(
                        route = "customerForm/{customerId}",
                        arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
                    ) { entry ->
                        val routeId = entry.arguments?.getString("customerId")
                        val customerId = routeId?.toLongOrNull()
                        CustomerFormRoute(
                            repository = container.customerFormRepository,
                            customerId = customerId,
                            onBack = { navController.popBackStack() },
                            onOpenCustomer = { existingId ->
                                navController.navigate("customerForm/$existingId") {
                                    popUpTo("customers") { inclusive = false }
                                }
                            },
                            onSettings = { navController.navigate("settings") },
                            onSaved = {
                                navController.previousBackStackEntry?.savedStateHandle?.set("refreshCustomers", true)
                                navController.popBackStack()
                            },
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


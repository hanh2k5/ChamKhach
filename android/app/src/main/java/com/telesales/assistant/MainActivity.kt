package com.telesales.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.telesales.assistant.ui.customerdetail.CustomerDetailRoute
import com.telesales.assistant.ui.calllog.CallLogRoute
import com.telesales.assistant.ui.followups.FollowUpsRoute
import com.telesales.assistant.ui.dashboard.DashboardRoute

class MainActivity : ComponentActivity() {
    private var reminderNavController: androidx.navigation.NavHostController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as TelesalesApplication).container
        setContent {
            TelesalesTheme {
                val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                val navController = rememberNavController()
                reminderNavController = navController
                val initialCustomerId = intent.getLongExtra(com.telesales.assistant.reminder.NotificationHelper.EXTRA_CUSTOMER_ID, 0L)
                if (initialCustomerId > 0) {
                    LaunchedEffect(initialCustomerId) {
                        container.reminderCoordinator.sync(container.followUpRepository)
                    }
                }
                NavHost(navController = navController, startDestination = if (initialCustomerId > 0) "customerDetail/$initialCustomerId" else "dashboard") {
                    composable("dashboard") {
                        DashboardRoute(
                            repository = container.dashboardRepository,
                            onSettings = { navController.navigate("settings") },
                            onAddCustomer = { navController.navigate("customerForm/new") },
                            onCustomers = { navController.navigate("customers") },
                            onFollowUps = { navController.navigate("followUps") },
                            onCustomer = { customerId -> navController.navigate("customerDetail/$customerId") },
                            onCall = { phone -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) },
                            followUpRepository = container.followUpRepository,
                            reminderCoordinator = container.reminderCoordinator,
                        )
                    }
                    composable("customers") { entry ->
                        val shouldRefresh by entry.savedStateHandle.getStateFlow("refreshCustomers", false).collectAsState()
                        CustomersRoute(
                            repository = container.customerRepository,
                            onSettings = { navController.navigate("settings") },
                            onAddCustomer = { navController.navigate("customerForm/new") },
                            onFollowUps = { navController.navigate("followUps") },
                            onDashboard = { navController.popBackStack("dashboard", inclusive = false) },
                            onEditCustomer = { customerId -> navController.navigate("customerDetail/$customerId") },
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
                                navController.navigate("customerDetail/$existingId") {
                                    popUpTo("customers") { inclusive = false }
                                }
                            },
                            onSettings = { navController.navigate("settings") },
                            onSaved = {
                                val previous = navController.previousBackStackEntry
                                if (previous?.destination?.route?.startsWith("customerDetail/") == true) {
                                    previous.savedStateHandle["refreshCustomerDetail"] = true
                                } else {
                                    previous?.savedStateHandle?.set("refreshCustomers", true)
                                }
                                navController.popBackStack()
                            },
                        )
                    }
                    composable(
                        route = "customerDetail/{customerId}",
                        arguments = listOf(navArgument("customerId") { type = NavType.LongType }),
                    ) { entry ->
                        val customerId = entry.arguments?.getLong("customerId") ?: return@composable
                        val shouldRefresh by entry.savedStateHandle.getStateFlow("refreshCustomerDetail", false).collectAsState()
                        CustomerDetailRoute(
                            customerId = customerId,
                            repository = container.customerDetailRepository,
                            shouldRefresh = shouldRefresh,
                            onRefreshConsumed = { entry.savedStateHandle["refreshCustomerDetail"] = false },
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate("customerForm/$customerId") },
                            onSettings = { navController.navigate("settings") },
                            onCall = { phone -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) },
                            onCallLog = { id -> navController.navigate("callLog/$id") },
                            reminderCoordinator = container.reminderCoordinator,
                            onArchived = {
                                Toast.makeText(this@MainActivity, getString(com.telesales.assistant.R.string.detail_archived_notice), Toast.LENGTH_SHORT).show()
                                navController.previousBackStackEntry?.savedStateHandle?.set("refreshCustomers", true)
                                navController.popBackStack()
                            },
                        )
                    }
                    composable("callLog/{customerId}", arguments = listOf(navArgument("customerId") { type = NavType.LongType })) { entry ->
                        val customerId = entry.arguments?.getLong("customerId") ?: return@composable
                        CallLogRoute(
                            customerId = customerId,
                            repository = container.callLogRepository,
                            onBack = { navController.popBackStack() },
                            onSaved = {
                                navController.previousBackStackEntry?.savedStateHandle?.set("refreshCustomerDetail", true)
                                navController.popBackStack()
                            },
                            reminderCoordinator = container.reminderCoordinator,
                        )
                    }
                    composable("followUps") {
                        FollowUpsRoute(
                            repository = container.followUpRepository,
                            onSettings = { navController.navigate("settings") },
                            onBack = { navController.popBackStack() },
                            onCall = { phone -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) },
                            onCustomer = { customerId -> navController.navigate("customerDetail/$customerId") },
                            reminderCoordinator = container.reminderCoordinator,
                        )
                    }
                    composable("settings") {
                        SettingsRoute(
                            settingsStore = container.settingsStore,
                            apiServiceFactory = container.apiServiceFactory,
                            onBack = { navController.popBackStack() },
                            permissionStateProvider = com.telesales.assistant.ui.settings.AndroidReminderPermissionStateProvider(this@MainActivity),
                            reminderCoordinator = container.reminderCoordinator,
                            debugBuild = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val customerId = intent.getLongExtra(com.telesales.assistant.reminder.NotificationHelper.EXTRA_CUSTOMER_ID, 0L)
        if (customerId > 0) reminderNavController?.navigate("customerDetail/$customerId")
    }
}


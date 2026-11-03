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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.telesales.assistant.ui.customers.CustomersRoute
import com.telesales.assistant.ui.customerform.CustomerFormRoute
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.telesales.assistant.ui.customerdetail.CustomerDetailRoute
import com.telesales.assistant.ui.calllog.CallLogRoute
import com.telesales.assistant.ui.followups.FollowUpsRoute
import com.telesales.assistant.ui.dashboard.DashboardRoute
import com.telesales.assistant.ui.navigation.AdaptiveNavigationShell
import com.telesales.assistant.ui.components.GlassCard

class MainActivity : ComponentActivity() {
    private var reminderNavController: androidx.navigation.NavHostController? = null
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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
                val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
                val compact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
                val initialCustomerId = intent.getLongExtra(com.telesales.assistant.reminder.NotificationHelper.EXTRA_CUSTOMER_ID, 0L)
                var selectedCustomerId by rememberSaveable { mutableStateOf(initialCustomerId.takeIf { it > 0 }) }
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                LaunchedEffect(initialCustomerId) {
                    withContext(Dispatchers.IO) { container.reminderCoordinator.recoverAfterBoot() }
                    if (initialCustomerId > 0) {
                        withContext(Dispatchers.IO) { container.reminderCoordinator.sync(container.followUpRepository) }
                    }
                }
                AdaptiveNavigationShell(
                    windowSizeClass = windowSizeClass,
                    selectedRoute = currentRoute,
                    visible = currentRoute in setOf("dashboard", "customers", "followUps", "settings"),
                    onNavigate = { route -> if (route != currentRoute) navController.navigate(route) { launchSingleTop = true } },
                ) { navigationContentModifier ->
                NavHost(
                    navController = navController,
                    modifier = navigationContentModifier,
                    startDestination = when {
                        initialCustomerId <= 0 -> "dashboard"
                        compact -> "customerDetail/$initialCustomerId"
                        else -> "customers"
                    },
                ) {
                    composable("dashboard") {
                        DashboardRoute(
                            repository = container.dashboardRepository,
                            onSettings = { navController.navigate("settings") },
                            onAddCustomer = { navController.navigate("customerForm/new") },
                            onCustomers = { navController.navigate("customers") },
                            onFollowUps = { navController.navigate("followUps") },
                            onCustomer = { customerId ->
                                if (compact) navController.navigate("customerDetail/$customerId")
                                else { selectedCustomerId = customerId; navController.navigate("customers") }
                            },
                            onCall = { phone -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) },
                            followUpRepository = container.followUpRepository,
                            reminderCoordinator = container.reminderCoordinator,
                        )
                    }
                    composable("customers") { entry ->
                        val shouldRefresh by entry.savedStateHandle.getStateFlow("refreshCustomers", false).collectAsState()
                        val shouldRefreshDetailPane by entry.savedStateHandle.getStateFlow("refreshCustomerDetailPane", false).collectAsState()
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 12.dp)) {
                            CustomersRoute(
                                repository = container.customerRepository,
                                onSettings = { navController.navigate("settings") },
                                onDashboard = { navController.popBackStack("dashboard", inclusive = false) },
                                onEditCustomer = { customerId ->
                                    if (compact) navController.navigate("customerDetail/$customerId")
                                    else selectedCustomerId = customerId
                                },
                                shouldRefresh = shouldRefresh,
                                onRefreshConsumed = { entry.savedStateHandle["refreshCustomers"] = false },
                                modifier = Modifier.weight(if (compact) 1f else 0.44f),
                            )
                            if (!compact) {
                                androidx.compose.material3.VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Box(Modifier.weight(0.56f).fillMaxSize(), contentAlignment = Alignment.Center) {
                                    val customerId = selectedCustomerId
                                    if (customerId == null) {
                                        GlassCard(Modifier.padding(24.dp)) {
                                            Text(
                                                text = getString(com.telesales.assistant.R.string.customers_select_detail),
                                                modifier = Modifier.padding(24.dp),
                                                style = MaterialTheme.typography.bodyLarge,
                                            )
                                        }
                                    } else {
                                        CustomerDetailRoute(
                                            customerId = customerId,
                                            repository = container.customerDetailRepository,
                                            shouldRefresh = shouldRefreshDetailPane,
                                            onRefreshConsumed = { entry.savedStateHandle["refreshCustomerDetailPane"] = false },
                                            onBack = { selectedCustomerId = null },
                                            onEdit = { navController.navigate("customerForm/$customerId") },
                                            onSettings = { navController.navigate("settings") },
                                            onCall = { phone -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) },
                                            onCallLog = { id -> navController.navigate("callLog/$id") },
                                            reminderCoordinator = container.reminderCoordinator,
                                            modifier = Modifier.fillMaxSize(),
                                            sidePane = true,
                                            onDeleted = {
                                                selectedCustomerId = null
                                                entry.savedStateHandle["refreshCustomers"] = true
                                                Toast.makeText(this@MainActivity, getString(com.telesales.assistant.R.string.detail_deleted_notice), Toast.LENGTH_SHORT).show()
                                            },
                                        )
                                    }
                                }
                            }
                        }
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
                                    if (previous?.destination?.route == "customers" && selectedCustomerId != null) {
                                        previous.savedStateHandle["refreshCustomerDetailPane"] = true
                                    }
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
                            onDeleted = {
                                Toast.makeText(this@MainActivity, getString(com.telesales.assistant.R.string.detail_deleted_notice), Toast.LENGTH_SHORT).show()
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
                                val previous = navController.previousBackStackEntry
                                if (previous?.destination?.route == "customers" && selectedCustomerId != null) {
                                    previous.savedStateHandle["refreshCustomers"] = true
                                    previous.savedStateHandle["refreshCustomerDetailPane"] = true
                                } else {
                                    previous?.savedStateHandle?.set("refreshCustomerDetail", true)
                                }
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
                            onCustomer = { customerId ->
                                if (compact) navController.navigate("customerDetail/$customerId")
                                else { selectedCustomerId = customerId; navController.navigate("customers") }
                            },
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val customerId = intent.getLongExtra(com.telesales.assistant.reminder.NotificationHelper.EXTRA_CUSTOMER_ID, 0L)
        if (customerId > 0) reminderNavController?.navigate("customerDetail/$customerId")
    }
}


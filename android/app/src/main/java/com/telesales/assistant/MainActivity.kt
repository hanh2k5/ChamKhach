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
import androidx.compose.runtime.DisposableEffect
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
    private var onNotificationClick: ((Long) -> Unit)? = null
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
                val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
                val compact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
                val initialCustomerId = intent.getLongExtra(com.telesales.assistant.reminder.NotificationHelper.EXTRA_CUSTOMER_ID, 0L)
                var selectedCustomerId by rememberSaveable { mutableStateOf(initialCustomerId.takeIf { it > 0 }) }
                var pendingCustomerId by rememberSaveable { mutableStateOf(initialCustomerId.takeIf { it > 0 }) }

                DisposableEffect(Unit) {
                    onNotificationClick = { id -> pendingCustomerId = id }
                    onDispose { onNotificationClick = null }
                }

                LaunchedEffect(pendingCustomerId) {
                    val targetId = pendingCustomerId ?: return@LaunchedEffect
                    if (compact) {
                        navController.navigate("customerDetail/$targetId") {
                            launchSingleTop = true
                        }
                    } else {
                        selectedCustomerId = targetId
                        navController.navigate("customers") {
                            launchSingleTop = true
                        }
                    }
                    pendingCustomerId = null
                }

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
                    visible = currentRoute in setOf("dashboard", "settings"),
                    onNavigate = { route -> if (route != currentRoute) navController.navigate(route) { launchSingleTop = true } },
                ) { navigationContentModifier ->
                NavHost(
                    navController = navController,
                    modifier = navigationContentModifier,
                    startDestination = "dashboard",
                ) {
                    composable("dashboard") {
                        val dashboardEntry = navController.getBackStackEntry("dashboard")
                        val shouldRefresh by dashboardEntry.savedStateHandle
                            .getStateFlow("refreshDashboard", false)
                            .collectAsState()
                        DashboardRoute(
                            repository = container.dashboardRepository,
                            followUpRepository = container.followUpRepository,
                            onSettings = { navController.navigate("settings") },
                            onOpenOverdue = { navController.navigate("followUps?group=overdue") },
                            onOpenToday = { navController.navigate("followUps?group=today") },
                            onOpenPotential = { navController.navigate("customers?potential=high,medium") },
                            onAddPotential = { navController.navigate("customerForm/new") },
                            shouldRefresh = shouldRefresh,
                            onRefreshConsumed = { dashboardEntry.savedStateHandle["refreshDashboard"] = false },
                            reminderCoordinator = container.reminderCoordinator,
                        )
                    }
                    composable(
                        route = "customers?potential={potential}",
                        arguments = listOf(navArgument("potential") { nullable = true }),
                    ) { entry ->
                        val initialPotential = entry.arguments?.getString("potential")
                            ?.takeUnless { it == "all" || it == "null" || it.isBlank() }
                        val shouldRefresh by entry.savedStateHandle.getStateFlow("refreshCustomers", false).collectAsState()
                        val shouldRefreshDetailPane by entry.savedStateHandle.getStateFlow("refreshCustomerDetailPane", false).collectAsState()
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 12.dp)) {
                            CustomersRoute(
                                repository = container.customerRepository,
                                initialPotential = initialPotential,
                                onSettings = { navController.navigate("settings") },
                                onDashboard = {
                                    navController.getBackStackEntry("dashboard")
                                        .savedStateHandle["refreshDashboard"] = true
                                    navController.popBackStack("dashboard", inclusive = false)
                                },
                                onEditCustomer = { customerId ->
                                    if (compact) navController.navigate("customerDetail/$customerId")
                                    else selectedCustomerId = customerId
                                },
                                onCall = { phone -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) },
                                onAddCustomer = { navController.navigate("customerForm/new") },
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
                            reminderCoordinator = container.reminderCoordinator,
                            onBack = {
                                runCatching { navController.getBackStackEntry("dashboard").savedStateHandle["refreshDashboard"] = true }
                                navController.popBackStack()
                            },
                            onOpenCustomer = { existingId ->
                                navController.navigate("customerDetail/$existingId") {
                                    popUpTo("customers") { inclusive = false }
                                }
                            },
                            onSettings = { navController.navigate("settings") },
                            onSaved = { _ ->
                                runCatching { navController.getBackStackEntry("dashboard").savedStateHandle["refreshDashboard"] = true }
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
                                // Dashboard tự refresh qua ON_RESUME khi user quay về — không cần set thêm
                                // Chỉ set refreshDashboard nếu previous KHÔNG phải followUps (tức là quay thẳng về dashboard)
                                val previous = navController.previousBackStackEntry
                                val prevRoute = previous?.destination?.route ?: ""
                                if (!prevRoute.startsWith("followUps") && prevRoute != "customers") {
                                    runCatching {
                                        navController.getBackStackEntry("dashboard").savedStateHandle["refreshDashboard"] = true
                                    }
                                }
                                if (prevRoute.startsWith("followUps")) {
                                    previous?.savedStateHandle?.set("refreshFollowUps", true)
                                } else if (prevRoute == "customers" && selectedCustomerId != null) {
                                    previous?.savedStateHandle?.set("refreshCustomers", true)
                                    previous?.savedStateHandle?.set("refreshCustomerDetailPane", true)
                                } else {
                                    previous?.savedStateHandle?.set("refreshCustomerDetail", true)
                                }
                                navController.popBackStack()
                            },
                            reminderCoordinator = container.reminderCoordinator,
                        )
                    }
                    composable(
                        route = "followUps?group={group}",
                        arguments = listOf(navArgument("group") { defaultValue = "all" }),
                    ) { entry ->
                        val initialGroup = entry.arguments?.getString("group") ?: "all"
                        val shouldRefresh by entry.savedStateHandle
                            .getStateFlow("refreshFollowUps", false)
                            .collectAsState()
                        FollowUpsRoute(
                            repository = container.followUpRepository,
                            onSettings = { navController.navigate("settings") },
                            onBack = {
                                navController.getBackStackEntry("dashboard")
                                    .savedStateHandle["refreshDashboard"] = true
                                navController.popBackStack()
                            },
                            onCall = { phone -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) },
                            onCallLog = { customerId -> navController.navigate("callLog/$customerId") },
                            onCustomer = { customerId ->
                                if (compact) navController.navigate("customerDetail/$customerId")
                                else { selectedCustomerId = customerId; navController.navigate("customers") }
                            },
                            reminderCoordinator = container.reminderCoordinator,
                            initialGroup = initialGroup,
                            shouldRefresh = shouldRefresh,
                            onRefreshConsumed = { entry.savedStateHandle["refreshFollowUps"] = false },
                        )
                    }
                    composable("settings") {
                        SettingsRoute(
                            settingsStore = container.settingsStore,
                            apiServiceFactory = container.apiServiceFactory,
                            onBack = {
                                runCatching { navController.getBackStackEntry("dashboard").savedStateHandle["refreshDashboard"] = true }
                                navController.popBackStack()
                            },
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
        if (customerId > 0) {
            onNotificationClick?.invoke(customerId)
        }
    }
}

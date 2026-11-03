package com.telesales.assistant.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.R
import com.telesales.assistant.data.repository.DashboardRepository
import com.telesales.assistant.domain.model.FollowUp
import com.telesales.assistant.ui.components.FollowUpCard
import com.telesales.assistant.ui.components.rememberPullToRefreshGesture
import java.time.Clock
import java.time.ZoneId

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DashboardRoute(
    repository: DashboardRepository,
    onSettings: () -> Unit,
    onAddCustomer: () -> Unit,
    onCustomers: () -> Unit,
    onFollowUps: () -> Unit,
    onCustomer: (Long) -> Unit,
    onCall: (String) -> Unit,
    clock: Clock = Clock.systemUTC(),
    businessZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    followUpRepository: com.telesales.assistant.data.repository.FollowUpRepository? = null,
    reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator? = null,
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardFactory(repository, clock, businessZone, followUpRepository, reminderCoordinator))
    val state by vm.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val firstResume = remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume.value) firstResume.value = false else vm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DashboardScreen(state, vm, onSettings, onAddCustomer, onCustomers, onFollowUps, onCustomer, onCall)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DashboardScreen(
    state: DashboardUiState,
    vm: DashboardViewModel,
    onSettings: () -> Unit,
    onAddCustomer: () -> Unit,
    onCustomers: () -> Unit,
    onFollowUps: () -> Unit,
    onCustomer: (Long) -> Unit,
    onCall: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val pullRefresh = rememberPullToRefreshGesture(listState, state.isRefreshing, vm::refresh)
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title), maxLines = 1, overflow = TextOverflow.Ellipsis) }, actions = {
            TextButton(onClick = onCustomers) { Text(stringResource(R.string.dashboard_customers), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_settings), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddCustomer) { Text(stringResource(R.string.dashboard_add_customer)) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isRefreshing || pullRefresh.isPulling) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(
                modifier = Modifier.fillMaxSize().then(pullRefresh.modifier).padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.dashboard != null) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard(stringResource(R.string.dashboard_overdue), state.dashboard.overdueCount, onFollowUps, Modifier.weight(1f))
                            MetricCard(stringResource(R.string.dashboard_today), state.dashboard.todayCount, onFollowUps, Modifier.weight(1f))
                            MetricCard(stringResource(R.string.dashboard_potential), state.dashboard.potentialCount, onCustomers, Modifier.weight(1f))
                        }
                    }
                    if (state.error != null) item {
                        Column {
                            Text(errorText(state.error), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = vm::retry) { Text(stringResource(R.string.customers_retry)) }
                            if (state.error == DashboardError.Unauthorized || state.error == DashboardError.SetupRequired) {
                                TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_open_settings)) }
                            }
                        }
                    }
                    item { Text(stringResource(R.string.dashboard_due_items), style = MaterialTheme.typography.titleLarge) }
                    if (state.dashboard.items.isEmpty()) {
                        item { Text(stringResource(R.string.dashboard_empty)) }
                    } else {
                        items(state.dashboard.items, key = { it.id }) { item ->
                            DashboardFollowUpRow(item, vm, onCall) { onCustomer(item.customer.id) }
                        }
                    }
                } else if (state.isLoading) {
                    item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.dashboard_loading))
                    } }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(errorText(state.error ?: DashboardError.Server), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = vm::retry) { Text(stringResource(R.string.customers_retry)) }
                            if (state.error == DashboardError.SetupRequired || state.error == DashboardError.Unauthorized) {
                                TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_open_settings)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun DashboardFollowUpRow(item: FollowUp, vm: DashboardViewModel, onCall: (String) -> Unit, onCustomer: () -> Unit) {
    FollowUpCard(item, vm.formatTime(item), { onCall(item.customer.phone) }, onCustomer)
}

@Composable
private fun errorText(error: DashboardError): String = when (error) {
    DashboardError.SetupRequired -> stringResource(R.string.dashboard_setup_required)
    DashboardError.Network -> stringResource(R.string.customers_network_error)
    DashboardError.Unauthorized -> stringResource(R.string.customers_unauthorized)
    DashboardError.Server -> stringResource(R.string.customers_server_error)
}

private class DashboardFactory(private val repository: DashboardRepository, private val clock: Clock, private val zone: ZoneId, private val followUpRepository: com.telesales.assistant.data.repository.FollowUpRepository?, private val reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
        return modelClass.cast(DashboardViewModel(repository, clock, zone, followUpRepository, reminderCoordinator))
    }
}

package com.telesales.assistant.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telesales.assistant.R
import com.telesales.assistant.ui.customers.CustomerError
import com.telesales.assistant.domain.model.Customer
import com.telesales.assistant.ui.components.LoadStateContent
import java.time.Clock
import java.time.ZoneId

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CustomersRoute(
    repository: com.telesales.assistant.data.repository.CustomerRepository,
    onSettings: () -> Unit,
    onAddCustomer: () -> Unit,
    onFollowUps: () -> Unit,
    onDashboard: () -> Unit,
    onEditCustomer: (Long) -> Unit,
    shouldRefresh: Boolean,
    onRefreshConsumed: () -> Unit,
    clock: Clock = Clock.systemUTC(),
    zoneId: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
) {
    val vm: CustomersViewModel = viewModel(factory = CustomersViewModelFactory(repository))
    val state by vm.state.collectAsState()
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            vm.refresh()
            onRefreshConsumed()
        }
    }
    CustomersScreen(
        state = state,
        vm = vm,
        onSettings = onSettings,
        clock = clock,
        zoneId = zoneId,
        onAddCustomer = onAddCustomer,
        onFollowUps = onFollowUps,
        onDashboard = onDashboard,
        onEditCustomer = onEditCustomer,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomersScreen(
    state: CustomersUiState,
    vm: CustomersViewModel,
    onSettings: () -> Unit,
    clock: Clock,
    zoneId: ZoneId,
    onAddCustomer: () -> Unit,
    onFollowUps: () -> Unit,
    onDashboard: () -> Unit,
    onEditCustomer: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.customers_title)) }, navigationIcon = {
        TextButton(onClick = onDashboard) { Text(stringResource(R.string.dashboard_title)) }
    }, actions = {
        TextButton(onClick = onAddCustomer) { Text(stringResource(R.string.customer_form_add_title)) }
        TextButton(onClick = onFollowUps) { Text(stringResource(R.string.followups_title)) }
        TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_settings)) }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(state.query, vm::onQueryChanged, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.customers_search)) }, singleLine = true)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterMenu(stringResource(R.string.customers_status), state.status, listOf("active", "archived", "all")) { it?.let(vm::onStatusChanged) }
                FilterMenu(stringResource(R.string.customers_potential), state.potential, listOf(null, "high", "medium", "low", "unrated"), vm::onPotentialChanged)
                FilterMenu(stringResource(R.string.customers_result), state.lastResult, listOf(null, "interested", "no_answer", "callback", "rejected"), vm::onLastResultChanged)
            }
            if (state.isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.error != null) {
                val msg = when (state.error) {
                    CustomerError.SetupRequired -> stringResource(R.string.customers_setup_required)
                    CustomerError.Network -> stringResource(R.string.customers_network_error)
                    CustomerError.Unauthorized -> stringResource(R.string.customers_unauthorized)
                    CustomerError.Server -> stringResource(R.string.customers_server_error)
                }
                LoadStateContent(msg, stringResource(R.string.customers_retry), loading = false, onRetry = if (state.error == CustomerError.SetupRequired || state.error == CustomerError.Unauthorized) null else vm::retry)
                if (state.error == CustomerError.SetupRequired || state.error == CustomerError.Unauthorized) TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_open_settings)) }
            } else if (state.isLoading && state.customers.isEmpty()) {
                LoadStateContent(stringResource(R.string.customers_loading), stringResource(R.string.customers_retry), loading = true)
            } else if (state.customers.isEmpty()) {
                LoadStateContent(stringResource(R.string.customers_empty), stringResource(R.string.customers_retry), loading = false, onRetry = vm::refresh)
            } else {
                TextButton(onClick = vm::refresh) { Text(stringResource(R.string.customers_refresh)) }
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.customers, key = { it.id }) { customer -> CustomerRow(customer, clock, zoneId) { onEditCustomer(customer.id) } }
                    if (state.isLoadingMore) item { CircularProgressIndicator() }
                }
                val layout by remember { derivedStateOf { listState.layoutInfo } }
                LaunchedEffect(layout.visibleItemsInfo.lastOrNull()?.index, state.customers.size) {
                    if (layout.visibleItemsInfo.lastOrNull()?.index == state.customers.lastIndex) vm.loadMore()
                }
            }
        }
    }
}

@Composable
private fun FilterMenu(label: String, selected: String?, options: List<String?>, onSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.customers_filter_label, label, filterValueLabel(selected))) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { value -> DropdownMenuItem(text = { Text(filterValueLabel(value)) }, onClick = { expanded = false; onSelected(value) }) }
        }
    }
}

@Composable
private fun filterValueLabel(value: String?): String = when (value) {
    "active" -> stringResource(R.string.status_active)
    "archived" -> stringResource(R.string.status_archived)
    "all" -> stringResource(R.string.status_all)
    "high" -> stringResource(R.string.potential_high)
    "medium" -> stringResource(R.string.potential_medium)
    "low" -> stringResource(R.string.potential_low)
    "unrated" -> stringResource(R.string.potential_unrated)
    "interested" -> stringResource(R.string.result_interested)
    "no_answer" -> stringResource(R.string.result_no_answer)
    "callback" -> stringResource(R.string.result_callback)
    "rejected" -> stringResource(R.string.result_rejected)
    else -> stringResource(R.string.customers_filter_all)
}

@Composable
private fun CustomerRow(customer: Customer, clock: Clock, zoneId: ZoneId, onClick: () -> Unit) {
    val time = customer.formattedLastContactedAt(clock, zoneId) ?: stringResource(R.string.customers_not_contacted)
    val result = when (customer.lastResult) {
        "interested" -> stringResource(R.string.result_interested)
        "no_answer" -> stringResource(R.string.result_no_answer)
        "callback" -> stringResource(R.string.result_callback)
        "rejected" -> stringResource(R.string.result_rejected)
        else -> stringResource(R.string.customers_not_contacted)
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(customer.companyName ?: customer.contactName ?: customer.phone, style = MaterialTheme.typography.titleMedium)
            customer.contactName?.let { Text(it) }
            Text(customer.phone)
            customer.industry?.let { Text(it) }
            Text(stringResource(R.string.customers_last_contact, time))
            Text(stringResource(R.string.customers_last_result, result))
        }
    }
}

package com.telesales.assistant.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telesales.assistant.R
import com.telesales.assistant.ui.customers.CustomerError
import com.telesales.assistant.domain.model.Customer
import com.telesales.assistant.ui.components.LoadStateContent
import com.telesales.assistant.ui.components.GlassCard
import java.time.Clock
import java.time.ZoneId

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CustomersRoute(
    repository: com.telesales.assistant.data.repository.CustomerRepository,
    onSettings: () -> Unit,
    onDashboard: () -> Unit,
    onEditCustomer: (Long) -> Unit,
    shouldRefresh: Boolean,
    onRefreshConsumed: () -> Unit,
    clock: Clock = Clock.systemUTC(),
    zoneId: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    modifier: Modifier = Modifier,
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
        onDashboard = onDashboard,
        onEditCustomer = onEditCustomer,
        modifier = modifier,
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
    onDashboard: () -> Unit,
    onEditCustomer: (Long) -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.customers_title), maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
            TextButton(onClick = onDashboard) { Text(stringResource(R.string.dashboard_title), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(state.query, vm::onQueryChanged, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.customers_search)) }, singleLine = true)
            FilterChoiceRow(
                label = stringResource(R.string.customers_potential),
                selected = state.potential,
                options = listOf(null, "high", "medium", "low", "unrated"),
                onSelected = vm::onPotentialChanged,
            )
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
private fun FilterChoiceRow(
    label: String,
    selected: String?,
    options: List<String?>,
    onSelected: (String?) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(82.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(options.size) { index ->
                val value = options[index]
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(filterValueLabel(value), maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun filterValueLabel(value: String?): String = when (value) {
    "all" -> stringResource(R.string.status_all)
    "high" -> stringResource(R.string.potential_high)
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
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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

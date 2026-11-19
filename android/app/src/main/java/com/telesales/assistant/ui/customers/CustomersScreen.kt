package com.telesales.assistant.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.telesales.assistant.ui.components.PaginationBar

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CustomersRoute(
    repository: com.telesales.assistant.data.repository.CustomerRepository,
    onSettings: () -> Unit,
    onDashboard: () -> Unit,
    onEditCustomer: (Long) -> Unit,
    onCall: (String) -> Unit,
    onAddCustomer: () -> Unit,
    shouldRefresh: Boolean,
    onRefreshConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    initialPotential: String? = null,
) {
    val vm: CustomersViewModel = viewModel(factory = CustomersViewModelFactory(repository, initialPotential))
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
        onDashboard = onDashboard,
        onEditCustomer = onEditCustomer,
        onCall = onCall,
        onAddCustomer = onAddCustomer,
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomersScreen(
    state: CustomersUiState,
    vm: CustomersViewModel,
    onSettings: () -> Unit,
    onDashboard: () -> Unit,
    onEditCustomer: (Long) -> Unit,
    onCall: (String) -> Unit,
    onAddCustomer: () -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.customers_title), maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
            TextButton(onClick = onDashboard) { Text(stringResource(R.string.customer_form_back), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }, actions = {
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(state.query, vm::onQueryChanged, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.customers_search)) }, singleLine = true)
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
                LoadStateContent(stringResource(R.string.customers_empty), stringResource(R.string.dashboard_add_customer), loading = false, onRetry = onAddCustomer)
            } else {
                TextButton(onClick = vm::refresh) { Text(stringResource(R.string.customers_refresh)) }
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    items(state.customers, key = { it.id }) { customer -> CustomerRow(customer, { onEditCustomer(customer.id) }) {
                        onCall(customer.phone)
                    } }
                }
                if (state.lastPage > 1) {
                    PaginationBar(
                        currentPage = state.currentPage,
                        lastPage = state.lastPage,
                        onPageSelected = vm::goToPage,
                    )
                }
            }
        }

    }
}

@Composable
internal fun CustomerRow(customer: Customer, onClick: () -> Unit, onCall: () -> Unit) {
    val displayName = customer.contactName?.takeIf(String::isNotBlank)
        ?: customer.companyName?.takeIf(String::isNotBlank)
        ?: customer.phone
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text(displayName.take(1).uppercase(), modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                onClick = onCall,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.height(32.dp),
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(
                        stringResource(R.string.detail_call),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    }
}

package com.telesales.assistant.ui.customerdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telesales.assistant.R
import com.telesales.assistant.data.repository.CustomerDetailFailure
import com.telesales.assistant.data.repository.CustomerDetailRepository
import com.telesales.assistant.domain.model.CustomerProfile
import com.telesales.assistant.domain.model.Interaction
import com.telesales.assistant.ui.components.LoadStateContent
import java.time.Clock
import java.time.ZoneId

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CustomerDetailRoute(
    customerId: Long,
    repository: CustomerDetailRepository,
    shouldRefresh: Boolean,
    onRefreshConsumed: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onCall: (String) -> Unit,
    onCallLog: (Long) -> Unit,
    onArchived: (List<Long>) -> Unit,
    clock: Clock = Clock.systemUTC(),
    zoneId: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator? = null,
) {
    val vm: CustomerDetailViewModel = viewModel(factory = CustomerDetailViewModelFactory(customerId, repository, clock, zoneId, reminderCoordinator))
    val state by vm.state.collectAsState()
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) { vm.retry(); onRefreshConsumed() }
    }
    LaunchedEffect(state.isArchived) { if (state.isArchived) onArchived(state.cancelledFollowUpIds) }
    CustomerDetailScreen(state, vm, onBack, onEdit, onSettings, onCall, onCallLog, clock, zoneId)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomerDetailScreen(
    state: CustomerDetailUiState,
    vm: CustomerDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onCall: (String) -> Unit,
    onCallLog: (Long) -> Unit,
    clock: Clock,
    zoneId: ZoneId,
) {
    if (state.archiveConfirmationVisible) AlertDialog(
        onDismissRequest = vm::dismissArchiveConfirmation,
        title = { Text(stringResource(R.string.detail_archive_title)) },
        text = { Text(stringResource(R.string.detail_archive_confirm)) },
        confirmButton = { TextButton(onClick = vm::archive, enabled = !state.isArchiving) { Text(stringResource(R.string.detail_archive)) } },
        dismissButton = { TextButton(onClick = vm::dismissArchiveConfirmation) { Text(stringResource(R.string.customer_form_edit_again)) } },
    )
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.detail_title)) }, navigationIcon = {
            TextButton(onClick = onBack) { Text(stringResource(R.string.customer_form_back)) }
        })
    }) { padding ->
        when {
            state.isLoading -> LoadStateContent(stringResource(R.string.detail_loading), stringResource(R.string.customers_retry), true)
            state.error != null -> {
                val message = failureMessage(state.error)
                Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(message)
                    if (state.error == CustomerDetailFailure.SetupRequired || state.error == CustomerDetailFailure.Unauthorized) {
                        TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_open_settings)) }
                        TextButton(onClick = vm::retry) { Text(stringResource(R.string.customers_retry)) }
                    } else TextButton(onClick = vm::retry) { Text(stringResource(R.string.customers_retry)) }
                }
            }
        state.customer != null -> DetailContent(state, vm, state.customer, onEdit, onCall, onCallLog, clock, zoneId, Modifier.padding(padding))
        }
    }
}

@Composable
private fun DetailContent(
    state: CustomerDetailUiState,
    vm: CustomerDetailViewModel,
    customer: CustomerProfile,
    onEdit: () -> Unit,
    onCall: (String) -> Unit,
    onCallLog: (Long) -> Unit,
    clock: Clock,
    zoneId: ZoneId,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(customer.companyName ?: customer.contactName ?: customer.phone)
                    detailField(R.string.detail_contact, customer.contactName)
                    detailField(R.string.detail_phone, customer.phone)
                    detailField(R.string.detail_address, customer.address)
                    detailField(R.string.detail_industry, customer.industry)
                    detailField(R.string.detail_source, customer.source)
                    Text(stringResource(R.string.detail_interaction_count, customer.interactionCount))
                    Text(stringResource(R.string.customers_last_contact, customer.lastContactedAt?.let(vm::formatTime) ?: stringResource(R.string.customers_not_contacted)))
                    Text(stringResource(R.string.customers_last_result, resultLabel(customer.lastResult)))
                    Text(stringResource(R.string.detail_potential, potentialLabel(customer.potentialLevel)))
                    customer.nextFollowUp?.let { Text(stringResource(R.string.detail_next_follow_up, vm.formatTime(it.remindAt))) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onCall(customer.phone) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.detail_call), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (customer.status != "archived") TextButton(onClick = { onCallLog(customer.id) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.call_log_title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else Spacer(Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.customer_form_edit_title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = vm::showArchiveConfirmation, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.detail_archive), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (state.archiveError != null) {
                        Text(failureMessage(state.archiveError))
                        TextButton(onClick = vm::retryArchive) { Text(stringResource(R.string.customers_retry)) }
                    }
                    if (state.isArchiving) CircularProgressIndicator()
                }
            }
        }
        item { Text(stringResource(R.string.detail_timeline), style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
        when {
            state.isTimelineLoading && state.interactions.isEmpty() -> item { CircularProgressIndicator() }
            state.timelineError != null && state.interactions.isEmpty() -> item {
                Column { Text(failureMessage(state.timelineError)); TextButton(onClick = vm::retryTimeline) { Text(stringResource(R.string.customers_retry)) } }
            }
            state.interactions.isEmpty() -> item { Text(stringResource(R.string.detail_timeline_empty)) }
            else -> items(state.interactions, key = { it.id }) { interaction -> InteractionRow(interaction, vm) }
        }
        if (state.isLoadingMore) item { CircularProgressIndicator() }
        if (state.timelineError != null && state.interactions.isNotEmpty()) item {
            Column { Text(failureMessage(state.timelineError)); TextButton(onClick = vm::retryTimeline) { Text(stringResource(R.string.customers_retry)) } }
        }
    }
    val layout by remember { derivedStateOf { listState.layoutInfo } }
    LaunchedEffect(layout.visibleItemsInfo.lastOrNull()?.index, state.interactions.size, state.hasMore) {
        val lastTimelineIndex = state.interactions.size + 1
        if (state.hasMore && layout.visibleItemsInfo.lastOrNull()?.index == lastTimelineIndex) vm.loadMore()
    }
}

@Composable
private fun detailField(label: Int, value: String?) {
    if (!value.isNullOrBlank()) Text(stringResource(label, value))
}

@Composable
private fun InteractionRow(interaction: Interaction, vm: CustomerDetailViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.detail_interaction_heading, resultLabel(interaction.result), vm.formatTime(interaction.contactedAt)))
            if (interaction.needs.isNotEmpty()) Text(stringResource(R.string.detail_needs, needLabels(interaction.needs)))
            interaction.note?.takeIf(String::isNotBlank)?.let { Text(stringResource(R.string.detail_note, it)) }
        }
    }
}

@Composable
private fun resultLabel(value: String?): String = when (value) {
    "interested" -> stringResource(R.string.result_interested)
    "no_answer" -> stringResource(R.string.result_no_answer)
    "callback" -> stringResource(R.string.result_callback)
    "rejected" -> stringResource(R.string.result_rejected)
    else -> stringResource(R.string.customers_not_contacted)
}

@Composable
private fun potentialLabel(value: String): String = when (value) {
    "high" -> stringResource(R.string.potential_high)
    "medium" -> stringResource(R.string.potential_medium)
    "low" -> stringResource(R.string.potential_low)
    else -> stringResource(R.string.potential_unrated)
}

@Composable
private fun needLabels(values: List<String>): String {
    val labels = buildList {
        if ("website" in values) add(stringResource(R.string.need_website))
        if ("web_app" in values) add(stringResource(R.string.need_web_app))
        if ("software" in values) add(stringResource(R.string.need_software))
        if ("ads" in values) add(stringResource(R.string.need_ads))
        if ("other" in values) add(stringResource(R.string.need_other))
    }
    return labels.joinToString()
}

@Composable
private fun failureMessage(error: CustomerDetailFailure): String = when (error) {
    CustomerDetailFailure.SetupRequired -> stringResource(R.string.detail_setup_required)
    CustomerDetailFailure.Network -> stringResource(R.string.customers_network_error)
    CustomerDetailFailure.Unauthorized -> stringResource(R.string.customers_unauthorized)
    CustomerDetailFailure.NotFound -> stringResource(R.string.customer_form_not_found)
    CustomerDetailFailure.Server -> stringResource(R.string.customers_server_error)
}

package com.telesales.assistant.ui.followups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telesales.assistant.R
import com.telesales.assistant.data.repository.FollowUpRepository
import com.telesales.assistant.domain.model.FollowUp
import com.telesales.assistant.ui.components.FollowUpCard
import com.telesales.assistant.ui.components.rememberPullToRefreshGesture
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FollowUpsRoute(
    repository: FollowUpRepository,
    onSettings: () -> Unit,
    onCall: (String) -> Unit,
    onCustomer: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    clock: Clock = Clock.systemUTC(),
    businessZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator? = null,
) {
    val vm: FollowUpsViewModel = viewModel(factory = FollowUpsFactory(repository, clock, businessZone, reminderCoordinator))
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val conflictMessage = stringResource(R.string.followups_conflict)
    LaunchedEffect(state.notice) {
        if (state.notice == "conflict") {
            snackbar.showSnackbar(conflictMessage)
            vm.dismissNotice()
        }
    }
    FollowUpsScreen(state, vm, snackbar, onSettings, onCall, onCustomer, onBack)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FollowUpsScreen(
    state: FollowUpsUiState,
    vm: FollowUpsViewModel,
    snackbar: SnackbarHostState,
    onSettings: () -> Unit,
    onCall: (String) -> Unit,
    onCustomer: (Long) -> Unit,
    onBack: (() -> Unit)?,
) {
    val listState = rememberLazyListState()
    val showDate = remember { mutableStateOf(false) }
    val showTime = remember { mutableStateOf(false) }
    val pullRefresh = rememberPullToRefreshGesture(listState, state.isRefreshing, vm::refresh)
    if (state.rescheduleForId != null) RescheduleDialog(state, vm, showDate, showTime, onSettings)
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.followups_title)) }, navigationIcon = {
            onBack?.let { TextButton(onClick = it) { Text(stringResource(R.string.customer_form_back)) } }
        }, actions = { TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_settings)) } }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isRefreshing || pullRefresh.isPulling) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxSize().then(pullRefresh.modifier).padding(horizontal = 16.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    state.isLoading && state.items.isEmpty() -> item { CircularProgressIndicator(Modifier.padding(24.dp)) }
                    state.error != null && state.items.isEmpty() -> item {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(errorText(state.error), color = MaterialTheme.colorScheme.error)
                            if (state.error == FollowUpsError.SetupRequired || state.error == FollowUpsError.Unauthorized) {
                                TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_open_settings)) }
                            }
                            TextButton(onClick = vm::retry) { Text(stringResource(R.string.customers_retry)) }
                        }
                    }
                    state.items.isEmpty() -> item { Text(stringResource(R.string.followups_empty), Modifier.padding(24.dp)) }
                    else -> {
                        if (state.error != null) item {
                            Column {
                                Text(errorText(state.error), color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = vm::retry) { Text(stringResource(R.string.customers_retry)) }
                            }
                        }
                        if (state.actionError != null) item {
                            Column {
                                Text(errorText(state.actionError), color = MaterialTheme.colorScheme.error)
                                if (state.actionError == FollowUpsError.SetupRequired || state.actionError == FollowUpsError.Unauthorized) {
                                    TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_open_settings)) }
                                }
                            }
                        }
                        if (state.validationErrors.isNotEmpty()) item {
                            Text(state.validationErrors.values.flatten().joinToString(), color = MaterialTheme.colorScheme.error)
                        }
                        val overdue = state.items.filter { it.group == "overdue" }
                        val today = state.items.filter { it.group == "today" }
                        val upcoming = state.items.filter { it.group == "upcoming" }
                        if (overdue.isNotEmpty()) {
                            item { Text(stringResource(R.string.followups_overdue), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error) }
                            items(overdue, key = { it.id }) { item -> FollowUpRow(item, state, vm, onCall, onCustomer) }
                        }
                        if (today.isNotEmpty()) {
                            item { Text(stringResource(R.string.followups_today), style = MaterialTheme.typography.titleLarge) }
                            items(today, key = { it.id }) { item -> FollowUpRow(item, state, vm, onCall, onCustomer) }
                        }
                        if (upcoming.isNotEmpty()) {
                            item { Text(stringResource(R.string.followups_upcoming), style = MaterialTheme.typography.titleLarge) }
                            items(upcoming, key = { it.id }) { item -> FollowUpRow(item, state, vm, onCall, onCustomer) }
                        }
                        if (state.isLoadingMore) item { CircularProgressIndicator(Modifier.padding(16.dp)) }
                    }
                }
            }
            val layout by remember { derivedStateOf { listState.layoutInfo } }
            LaunchedEffect(layout.visibleItemsInfo.lastOrNull()?.index, state.items.size, state.hasMore, state.isLoadingMore) {
                if (state.hasMore && state.error == null && layout.visibleItemsInfo.lastOrNull()?.index == layout.totalItemsCount - 1) vm.loadMore()
            }
        }
    }
}

@Composable
private fun FollowUpRow(item: FollowUp, state: FollowUpsUiState, vm: FollowUpsViewModel, onCall: (String) -> Unit, onCustomer: (Long) -> Unit) {
    FollowUpCard(item, vm.formatTime(item), { onCall(item.customer.phone) }, { onCustomer(item.customer.id) }, callEnabled = item.id !in state.busyIds) {
        Button(onClick = { vm.complete(item) }, enabled = item.id !in state.busyIds) {
            Text(stringResource(if (item.id in state.busyIds) R.string.followups_processing else R.string.followups_complete))
        }
        TextButton(onClick = { vm.beginReschedule(item) }, enabled = item.id !in state.busyIds) { Text(stringResource(R.string.followups_reschedule)) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RescheduleDialog(state: FollowUpsUiState, vm: FollowUpsViewModel, showDate: androidx.compose.runtime.MutableState<Boolean>, showTime: androidx.compose.runtime.MutableState<Boolean>, onSettings: () -> Unit) {
    if (showDate.value) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.rescheduleDate?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDate.value = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { vm.setRescheduleDate(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()) }
                showDate.value = false
            }) { Text(stringResource(R.string.call_log_ok)) }
        }, dismissButton = { TextButton(onClick = { showDate.value = false }) { Text(stringResource(R.string.call_log_cancel)) } }) { DatePicker(picker) }
    }
    if (showTime.value) {
        val time = state.rescheduleTime ?: LocalTime.of(9, 0)
        val picker = rememberTimePickerState(time.hour, time.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { showTime.value = false }, confirmButton = {
            TextButton(onClick = { vm.setRescheduleTime(LocalTime.of(picker.hour, picker.minute)); showTime.value = false }) { Text(stringResource(R.string.call_log_ok)) }
        }, dismissButton = { TextButton(onClick = { showTime.value = false }) { Text(stringResource(R.string.call_log_cancel)) } }, text = { TimePicker(picker) })
    }
    if (!showDate.value && !showTime.value) AlertDialog(
        onDismissRequest = vm::dismissReschedule,
        title = { Text(stringResource(R.string.followups_reschedule)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showDate.value = true }) { Text(state.rescheduleDate?.let { stringResource(R.string.call_log_date_value, it.toString()) } ?: stringResource(R.string.call_log_select_date)) }
            TextButton(onClick = { showTime.value = true }) { Text(state.rescheduleTime?.toString() ?: stringResource(R.string.call_log_select_time)) }
            state.rescheduleError?.let { Text(stringResource(if (it == "past") R.string.followups_past_time else R.string.followups_schedule_incomplete), color = MaterialTheme.colorScheme.error) }
            state.validationErrors["remind_at"]?.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
            state.actionError?.let { Text(errorText(it), color = MaterialTheme.colorScheme.error) }
            if (state.actionError == FollowUpsError.SetupRequired || state.actionError == FollowUpsError.Unauthorized) {
                TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_open_settings)) }
            }
        } },
        confirmButton = { TextButton(onClick = vm::submitReschedule) { Text(stringResource(R.string.followups_save_schedule)) } },
        dismissButton = { TextButton(onClick = vm::dismissReschedule) { Text(stringResource(R.string.call_log_cancel)) } },
    )
}

@Composable
private fun errorText(error: FollowUpsError): String = when (error) {
    FollowUpsError.SetupRequired -> stringResource(R.string.customers_setup_required)
    FollowUpsError.Network -> stringResource(R.string.customers_network_error)
    FollowUpsError.Unauthorized -> stringResource(R.string.customers_unauthorized)
    FollowUpsError.Server -> stringResource(R.string.customers_server_error)
}

private class FollowUpsFactory(private val repository: FollowUpRepository, private val clock: Clock, private val zone: ZoneId, private val reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FollowUpsViewModel::class.java))
        return modelClass.cast(FollowUpsViewModel(repository, clock, zone, reminderCoordinator))
    }
}

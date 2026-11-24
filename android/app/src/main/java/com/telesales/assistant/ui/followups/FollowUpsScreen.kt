package com.telesales.assistant.ui.followups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telesales.assistant.R
import com.telesales.assistant.data.repository.FollowUpRepository
import com.telesales.assistant.domain.model.FollowUp
import com.telesales.assistant.ui.components.PaginationBar
import com.telesales.assistant.ui.components.rememberPullToRefreshGesture
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FollowUpsRoute(
    repository: FollowUpRepository,
    onSettings: () -> Unit,
    onCall: (String) -> Unit,
    onCallLog: (Long) -> Unit,
    onCustomer: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    clock: Clock = Clock.systemUTC(),
    businessZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator? = null,
    initialGroup: String = "all",
    shouldRefresh: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
) {
    val vm: FollowUpsViewModel = viewModel(factory = FollowUpsViewModelFactory(repository, clock, businessZone, reminderCoordinator, initialGroup))
    val state by vm.state.collectAsState()
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            vm.refresh()
            onRefreshConsumed()
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val conflictMessage = stringResource(R.string.followups_conflict)
    LaunchedEffect(state.notice) {
        if (state.notice == "conflict") {
            snackbar.showSnackbar(conflictMessage)
            vm.dismissNotice()
        }
    }
    FollowUpsScreen(state, vm, snackbar, onSettings, onCall, onCallLog, onCustomer, onBack)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FollowUpsScreen(
    state: FollowUpsUiState,
    vm: FollowUpsViewModel,
    snackbar: SnackbarHostState,
    onSettings: () -> Unit,
    onCall: (String) -> Unit,
    onCallLog: (Long) -> Unit,
    onCustomer: (Long) -> Unit,
    onBack: (() -> Unit)?,
) {
    val listState = rememberLazyListState()
    val showDate = remember { mutableStateOf(false) }
    val showTime = remember { mutableStateOf(false) }
    val pullRefresh = rememberPullToRefreshGesture(listState, state.isRefreshing, vm::refresh)
    if (state.rescheduleForId != null) RescheduleDialog(state, vm, showDate, showTime, onSettings)
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.followups_title), maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
            onBack?.let { TextButton(onClick = it) { Text(stringResource(R.string.customer_form_back)) } }
        }, colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 1000.dp)) {
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
                            items(overdue, key = { it.id }) { item -> FollowUpRow(item, item.id in state.busyIds, vm, onCall, onCallLog, onCustomer) }
                        }
                        if (today.isNotEmpty()) {
                            item { Text(stringResource(R.string.followups_today), style = MaterialTheme.typography.titleLarge) }
                            items(today, key = { it.id }) { item -> FollowUpRow(item, item.id in state.busyIds, vm, onCall, onCallLog, onCustomer) }
                        }
                        if (upcoming.isNotEmpty()) {
                            item { Text(stringResource(R.string.followups_upcoming), style = MaterialTheme.typography.titleLarge) }
                            items(upcoming, key = { it.id }) { item -> FollowUpRow(item, item.id in state.busyIds, vm, onCall, onCallLog, onCustomer) }
                        }
                        if (state.lastPage > 1) {
                            item {
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
        }
        }
    }
}

// Pagination handled by shared PaginationBar in ui/components/PaginationBar.kt

@Composable
internal fun FollowUpRow(item: FollowUp, busy: Boolean, vm: FollowUpsViewModel, onCall: (String) -> Unit, onCallLog: (Long) -> Unit, onCustomer: (Long) -> Unit) {
    val groupColor = when (item.group) {
        "overdue" -> MaterialTheme.colorScheme.errorContainer
        "today" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val groupTextColor = when (item.group) {
        "overdue" -> MaterialTheme.colorScheme.onErrorContainer
        "today" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val formattedTime = remember(item.id, item.remindAt) { vm.formatTime(item) }
    val timeParts = formattedTime.split(" ", limit = 2)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onCustomer(item.customer.id) },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, groupColor.copy(alpha = 0.9f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = groupColor, shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.width(68.dp).padding(horizontal = 5.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(timeParts.firstOrNull().orEmpty(), style = MaterialTheme.typography.labelSmall, color = groupTextColor, maxLines = 1)
                    Text(timeParts.getOrElse(1) { "" }.substringBeforeLast(":"), style = MaterialTheme.typography.titleSmall, color = groupTextColor, maxLines = 1)
                }
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(item.customer.contactName?.takeIf(String::isNotBlank) ?: item.customer.companyName?.takeIf(String::isNotBlank) ?: item.customer.phone, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.customer.companyName?.takeIf(String::isNotBlank)?.let { "$it • ${item.customer.phone}" } ?: item.customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.note?.takeIf(String::isNotBlank)?.let { Text(stringResource(R.string.followups_note, it), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary) }
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun RescheduleDialog(state: FollowUpsUiState, vm: FollowUpsViewModel, showDate: androidx.compose.runtime.MutableState<Boolean>, showTime: androidx.compose.runtime.MutableState<Boolean>, onSettings: () -> Unit) {
    if (showDate.value) {
        val initialDate = state.rescheduleDate ?: LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
        val picker = rememberDatePickerState(initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDate.value = false }, confirmButton = {
            TextButton(onClick = {
                val date = picker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() } ?: initialDate
                vm.setRescheduleDate(date)
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
            TextButton(onClick = { showDate.value = true }) { Text(state.rescheduleDate?.let { stringResource(R.string.call_log_date_value, it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) } ?: stringResource(R.string.call_log_select_date)) }
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

class FollowUpsViewModelFactory(private val repository: FollowUpRepository, private val clock: Clock, private val zone: ZoneId, private val reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator?, private val initialGroup: String = "all") : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FollowUpsViewModel::class.java))
        return modelClass.cast(FollowUpsViewModel(repository, clock, zone, reminderCoordinator, initialGroup))
    }
}

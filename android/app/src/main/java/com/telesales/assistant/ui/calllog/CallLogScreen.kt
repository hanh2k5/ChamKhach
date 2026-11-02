package com.telesales.assistant.ui.calllog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.R
import com.telesales.assistant.data.repository.CallLogRepository
import com.telesales.assistant.domain.model.CustomerProfile
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun CallLogRoute(customerId: Long, repository: CallLogRepository, onBack: () -> Unit, onSaved: () -> Unit, reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator? = null) {
    val vm: CallLogViewModel = viewModel(factory = CallLogFactory(customerId, repository, reminderCoordinator))
    val state by vm.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    CallLogScreen(state, vm, onBack)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CallLogScreen(state: CallLogUiState, vm: CallLogViewModel, onBack: () -> Unit) {
    val showDate = remember { mutableStateOf(false) }
    val showTime = remember { mutableStateOf(false) }
    if (state.missingScheduleDialog) AlertDialog(
        onDismissRequest = vm::dismissScheduleDialog,
        title = { Text(stringResource(R.string.call_log_choose_schedule_title)) },
        text = { Text(stringResource(R.string.call_log_choose_schedule_message)) },
        confirmButton = { TextButton(onClick = { vm.dismissScheduleDialog(); showDate.value = true }) { Text(stringResource(R.string.call_log_choose_schedule)) } },
        dismissButton = { TextButton(onClick = vm::saveWithoutSchedule) { Text(stringResource(R.string.call_log_save_without_schedule)) } },
    )
    if (showDate.value) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.date?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDate.value = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { vm.setDate(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()) }
                showDate.value = false; showTime.value = true
            }) { Text(stringResource(R.string.call_log_ok)) }
        }, dismissButton = { TextButton(onClick = { showDate.value = false }) { Text(stringResource(R.string.call_log_cancel)) } }) { DatePicker(state = picker) }
    }
    if (showTime.value) {
        val initial = state.time ?: LocalTime.of(9, 0)
        val picker = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { showTime.value = false }, confirmButton = {
            TextButton(onClick = { vm.setTime(LocalTime.of(picker.hour, picker.minute)); showTime.value = false }) { Text(stringResource(R.string.call_log_ok)) }
        }, dismissButton = { TextButton(onClick = { showTime.value = false }) { Text(stringResource(R.string.call_log_cancel)) } }, text = { TimePicker(state = picker) })
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.call_log_title)) }, navigationIcon = {
        TextButton(onClick = onBack) { Text(stringResource(R.string.customer_form_back)) }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                state.isLoading -> Text(stringResource(R.string.call_log_loading))
                state.customer != null -> FormContent(state, vm, state.customer, showDate, showTime)
                else -> Text(stringResource(R.string.call_log_load_error))
            }
        }
    }
}

@Composable
private fun FormContent(state: CallLogUiState, vm: CallLogViewModel, customer: CustomerProfile, showDate: androidx.compose.runtime.MutableState<Boolean>, showTime: androidx.compose.runtime.MutableState<Boolean>) {
    Text(customer.companyName ?: customer.contactName ?: customer.phone, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
    Text(customer.phone)
    Text(stringResource(R.string.call_log_result_label), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
    listOf("interested" to R.string.result_interested, "no_answer" to R.string.result_no_answer, "callback" to R.string.result_callback, "rejected" to R.string.result_rejected).forEach { (value, label) ->
        if (state.result == value) Button(onClick = { vm.chooseResult(value) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(label), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        } else OutlinedButton(onClick = { vm.chooseResult(value) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(label), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        }
    }
    if (state.formError == "result") Text(stringResource(R.string.call_log_result_required), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
    Text(stringResource(R.string.call_log_needs_label), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
    listOf("website" to R.string.need_website, "web_app" to R.string.need_web_app, "software" to R.string.need_software, "ads" to R.string.need_ads, "other" to R.string.need_other).forEach { (value, label) ->
        FilterChip(selected = value in state.needs, onClick = { vm.toggleNeed(value) }, label = { Text(stringResource(label)) })
    }
    OutlinedTextField(value = state.note, onValueChange = vm::setNote, label = { Text(stringResource(R.string.call_log_note)) }, supportingText = { Text("${state.note.length}/500") }, modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3)
    Text(stringResource(R.string.call_log_schedule_label), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showDate.value = true }) { Text(state.date?.let { stringResource(R.string.call_log_date_value, it.toString()) } ?: stringResource(R.string.call_log_select_date)) }
        OutlinedButton(onClick = { showTime.value = true }) { Text(state.time?.let { stringResource(R.string.call_log_time_value, it.toString()) } ?: stringResource(R.string.call_log_select_time)) }
        TextButton(onClick = vm::clearSchedule) { Text(stringResource(R.string.call_log_clear_schedule)) }
    }
    if (state.formError == "past_schedule") Text(stringResource(R.string.call_log_schedule_past), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
    if (state.formError == "incomplete_schedule") Text(stringResource(R.string.call_log_schedule_incomplete), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
    state.validationErrors.forEach { (field, messages) -> Text("$field: ${messages.joinToString()}", color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    state.formError?.takeIf { it in setOf("setup", "network", "unauthorized", "server") }?.let { Text(stringResource(when (it) {
        "setup" -> R.string.customers_setup_required
        "network" -> R.string.customers_network_error
        "unauthorized" -> R.string.customers_unauthorized
        else -> R.string.customers_server_error
    }), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    Button(onClick = vm::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(if (state.isSaving) R.string.call_log_saving else R.string.call_log_save)) }
}

private class CallLogFactory(private val id: Long, private val repository: CallLogRepository, private val reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CallLogViewModel::class.java))
        return modelClass.cast(CallLogViewModel(id, repository, Clock.systemUTC(), reminderCoordinator))
    }
}

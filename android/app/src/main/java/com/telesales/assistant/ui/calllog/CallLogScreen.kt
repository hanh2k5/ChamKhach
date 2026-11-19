package com.telesales.assistant.ui.calllog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telesales.assistant.R
import com.telesales.assistant.ui.components.GlassCard
import com.telesales.assistant.data.repository.CallLogRepository
import com.telesales.assistant.domain.model.CustomerProfile
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    if (showDate.value) {
        val pickerDate = state.date ?: vm.currentLocalDate()
        val picker = rememberDatePickerState(initialSelectedDateMillis = pickerDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDate.value = false }, confirmButton = {
            TextButton(onClick = {
                val selectedDateMillis = picker.selectedDateMillis
                vm.setDate(selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() } ?: pickerDate)
                showDate.value = false; showTime.value = true
            }) { Text(stringResource(R.string.call_log_ok)) }
        }, dismissButton = { TextButton(onClick = { showDate.value = false }) { Text(stringResource(R.string.call_log_cancel)) } }) { DatePicker(state = picker) }
    }
    if (showTime.value) {
        val initial = state.time ?: vm.currentLocalTime()
        val picker = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        AlertDialog(onDismissRequest = { showTime.value = false }, confirmButton = {
            TextButton(onClick = { vm.setTime(LocalTime.of(picker.hour, picker.minute)); showTime.value = false }) { Text(stringResource(R.string.call_log_ok)) }
        }, dismissButton = { TextButton(onClick = { showTime.value = false }) { Text(stringResource(R.string.call_log_cancel)) } }, text = { TimePicker(state = picker) })
    }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = { TopAppBar(title = { Text(stringResource(R.string.call_log_title), maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
        TextButton(onClick = onBack) { Text(stringResource(R.string.customer_form_back)) }
    }, colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 720.dp).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                state.isLoading -> Text(stringResource(R.string.call_log_loading))
                state.customer != null -> FormContent(state, vm, state.customer, showDate, showTime)
                else -> Text(stringResource(R.string.call_log_load_error))
            }
        }
        }
    }
}

@Composable
private fun FormContent(state: CallLogUiState, vm: CallLogViewModel, customer: CustomerProfile, showDate: androidx.compose.runtime.MutableState<Boolean>, showTime: androidx.compose.runtime.MutableState<Boolean>) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.label_customer_name_company), style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            Text(customer.companyName ?: customer.contactName ?: customer.phone, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.field_phone_value, customer.phone), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.call_log_note), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = state.note, onValueChange = vm::setNote, label = { Text(stringResource(R.string.call_log_note)) }, supportingText = { Text(stringResource(R.string.call_log_note_counter, state.note.length)) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
        }
    }
    GlassCard(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(stringResource(R.string.call_log_schedule_label), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDate.value = true }, modifier = Modifier.weight(1f)) { Text(state.date?.let { stringResource(R.string.call_log_date_value, it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) } ?: stringResource(R.string.call_log_select_date), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            OutlinedButton(onClick = { showTime.value = true }, modifier = Modifier.weight(1f)) { Text(state.time?.let { stringResource(R.string.call_log_time_value, it.toString()) } ?: stringResource(R.string.call_log_select_time), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        TextButton(onClick = vm::clearSchedule) { Text(stringResource(R.string.call_log_clear_schedule), maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
    }
    }
    if (state.formError == "past_schedule") Text(stringResource(R.string.call_log_schedule_past), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
    if (state.formError == "incomplete_schedule") Text(stringResource(R.string.call_log_schedule_incomplete), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
    state.validationErrors.forEach { (field, messages) -> Text(stringResource(R.string.call_log_validation_error, field, messages.joinToString()), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    state.formError?.takeIf { it in setOf("setup", "network", "unauthorized", "server") }?.let { Text(stringResource(when (it) {
        "setup" -> R.string.customers_setup_required
        "network" -> R.string.customers_network_error
        "unauthorized" -> R.string.customers_unauthorized
        else -> R.string.customers_server_error
    }), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    Button(onClick = vm::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(if (state.isSaving) R.string.call_log_saving else R.string.call_log_save), maxLines = 1) }
}

private class CallLogFactory(private val id: Long, private val repository: CallLogRepository, private val reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CallLogViewModel::class.java))
        return modelClass.cast(CallLogViewModel(id, repository, Clock.systemUTC(), reminderCoordinator))
    }
}

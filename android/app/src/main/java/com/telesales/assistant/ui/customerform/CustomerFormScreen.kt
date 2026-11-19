package com.telesales.assistant.ui.customerform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telesales.assistant.R
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.reminder.ReminderCoordinator
import com.telesales.assistant.ui.components.GlassCard
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CustomerFormRoute(
    repository: CustomerFormRepository,
    customerId: Long?,
    onBack: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onSettings: () -> Unit,
    onSaved: (Long) -> Unit,
    reminderCoordinator: ReminderCoordinator? = null,
    clock: Clock = Clock.systemUTC(),
) {
    val vm: CustomerFormViewModel = viewModel(factory = CustomerFormViewModelFactory(repository, customerId, clock, reminderCoordinator))
    val state by vm.state.collectAsState()
    LaunchedEffect(state.savedCustomerId) { state.savedCustomerId?.let(onSaved) }
    CustomerFormScreen(
        state = state,
        isEditing = customerId != null,
        onBack = onBack,
        onOpenCustomer = onOpenCustomer,
        onSettings = onSettings,
        onCompanyNameChanged = vm::onCompanyNameChanged,
        onContactNameChanged = vm::onContactNameChanged,
        onPhoneChanged = vm::onPhoneChanged,
        onAddressChanged = vm::onAddressChanged,
        onActiveSinceChanged = vm::onActiveSinceChanged,
        onIndustryChanged = vm::onIndustryChanged,
        currentLocalDate = vm::currentLocalDate,
        currentLocalTime = vm::currentLocalTime,
        onInitialNoteChanged = vm::onInitialNoteChanged,
        onPotentialLevelChanged = vm::onPotentialLevelChanged,
        onFollowUpDateChanged = vm::setFollowUpDate,
        onFollowUpTimeChanged = vm::setFollowUpTime,
        onClearFollowUp = vm::clearFollowUp,
        onNeedToggled = vm::toggleNeed,
        onSave = vm::save,
        onRetry = vm::retryLoad,
        onDismissDuplicate = vm::dismissDuplicate,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomerFormScreen(
    state: CustomerFormUiState,
    isEditing: Boolean,
    onBack: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onSettings: () -> Unit,
    onCompanyNameChanged: (String) -> Unit,
    onContactNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onActiveSinceChanged: (String) -> Unit,
    onIndustryChanged: (String) -> Unit,
    currentLocalDate: () -> LocalDate,
    currentLocalTime: () -> LocalTime,
    onInitialNoteChanged: (String) -> Unit,
    onPotentialLevelChanged: (String) -> Unit,
    onFollowUpDateChanged: (LocalDate?) -> Unit,
    onFollowUpTimeChanged: (LocalTime?) -> Unit,
    onClearFollowUp: () -> Unit,
    onNeedToggled: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDismissDuplicate: () -> Unit,
) {
    val showDate = remember { mutableStateOf(false) }
    val showTime = remember { mutableStateOf(false) }
    val phoneFocusRequester = remember { FocusRequester() }
    val contactNameFocusRequester = remember { FocusRequester() }
    val companyNameFocusRequester = remember { FocusRequester() }
    val addressFocusRequester = remember { FocusRequester() }
    val activeSinceFocusRequester = remember { FocusRequester() }
    val industryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.fieldErrors) {
        if (state.fieldErrors.isNotEmpty()) {
            when {
                "phone" in state.fieldErrors -> phoneFocusRequester.requestFocus()
                "contact_name" in state.fieldErrors -> contactNameFocusRequester.requestFocus()
                "company_name" in state.fieldErrors -> companyNameFocusRequester.requestFocus()
                "address" in state.fieldErrors -> addressFocusRequester.requestFocus()
                "active_since" in state.fieldErrors -> activeSinceFocusRequester.requestFocus()
                "industry" in state.fieldErrors -> industryFocusRequester.requestFocus()
            }
        }
    }
    if (showDate.value) {
        val initialDate = state.followUpDate ?: currentLocalDate()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate.value = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = picker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() } ?: initialDate
                    onFollowUpDateChanged(date)
                    showDate.value = false
                    showTime.value = true
                }) { Text(stringResource(R.string.call_log_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate.value = false }) { Text(stringResource(R.string.call_log_cancel)) } },
        ) { DatePicker(state = picker) }
    }
    if (showTime.value) {
        val initial = state.followUpTime ?: currentLocalTime()
        val picker = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime.value = false },
            confirmButton = { TextButton(onClick = { onFollowUpTimeChanged(LocalTime.of(picker.hour, picker.minute)); showTime.value = false }) { Text(stringResource(R.string.call_log_ok)) } },
            dismissButton = { TextButton(onClick = { showTime.value = false }) { Text(stringResource(R.string.call_log_cancel)) } },
            text = { TimePicker(state = picker) },
        )
    }
    if (state.duplicateCustomerId != null) {
        AlertDialog(
            onDismissRequest = onDismissDuplicate,
            title = { Text(stringResource(R.string.customer_form_duplicate_title)) },
            text = { Text(stringResource(R.string.customer_form_duplicate_message)) },
            confirmButton = {
                TextButton(onClick = { onOpenCustomer(state.duplicateCustomerId) }) {
                    Text(stringResource(R.string.customer_form_open_existing))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDuplicate) { Text(stringResource(R.string.customer_form_edit_again)) }
            },
        )
    }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = {
        TopAppBar(
            title = { Text(stringResource(if (isEditing) R.string.customer_form_edit_title else R.string.customer_form_add_title)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.customer_form_back)) } },
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.padding(24.dp))
                state.isLoadFailure && state.error != null -> FormError(state.error, onRetry, onSettings, Modifier.fillMaxWidth().widthIn(max = 640.dp).padding(16.dp))
                else -> Column(
                Modifier.fillMaxWidth().widthIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.error != null) SaveError(state.error, onSave, onSettings)
                if (state.initialInteractionPending) Text(stringResource(R.string.customer_form_initial_call_pending), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.label_customer_name_company), style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                Field(R.string.label_phone, state.phone, "phone", state.fieldErrors, onPhoneChanged, enabled = !state.initialInteractionPending, focusRequester = phoneFocusRequester)
                Field(R.string.label_contact_name, state.contactName, "contact_name", state.fieldErrors, onContactNameChanged, enabled = !state.initialInteractionPending, focusRequester = contactNameFocusRequester)
                Field(R.string.label_company_name, state.companyName, "company_name", state.fieldErrors, onCompanyNameChanged, enabled = !state.initialInteractionPending, focusRequester = companyNameFocusRequester)
                Field(R.string.label_address, state.address, "address", state.fieldErrors, onAddressChanged, enabled = !state.initialInteractionPending, focusRequester = addressFocusRequester)
                Field(R.string.label_active_since, state.activeSince, "active_since", state.fieldErrors, onActiveSinceChanged, enabled = !state.initialInteractionPending, focusRequester = activeSinceFocusRequester)
                Field(R.string.label_industry, state.industry, "industry", state.fieldErrors, onIndustryChanged, enabled = !state.initialInteractionPending, focusRequester = industryFocusRequester)
                }
                }
                if (!isEditing) {
                    OutlinedTextField(
                        value = state.initialNote,
                        onValueChange = onInitialNoteChanged,
                        label = { Text(stringResource(R.string.call_log_note)) },
                        supportingText = { Text(stringResource(R.string.call_log_note_counter, state.initialNote.length)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        maxLines = 3,
                        enabled = !state.initialInteractionPending,
                    )
                }
                Text(stringResource(R.string.label_needs), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("website", "software", "ads")) { need ->
                        val label = when (need) { "website" -> R.string.need_website; "software" -> R.string.need_software; else -> R.string.need_ads }
                        androidx.compose.material3.FilterChip(selected = need in state.needs, onClick = { onNeedToggled(need) }, enabled = !state.initialInteractionPending, label = { Text(stringResource(label), maxLines = 1) }, colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary, selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary))
                    }
                }
                if (!isEditing) {
                    Text(stringResource(R.string.call_log_schedule_label), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showDate.value = true }, modifier = Modifier.weight(1f), enabled = !state.isSaving) {
                            Text(state.followUpDate?.let { stringResource(R.string.call_log_date_value, it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) } ?: stringResource(R.string.call_log_select_date), maxLines = 1)
                        }
                        OutlinedButton(onClick = { showTime.value = true }, modifier = Modifier.weight(1f), enabled = !state.isSaving) {
                            Text(state.followUpTime?.let { stringResource(R.string.call_log_time_value, it.toString()) } ?: stringResource(R.string.call_log_select_time), maxLines = 1)
                        }
                    }
                    if (state.followUpDate != null || state.followUpTime != null) {
                        TextButton(onClick = onClearFollowUp, enabled = !state.isSaving) { Text(stringResource(R.string.call_log_clear_schedule)) }
                    }
                    when (state.formError) {
                        "past_schedule" -> Text(stringResource(R.string.call_log_schedule_past), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        "incomplete_schedule" -> Text(stringResource(R.string.call_log_schedule_incomplete), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                }
                if (isEditing || !state.initialInteractionPending) {
                    Text(stringResource(R.string.customer_form_potential), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("medium" to R.string.potential_medium, "high" to R.string.potential_high)) { (level, label) ->
                            androidx.compose.material3.FilterChip(
                                selected = state.potentialLevel == level,
                                onClick = { onPotentialLevelChanged(level) },
                                enabled = !state.initialInteractionPending,
                                label = { Text(stringResource(label), maxLines = 1) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                }
                Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    if (state.isSaving) CircularProgressIndicator()
                    else Text(stringResource(if (state.initialInteractionPending) R.string.customer_form_retry_initial_call else R.string.customer_form_save))
                }
            }
            }
        }
    }
}

@Composable
private fun Field(
    label: Int,
    value: String,
    key: String,
    errors: Map<String, List<String>>,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    val hasError = !errors[key].isNullOrEmpty()
    var textFieldValue by remember(value) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = if (hasError) TextRange(0, value.length) else TextRange(value.length),
            ),
        )
    }

    LaunchedEffect(hasError) {
        if (hasError) {
            textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length))
        }
    }

    val modifier = Modifier
        .fillMaxWidth()
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            if (newValue.text != value) {
                onValueChange(newValue.text)
            }
        },
        enabled = enabled,
        modifier = modifier,
        label = { Text(stringResource(label)) },
        isError = hasError,
        supportingText = if (hasError || key == "phone") ({
            Column {
                if (key == "phone") Text(stringResource(R.string.customer_form_required))
                if (hasError) Text(stringResource(R.string.customer_form_field_error))
            }
        }) else null,
        singleLine = key != "address",
    )
}

@Composable
private fun FormError(error: CustomerFormError, onRetry: () -> Unit, onSettings: () -> Unit, modifier: Modifier) {
    val message = when (error) {
        CustomerFormError.SetupRequired -> stringResource(R.string.customer_form_setup_required)
        CustomerFormError.Network -> stringResource(R.string.customer_form_network_error)
        CustomerFormError.Unauthorized -> stringResource(R.string.customer_form_unauthorized)
        CustomerFormError.NotFound -> stringResource(R.string.customer_form_not_found)
        CustomerFormError.Server -> stringResource(R.string.customer_form_server_error)
    }
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message)
        if (error == CustomerFormError.SetupRequired || error == CustomerFormError.Unauthorized) {
            TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_open_settings)) }
        } else TextButton(onClick = onRetry) { Text(stringResource(R.string.customers_retry)) }
    }
}

@Composable
private fun SaveError(error: CustomerFormError, onRetry: () -> Unit, onSettings: () -> Unit) {
    val message = when (error) {
        CustomerFormError.SetupRequired -> stringResource(R.string.customer_form_setup_required)
        CustomerFormError.Network -> stringResource(R.string.customer_form_network_error)
        CustomerFormError.Unauthorized -> stringResource(R.string.customer_form_unauthorized)
        CustomerFormError.NotFound -> stringResource(R.string.customer_form_not_found)
        CustomerFormError.Server -> stringResource(R.string.customer_form_server_error)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(message)
        if (error == CustomerFormError.SetupRequired || error == CustomerFormError.Unauthorized) {
            TextButton(onClick = onSettings) { Text(stringResource(R.string.customers_open_settings)) }
        } else TextButton(onClick = onRetry) { Text(stringResource(R.string.customers_retry)) }
    }
}

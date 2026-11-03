package com.telesales.assistant.ui.customerform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CustomerFormRoute(
    repository: CustomerFormRepository,
    customerId: Long?,
    onBack: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onSettings: () -> Unit,
    onSaved: () -> Unit,
) {
    val vm: CustomerFormViewModel = viewModel(factory = CustomerFormViewModelFactory(repository, customerId))
    val state by vm.state.collectAsState()
    LaunchedEffect(state.savedCustomerId) { if (state.savedCustomerId != null) onSaved() }
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
        onIndustryChanged = vm::onIndustryChanged,
        onSourceChanged = vm::onSourceChanged,
        onPotentialLevelChanged = vm::onPotentialLevelChanged,
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
    onIndustryChanged: (String) -> Unit,
    onSourceChanged: (String) -> Unit,
    onPotentialLevelChanged: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDismissDuplicate: () -> Unit,
) {
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
                Field(R.string.customer_form_company, state.companyName, "company_name", state.fieldErrors, onCompanyNameChanged)
                Field(R.string.customer_form_contact, state.contactName, "contact_name", state.fieldErrors, onContactNameChanged)
                Field(R.string.customer_form_phone, state.phone, "phone", state.fieldErrors, onPhoneChanged)
                Field(R.string.customer_form_address, state.address, "address", state.fieldErrors, onAddressChanged)
                Field(R.string.customer_form_industry, state.industry, "industry", state.fieldErrors, onIndustryChanged)
                Field(R.string.customer_form_source, state.source, "source", state.fieldErrors, onSourceChanged)
                EnumMenu(R.string.customer_form_potential, state.potentialLevel, listOf("high", "medium", "low", "unrated"), onPotentialLevelChanged)
                Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    if (state.isSaving) CircularProgressIndicator() else Text(stringResource(R.string.customer_form_save))
                }
            }
            }
        }
    }
}

@Composable
private fun Field(label: Int, value: String, key: String, errors: Map<String, List<String>>, onValueChange: (String) -> Unit) {
    val hasError = !errors[key].isNullOrEmpty()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        isError = hasError,
        supportingText = if (hasError) ({ Text(stringResource(R.string.customer_form_field_error)) }) else null,
        singleLine = key != "address",
    )
}

@Composable
private fun EnumMenu(label: Int, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.customer_form_enum_value, stringResource(label), enumLabel(value))) }
    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option -> DropdownMenuItem(
            text = { Text(enumLabel(option)) },
            onClick = { expanded = false; onSelect(option) },
        ) }
    }
}

@Composable
private fun enumLabel(value: String): String = when (value) {
    "high" -> stringResource(R.string.potential_high)
    "low" -> stringResource(R.string.potential_low)
    "unrated" -> stringResource(R.string.potential_unrated)
    else -> stringResource(R.string.potential_unrated)
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

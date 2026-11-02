package com.telesales.assistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telesales.assistant.R
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.SettingsStore

@Composable
fun SettingsRoute(settingsStore: SettingsStore, apiServiceFactory: ApiServiceProvider, onBack: (() -> Unit)? = null) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(settingsStore, apiServiceFactory),
    )
    val state by settingsViewModel.state.collectAsState()
    SettingsScreen(
        state = state,
        onBaseUrlChanged = settingsViewModel::onBaseUrlChanged,
        onTokenChanged = settingsViewModel::onTokenChanged,
        onSave = settingsViewModel::save,
        onCheckConnection = settingsViewModel::checkConnection,
        onBack = onBack,
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onBaseUrlChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCheckConnection: () -> Unit,
    onBack: (() -> Unit)?,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onBack != null) {
                androidx.compose.material3.TextButton(onClick = onBack) {
                    Text(stringResource(R.string.settings_back))
                }
            }
            Text(text = stringResource(R.string.settings_title))
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onBaseUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isLoaded && !state.isChecking,
                label = { Text(stringResource(R.string.settings_base_url)) },
                placeholder = { Text(stringResource(R.string.settings_base_url_hint)) },
                isError = state.baseUrlInvalid,
                supportingText = if (state.baseUrlInvalid) {
                    { Text(stringResource(R.string.settings_base_url_error)) }
                } else null,
                singleLine = true,
            )
            OutlinedTextField(
                value = state.token,
                onValueChange = onTokenChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isLoaded && !state.isChecking,
                label = { Text(stringResource(R.string.settings_token)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Button(
                onClick = onSave,
                enabled = state.isLoaded && !state.isChecking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_save))
            }
            Button(
                onClick = onCheckConnection,
                enabled = state.isLoaded && !state.isChecking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_check_connection))
            }
            if (state.isChecking) {
                CircularProgressIndicator()
            }
            StatusText(state.status)
        }
    }
}

@Composable
private fun StatusText(status: SettingsStatus) {
    val message = when (status) {
        SettingsStatus.None -> null
        SettingsStatus.Saved -> stringResource(R.string.settings_saved)
        SettingsStatus.Checking -> stringResource(R.string.settings_checking)
        SettingsStatus.Connected -> stringResource(R.string.settings_connected)
        SettingsStatus.InvalidUrl -> stringResource(R.string.settings_base_url_error)
        SettingsStatus.EndpointNotFound -> stringResource(R.string.settings_endpoint_not_found)
        SettingsStatus.MissingToken -> stringResource(R.string.settings_token_required)
        SettingsStatus.Unauthorized -> stringResource(R.string.settings_invalid_token)
        SettingsStatus.NetworkError -> stringResource(R.string.settings_network_error)
        SettingsStatus.ServerError -> stringResource(R.string.settings_server_error)
        SettingsStatus.StorageError -> stringResource(R.string.settings_storage_error)
    }
    message?.let { Text(text = it) }
}

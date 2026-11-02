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
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telesales.assistant.R
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.SettingsStore

@Composable
fun SettingsRoute(settingsStore: SettingsStore, apiServiceFactory: ApiServiceProvider, onBack: (() -> Unit)? = null, permissionStateProvider: ReminderPermissionStateProvider? = null, reminderCoordinator: com.telesales.assistant.reminder.ReminderCoordinator? = null, debugBuild: Boolean = false) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(settingsStore, apiServiceFactory, permissionStateProvider, reminderCoordinator, debugBuild),
    )
    val state by settingsViewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, settingsViewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) settingsViewModel.refreshReminderPermissions() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    SettingsScreen(
        state = state,
        onBaseUrlChanged = settingsViewModel::onBaseUrlChanged,
        onTokenChanged = settingsViewModel::onTokenChanged,
        onSave = settingsViewModel::save,
        onCheckConnection = settingsViewModel::checkConnection,
        onBack = onBack,
        onDebugReminder = settingsViewModel::scheduleDebugReminder,
        debugBuild = debugBuild,
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
    onDebugReminder: () -> Unit,
    debugBuild: Boolean,
) {
    val context = LocalContext.current
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
            Text(text = stringResource(R.string.reminder_settings_title))
            Text(text = stringResource(if (state.notificationsGranted) R.string.reminder_notifications_enabled else R.string.reminder_notifications_disabled))
            Text(text = stringResource(if (state.exactAlarmsGranted) R.string.reminder_exact_enabled else R.string.reminder_exact_disabled))
            if (!state.notificationsGranted) Text(stringResource(R.string.reminder_notifications_explanation), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            if (!state.exactAlarmsGranted) Text(stringResource(R.string.reminder_exact_explanation), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reminder_open_exact_settings)) }
            }
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reminder_open_notification_settings)) }
            if (debugBuild) Button(onClick = onDebugReminder, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reminder_debug_schedule))
            }
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

package com.telesales.assistant.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import com.telesales.assistant.ui.components.GlassCard

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
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (onBack != null) {
                androidx.compose.material3.TextButton(
                    onClick = onBack,
                    modifier = Modifier.offset(x = (-8).dp),
                ) {
                    Text(stringResource(R.string.settings_back))
                }
            }
            Text(text = stringResource(R.string.settings_title), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = stringResource(R.string.reminder_settings_title), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.weight(1f),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.run { if (state.notificationsGranted) secondaryContainer else errorContainer },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        ) {
                            Text(text = stringResource(if (state.notificationsGranted) R.string.settings_notification_status_on else R.string.settings_notification_status_off), modifier = Modifier.padding(9.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        }
                        androidx.compose.material3.Surface(
                            modifier = Modifier.weight(1f),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.run { if (state.exactAlarmsGranted) secondaryContainer else errorContainer },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        ) {
                            Text(text = stringResource(if (state.exactAlarmsGranted) R.string.settings_alarm_status_on else R.string.settings_alarm_status_off), modifier = Modifier.padding(9.dp), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                        }
                    }
                    if (!state.notificationsGranted) Text(stringResource(R.string.reminder_notifications_explanation), color = androidx.compose.material3.MaterialTheme.colorScheme.error, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    if (!state.exactAlarmsGranted) Text(stringResource(R.string.reminder_exact_explanation), color = androidx.compose.material3.MaterialTheme.colorScheme.error, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Button(onClick = {
                                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                            }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_open_notifications), maxLines = 1, softWrap = false, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            Button(onClick = {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                            }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_open_alarm), maxLines = 1, softWrap = false, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }

                    }

                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.settings_title), style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSave, enabled = state.isLoaded && !state.isChecking, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_save), maxLines = 1, softWrap = false)
                        }
                        Button(onClick = onCheckConnection, enabled = state.isLoaded && !state.isChecking, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_check_short), maxLines = 1, softWrap = false)
                        }
                    }
                    if (state.isChecking) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            CircularProgressIndicator()
                        }
                    }
                    StatusText(state.status)
                }
            }
        }
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
    message?.let {
        Text(
            text = it,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}

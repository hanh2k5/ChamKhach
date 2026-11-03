package com.telesales.assistant.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.RowScope
import com.telesales.assistant.R
import com.telesales.assistant.domain.model.FollowUp

@Composable
fun FollowUpCard(
    item: FollowUp,
    formattedTime: String,
    onCall: () -> Unit,
    onCustomer: () -> Unit,
    callEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val customerLabel = item.customer.companyName ?: item.customer.contactName ?: item.customer.phone
    GlassCard(Modifier.fillMaxWidth().clickable(onClick = onCustomer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (item.group == "overdue") {
                Text(stringResource(R.string.followups_overdue_badge), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
            Text(customerLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(item.customer.phone)
            Text(stringResource(R.string.followups_remind_at, formattedTime))
            item.note?.takeIf(String::isNotBlank)?.let { Text(stringResource(R.string.followups_note, it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCall, enabled = callEnabled) { Text(stringResource(R.string.detail_call)) }
                actions()
            }
        }
    }
}

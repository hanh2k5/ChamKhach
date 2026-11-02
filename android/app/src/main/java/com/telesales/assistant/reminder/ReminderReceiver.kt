package com.telesales.assistant.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.telesales.assistant.TelesalesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AndroidReminderScheduler.EXTRA_FOLLOW_UP_ID, Long.MIN_VALUE)
        if (id == Long.MIN_VALUE) return
        val pendingResult = goAsync()
        val app = context.applicationContext as TelesalesApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = app.container.reminderStore.get(id)
                if (reminder != null && !reminder.notified) {
                    NotificationHelper(app).show(reminder)
                    app.container.reminderStore.markNotified(id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

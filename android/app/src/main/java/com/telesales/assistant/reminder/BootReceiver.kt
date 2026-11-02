package com.telesales.assistant.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.telesales.assistant.TelesalesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val app = context.applicationContext as TelesalesApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.reminderCoordinator.recoverAfterBoot()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

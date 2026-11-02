package com.telesales.assistant.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.telesales.assistant.data.local.ReminderEntity

class AndroidExactAlarmPermission(private val alarmManager: AlarmManager) : ExactAlarmPermission {
    override fun canScheduleExactAlarms(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
}

enum class AlarmPrecision { Exact, Inexact }

fun alarmPrecision(permission: ExactAlarmPermission): AlarmPrecision =
    if (permission.canScheduleExactAlarms()) AlarmPrecision.Exact else AlarmPrecision.Inexact

class AndroidReminderScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val exactAlarmPermission: ExactAlarmPermission,
) : ReminderScheduler {
    override fun schedule(reminder: ReminderEntity) {
        val pendingIntent = requireNotNull(pendingIntent(reminder.followUpId, PendingIntent.FLAG_UPDATE_CURRENT))
        if (alarmPrecision(exactAlarmPermission) == AlarmPrecision.Exact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.remindAtEpochMs, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.remindAtEpochMs, pendingIntent)
        }
    }

    override fun cancel(followUpId: Long) {
        val pendingIntent = pendingIntent(followUpId, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun pendingIntent(id: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("telesales://reminder/$id")
            putExtra(EXTRA_FOLLOW_UP_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_REMINDER = "com.telesales.assistant.REMINDER"
        const val EXTRA_FOLLOW_UP_ID = "follow_up_id"
    }
}

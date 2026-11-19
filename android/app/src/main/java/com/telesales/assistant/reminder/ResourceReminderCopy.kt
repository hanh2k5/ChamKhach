package com.telesales.assistant.reminder

import android.content.Context
import com.telesales.assistant.R
import com.telesales.assistant.data.local.ReminderEntity
import java.time.Instant
import java.time.ZoneId

class ResourceReminderCopy(context: Context) : ReminderCopy {
    private val resources = context.applicationContext.resources
    override fun notificationTitle(customerName: String): String = resources.getString(R.string.reminder_notification_title, customerName)

    override fun notificationBody(remindAt: Instant, note: String?, zoneId: ZoneId): String {
        return resources.getString(R.string.reminder_note_format, note?.takeIf(String::isNotBlank) ?: resources.getString(R.string.reminder_no_note))
    }

    override fun debugReminder(atEpochMs: Long): ReminderEntity = ReminderEntity(
        followUpId = 0,
        customerId = 1,
        title = resources.getString(R.string.reminder_debug_title),
        body = resources.getString(R.string.reminder_debug_body),
        remindAtEpochMs = atEpochMs,
    )

    override fun missedBody(body: String): String = resources.getString(R.string.reminder_missed_format, body)
}

package com.telesales.assistant.reminder

import android.content.Context
import com.telesales.assistant.R
import com.telesales.assistant.data.local.ReminderEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ResourceReminderCopy(context: Context) : ReminderCopy {
    private val resources = context.applicationContext.resources
    override fun notificationTitle(customerName: String): String = resources.getString(R.string.reminder_notification_title, customerName)

    override fun notificationBody(remindAt: Instant, note: String?, zoneId: ZoneId): String {
        val time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(zoneId).format(remindAt)
        val base = resources.getString(R.string.reminder_time_format, time)
        return note?.takeIf(String::isNotBlank)?.let { resources.getString(R.string.reminder_note_format, base, it) } ?: base
    }

    override fun debugReminder(atEpochMs: Long): ReminderEntity = ReminderEntity(
        followUpId = 0,
        customerId = 0,
        title = resources.getString(R.string.reminder_debug_title),
        body = resources.getString(R.string.reminder_debug_body),
        remindAtEpochMs = atEpochMs,
    )
}

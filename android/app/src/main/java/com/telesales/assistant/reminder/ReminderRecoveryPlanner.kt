package com.telesales.assistant.reminder

import com.telesales.assistant.data.local.ReminderEntity
import java.time.Clock

sealed interface ReminderRecoveryAction {
    data class Schedule(val reminder: ReminderEntity) : ReminderRecoveryAction
    data class NotifyMissed(val reminder: ReminderEntity) : ReminderRecoveryAction
}

object ReminderRecoveryPlanner {
    fun plan(reminders: List<ReminderEntity>, clock: Clock): List<ReminderRecoveryAction> =
        reminders.filterNot(ReminderEntity::notified).map { reminder ->
            if (reminder.remindAtEpochMs > clock.millis()) ReminderRecoveryAction.Schedule(reminder)
            else ReminderRecoveryAction.NotifyMissed(reminder)
        }
}

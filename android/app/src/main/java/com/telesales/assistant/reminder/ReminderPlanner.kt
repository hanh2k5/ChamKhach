package com.telesales.assistant.reminder

import com.telesales.assistant.data.local.ReminderEntity
import java.time.Clock
import java.time.Instant

sealed interface ReminderAction {
    data class Schedule(val reminder: ReminderEntity) : ReminderAction
    data class Cancel(val followUpId: Long) : ReminderAction
}

object ReminderPlanner {
    fun interaction(reminder: ReminderEntity?, completedIds: List<Long>): List<ReminderAction> =
        buildList {
            reminder?.let { add(ReminderAction.Schedule(it)) }
            completedIds.forEach { add(ReminderAction.Cancel(it)) }
        }

    fun completeOrCancel(id: Long) = listOf(ReminderAction.Cancel(id))

    fun archive(cancelledIds: List<Long>) = cancelledIds.map(ReminderAction::Cancel)

    fun synchronize(
        existing: List<ReminderEntity>,
        pending: List<ReminderEntity>,
        clock: Clock,
    ): List<ReminderAction> {
        val pendingById = pending.associateBy(ReminderEntity::followUpId)
        val actions = mutableListOf<ReminderAction>()
        existing.filter { it.followUpId !in pendingById }.forEach { actions += ReminderAction.Cancel(it.followUpId) }
        pending.forEach { next ->
            val previous = existing.firstOrNull { it.followUpId == next.followUpId }
            if (next.remindAtEpochMs <= clock.millis()) {
                if (previous != null) actions += ReminderAction.Cancel(next.followUpId)
            } else if (previous == null || previous.remindAtEpochMs != next.remindAtEpochMs) {
                actions += ReminderAction.Schedule(next.copy(notified = false))
            }
        }
        return actions
    }
}

package com.telesales.assistant.reminder

import com.telesales.assistant.data.local.ReminderEntity
import com.telesales.assistant.data.repository.FollowUpListResult
import com.telesales.assistant.data.repository.FollowUpRepository
import com.telesales.assistant.domain.model.FollowUp
import java.time.Clock
import java.time.ZoneId

interface ReminderCopy {
    fun notificationTitle(customerName: String): String
    fun notificationBody(remindAt: java.time.Instant, note: String?, zoneId: ZoneId): String
    fun debugReminder(atEpochMs: Long): ReminderEntity
}

class ReminderCoordinator(
    private val store: ReminderStore,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
    private val businessZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    private val copy: ReminderCopy,
) {
    suspend fun apply(actions: List<ReminderAction>) {
        actions.forEach { action ->
            when (action) {
                is ReminderAction.Schedule -> {
                    store.upsert(action.reminder)
                    scheduler.schedule(action.reminder)
                }
                is ReminderAction.Cancel -> {
                    scheduler.cancel(action.followUpId)
                    store.delete(action.followUpId)
                }
            }
        }
    }

    suspend fun sync(repository: FollowUpRepository) {
        val pending = mutableListOf<FollowUp>()
        var page = 1
        while (true) {
            val result = repository.getFollowUps(page)
            if (result !is FollowUpListResult.Success) return
            pending += result.page.items
            if (result.page.currentPage >= result.page.lastPage) break
            page++
        }
        val reminders = pending.map(::toReminder)
        apply(ReminderPlanner.synchronize(store.getAll(), reminders, clock))
    }

    fun toReminder(followUp: FollowUp): ReminderEntity {
        val name = followUp.customer.contactName?.takeIf(String::isNotBlank)
            ?: followUp.customer.companyName?.takeIf(String::isNotBlank)
            ?: followUp.customer.phone
        return ReminderEntity(
            followUpId = followUp.id,
            customerId = followUp.customer.id,
            title = copy.notificationTitle(name),
            body = copy.notificationBody(followUp.remindAt, followUp.note, businessZone),
            remindAtEpochMs = followUp.remindAt.toEpochMilli(),
        )
    }

    fun createReminder(followUpId: Long, customerId: Long, customerName: String, remindAt: java.time.Instant, note: String?): ReminderEntity =
        ReminderEntity(
            followUpId = followUpId,
            customerId = customerId,
            title = copy.notificationTitle(customerName),
            body = copy.notificationBody(remindAt, note, businessZone),
            remindAtEpochMs = remindAt.toEpochMilli(),
        )

    fun createDebugReminder(id: Long): ReminderEntity = copy.debugReminder(clock.millis() + 30_000).copy(followUpId = id)
}

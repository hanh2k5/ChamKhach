package com.telesales.assistant.reminder

import com.telesales.assistant.data.local.ReminderEntity
import com.telesales.assistant.data.repository.FollowUpListResult
import com.telesales.assistant.data.repository.FollowUpRepository
import com.telesales.assistant.domain.model.FollowUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.ZoneId

interface ReminderCopy {
    fun notificationTitle(customerName: String): String
    fun notificationBody(remindAt: java.time.Instant, note: String?, zoneId: ZoneId): String
    fun debugReminder(atEpochMs: Long): ReminderEntity
    fun missedBody(body: String): String
}

fun interface ReminderNotifier {
    fun show(reminder: ReminderEntity)
}

class ReminderCoordinator(
    private val store: ReminderStore,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
    private val businessZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh"),
    private val copy: ReminderCopy,
    private val notifier: ReminderNotifier,
) {
    suspend fun apply(actions: List<ReminderAction>) = withContext(Dispatchers.IO) {
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

    suspend fun sync(repository: FollowUpRepository) = withContext(Dispatchers.IO) {
        recoverAfterBoot()
        val pending = mutableListOf<FollowUp>()
        var page = 1
        while (page <= MAX_SYNC_PAGES) {
            val result = repository.getFollowUps(page)
            if (result !is FollowUpListResult.Success) return@withContext
            pending += result.page.items
            if (result.page.currentPage >= result.page.lastPage) break
            page++
        }
        val reminders = pending.map(::toReminder)
        apply(ReminderPlanner.synchronize(store.getAll(), reminders, clock))
    }

    suspend fun recoverAfterBoot() = withContext(Dispatchers.IO) {
        val actions = ReminderRecoveryPlanner.plan(store.getAll(), clock)
        val scheduleActions = actions.filterIsInstance<ReminderRecoveryAction.Schedule>()
            .sortedBy { it.reminder.remindAtEpochMs }
            .take(MAX_SCHEDULED_ALARMS)
        val missedActions = actions.filterIsInstance<ReminderRecoveryAction.NotifyMissed>().take(MAX_MISSED_NOTIFICATIONS)

        scheduleActions.forEach { scheduler.schedule(it.reminder) }
        missedActions.forEach { action ->
            if (store.claimNotification(action.reminder.followUpId)) {
                notifier.show(action.reminder.copy(body = copy.missedBody(action.reminder.body)))
            }
        }
    }

    fun toReminder(followUp: FollowUp): ReminderEntity {
        val name = followUp.customer.contactName?.takeIf(String::isNotBlank)
            ?: followUp.customer.companyName?.takeIf(String::isNotBlank)
            ?: followUp.customer.phone
        return ReminderEntity(
            followUpId = followUp.id,
            customerId = followUp.customer.id,
            title = copy.notificationTitle("$name - ${followUp.customer.phone}"),
            body = copy.notificationBody(followUp.remindAt, followUp.note, businessZone),
            remindAtEpochMs = followUp.remindAt.toEpochMilli(),
        )
    }

    fun createReminder(followUpId: Long, customerId: Long, customerName: String, customerPhone: String, remindAt: java.time.Instant, note: String?): ReminderEntity =
        ReminderEntity(
            followUpId = followUpId,
            customerId = customerId,
            title = copy.notificationTitle("$customerName - $customerPhone"),
            body = copy.notificationBody(remindAt, note, businessZone),
            remindAtEpochMs = remindAt.toEpochMilli(),
        )

    fun createDebugReminder(id: Long): ReminderEntity = copy.debugReminder(clock.millis() + 30_000).copy(followUpId = id)

    companion object {
        const val MAX_SYNC_PAGES = 5
        const val MAX_MISSED_NOTIFICATIONS = 5
        const val MAX_SCHEDULED_ALARMS = 100
    }
}

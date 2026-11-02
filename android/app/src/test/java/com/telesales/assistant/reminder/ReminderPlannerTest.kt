package com.telesales.assistant.reminder

import androidx.room.OnConflictStrategy
import com.telesales.assistant.data.local.ReminderDao
import com.telesales.assistant.data.local.ReminderEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ReminderPlannerTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneOffset.UTC)

    @Test fun savesScheduledInteractionAndCancelsCompletedIds() {
        val item = reminder(8)
        val scheduler = FakeReminderScheduler().apply { schedule(reminder(3)); schedule(reminder(4)) }
        val actions = ReminderPlanner.interaction(item, listOf(3, 4))
        assertEquals(listOf(ReminderAction.Schedule(item), ReminderAction.Cancel(3), ReminderAction.Cancel(4)), actions)
        apply(actions, scheduler)
        assertEquals(mapOf(8L to item), scheduler.reminders)
    }

    @Test fun savesInteractionWithoutScheduleByCancellingCompletedIdsOnly() {
        val scheduler = FakeReminderScheduler().apply { schedule(reminder(3)) }
        val actions = ReminderPlanner.interaction(null, listOf(3))
        assertEquals(listOf(ReminderAction.Cancel(3)), actions)
        apply(actions, scheduler)
        assertEquals(emptyMap<Long, ReminderEntity>(), scheduler.reminders)
    }

    @Test fun completedOrCancelledFollowUpIsCancelled() {
        val scheduler = FakeReminderScheduler().apply { schedule(reminder(5)) }
        val actions = ReminderPlanner.completeOrCancel(5)
        assertEquals(listOf(ReminderAction.Cancel(5)), actions)
        apply(actions, scheduler)
        assertEquals(emptyMap<Long, ReminderEntity>(), scheduler.reminders)
    }

    @Test fun rescheduleOverwritesSameIdWithoutDuplicateAction() {
        val first = reminder(5, clock.millis() + 60_000)
        val moved = reminder(5, clock.millis() + 120_000)
        val actions = ReminderPlanner.synchronize(listOf(first), listOf(moved), clock)
        assertEquals(listOf(ReminderAction.Schedule(moved)), actions)
        val scheduler = FakeReminderScheduler()
        scheduler.schedule(first); scheduler.schedule(moved)
        assertEquals(1, scheduler.reminders.size)
        assertEquals(moved, scheduler.reminders[5])
    }

    @Test fun archiveCancelsAllFollowUps() {
        val scheduler = FakeReminderScheduler().apply { schedule(reminder(1)); schedule(reminder(2)) }
        val actions = ReminderPlanner.archive(listOf(1, 2))
        assertEquals(listOf(ReminderAction.Cancel(1), ReminderAction.Cancel(2)), actions)
        apply(actions, scheduler)
        assertEquals(emptyMap<Long, ReminderEntity>(), scheduler.reminders)
    }

    @Test fun synchronizationCancelsLocalIdsMissingFromPending() {
        val scheduler = FakeReminderScheduler().apply { schedule(reminder(1)); schedule(reminder(2)) }
        val actions = ReminderPlanner.synchronize(listOf(reminder(1), reminder(2)), listOf(reminder(2)), clock)
        assertEquals(listOf(ReminderAction.Cancel(1)), actions)
        apply(actions, scheduler)
        assertEquals(setOf(2L), scheduler.reminders.keys)
    }

    @Test fun synchronizationSchedulesMissingReminder() {
        val next = reminder(2, clock.millis() + 60_000)
        val actions = ReminderPlanner.synchronize(emptyList(), listOf(next), clock)
        assertEquals(listOf(ReminderAction.Schedule(next)), actions)
        val scheduler = FakeReminderScheduler()
        apply(actions, scheduler)
        assertEquals(mapOf(2L to next), scheduler.reminders)
    }

    @Test fun synchronizationReschedulesChangedTime() {
        val old = reminder(2, clock.millis() + 60_000)
        val updated = reminder(2, clock.millis() + 120_000)
        assertEquals(listOf(ReminderAction.Schedule(updated)), ReminderPlanner.synchronize(listOf(old), listOf(updated), clock))
    }

    @Test fun synchronizationDoesNotSchedulePastReminder() {
        val past = reminder(2, clock.millis() - 1)
        assertEquals(emptyList<ReminderAction>(), ReminderPlanner.synchronize(emptyList(), listOf(past), clock))
    }

    @Test fun synchronizationDoesNotRepeatAlreadyNotifiedReminder() {
        val notified = reminder(2).copy(notified = true)
        assertEquals(emptyList<ReminderAction>(), ReminderPlanner.synchronize(listOf(notified), listOf(notified), clock))
    }

    @Test fun synchronizationCancelsAnExistingPastAlarm() {
        val past = reminder(2, clock.millis() - 1)
        assertEquals(listOf(ReminderAction.Cancel(2)), ReminderPlanner.synchronize(listOf(past), listOf(past), clock))
    }

    @Test fun daoStoreDelegatesPersistenceOperations() = kotlinx.coroutines.test.runTest {
        val dao = FakeReminderDao()
        val store = ReminderDaoStore(dao)
        store.upsert(reminder(9))
        assertEquals(reminder(9), store.get(9))
        assertEquals(listOf(reminder(9)), store.getAll())
        store.markNotified(9)
        assertEquals(true, store.get(9)?.notified)
        store.delete(9)
        assertEquals(null, store.get(9))
    }

    @Test fun choosesExactAndInexactAlarmBasedOnPermission() {
        assertEquals(AlarmPrecision.Exact, alarmPrecision(FakeExactAlarmPermission(true)))
        assertEquals(AlarmPrecision.Inexact, alarmPrecision(FakeExactAlarmPermission(false)))
    }

    private fun reminder(id: Long, at: Long = clock.millis() + 300_000) = ReminderEntity(id, 12, "Gọi lại: Khách", "Giờ hẹn: 02/11/2026 07:05", at)

    private fun apply(actions: List<ReminderAction>, scheduler: FakeReminderScheduler) {
        actions.forEach { action -> when (action) {
            is ReminderAction.Schedule -> scheduler.schedule(action.reminder)
            is ReminderAction.Cancel -> scheduler.cancel(action.followUpId)
        } }
    }

    private class FakeReminderScheduler : ReminderScheduler {
        val reminders = mutableMapOf<Long, ReminderEntity>()
        override fun schedule(reminder: ReminderEntity) { reminders[reminder.followUpId] = reminder }
        override fun cancel(followUpId: Long) { reminders.remove(followUpId) }
    }

    private class FakeExactAlarmPermission(private val granted: Boolean) : ExactAlarmPermission {
        override fun canScheduleExactAlarms() = granted
    }

    private class FakeReminderDao : ReminderDao {
        private val items = mutableMapOf<Long, ReminderEntity>()
        override suspend fun getAll() = items.values.sortedBy(ReminderEntity::remindAtEpochMs)
        override suspend fun get(id: Long) = items[id]
        override suspend fun upsert(reminder: ReminderEntity) { items[reminder.followUpId] = reminder }
        override suspend fun delete(id: Long) { items.remove(id) }
        override suspend fun markNotified(id: Long) { items[id]?.let { items[id] = it.copy(notified = true) } }
    }
}

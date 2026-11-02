package com.telesales.assistant.reminder

import com.telesales.assistant.data.local.ReminderEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class ReminderRecoveryPlannerTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneOffset.UTC)

    @Test fun futureUnnotifiedReminderIsPlannedForReschedule() {
        val future = reminder(1, clock.millis() + 60_000)
        assertEquals(listOf(ReminderRecoveryAction.Schedule(future)), ReminderRecoveryPlanner.plan(listOf(future), clock))
    }

    @Test fun pastUnnotifiedReminderIsPlannedAsMissed() {
        val past = reminder(2, clock.millis() - 1)
        assertEquals(listOf(ReminderRecoveryAction.NotifyMissed(past)), ReminderRecoveryPlanner.plan(listOf(past), clock))
    }

    @Test fun emptyReminderListProducesNoActions() {
        assertTrue(ReminderRecoveryPlanner.plan(emptyList(), clock).isEmpty())
    }

    @Test fun alreadyNotifiedReminderIsIgnoredAfterReboot() {
        assertTrue(ReminderRecoveryPlanner.plan(listOf(reminder(3).copy(notified = true)), clock).isEmpty())
    }

    @Test fun recoverySchedulesFutureAndMarksMissedNotifiedExactlyOnce() = runTest {
        val future = reminder(1, clock.millis() + 60_000)
        val missed = reminder(2, clock.millis() - 1)
        val store = FakeReminderStore(listOf(future, missed))
        val scheduler = FakeScheduler()
        val notifications = mutableListOf<ReminderEntity>()
        val coordinator = ReminderCoordinator(
            store = store,
            scheduler = scheduler,
            clock = clock,
            copy = FakeCopy(),
            notifier = ReminderNotifier { notifications.add(it); Unit },
        )

        coordinator.recoverAfterBoot()
        coordinator.recoverAfterBoot()

        assertEquals(mapOf(future.followUpId to future), scheduler.scheduled)
        assertEquals(1, notifications.size)
        assertEquals("Lịch bị lỡ: Giờ hẹn", notifications.single().body)
        assertTrue(store.get(2)!!.notified)
        assertFalse(store.get(1)!!.notified)
    }

    private fun reminder(id: Long, at: Long = clock.millis() + 120_000) =
        ReminderEntity(id, id + 100, "Khách $id", "Giờ hẹn", at)

    private class FakeReminderStore(reminders: List<ReminderEntity>) : ReminderStore {
        private val values = reminders.associateBy(ReminderEntity::followUpId).toMutableMap()
        override suspend fun getAll() = values.values.toList()
        override suspend fun get(id: Long) = values[id]
        override suspend fun upsert(reminder: ReminderEntity) { values[reminder.followUpId] = reminder }
        override suspend fun delete(id: Long) { values.remove(id) }
        override suspend fun markNotified(id: Long) { values[id]?.let { values[id] = it.copy(notified = true) } }
        override suspend fun claimNotification(id: Long): Boolean {
            val item = values[id] ?: return false
            if (item.notified) return false
            values[id] = item.copy(notified = true)
            return true
        }
    }

    private class FakeScheduler : ReminderScheduler {
        val scheduled = mutableMapOf<Long, ReminderEntity>()
        override fun schedule(reminder: ReminderEntity) { scheduled[reminder.followUpId] = reminder }
        override fun cancel(followUpId: Long) { scheduled.remove(followUpId) }
    }

    private class FakeCopy : ReminderCopy {
        override fun notificationTitle(customerName: String) = customerName
        override fun notificationBody(remindAt: Instant, note: String?, zoneId: ZoneId) = "body"
        override fun debugReminder(atEpochMs: Long) = ReminderEntity(0, 0, "Debug", "Debug", atEpochMs)
        override fun missedBody(body: String) = "Lịch bị lỡ: $body"
    }
}

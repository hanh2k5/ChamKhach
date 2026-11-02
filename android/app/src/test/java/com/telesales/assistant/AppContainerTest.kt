package com.telesales.assistant

import com.telesales.assistant.data.network.ApiServiceFactory
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.data.settings.InMemoryPreferencesDataStore
import com.telesales.assistant.di.AppContainer
import com.telesales.assistant.data.repository.ApiCustomerRepository
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.data.repository.CustomerDetailRepository
import com.telesales.assistant.data.repository.CallLogRepository
import com.telesales.assistant.data.repository.FollowUpRepository
import com.telesales.assistant.data.repository.ApiFollowUpRepository
import com.telesales.assistant.data.repository.DashboardRepository
import com.telesales.assistant.data.repository.ApiDashboardRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.telesales.assistant.data.local.ReminderEntity
import com.telesales.assistant.reminder.ReminderCoordinator
import com.telesales.assistant.reminder.ReminderDaoStore
import com.telesales.assistant.reminder.ReminderScheduler
import com.telesales.assistant.reminder.ReminderStore
import java.time.Clock
import com.telesales.assistant.reminder.ReminderCopy

class AppContainerTest {
    @Test
    fun appContainer_exposesSettingsAndApiDependencies() {
        val container = object : AppContainer {
            override val settingsStore = SettingsStore(InMemoryPreferencesDataStore())
            override val apiServiceFactory = ApiServiceFactory()
            private val customerApiRepository = ApiCustomerRepository(settingsStore, apiServiceFactory)
            override val customerRepository = customerApiRepository
            override val customerFormRepository: CustomerFormRepository = customerApiRepository
            override val customerDetailRepository: CustomerDetailRepository = customerApiRepository
            override val callLogRepository: CallLogRepository = customerApiRepository
            override val followUpRepository: FollowUpRepository = ApiFollowUpRepository(settingsStore, apiServiceFactory)
            override val dashboardRepository: DashboardRepository = ApiDashboardRepository(settingsStore, apiServiceFactory)
            override val reminderStore: ReminderStore = object : ReminderStore {
                override suspend fun getAll() = emptyList<ReminderEntity>()
                override suspend fun get(id: Long): ReminderEntity? = null
                override suspend fun upsert(reminder: ReminderEntity) = Unit
                override suspend fun delete(id: Long) = Unit
                override suspend fun markNotified(id: Long) = Unit
            }
            override val reminderScheduler: ReminderScheduler = object : ReminderScheduler {
                override fun schedule(reminder: ReminderEntity) = Unit
                override fun cancel(followUpId: Long) = Unit
            }
            override val reminderCoordinator = ReminderCoordinator(reminderStore, reminderScheduler, Clock.systemUTC(), copy = object : ReminderCopy {
                override fun notificationTitle(customerName: String) = customerName
                override fun notificationBody(remindAt: java.time.Instant, note: String?, zoneId: java.time.ZoneId) = note.orEmpty()
                override fun debugReminder(atEpochMs: Long) = ReminderEntity(0, 0, "debug", "debug", atEpochMs)
            })
        }

        assertNotNull(container.settingsStore)
        assertNotNull(container.apiServiceFactory)
        assertTrue(container is AppContainer)
    }
}


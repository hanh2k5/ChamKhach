package com.telesales.assistant.di

import android.content.Context
import com.telesales.assistant.data.network.ApiServiceFactory
import com.telesales.assistant.data.network.ApiServiceProvider
import com.telesales.assistant.data.settings.SettingsStore
import com.telesales.assistant.data.repository.ApiCustomerRepository
import com.telesales.assistant.data.repository.CustomerRepository
import com.telesales.assistant.data.repository.CustomerFormRepository
import com.telesales.assistant.data.repository.CustomerDetailRepository
import com.telesales.assistant.data.repository.CallLogRepository
import com.telesales.assistant.data.repository.ApiFollowUpRepository
import com.telesales.assistant.data.repository.FollowUpRepository
import com.telesales.assistant.data.repository.ApiDashboardRepository
import com.telesales.assistant.data.repository.DashboardRepository
import androidx.room.Room
import android.app.AlarmManager
import com.telesales.assistant.data.local.AppDatabase
import com.telesales.assistant.reminder.AndroidExactAlarmPermission
import com.telesales.assistant.reminder.AndroidReminderScheduler
import com.telesales.assistant.reminder.ReminderCoordinator
import com.telesales.assistant.reminder.ReminderDaoStore
import com.telesales.assistant.reminder.ReminderScheduler
import com.telesales.assistant.reminder.ReminderStore
import com.telesales.assistant.reminder.ResourceReminderCopy
import java.time.Clock

/**
 * Interface cho Dependency Container theo mô hình Manual DI (SRS mục 2 & 9.1).
 * Các dependency cụ thể (SettingsRepository, CustomerRepository, Database, ApiService)
 * sẽ được cung cấp qua container này ở các task A1..A8.
 */
interface AppContainer {
    val settingsStore: SettingsStore
    val apiServiceFactory: ApiServiceProvider
    val customerRepository: CustomerRepository
    val customerFormRepository: CustomerFormRepository
    val customerDetailRepository: CustomerDetailRepository
    val callLogRepository: CallLogRepository
    val followUpRepository: FollowUpRepository
    val dashboardRepository: DashboardRepository
    val reminderStore: ReminderStore
    val reminderScheduler: ReminderScheduler
    val reminderCoordinator: ReminderCoordinator
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val settingsStore = SettingsStore(context)
    override val apiServiceFactory = ApiServiceFactory()
    private val customerApiRepository = ApiCustomerRepository(settingsStore, apiServiceFactory)
    override val customerRepository: CustomerRepository = customerApiRepository
    override val customerFormRepository: CustomerFormRepository = customerApiRepository
    override val customerDetailRepository: CustomerDetailRepository = customerApiRepository
    override val callLogRepository: CallLogRepository = customerApiRepository
    override val followUpRepository: FollowUpRepository = ApiFollowUpRepository(settingsStore, apiServiceFactory)
    override val dashboardRepository: DashboardRepository = ApiDashboardRepository(settingsStore, apiServiceFactory)
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "reminders.db").build()
    override val reminderStore: ReminderStore = ReminderDaoStore(database.reminderDao())
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    override val reminderScheduler: ReminderScheduler = AndroidReminderScheduler(
        context, alarmManager, AndroidExactAlarmPermission(alarmManager),
    )
    override val reminderCoordinator = ReminderCoordinator(reminderStore, reminderScheduler, Clock.systemUTC(), copy = ResourceReminderCopy(context))
}


package com.telesales.assistant.reminder

import com.telesales.assistant.data.local.ReminderEntity

interface ReminderScheduler {
    fun schedule(reminder: ReminderEntity)
    fun cancel(followUpId: Long)
}

interface ExactAlarmPermission {
    fun canScheduleExactAlarms(): Boolean
}

interface ReminderStore {
    suspend fun getAll(): List<ReminderEntity>
    suspend fun get(id: Long): ReminderEntity?
    suspend fun upsert(reminder: ReminderEntity)
    suspend fun delete(id: Long)
    suspend fun markNotified(id: Long)
    suspend fun claimNotification(id: Long): Boolean
}

class ReminderDaoStore(private val dao: com.telesales.assistant.data.local.ReminderDao) : ReminderStore {
    override suspend fun getAll() = dao.getAll()
    override suspend fun get(id: Long) = dao.get(id)
    override suspend fun upsert(reminder: ReminderEntity) = dao.upsert(reminder)
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun markNotified(id: Long) = dao.markNotified(id)
    override suspend fun claimNotification(id: Long) = dao.claimNotification(id) > 0
}

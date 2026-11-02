package com.telesales.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY remindAtEpochMs")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE followUpId = :id")
    suspend fun get(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE followUpId = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE reminders SET notified = 1 WHERE followUpId = :id")
    suspend fun markNotified(id: Long)
}

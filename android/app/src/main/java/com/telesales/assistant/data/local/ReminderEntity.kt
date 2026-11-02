package com.telesales.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val followUpId: Long,
    val customerId: Long,
    val title: String,
    val body: String,
    val remindAtEpochMs: Long,
    val notified: Boolean = false,
)

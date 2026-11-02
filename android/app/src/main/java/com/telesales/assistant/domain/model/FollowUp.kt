package com.telesales.assistant.domain.model

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FollowUp(
    val id: Long,
    val remindAt: Instant,
    val status: String,
    val group: String,
    val customer: FollowUpCustomer,
    val note: String?,
) {
    fun formattedRemindAt(clock: Clock, zoneId: ZoneId): String =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(clock.withZone(zoneId).zone).format(remindAt)
}

data class FollowUpCustomer(
    val id: Long,
    val companyName: String?,
    val contactName: String?,
    val phone: String,
)

package com.telesales.assistant.domain.model

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Customer(
    val id: Long,
    val companyName: String?,
    val contactName: String?,
    val phone: String,
    val industry: String?,
    val potentialLevel: String,
    val status: String,
    val interactionCount: Int,
    val lastContactedAt: Instant?,
    val lastResult: String?,
) {
    fun formattedLastContactedAt(clock: Clock, zoneId: ZoneId): String? = lastContactedAt?.let {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(clock.withZone(zoneId).zone).format(it)
    }
}

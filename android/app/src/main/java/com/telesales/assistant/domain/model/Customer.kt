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

data class CustomerProfile(
    val id: Long,
    val companyName: String?,
    val contactName: String?,
    val phone: String,
    val address: String?,
    val industry: String?,
    val source: String?,
    val potentialLevel: String,
    val status: String,
    val interactionCount: Int = 0,
    val lastContactedAt: Instant? = null,
    val lastResult: String? = null,
    val nextFollowUp: NextFollowUp? = null,
)

data class NextFollowUp(val id: Long, val remindAt: Instant)

data class Interaction(
    val id: Long,
    val result: String,
    val needs: List<String>,
    val note: String?,
    val contactedAt: Instant,
)

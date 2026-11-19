package com.telesales.assistant.data.mapper

import com.telesales.assistant.data.network.FollowUpCustomerDto
import com.telesales.assistant.data.network.FollowUpDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZoneId

class FollowUpMapperTest {
    @Test fun mapsNullNoteAndUtcInstantToVietnamDisplayTimeAndBack() {
        val dto = FollowUpDto(5, "2026-11-02T17:30:00+00:00", "pending", "upcoming", FollowUpCustomerDto(7, null, "Chị Lan", "0900000000"), null)
        val domain = dto.toDomain()
        assertEquals(null, domain.note)
        assertEquals("03/11/2026 00:30:00", domain.formattedRemindAt(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), ZoneId.of("Asia/Ho_Chi_Minh")))
        assertEquals(dto, domain.toDto())
    }
}

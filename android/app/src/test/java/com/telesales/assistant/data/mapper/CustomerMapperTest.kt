package com.telesales.assistant.data.mapper

import com.telesales.assistant.data.network.CustomerDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class CustomerMapperTest {
    @Test fun mapsDtoToDomainAndBackAndConvertsUtcToBusinessZone() {
        val dto = CustomerDto(7, "Cửa hàng", "Lan", "0912345678", "Bán lẻ", "high", "active", 3,
            "2026-10-30T07:15:00+00:00", "interested")
        val customer = dto.toDomain()
        assertEquals(Instant.parse("2026-10-30T07:15:00Z"), customer.lastContactedAt)
        assertEquals("2026-10-30T07:15:00Z", customer.toDto().last_contacted_at)
        val fixedClock = Clock.fixed(Instant.parse("2026-10-30T07:15:00Z"), ZoneId.of("UTC"))
        assertEquals("30/10/2026 14:15", customer.formattedLastContactedAt(fixedClock, ZoneId.of("Asia/Ho_Chi_Minh")))
    }
}

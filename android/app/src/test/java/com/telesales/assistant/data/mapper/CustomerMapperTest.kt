package com.telesales.assistant.data.mapper

import com.telesales.assistant.data.network.CustomerDto
import com.telesales.assistant.data.network.CustomerProfileDto
import com.telesales.assistant.data.mapper.toCreateDto
import com.telesales.assistant.data.mapper.toUpdateDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    @Test fun mapsCustomerProfileForEditAndCreateRequest() {
        val profile = CustomerProfileDto(8, "Công ty", "Mai", "0944417032", "Địa chỉ", "Bán lẻ", "Giới thiệu", "high", "active").toDomain()
        val editDto = profile.toUpdateDto()
        assertEquals("Công ty", editDto.company_name)
        assertEquals("0944417032", editDto.phone)
        assertEquals("active", editDto.status)

        val createDto = profile.copy(id = 0, address = null).toCreateDto()
        val createJson = Json.encodeToString(createDto)
        val editJson = Json.encodeToString(profile.copy(address = null).toUpdateDto())
        assertFalse(createJson.contains("status"))
        assertEquals(true, editJson.contains("\"address\":null"))
    }
}

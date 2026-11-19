package com.telesales.assistant.data.mapper

import com.telesales.assistant.data.network.CustomerDto
import com.telesales.assistant.data.network.CustomerProfileDto
import com.telesales.assistant.data.network.InteractionDto
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
    @Test fun mapsInteractionNeedsAndUtcTimeToDomainAndBack() {
        val dto = InteractionDto(19, "callback", listOf("website", "ads"), "Gọi chiều mai", "2026-11-02T03:00:00Z")
        val interaction = dto.toDomain()
        assertEquals(listOf("website", "ads"), interaction.needs)
        assertEquals(Instant.parse("2026-11-02T03:00:00Z"), interaction.contactedAt)
        assertEquals(dto, interaction.toDto())
        val clock = Clock.fixed(Instant.parse("2026-11-02T03:00:00Z"), ZoneId.of("UTC"))
        assertEquals("02/11/2026 10:00:00", java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")).zone).format(interaction.contactedAt))
    }

    @Test fun mapsDtoToDomainAndBackAndConvertsUtcToBusinessZone() {
        val dto = CustomerDto(id = 7, company_name = "Cửa hàng", contact_name = "Lan", phone = "0912345678",
            industry = "Bán lẻ", status = "active", interaction_count = 3,
            last_contacted_at = "2026-10-30T07:15:00+00:00", last_result = "interested")
        val customer = dto.toDomain()
        assertEquals(Instant.parse("2026-10-30T07:15:00Z"), customer.lastContactedAt)
        assertEquals("2026-10-30T07:15:00Z", customer.toDto().last_contacted_at)
        val fixedClock = Clock.fixed(Instant.parse("2026-10-30T07:15:00Z"), ZoneId.of("UTC"))
        assertEquals("30/10/2026 14:15:00", customer.formattedLastContactedAt(fixedClock, ZoneId.of("Asia/Ho_Chi_Minh")))
    }

    @Test fun mapsCustomerProfileForEditAndCreateRequest() {
        val profile = CustomerProfileDto(8, "Công ty", "Mai", "0944417032", "Địa chỉ", "Bán lẻ", "Giới thiệu", "active").toDomain()
        val editDto = profile.toUpdateDto()
        assertEquals("Công ty", editDto.company_name)
        assertEquals("0944417032", editDto.phone)

        val createDto = profile.copy(id = 0, address = null).toCreateDto()
        val createJson = Json.encodeToString(createDto)
        val editJson = Json.encodeToString(profile.copy(address = null).toUpdateDto())
        assertFalse(createJson.contains("status"))
        assertFalse(editJson.contains("status"))
        assertEquals(true, editJson.contains("\"address\":null"))
    }

    @Test fun mapsCustomerWorkflowFieldsToAndFromApiDtos() {
        val dto = CustomerProfileDto(
            id = 8,
            phone = "0944417032",
            active_since = "5 năm",
            industry = "Xây dựng",
            needs = listOf("website", "ads"),
        )
        val profile = dto.toDomain()
        assertEquals("5 năm", profile.activeSince)
        assertEquals(listOf("website", "ads"), profile.needs)
        assertEquals("5 năm", profile.toCreateDto().active_since)
        assertEquals(listOf("website", "ads"), profile.toUpdateDto().needs)
    }
}

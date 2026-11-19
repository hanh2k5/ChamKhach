package com.telesales.assistant.data.mapper

import com.telesales.assistant.data.network.DashboardDto
import com.telesales.assistant.data.network.FollowUpCustomerDto
import com.telesales.assistant.data.network.FollowUpDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class DashboardMapperTest {
    @Test fun mapsServerCountsEmptyItemsAndBack() {
        val dto = DashboardDto(overdue_count = 2, today_count = 4, items = emptyList())
        val domain = dto.toDomain()
        assertEquals(2, domain.overdueCount)
        assertEquals(4, domain.todayCount)
        assertEquals(emptyList<Any>(), domain.items)
        assertEquals(dto, domain.toDto())
    }

    @Test fun mapsItemWithNullNoteAndFormatsUtcInVietnamTime() {
        val dto = DashboardDto(1, 0, listOf(FollowUpDto(
            6, "2026-11-02T17:30:00+00:00", "pending", "overdue",
            FollowUpCustomerDto(7, null, "Chị Lan", "0900000000"), null,
        )))
        val domain = dto.toDomain()
        val item = domain.items.single()
        assertEquals(null, item.note)
        assertEquals("03/11/2026 00:30:00", item.formattedRemindAt(
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), ZoneId.of("Asia/Ho_Chi_Minh"),
        ))
        assertEquals(dto, domain.toDto())
    }
}

package com.telesales.assistant.data.mapper

import com.telesales.assistant.data.network.DashboardDto
import com.telesales.assistant.domain.model.DashboardSummary

fun DashboardDto.toDomain(): DashboardSummary = DashboardSummary(
    overdueCount = overdue_count,
    todayCount = today_count,
    potentialCount = potential_count,
    items = items.map { it.toDomain() },
)

fun DashboardSummary.toDto(): DashboardDto = DashboardDto(
    overdue_count = overdueCount,
    today_count = todayCount,
    potential_count = potentialCount,
    items = items.map { it.toDto() },
)

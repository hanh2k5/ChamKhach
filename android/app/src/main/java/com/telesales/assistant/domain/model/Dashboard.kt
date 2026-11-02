package com.telesales.assistant.domain.model

data class DashboardSummary(
    val overdueCount: Int,
    val todayCount: Int,
    val potentialCount: Int,
    val items: List<FollowUp>,
)

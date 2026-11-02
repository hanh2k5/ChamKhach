package com.telesales.assistant.data.mapper

import com.telesales.assistant.data.network.CustomerDto
import com.telesales.assistant.domain.model.Customer
import java.time.Instant

fun CustomerDto.toDomain(): Customer = Customer(
    id = id,
    companyName = company_name,
    contactName = contact_name,
    phone = phone,
    industry = industry,
    potentialLevel = potential_level,
    status = status,
    interactionCount = interaction_count,
    lastContactedAt = last_contacted_at?.let(Instant::parse),
    lastResult = last_result,
)

fun Customer.toDto(): CustomerDto = CustomerDto(
    id = id,
    company_name = companyName,
    contact_name = contactName,
    phone = phone,
    industry = industry,
    potential_level = potentialLevel,
    status = status,
    interaction_count = interactionCount,
    last_contacted_at = lastContactedAt?.toString(),
    last_result = lastResult,
)

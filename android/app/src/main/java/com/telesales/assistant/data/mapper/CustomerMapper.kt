package com.telesales.assistant.data.mapper

import com.telesales.assistant.data.network.CustomerDto
import com.telesales.assistant.data.network.CustomerProfileDto
import com.telesales.assistant.data.network.CustomerCreateDto
import com.telesales.assistant.data.network.CustomerUpdateDto
import com.telesales.assistant.data.network.InteractionDto
import com.telesales.assistant.data.network.NextFollowUpDto
import com.telesales.assistant.domain.model.Customer
import com.telesales.assistant.domain.model.Interaction
import com.telesales.assistant.domain.model.NextFollowUp
import com.telesales.assistant.domain.model.CustomerProfile
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

fun CustomerProfileDto.toDomain(): CustomerProfile = CustomerProfile(
    id = id,
    companyName = company_name,
    contactName = contact_name,
    phone = phone,
    address = address,
    industry = industry,
    source = source,
    potentialLevel = potential_level,
    status = status,
    interactionCount = interaction_count,
    lastContactedAt = last_contacted_at?.let(Instant::parse),
    lastResult = last_result,
    nextFollowUp = next_follow_up?.toDomain(),
    activeSince = active_since,
    needs = needs.orEmpty(),
)

fun NextFollowUpDto.toDomain() = NextFollowUp(id, Instant.parse(remind_at))

fun InteractionDto.toDomain(): Interaction = Interaction(
    id = id,
    result = result,
    needs = needs,
    note = note,
    contactedAt = Instant.parse(contacted_at),
)

fun Interaction.toDto(): InteractionDto = InteractionDto(
    id = id,
    result = result,
    needs = needs,
    note = note,
    contacted_at = contactedAt.toString(),
)

fun CustomerProfile.toCreateDto(): CustomerCreateDto = CustomerCreateDto(
    company_name = companyName,
    contact_name = contactName,
    phone = phone,
    address = address,
    active_since = activeSince,
    industry = industry,
    needs = needs,
    source = source,
    potential_level = potentialLevel,
)

fun CustomerProfile.toUpdateDto(): CustomerUpdateDto = CustomerUpdateDto(
    company_name = companyName,
    contact_name = contactName,
    phone = phone,
    address = address,
    active_since = activeSince,
    industry = industry,
    needs = needs,
    source = source,
    potential_level = potentialLevel,
)

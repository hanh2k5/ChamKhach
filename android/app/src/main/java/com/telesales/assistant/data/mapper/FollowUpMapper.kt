package com.telesales.assistant.data.mapper

import com.telesales.assistant.data.network.FollowUpCustomerDto
import com.telesales.assistant.data.network.FollowUpDto
import com.telesales.assistant.domain.model.FollowUp
import com.telesales.assistant.domain.model.FollowUpCustomer
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun FollowUpDto.toDomain(): FollowUp = FollowUp(
    id = id,
    remindAt = Instant.parse(remind_at),
    status = status,
    group = group,
    customer = FollowUpCustomer(customer.id, customer.company_name, customer.contact_name, customer.phone),
    note = note,
)

fun FollowUp.toDto(): FollowUpDto = FollowUpDto(
    id = id,
    remind_at = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx").withZone(ZoneOffset.UTC).format(remindAt),
    status = status,
    group = group,
    customer = FollowUpCustomerDto(customer.id, customer.companyName, customer.contactName, customer.phone),
    note = note,
)

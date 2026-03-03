package com.blexifi.app.data

import java.time.Instant

data class ChatMessage(
    val id: String,
    val fromId: String,
    val toId: String,
    val body: String,
    val timestampMs: Long = Instant.now().toEpochMilli(),
    val deliveryState: DeliveryState = DeliveryState.PENDING,
)

enum class DeliveryState { PENDING, RELAYED, DELIVERED, FAILED }

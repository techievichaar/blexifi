package com.blexifi.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val envelopeId: String,
    val sourceId: String,
    val destinationId: String,
    val ciphertext: String,
    val attempts: Int,
    val state: String,
    val createdAtMs: Long,
)

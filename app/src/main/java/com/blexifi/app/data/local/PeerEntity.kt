package com.blexifi.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val peerId: String,
    val displayName: String,
    val lastSeenMs: Long,
    val capabilitiesCsv: String,
)

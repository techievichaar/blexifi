package com.blexifi.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE state IN ('PENDING','RELAYED') ORDER BY createdAtMs ASC")
    suspend fun pendingOutbox(): List<MessageEntity>

    @Query("UPDATE messages SET state = :newState, attempts = :attempts WHERE envelopeId = :envelopeId")
    suspend fun updateState(envelopeId: String, newState: String, attempts: Int)
}

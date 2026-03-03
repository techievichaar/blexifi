package com.blexifi.app.data

import com.blexifi.app.data.local.MessageDao
import com.blexifi.app.data.local.MessageEntity
import com.blexifi.mesh.CryptoBox
import com.blexifi.mesh.Envelope
import com.blexifi.mesh.MeshRouter
import com.blexifi.mesh.PeerLink
import com.blexifi.mesh.SeenCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.crypto.SecretKey
import java.util.UUID

class OfflineChatRepository(
    private val selfId: String,
    private val router: MeshRouter = MeshRouter(SeenCache()),
    private val sessionKey: SecretKey = CryptoBox.generateKey(),
    private val messageDao: MessageDao? = null,
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    suspend fun bootstrapFromStore() {
        val dao = messageDao ?: return
        val pending = dao.pendingOutbox()
        _messages.value = pending.map {
            ChatMessage(
                id = it.envelopeId,
                fromId = it.sourceId,
                toId = it.destinationId,
                body = "[encrypted-payload]",
                timestampMs = it.createdAtMs,
                deliveryState = when (it.state) {
                    "DELIVERED" -> DeliveryState.DELIVERED
                    "FAILED" -> DeliveryState.FAILED
                    "RELAYED" -> DeliveryState.RELAYED
                    else -> DeliveryState.PENDING
                },
            )
        }
    }

    suspend fun sendText(targetId: String, text: String, neighbors: List<PeerLink>): Envelope {
        val messageId = UUID.randomUUID().toString()
        val msg = ChatMessage(id = messageId, fromId = selfId, toId = targetId, body = text)
        _messages.value = _messages.value + msg

        val encryptedPayload = CryptoBox.encryptToBase64(sessionKey, text)
        val envelope = Envelope.text(selfId, targetId, encryptedPayload, 6)
        router.onReceive(selfId, envelope, neighbors)

        messageDao?.upsert(
            MessageEntity(
                envelopeId = envelope.envelopeId.toString(),
                sourceId = envelope.sourceId,
                destinationId = envelope.destinationId,
                ciphertext = envelope.ciphertext,
                attempts = 0,
                state = "PENDING",
                createdAtMs = envelope.timestampMs,
            ),
        )

        return envelope
    }

    fun decryptForDisplay(envelope: Envelope): String {
        return CryptoBox.decryptFromBase64(sessionKey, envelope.ciphertext)
    }

    suspend fun markDelivered(envelope: Envelope) {
        _messages.value = _messages.value.map {
            if (it.fromId == envelope.sourceId && it.toId == envelope.destinationId) {
                it.copy(deliveryState = DeliveryState.DELIVERED)
            } else it
        }
        messageDao?.updateState(
            envelopeId = envelope.envelopeId.toString(),
            newState = "DELIVERED",
            attempts = 0,
        )
    }
}

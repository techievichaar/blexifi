package com.blexifi.app.data

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
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun sendText(targetId: String, text: String, neighbors: List<PeerLink>): Envelope {
        val msg = ChatMessage(id = UUID.randomUUID().toString(), fromId = selfId, toId = targetId, body = text)
        _messages.value = _messages.value + msg

        val encryptedPayload = CryptoBox.encryptToBase64(sessionKey, text)
        val envelope = Envelope.text(selfId, targetId, encryptedPayload, 6)
        router.onReceive(selfId, envelope, neighbors)
        return envelope
    }

    fun decryptForDisplay(envelope: Envelope): String {
        return CryptoBox.decryptFromBase64(sessionKey, envelope.ciphertext)
    }

    fun markDelivered(envelope: Envelope) {
        _messages.value = _messages.value.map {
            if (it.fromId == envelope.sourceId && it.toId == envelope.destinationId) {
                it.copy(deliveryState = DeliveryState.DELIVERED)
            } else it
        }
    }
}

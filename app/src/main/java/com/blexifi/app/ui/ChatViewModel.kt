package com.blexifi.app.ui

import androidx.lifecycle.ViewModel
import com.blexifi.app.data.ChatMessage
import com.blexifi.app.data.OfflineChatRepository
import com.blexifi.mesh.PeerLink
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel(
    private val repository: OfflineChatRepository = OfflineChatRepository(selfId = "device-A"),
) : ViewModel() {
    val messages: StateFlow<List<ChatMessage>> = repository.messages

    fun send(targetId: String, message: String) {
        if (targetId.isBlank() || message.isBlank()) return

        repository.sendText(
            targetId = targetId,
            text = message,
            neighbors = listOf(
                PeerLink("relay-C", 8, 7, 9),
                PeerLink("relay-D", 7, 8, 8),
            ),
        )
    }
}

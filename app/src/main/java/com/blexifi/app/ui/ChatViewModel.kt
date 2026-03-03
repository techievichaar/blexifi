package com.blexifi.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blexifi.app.data.ChatMessage
import com.blexifi.app.data.OfflineChatRepository
import com.blexifi.mesh.PeerLink
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: OfflineChatRepository,
) : ViewModel() {
    val messages: StateFlow<List<ChatMessage>> = repository.messages

    init {
        viewModelScope.launch {
            repository.bootstrapFromStore()
        }
    }

    fun send(targetId: String, message: String) {
        if (targetId.isBlank() || message.isBlank()) return

        viewModelScope.launch {
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
}

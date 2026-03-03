package com.blexifi.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blexifi.app.R
import com.blexifi.app.data.OfflineChatRepository
import com.blexifi.app.data.local.AppDatabase
import com.blexifi.app.service.OfflineMeshService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val repository = OfflineChatRepository(
            selfId = "device-A",
            messageDao = AppDatabase.get(this).messageDao(),
        )
        viewModel = ChatViewModel(repository)

        val statusText = findViewById<TextView>(R.id.statusText)
        val targetInput = findViewById<EditText>(R.id.targetInput)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val startButton = findViewById<Button>(R.id.startButton)
        val messagesView = findViewById<TextView>(R.id.messagesView)

        startButton.setOnClickListener {
            startForegroundService(Intent(this, OfflineMeshService::class.java))
            statusText.text = "Mesh service running"
        }

        sendButton.setOnClickListener {
            viewModel.send(targetInput.text.toString(), messageInput.text.toString())
            messageInput.text.clear()
        }

        lifecycleScope.launch {
            viewModel.messages.collect { msgs ->
                messagesView.text = if (msgs.isEmpty()) {
                    "No messages yet"
                } else {
                    msgs.joinToString("\n") { "${it.fromId} -> ${it.toId}: ${it.body} [${it.deliveryState}]" }
                }
            }
        }
    }
}

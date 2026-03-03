package com.blexifi.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.blexifi.app.service.OfflineMeshService
import com.blexifi.app.service.OemReliabilityPolicy

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (!OemReliabilityPolicy.bootActions().contains(action)) return

        val serviceIntent = Intent(context, OfflineMeshService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}

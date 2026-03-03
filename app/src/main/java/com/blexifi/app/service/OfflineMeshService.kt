package com.blexifi.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.IBinder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.blexifi.app.work.RetryOutboxWorker
import com.blexifi.mesh.BlePresencePayload
import com.blexifi.mesh.PeerDirectory
import java.util.concurrent.TimeUnit

class OfflineMeshService : Service() {
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var wifiP2pManager: WifiP2pManager? = null
    private val peerDirectory = PeerDirectory()

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        initializeTransports()
        scheduleRetryWorker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // BLE advertise payload format (broadcast to nearby peers)
        val payload = BlePresencePayload(
            deviceId = "device-A",
            protocolVersion = 1,
            capabilities = listOf("relay", "ack", "wifi_direct"),
        ).encode()

        // TODO: attach `payload` bytes to BLE advertise packet and start scan callbacks.
        // TODO: initialize Wi‑Fi Direct discovery + group/socket orchestration.
        if (payload.isEmpty()) {
            stopSelf()
        }

        // Local decode sanity and peer-table update path (same format used by scanner callback pipeline).
        handlePresencePayload(payload)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: stop scans, unregister listeners, close sockets.
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handlePresencePayload(raw: ByteArray) {
        val parsed = BlePresencePayload.decode(raw)
        peerDirectory.updateFromBlePayload(parsed, System.currentTimeMillis())
    }

    private fun initializeTransports() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothScanner = adapter?.bluetoothLeScanner
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private fun scheduleRetryWorker() {
        val request = PeriodicWorkRequestBuilder<RetryOutboxWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            RETRY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Offline Mesh", NotificationManager.IMPORTANCE_LOW),
            )
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Blexifi mesh active")
            .setContentText("Discovering nearby peers over BLE/Wi‑Fi Direct")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "offline_mesh"
        private const val NOTIFICATION_ID = 101
        private const val RETRY_WORK_NAME = "retry_outbox_worker"
    }
}

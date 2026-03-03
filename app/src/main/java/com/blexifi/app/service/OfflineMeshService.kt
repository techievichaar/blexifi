package com.blexifi.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.IBinder
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.blexifi.app.ops.HealthReporter
import com.blexifi.app.transport.BleScannerPipeline
import com.blexifi.app.transport.WifiDirectCoordinator
import com.blexifi.app.work.RetryOutboxWorker
import com.blexifi.mesh.BlePresencePayload
import com.blexifi.mesh.PeerDirectory
import java.util.concurrent.TimeUnit

class OfflineMeshService : Service() {
    private var wifiP2pManager: WifiP2pManager? = null
    private val peerDirectory = PeerDirectory()
    private var bleScannerPipeline: BleScannerPipeline? = null
    private var wifiDirectCoordinator: WifiDirectCoordinator? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        HealthReporter.increment(this, "service_on_create")
        HealthReporter.setLastTimestamp(this, "service_last_create_ms", System.currentTimeMillis())
        initializeTransports()
        scheduleRetryWorker()
        OemReliabilityPolicy.buildBatteryOptimizationIntent(this)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(intent) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val payload = BlePresencePayload(
            deviceId = "device-A",
            protocolVersion = 1,
            capabilities = listOf("relay", "ack", "wifi_direct"),
        ).encode()

        if (payload.isEmpty()) {
            stopSelf()
        }

        handlePresencePayload(payload)
        HealthReporter.increment(this, "service_on_start")
        HealthReporter.setLastTimestamp(this, "service_last_start_ms", System.currentTimeMillis())
        bleScannerPipeline?.start()
        wifiDirectCoordinator?.discoverPeers()
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        HealthReporter.increment(this, "service_on_destroy")
        HealthReporter.setLastTimestamp(this, "service_last_destroy_ms", System.currentTimeMillis())
        bleScannerPipeline?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handlePresencePayload(raw: ByteArray) {
        val parsed = BlePresencePayload.decode(raw)
        peerDirectory.updateFromBlePayload(parsed, System.currentTimeMillis())
    }

    private fun initializeTransports() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        bleScannerPipeline = BleScannerPipeline(adapter, peerDirectory)

        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        wifiDirectCoordinator = wifiP2pManager?.let { WifiDirectCoordinator(this, it) }
    }

    private fun scheduleRetryWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<RetryOutboxWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
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

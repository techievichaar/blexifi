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

class OfflineMeshService : Service() {
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var wifiP2pManager: WifiP2pManager? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        initializeTransports()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: start BLE scan/advertise and Wi‑Fi Direct discovery here.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: stop scans, unregister listeners, close sockets.
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeTransports() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothScanner = adapter?.bluetoothLeScanner
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
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
    }
}

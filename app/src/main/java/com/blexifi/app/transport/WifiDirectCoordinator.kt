package com.blexifi.app.transport

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

class WifiDirectCoordinator(
    context: Context,
    private val manager: WifiP2pManager,
) {
    private val channel: WifiP2pManager.Channel = manager.initialize(context, context.mainLooper, null)

    fun discoverPeers() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "discoverPeers started")
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "discoverPeers failed: $reason")
            }
        })
    }

    fun connect(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "connect request submitted")
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "connect failed: $reason")
            }
        })
    }

    companion object {
        private const val TAG = "WifiDirectCoordinator"
    }
}

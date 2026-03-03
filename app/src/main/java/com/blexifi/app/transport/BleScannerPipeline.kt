package com.blexifi.app.transport

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import com.blexifi.mesh.BlePresencePayload
import com.blexifi.mesh.PeerDirectory
import java.util.UUID

class BleScannerPipeline(
    private val adapter: BluetoothAdapter?,
    private val peerDirectory: PeerDirectory,
) {
    private val scanner: BluetoothLeScanner?
        get() = adapter?.bluetoothLeScanner

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val bytes = result?.scanRecord?.getServiceData(PRESENCE_UUID) ?: return
            val payload = runCatching { BlePresencePayload.decode(bytes) }.getOrNull() ?: return
            peerDirectory.updateFromBlePayload(payload, System.currentTimeMillis())
        }
    }

    fun start() {
        scanner?.startScan(callback)
    }

    fun stop() {
        scanner?.stopScan(callback)
    }

    companion object {
        val PRESENCE_UUID: ParcelUuid = ParcelUuid(UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb"))
    }
}

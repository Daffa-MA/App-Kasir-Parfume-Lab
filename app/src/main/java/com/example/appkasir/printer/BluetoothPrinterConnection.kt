package com.example.appkasir.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Handler untuk koneksi Bluetooth ke thermal printer
 */
class BluetoothPrinterConnection(private val context: Context) {
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false
    
    private val printerUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID untuk printer
    
    // Callback listener
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }
    
    /**
     * Get list of paired Bluetooth devices
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }
    
    /**
     * Connect to printer via Bluetooth
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!hasBluetoothPermission()) {
                onError?.invoke("Bluetooth permission denied")
                return@withContext false
            }
            
            // Close existing connection if any
            disconnect()
            
            // Create socket
            bluetoothSocket = device.createRfcommSocketToServiceRecord(printerUUID)
            bluetoothAdapter?.cancelDiscovery()
            
            // Connect with timeout
            bluetoothSocket?.connect()
            
            // Initialize streams
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            
            isConnected = true
            onConnectionStateChanged?.invoke(true)
            
            true
        } catch (e: Exception) {
            isConnected = false
            onConnectionStateChanged?.invoke(false)
            onError?.invoke("Connection failed: ${e.message}")
            disconnect()
            false
        }
    }
    
    /**
     * Disconnect from printer
     */
    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            onError?.invoke("Disconnect error: ${e.message}")
        } finally {
            isConnected = false
            onConnectionStateChanged?.invoke(false)
        }
    }
    
    /**
     * Send data to printer
     */
    suspend fun sendData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isConnected) {
                onError?.invoke("Printer not connected")
                return@withContext false
            }
            
            outputStream?.write(data)
            outputStream?.flush()
            true
        } catch (e: Exception) {
            onError?.invoke("Send failed: ${e.message}")
            false
        }
    }
    
    /**
     * Send text to printer
     */
    suspend fun sendText(text: String): Boolean {
        return sendData(text.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Check Bluetooth permission
     */
    private fun hasBluetoothPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
}

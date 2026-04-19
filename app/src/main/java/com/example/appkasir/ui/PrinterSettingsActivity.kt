package com.example.appkasir.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appkasir.R
import com.example.appkasir.printer.BluetoothPrinterConnection
import com.example.appkasir.printer.PrinterConstants
import com.example.appkasir.printer.PrinterManager
import com.example.appkasir.printer.ReceiptFormatter
import kotlinx.coroutines.launch
import android.widget.TextView

/**
 * Printer Configuration and Settings Activity
 */
class PrinterSettingsActivity : AppCompatActivity() {
    
    private lateinit var printerManager: PrinterManager
    private lateinit var bluetoothConnection: BluetoothPrinterConnection
    private var selectedDevice: BluetoothDevice? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_settings)
        
        printerManager = PrinterManager(this)
        bluetoothConnection = BluetoothPrinterConnection(this)
        
        setupViews()
        setupListeners()
        loadPrinterSettings()
    }
    
    private fun setupViews() {
        // Toolbar/Header
        findViewById<Button>(R.id.btnPrinterBack).apply {
            setOnClickListener { finish() }
        }
        
        // Device List
        findViewById<Button>(R.id.btnScanPrinters).apply {
            setOnClickListener { scanAvailablePrinters() }
        }
        
        // Connection Type Spinner
        val spinnerConnectionType = findViewById<Spinner>(R.id.spinnerConnectionType)
        val connectionTypes = arrayOf("Bluetooth", "USB", "Network")
        val connectionAdapter = ArrayAdapter(this, R.layout.spinner_item_modern, connectionTypes)
        connectionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_modern)
        spinnerConnectionType.adapter = connectionAdapter
        
        // Paper Width Spinner
        val spinnerPaperWidth = findViewById<Spinner>(R.id.spinnerPaperWidth)
        val paperWidths = arrayOf("58mm", "80mm")
        val paperAdapter = ArrayAdapter(this, R.layout.spinner_item_modern, paperWidths)
        paperAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_modern)
        spinnerPaperWidth.adapter = paperAdapter
        
        // Settings Switches
        findViewById<Switch>(R.id.switchAutoPrint).apply {
            isChecked = printerManager.isAutoPrint()
        }
        
        findViewById<Switch>(R.id.switchOpenDrawer).apply {
            isChecked = printerManager.isOpenDrawer()
        }
        
        findViewById<Switch>(R.id.switchPrintPreview).apply {
            isChecked = printerManager.isPrintPreview()
        }
        
        // Action Buttons
        findViewById<Button>(R.id.btnTestPrint).apply {
            setOnClickListener { testPrint() }
        }
        
        findViewById<Button>(R.id.btnSaveSettings).apply {
            setOnClickListener { saveSettings() }
        }
    }
    
    private fun setupListeners() {
        bluetoothConnection.onConnectionStateChanged = { connected ->
            if (connected) {
                showInfo("Printer terhubung")
            }
        }
        
        bluetoothConnection.onError = { error ->
            showError(error)
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun scanAvailablePrinters() {
        val devices = bluetoothConnection.getPairedDevices()
        
        if (devices.isEmpty()) {
            showError("Tidak ada perangkat Bluetooth yang dipasangkan")
            return
        }
        
        val deviceNames = devices.map { "${it.name} (${it.address})" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Pilih Printer")
            .setItems(deviceNames) { _, which ->
                selectedDevice = devices[which]
                printerManager.savePrinterDevice(
                    devices[which].name ?: "Unknown",
                    devices[which].address,
                    PrinterConstants.CONNECTION_TYPE_BLUETOOTH
                )
                showInfo("Printer dipilih: ${devices[which].name}")
                loadPrinterSettings()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun testPrint() {
        if (!printerManager.isPrinterConfigured()) {
            showError("Silakan konfigurasi printer terlebih dahulu")
            return
        }
        
        lifecycleScope.launch {
            try {
                // Find device to connect
                val devices = bluetoothConnection.getPairedDevices()
                val address = printerManager.getPrinterAddress()
                val device = devices.find { it.address == address }
                
                if (device == null) {
                    showError("Printer tidak ditemukan")
                    return@launch
                }
                
                // Connect and print test receipt
                val connected = bluetoothConnection.connect(device)
                if (!connected) {
                    showError("Gagal menghubungkan printer")
                    return@launch
                }
                
                // Create test receipt
                val receipt = ReceiptFormatter()
                    .addHeader("AppKasir Test", "Thermal Printer Test")
                    .addTransactionInfo("TEST001", "Admin")
                    .addFooter("Printer Test Berhasil", openDrawer = printerManager.isOpenDrawer())
                
                // Send to printer
                val sent = bluetoothConnection.sendData(receipt.getReceiptData())
                
                bluetoothConnection.disconnect()
                
                if (sent) {
                    showInfo("Print test berhasil!")
                } else {
                    showError("Gagal mengirim data ke printer")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }
    
    private fun saveSettings() {
        try {
            val switchAutoPrint = findViewById<Switch>(R.id.switchAutoPrint)
            val switchOpenDrawer = findViewById<Switch>(R.id.switchOpenDrawer)
            val switchPrintPreview = findViewById<Switch>(R.id.switchPrintPreview)
            val spinnerPaperWidth = findViewById<Spinner>(R.id.spinnerPaperWidth)
            
            val paperWidth = if (spinnerPaperWidth.selectedItemPosition == 0) 58 else 80
            
            printerManager.savePrinterSettings(
                autoPrint = switchAutoPrint.isChecked,
                openDrawer = switchOpenDrawer.isChecked,
                printPreview = switchPrintPreview.isChecked
            )
            
            printerManager.setPaperWidth(paperWidth)
            
            showInfo("Pengaturan printer berhasil disimpan")
        } catch (e: Exception) {
            showError("Gagal menyimpan pengaturan: ${e.message}")
        }
    }
    
    private fun loadPrinterSettings() {
        val printerName = printerManager.getPrinterName() ?: "Belum dikonfigurasi"
        findViewById<TextView>(R.id.txtPrinterName).text = "Printer: $printerName"
    }
    
    private fun showInfo(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothConnection.disconnect()
    }
}

package com.example.appkasir.printer

import android.content.Context
import android.content.SharedPreferences

/**
 * Printer configuration and device management
 */
class PrinterManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("PrinterConfig", Context.MODE_PRIVATE)
    
    /**
     * Save selected printer device
     */
    fun savePrinterDevice(
        deviceName: String,
        deviceAddress: String,
        connectionType: String = PrinterConstants.CONNECTION_TYPE_BLUETOOTH
    ) {
        prefs.edit().apply {
            putString("printer_name", deviceName)
            putString("printer_address", deviceAddress)
            putString("printer_connection_type", connectionType)
            putLong("printer_save_time", System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Get saved printer device name
     */
    fun getPrinterName(): String? {
        return prefs.getString("printer_name", null)
    }
    
    /**
     * Get saved printer device address
     */
    fun getPrinterAddress(): String? {
        return prefs.getString("printer_address", null)
    }
    
    /**
     * Get printer connection type
     */
    fun getConnectionType(): String {
        return prefs.getString("printer_connection_type", PrinterConstants.CONNECTION_TYPE_BLUETOOTH) ?: 
            PrinterConstants.CONNECTION_TYPE_BLUETOOTH
    }
    
    /**
     * Check if printer is configured
     */
    fun isPrinterConfigured(): Boolean {
        return getPrinterName() != null && getPrinterAddress() != null
    }
    
    /**
     * Save paper width setting
     */
    fun setPaperWidth(width: Int) {
        prefs.edit().putInt("paper_width", width).apply()
    }
    
    /**
     * Get paper width setting (58 or 80 mm)
     */
    fun getPaperWidth(): Int {
        return prefs.getInt("paper_width", 58)
    }
    
    /**
     * Save printer settings
     */
    fun savePrinterSettings(
        autoPrint: Boolean = false,
        openDrawer: Boolean = false,
        printPreview: Boolean = true,
        cutPaper: Boolean = true
    ) {
        prefs.edit().apply {
            putBoolean("auto_print", autoPrint)
            putBoolean("open_drawer", openDrawer)
            putBoolean("print_preview", printPreview)
            putBoolean("cut_paper", cutPaper)
            apply()
        }
    }
    
    /**
     * Get auto print setting
     */
    fun isAutoPrint(): Boolean {
        return prefs.getBoolean("auto_print", false)
    }
    
    /**
     * Get open drawer setting
     */
    fun isOpenDrawer(): Boolean {
        return prefs.getBoolean("open_drawer", false)
    }
    
    /**
     * Get print preview setting
     */
    fun isPrintPreview(): Boolean {
        return prefs.getBoolean("print_preview", true)
    }
    
    /**
     * Get cut paper setting
     */
    fun isCutPaper(): Boolean {
        return prefs.getBoolean("cut_paper", true)
    }
    
    /**
     * Clear printer configuration
     */
    fun clearPrinterConfig() {
        prefs.edit().apply {
            remove("printer_name")
            remove("printer_address")
            remove("printer_connection_type")
            apply()
        }
    }
    
    /**
     * Get all printer settings as map
     */
    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            "printer_name" to (getPrinterName() ?: "Belum dikonfigurasi"),
            "connection_type" to getConnectionType(),
            "paper_width" to getPaperWidth(),
            "auto_print" to isAutoPrint(),
            "open_drawer" to isOpenDrawer(),
            "print_preview" to isPrintPreview(),
            "cut_paper" to isCutPaper()
        )
    }
}

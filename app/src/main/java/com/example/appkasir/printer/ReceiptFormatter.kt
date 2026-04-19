package com.example.appkasir.printer

import com.example.appkasir.ui.model.CartItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thermal printer receipt formatter untuk ESC/POS protocol
 */
class ReceiptFormatter(
    private val paperWidth: Int = 58, // mm
    private val charsPerLine: Int = PrinterConstants.CHARS_PER_LINE_58MM
) {
    
    private val commands = PrinterConstants.EscPos
    private val sb = StringBuilder()
    
    /**
     * Create receipt header
     */
    fun addHeader(storeName: String, storeInfo: String = ""): ReceiptFormatter {
        sb.append(commands.INIT) // Initialize printer
        sb.append(commands.ALIGN_CENTER)
        
        sb.append(commands.TEXT_SIZE_LARGE)
        sb.append(commands.BOLD_ON)
        sb.append(storeName)
        sb.append(commands.BOLD_OFF)
        sb.append(commands.LINE_FEED)
        
        sb.append(commands.TEXT_SIZE_NORMAL)
        if (storeInfo.isNotEmpty()) {
            sb.append(storeInfo)
            sb.append(commands.LINE_FEED)
        }
        
        sb.append(commands.LINE_FEED)
        return this
    }
    
    /**
     * Add transaction info
     */
    fun addTransactionInfo(
        transactionId: String,
        cashier: String = "",
        dateTime: Date = Date()
    ): ReceiptFormatter {
        sb.append(commands.ALIGN_LEFT)
        sb.append(commands.TEXT_SIZE_NORMAL)
        
        sb.append("No Transaksi: $transactionId")
        sb.append(commands.LINE_FEED)
        
        if (cashier.isNotEmpty()) {
            sb.append("Kasir: $cashier")
            sb.append(commands.LINE_FEED)
        }
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
        sb.append("Tanggal: ${dateFormat.format(dateTime)}")
        sb.append(commands.LINE_FEED)
        sb.append(commands.LINE_FEED)
        
        return this
    }
    
    /**
     * Add items header
     */
    fun addItemsHeader(): ReceiptFormatter {
        sb.append(commands.ALIGN_LEFT)
        sb.append(commands.TEXT_SIZE_NORMAL)
        sb.append(commands.BOLD_ON)
        sb.append(formatLine("Item", "Qty", "Total", includePrice = true))
        sb.append(commands.BOLD_OFF)
        sb.append(repeatChar("-", charsPerLine))
        sb.append(commands.LINE_FEED)
        return this
    }
    
    /**
     * Add cart items
     */
    fun addItems(items: List<CartItem>): ReceiptFormatter {
        sb.append(commands.ALIGN_LEFT)
        sb.append(commands.TEXT_SIZE_NORMAL)
        
        for (item in items) {
            when (item) {
                is CartItem.Perfume -> {
                    val itemName = "${item.product.name} (${item.ml}ml)"
                    val qty = item.ml.toString()
                    val total = formatCurrency(item.subtotal)
                    
                    // Line 1: Item name
                    sb.append(itemName)
                    sb.append(commands.LINE_FEED)
                    
                    // Line 2: Qty and Total
                    sb.append(formatRight("Qty: $qty", "Rp $total"))
                    sb.append(commands.LINE_FEED)
                }
                
                is CartItem.Bottle -> {
                    val itemName = item.bottle.name
                    val qty = "${item.pcs} pcs"
                    val total = formatCurrency(item.subtotal)
                    
                    // Line 1: Item name
                    sb.append(itemName)
                    sb.append(commands.LINE_FEED)
                    
                    // Line 2: Qty and Total
                    sb.append(formatRight("Qty: $qty", "Rp $total"))
                    sb.append(commands.LINE_FEED)
                }
            }
        }
        
        sb.append(repeatChar("-", charsPerLine))
        sb.append(commands.LINE_FEED)
        return this
    }
    
    /**
     * Add transaction summary
     */
    fun addSummary(
        subtotal: Long,
        discount: Long = 0,
        tax: Long = 0,
        totalAmount: Long,
        paidAmount: Long = 0,
        paymentMethod: String = "Cash"
    ): ReceiptFormatter {
        sb.append(commands.ALIGN_LEFT)
        sb.append(commands.TEXT_SIZE_NORMAL)
        
        // Subtotal
        sb.append(formatRight("Subtotal:", formatCurrency(subtotal)))
        sb.append(commands.LINE_FEED)
        
        // Discount
        if (discount > 0) {
            sb.append(formatRight("Diskon:", "- Rp ${formatCurrency(discount)}"))
            sb.append(commands.LINE_FEED)
        }
        
        // Tax
        if (tax > 0) {
            sb.append(formatRight("Pajak:", "+ Rp ${formatCurrency(tax)}"))
            sb.append(commands.LINE_FEED)
        }
        
        // Total
        sb.append(commands.BOLD_ON)
        sb.append(commands.TEXT_SIZE_LARGE)
        sb.append(formatRight("Total:", "Rp ${formatCurrency(totalAmount)}"))
        sb.append(commands.BOLD_OFF)
        sb.append(commands.TEXT_SIZE_NORMAL)
        sb.append(commands.LINE_FEED)
        
        // Payment Method
        sb.append(formatRight("Pembayaran:", paymentMethod))
        sb.append(commands.LINE_FEED)
        
        // Paid Amount
        if (paidAmount > 0) {
            sb.append(formatRight("Jumlah Dibayar:", "Rp ${formatCurrency(paidAmount)}"))
            sb.append(commands.LINE_FEED)
            
            val change = paidAmount - totalAmount
            if (change >= 0) {
                sb.append(commands.BOLD_ON)
                sb.append(formatRight("Kembalian:", "Rp ${formatCurrency(change)}"))
                sb.append(commands.BOLD_OFF)
                sb.append(commands.LINE_FEED)
            }
        }
        
        sb.append(commands.LINE_FEED)
        return this
    }
    
    /**
     * Add footer
     */
    fun addFooter(
        message: String = "Terima Kasih",
        openDrawer: Boolean = false
    ): ReceiptFormatter {
        sb.append(commands.ALIGN_CENTER)
        sb.append(commands.BOLD_ON)
        sb.append(message)
        sb.append(commands.BOLD_OFF)
        sb.append(commands.LINE_FEED)
        sb.append(commands.LINE_FEED)
        
        // Add website or contact if needed
        sb.append(commands.TEXT_SIZE_NORMAL)
        sb.append("Powered by AppKasir")
        sb.append(commands.LINE_FEED)
        sb.append(commands.LINE_FEED)
        
        // Cut paper
        sb.append(commands.PAPER_CUT_FULL)
        
        // Open cash drawer if needed
        if (openDrawer) {
            sb.append(commands.CASH_DRAWER_OPEN)
        }
        
        return this
    }
    
    /**
     * Get receipt data as bytes
     */
    fun getReceiptData(): ByteArray {
        return sb.toString().toByteArray(Charsets.UTF_8)
    }
    
    /**
     * Get receipt data as string
     */
    fun getReceipt(): String {
        return sb.toString()
    }
    
    /**
     * Format right-aligned text
     */
    private fun formatRight(left: String, right: String): String {
        val totalLength = charsPerLine
        val availableSpace = totalLength - left.length
        val spaces = maxOf(1, availableSpace - right.length)
        
        return left + " ".repeat(spaces) + right
    }
    
    /**
     * Format line with alignment
     */
    private fun formatLine(
        first: String,
        second: String,
        third: String,
        includePrice: Boolean = false
    ): String {
        return if (includePrice) {
            val spacing1 = " ".repeat(maxOf(1, 10 - first.length))
            val spacing2 = " ".repeat(maxOf(1, 8 - second.length))
            "$first$spacing1$second$spacing2$third"
        } else {
            val spacing = " ".repeat(maxOf(1, (charsPerLine - first.length - third.length) / 2))
            "$first$spacing$third"
        }
    }
    
    /**
     * Repeat character
     */
    private fun repeatChar(char: String, times: Int): String {
        return char.repeat(times) + "\n"
    }
    
    /**
     * Format currency
     */
    private fun formatCurrency(amount: Long): String {
        return String.format(Locale("id", "ID"), "%,d", amount)
    }
}

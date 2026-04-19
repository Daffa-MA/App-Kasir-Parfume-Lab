package com.example.appkasir.printer

/**
 * Konstanta dan command untuk thermal printer dengan protokol ESC/POS
 */
object PrinterConstants {
    
    // Connection Types
    const val CONNECTION_TYPE_BLUETOOTH = "bluetooth"
    const val CONNECTION_TYPE_USB = "usb"
    const val CONNECTION_TYPE_NETWORK = "network"
    
    // ESC/POS Commands
    object EscPos {
        // Initiate Printer
        const val INIT = "\u001B@"
        
        // Alignment
        const val ALIGN_LEFT = "\u001B\u0061\u0000"
        const val ALIGN_CENTER = "\u001B\u0061\u0001"
        const val ALIGN_RIGHT = "\u001B\u0061\u0002"
        
        // Text Size
        const val TEXT_SIZE_NORMAL = "\u001B\u0021\u0000"
        const val TEXT_SIZE_LARGE = "\u001B\u0021\u0010" // 2x height and width
        const val TEXT_SIZE_DOUBLE_HEIGHT = "\u001B\u0021\u0001"
        const val TEXT_SIZE_DOUBLE_WIDTH = "\u001B\u0021\u0010"
        
        // Text Style
        const val BOLD_ON = "\u001B\u0045\u0001"
        const val BOLD_OFF = "\u001B\u0045\u0000"
        const val UNDERLINE_ON = "\u001B\u002D\u0001"
        const val UNDERLINE_OFF = "\u001B\u002D\u0000"
        
        // Line Spacing
        const val LINE_SPACING_DEFAULT = "\u001B\u0033\u0024" // 36 dots = 1/8 inch
        const val LINE_SPACING_LARGE = "\u001B\u0033\u0030"
        
        // Cut Paper
        const val PAPER_CUT_FULL = "\u001D\u0056\u0000" // Full cut
        const val PAPER_CUT_PARTIAL = "\u001D\u0056\u0001" // Partial cut
        
        // Bell
        const val BEEP = "\u0007"
        
        // New Line
        const val LINE_FEED = "\u000A"
        const val CARRIAGE_RETURN = "\u000D"
        
        // Cash Drawer
        const val CASH_DRAWER_OPEN = "\u001B\u0070\u0000\u00FA\u00FA"
        
        // Print Density (0-15)
        const val PRINT_DENSITY = "\u001D\u007E"
    }
    
    // Printer Settings
    const val DEFAULT_PAPER_WIDTH = 58 // mm
    const val CHARS_PER_LINE_58MM = 32 // Approximate
    const val CHARS_PER_LINE_80MM = 48 // Approximate
    
    // Timeouts (ms)
    const val CONNECTION_TIMEOUT = 5000
    const val WRITE_TIMEOUT = 3000
    
    // Print Defaults
    const val DEFAULT_FONT_FAMILY = "monospace"
    const val RECEIPT_LINE_LENGTH = 32
}

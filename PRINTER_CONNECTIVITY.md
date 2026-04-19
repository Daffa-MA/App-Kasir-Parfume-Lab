# 🖨 Fitur Konektivitas Printer & Perangkat Kasir

## Deskripsi
AppKasir sekarang dilengkapi dengan sistem konektivitas lengkap untuk menghubungkan thermal printer (mesin cetak nota) dan perangkat lainnya melalui berbagai metode koneksi.

---

## ✨ Fitur Utama

### 1. **Thermal Printer (ESC/POS Protocol)**
- ✅ Koneksi via Bluetooth (utama)
- ✅ Koneksi via USB
- ✅ Koneksi via Network/WiFi
- ✅ Cetak nota otomatis setelah transaksi
- ✅ Support untuk printer 58mm dan 80mm
- ✅ Pembukaan drawer kasir otomatis

### 2. **Printer Features**
- ✅ Cetak header dan informasi toko
- ✅ Cetak detail produk dengan harga
- ✅ Cetak ringkasan pembayaran
- ✅ Cetak kembalian uang
- ✅ Format receipt profesional
- ✅ Support multiple language (Unicode)

### 3. **Pengaturan Printer**
- ✅ Scan perangkat Bluetooth berpasangan
- ✅ Pilih koneksi type (Bluetooth/USB/Network)
- ✅ Atur lebar kertas (58mm/80mm)
- ✅ Cetak test untuk verifikasi
- ✅ Pengaturan auto-print
- ✅ Toggle pembukaan drawer

### 4. **Cash Drawer Integration**
- ✅ Pembukaan drawer otomatis saat transaksi selesai
- ✅ Command ESC/POS standar industri
- ✅ Support untuk mechanical dan electronic drawer

---

## 🔧 Pengaturan Printer

### Lokasi Menu
1. Buka **Admin Dashboard**
2. Klik **🖨 Pengaturan Printer**
3. Konfigurasi printer sesuai kebutuhan

### Langkah Setup:

#### 1. **Pindai Printer**
```
1. Klik tombol "Pindai Printer"
2. Pilih printer dari list perangkat Bluetooth berpasangan
3. Printer akan tersimpan secara otomatis
```

#### 2. **Pilih Tipe Koneksi**
```
- Bluetooth: Koneksi wireless (paling umum di Indonesia)
- USB: Koneksi langsung via kabel USB
- Network: Koneksi via WiFi/LAN
```

#### 3. **Atur Lebar Kertas**
```
- 58mm: Ukuran standar kasir kecil
- 80mm: Ukuran thermal printer besar
```

#### 4. **Aktifkan Opsi Cetak**
```
✓ Cetak Otomatis: Cetak nota langsung setelah pembayaran
✓ Buka Drawer: Pembukaan drawer otomatis
✓ Preview: Tampilkan preview sebelum cetak
```

#### 5. **Test Print**
```
1. Klik "Cetak Test"
2. Printer akan menerima data test
3. Verifikasi output di printer fisik
```

---

## 📋 Format Nota Cetak

### Header
```
        APPKASIR STORE
      Thermal Printer Test
     
No Transaksi: TRX001
Kasir: Admin
Tanggal: 18/04/2026 14:30:00
```

### Items Section
```
────────────────────────────
Item            Qty      Total
────────────────────────────
Citrus Fresh
Qty: 10ml               Rp 120,000
────────────────────────────
```

### Payment Summary
```
Subtotal:          Rp 500,000
Diskon:          - Rp 50,000
Pajak:           + Rp 10,000
Total:           Rp 460,000
Pembayaran:              Cash
Jumlah Dibayar:  Rp 500,000
Kembalian:        Rp 40,000
```

### Footer
```
      Terima Kasih
      Powered by AppKasir
```

---

## 🛠 Teknologi & Protocol

### ESC/POS Commands
ESC/POS adalah standar protocol internasional untuk thermal printer:

```kotlin
// Inisialisasi
INIT = "\u001B@"

// Alignment
ALIGN_LEFT = "\u001B\u0061\u0000"
ALIGN_CENTER = "\u001B\u0061\u0001"
ALIGN_RIGHT = "\u001B\u0061\u0002"

// Text Style
BOLD_ON = "\u001B\u0045\u0001"
TEXT_SIZE_LARGE = "\u001B\u0021\u0010"

// Print Control
PAPER_CUT_FULL = "\u001D\u0056\u0000"
CASH_DRAWER_OPEN = "\u001B\u0070\u0000\u00FA\u00FA"
```

### Connection Types

**Bluetooth (Recommended)**
- ✅ Wireless, jangkauan 10-50m
- ✅ Paling umum di Indonesia
- ✅ UUID standar: 00001101-0000-1000-8000-00805F9B34FB
- ✅ Timeout: 5 detik

**USB**
- ✅ Koneksi stabil dan langsung
- ✅ Power dari perangkat
- ✅ Tidak perlu pairing

**Network/WiFi**
- ✅ Koneksi jarak jauh
- ✅ Support IP address
- ✅ Ideal untuk multi-lokasi

---

## 📦 Struktur Kode

### Package: `com.example.appkasir.printer`

#### `PrinterConstants.kt`
- Konstanta ESC/POS commands
- Definisi connection types
- Default settings

#### `BluetoothPrinterConnection.kt`
- Handler koneksi Bluetooth
- Manage socket connection
- Send/receive data
- Permission handling

#### `ReceiptFormatter.kt`
- Format receipt sesuai ESC/POS
- Support multiple paper widths
- Builder pattern untuk flexibility
- Currency formatting

#### `PrinterManager.kt`
- SharedPreferences management
- Save/load konfigurasi printer
- Setting management

#### `PrinterSettingsActivity.kt`
- UI untuk konfigurasi printer
- Scan perangkat Bluetooth
- Test print functionality
- Setting storage

### Layout: `activity_printer_settings.xml`
- Material Design 3 interface
- Section untuk koneksi, pengaturan, aksi
- ScrollView untuk multiple options

---

## 🔐 Permission Requirements

### AndroidManifest.xml
```xml
<!-- Bluetooth Permissions (Android 12+) -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Hardware Features -->
<uses-feature android:name="android.hardware.usb.host" android:required="false" />

<!-- Storage (untuk log dan config) -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

---

## 💡 Best Practices

### Setup Awal
1. **Hubungkan printer fisik** ke device Android via Bluetooth
2. **Pasangkan (Pair)** di Android settings jika belum
3. **Buka AppKasir** → Admin Dashboard → Pengaturan Printer
4. **Scan & pilih** printer dari list
5. **Test print** untuk verifikasi koneksi

### Troubleshooting

#### Printer tidak terdeteksi
```
✓ Periksa printer dalam jarak dekat
✓ Pastikan printer dalam mode pairing
✓ Restart printer dan Android device
✓ Bersihkan cache Bluetooth
```

#### Print gagal/lambat
```
✓ Periksa koneksi Bluetooth stabil
✓ Reduce jarak antara printer & device
✓ Cek buffer printer tidak penuh
✓ Restart aplikasi
```

#### Drawer tidak terbuka
```
✓ Pastikan printer support drawer
✓ Check cash drawer terhubung dengan baik
✓ Test command drawer di printer setting
```

---

## 🚀 Integrasi dengan Transaksi

Printer secara otomatis mencetak nota saat:
1. ✅ Transaksi selesai dan pembayaran diterima
2. ✅ Sistem memformat nota dengan semua item
3. ✅ Mengirim ke printer via Bluetooth
4. ✅ Drawer terbuka otomatis (jika diaktifkan)
5. ✅ Nota terpotong otomatis

---

## 📱 Kompatibilitas

### Android Version
- Minimum: Android 5.0 (API 21)
- Target: Android 12+ (API 31+)
- Full support: Android 13+ (API 33+)

### Printer Brand Support
- ✅ Epson TM Series (TM-U220, TM-T88)
- ✅ Xprinter (XP-58, XP-80)
- ✅ Thermal Printer generik (58mm/80mm)
- ✅ Semua printer ESC/POS compatible

---

## 📊 Data Format

### Receipt Data (ByteArray)
```
Encoding: UTF-8
Line Width: 32 chars (58mm) atau 48 chars (80mm)
Paper Cut: Full cut (Advanced) atau Partial cut (Standard)
```

### Connection Info (SharedPreferences)
```
Key: printer_name → Nama printer
Key: printer_address → MAC address Bluetooth
Key: printer_connection_type → bluetooth/usb/network
Key: auto_print → boolean
Key: open_drawer → boolean
```

---

## ⚠️ Catatan Penting

1. **Bluetooth Pairing**: Printer harus dipasangkan di Settings Android terlebih dahulu
2. **Permissions**: User harus grant Bluetooth permissions saat pertama kali
3. **Connection Timeout**: Koneksi akan timeout jika lebih dari 5 detik
4. **Paper Handling**: Pastikan ada kertas di printer sebelum cetak
5. **Unicode Support**: Format rupiah dan karakter spesial sudah di-support

---

## 🔄 Update & Support

### Fitur Masa Depan (Roadmap)
- [ ] Support untuk barcode scanner
- [ ] Network printer auto-discovery
- [ ] Wireless printing ke multiple printers
- [ ] Cloud printing integration
- [ ] Print history/logging
- [ ] Mobile app web interface

### Dokumentasi Lengkap
Lihat file `DATABASE.md` dan `UI_UX_IMPROVEMENTS.md` untuk informasi lebih lanjut tentang arsitektur aplikasi.

---

*Last Updated: April 18, 2026*
*Version: 1.0*

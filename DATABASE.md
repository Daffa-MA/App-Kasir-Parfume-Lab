# 📊 AppKasir Database Documentation

## Overview
AppKasir menggunakan **Room Database** (SQLite) untuk menyimpan data aplikasi secara lokal. Database dirancang untuk offline-first approach dengan sync capabilities.

---

## 📋 Database Structure

### 1️⃣ **perfume_products** Table
Menyimpan data produk perfume (bibit wangi).

| Column | Type | Description |
|--------|------|-------------|
| **id** | TEXT (PK) | Unique identifier (e.g., "P001") |
| **name** | TEXT | Nama produk |
| **pricePerMl** | INTEGER | Harga per ml (Rp) |
| **stockMl** | REAL | Stok dalam ml |

**Sample Data:**
```
P001 | Ocean Breeze | 15000 | 500.0
P002 | Rose Delight | 20000 | 350.0
P003 | Lavender Mist | 18000 | 600.0
P006 | Oud Royal | 50000 | 150.0
```

---

### 2️⃣ **bottle_products** Table
Menyimpan data produk botol (kemasan).

| Column | Type | Description |
|--------|------|-------------|
| **id** | TEXT (PK) | Unique identifier (e.g., "B001") |
| **name** | TEXT | Nama produk |
| **capacityMl** | INTEGER | Kapasitas botol dalam ml |
| **price** | INTEGER | Harga botol (Rp) |
| **stockPcs** | INTEGER | Stok dalam piece |

**Sample Data:**
```
B001 | 30ml Bottle | 30 | 10000 | 500
B002 | 50ml Bottle | 50 | 15000 | 300
B003 | 100ml Bottle | 100 | 25000 | 200
```

---

### 3️⃣ **transactions** Table
Menyimpan record setiap transaksi penjualan.

| Column | Type | Description |
|--------|------|-------------|
| **id** | LONG (PK, Auto) | Auto-increment transaction ID |
| **total** | LONG | Total harga exact (Rp) |
| **roundedTotal** | LONG | Total harga dibulatkan ke 100 (Rp) |
| **status** | STRING | Status: "PENDING" atau "SYNCED" |
| **createdAt** | LONG | Timestamp pembuatan transaksi |
| **cashReceived** | LONG | Uang yang diterima (Rp) |
| **changeAmount** | LONG | Uang kembalian (Rp) |

**Status Values:**
- `PENDING` - Transaksi belum di-sync ke backend
- `SYNCED` - Transaksi sudah di-sync ke backend

**Example:**
```
ID: 1 | Total: 125,500 | Rounded: 125,600 | Status: SYNCED | Cash: 150,000 | Change: 24,400
```

---

### 4️⃣ **transaction_items** Table
Menyimpan detail item dalam setiap transaksi (line items).

| Column | Type | Description |
|--------|------|-------------|
| **id** | LONG (PK, Auto) | Item ID |
| **transactionId** | LONG (FK) | Reference ke transactions.id |
| **name** | STRING | Nama produk |
| **type** | STRING | Tipe produk: "perfume" atau "bottle" |
| **qty** | DOUBLE | Jumlah (ml untuk perfume, pcs untuk bottle) |
| **subtotal** | LONG | Total harga item (Rp) |

**Example:**
```
ID: 1 | Transaction: 1 | Name: Ocean Breeze | Type: perfume | Qty: 10.0 ml | Subtotal: 150,000
ID: 2 | Transaction: 1 | Name: 30ml Bottle | Type: bottle | Qty: 1.0 | Subtotal: 10,000
```

---

## 🔄 Relationships

```
┌─────────────────────┐
│   transactions      │
│  (master record)    │
└──────────┬──────────┘
           │ 1:M
           │
      transactionId
           │
           ▼
┌─────────────────────┐
│ transaction_items   │ ◄─── Detail setiap transaksi
│ (line items)        │      (dapat ada 1 atau lebih items)
└─────────────────────┘
```

---

## 💾 Data Flow

### Saat Melakukan Transaksi:
1. **User** memilih produk (Perfume/Bottle)
2. **Aplikasi** menyimpan ke cart (in-memory)
3. **User** checkout
4. **Aplikasi** membuat record di `transactions` table
5. **Aplikasi** membuat records di `transaction_items` table (satu per item di cart)
6. **Aplikasi** mencoba sync ke backend
7. Jika **BERHASIL** → status menjadi `SYNCED`
8. Jika **GAGAL** → status tetap `PENDING` (akan di-retry nanti)

### Admin Manage Products:
1. **Admin** buka Admin Dashboard
2. **Admin** dapat Add/Edit/Delete products
3. **Perubahan** langsung tersimpan di database
4. **Stock** terupdate untuk setiap transaksi

---

## 🎯 Database Lifecycle

### Initialization:
- Database dibuat otomatis saat pertama kali app berjalan
- File: `perfume_lab_pos.db`
- Lokasi: `/data/data/com.example.appkasir/databases/`

### Default Data (Seeding):
```kotlin
// Perfume products default
P001 - Ocean Breeze: 15,000/ml | 500ml stock
P002 - Rose Delight: 20,000/ml | 350ml stock
P003 - Lavender Mist: 18,000/ml | 600ml stock
P004 - Citrus Fresh: 12,000/ml | 800ml stock
P005 - Vanilla Dream: 25,000/ml | 200ml stock
P006 - Oud Royal: 50,000/ml | 150ml stock

// Bottle products default
B001 - 30ml Bottle: 10,000 | 500pcs stock
B002 - 50ml Bottle: 15,000 | 300pcs stock
B003 - 100ml Bottle: 25,000 | 200pcs stock
```

### Migrations:
Database memiliki migration system:
- **v1 → v2**: Menambah kolom `rounded_total`
- **v2 → v3**: Membuat tables untuk products

---

## 🔐 Access Patterns

### Read Operations:
```kotlin
// Ambil semua perfume
val perfumes = catalogRepository.getPerfumes()

// Ambil semua bottle
val bottles = catalogRepository.getBottles()

// Ambil transaksi pending
val pending = transactionRepository.getPendingTransactions()
```

### Write Operations:
```kotlin
// Simpan transaksi baru
val transactionId = transactionRepository.createTransaction(items, total)

// Update stock
catalogRepository.updatePerfume(perfume)
catalogRepository.updateBottle(bottle)

// Mark as synced
transactionRepository.markSynced(transactionId)
```

---

## 📊 Sample Query Results

### Transaksi Hari Ini:
```
Transaction #1
├─ Ocean Breeze x 10ml → Rp 150,000
├─ Alcohol x 20ml → Rp 40,000
├─ 30ml Bottle x 1pcs → Rp 10,000
└─ Total: Rp 200,000 | Status: SYNCED | Change: 0

Transaction #2
├─ Rose Delight x 15ml → Rp 300,000
├─ Alcohol x 35ml → Rp 70,000
├─ 50ml Bottle x 1pcs → Rp 15,000
└─ Total: Rp 385,000 | Status: PENDING | Change: 15,000
```

---

## 🛡️ Data Integrity

### Cascade Delete:
- Saat transaction dihapus → transaction_items otomatis dihapus (FK relationship)

### Constraints:
- Primary Keys: Unique constraint untuk setiap record
- Foreign Keys: transactionId harus valid reference ke transactions table

### Data Validation:
- Stock tidak boleh negatif
- Total harus > 0
- Type harus "perfume" atau "bottle"

---

## 📈 Performance

### Indexing:
- `transactions.status` - indexed (untuk quick status lookup)
- `transaction_items.transactionId` - indexed (untuk join operations)

### Query Performance:
- Fetch all transactions: O(n) - linear time
- Filter by status: O(log n) - using index
- Get transaction with items: O(n) - single query with JOIN

---

## 🔄 Sync Strategy

Database menggunakan **Offline-First** approach:

1. **Local Storage**: Semua data disimpan di Room Database
2. **Background Sync**: Worker mencoba sync pending transactions
3. **Retry Logic**: Failed syncs akan di-retry dengan exponential backoff
4. **Conflict Resolution**: Last-write-wins strategy

---

## 📝 File Locations

### Database File:
- Path: `/data/data/com.example.appkasir/databases/perfume_lab_pos.db`
- Readable via: Android Studio → Device File Explorer

### Entity Definitions:
- `PerfumeProductEntity.kt` - Perfume model
- `BottleProductEntity.kt` - Bottle model
- `TransactionEntity.kt` - Transaction model
- `TransactionItemEntity.kt` - Line item model

### DAO (Data Access Objects):
- `CatalogDao.kt` - Methods untuk products
- `TransactionDao.kt` - Methods untuk transactions

### Repository:
- `CatalogRepository.kt` - Business logic untuk catalog
- `TransactionRepository.kt` - Business logic untuk transactions

---

## 🎓 Key Takeaways

✅ **Room Database** untuk local SQLite storage
✅ **Offline-First** design untuk reliability
✅ **Auto-increment IDs** untuk transactions
✅ **Foreign Key relationships** untuk data integrity
✅ **Migration system** untuk schema evolution
✅ **Async operations** menggunakan Kotlin Coroutines
✅ **Sync worker** untuk background sync

---

*Last Updated: April 17, 2026*
*Database Version: 3*

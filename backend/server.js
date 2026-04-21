const express = require('express');
const cors = require('cors');
const initSqlJs = require('sql.js');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;
const HOST = '0.0.0.0';
const DB_PATH = path.join(__dirname, 'pos.db');

// ==================== MIDDLEWARE ====================
app.use(cors());
app.use(express.json());

// ==================== DATABASE ====================
let db;

async function initDB() {
  const SQL = await initSqlJs();

  // Load existing DB or create new
  try {
    if (fs.existsSync(DB_PATH)) {
      const buffer = fs.readFileSync(DB_PATH);
      db = new SQL.Database(buffer);
      console.log('✅ Database loaded from', DB_PATH);
    } else {
      db = new SQL.Database();
      console.log('✅ New database created');
    }
  } catch (err) {
    db = new SQL.Database();
    console.log('✅ New database created (corrupted file ignored)');
  }

  // Create tables
  db.run(`
    CREATE TABLE IF NOT EXISTS products (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      category TEXT NOT NULL CHECK(category IN ('perfume', 'bottle')),
      price_per_ml REAL DEFAULT 0,
      price REAL DEFAULT 0,
      capacity_ml INTEGER DEFAULT 0,
      stock_ml REAL DEFAULT 0,
      stock_pcs INTEGER DEFAULT 0
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS transactions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      items TEXT NOT NULL,
      perfume_subtotal REAL DEFAULT 0,
      alcohol_subtotal REAL DEFAULT 0,
      bottle_subtotal REAL DEFAULT 0,
      total REAL NOT NULL,
      payment_method TEXT NOT NULL,
      cash_received REAL DEFAULT 0,
      change_amount REAL DEFAULT 0,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS transaction_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      transaction_id INTEGER NOT NULL,
      product_id TEXT NOT NULL,
      product_name TEXT NOT NULL,
      category TEXT NOT NULL,
      quantity REAL NOT NULL,
      unit_price REAL NOT NULL,
      subtotal REAL NOT NULL
    )
  `);

  seedData();
  saveDB();
}

function saveDB() {
  try {
    const data = db.export();
    const buffer = Buffer.from(data);
    fs.writeFileSync(DB_PATH, buffer);
  } catch (err) {
    console.error('Failed to save DB:', err.message);
  }
}

function seedData() {
  const count = db.exec('SELECT COUNT(*) as c FROM products');
  if (count.length > 0 && count[0].values[0][0] > 0) return;

  const products = [
    // Perfumes
    { id: 'P001', name: 'Ocean Breeze', category: 'perfume', price_per_ml: 15000, price: 0, capacity_ml: 0, stock_ml: 500, stock_pcs: 0 },
    { id: 'P002', name: 'Rose Delight', category: 'perfume', price_per_ml: 20000, price: 0, capacity_ml: 0, stock_ml: 350, stock_pcs: 0 },
    { id: 'P003', name: 'Lavender Mist', category: 'perfume', price_per_ml: 18000, price: 0, capacity_ml: 0, stock_ml: 600, stock_pcs: 0 },
    { id: 'P004', name: 'Citrus Fresh', category: 'perfume', price_per_ml: 12000, price: 0, capacity_ml: 0, stock_ml: 800, stock_pcs: 0 },
    { id: 'P005', name: 'Vanilla Dream', category: 'perfume', price_per_ml: 25000, price: 0, capacity_ml: 0, stock_ml: 200, stock_pcs: 0 },
    { id: 'P006', name: 'Oud Royal', category: 'perfume', price_per_ml: 50000, price: 0, capacity_ml: 0, stock_ml: 150, stock_pcs: 0 },
    { id: 'P007', name: 'Jasmine Bloom', category: 'perfume', price_per_ml: 22000, price: 0, capacity_ml: 0, stock_ml: 400, stock_pcs: 0 },
    { id: 'P008', name: 'Sandalwood Pure', category: 'perfume', price_per_ml: 30000, price: 0, capacity_ml: 0, stock_ml: 250, stock_pcs: 0 },
    { id: 'P009', name: 'Musk Amber', category: 'perfume', price_per_ml: 35000, price: 0, capacity_ml: 0, stock_ml: 180, stock_pcs: 0 },
    { id: 'P010', name: 'Green Tea', category: 'perfume', price_per_ml: 10000, price: 0, capacity_ml: 0, stock_ml: 1000, stock_pcs: 0 },
    // Bottles
    { id: 'B001', name: 'Vial 3ml', category: 'bottle', price_per_ml: 0, price: 1500, capacity_ml: 3, stock_ml: 0, stock_pcs: 200 },
    { id: 'B002', name: 'Vial 5ml', category: 'bottle', price_per_ml: 0, price: 2000, capacity_ml: 5, stock_ml: 0, stock_pcs: 200 },
    { id: 'B003', name: 'Vial 10ml', category: 'bottle', price_per_ml: 0, price: 3000, capacity_ml: 10, stock_ml: 0, stock_pcs: 150 },
    { id: 'B004', name: 'Botol Spray 15ml', category: 'bottle', price_per_ml: 0, price: 5000, capacity_ml: 15, stock_ml: 0, stock_pcs: 100 },
    { id: 'B005', name: 'Botol Spray 30ml', category: 'bottle', price_per_ml: 0, price: 7500, capacity_ml: 30, stock_ml: 0, stock_pcs: 80 },
    { id: 'B006', name: 'Botol Roll On 5ml', category: 'bottle', price_per_ml: 0, price: 2500, capacity_ml: 5, stock_ml: 0, stock_pcs: 120 },
    { id: 'B007', name: 'Botol Roll On 10ml', category: 'bottle', price_per_ml: 0, price: 3500, capacity_ml: 10, stock_ml: 0, stock_pcs: 100 },
    { id: 'B008', name: 'Botol Atomizer 20ml', category: 'bottle', price_per_ml: 0, price: 6000, capacity_ml: 20, stock_ml: 0, stock_pcs: 90 },
  ];

  for (const p of products) {
    db.run(
      `INSERT OR IGNORE INTO products (id, name, category, price_per_ml, price, capacity_ml, stock_ml, stock_pcs)
       VALUES ('${p.id}', '${p.name}', '${p.category}', ${p.price_per_ml}, ${p.price}, ${p.capacity_ml}, ${p.stock_ml}, ${p.stock_pcs})`
    );
  }

  console.log('✅ Seed data inserted');
}

// ==================== CONSTANTS ====================
const ALCOHOL_PRICE_PER_ML = 2000;
const ALCOHOL_FREE_THRESHOLD_ML = 100;

// ==================== ROUTES ====================

/**
 * GET /products
 */
app.get('/products', (req, res) => {
  try {
    const results = db.exec('SELECT * FROM products ORDER BY category, name');

    if (results.length === 0) {
      return res.json({ success: true, data: { perfumes: [], bottles: [] } });
    }

    const columns = results[0].columns;
    const rows = results[0].values;

    const products = rows.map(row => {
      const obj = {};
      columns.forEach((col, i) => obj[col] = row[i]);
      return obj;
    });

    const perfumes = products
      .filter(p => p.category === 'perfume')
      .map(p => ({ id: p.id, name: p.name, price_per_ml: p.price_per_ml, stock_ml: p.stock_ml }));

    const bottles = products
      .filter(p => p.category === 'bottle')
      .map(p => ({ id: p.id, name: p.name, price: p.price, capacity_ml: p.capacity_ml, stock_pcs: p.stock_pcs }));

    res.json({ success: true, data: { perfumes, bottles } });
  } catch (err) {
    console.error('GET /products error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * POST /transaction
 * Body:
 * {
 *   items: [
 *     { product_id: "P001", quantity: 10, category: "perfume" },
 *     { product_id: "B005", quantity: 2, category: "bottle" }
 *   ],
 *   payment_method: "cash" | "qr",
 *   cash_received: 2000000
 * }
 */
app.post('/transaction', (req, res) => {
  try {
    const { items, payment_method, cash_received } = req.body;

    // Validation
    if (!items || !Array.isArray(items) || items.length === 0) {
      return res.status(400).json({ success: false, error: 'Items array is required' });
    }
    if (!payment_method || !['cash', 'qr'].includes(payment_method)) {
      return res.status(400).json({ success: false, error: 'payment_method must be "cash" or "qr"' });
    }

    let perfumeSubtotal = 0;
    let totalBibitMl = 0;
    let bottleSubtotal = 0;
    const insertedItems = [];
    const updatedStock = [];

    // Validate stock and calculate totals
    for (const item of items) {
      const productRes = db.exec(`SELECT * FROM products WHERE id = '${item.product_id}'`);
      if (productRes.length === 0 || productRes[0].values.length === 0) {
        return res.status(400).json({ success: false, error: `Product ${item.product_id} not found` });
      }

      const cols = productRes[0].columns;
      const row = productRes[0].values[0];
      const product = {};
      cols.forEach((col, i) => product[col] = row[i]);

      if (item.category === 'perfume') {
        if (item.quantity > product.stock_ml) {
          return res.status(400).json({
            success: false,
            error: `Insufficient stock for ${product.name}. Available: ${product.stock_ml} ml`
          });
        }
        perfumeSubtotal += item.quantity * product.price_per_ml;
        totalBibitMl += item.quantity;

        // Reduce stock
        db.run(`UPDATE products SET stock_ml = stock_ml - ${item.quantity} WHERE id = '${item.product_id}'`);

        insertedItems.push({
          product_id: item.product_id,
          product_name: product.name,
          category: item.category,
          quantity: item.quantity,
          subtotal: item.quantity * product.price_per_ml
        });

        updatedStock.push({
          product_id: item.product_id,
          product_name: product.name,
          stock_ml: product.stock_ml - item.quantity,
          stock_pcs: product.stock_pcs
        });

      } else if (item.category === 'bottle') {
        if (item.quantity > product.stock_pcs) {
          return res.status(400).json({
            success: false,
            error: `Insufficient stock for ${product.name}. Available: ${product.stock_pcs} pcs`
          });
        }
        bottleSubtotal += item.quantity * product.price;

        // Reduce stock
        db.run(`UPDATE products SET stock_pcs = stock_pcs - ${item.quantity} WHERE id = '${item.product_id}'`);

        insertedItems.push({
          product_id: item.product_id,
          product_name: product.name,
          category: item.category,
          quantity: item.quantity,
          subtotal: item.quantity * product.price
        });

        updatedStock.push({
          product_id: item.product_id,
          product_name: product.name,
          stock_ml: product.stock_ml,
          stock_pcs: product.stock_pcs - item.quantity
        });
      }
    }

    // Alcohol calculation
    const alcoholSubtotal = totalBibitMl < ALCOHOL_FREE_THRESHOLD_ML
      ? 0
      : totalBibitMl * ALCOHOL_PRICE_PER_ML;

    const total = perfumeSubtotal + alcoholSubtotal + bottleSubtotal;

    // Cash validation
    let cashReceived = 0;
    let changeAmount = 0;

    if (payment_method === 'cash') {
      cashReceived = cash_received || 0;
      if (cashReceived < total) {
        return res.status(400).json({
          success: false,
          error: `Insufficient payment. Need: Rp ${total.toLocaleString('id-ID')}`
        });
      }
      changeAmount = cashReceived - total;
    }

    // Insert transaction
    const itemsJson = JSON.stringify(items);
    db.run(`
      INSERT INTO transactions (items, perfume_subtotal, alcohol_subtotal, bottle_subtotal, total, payment_method, cash_received, change_amount)
      VALUES ('${itemsJson}', ${perfumeSubtotal}, ${alcoholSubtotal}, ${bottleSubtotal}, ${total}, '${payment_method}', ${cashReceived}, ${changeAmount})
    `);

    // Get last inserted transaction ID
    const txIdRes = db.exec('SELECT last_insert_rowid() as id');
    const transactionId = txIdRes[0].values[0][0];

    // Insert transaction items
    for (const item of insertedItems) {
      db.run(`
        INSERT INTO transaction_items (transaction_id, product_id, product_name, category, quantity, unit_price, subtotal)
        VALUES (${transactionId}, '${item.product_id}', '${item.product_name}', '${item.category}', ${item.quantity}, ${item.subtotal / item.quantity}, ${item.subtotal})
      `);
    }

    // Save to disk
    saveDB();

    res.status(201).json({
      success: true,
      data: {
        transaction_id: transactionId,
        items: insertedItems,
        summary: {
          perfume_subtotal: perfumeSubtotal,
          alcohol_ml: totalBibitMl,
          alcohol_subtotal: alcoholSubtotal,
          bottle_subtotal: bottleSubtotal,
          total,
          payment_method,
          cash_received: payment_method === 'cash' ? cashReceived : null,
          change: payment_method === 'cash' ? changeAmount : null
        },
        updated_stock: updatedStock
      }
    });

  } catch (err) {
    console.error('POST /transaction error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * GET /transactions?limit=50
 */
app.get('/transactions', (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 50;
    const results = db.exec(`SELECT * FROM transactions ORDER BY created_at DESC LIMIT ${limit}`);

    if (results.length === 0) {
      return res.json({ success: true, data: [] });
    }

    const columns = results[0].columns;
    const rows = results[0].values;

    const transactions = rows.map(row => {
      const obj = {};
      columns.forEach((col, i) => obj[col] = row[i]);
      try { obj.items = JSON.parse(obj.items); } catch (e) { }
      return obj;
    });

    res.json({ success: true, data: transactions });
  } catch (err) {
    console.error('GET /transactions error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * GET /stock
 */
app.get('/stock', (req, res) => {
  try {
    const results = db.exec(`SELECT id, name, category, stock_ml, stock_pcs FROM products ORDER BY category, name`);

    if (results.length === 0) {
      return res.json({ success: true, data: [] });
    }

    const columns = results[0].columns;
    const rows = results[0].values;
    const data = rows.map(row => {
      const obj = {};
      columns.forEach((col, i) => obj[col] = row[i]);
      return obj;
    });

    res.json({ success: true, data });
  } catch (err) {
    console.error('GET /stock error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// ==================== START SERVER ====================
initDB().then(() => {
  app.listen(PORT, HOST, () => {
    console.log(`
╔══════════════════════════════════════════╗
║   POS Backend Server                     ║
║   Running on http://localhost:${PORT}       ║
║   Also accessible via local IP           ║
║   Database: ${DB_PATH}    ║
╚══════════════════════════════════════════╝
    `);
  });
});

// Graceful shutdown
process.on('SIGINT', () => {
  saveDB();
  console.log('\n💾 Database saved. Server shut down.');
  process.exit(0);
});

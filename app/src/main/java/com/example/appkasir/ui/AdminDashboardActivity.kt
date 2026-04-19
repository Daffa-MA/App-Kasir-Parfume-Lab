package com.example.appkasir.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
import com.example.appkasir.data.CatalogRepository
import com.example.appkasir.data.local.AppDatabase
import com.example.appkasir.ui.adapter.AdminProductAdapter
import com.example.appkasir.ui.model.BottleProduct
import com.example.appkasir.ui.model.PerfumeProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var recyclerProducts: RecyclerView
    private lateinit var btnAddProduct: Button
    private lateinit var txtType: Spinner
    private lateinit var adapter: AdminProductAdapter

    private lateinit var catalogRepository: CatalogRepository
    private var currentType = "perfume" // perfume or bottle
    private var perfumeProducts = mutableListOf<PerfumeProduct>()
    private var bottleProducts = mutableListOf<BottleProduct>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user has admin role
        val sharedPref = getSharedPreferences("AppKasir", MODE_PRIVATE)
        val userRole = sharedPref.getString("userRole", "operator") ?: "operator"
        
        if (userRole != "admin") {
            Toast.makeText(this, "Access Denied! Only admins can access this.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setContentView(R.layout.activity_admin_dashboard)

        val database = AppDatabase.getInstance(this)
        catalogRepository = CatalogRepository(database.catalogDao())

        setupViews()
        loadProducts("perfume")
    }

    private fun setupViews() {
        recyclerProducts = findViewById(R.id.recyclerAdminProducts)
        btnAddProduct = findViewById(R.id.btnAddAdminProduct)
        txtType = findViewById(R.id.spinnerProductType)

        val types = arrayOf("Perfume", "Bottle")
        val spinner = ArrayAdapter(this, R.layout.spinner_item_modern, types)
        spinner.setDropDownViewResource(R.layout.spinner_dropdown_item_modern)
        txtType.adapter = spinner
        txtType.setSelection(0)

        // Setup spinner listener
        txtType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                currentType = if (position == 0) "perfume" else "bottle"
                loadProducts(currentType)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Setup recycler
        adapter = AdminProductAdapter(
            currentType = currentType,
            perfumes = perfumeProducts,
            bottles = bottleProducts,
            onEdit = { item -> showEditDialog(item) },
            onDelete = { item -> showDeleteConfirm(item) }
        )
        recyclerProducts.layoutManager = LinearLayoutManager(this)
        recyclerProducts.adapter = adapter

        // Setup button
        btnAddProduct.setOnClickListener {
            showAddProductDialog()
        }

        // Setup back button
        findViewById<android.widget.Button>(R.id.btnAdminBack).setOnClickListener {
            finish()
        }
        
        // Setup printer settings button
        findViewById<android.widget.Button>(R.id.btnPrinterSettings).setOnClickListener {
            startActivity(android.content.Intent(this, PrinterSettingsActivity::class.java))
        }
    }

    private fun loadProducts(type: String) {
        lifecycleScope.launch {
            try {
                if (type == "perfume") {
                    perfumeProducts.clear()
                    val perfumes = withContext(Dispatchers.IO) {
                        catalogRepository.getPerfumes()
                    }
                    perfumeProducts.addAll(perfumes)
                } else {
                    bottleProducts.clear()
                    val bottles = withContext(Dispatchers.IO) {
                        catalogRepository.getBottles()
                    }
                    bottleProducts.addAll(bottles)
                }
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Error loading products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddProductDialog() {
        val layout = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)
        val edtId = layout.findViewById<EditText>(R.id.edtProductId)
        val edtName = layout.findViewById<EditText>(R.id.edtProductName)
        val edtValue1 = layout.findViewById<EditText>(R.id.edtProductValue1) // pricePerMl or capacityMl
        val edtValue2 = layout.findViewById<EditText>(R.id.edtProductValue2) // stockMl or price
        val edtValue3 = layout.findViewById<EditText>(R.id.edtProductValue3) // stockPcs (only for bottle)
        val txtLabel1 = layout.findViewById<TextView>(R.id.txtLabel1)
        val txtLabel2 = layout.findViewById<TextView>(R.id.txtLabel2)
        val txtAddProductTitle = layout.findViewById<TextView>(R.id.txtAddProductTitle)
        val txtAddProductSubtitle = layout.findViewById<TextView>(R.id.txtAddProductSubtitle)
        val layoutValue3 = layout.findViewById<android.view.View>(R.id.layoutValue3)

        // Configure labels and visibility based on type
        if (currentType == "perfume") {
            txtLabel1.text = "Harga Per Ml (Rp)"
            txtLabel2.text = "Stock (ml)"
            layoutValue3.visibility = android.view.View.GONE
            edtValue1.hint = "ex: 1000"
            edtValue2.hint = "ex: 500"
        } else {
            txtLabel1.text = "Kapasitas (ml)"
            txtLabel2.text = "Harga (Rp)"
            layoutValue3.visibility = android.view.View.VISIBLE
            edtValue1.hint = "ex: 3"
            edtValue2.hint = "ex: 1500"
            edtValue3.hint = "ex: 100"
        }
        txtAddProductTitle.text = "Tambah ${if (currentType == "perfume") "Parfum" else "Botol"}"
        txtAddProductSubtitle.text = "Tambahkan data ${if (currentType == "perfume") "parfum" else "botol"} baru ke katalog"

        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null)
            .create()

        // Set button click listener for better control
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val id = edtId.text.toString().trim()
                val name = edtName.text.toString().trim()
                val value1 = edtValue1.text.toString().trim()
                val value2 = edtValue2.text.toString().trim()
                val value3 = edtValue3.text.toString().trim()

                // Validation
                if (id.isEmpty()) {
                    showError("ID produk tidak boleh kosong")
                    return@setOnClickListener
                }
                if (name.isEmpty()) {
                    showError("Nama produk tidak boleh kosong")
                    return@setOnClickListener
                }
                if (value1.isEmpty()) {
                    showError("${txtLabel1.text} tidak boleh kosong")
                    return@setOnClickListener
                }
                if (value2.isEmpty()) {
                    showError("${txtLabel2.text} tidak boleh kosong")
                    return@setOnClickListener
                }

                try {
                    if (currentType == "perfume") {
                        val pricePerMl = value1.toLong()
                        val stockMl = value2.toDouble()
                        
                        if (pricePerMl <= 0) {
                            showError("Harga per ml harus lebih dari 0")
                            return@setOnClickListener
                        }
                        if (stockMl <= 0) {
                            showError("Stock ml harus lebih dari 0")
                            return@setOnClickListener
                        }
                        
                        val perfume = PerfumeProduct(
                            id = id,
                            name = name,
                            pricePerMl = pricePerMl,
                            stockMl = stockMl
                        )
                        addPerfume(perfume)
                    } else {
                        if (value3.isEmpty()) {
                            showError("Stock (pcs) tidak boleh kosong")
                            return@setOnClickListener
                        }
                        
                        val capacityMl = value1.toInt()
                        val price = value2.toLong()
                        val stockPcs = value3.toInt()
                        
                        if (capacityMl <= 0) {
                            showError("Kapasitas harus lebih dari 0")
                            return@setOnClickListener
                        }
                        if (price <= 0) {
                            showError("Harga harus lebih dari 0")
                            return@setOnClickListener
                        }
                        if (stockPcs <= 0) {
                            showError("Stock pcs harus lebih dari 0")
                            return@setOnClickListener
                        }
                        
                        val bottle = BottleProduct(
                            id = id,
                            name = name,
                            capacityMl = capacityMl,
                            price = price,
                            stockPcs = stockPcs
                        )
                        addBottle(bottle)
                    }
                    
                    dialog.dismiss()
                } catch (e: NumberFormatException) {
                    showError("Input harus berupa angka yang valid")
                } catch (e: Exception) {
                    showError("Terjadi kesalahan: ${e.message}")
                }
            }
        }
        
        // Style dialog title with gold color
        val titleId = this.resources.getIdentifier("alertTitle", "id", "android")
        val titleView = dialog.findViewById<TextView>(titleId)
        titleView?.setTextColor(ContextCompat.getColor(this, R.color.pos_gold))
        
        dialog.show()
    }

    private fun showEditDialog(item: Any) {
        val isPerfume = item is PerfumeProduct
        val layout = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)
        val edtId = layout.findViewById<EditText>(R.id.edtProductId)
        val edtName = layout.findViewById<EditText>(R.id.edtProductName)
        val edtValue1 = layout.findViewById<EditText>(R.id.edtProductValue1)
        val edtValue2 = layout.findViewById<EditText>(R.id.edtProductValue2)
        val edtValue3 = layout.findViewById<EditText>(R.id.edtProductValue3)
        val txtLabel1 = layout.findViewById<TextView>(R.id.txtLabel1)
        val txtLabel2 = layout.findViewById<TextView>(R.id.txtLabel2)
        val txtLabel3 = layout.findViewById<TextView>(R.id.txtLabel3)
        val txtAddProductTitle = layout.findViewById<TextView>(R.id.txtAddProductTitle)
        val txtAddProductSubtitle = layout.findViewById<TextView>(R.id.txtAddProductSubtitle)

        if (isPerfume) {
            item as PerfumeProduct
            edtId.setText(item.id)
            edtId.isEnabled = false
            edtName.setText(item.name)
            edtValue1.setText(item.pricePerMl.toString())
            edtValue2.setText(item.stockMl.toString())
            txtLabel1.text = "Price Per Ml (Rp)"
            txtLabel2.text = "Stock (ml)"
            edtValue3.visibility = android.view.View.GONE
            txtLabel3.visibility = android.view.View.GONE
            txtAddProductTitle.text = "Edit Parfum"
            txtAddProductSubtitle.text = "Perbarui data parfum yang ada"
        } else {
            item as BottleProduct
            edtId.setText(item.id)
            edtId.isEnabled = false
            edtName.setText(item.name)
            edtValue1.setText(item.capacityMl.toString())
            edtValue2.setText(item.price.toString())
            edtValue3.setText(item.stockPcs.toString())
            txtLabel1.text = "Capacity (ml)"
            txtLabel2.text = "Price (Rp)"
            txtLabel3.text = "Stock (pcs)"
            txtAddProductTitle.text = "Edit Botol"
            txtAddProductSubtitle.text = "Perbarui data botol yang ada"
        }

        val editDialog = AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val name = edtName.text.toString().trim()
                val value1 = edtValue1.text.toString().trim()
                val value2 = edtValue2.text.toString().trim()
                val value3 = edtValue3.text.toString().trim()

                if (name.isEmpty() || value1.isEmpty() || value2.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        if (isPerfume) {
                            val perfume = (item as PerfumeProduct).copy(
                                name = name,
                                pricePerMl = value1.toLong(),
                                stockMl = value2.toDouble()
                            )
                            updatePerfume(perfume)
                        } else {
                            if (value3.isEmpty()) {
                                Toast.makeText(this, "Stock (pcs) is required", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            val bottle = (item as BottleProduct).copy(
                                name = name,
                                capacityMl = value1.toInt(),
                                price = value2.toLong(),
                                stockPcs = value3.toInt()
                            )
                            updateBottle(bottle)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        // Style dialog title with gold color
        val editTitleId = this.resources.getIdentifier("alertTitle", "id", "android")
        val editTitleView = editDialog.findViewById<TextView>(editTitleId)
        editTitleView?.setTextColor(ContextCompat.getColor(this, R.color.pos_gold))
        
        editDialog.show()
    }

    private fun showDeleteConfirm(item: Any) {
        val isPerfume = item is PerfumeProduct
        val name = if (isPerfume) (item as PerfumeProduct).name else (item as BottleProduct).name
        val id = if (isPerfume) (item as PerfumeProduct).id else (item as BottleProduct).id

        val deleteDialog = AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete \"$name\"?")
            .setPositiveButton("Delete") { _, _ ->
                if (isPerfume) {
                    deletePerfume(id)
                } else {
                    deleteBottle(id)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        // Style dialog title with gold color
        val deleteTitleId = this.resources.getIdentifier("alertTitle", "id", "android")
        val deleteTitleView = deleteDialog.findViewById<TextView>(deleteTitleId)
        deleteTitleView?.setTextColor(ContextCompat.getColor(this, R.color.pos_gold))
        
        deleteDialog.show()
    }

    private fun addPerfume(perfume: PerfumeProduct) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    catalogRepository.addPerfume(perfume)
                }
                Toast.makeText(this@AdminDashboardActivity, "Perfume added successfully", Toast.LENGTH_SHORT).show()
                loadProducts("perfume")
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePerfume(perfume: PerfumeProduct) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    catalogRepository.updatePerfume(perfume)
                }
                Toast.makeText(this@AdminDashboardActivity, "Perfume updated successfully", Toast.LENGTH_SHORT).show()
                loadProducts("perfume")
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deletePerfume(id: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    catalogRepository.deletePerfume(id)
                }
                Toast.makeText(this@AdminDashboardActivity, "Perfume deleted successfully", Toast.LENGTH_SHORT).show()
                loadProducts("perfume")
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addBottle(bottle: BottleProduct) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    catalogRepository.addBottle(bottle)
                }
                Toast.makeText(this@AdminDashboardActivity, "Bottle added successfully", Toast.LENGTH_SHORT).show()
                loadProducts("bottle")
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateBottle(bottle: BottleProduct) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    catalogRepository.updateBottle(bottle)
                }
                Toast.makeText(this@AdminDashboardActivity, "Bottle updated successfully", Toast.LENGTH_SHORT).show()
                loadProducts("bottle")
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteBottle(id: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    catalogRepository.deleteBottle(id)
                }
                Toast.makeText(this@AdminDashboardActivity, "Bottle deleted successfully", Toast.LENGTH_SHORT).show()
                loadProducts("bottle")
            } catch (e: Exception) {
                Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

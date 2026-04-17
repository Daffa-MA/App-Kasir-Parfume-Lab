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

        // Setup spinner for product type
        val types = arrayOf("Perfume", "Bottle")
        val spinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        spinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
        val txtLabel3 = layout.findViewById<TextView>(R.id.txtLabel3)

        // Configure labels based on type
        if (currentType == "perfume") {
            txtLabel1.text = "Price Per Ml (Rp)"
            txtLabel2.text = "Stock (ml)"
            edtValue3.visibility = android.view.View.GONE
            txtLabel3.visibility = android.view.View.GONE
        } else {
            txtLabel1.text = "Capacity (ml)"
            txtLabel2.text = "Price (Rp)"
            txtLabel3.text = "Stock (pcs)"
            edtValue3.visibility = android.view.View.VISIBLE
            txtLabel3.visibility = android.view.View.VISIBLE
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add ${if (currentType == "perfume") "Perfume" else "Bottle"}")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val id = edtId.text.toString().trim()
                val name = edtName.text.toString().trim()
                val value1 = edtValue1.text.toString().trim()
                val value2 = edtValue2.text.toString().trim()
                val value3 = edtValue3.text.toString().trim()

                if (id.isEmpty() || name.isEmpty() || value1.isEmpty() || value2.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        if (currentType == "perfume") {
                            val perfume = PerfumeProduct(
                                id = id,
                                name = name,
                                pricePerMl = value1.toLong(),
                                stockMl = value2.toDouble()
                            )
                            addPerfume(perfume)
                        } else {
                            if (value3.isEmpty()) {
                                Toast.makeText(this, "Stock (pcs) is required", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            val bottle = BottleProduct(
                                id = id,
                                name = name,
                                capacityMl = value1.toInt(),
                                price = value2.toLong(),
                                stockPcs = value3.toInt()
                            )
                            addBottle(bottle)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid input: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        }

        AlertDialog.Builder(this)
            .setTitle("Edit ${if (isPerfume) "Perfume" else "Bottle"}")
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
            .show()
    }

    private fun showDeleteConfirm(item: Any) {
        val isPerfume = item is PerfumeProduct
        val name = if (isPerfume) (item as PerfumeProduct).name else (item as BottleProduct).name
        val id = if (isPerfume) (item as PerfumeProduct).id else (item as BottleProduct).id

        AlertDialog.Builder(this)
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
            .show()
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
}

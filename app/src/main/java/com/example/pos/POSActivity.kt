package com.example.pos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
import com.example.appkasir.databinding.ActivityPosBinding
import com.example.pos.network.ApiClient
import com.example.pos.network.ApiPerfume
import com.example.pos.network.ApiBottle
import com.example.pos.network.TransactionItemRequest
import com.example.pos.network.TransactionRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

// ==================== DATA MODELS ====================

data class Product(
    val id: String,
    val name: String,
    val pricePerMl: Double,
    var stockMl: Double
)

data class Bottle(
    val id: String,
    val name: String,
    val capacityMl: Int,
    val price: Long,
    var stockPcs: Int
)

sealed class CartItem {
    abstract val subtotal: Long
    abstract val name: String
    abstract val detail: String
    abstract val isBottle: Boolean

    data class PerfumeItem(
        val product: Product,
        val ml: Double,
        override val subtotal: Long
    ) : CartItem() {
        override val name: String = product.name
        override val detail: String = "${ml.toInt()} ml"
        override val isBottle: Boolean = false
    }

    data class BottleItem(
        val bottle: Bottle,
        val qty: Int,
        override val subtotal: Long
    ) : CartItem() {
        override val name: String = bottle.name
        override val detail: String = "${qty} pcs"
        override val isBottle: Boolean = true
    }
}

// ==================== ALCOHOL CONFIG ====================

object AlcoholConfig {
    const val ALCOHOL_PRICE_PER_ML = 2000.0 // Rp per ml
    const val FREE_THRESHOLD_ML = 100.0     // Below this, alcohol is free
}

// ==================== ACTIVITY ====================

class POSActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPosBinding
    private val productList = mutableListOf<Product>()
    private val bottleList = mutableListOf<Bottle>()
    private val cartItems = mutableListOf<CartItem>()

    private var currentTab = Tab.PERFUME
    private var activeDialog: AlertDialog? = null

    private val paymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            cartItems.clear()
            productList.forEach { p -> p.stockMl = getInitialStock(p) }
            bottleList.forEach { b -> b.stockPcs = getInitialBottleStock(b) }
            refreshAll()
        }
    }

    // Store initial stock for reset
    private val initialProductStock = mutableMapOf<String, Double>()
    private val initialBottleStock = mutableMapOf<String, Int>()

    enum class Tab { PERFUME, BOTTLE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchProductsFromBackend()
        setupTabs()
        setupRecyclerViews()
        setupSearch()
        setupCheckoutButton()
    }

    private fun fetchProductsFromBackend() {
        ApiClient.instance.getProducts().enqueue(object : Callback<com.example.pos.network.ApiProductsResponse> {
            override fun onResponse(call: Call<com.example.pos.network.ApiProductsResponse>, response: Response<com.example.pos.network.ApiProductsResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data
                    productList.clear()
                    bottleList.clear()

                    for (p in data.perfumes) {
                        productList.add(Product(p.id, p.name, p.pricePerMl, p.stockMl))
                        initialProductStock[p.id] = p.stockMl
                    }
                    for (b in data.bottles) {
                        bottleList.add(Bottle(b.id, b.name, b.capacityMl, b.price, b.stockPcs))
                        initialBottleStock[b.id] = b.stockPcs
                    }

                    refreshAll()
                    Toast.makeText(this@POSActivity, "Loaded ${productList.size} perfumes, ${bottleList.size} bottles", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@POSActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.example.pos.network.ApiProductsResponse>, t: Throwable) {
                Toast.makeText(this@POSActivity, "Server not reachable. Using local data.", Toast.LENGTH_LONG).show()
                loadSampleData()
            }
        })
    }

    private fun loadSampleData() {
        productList.addAll(
            listOf(
                Product("P001", "Ocean Breeze", 15000.0, 500.0),
                Product("P002", "Rose Delight", 20000.0, 350.0),
                Product("P003", "Lavender Mist", 18000.0, 600.0),
                Product("P004", "Citrus Fresh", 12000.0, 800.0),
                Product("P005", "Vanilla Dream", 25000.0, 200.0),
                Product("P006", "Oud Royal", 50000.0, 150.0),
                Product("P007", "Jasmine Bloom", 22000.0, 400.0),
                Product("P008", "Sandalwood Pure", 30000.0, 250.0),
                Product("P009", "Musk Amber", 35000.0, 180.0),
                Product("P010", "Green Tea", 10000.0, 1000.0)
            )
        )
        bottleList.addAll(
            listOf(
                Bottle("B001", "Vial 3ml", 3, 1500, 200),
                Bottle("B002", "Vial 5ml", 5, 2000, 200),
                Bottle("B003", "Vial 10ml", 10, 3000, 150),
                Bottle("B004", "Botol Spray 15ml", 15, 5000, 100),
                Bottle("B005", "Botol Spray 30ml", 30, 7500, 80),
                Bottle("B006", "Botol Roll On 5ml", 5, 2500, 120),
                Bottle("B007", "Botol Roll On 10ml", 10, 3500, 100),
                Bottle("B008", "Botol Atomizer 20ml", 20, 6000, 90)
            )
        )
        productList.forEach { initialProductStock[it.id] = it.stockMl }
        bottleList.forEach { initialBottleStock[it.id] = it.stockPcs }
        refreshAll()
    }

    private fun getInitialStock(product: Product): Double = initialProductStock[product.id] ?: 0.0
    private fun getInitialBottleStock(bottle: Bottle): Int = initialBottleStock[bottle.id] ?: 0

    // ==================== TABS ====================

    private fun setupTabs() {
        binding.btnTabPerfume.setOnClickListener { switchTab(Tab.PERFUME) }
        binding.btnTabBottle.setOnClickListener { switchTab(Tab.BOTTLE) }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        if (tab == Tab.PERFUME) {
            binding.btnTabPerfume.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#D4AF37")
            )
            binding.btnTabPerfume.setTextColor(Color.parseColor("#121212"))
            binding.btnTabBottle.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#2A2A2A")
            )
            binding.btnTabBottle.setTextColor(Color.parseColor("#4A90D9"))
        } else {
            binding.btnTabBottle.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#4A90D9")
            )
            binding.btnTabBottle.setTextColor(Color.parseColor("#121212"))
            binding.btnTabPerfume.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#2A2A2A")
            )
            binding.btnTabPerfume.setTextColor(Color.parseColor("#D4AF37"))
        }
        refreshProductList()
    }

    private fun setupSearch() {
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                binding.btnClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                refreshProductList()
            }
        })

        binding.btnClearSearch.setOnClickListener {
            binding.edtSearch.text?.clear()
        }
    }

    private fun getSearchQuery(): String = binding.edtSearch.text.toString().trim()

    // ==================== RECYCLER VIEWS ====================

    private fun setupRecyclerViews() {
        binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerProducts.adapter = productAdapter

        binding.recyclerPerfumeCart.layoutManager = LinearLayoutManager(this)
        binding.recyclerPerfumeCart.adapter = perfumeCartAdapter

        binding.recyclerBottleCart.layoutManager = LinearLayoutManager(this)
        binding.recyclerBottleCart.adapter = bottleCartAdapter
    }

    // ==================== PRODUCT ADAPTER ====================

    private fun getFilteredProducts(): List<Product> {
        val query = getSearchQuery()
        return if (query.isEmpty()) productList
        else productList.filter { it.name.contains(query, ignoreCase = true) }
    }

    private fun getFilteredBottles(): List<Bottle> {
        val query = getSearchQuery()
        return if (query.isEmpty()) bottleList
        else bottleList.filter { it.name.contains(query, ignoreCase = true) }
    }

    private val productAdapter = object : RecyclerView.Adapter<ProductViewHolder>() {

        override fun getItemViewType(position: Int): Int {
            return if (currentTab == Tab.PERFUME) ProductViewHolder.TYPE_PERFUME
            else ProductViewHolder.TYPE_BOTTLE
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            return if (viewType == ProductViewHolder.TYPE_PERFUME) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_product, parent, false)
                ProductViewHolder(view, ProductViewHolder.TYPE_PERFUME)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_bottle, parent, false)
                ProductViewHolder(view, ProductViewHolder.TYPE_BOTTLE)
            }
        }

        override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
            if (holder.type != ProductViewHolder.TYPE_PERFUME && holder.type != ProductViewHolder.TYPE_BOTTLE) return

            if (holder.type == ProductViewHolder.TYPE_PERFUME) {
                val filtered = getFilteredProducts()
                if (position >= filtered.size) return
                val product = filtered[position]
                holder.txtProductName!!.text = product.name
                holder.txtPricePerMl!!.text = formatCurrency(product.pricePerMl.toLong()) + "/ml"

                if (product.stockMl <= 0) {
                    holder.itemView.alpha = 0.4f
                    holder.txtStock!!.text = "SOLD OUT"
                    holder.txtStock!!.setTextColor(Color.parseColor("#FF5252"))
                } else {
                    holder.itemView.alpha = 1.0f
                    holder.txtStock!!.text = "${product.stockMl.toInt()} ml"
                    holder.txtStock!!.setTextColor(Color.parseColor("#4CAF50"))
                }

                holder.itemView.setOnClickListener {
                    if (product.stockMl <= 0) {
                        showOutOfStockAlert(product.name)
                        return@setOnClickListener
                    }
                    showMlInputDialog(product)
                }
            } else {
                val filtered = getFilteredBottles()
                if (position >= filtered.size) return
                val bottle = filtered[position]
                holder.txtBottleName!!.text = bottle.name
                holder.txtBottleCapacity!!.text = "${bottle.capacityMl} ml"
                holder.txtBottlePrice!!.text = formatCurrency(bottle.price)

                if (bottle.stockPcs <= 0) {
                    holder.itemView.alpha = 0.4f
                    holder.txtBottleStock!!.text = "SOLD OUT"
                    holder.txtBottleStock!!.setTextColor(Color.parseColor("#FF5252"))
                } else {
                    holder.itemView.alpha = 1.0f
                    holder.txtBottleStock!!.text = "${bottle.stockPcs} pcs"
                    holder.txtBottleStock!!.setTextColor(Color.parseColor("#4CAF50"))
                }

                holder.itemView.setOnClickListener {
                    if (bottle.stockPcs <= 0) {
                        showOutOfStockAlert(bottle.name)
                        return@setOnClickListener
                    }
                    showBottleInputDialog(bottle)
                }
            }
        }

        override fun getItemCount() = if (currentTab == Tab.PERFUME) getFilteredProducts().size else getFilteredBottles().size
    }

    // ==================== CART ADAPTERS ====================

    private val perfumeCartAdapter = object : RecyclerView.Adapter<CartViewHolder>() {
        private val items: List<CartItem.PerfumeItem>
            get() = cartItems.filterIsInstance<CartItem.PerfumeItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cart, parent, false)
            return CartViewHolder(view)
        }

        override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
            val item = items[position]
            holder.txtCartItemName.text = item.name
            holder.txtCartItemDetail.text = item.detail
            holder.txtCartItemSubtotal.text = formatCurrency(item.subtotal)
            holder.txtCartItemIcon.text = "P"
            holder.txtCartItemIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#D4AF37")
            )
            holder.txtCartItemSubtotal.setTextColor(Color.parseColor("#D4AF37"))
            holder.btnRemoveItem.setOnClickListener { removeCartItem(item) }
        }

        override fun getItemCount() = items.size
    }

    private val bottleCartAdapter = object : RecyclerView.Adapter<CartViewHolder>() {
        private val items: List<CartItem.BottleItem>
            get() = cartItems.filterIsInstance<CartItem.BottleItem>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cart, parent, false)
            return CartViewHolder(view)
        }

        override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
            val item = items[position]
            holder.txtCartItemName.text = item.name
            holder.txtCartItemDetail.text = item.detail
            holder.txtCartItemSubtotal.text = formatCurrency(item.subtotal)
            holder.txtCartItemIcon.text = "B"
            holder.txtCartItemIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#4A90D9")
            )
            holder.txtCartItemSubtotal.setTextColor(Color.parseColor("#4A90D9"))
            holder.btnRemoveItem.setOnClickListener { removeCartItem(item) }
        }

        override fun getItemCount() = items.size
    }

    // ==================== CHECKOUT ====================

    private fun setupCheckoutButton() {
        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val total = calculateGrandTotal()
            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_TOTAL, total)
                putExtra(PaymentActivity.EXTRA_CART_ITEMS, buildCartItemsJson())
            }
            paymentLauncher.launch(intent)
        }
    }

    private fun buildCartItemsJson(): String {
        val items = cartItems.map { item ->
            when (item) {
                is CartItem.PerfumeItem -> {
                    """{"product_id":"${item.product.id}","quantity":${item.ml},"category":"perfume"}"""
                }
                is CartItem.BottleItem -> {
                    """{"product_id":"${item.bottle.id}","quantity":${item.qty},"category":"bottle"}"""
                }
            }
        }.joinToString(",")
        return "[$items]"
    }

    // ==================== CALCULATIONS ====================

    private fun calculatePerfumeSubtotal(): Long {
        return cartItems.filterIsInstance<CartItem.PerfumeItem>().sumOf { it.subtotal }
    }

    private fun calculateBottleSubtotal(): Long {
        return cartItems.filterIsInstance<CartItem.BottleItem>().sumOf { it.subtotal }
    }

    private fun calculateAlcoholSubtotal(): Long {
        val totalBibitMl = cartItems
            .filterIsInstance<CartItem.PerfumeItem>()
            .sumOf { it.ml }

        return if (totalBibitMl < AlcoholConfig.FREE_THRESHOLD_ML) {
            0L
        } else {
            (totalBibitMl * AlcoholConfig.ALCOHOL_PRICE_PER_ML).toLong()
        }
    }

    private fun getTotalBibitMl(): Double {
        return cartItems
            .filterIsInstance<CartItem.PerfumeItem>()
            .sumOf { it.ml }
    }

    private fun calculateGrandTotal(): Long {
        return calculatePerfumeSubtotal() + calculateAlcoholSubtotal() + calculateBottleSubtotal()
    }

    private fun updateCartTotals() {
        val perfumeSub = calculatePerfumeSubtotal()
        val alcoholMl = getTotalBibitMl()
        val alcoholSub = calculateAlcoholSubtotal()
        val bottleSub = calculateBottleSubtotal()
        val grandTotal = calculateGrandTotal()

        binding.txtPerfumeSubtotal.text = formatCurrency(perfumeSub)
        binding.txtAlcoholMl.text = "${alcoholMl.toInt()} ml"
        binding.txtAlcoholSubtotal.text = formatCurrency(alcoholSub)
        binding.txtBottleSubtotal.text = formatCurrency(bottleSub)
        binding.txtTotal.text = formatCurrency(grandTotal)

        // Show/hide empty state
        val hasItems = cartItems.isNotEmpty()
        // We use the scrollable section for display
    }

    private fun refreshAll() {
        refreshProductList()
        perfumeCartAdapter.notifyDataSetChanged()
        bottleCartAdapter.notifyDataSetChanged()
        updateCartTotals()
    }

    private fun refreshProductList() {
        productAdapter.notifyDataSetChanged()
    }

    // ==================== DIALOGS ====================

    private fun showOutOfStockAlert(productName: String) {
        AlertDialog.Builder(this)
            .setTitle("Out of Stock")
            .setMessage("Product \"$productName\" is out of stock!")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("OK", null)
            .create()
            .show()
    }

    private fun showMlInputDialog(product: Product) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_input_ml, null, false)

        val gridMlPresets = dialogView.findViewById<View>(R.id.gridMlPresets)
        val edtCustomMl = dialogView.findViewById<EditText>(R.id.edtCustomMl)
        val txtDialogProductName = dialogView.findViewById<TextView>(R.id.txtDialogProductName)
        val txtDialogMl = dialogView.findViewById<TextView>(R.id.txtDialogMl)
        val txtDialogPricePerMl = dialogView.findViewById<TextView>(R.id.txtDialogPricePerMl)
        val txtDialogSubtotal = dialogView.findViewById<TextView>(R.id.txtDialogSubtotal)
        val btnAddToCart = dialogView.findViewById<Button>(R.id.btnAddToCart)

        txtDialogProductName.text = product.name
        txtDialogPricePerMl.text = formatCurrency(product.pricePerMl.toLong()) + "/ml"

        val selectedMl = doubleArrayOf(-1.0)

        val presetButtons = listOf(
            R.id.btnMl3, R.id.btnMl5, R.id.btnMl10, R.id.btnMl15,
            R.id.btnMl20, R.id.btnMl30, R.id.btnMl50, R.id.btnMl100
        )

        presetButtons.forEach { btnId ->
            val btn = dialogView.findViewById<Button>(btnId)
            btn.setOnClickListener {
                selectedMl[0] = btn.text.toString().toDouble()
                edtCustomMl.text?.clear()
                updateCalculationPreview(
                    txtDialogMl, txtDialogSubtotal, product.pricePerMl.toLong(), selectedMl[0]
                )
                highlightSelectedButton(gridMlPresets, btn, presetButtons)
            }
        }

        edtCustomMl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                selectedMl[0] = s.toString().toDoubleOrNull() ?: -1.0
                if (selectedMl[0] > 0) {
                    updateCalculationPreview(
                        txtDialogMl, txtDialogSubtotal, product.pricePerMl.toLong(), selectedMl[0]
                    )
                    clearButtonHighlight(gridMlPresets, presetButtons)
                }
            }
        })

        btnAddToCart.setOnClickListener {
            if (selectedMl[0] <= 0) {
                Toast.makeText(this, "Please select or enter ml amount!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedMl[0] > product.stockMl) {
                Toast.makeText(this, "Insufficient stock!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val subtotal = (product.pricePerMl * selectedMl[0]).toLong()
            cartItems.add(CartItem.PerfumeItem(product, selectedMl[0], subtotal))
            product.stockMl -= selectedMl[0]
            refreshAll()

            Toast.makeText(this, "${product.name} (${selectedMl[0].toInt()} ml) added!", Toast.LENGTH_SHORT).show()
            activeDialog?.dismiss()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(product.name)
            .setView(dialogView)
            .create()
        activeDialog = dialog
        dialog.show()
    }

    private fun showBottleInputDialog(bottle: Bottle) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_bottle_qty, null, false)

        val edtBottleQty = dialogView.findViewById<EditText>(R.id.edtBottleQty)
        val txtDialogBottleName = dialogView.findViewById<TextView>(R.id.txtDialogBottleName)
        val txtDialogBottleQty = dialogView.findViewById<TextView>(R.id.txtDialogBottleQty)
        val txtDialogBottlePrice = dialogView.findViewById<TextView>(R.id.txtDialogBottlePrice)
        val txtDialogBottleSubtotal = dialogView.findViewById<TextView>(R.id.txtDialogBottleSubtotal)
        val btnAddToCart = dialogView.findViewById<Button>(R.id.btnAddBottleToCart)

        txtDialogBottleName.text = bottle.name
        txtDialogBottlePrice.text = formatCurrency(bottle.price) + "/pcs"

        val selectedQty = intArrayOf(0)

        fun updatePreview() {
            if (selectedQty[0] > 0) {
                txtDialogBottleQty.text = "${selectedQty[0]} pcs"
                txtDialogBottleSubtotal.text = formatCurrency(bottle.price * selectedQty[0])
            } else {
                txtDialogBottleQty.text = "-"
                txtDialogBottleSubtotal.text = "Rp 0"
            }
        }

        val presetBtns = listOf(
            dialogView.findViewById<Button>(R.id.btnBottleQty1) to 1,
            dialogView.findViewById<Button>(R.id.btnBottleQty2) to 2,
            dialogView.findViewById<Button>(R.id.btnBottleQty5) to 5,
            dialogView.findViewById<Button>(R.id.btnBottleQty10) to 10
        )

        presetBtns.forEach { (btn, qty) ->
            btn.setOnClickListener {
                selectedQty[0] = qty
                edtBottleQty.setText(qty.toString())
                updatePreview()
            }
        }

        edtBottleQty.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                selectedQty[0] = s.toString().toIntOrNull() ?: 0
                updatePreview()
            }
        })

        btnAddToCart.setOnClickListener {
            if (selectedQty[0] <= 0) {
                Toast.makeText(this, "Please enter quantity!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedQty[0] > bottle.stockPcs) {
                Toast.makeText(this, "Insufficient stock!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val subtotal = bottle.price * selectedQty[0]
            cartItems.add(CartItem.BottleItem(bottle, selectedQty[0], subtotal))
            bottle.stockPcs -= selectedQty[0]
            refreshAll()

            Toast.makeText(this, "${bottle.name} (${selectedQty[0]} pcs) added!", Toast.LENGTH_SHORT).show()
            activeDialog?.dismiss()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(bottle.name)
            .setView(dialogView)
            .create()
        activeDialog = dialog
        dialog.show()
    }

    // ==================== HELPERS ====================

    private fun removeCartItem(item: CartItem) {
        when (item) {
            is CartItem.PerfumeItem -> {
                item.product.stockMl += item.ml
            }
            is CartItem.BottleItem -> {
                item.bottle.stockPcs += item.qty
            }
        }
        cartItems.remove(item)
        refreshAll()
        Toast.makeText(this, "Item removed", Toast.LENGTH_SHORT).show()
    }

    private fun updateCalculationPreview(
        txtMl: TextView, txtSubtotal: TextView, pricePerMl: Long, ml: Double
    ) {
        txtMl.text = "$ml ml"
        txtSubtotal.text = formatCurrency((pricePerMl * ml).toLong())
    }

    private fun highlightSelectedButton(grid: View, selected: Button, ids: List<Int>) {
        ids.forEach { id ->
            val btn = grid.findViewById<Button>(id)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor(if (btn.id == selected.id) "#D4AF37" else "#1E1E1E")
            )
            btn.setTextColor(Color.parseColor(if (btn.id == selected.id) "#121212" else "#D4AF37"))
        }
    }

    private fun clearButtonHighlight(grid: View, ids: List<Int>) {
        ids.forEach { id ->
            val btn = grid.findViewById<Button>(id)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#1E1E1E")
            )
            btn.setTextColor(Color.parseColor("#D4AF37"))
        }
    }

    private fun formatCurrency(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }

    // ==================== VIEWHOLDERS ====================

    class ProductViewHolder(view: View, val type: Int) : RecyclerView.ViewHolder(view) {
        val txtProductName: TextView? = view.findViewById(R.id.txtProductName)
        val txtPricePerMl: TextView? = view.findViewById(R.id.txtPricePerMl)
        val txtStock: TextView? = view.findViewById(R.id.txtStock)

        val txtBottleName: TextView? = view.findViewById(R.id.txtBottleName)
        val txtBottleCapacity: TextView? = view.findViewById(R.id.txtBottleCapacity)
        val txtBottlePrice: TextView? = view.findViewById(R.id.txtBottlePrice)
        val txtBottleStock: TextView? = view.findViewById(R.id.txtBottleStock)

        companion object {
            const val TYPE_PERFUME = 1
            const val TYPE_BOTTLE = 2
        }
    }

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCartItemIcon: TextView = view.findViewById(R.id.txtCartItemIcon)
        val txtCartItemName: TextView = view.findViewById(R.id.txtCartItemName)
        val txtCartItemDetail: TextView = view.findViewById(R.id.txtCartItemDetail)
        val txtCartItemSubtotal: TextView = view.findViewById(R.id.txtCartItemSubtotal)
        val btnRemoveItem: ImageButton = view.findViewById(R.id.btnRemoveItem)
    }
}

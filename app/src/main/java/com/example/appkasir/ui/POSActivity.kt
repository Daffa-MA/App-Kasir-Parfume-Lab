package com.example.appkasir.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
import com.example.appkasir.data.CatalogRepository
import com.example.appkasir.data.LocalTransactionItem
import com.example.appkasir.data.TransactionRepository
import com.example.appkasir.data.local.AppDatabase
import com.example.appkasir.databinding.ActivityPosBinding
import com.example.appkasir.sync.SyncEngine
import com.example.appkasir.sync.SyncIndicator
import com.example.appkasir.sync.SyncUiState
import com.example.appkasir.sync.SyncWorker
import com.example.appkasir.ui.adapter.CartAdapter
import com.example.appkasir.ui.adapter.PerfumeMixAdapter
import com.example.appkasir.ui.adapter.PerfumeMixState
import com.example.appkasir.ui.adapter.ProductAdapter
import com.example.appkasir.ui.model.BottleProduct
import com.example.appkasir.ui.model.CartItem
import com.example.appkasir.ui.model.PerfumeProduct
import com.example.appkasir.ui.model.formatCurrency
import com.example.appkasir.utils.CalculateUtils
import com.example.appkasir.utils.PosTotals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class POSActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPosBinding
    private lateinit var repository: TransactionRepository
    private lateinit var catalogRepository: CatalogRepository

    private val perfumeProducts = mutableListOf<PerfumeProduct>()
    private val bottleProducts = mutableListOf<BottleProduct>()
    private val cartItems = mutableListOf<CartItem>()

    private var activeDialog: AlertDialog? = null
    private var syncStatusJob: Job? = null

    private var pendingCheckoutItems: List<CartItem> = emptyList()
    private var pendingCheckoutTotal: Long = 0L

    private val productAdapter = ProductAdapter { bottle ->
        if (bottle.stockPcs <= 0) {
            showInfo("Stock bottle habis")
            return@ProductAdapter
        }
        showBottleMixDialog(bottle)
    }

    private val perfumeCartAdapter = CartAdapter(
        { item -> removeCartItem(item) },
        { item -> editCartItem(item) }
    )

    private val bottleCartAdapter = CartAdapter(
        { item -> removeCartItem(item) },
        { item -> editCartItem(item) }
    )

    private val paymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val data = result.data
        val cashReceived = data?.getLongExtra(PaymentActivity.EXTRA_CASH_RECEIVED, 0L) ?: 0L
        val changeAmount = data?.getLongExtra(PaymentActivity.EXTRA_CHANGE_AMOUNT, 0L) ?: 0L
        val perfumeSnapshot = perfumeProducts.map { it.copy() }
        val bottleSnapshot = bottleProducts.map { it.copy() }

        lifecycleScope.launch {
            val itemsToPersist = pendingCheckoutItems.map {
                LocalTransactionItem(
                    name = it.name,
                    type = it.type,
                    qty = it.qty,
                    subtotal = it.subtotal
                )
            }

            withContext(Dispatchers.IO) {
                repository.savePendingTransaction(
                    total = pendingCheckoutTotal,
                    roundedTotal = pendingCheckoutTotal,
                    cashReceived = cashReceived,
                    changeAmount = changeAmount,
                    items = itemsToPersist
                )
                catalogRepository.saveCurrentStock(perfumeSnapshot, bottleSnapshot)
            }

            cartItems.clear()
            pendingCheckoutItems = emptyList()
            pendingCheckoutTotal = 0L
            refreshAll()

            // Sync Logic
            applySyncState(
                SyncUiState(
                    indicator = SyncIndicator.LOADING,
                    pendingCount = 0,
                    message = "Syncing..."
                )
            )
            SyncWorker.startNow(this@POSActivity)
            refreshSyncState()
            showInfo("Transaksi tersimpan offline")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getInstance(this)
        val dao = database.transactionDao()
        repository = TransactionRepository(dao)
        catalogRepository = CatalogRepository(database.catalogDao())

        setupTabs()
        setupUserInfo()
        setupSearch()
        setupRecyclerViews()
        setupCheckout()
        setupSyncActions()
        loadCatalogProducts()

        SyncWorker.startNow(this)
    }

    override fun onStart() {
        super.onStart()
        startSyncStatusPolling()
    }

    override fun onStop() {
        syncStatusJob?.cancel()
        super.onStop()
    }

    private fun loadCatalogProducts() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                catalogRepository.ensureSeeded()
            }

            val perfumes = withContext(Dispatchers.IO) {
                catalogRepository.getPerfumes()
            }
            val bottles = withContext(Dispatchers.IO) {
                catalogRepository.getBottles()
            }

            perfumeProducts.clear()
            perfumeProducts.addAll(perfumes)
            bottleProducts.clear()
            bottleProducts.addAll(bottles)
            refreshAll()
        }
    }

    private fun setupTabs() {
        binding.btnTabPerfume.visibility = View.VISIBLE
        binding.btnTabPerfume.text = "Sinkron"
        binding.btnTabPerfume.backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor("#4A90D9")
        )
        binding.btnTabPerfume.setTextColor(Color.parseColor("#0E1328"))
        binding.btnTabPerfume.setOnClickListener {
            triggerSyncNow()
        }

        binding.btnTabBottle.visibility = View.VISIBLE
        binding.btnTabBottle.text = "Riwayat"
        binding.btnTabBottle.backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor("#2A2A2A")
        )
        binding.btnTabBottle.setTextColor(Color.parseColor("#4A90D9"))
        binding.btnTabBottle.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        // Get user role
        val sharedPref = getSharedPreferences("AppKasir", MODE_PRIVATE)
        val userRole = sharedPref.getString("userRole", "operator") ?: "operator"
        val currentUser = sharedPref.getString("currentUser", "User") ?: "User"

        // Show admin button only for admin role
        if (userRole == "admin") {
            binding.btnAdmin.visibility = View.VISIBLE
            binding.btnAdmin.setOnClickListener {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
            }
        } else {
            binding.btnAdmin.visibility = View.GONE
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirm()
        }
    }

    private fun setupUserInfo() {
        val sharedPref = getSharedPreferences("AppKasir", MODE_PRIVATE)
        val currentUser = sharedPref.getString("currentUser", "User") ?: "User"
        val userRole = sharedPref.getString("userRole", "operator") ?: "operator"

        val txtUsername = findViewById<TextView?>(R.id.txtUsername)
        val txtUserRole = findViewById<TextView?>(R.id.txtUserRole)

        txtUsername?.text = currentUser
        txtUserRole?.text = userRole.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        // Apply role-based restrictions
        applyRoleBasedRestrictions(userRole)
    }

    private fun applyRoleBasedRestrictions(userRole: String) {
        when (userRole) {
            "admin" -> {
                // Admin has full access - no restrictions
            }
            "operator" -> {
                // Operator can do transactions but cannot access admin features
                binding.btnAdmin.visibility = View.GONE
            }
            "demo" -> {
                // Demo user has limited features
                binding.btnAdmin.visibility = View.GONE
                // Additional restrictions can be added here
            }
        }
    }

    private fun setupSearch() {
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                binding.btnClearSearch.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
                refreshProductList()
            }
        })

        binding.btnClearSearch.setOnClickListener {
            binding.edtSearch.text?.clear()
        }
    }

    private fun setupRecyclerViews() {
        val screenWidthDp = resources.configuration.screenWidthDp
        val spanCount = when {
            screenWidthDp >= 720 -> 3
            screenWidthDp >= 480 -> 2
            else -> 1
        }

        binding.recyclerProducts.layoutManager = GridLayoutManager(this, spanCount)
        binding.recyclerProducts.isNestedScrollingEnabled = false
        binding.recyclerProducts.adapter = productAdapter

        binding.recyclerPerfumeCart.layoutManager = LinearLayoutManager(this)
        binding.recyclerPerfumeCart.isNestedScrollingEnabled = false
        binding.recyclerPerfumeCart.adapter = perfumeCartAdapter

        binding.recyclerBottleCart.layoutManager = LinearLayoutManager(this)
        binding.recyclerBottleCart.isNestedScrollingEnabled = false
        binding.recyclerBottleCart.adapter = bottleCartAdapter
    }

    private fun setupCheckout() {
        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                showInfo("Cart kosong")
                return@setOnClickListener
            }

            // POS Logic
            val totals = CalculateUtils.calculateTotals(cartItems)
            pendingCheckoutItems = cartItems.toList()
            pendingCheckoutTotal = totals.roundedTotal

            val intent = Intent(this, PaymentActivity::class.java).apply {
                putExtra(PaymentActivity.EXTRA_TOTAL_AMOUNT, totals.roundedTotal)
            }
            paymentLauncher.launch(intent)
        }
    }

    private fun setupSyncActions() {
        binding.btnRetrySync.setOnClickListener {
            triggerSyncNow()
        }
    }

    private fun triggerSyncNow() {
        lifecycleScope.launch {
            applySyncState(
                SyncUiState(
                    indicator = SyncIndicator.LOADING,
                    pendingCount = 0,
                    message = "Syncing..."
                )
            )
            val result = withContext(Dispatchers.IO) {
                SyncEngine.syncPendingTransactions(this@POSActivity)
            }
            applySyncState(result)
        }
    }

    private fun refreshAll() {
        refreshProductList()
        refreshCartLists()
        refreshCartTotals()
    }

    private fun refreshProductList() {
        val query = binding.edtSearch.text?.toString().orEmpty().trim()
        val filtered = if (query.isBlank()) {
            bottleProducts
        } else {
            bottleProducts.filter { it.name.contains(query, ignoreCase = true) }
        }

        productAdapter.submitList(filtered)
    }

    private fun refreshCartLists() {
        val perfumeItems = cartItems.filterIsInstance<CartItem.Perfume>()
        val bottleItems = cartItems.filterIsInstance<CartItem.Bottle>()
        perfumeCartAdapter.submitList(perfumeItems)
        bottleCartAdapter.submitList(bottleItems)
    }

    private fun refreshCartTotals() {
        // POS Logic
        val totals = CalculateUtils.calculateTotals(cartItems)
        binding.txtPerfumeSubtotal.text = formatCurrency(totals.perfumeSubtotal)
        binding.txtAlcoholMl.text = "${formatMlValue(totals.totalAlcoholMl)} ml"
        binding.txtAlcoholSubtotal.text = formatCurrency(totals.alcoholSubtotal)
        binding.txtBottleSubtotal.text = formatCurrency(totals.bottleSubtotal)
        binding.txtTotal.text = formatCurrency(totals.roundedTotal)
    }

    private fun removeCartItem(item: CartItem) {
        when (item) {
            is CartItem.Perfume -> item.product.stockMl += item.ml
            is CartItem.Bottle -> item.bottle.stockPcs += item.pcs
        }
        cartItems.remove(item)
        refreshAll()
    }

    private fun editCartItem(item: CartItem) {
        val input = EditText(this).apply {
            setPadding(48, 36, 48, 24)
            setTextColor(Color.parseColor("#FFFFFF"))
            setHintTextColor(Color.parseColor("#8A8A8A"))
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setText(
                when (item) {
                    is CartItem.Perfume -> formatMlValue(item.ml)
                    is CartItem.Bottle -> item.pcs.toString()
                }
            )
            inputType = when (item) {
                is CartItem.Perfume -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                is CartItem.Bottle -> InputType.TYPE_CLASS_NUMBER
            }
            hint = when (item) {
                is CartItem.Perfume -> "ml"
                is CartItem.Bottle -> "pcs"
            }
            setSelection(text.length)
        }

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.card_with_shadow)
        }

        val titleView = TextView(this).apply {
            text = "Edit ${item.name}"
            setTextColor(Color.parseColor("#FFE047"))
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val subtitleView = TextView(this).apply {
            text = "Masukkan jumlah baru"
            setTextColor(Color.parseColor("#7F8FA3"))
            textSize = 12f
        }

        header.addView(titleView)
        header.addView(subtitleView)

        val inputContainer = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 12
        }

        dialogLayout.addView(header)
        dialogLayout.addView(input, inputContainer)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogLayout)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                when (item) {
                    is CartItem.Perfume -> {
                        val newMl = input.text.toString().trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                        if (newMl <= 0.0) {
                            showInfo("Jumlah ml harus lebih dari 0")
                            return@setOnClickListener
                        }

                        val availableMl = item.product.stockMl + item.ml
                        if (newMl > availableMl) {
                            showInfo("Stok tidak cukup")
                            return@setOnClickListener
                        }

                        item.product.stockMl = availableMl - newMl
                        val updatedItem = item.copy(
                            ml = newMl,
                            subtotal = (newMl * item.product.pricePerMl).toLong()
                        )
                        replaceCartItem(item, updatedItem)
                    }

                    is CartItem.Bottle -> {
                        val newQty = input.text.toString().trim().toIntOrNull() ?: 0
                        if (newQty <= 0) {
                            showInfo("Jumlah pcs harus lebih dari 0")
                            return@setOnClickListener
                        }

                        val availablePcs = item.bottle.stockPcs + item.pcs
                        if (newQty > availablePcs) {
                            showInfo("Stok bottle tidak cukup")
                            return@setOnClickListener
                        }

                        item.bottle.stockPcs = availablePcs - newQty
                        val updatedItem = item.copy(
                            pcs = newQty,
                            subtotal = item.bottle.price * newQty.toLong()
                        )
                        replaceCartItem(item, updatedItem)
                    }
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun replaceCartItem(oldItem: CartItem, newItem: CartItem) {
        val index = cartItems.indexOfFirst { it === oldItem }
        if (index >= 0) {
            cartItems[index] = newItem
            refreshAll()
        }
    }

    private fun showBottleMixDialog(bottle: BottleProduct) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bottle_mix, null, false)

        val txtMixBottleInfo = dialogView.findViewById<TextView>(R.id.txtMixBottleInfo)
        val edtMixSearch = dialogView.findViewById<EditText>(R.id.edtMixSearch)
        val recyclerMixPerfume = dialogView.findViewById<RecyclerView>(R.id.recyclerMixPerfume)
        val txtMixTotalMl = dialogView.findViewById<TextView>(R.id.txtMixTotalMl)
        val txtMixAlcoholInfo = dialogView.findViewById<TextView>(R.id.txtMixAlcoholInfo)
        val txtMixAlcoholPrice = dialogView.findViewById<TextView>(R.id.txtMixAlcoholPrice)
        val txtMixSummary = dialogView.findViewById<TextView>(R.id.txtMixSummary)
        val txtMixTotalPrice = dialogView.findViewById<TextView>(R.id.txtMixTotalPrice)
        val txtMixDialogTitle = dialogView.findViewById<TextView>(R.id.txtMixDialogTitle)
        val txtMixDialogSubtitle = dialogView.findViewById<TextView>(R.id.txtMixDialogSubtitle)
        val layoutAlcoholPrice = dialogView.findViewById<View>(R.id.layoutAlcoholPrice)
        val btnAddMixToCart = dialogView.findViewById<View>(R.id.btnAddMixToCart)

        txtMixDialogTitle.text = "Mix untuk ${bottle.name}"
        txtMixDialogSubtitle.text = "Pilih bibit parfum untuk campuran"
        txtMixBottleInfo.text = "${bottle.name} | ${formatCurrency(bottle.price)}"

        val mixAdapter = PerfumeMixAdapter { selectedItems ->
            val summary = calculateMixSummary(selectedItems, bottle)
            val isExceeded = summary.totalBibitMl > bottle.capacityMl
            val totalBibitMlText = formatMlValue(summary.totalBibitMl)
            val totalAlcoholMlText = formatMlValue(summary.totalAlcoholMl)
            
            // Display ml with warning if exceeded
            val mlText = "$totalBibitMlText ml"
            txtMixTotalMl.text = if (isExceeded) {
                "$mlText (Melebihi kapasitas ${bottle.capacityMl}ml!)"
            } else {
                mlText
            }
            
            // Change text color to red if exceeded
            txtMixTotalMl.setTextColor(
                if (isExceeded) Color.parseColor("#FF5252") else Color.parseColor("#FFFFFF")
            )

            if (summary.totalAlcoholMl > 0.0) {
                layoutAlcoholPrice.visibility = View.VISIBLE
                txtMixAlcoholInfo.text = "$totalAlcoholMlText ml"
                txtMixAlcoholPrice.text = formatCurrency(summary.alcoholSubtotal)
            } else {
                layoutAlcoholPrice.visibility = View.GONE
                txtMixAlcoholInfo.text = "0 ml"
                txtMixAlcoholPrice.text = formatCurrency(0)
            }

            txtMixSummary.text = "${formatCurrency(summary.perfumeSubtotal)} + ${formatCurrency(summary.alcoholSubtotal)}"
            txtMixTotalPrice.text = formatCurrency(summary.total)
            
            // Disable button if exceeds capacity
            btnAddMixToCart.isEnabled = !isExceeded
            btnAddMixToCart.alpha = if (isExceeded) 0.5f else 1.0f
        }

        recyclerMixPerfume.layoutManager = LinearLayoutManager(this)
        recyclerMixPerfume.adapter = mixAdapter
        mixAdapter.submitProducts(perfumeProducts)

        edtMixSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                mixAdapter.filter(s?.toString().orEmpty())
            }
        })

        btnAddMixToCart.setOnClickListener {
            if (bottle.stockPcs <= 0) {
                showInfo("Stock bottle habis")
                return@setOnClickListener
            }

            val selectedMix = mixAdapter.selectedItems()
            if (selectedMix.isEmpty()) {
                showInfo("Pilih minimal 1 bibit parfum")
                return@setOnClickListener
            }

            val totalBibitMl = selectedMix.sumOf { it.selectedMl }
            if (totalBibitMl > bottle.capacityMl) {
                showInfo("Total bibit (${formatMlValue(totalBibitMl)}ml) melebihi kapasitas botol (${bottle.capacityMl}ml)")
                return@setOnClickListener
            }

            val hasInvalidMl = selectedMix.any { it.selectedMl <= 0 || it.selectedMl > it.product.stockMl }
            if (hasInvalidMl) {
                showInfo("Periksa jumlah ml mix, stock tidak mencukupi")
                return@setOnClickListener
            }

            selectedMix.forEach { mix ->
                val perfumeSubtotal = (mix.selectedMl * mix.product.pricePerMl).toLong()
                cartItems.add(CartItem.Perfume(mix.product, mix.selectedMl, perfumeSubtotal))
                mix.product.stockMl -= mix.selectedMl
            }

            cartItems.add(CartItem.Bottle(bottle, 1, bottle.price))
            bottle.stockPcs -= 1

            refreshAll()
            activeDialog?.dismiss()
            showInfo("Mix berhasil ditambahkan ke cart")
        }

        activeDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        activeDialog?.show()
    }

    private fun showLogoutConfirm() {
        val logoutDialog = AlertDialog.Builder(this)
            .setMessage("Apakah Anda yakin ingin logout?")
            .setPositiveButton("Ya, Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Batal", null)
            .create()

        logoutDialog.show()
    }

    private fun performLogout() {
        // Clear login state
        val sharedPref = getSharedPreferences("AppKasir", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isLoggedIn", false)
            putString("currentUser", "")
            putString("userRole", "")
            apply()
        }

        Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun calculateMixSummary(
        selectedMix: List<PerfumeMixState>,
        bottle: BottleProduct
    ): PosTotals {
        val perfumeSubtotal = selectedMix.sumOf { (it.selectedMl * it.product.pricePerMl).toLong() }
        val totalBibitMl = selectedMix.sumOf { it.selectedMl }
        val totalAlcoholMl = (bottle.capacityMl - totalBibitMl).coerceAtLeast(0.0)
        val alcoholSubtotal = CalculateUtils.calculateAlcoholSubtotal(
            totalBibitMl = totalBibitMl,
            totalAlcoholMl = totalAlcoholMl,
            alcoholPricePerMl = 2000.0
        )

        val bottleSubtotal = bottle.price
        val total = perfumeSubtotal + alcoholSubtotal + bottleSubtotal

        return PosTotals(
            perfumeSubtotal = perfumeSubtotal,
            alcoholSubtotal = alcoholSubtotal,
            bottleSubtotal = bottleSubtotal,
            total = total,
            roundedTotal = CalculateUtils.roundToNearestHundred(total),
            totalBibitMl = totalBibitMl,
            totalAlcoholMl = totalAlcoholMl
        )
    }

    private fun formatMlValue(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString().trimEnd('0').trimEnd('.')
        }
    }

    private fun startSyncStatusPolling() {
        syncStatusJob?.cancel()
        syncStatusJob = lifecycleScope.launch {
            while (isActive) {
                refreshSyncState()
                delay(5000)
            }
        }
    }

    private suspend fun refreshSyncState() {
        val state = withContext(Dispatchers.IO) {
            SyncEngine.currentState(this@POSActivity)
        }
        applySyncState(state)
    }

    private fun applySyncState(state: SyncUiState) {
        val color = when (state.indicator) {
            SyncIndicator.SYNCED -> Color.parseColor("#4CAF50")
            SyncIndicator.PENDING -> Color.parseColor("#FFC107")
            SyncIndicator.OFFLINE -> Color.parseColor("#FF5252")
            SyncIndicator.ERROR -> Color.parseColor("#FF5252")
            SyncIndicator.LOADING -> Color.parseColor("#AAAAAA")
        }

        binding.txtSyncStatus.text = when (state.indicator) {
            SyncIndicator.SYNCED -> "SYNC: SYNCED"
            SyncIndicator.PENDING -> "SYNC: PENDING (${state.pendingCount})"
            SyncIndicator.OFFLINE -> "SYNC: OFFLINE"
            SyncIndicator.ERROR -> "SYNC: ERROR (${state.pendingCount})"
            SyncIndicator.LOADING -> "SYNC: LOADING"
        }
        binding.txtSyncStatus.setTextColor(color)

        val isLoading = state.indicator == SyncIndicator.LOADING
        binding.progressSync.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRetrySync.visibility = if (state.indicator == SyncIndicator.SYNCED) View.GONE else View.VISIBLE
    }

    private fun showInfo(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

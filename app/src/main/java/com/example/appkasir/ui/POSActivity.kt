package com.example.appkasir.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
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

    private val perfumeCartAdapter = CartAdapter { item ->
        removeCartItem(item)
    }

    private val bottleCartAdapter = CartAdapter { item ->
        removeCartItem(item)
    }

    private val paymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val data = result.data
        val cashReceived = data?.getLongExtra(PaymentActivity.EXTRA_CASH_RECEIVED, 0L) ?: 0L
        val changeAmount = data?.getLongExtra(PaymentActivity.EXTRA_CHANGE_AMOUNT, 0L) ?: 0L

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

        val dao = AppDatabase.getInstance(this).transactionDao()
        repository = TransactionRepository(dao)

        loadSampleProducts()
        setupTabs()
        setupSearch()
        setupRecyclerViews()
        setupCheckout()
        setupSyncActions()

        refreshAll()
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

    private fun loadSampleProducts() {
        perfumeProducts.clear()
        bottleProducts.clear()

        perfumeProducts += listOf(
            PerfumeProduct("P001", "Ocean Breeze", 15000, 500.0),
            PerfumeProduct("P002", "Rose Delight", 20000, 350.0),
            PerfumeProduct("P003", "Lavender Mist", 18000, 600.0),
            PerfumeProduct("P004", "Citrus Fresh", 12000, 800.0),
            PerfumeProduct("P005", "Vanilla Dream", 25000, 200.0),
            PerfumeProduct("P006", "Oud Royal", 50000, 150.0)
        )

        bottleProducts += listOf(
            BottleProduct("B001", "Vial 3ml", 3, 1500, 200),
            BottleProduct("B002", "Vial 5ml", 5, 2000, 200),
            BottleProduct("B003", "Vial 10ml", 10, 3000, 150),
            BottleProduct("B004", "Bottle Spray 15ml", 15, 5000, 100),
            BottleProduct("B005", "Bottle Spray 30ml", 30, 7500, 80)
        )
    }

    private fun setupTabs() {
        binding.btnTabPerfume.text = "CATALOG MIX BOTTLE"
        binding.btnTabPerfume.isEnabled = false
        binding.btnTabPerfume.backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor("#4A90D9")
        )
        binding.btnTabPerfume.setTextColor(Color.parseColor("#121212"))

        binding.btnTabBottle.visibility = View.GONE
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
        binding.recyclerProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerProducts.adapter = productAdapter

        binding.recyclerPerfumeCart.layoutManager = LinearLayoutManager(this)
        binding.recyclerPerfumeCart.adapter = perfumeCartAdapter

        binding.recyclerBottleCart.layoutManager = LinearLayoutManager(this)
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
        binding.txtAlcoholMl.text = "${totals.totalAlcoholMl.toInt()} ml"
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
        val layoutAlcoholPrice = dialogView.findViewById<View>(R.id.layoutAlcoholPrice)
        val btnAddMixToCart = dialogView.findViewById<View>(R.id.btnAddMixToCart)

        txtMixBottleInfo.text = "${bottle.name} | ${formatCurrency(bottle.price)}"

        val mixAdapter = PerfumeMixAdapter { selectedItems ->
            val summary = calculateMixSummary(selectedItems, bottle)
            txtMixTotalMl.text = "${summary.totalBibitMl.toInt()} ml"

            if (summary.totalBibitMl > 100.0) {
                layoutAlcoholPrice.visibility = View.VISIBLE
                txtMixAlcoholInfo.text = "${summary.totalBibitMl.toInt()} ml"
                txtMixAlcoholPrice.text = formatCurrency(summary.alcoholSubtotal)
            } else {
                layoutAlcoholPrice.visibility = View.GONE
                txtMixAlcoholInfo.text = "0 ml"
                txtMixAlcoholPrice.text = formatCurrency(0)
            }

            txtMixSummary.text = "${formatCurrency(summary.perfumeSubtotal)} + ${formatCurrency(summary.bottleSubtotal)}"
            txtMixTotalPrice.text = formatCurrency(summary.total)
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
            .setTitle("Mix untuk ${bottle.name}")
            .setView(dialogView)
            .create()
        activeDialog?.show()
    }

    private fun calculateMixSummary(
        selectedMix: List<PerfumeMixState>,
        bottle: BottleProduct
    ): PosTotals {
        val perfumeSubtotal = selectedMix.sumOf { (it.selectedMl * it.product.pricePerMl).toLong() }
        val totalBibitMl = selectedMix.sumOf { it.selectedMl }
        val alcoholSubtotal = CalculateUtils.calculateAlcoholSubtotal(
            totalBibitMl = totalBibitMl,
            totalAlcoholMl = totalBibitMl,
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
            totalAlcoholMl = totalBibitMl
        )
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

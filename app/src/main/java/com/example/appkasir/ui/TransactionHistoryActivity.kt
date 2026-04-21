package com.example.appkasir.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appkasir.data.TransactionRepository
import com.example.appkasir.data.local.AppDatabase
import com.example.appkasir.data.local.entity.TransactionWithItems
import com.example.appkasir.databinding.ActivityTransactionHistoryBinding
import com.example.appkasir.ui.adapter.TransactionHistoryAdapter
import com.example.appkasir.ui.model.formatCurrency
import com.example.appkasir.ui.model.formatQuantity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var repository: TransactionRepository
    private var allTransactions: List<TransactionWithItems> = emptyList()
    private var selectedTransactionId: Long? = null
    private var activeStatusFilter: String? = null
    private var searchQuery: String = ""

    private val adapter = TransactionHistoryAdapter { transaction ->
        onTransactionSelected(transaction)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = TransactionRepository(AppDatabase.getInstance(this).transactionDao())

        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter
        binding.btnBackHistory.setOnClickListener { finish() }
        binding.btnFilterAll.setOnClickListener {
            activeStatusFilter = null
            updateFilterButtons()
            applyFiltersAndRender()
        }
        binding.btnFilterPending.setOnClickListener {
            activeStatusFilter = "PENDING"
            updateFilterButtons()
            applyFiltersAndRender()
        }
        binding.btnFilterSynced.setOnClickListener {
            activeStatusFilter = "SYNCED"
            updateFilterButtons()
            applyFiltersAndRender()
        }
        binding.edtSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim().orEmpty()
                applyFiltersAndRender()
            }
        })
        binding.btnPrintDetail.setOnClickListener {
            val id = selectedTransactionId
            if (id == null) {
                Toast.makeText(this, "Pilih transaksi terlebih dahulu", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cetak transaksi #$id", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnSendReceipt.setOnClickListener {
            val id = selectedTransactionId
            if (id == null) {
                Toast.makeText(this, "Pilih transaksi terlebih dahulu", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Struk digital untuk #$id siap dikirim", Toast.LENGTH_SHORT).show()
            }
        }

        updateFilterButtons()
        bindEmptyDetailPanel()
        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                repository.getAllTransactions()
            }
            allTransactions = items
            applyFiltersAndRender()
        }
    }

    private fun applyFiltersAndRender() {
        val filtered = allTransactions.filter { transaction ->
            val statusMatch = activeStatusFilter == null || transaction.transaction.status == activeStatusFilter
            val searchMatch = if (searchQuery.isBlank()) {
                true
            } else {
                val query = searchQuery.lowercase(Locale.getDefault())
                val idMatch = "#${transaction.transaction.id}".contains(query)
                val itemsMatch = transaction.items.any { it.name.lowercase(Locale.getDefault()).contains(query) }
                idMatch || itemsMatch
            }
            statusMatch && searchMatch
        }

        adapter.submitList(filtered)
        binding.txtEmptyHistory.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.txtHistoryMeta.text = if (allTransactions.isEmpty()) {
            "Belum ada data transaksi"
        } else {
            "Menampilkan ${filtered.size} dari ${allTransactions.size} transaksi"
        }

        if (filtered.isEmpty()) {
            selectedTransactionId = null
            adapter.setSelectedTransactionId(null)
            bindEmptyDetailPanel()
            return
        }

        val selected = filtered.find { it.transaction.id == selectedTransactionId } ?: filtered.first().also {
            selectedTransactionId = it.transaction.id
        }
        adapter.setSelectedTransactionId(selected.transaction.id)
        bindDetailPanel(selected)
    }

    private fun onTransactionSelected(transaction: TransactionWithItems) {
        selectedTransactionId = transaction.transaction.id
        adapter.setSelectedTransactionId(selectedTransactionId)
        bindDetailPanel(transaction)
    }

    private fun bindDetailPanel(transaction: TransactionWithItems) {
        val dateText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(transaction.transaction.createdAt))
        val ref = "TRX-${transaction.transaction.id.toString().padStart(5, '0')}"
        val itemSummary = if (transaction.items.isEmpty()) {
            "-"
        } else {
            transaction.items.joinToString("\n") {
                "• ${it.name} x${formatQuantity(it.qty)} (${formatCurrency(it.subtotal)})"
            }
        }
        val statusColor = when (transaction.transaction.status) {
            "SYNCED" -> Color.parseColor("#FFE082")
            "PENDING" -> Color.parseColor("#D4AF37")
            else -> Color.parseColor("#A67C00")
        }

        binding.txtDetailRef.text = ref
        binding.txtDetailDate.text = dateText
        binding.txtDetailStatus.text = transaction.transaction.status
        binding.txtDetailStatus.setTextColor(statusColor)
        binding.txtDetailItems.text = itemSummary
        binding.txtDetailCash.text = formatCurrency(transaction.transaction.cashReceived)
        binding.txtDetailChange.text = formatCurrency(transaction.transaction.changeAmount)
        binding.txtDetailGrandTotal.text = formatCurrency(transaction.transaction.roundedTotal)
    }

    private fun bindEmptyDetailPanel() {
        binding.txtDetailRef.text = "TRX-00000"
        binding.txtDetailDate.text = "-"
        binding.txtDetailStatus.text = "Belum dipilih"
        binding.txtDetailStatus.setTextColor(Color.parseColor("#B88A1D"))
        binding.txtDetailItems.text = "-"
        binding.txtDetailCash.text = formatCurrency(0)
        binding.txtDetailChange.text = formatCurrency(0)
        binding.txtDetailGrandTotal.text = formatCurrency(0)
    }

    private fun updateFilterButtons() {
        applyFilterButtonStyle(binding.btnFilterAll, activeStatusFilter == null)
        applyFilterButtonStyle(binding.btnFilterPending, activeStatusFilter == "PENDING")
        applyFilterButtonStyle(binding.btnFilterSynced, activeStatusFilter == "SYNCED")
    }

    private fun applyFilterButtonStyle(button: com.google.android.material.button.MaterialButton, isActive: Boolean) {
        if (isActive) {
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2AD4AF37"))
            button.strokeColor = ColorStateList.valueOf(Color.parseColor("#D4AF37"))
            button.setTextColor(Color.parseColor("#FFE082"))
        } else {
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#101010"))
            button.strokeColor = ColorStateList.valueOf(Color.parseColor("#4A3A10"))
            button.setTextColor(Color.parseColor("#B88A1D"))
        }
    }
}

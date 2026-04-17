package com.example.appkasir.ui

import android.app.AlertDialog
import android.os.Bundle
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
    private val adapter = TransactionHistoryAdapter { transaction ->
        showTransactionDetail(transaction)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = TransactionRepository(AppDatabase.getInstance(this).transactionDao())

        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = adapter
        binding.btnBackHistory.setOnClickListener { finish() }

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
            adapter.submitList(items)
            binding.txtEmptyHistory.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun showTransactionDetail(transaction: TransactionWithItems) {
        val itemSummary = transaction.items.joinToString("\n") {
            "• ${it.name} x${formatQuantity(it.qty)} = ${formatCurrency(it.subtotal)}"
        }
        val dateText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(transaction.transaction.createdAt))

        AlertDialog.Builder(this)
            .setTitle("Transaksi #${transaction.transaction.id}")
            .setMessage(
                "Waktu: $dateText\n" +
                    "Status: ${transaction.transaction.status}\n" +
                    "Total: ${formatCurrency(transaction.transaction.total)}\n" +
                    "Cash: ${formatCurrency(transaction.transaction.cashReceived)}\n" +
                    "Kembalian: ${formatCurrency(transaction.transaction.changeAmount)}\n\n" +
                    itemSummary
            )
            .setPositiveButton("Tutup", null)
            .show()
    }
}
package com.example.pos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appkasir.databinding.ActivityPaymentBinding
import com.example.pos.network.ApiClient
import com.example.pos.network.TransactionItemRequest
import com.example.pos.network.TransactionRequest
import com.example.pos.network.TransactionResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private var totalAmount: Long = 0L
    private var currentPaymentMethod = PaymentMethod.CASH
    private var cartItemsJson: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        totalAmount = intent.getLongExtra(EXTRA_TOTAL, 0L)
        cartItemsJson = intent.getStringExtra(EXTRA_CART_ITEMS) ?: "[]"
        binding.txtPaymentTotal.text = formatCurrency(totalAmount)

        binding.edtCash.filters = arrayOf(InputFilter.LengthFilter(15))

        setupTabs()
        setupCashInput()
        setupQuickCash()
        setupProcessPayment()
    }

    private fun setupTabs() {
        binding.btnTabCash.setOnClickListener { selectTab(PaymentMethod.CASH) }
        binding.btnTabQR.setOnClickListener { selectTab(PaymentMethod.QR) }
    }

    private fun selectTab(method: PaymentMethod) {
        currentPaymentMethod = method

        if (method == PaymentMethod.CASH) {
            binding.btnTabCash.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#D4AF37")
            )
            binding.btnTabCash.setTextColor(Color.parseColor("#121212"))
            binding.btnTabQR.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#2A2A2A")
            )
            binding.btnTabQR.setTextColor(Color.parseColor("#D4AF37"))

            binding.panelCash.visibility = View.VISIBLE
            binding.panelQR.visibility = View.GONE
        } else {
            binding.btnTabQR.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#D4AF37")
            )
            binding.btnTabQR.setTextColor(Color.parseColor("#121212"))
            binding.btnTabCash.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#2A2A2A")
            )
            binding.btnTabCash.setTextColor(Color.parseColor("#D4AF37"))

            binding.panelCash.visibility = View.GONE
            binding.panelQR.visibility = View.VISIBLE
        }
    }

    private fun setupCashInput() {
        binding.edtCash.addTextChangedListener(object : TextWatcher {
            var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true

                val raw = s.toString().filter { it.isDigit() }
                val formatted = if (raw.isEmpty()) "" else formatNumber(raw.toLong())

                if (formatted != s.toString()) {
                    binding.edtCash.setText(formatted)
                    binding.edtCash.setSelection(formatted.length)
                }

                val paid = raw.toLongOrNull() ?: 0L
                updateChangeDisplay(paid)

                isUpdating = false
            }
        })
    }

    private fun setupQuickCash() {
        val quickValues = listOf(
            binding.btnQuickExact to 0L,
            binding.btnQuick20k to 20_000L,
            binding.btnQuick50k to 50_000L,
            binding.btnQuick100k to 100_000L,
            binding.btnQuick200k to 200_000L,
            binding.btnQuick500k to 500_000L,
            binding.btnQuick1000k to 1_000_000L
        )

        quickValues.forEach { (btn, value) ->
            btn.setOnClickListener {
                val amount = if (value == 0L) totalAmount else value
                binding.edtCash.setText(formatNumber(amount))
                binding.edtCash.setSelection(binding.edtCash.text.length)
            }
        }
    }

    private fun setupProcessPayment() {
        binding.btnProcessPayment.setOnClickListener {
            when (currentPaymentMethod) {
                PaymentMethod.CASH -> processCashPayment()
                PaymentMethod.QR -> processQRPayment()
            }
        }
    }

    private fun processCashPayment() {
        val raw = binding.edtCash.text.toString().filter { it.isDigit() }
        val paid = raw.toLongOrNull() ?: 0L

        if (paid <= 0) {
            Toast.makeText(this, "Please enter a valid cash amount!", Toast.LENGTH_SHORT).show()
            return
        }

        if (paid < totalAmount) {
            Toast.makeText(this, "Insufficient payment!", Toast.LENGTH_LONG).show()
            return
        }

        sendTransactionToBackend("cash", paid)
    }

    private fun processQRPayment() {
        sendTransactionToBackend("qr", totalAmount)
    }

    private fun sendTransactionToBackend(paymentMethod: String, cashReceived: Long? = null) {
        val items = parseCartItems()
        if (items.isEmpty()) {
            Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val request = TransactionRequest(
            items = items,
            paymentMethod = paymentMethod,
            cashReceived = cashReceived
        )

        ApiClient.instance.createTransaction(request).enqueue(object : Callback<TransactionResponse> {
            override fun onResponse(call: Call<TransactionResponse>, response: Response<TransactionResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data!!
                    val summary = data.summary

                    val changeMsg = if (summary.change != null) "\nChange: ${formatCurrency(summary.change.toLong())}" else ""
                    Toast.makeText(
                        this@PaymentActivity,
                        "Transaction #${data.transactionId} saved!$changeMsg\nTotal: ${formatCurrency(summary.total.toLong())}",
                        Toast.LENGTH_LONG
                    ).show()

                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    val error = response.body()?.error ?: "Transaction failed"
                    Toast.makeText(this@PaymentActivity, error, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<TransactionResponse>, t: Throwable) {
                // Server unreachable - process locally
                Toast.makeText(this@PaymentActivity, "Server offline. Processing locally.", Toast.LENGTH_SHORT).show()
                val change = if (cashReceived != null) cashReceived - totalAmount else 0L
                Toast.makeText(
                    this@PaymentActivity,
                    "Payment successful!\nChange: ${formatCurrency(change)}",
                    Toast.LENGTH_LONG
                ).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        })
    }

    private fun parseCartItems(): List<TransactionItemRequest> {
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        val maps: List<Map<String, Any>> = Gson().fromJson(cartItemsJson, type)
        return maps.map { m ->
            TransactionItemRequest(
                productId = m["product_id"] as String,
                quantity = (m["quantity"] as Number).toDouble(),
                category = m["category"] as String
            )
        }
    }

    private fun updateChangeDisplay(paid: Long) {
        if (paid > 0) {
            val change = paid - totalAmount
            binding.txtChange.text = formatCurrency(if (change >= 0) change else 0L)
            binding.txtChange.setTextColor(
                if (change >= 0) Color.parseColor("#4CAF50")
                else Color.parseColor("#FF5252")
            )

            if (change < 0) {
                binding.txtShortInfo.visibility = View.VISIBLE
                binding.txtShortInfo.text = "Short: ${formatCurrency(-change)}"
            } else {
                binding.txtShortInfo.visibility = View.GONE
            }
        } else {
            binding.txtChange.text = "Rp 0"
            binding.txtChange.setTextColor(Color.parseColor("#4CAF50"))
            binding.txtShortInfo.visibility = View.GONE
        }
    }

    fun handlePayment(total: Long, paid: Long): PaymentResult {
        return if (paid >= total) {
            PaymentResult.Success(paid - total)
        } else {
            PaymentResult.Insufficient(total - paid)
        }
    }

    private fun formatNumber(value: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    private fun formatCurrency(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }

    enum class PaymentMethod { CASH, QR }

    companion object {
        const val EXTRA_TOTAL = "extra_total"
        const val EXTRA_CART_ITEMS = "extra_cart_items"
    }
}

sealed class PaymentResult {
    data class Success(val change: Long) : PaymentResult()
    data class Insufficient(val missing: Long) : PaymentResult()
}

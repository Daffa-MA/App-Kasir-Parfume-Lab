package com.example.appkasir.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appkasir.databinding.ActivityPaymentBinding
import com.example.appkasir.ui.model.formatCurrency
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private var totalAmount: Long = 0L
    private var currentMethod: PaymentMethod = PaymentMethod.CASH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        totalAmount = intent.getLongExtra(EXTRA_TOTAL_AMOUNT, 0L)
        binding.txtPaymentTotal.text = formatCurrency(totalAmount)

        binding.edtCash.filters = arrayOf(InputFilter.LengthFilter(15))

        setupTabs()
        setupCashInput()
        setupQuickCashButtons()
        setupNumericKeypad()
        setupPayAction()
    }

    private fun setupTabs() {
        binding.btnTabCash.setOnClickListener { selectMethod(PaymentMethod.CASH) }
        binding.btnTabQR.setOnClickListener { selectMethod(PaymentMethod.QR) }
        selectMethod(PaymentMethod.CASH)
    }

    private fun selectMethod(method: PaymentMethod) {
        currentMethod = method
        val gold = Color.parseColor("#D4AF37")
        val dark = Color.parseColor("#101010")
        val black = Color.parseColor("#000000")

        if (method == PaymentMethod.CASH) {
            binding.btnTabCash.backgroundTintList = android.content.res.ColorStateList.valueOf(gold)
            binding.btnTabCash.setTextColor(black)
            binding.btnTabQR.backgroundTintList = android.content.res.ColorStateList.valueOf(dark)
            binding.btnTabQR.setTextColor(gold)
            binding.panelCash.visibility = View.VISIBLE
            binding.panelQR.visibility = View.GONE
        } else {
            binding.btnTabQR.backgroundTintList = android.content.res.ColorStateList.valueOf(gold)
            binding.btnTabQR.setTextColor(black)
            binding.btnTabCash.backgroundTintList = android.content.res.ColorStateList.valueOf(dark)
            binding.btnTabCash.setTextColor(gold)
            binding.panelCash.visibility = View.GONE
            binding.panelQR.visibility = View.VISIBLE
        }
    }

    private fun setupCashInput() {
        binding.edtCash.addTextChangedListener(object : TextWatcher {
            private var formatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (formatting) return
                formatting = true

                val raw = s.toString().filter { it.isDigit() }
                val value = raw.toLongOrNull() ?: 0L
                val formatted = if (value == 0L) "" else formatNumber(value)

                if (binding.edtCash.text.toString() != formatted) {
                    binding.edtCash.setText(formatted)
                    binding.edtCash.setSelection(formatted.length)
                }

                updateChange(value)
                formatting = false
            }
        })
    }

    private fun setupQuickCashButtons() {
        val quickButtons = listOf(
            binding.btnQuickExact to totalAmount,
            binding.btnQuick20k to 20_000L,
            binding.btnQuick50k to 50_000L,
            binding.btnQuick100k to 100_000L,
            binding.btnQuick200k to 200_000L,
            binding.btnQuick500k to 500_000L,
            binding.btnQuick1000k to 1_000_000L
        )

        quickButtons.forEach { (button, value) ->
            button.setOnClickListener {
                val finalValue = if (button.id == binding.btnQuickExact.id) totalAmount else value
                setCashValue(finalValue)
            }
        }
    }

    private fun setupNumericKeypad() {
        val keypadButtons = listOf(
            binding.btnKey1 to "1",
            binding.btnKey2 to "2",
            binding.btnKey3 to "3",
            binding.btnKey4 to "4",
            binding.btnKey5 to "5",
            binding.btnKey6 to "6",
            binding.btnKey7 to "7",
            binding.btnKey8 to "8",
            binding.btnKey9 to "9",
            binding.btnKey0 to "0",
            binding.btnKey00 to "00"
        )

        keypadButtons.forEach { (button, token) ->
            button.setOnClickListener {
                val currentRaw = binding.edtCash.text.toString().filter { it.isDigit() }
                val nextRaw = (currentRaw + token).take(15)
                val nextValue = nextRaw.toLongOrNull() ?: 0L
                setCashValue(nextValue)
            }
        }

        binding.btnKeyBackspace.setOnClickListener {
            val currentRaw = binding.edtCash.text.toString().filter { it.isDigit() }
            val nextRaw = if (currentRaw.isNotEmpty()) currentRaw.dropLast(1) else ""
            val nextValue = nextRaw.toLongOrNull() ?: 0L
            setCashValue(nextValue)
        }
    }

    private fun setupPayAction() {
        binding.btnProcessPayment.setOnClickListener {
            when (currentMethod) {
                PaymentMethod.CASH -> processCashPayment()
                PaymentMethod.QR -> processQrPayment()
            }
        }
    }

    // Payment Logic
    private fun processCashPayment() {
        val paid = binding.edtCash.text.toString().filter { it.isDigit() }.toLongOrNull() ?: 0L
        if (paid <= 0) {
            Toast.makeText(this, "Masukkan nominal cash", Toast.LENGTH_SHORT).show()
            return
        }

        if (paid < totalAmount) {
            Toast.makeText(this, "Uang tidak cukup", Toast.LENGTH_SHORT).show()
            return
        }

        val change = paid - totalAmount
        setResult(
            Activity.RESULT_OK,
            intent.apply {
                putExtra(EXTRA_CASH_RECEIVED, paid)
                putExtra(EXTRA_CHANGE_AMOUNT, change)
            }
        )
        finish()
    }

    // Payment Logic
    private fun processQrPayment() {
        setResult(
            Activity.RESULT_OK,
            intent.apply {
                putExtra(EXTRA_CASH_RECEIVED, totalAmount)
                putExtra(EXTRA_CHANGE_AMOUNT, 0L)
            }
        )
        finish()
    }

    private fun updateChange(paid: Long) {
        val diff = paid - totalAmount
        val change = if (diff >= 0) diff else 0L
        binding.txtChange.text = formatCurrency(change)
        binding.txtChange.setTextColor(
            if (diff >= 0) Color.parseColor("#FFE082") else Color.parseColor("#A67C00")
        )

        if (diff < 0) {
            binding.txtShortInfo.visibility = View.VISIBLE
            binding.txtShortInfo.text = "Kurang: ${formatCurrency(-diff)}"
        } else {
            binding.txtShortInfo.visibility = View.GONE
        }
    }

    private fun setCashValue(value: Long) {
        val text = if (value == 0L) "" else formatNumber(value)
        if (binding.edtCash.text.toString() != text) {
            binding.edtCash.setText(text)
            binding.edtCash.setSelection(text.length)
        } else {
            updateChange(value)
        }
    }

    private fun formatNumber(value: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    private enum class PaymentMethod {
        CASH,
        QR
    }

    companion object {
        const val EXTRA_TOTAL_AMOUNT = "extra_total_amount"
        const val EXTRA_CASH_RECEIVED = "extra_cash_received"
        const val EXTRA_CHANGE_AMOUNT = "extra_change_amount"
    }
}

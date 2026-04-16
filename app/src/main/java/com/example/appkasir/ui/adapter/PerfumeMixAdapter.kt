package com.example.appkasir.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
import com.example.appkasir.ui.model.PerfumeProduct
import com.example.appkasir.ui.model.formatCurrency

data class PerfumeMixState(
    val product: PerfumeProduct,
    var isSelected: Boolean = false,
    var selectedMl: Double = 0.0
)

class PerfumeMixAdapter(
    private val onStateChanged: (List<PerfumeMixState>) -> Unit
) : RecyclerView.Adapter<PerfumeMixAdapter.MixViewHolder>() {

    private val allItems = mutableListOf<PerfumeMixState>()
    private val filteredItems = mutableListOf<PerfumeMixState>()

    fun submitProducts(products: List<PerfumeProduct>) {
        allItems.clear()
        allItems.addAll(products.map { PerfumeMixState(product = it) })
        filteredItems.clear()
        filteredItems.addAll(allItems)
        notifyDataSetChanged()
        onStateChanged(selectedItems())
    }

    fun filter(query: String) {
        val normalized = query.trim()
        filteredItems.clear()
        if (normalized.isBlank()) {
            filteredItems.addAll(allItems)
        } else {
            filteredItems.addAll(
                allItems.filter { it.product.name.contains(normalized, ignoreCase = true) }
            )
        }
        notifyDataSetChanged()
    }

    fun selectedItems(): List<PerfumeMixState> {
        return allItems.filter { it.isSelected && it.selectedMl > 0 }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MixViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mix_perfume, parent, false)
        return MixViewHolder(view)
    }

    override fun onBindViewHolder(holder: MixViewHolder, position: Int) {
        holder.bind(filteredItems[position])
    }

    override fun getItemCount(): Int = filteredItems.size

    inner class MixViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chkUse: CheckBox = itemView.findViewById(R.id.chkUsePerfume)
        private val txtName: TextView = itemView.findViewById(R.id.txtMixPerfumeName)
        private val txtMeta: TextView = itemView.findViewById(R.id.txtMixPerfumeMeta)
        private val edtMl: EditText = itemView.findViewById(R.id.edtMixMl)

        fun bind(state: PerfumeMixState) {
            txtName.text = state.product.name
            txtMeta.text = "${formatCurrency(state.product.pricePerMl)}/ml | stock ${state.product.stockMl.toInt()} ml"

            chkUse.setOnCheckedChangeListener(null)
            chkUse.isChecked = state.isSelected
            chkUse.isEnabled = state.product.stockMl > 0

            val mlText = if (state.selectedMl > 0) {
                if (state.selectedMl % 1.0 == 0.0) state.selectedMl.toInt().toString() else state.selectedMl.toString()
            } else {
                ""
            }

            edtMl.setText(mlText)
            edtMl.isEnabled = chkUse.isChecked && state.isSelected

            // Update visual states based on selection and availability
            val isAvailable = state.product.stockMl > 0
            val isSelected = state.isSelected
            
            if (isAvailable) {
                itemView.alpha = 1f
                // Update text colors based on selection
                val textColor = if (isSelected) {
                    itemView.context.getColor(android.R.color.white)
                } else {
                    itemView.context.getColor(android.R.color.darker_gray)
                }
                txtName.setTextColor(textColor)
                txtMeta.alpha = if (isSelected) 1f else 0.7f
            } else {
                // Out of stock
                itemView.alpha = 0.6f
                txtName.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                txtMeta.alpha = 0.5f
            }

            chkUse.setOnCheckedChangeListener { _, isChecked ->
                state.isSelected = isChecked
                edtMl.isEnabled = isChecked
                if (!isChecked) {
                    state.selectedMl = 0.0
                    edtMl.setText("")
                }
                onStateChanged(selectedItems())
            }

            edtMl.doOnTextChanged { text, _, _, _ ->
                if (!chkUse.isChecked) return@doOnTextChanged
                val value = text?.toString()?.toDoubleOrNull() ?: 0.0
                state.selectedMl = value.coerceAtMost(state.product.stockMl)
                onStateChanged(selectedItems())
            }
        }
    }
}

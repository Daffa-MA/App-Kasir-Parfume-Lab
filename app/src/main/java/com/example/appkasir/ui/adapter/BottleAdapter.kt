package com.example.appkasir.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
import com.example.appkasir.ui.model.BottleProduct
import com.example.appkasir.ui.model.formatCurrency

class BottleAdapter(
    private val onItemClick: (BottleProduct) -> Unit
) : RecyclerView.Adapter<BottleAdapter.BottleViewHolder>() {

    private val items = mutableListOf<BottleProduct>()

    fun submitList(newItems: List<BottleProduct>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bottle, parent, false)
        return BottleViewHolder(view)
    }

    override fun onBindViewHolder(holder: BottleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BottleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtBottleName: TextView = itemView.findViewById(R.id.txtBottleName)
        private val txtBottleCapacity: TextView = itemView.findViewById(R.id.txtBottleCapacity)
        private val txtBottlePrice: TextView = itemView.findViewById(R.id.txtBottlePrice)
        private val txtBottleStock: TextView = itemView.findViewById(R.id.txtBottleStock)

        fun bind(item: BottleProduct) {
            txtBottleName.text = item.name
            txtBottleCapacity.text = "${item.capacityMl} ml"
            txtBottlePrice.text = formatCurrency(item.price)
            txtBottleStock.text = if (item.stockPcs > 0) "${item.stockPcs} pcs" else "SOLD OUT"
            itemView.alpha = if (item.stockPcs > 0) 1f else 0.4f
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}

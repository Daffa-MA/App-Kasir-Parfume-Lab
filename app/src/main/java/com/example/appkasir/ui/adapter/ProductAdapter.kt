package com.example.appkasir.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
import com.example.pos.Product
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val items = mutableListOf<Product>()

    fun submitList(newItems: List<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.txtProductName)
        private val tvMeta: TextView = itemView.findViewById(R.id.txtPricePerMl)
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        fun bind(item: Product) {
            val priceValue = "${currencyFormat.format(item.pricePerMl)}/ml"
            val stockValue = "Stok: ${item.stockMl.toInt()} ml"

            tvName.text = item.name
            tvMeta.text = "$priceValue | $stockValue"
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}

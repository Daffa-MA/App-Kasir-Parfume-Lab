package com.example.appkasir.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
import com.example.appkasir.ui.model.BottleProduct
import com.example.appkasir.ui.model.formatCurrency

class ProductAdapter(
    private val onItemClick: (BottleProduct) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val items = mutableListOf<BottleProduct>()

    fun submitList(newItems: List<BottleProduct>) {
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
        private val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        private val tvName: TextView = itemView.findViewById(R.id.txtProductName)
        private val tvMeta: TextView = itemView.findViewById(R.id.txtPricePerMl)
        private val tvStock: TextView = itemView.findViewById(R.id.txtStock)

        fun bind(item: BottleProduct) {
            val priceValue = "${formatCurrency(item.price)} | ${item.capacityMl} ml"

            tvName.text = item.name
            tvMeta.text = priceValue
            tvStock.text = if (item.stockPcs > 0) "${item.stockPcs} pcs" else "SOLD OUT"
            imgProduct.setImageResource(R.drawable.ic_catalog_bottle)
            itemView.alpha = if (item.stockPcs > 0) 1f else 0.4f
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}

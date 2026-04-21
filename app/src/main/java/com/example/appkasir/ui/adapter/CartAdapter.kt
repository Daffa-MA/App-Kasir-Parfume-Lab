package com.example.appkasir.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
import com.example.appkasir.ui.model.CartItem
import com.example.appkasir.ui.model.formatCurrency

class CartAdapter(
    private val onRemove: (CartItem) -> Unit,
    private val onEdit: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val items = mutableListOf<CartItem>()

    fun submitList(newItems: List<CartItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtCartItemIcon: TextView = itemView.findViewById(R.id.txtCartItemIcon)
        private val txtCartItemName: TextView = itemView.findViewById(R.id.txtCartItemName)
        private val txtCartItemDetail: TextView = itemView.findViewById(R.id.txtCartItemDetail)
        private val txtCartItemSubtotal: TextView = itemView.findViewById(R.id.txtCartItemSubtotal)
        private val btnEditItem: ImageButton = itemView.findViewById(R.id.btnEditItem)
        private val btnRemoveItem: ImageButton = itemView.findViewById(R.id.btnRemoveItem)

        fun bind(item: CartItem) {
            txtCartItemName.text = item.name
            txtCartItemDetail.text = item.detail
            txtCartItemSubtotal.text = formatCurrency(item.subtotal)

            if (item is CartItem.Perfume) {
                txtCartItemIcon.text = "P"
                txtCartItemIcon.setBackgroundColor(Color.parseColor("#D4AF37"))
                txtCartItemSubtotal.setTextColor(Color.parseColor("#D4AF37"))
            } else {
                txtCartItemIcon.text = "B"
                txtCartItemIcon.setBackgroundColor(Color.parseColor("#A67C00"))
                txtCartItemSubtotal.setTextColor(Color.parseColor("#A67C00"))
            }

            btnEditItem.setOnClickListener { onEdit(item) }
            btnRemoveItem.setOnClickListener { onRemove(item) }
        }
    }
}

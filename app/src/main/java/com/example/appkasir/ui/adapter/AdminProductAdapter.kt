package com.example.appkasir.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appkasir.R
import com.example.appkasir.ui.model.BottleProduct
import com.example.appkasir.ui.model.PerfumeProduct
import com.example.appkasir.ui.model.formatCurrency

class AdminProductAdapter(
    private val currentType: String,
    private val perfumes: List<PerfumeProduct>,
    private val bottles: List<BottleProduct>,
    private val onEdit: (item: Any) -> Unit,
    private val onDelete: (item: Any) -> Unit
) : RecyclerView.Adapter<AdminProductAdapter.ViewHolder>() {

    override fun getItemCount(): Int = if (currentType == "perfume") perfumes.size else bottles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (currentType == "perfume") {
            holder.bindPerfume(perfumes[position])
        } else {
            holder.bindBottle(bottles[position])
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgIcon = itemView.findViewById<TextView>(R.id.imgProductIcon)
        private val txtId = itemView.findViewById<TextView>(R.id.txtProductId)
        private val txtName = itemView.findViewById<TextView>(R.id.txtProductName)
        private val txtDetail = itemView.findViewById<TextView>(R.id.txtProductDetail)
        private val btnEdit = itemView.findViewById<ImageButton>(R.id.btnEditProduct)
        private val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDeleteProduct)

        fun bindPerfume(perfume: PerfumeProduct) {
            txtId.text = "ID: ${perfume.id}"
            txtName.text = perfume.name
            txtDetail.text = "Price: ${formatCurrency(perfume.pricePerMl)} / Stock: ${perfume.stockMl} ml"
            imgIcon.text = "P"
            imgIcon.setBackgroundColor(itemView.context.getColor(R.color.pos_gold_light))

            btnEdit.setOnClickListener { onEdit(perfume) }
            btnDelete.setOnClickListener { onDelete(perfume) }
        }

        fun bindBottle(bottle: BottleProduct) {
            txtId.text = "ID: ${bottle.id}"
            txtName.text = bottle.name
            txtDetail.text = "Capacity: ${bottle.capacityMl}ml / Price: ${formatCurrency(bottle.price)} / Stock: ${bottle.stockPcs} pcs"
            imgIcon.text = "B"
            imgIcon.setBackgroundColor(itemView.context.getColor(R.color.pos_teal))

            btnEdit.setOnClickListener { onEdit(bottle) }
            btnDelete.setOnClickListener { onDelete(bottle) }
        }
    }
}

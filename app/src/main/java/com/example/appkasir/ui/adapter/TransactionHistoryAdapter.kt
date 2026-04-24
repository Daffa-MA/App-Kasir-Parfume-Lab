        package com.example.appkasir.ui.adapter

        import android.graphics.Color
        import android.view.LayoutInflater
        import android.view.View
        import android.view.ViewGroup
        import android.widget.TextView
        import com.google.android.material.card.MaterialCardView
        import androidx.recyclerview.widget.RecyclerView
        import com.example.appkasir.R
        import com.example.appkasir.data.local.entity.TransactionWithItems
        import com.example.appkasir.ui.model.formatCurrency
        import com.example.appkasir.ui.model.formatQuantity
        import java.text.SimpleDateFormat
        import java.util.Date
        import java.util.Locale

        class TransactionHistoryAdapter(
            private val onItemClick: (TransactionWithItems) -> Unit
        ) : RecyclerView.Adapter<TransactionHistoryAdapter.HistoryViewHolder>() {

            private val items = mutableListOf<TransactionWithItems>()
            private var selectedTransactionId: Long? = null

            fun submitList(newItems: List<TransactionWithItems>) {
                items.clear()
                items.addAll(newItems)
                notifyDataSetChanged()
            }

            fun setSelectedTransactionId(id: Long?) {
                selectedTransactionId = id
                notifyDataSetChanged()
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction_history, parent, false)
                return HistoryViewHolder(view)
            }

            override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
                holder.bind(items[position])
            }

            override fun getItemCount(): Int = items.size

            inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                private val cardItem: MaterialCardView = itemView.findViewById(R.id.cardHistoryItem)
                private val viewAccent: View = itemView.findViewById(R.id.viewHistoryAccent)
                private val txtId: TextView = itemView.findViewById(R.id.txtHistoryId)
                private val txtTime: TextView = itemView.findViewById(R.id.txtHistoryTime)
                private val txtStatus: TextView = itemView.findViewById(R.id.txtHistoryStatus)
                private val txtItems: TextView = itemView.findViewById(R.id.txtHistoryItems)
                private val txtTotal: TextView = itemView.findViewById(R.id.txtHistoryTotal)

                fun bind(transaction: TransactionWithItems) {
                    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    txtId.text = "#${transaction.transaction.id}"
                    txtTime.text = formatter.format(Date(transaction.transaction.createdAt))
                    txtStatus.text = transaction.transaction.status
                    txtTotal.text = formatCurrency(transaction.transaction.roundedTotal)
                    txtItems.text = transaction.items.joinToString(" • ") {
                        "${it.name} x${formatQuantity(it.qty)}"
                    }

                    val isSelected = selectedTransactionId == transaction.transaction.id
                    val statusColor = when (transaction.transaction.status) {
                        "SYNCED" -> Color.parseColor("#FFE082")
                        "PENDING" -> Color.parseColor("#D4AF37")
                        else -> Color.parseColor("#A67C00")
                    }
                    txtStatus.setTextColor(statusColor)
                    txtTotal.setTextColor(statusColor)
                    viewAccent.setBackgroundColor(if (isSelected) Color.parseColor("#D4AF37") else Color.parseColor("#4A3A10"))
                    cardItem.strokeWidth = if (isSelected) 2 else 1
                    cardItem.cardElevation = if (isSelected) 4f else 0f
                    cardItem.setCardBackgroundColor(
                        if (isSelected) Color.parseColor("#1A1A1A") else Color.parseColor("#101010")
                    )

                    itemView.setOnClickListener { onItemClick(transaction) }
                }
            }
            }